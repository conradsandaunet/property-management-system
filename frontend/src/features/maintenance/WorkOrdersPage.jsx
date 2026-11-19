import { useState } from "react";
import { Priority } from "../../components/Indicators";
import EmptyState from "../../components/EmptyState";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { changeMaintenanceStatus, getAssignedMaintenance } from "../../api/maintenance";
import { apartmentLabel, formatDate } from "../../lib/format";
import { PRIORITY_RANK, requestTitle } from "./labels";
import RequestSteps from "./RequestSteps";
import CommentThread from "./CommentThread";

// The only step a worker can take from each status (same rules as the backend).
const NEXT_STEP = {
    ASSIGNED: { status: "IN_PROGRESS", label: "Start work", done: "Work started" },
    IN_PROGRESS: { status: "RESOLVED", label: "Mark as done", done: "Marked as done. The resident gets a notification." },
};

const GROUPS = [
    { status: "IN_PROGRESS", title: "In progress" },
    { status: "ASSIGNED", title: "To do" },
];

export default function WorkOrdersPage() {
    const { token, me } = useAuth();
    const toast = useToast();
    const orders = useAsync(() => getAssignedMaintenance(token), [token]);
    const [savingId, setSavingId] = useState(null);
    const [openComments, setOpenComments] = useState(null);

    async function advance(order) {
        const step = NEXT_STEP[order.status];
        setSavingId(order.id);
        try {
            await changeMaintenanceStatus(token, order.id, step.status);
            toast(step.done);
        } catch (e) {
            toast(e.status === 409 ? "That change isn't allowed any more" : "Couldn't update the status", { kind: "error" });
        } finally {
            setSavingId(null);
            orders.reload();
        }
    }

    const sorted = (orders.data ?? []).slice()
        .sort((a, b) => PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority] || a.createdAt.localeCompare(b.createdAt));

    return (
        <>
            <PageHeader title="Work orders" subtitle={`Requests assigned to ${me.email}, most urgent first.`} />

            {orders.error && <Notice kind="error">Couldn't load your work orders.</Notice>}
            {orders.loading && !orders.data && <Skeleton rows={3} />}
            {orders.data && sorted.length === 0 && (
                <EmptyState title="Nothing assigned to you">New work orders show up here when the board assigns them.</EmptyState>
            )}

            {GROUPS.map((group) => {
                const list = sorted.filter((o) => o.status === group.status);
                if (list.length === 0) return null;
                return (
                    <section key={group.status} className="section">
                        <div className="section__head">
                            <h2 className="section__title">{group.title} <span className="muted">({list.length})</span></h2>
                        </div>
                        <div className="grid" style={{ gridTemplateColumns: "1fr" }}>
                            {list.map((o) => {
                                const step = NEXT_STEP[o.status];
                                return (
                                    <article key={o.id} className="card">
                                        <div className="section__head" style={{ marginBottom: 4 }}>
                                            <h3 className="item__title">{requestTitle(o)}</h3>
                                            <Priority priority={o.priority} suffix=" priority" />
                                        </div>
                                        <p>{o.description}</p>
                                        <p className="small muted" style={{ marginTop: 6 }}>
                                            {o.apartmentNumber ? `Apartment ${apartmentLabel(o)}` : "Apartment not recorded"} · Reported {formatDate(o.createdAt)}
                                        </p>
                                        <RequestSteps status={o.status} />
                                        <div className="form-actions" style={{ marginTop: 14 }}>
                                            {step && (
                                                <button className="btn" type="button" disabled={savingId === o.id} onClick={() => advance(o)}>
                                                    {savingId === o.id ? "Saving…" : step.label}
                                                </button>
                                            )}
                                            <button type="button" className="btn btn--ghost" aria-expanded={openComments === o.id}
                                                    onClick={() => setOpenComments(openComments === o.id ? null : o.id)}>
                                                {openComments === o.id ? "Hide comments" : "Comments"}
                                            </button>
                                        </div>
                                        {openComments === o.id && <CommentThread requestId={o.id} viewer="worker" canWrite />}
                                    </article>
                                );
                            })}
                        </div>
                    </section>
                );
            })}
        </>
    );
}
