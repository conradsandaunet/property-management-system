import { useState } from "react";
import { StatusDot } from "../../components/Indicators";
import EmptyState from "../../components/EmptyState";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { getInvoicePdf, getInvoices, payInvoice } from "../../api/billing";
import {
    CATEGORY_LABELS,
    STATUS_LABELS,
    STATUS_TONE,
    downloadPdf,
    formatDate,
    formatMoney,
    formatPeriod,
} from "./format";

export default function InvoicesPage() {
    const { token } = useAuth();
    const toast = useToast();
    // Newest first: the first one is the current invoice, the rest is history.
    const { data: invoices, error, loading, reload } = useAsync(() => getInvoices(token), [token]);
    const [busyId, setBusyId] = useState(null);

    async function handlePay(invoice) {
        setBusyId(invoice.id);
        try {
            await payInvoice(token, invoice.id);
            toast(`Paid ${formatMoney(invoice.totalAmount)} for ${formatPeriod(invoice.period)}`);
            reload();
        } catch (e) {
            toast(e.status === 409 ? "This invoice is already paid" : "Couldn't pay the invoice", { kind: "error" });
        } finally {
            setBusyId(null);
        }
    }

    async function handlePdf(invoice) {
        try {
            downloadPdf(await getInvoicePdf(token, invoice.id), `invoice-${invoice.period}.pdf`);
        } catch {
            toast("Couldn't download the invoice", { kind: "error" });
        }
    }

    const [current, ...history] = invoices ?? [];

    return (
        <>
            <PageHeader title="Invoices" subtitle="Monthly shared costs for your apartment." />

            {error && <Notice kind="error">Couldn't load your invoices.</Notice>}
            {loading && <Skeleton rows={4} />}
            {invoices && invoices.length === 0 && (
                <EmptyState title="No invoices yet">Your monthly invoice will show up here.</EmptyState>
            )}

            {current && (
                <section className="card section" style={{ maxWidth: "var(--w-page)" }}>
                    <div className="section__head">
                        <div>
                            <h2 className="section__title">{formatPeriod(current.period)}</h2>
                            <p className="small muted">Due {formatDate(current.dueDate)}</p>
                        </div>
                        <StatusDot tone={STATUS_TONE[current.status]}>{STATUS_LABELS[current.status]}</StatusDot>
                    </div>
                    <ul className="rows">
                        {current.lines.map((line, i) => (
                            <li key={i} className="row">
                                <span>
                                    {CATEGORY_LABELS[line.category] ?? line.category}
                                    <span className="muted small"> · {line.description}</span>
                                </span>
                                <span className="row__value">{formatMoney(line.amount)}</span>
                            </li>
                        ))}
                        <li className="row">
                            <span className="item__title">Total</span>
                            <span className="row__value row__value--total">{formatMoney(current.totalAmount)}</span>
                        </li>
                    </ul>
                    {current.status !== "PAID" && (
                        <div className="pay-details" aria-label="Payment details">
                            <div>
                                <div className="pay-details__label">Account number</div>
                                <div className="pay-details__value">{current.accountNumber}</div>
                            </div>
                            <div>
                                <div className="pay-details__label">KID</div>
                                <div className="pay-details__value">{current.kid}</div>
                            </div>
                            <div>
                                <div className="pay-details__label">Amount</div>
                                <div className="pay-details__value">{formatMoney(current.totalAmount)}</div>
                            </div>
                            <div>
                                <div className="pay-details__label">Due</div>
                                <div className="pay-details__value">{formatDate(current.dueDate)}</div>
                            </div>
                        </div>
                    )}

                    <div className="form-actions">
                        {current.status !== "PAID" && (
                            <button className="btn" type="button" disabled={busyId === current.id}
                                    onClick={() => handlePay(current)}>
                                {busyId === current.id ? "Paying…" : "Pay in online bank (simulated)"}
                            </button>
                        )}
                        <button className="btn btn--secondary" type="button" onClick={() => handlePdf(current)}>
                            Download PDF
                        </button>
                    </div>
                    {current.status !== "PAID" && (
                        <p className="hint">
                            Pay with the account number and KID in your online bank. The button simulates the bank
                            confirming the payment; in a real setup it would come from the bank.
                        </p>
                    )}
                </section>
            )}

            {history.length > 0 && (
                <section className="section">
                    <div className="section__head"><h2 className="section__title">Earlier invoices</h2></div>
                    <div className="table-wrap">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Period</th>
                                    <th className="hide-sm">Due</th>
                                    <th className="num">Amount</th>
                                    <th>Status</th>
                                    <th className="actions"><span className="sr-only">Actions</span></th>
                                </tr>
                            </thead>
                            <tbody>
                                {history.map((inv) => (
                                    <tr key={inv.id}>
                                        <td className="table__primary">{formatPeriod(inv.period)}</td>
                                        <td className="hide-sm muted">{formatDate(inv.dueDate)}</td>
                                        <td className="num">{formatMoney(inv.totalAmount)}</td>
                                        <td><StatusDot tone={STATUS_TONE[inv.status]}>{STATUS_LABELS[inv.status]}</StatusDot></td>
                                        <td className="actions">
                                            {inv.status !== "PAID" && (
                                                <button className="btn btn--sm" type="button" disabled={busyId === inv.id}
                                                        title={`KID ${inv.kid}. Simulates paying in your online bank.`}
                                                        onClick={() => handlePay(inv)}>
                                                    Pay (simulated)
                                                </button>
                                            )}{" "}
                                            <button className="btn btn--ghost btn--sm" type="button" onClick={() => handlePdf(inv)}>
                                                PDF
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            )}
        </>
    );
}
