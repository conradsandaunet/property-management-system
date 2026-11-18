import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { RequestStatus } from "../../components/Indicators";
import Drawer from "../../components/Drawer";
import EmptyState from "../../components/EmptyState";
import Icon from "../../components/Icon";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { createMaintenance, getMyMaintenance } from "../../api/maintenance";
import { formatDate } from "../../lib/format";
import { PRIORITY_LABELS, requestTitle } from "./labels";
import NewRequestForm from "./NewRequestForm";
import RequestSteps from "./RequestSteps";
import CommentThread from "./CommentThread";

export default function MyRequestsPage() {
    const { token } = useAuth();
    const toast = useToast();
    const [params, setParams] = useSearchParams();
    const requests = useAsync(() => getMyMaintenance(token), [token]);
    const formOpen = params.get("new") === "1";
    const [openComments, setOpenComments] = useState(null);

    const setFormOpen = (open) => setParams(open ? { new: "1" } : {}, { replace: true });

    async function handleCreate(values) {
        await createMaintenance(token, values);
        setFormOpen(false);
        toast("Request sent. The board will assign someone.");
        requests.reload();
    }

    const open = (requests.data ?? []).filter((r) => r.status !== "RESOLVED");
    const resolved = (requests.data ?? []).filter((r) => r.status === "RESOLVED");
    const reportButton = (
        <button className="btn" type="button" onClick={() => setFormOpen(true)}>
            <Icon name="plus" size={16} /> Report an issue
        </button>
    );

    return (
        <>
            <PageHeader title="Maintenance" subtitle="Report problems in your apartment and follow them up." actions={reportButton} />

            {requests.error && <Notice kind="error">Couldn't load your requests.</Notice>}
            {requests.loading && <Skeleton rows={3} />}

            {requests.data && requests.data.length === 0 && (
                <EmptyState title="No requests yet" action={reportButton}>
                    Something broken or not working? Report it and follow the progress here.
                </EmptyState>
            )}

            {open.length > 0 && (
                <section className="section">
                    <div className="section__head"><h2 className="section__title">Open</h2></div>
                    <div className="grid" style={{ gridTemplateColumns: "1fr" }}>
                        {open.map((r) => (
                            <article key={r.id} className="card">
                                <div className="section__head" style={{ marginBottom: 4 }}>
                                    <h3 className="item__title">{requestTitle(r)}</h3>
                                    <RequestStatus status={r.status} />
                                </div>
                                <p>{r.description}</p>
                                <p className="small muted" style={{ marginTop: 6 }}>
                                    Reported {formatDate(r.createdAt)} · {PRIORITY_LABELS[r.priority]} priority
                                    {r.assignedWorkerName && ` · ${r.assignedWorkerName} is on it`}
                                </p>
                                <RequestSteps status={r.status} />
                                <div style={{ marginTop: 10 }}>
                                    <button type="button" className="btn-link" aria-expanded={openComments === r.id}
                                            onClick={() => setOpenComments(openComments === r.id ? null : r.id)}>
                                        {openComments === r.id ? "Hide comments" : "Comments"}
                                    </button>
                                </div>
                                {openComments === r.id && <CommentThread requestId={r.id} viewer="resident" canWrite />}
                            </article>
                        ))}
                    </div>
                </section>
            )}

            {resolved.length > 0 && (
                <section className="section">
                    <div className="section__head"><h2 className="section__title">Resolved</h2></div>
                    <ul className="item-list card card--flush">
                        {resolved.map((r) => (
                            <li key={r.id} className="item">
                                <div className="item__body">
                                    <div className="item__title">{requestTitle(r)}</div>
                                    <div className="item__meta">
                                        <span>Reported {formatDate(r.createdAt)}</span>
                                        {r.assignedWorkerName && <span>Fixed by {r.assignedWorkerName}</span>}
                                    </div>
                                </div>
                                <RequestStatus status="RESOLVED" />
                            </li>
                        ))}
                    </ul>
                </section>
            )}

            <Drawer open={formOpen} title="Report an issue" onClose={() => setFormOpen(false)}>
                <NewRequestForm onSubmit={handleCreate} onCancel={() => setFormOpen(false)} />
            </Drawer>
        </>
    );
}
