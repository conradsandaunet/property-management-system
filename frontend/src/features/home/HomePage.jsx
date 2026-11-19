import { useState } from "react";
import { Link } from "react-router-dom";
import { RequestStatus } from "../../components/Indicators";
import FacadeImage from "../../components/FacadeImage";
import Icon from "../../components/Icon";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { getInvoices, payInvoice } from "../../api/billing";
import { getMyMaintenance } from "../../api/maintenance";
import { getMyReservations } from "../../api/reservations";
import { useNotifications } from "../notifications/useNotifications";
import { formatDate, formatDateTime, formatRelativeDay } from "../../lib/format";
import { formatMoney, formatPeriod } from "../billing/format";
import { requestTitle } from "../maintenance/labels";
import { formatTime } from "../reservations/format";

function InvoiceBanner({ invoices, onPay, paying }) {
    const unpaid = invoices.filter((i) => i.status !== "PAID");
    if (unpaid.length === 0) return null;

    const overdue = unpaid.filter((i) => i.status === "OVERDUE");
    const invoice = overdue[0] ?? unpaid[0];
    const isOverdue = invoice.status === "OVERDUE";

    return (
        <div className={`banner banner--${isOverdue ? "warning" : "info"}`}>
            <Icon name="receipt" size={22} />
            <div className="banner__body">
                <div className="banner__title">
                    {isOverdue
                        ? `Your ${formatPeriod(invoice.period)} invoice is overdue`
                        : `Your ${formatPeriod(invoice.period)} invoice is ready`}
                </div>
                <div className="small">
                    {formatMoney(invoice.totalAmount)} · due {formatDate(invoice.dueDate)}
                    {overdue.length > 1 && ` · ${overdue.length} overdue invoices`}
                </div>
            </div>
            <Link className="btn btn--secondary" to="/invoices">View</Link>
            <button type="button" className="btn" disabled={paying} onClick={() => onPay(invoice)}
                    title="Simulates paying in your online bank">
                {paying ? "Paying…" : "Pay (simulated)"}
            </button>
        </div>
    );
}

function DashCard({ title, link, children }) {
    return (
        <section className="card">
            <div className="section__head">
                <h2 className="section__title">{title}</h2>
                {link}
            </div>
            {children}
        </section>
    );
}

export default function HomePage() {
    const { token, me } = useAuth();
    const toast = useToast();
    const invoices = useAsync(() => getInvoices(token), [token]);
    const requests = useAsync(() => getMyMaintenance(token), [token]);
    const bookings = useAsync(() => getMyReservations(token), [token]);
    const { items: notifications } = useNotifications();
    const [paying, setPaying] = useState(false);

    async function handlePay(invoice) {
        setPaying(true);
        try {
            await payInvoice(token, invoice.id);
            toast(`Paid ${formatMoney(invoice.totalAmount)} for ${formatPeriod(invoice.period)}`);
            invoices.reload();
        } catch {
            toast("Couldn't pay the invoice", { kind: "error" });
        } finally {
            setPaying(false);
        }
    }

    const openRequests = (requests.data ?? []).filter((r) => r.status !== "RESOLVED");
    const now = new Date();
    const upcoming = (bookings.data ?? [])
        .filter((b) => b.status === "ACTIVE" && new Date(b.endTime) > now)
        .sort((a, b) => a.startTime.localeCompare(b.startTime));

    return (
        <>
            <div className="home-head">
                <PageHeader
                    eyebrow={`Apartment ${me.apartmentNumber} · ${me.buildingName}`}
                    title={`Hi, ${me.firstName}`}
                    subtitle="Here's what's going on with your home."
                />
                {me.buildingId && (
                    <FacadeImage buildingId={me.buildingId} alt={`${me.buildingName} facade`} className="facade facade--thumb" />
                )}
            </div>

            {invoices.error && <Notice kind="error" onRetry={invoices.reload}>Couldn't load your invoices.</Notice>}
            {invoices.data && <InvoiceBanner invoices={invoices.data} onPay={handlePay} paying={paying} />}

            <div className="grid">
                <DashCard title="Your requests" link={<Link className="btn-link" to="/maintenance?new=1">Report an issue</Link>}>
                    {requests.loading && <Skeleton rows={2} />}
                    {requests.error && <Notice kind="error" onRetry={requests.reload}>Couldn't load your requests.</Notice>}
                    {requests.data && openRequests.length === 0 && (
                        <p className="muted">No open requests. Report an issue if something needs fixing.</p>
                    )}
                    <ul className="rows">
                        {openRequests.slice(0, 3).map((r) => (
                            <li key={r.id} className="row">
                                <Link to="/maintenance" className="row__label" style={{ color: "var(--ink)", textDecoration: "none" }}>
                                    {requestTitle(r)}
                                </Link>
                                <RequestStatus status={r.status} />
                            </li>
                        ))}
                    </ul>
                </DashCard>

                <DashCard title="Upcoming bookings" link={<Link className="btn-link" to="/bookings">Book</Link>}>
                    {bookings.loading && <Skeleton rows={2} />}
                    {bookings.error && <Notice kind="error" onRetry={bookings.reload}>Couldn't load your bookings.</Notice>}
                    {bookings.data && upcoming.length === 0 && (
                        <p className="muted">Nothing booked. Reserve the common room or a guest parking space.</p>
                    )}
                    <ul className="rows">
                        {upcoming.slice(0, 3).map((b) => (
                            <li key={b.id} className="row">
                                <span>{b.resourceName}</span>
                                <span className="muted small">
                                    {formatRelativeDay(b.startTime)} {formatTime(b.startTime)}–{formatTime(b.endTime)}
                                </span>
                            </li>
                        ))}
                    </ul>
                </DashCard>

                <DashCard title="Notifications" link={<Link className="btn-link" to="/notifications">See all</Link>}>
                    {notifications.length === 0 && <p className="muted">You're all caught up.</p>}
                    <ul className="rows">
                        {notifications.slice(0, 3).map((n) => (
                            <li key={n.id} className="row" style={{ alignItems: "flex-start" }}>
                                <span style={{ fontWeight: n.read ? 400 : 500 }}>{n.message}</span>
                                <span className="muted small" style={{ whiteSpace: "nowrap" }}>{formatDateTime(n.createdAt)}</span>
                            </li>
                        ))}
                    </ul>
                </DashCard>
            </div>
        </>
    );
}
