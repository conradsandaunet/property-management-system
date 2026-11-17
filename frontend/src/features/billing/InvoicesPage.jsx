import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "../residents/errors";
import { getInvoicePdf, getInvoices, payInvoice } from "../../api/billing";
import {
    CATEGORY_LABELS,
    STATUS_LABELS,
    STATUS_VARIANT,
    downloadPdf,
    formatDate,
    formatMoney,
    formatPeriod,
} from "./format";

export default function InvoicesPage() {
    const { token } = useAuth();
    // Newest first, so the first one is the current invoice and the rest is history.
    const { data: invoices, error: loadError, loading, reload } = useAsync(() => getInvoices(token), [token]);
    const [error, setError] = useState("");
    const [busyId, setBusyId] = useState(null);

    async function handlePay(id) {
        setBusyId(id);
        setError("");
        try {
            await payInvoice(token, id);
            reload();
        } catch (e) {
            setError(describeError(e, "Couldn't pay the invoice.", { 409: "This invoice is already paid." }));
        } finally {
            setBusyId(null);
        }
    }

    async function handlePdf(id) {
        setError("");
        try {
            downloadPdf(await getInvoicePdf(token, id), `invoice-${id}.pdf`);
        } catch (e) {
            setError(describeError(e, "Couldn't download the invoice."));
        }
    }

    const [current, ...history] = invoices ?? [];

    return (
        <>
            <PageHeader eyebrow="Billing" title="My invoices" />

            {(error || loadError) && (
                <Notice kind="error">{error || describeError(loadError, "Couldn't load your invoices.")}</Notice>
            )}
            {loading && <Notice>Loading...</Notice>}
            {invoices && invoices.length === 0 && <Notice>You don't have any invoices yet.</Notice>}

            {current && (
                <section className="section-gap">
                    <div className="list-item__top">
                        <span className="list-item__title">{formatPeriod(current.period)}</span>
                        <Badge variant={STATUS_VARIANT[current.status]}>{STATUS_LABELS[current.status]}</Badge>
                    </div>

                    <ul className="rows section-gap">
                        {current.lines.map((line, i) => (
                            <li key={i} className="row">
                                <span className="row__label">
                                    {line.description} · {CATEGORY_LABELS[line.category] ?? line.category}
                                </span>
                                <span className="row__value">{formatMoney(line.amount)}</span>
                            </li>
                        ))}
                        <li className="row">
                            <span className="row__label">Total · due {formatDate(current.dueDate)}</span>
                            <span className="row__value row__value--stat">{formatMoney(current.totalAmount)}</span>
                        </li>
                    </ul>

                    <div className="form-actions">
                        {current.status !== "PAID" && (
                            <button className="btn" type="button" disabled={busyId === current.id}
                                    onClick={() => handlePay(current.id)}>
                                {busyId === current.id ? "Paying…" : `Pay ${formatMoney(current.totalAmount)}`}
                            </button>
                        )}
                        <button className="btn-link" type="button" onClick={() => handlePdf(current.id)}>
                            Download PDF
                        </button>
                    </div>
                </section>
            )}

            {history.length > 0 && (
                <>
                    <h2 className="list-item__title section-gap">Previous invoices</h2>
                    <ul className="list">
                        {history.map((inv) => (
                            <li key={inv.id} className="list-item">
                                <div className="list-item__top">
                                    <span className="list-item__title">{formatPeriod(inv.period)}</span>
                                    <Badge variant={STATUS_VARIANT[inv.status]}>{STATUS_LABELS[inv.status]}</Badge>
                                </div>
                                <div className="list-item__meta">
                                    <span>{formatMoney(inv.totalAmount)}</span>
                                    <span>Due {formatDate(inv.dueDate)}</span>
                                </div>
                                <div className="list-item__actions">
                                    {inv.status !== "PAID" && (
                                        <button className="btn-link" type="button" disabled={busyId === inv.id}
                                                onClick={() => handlePay(inv.id)}>
                                            {busyId === inv.id ? "Paying…" : "Pay"}
                                        </button>
                                    )}
                                    <button className="btn-link" type="button" onClick={() => handlePdf(inv.id)}>
                                        Download PDF
                                    </button>
                                </div>
                            </li>
                        ))}
                    </ul>
                </>
            )}
        </>
    );
}
