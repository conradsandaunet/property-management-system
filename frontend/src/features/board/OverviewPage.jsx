import { useState } from "react";
import { Link } from "react-router-dom";
import { Priority, StatusDot } from "../../components/Indicators";
import ConfirmDialog from "../../components/ConfirmDialog";
import Drawer from "../../components/Drawer";
import EmptyState from "../../components/EmptyState";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import StatCard from "../../components/StatCard";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useDirectory } from "../../hooks/useDirectory";
import { useAuth } from "../../auth/useAuth";
import { getOverview } from "../../api/residents";
import { getPaymentOverview, markInvoicePaid } from "../../api/billing";
import { assignWorker, getOpenMaintenance, getWorkers } from "../../api/maintenance";
import { getResources } from "../../api/reservations";
import { currentPeriod, formatMoney, formatPeriod } from "../billing/format";
import { PRIORITY_RANK, requestTitle, notifyMaintenanceChanged } from "../maintenance/labels";
import AssignWorkerForm from "../maintenance/AssignWorkerForm";
import { formatDay, isBlockedNow, upcomingBlock } from "../reservations/format";

const SHOWN = 5;

function Panel({ title, link, children }) {
    return (
        <section className="section">
            <div className="section__head">
                <h2 className="section__title">{title}</h2>
                {link}
            </div>
            {children}
        </section>
    );
}

export default function OverviewPage() {
    const { token } = useAuth();
    const toast = useToast();
    const dir = useDirectory(token);
    const building = useAsync(() => getOverview(token), [token]);
    const thisMonth = useAsync(() => getPaymentOverview(token, currentPeriod()), [token]);
    const lastMonth = useAsync(() => getPaymentOverview(token, currentPeriod(-1)), [token]);
    const requests = useAsync(() => getOpenMaintenance(token), [token]);
    const workers = useAsync(() => getWorkers(token), [token]);
    const resources = useAsync(() => getResources(token), [token]);
    const [assigning, setAssigning] = useState(null);
    const [paying, setPaying] = useState(null);

    const unassigned = (requests.data ?? [])
        .filter((r) => r.status === "SUBMITTED")
        .sort((a, b) => PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority] || a.createdAt.localeCompare(b.createdAt));
    const overdue = [...(lastMonth.data?.invoices ?? []), ...(thisMonth.data?.invoices ?? [])]
        .filter((i) => i.status === "OVERDUE");
    const overdueSum = overdue.reduce((sum, i) => sum + Number(i.totalAmount), 0);
    const closed = (resources.data ?? []).filter((r) => isBlockedNow(r) || upcomingBlock(r));

    async function handleAssign(workerId) {
        try {
            await assignWorker(token, assigning.id, workerId);
            toast("Worker assigned");
            setAssigning(null);
            requests.reload();
            notifyMaintenanceChanged();
        } catch {
            toast("Couldn't assign the worker", { kind: "error" });
        }
    }

    async function handleMarkPaid() {
        try {
            await markInvoicePaid(token, paying.id);
            toast(`Marked ${dir.apartmentLabel(paying.apartmentId)} as paid`);
            thisMonth.reload();
            lastMonth.reload();
        } catch {
            toast("Couldn't mark the invoice as paid", { kind: "error" });
        }
        setPaying(null);
    }

    const b = building.data;

    return (
        <>
            <PageHeader title="Overview" subtitle="What needs the board's attention." />

            <div className="grid grid--stats section">
                <StatCard label="Apartments" value={b?.totalApartments ?? "–"}
                          hint={b && `${b.vacantApartments} vacant`} to="/board/property" />
                <StatCard label="Residents" value={b?.totalResidents ?? "–"} to="/board/property?tab=residents" />
                <StatCard label="Overdue invoices" value={overdue.length}
                          hint={overdue.length > 0 ? formatMoney(overdueSum) : "All paid on time"}
                          tone={overdue.length > 0 ? "warning" : undefined} to="/board/billing?status=OVERDUE" />
                <StatCard label="Open requests" value={requests.data?.length ?? "–"}
                          hint={requests.data && `${unassigned.length} need a worker`} to="/board/maintenance" />
            </div>

            <div className="split">
                <Panel title="Needs a worker" link={<Link className="btn-link" to="/board/maintenance">All requests</Link>}>
                    {requests.loading && <Skeleton rows={2} />}
                    {requests.data && unassigned.length === 0 && (
                        <EmptyState compact>Every open request has a worker.</EmptyState>
                    )}
                    {unassigned.length > 0 && (
                        <ul className="item-list card card--flush">
                            {unassigned.slice(0, SHOWN).map((r) => (
                                <li key={r.id} className="item">
                                    <span style={{ width: 76, flexShrink: 0 }}><Priority priority={r.priority} /></span>
                                    <div className="item__body">
                                        <div className="item__title">{requestTitle(r)}</div>
                                        <div className="item__meta">{dir.residentApartment(r.residentId)}</div>
                                    </div>
                                    <button type="button" className="btn btn--secondary btn--sm" onClick={() => setAssigning(r)}>
                                        Assign
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </Panel>

                <Panel title="Overdue invoices" link={<Link className="btn-link" to="/board/billing">Billing</Link>}>
                    {(thisMonth.loading || lastMonth.loading) && <Skeleton rows={2} />}
                    {thisMonth.data && lastMonth.data && overdue.length === 0 && (
                        <EmptyState compact>No overdue invoices.</EmptyState>
                    )}
                    {overdue.length > 0 && (
                        <ul className="item-list card card--flush">
                            {overdue.slice(0, SHOWN).map((inv) => (
                                <li key={inv.id} className="item">
                                    <div className="item__body">
                                        <div className="item__title">{dir.apartmentLabel(inv.apartmentId)}</div>
                                        <div className="item__meta">{formatPeriod(inv.period)} · {formatMoney(inv.totalAmount)}</div>
                                    </div>
                                    <button type="button" className="btn btn--secondary btn--sm" onClick={() => setPaying(inv)}>
                                        Mark as paid
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </Panel>
            </div>

            {closed.length > 0 && (
                <Panel title="Closed resources" link={<Link className="btn-link" to="/board/resources">Resources</Link>}>
                    <ul className="item-list card card--flush">
                        {closed.map((r) => {
                            const block = upcomingBlock(r);
                            return (
                                <li key={r.id} className="item">
                                    <div className="item__body">
                                        <div className="item__title">{r.name}</div>
                                        <div className="item__meta">
                                            {r.status === "MAINTENANCE"
                                                ? "Closed until further notice"
                                                : `Closed ${formatDay(block.from)} – ${formatDay(block.to)}`}
                                        </div>
                                    </div>
                                    <StatusDot tone={isBlockedNow(r) ? "warning" : "neutral"}>{isBlockedNow(r) ? "Closed now" : "Upcoming"}</StatusDot>
                                </li>
                            );
                        })}
                    </ul>
                </Panel>
            )}

            <Drawer open={Boolean(assigning)} title="Assign a worker" onClose={() => setAssigning(null)}>
                {assigning && workers.data && (
                    <AssignWorkerForm request={assigning} workers={workers.data}
                                      reporter={`${dir.residentName(assigning.residentId)} · ${dir.residentApartment(assigning.residentId)}`}
                                      onAssign={handleAssign} onCancel={() => setAssigning(null)} />
                )}
            </Drawer>

            <ConfirmDialog open={Boolean(paying)} title="Register payment?" confirmLabel="Mark as paid"
                           onConfirm={handleMarkPaid} onCancel={() => setPaying(null)}>
                {paying && `${dir.apartmentLabel(paying.apartmentId)}, ${formatPeriod(paying.period)}: ${formatMoney(paying.totalAmount)}. Use this for payments made outside the portal.`}
            </ConfirmDialog>
        </>
    );
}
