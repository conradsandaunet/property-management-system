import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { StatusDot } from "../../components/Indicators";
import ConfirmDialog from "../../components/ConfirmDialog";
import EmptyState from "../../components/EmptyState";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import StatCard from "../../components/StatCard";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useDirectory } from "../../hooks/useDirectory";
import { useAuth } from "../../auth/useAuth";
import { generateInvoices, getPaymentOverview, markInvoicePaid } from "../../api/billing";
import { STATUS_LABELS, STATUS_TONE, currentPeriod, formatDate, formatMoney, formatPeriod } from "./format";

const sum = (invoices) => invoices.reduce((total, i) => total + Number(i.totalAmount), 0);

export default function BillingPage() {
    const { token } = useAuth();
    const toast = useToast();
    const dir = useDirectory(token);
    const [params, setParams] = useSearchParams();
    const period = params.get("period") ?? currentPeriod();
    const filter = params.get("status") ?? "";
    const overview = useAsync(() => getPaymentOverview(token, period), [token, period]);
    const [generating, setGenerating] = useState(false);
    const [confirmGenerate, setConfirmGenerate] = useState(false);
    const [paying, setPaying] = useState(null);

    function setParam(key, value) {
        const next = new URLSearchParams(params);
        if (value) next.set(key, value); else next.delete(key);
        setParams(next, { replace: true });
    }

    async function handleGenerate() {
        setGenerating(true);
        try {
            const created = await generateInvoices(token, period);
            toast(created.length > 0
                ? `Created ${created.length} invoice${created.length > 1 ? "s" : ""} for ${formatPeriod(period)}`
                : `Every apartment already has an invoice for ${formatPeriod(period)}`);
            overview.reload();
        } catch (e) {
            toast(e.status === 502 ? "Couldn't reach the resident service" : "Couldn't generate invoices", { kind: "error" });
        } finally {
            setGenerating(false);
            setConfirmGenerate(false);
        }
    }

    async function handleMarkPaid() {
        try {
            await markInvoicePaid(token, paying.id);
            toast(`Marked ${dir.apartmentLabel(paying.apartmentId)} as paid`);
            overview.reload();
        } catch (e) {
            toast(e.status === 409 ? "This invoice is already paid" : "Couldn't mark the invoice as paid", { kind: "error" });
        }
        setPaying(null);
    }

    const invoices = overview.data?.invoices ?? [];
    const byStatus = (status) => invoices.filter((i) => !status || i.status === status);
    const visible = byStatus(filter).slice().sort((a, b) =>
        dir.apartmentLabel(a.apartmentId).localeCompare(dir.apartmentLabel(b.apartmentId), undefined, { numeric: true }));

    const stat = (status, label, tone) => {
        const list = byStatus(status);
        return (
            <StatCard key={label} label={label} value={list.length} hint={formatMoney(sum(list))}
                      tone={list.length > 0 ? tone : undefined} active={filter === status}
                      onClick={() => setParam("status", status)} />
        );
    };

    return (
        <>
            <PageHeader
                title="Billing"
                subtitle="Monthly invoices for every apartment."
                actions={(
                    <>
                        <input className="input" type="month" aria-label="Period" value={period} style={{ width: 170 }}
                               onChange={(e) => setParam("period", e.target.value || currentPeriod())} />
                        <button className="btn" type="button" disabled={generating} onClick={() => setConfirmGenerate(true)}>
                            Generate invoices
                        </button>
                    </>
                )}
            />

            {overview.error && <Notice kind="error" onRetry={overview.reload}>Couldn't load the invoices for {formatPeriod(period)}.</Notice>}

            <div className="grid grid--stats section">
                {stat("", "All invoices")}
                {stat("OVERDUE", "Overdue", "warning")}
                {stat("PENDING", "Pending")}
                {stat("PAID", "Paid")}
            </div>

            {overview.loading && <Skeleton rows={4} />}
            {overview.data && invoices.length === 0 && (
                <EmptyState title={`No invoices for ${formatPeriod(period)}`}
                            action={<button className="btn" type="button" onClick={() => setConfirmGenerate(true)}>Generate invoices</button>}>
                    Create one invoice per apartment with the standard monthly charges.
                </EmptyState>
            )}
            {overview.data && invoices.length > 0 && visible.length === 0 && (
                <EmptyState compact>No {STATUS_LABELS[filter]?.toLowerCase()} invoices for {formatPeriod(period)}.</EmptyState>
            )}

            {visible.length > 0 && (
                <div className="table-wrap">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>Apartment</th>
                                <th className="hide-sm">Due</th>
                                <th className="num">Amount</th>
                                <th>Status</th>
                                <th className="actions"><span className="sr-only">Actions</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {visible.map((inv) => (
                                <tr key={inv.id}>
                                    <td className="table__primary">{dir.apartmentLabel(inv.apartmentId)}</td>
                                    <td className="hide-sm muted">{formatDate(inv.dueDate)}</td>
                                    <td className="num">{formatMoney(inv.totalAmount)}</td>
                                    <td><StatusDot tone={STATUS_TONE[inv.status]}>{STATUS_LABELS[inv.status]}</StatusDot></td>
                                    <td className="actions">
                                        {inv.status !== "PAID" && (
                                            <button type="button" className="btn btn--ghost btn--sm" onClick={() => setPaying(inv)}>
                                                Mark as paid
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <ConfirmDialog open={confirmGenerate} title={`Generate invoices for ${formatPeriod(period)}?`}
                           confirmLabel="Generate" onConfirm={handleGenerate} onCancel={() => setConfirmGenerate(false)}>
                Every apartment without an invoice for this month gets one with the standard charges.
                Apartments that already have one are skipped.
            </ConfirmDialog>

            <ConfirmDialog open={Boolean(paying)} title="Register payment?" confirmLabel="Mark as paid"
                           onConfirm={handleMarkPaid} onCancel={() => setPaying(null)}>
                {paying && `${dir.apartmentLabel(paying.apartmentId)}, ${formatPeriod(paying.period)}: ${formatMoney(paying.totalAmount)}. Use this for payments made outside the portal.`}
            </ConfirmDialog>
        </>
    );
}
