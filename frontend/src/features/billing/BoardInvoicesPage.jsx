import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "../residents/errors";
import { getApartments } from "../../api/residents";
import { generateInvoices, getPaymentOverview, markInvoicePaid } from "../../api/billing";
import {
    STATUS_LABELS,
    STATUS_VARIANT,
    currentPeriod,
    formatDate,
    formatMoney,
    formatPeriod,
} from "./format";

const FILTERS = [
    { value: "", label: "All" },
    { value: "OVERDUE", label: "Overdue" },
    { value: "PENDING", label: "Pending" },
    { value: "PAID", label: "Paid" },
];

export default function BoardInvoicesPage() {
    const { token } = useAuth();
    const [period, setPeriod] = useState(currentPeriod);
    const [filter, setFilter] = useState("");
    const [error, setError] = useState("");
    const [message, setMessage] = useState("");
    const [generating, setGenerating] = useState(false);
    const [confirmingId, setConfirmingId] = useState(null);

    const overview = useAsync(() => getPaymentOverview(token, period), [token, period]);
    // Billing only stores apartment ids; the numbers come from resident-service.
    const apartments = useAsync(() => getApartments(token), [token]);

    const apartmentNumber = (id) =>
        apartments.data?.find((a) => a.id === id)?.apartmentNumber ?? `#${id}`;

    async function handleGenerate() {
        setGenerating(true);
        setError("");
        setMessage("");
        try {
            const created = await generateInvoices(token, period);
            setMessage(created.length > 0
                ? `Created ${created.length} invoice${created.length > 1 ? "s" : ""} for ${formatPeriod(period)}.`
                : `Every apartment already has an invoice for ${formatPeriod(period)}.`);
            overview.reload();
        } catch (e) {
            setError(describeError(e, "Couldn't generate invoices.", {
                502: "Couldn't reach the resident service to get the list of apartments.",
            }));
        } finally {
            setGenerating(false);
        }
    }

    async function handleMarkPaid(id) {
        setError("");
        setMessage("");
        try {
            await markInvoicePaid(token, id);
            overview.reload();
        } catch (e) {
            setError(describeError(e, "Couldn't mark the invoice as paid.", { 409: "This invoice is already paid." }));
        } finally {
            setConfirmingId(null);
        }
    }

    const data = overview.data;
    const stats = data && [
        ["Overdue", data.overdue],
        ["Pending", data.pending],
        ["Paid", data.paid],
        ["Invoices", data.invoices.length],
    ];
    const visible = data && data.invoices
        .filter((inv) => !filter || inv.status === filter)
        .sort((a, b) => apartmentNumber(a.apartmentId)
            .localeCompare(apartmentNumber(b.apartmentId), undefined, { numeric: true }));

    return (
        <>
            <PageHeader eyebrow="Board" title="Invoices" />

            <div className="toolbar">
                <div className="field">
                    <label className="label" htmlFor="period">Period</label>
                    <input id="period" className="input" type="month" value={period}
                           onChange={(e) => { setPeriod(e.target.value || currentPeriod()); setMessage(""); }} />
                </div>
                <div className="field">
                    <label className="label" htmlFor="status-filter">Show</label>
                    <select id="status-filter" className="input" value={filter}
                            onChange={(e) => setFilter(e.target.value)}>
                        {FILTERS.map((f) => <option key={f.value} value={f.value}>{f.label}</option>)}
                    </select>
                </div>
                <button className="btn" type="button" disabled={generating} onClick={handleGenerate}>
                    {generating ? "Generating…" : "Generate invoices"}
                </button>
            </div>

            {message && <Notice kind="success">{message}</Notice>}
            {(error || overview.error) && (
                <Notice kind="error">{error || describeError(overview.error, "Couldn't load the overview.")}</Notice>
            )}
            {overview.loading && <Notice>Loading...</Notice>}

            {stats && (
                <ul className="rows section-gap">
                    {stats.map(([label, value]) => (
                        <li key={label} className="row">
                            <span className="row__label">{label}</span>
                            <span className="row__value row__value--stat">{value}</span>
                        </li>
                    ))}
                </ul>
            )}

            {data && data.invoices.length === 0 && (
                <Notice>No invoices for {formatPeriod(period)} yet. Use “Generate invoices” to create them.</Notice>
            )}

            {data && data.invoices.length > 0 && visible.length === 0 && (
                <Notice>No invoices with this status.</Notice>
            )}

            {visible && visible.length > 0 && (
                <ul className="list list--wide">
                    {visible.map((inv) => (
                        <li key={inv.id} className="list-item">
                            <div className="list-item__top">
                                <span className="list-item__title">Apartment {apartmentNumber(inv.apartmentId)}</span>
                                <Badge variant={STATUS_VARIANT[inv.status]}>{STATUS_LABELS[inv.status]}</Badge>
                            </div>
                            <div className="list-item__meta">
                                <span>{formatMoney(inv.totalAmount)}</span>
                                <span>Due {formatDate(inv.dueDate)}</span>
                                <span>Invoice #{inv.id}</span>
                            </div>
                            {inv.status !== "PAID" && (
                                <div className="list-item__actions">
                                    {confirmingId !== inv.id ? (
                                        <button className="btn-link" type="button"
                                                onClick={() => setConfirmingId(inv.id)}>
                                            Mark as paid
                                        </button>
                                    ) : (
                                        <span className="list-item__confirm">
                                            Register payment for apartment {apartmentNumber(inv.apartmentId)}?
                                            <button className="btn-link" type="button"
                                                    onClick={() => handleMarkPaid(inv.id)}>
                                                Yes, mark as paid
                                            </button>
                                            <button className="btn-link" type="button"
                                                    onClick={() => setConfirmingId(null)}>
                                                Cancel
                                            </button>
                                        </span>
                                    )}
                                </div>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
