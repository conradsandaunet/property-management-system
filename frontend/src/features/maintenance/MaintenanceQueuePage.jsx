import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Priority, RequestStatus } from "../../components/Indicators";
import Drawer from "../../components/Drawer";
import EmptyState from "../../components/EmptyState";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useDirectory } from "../../hooks/useDirectory";
import { useAuth } from "../../auth/useAuth";
import { assignWorker, getOpenMaintenance, getWorkers } from "../../api/maintenance";
import { apartmentLabel, formatDate } from "../../lib/format";
import AssignWorkerForm from "./AssignWorkerForm";
import CommentThread from "./CommentThread";
import RequestSteps from "./RequestSteps";
import { PRIORITY_RANK, requestTitle, notifyMaintenanceChanged } from "./labels";

const FILTERS = [
    { value: "", label: "All open" },
    { value: "SUBMITTED", label: "Needs a worker" },
    { value: "ASSIGNED", label: "Assigned" },
    { value: "IN_PROGRESS", label: "In progress" },
];

export default function MaintenanceQueuePage() {
    const { token } = useAuth();
    const toast = useToast();
    const dir = useDirectory(token);
    const [params, setParams] = useSearchParams();
    const filter = params.get("status") ?? "";
    const requests = useAsync(() => getOpenMaintenance(token), [token]);
    const workers = useAsync(() => getWorkers(token), [token]);
    const [assigning, setAssigning] = useState(null);
    const [viewing, setViewing] = useState(null);

    async function handleAssign(workerId) {
        try {
            await assignWorker(token, assigning.id, workerId);
            toast(assigning.assignedWorkerId ? "Worker changed" : "Worker assigned");
            setAssigning(null);
            requests.reload();
            notifyMaintenanceChanged();
        } catch {
            toast("Couldn't assign the worker", { kind: "error" });
        }
    }

    // The request stores its own apartment. Older requests don't, so fall back to the directory.
    const apartmentOf = (r) => (r.apartmentNumber ? apartmentLabel(r) : dir.residentApartment(r.residentId));

    const all = (requests.data ?? [])
        .slice()
        .sort((a, b) => PRIORITY_RANK[a.priority] - PRIORITY_RANK[b.priority] || a.createdAt.localeCompare(b.createdAt));
    const count = (status) => all.filter((r) => !status || r.status === status).length;
    const visible = all.filter((r) => !filter || r.status === filter);

    return (
        <>
            <PageHeader title="Maintenance" subtitle="Open requests from residents, most urgent first." />

            <div className="toolbar">
                <div className="chips" role="group" aria-label="Filter by status">
                    {FILTERS.map((f) => (
                        <button key={f.value} type="button" className={`chip${filter === f.value ? " is-active" : ""}`}
                                aria-pressed={filter === f.value}
                                onClick={() => setParams(f.value ? { status: f.value } : {}, { replace: true })}>
                            {f.label} <span className="chip__count">{count(f.value)}</span>
                        </button>
                    ))}
                </div>
            </div>

            {(requests.error || workers.error) && <Notice kind="error">Couldn't load the maintenance requests.</Notice>}
            {requests.loading && <Skeleton rows={4} />}
            {requests.data && visible.length === 0 && (
                <EmptyState title={filter ? "Nothing here" : "No open requests"}>
                    {filter ? "No requests with this status." : "When residents report a problem, it shows up here."}
                </EmptyState>
            )}

            {visible.length > 0 && (
                <div className="table-wrap">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>Priority</th>
                                <th>Request</th>
                                <th className="hide-sm">Reported by</th>
                                <th className="hide-sm">Reported</th>
                                <th>Worker</th>
                                <th>Status</th>
                                <th className="actions"><span className="sr-only">Actions</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {visible.map((r) => (
                                <tr key={r.id} className="is-clickable" onClick={() => setViewing(r)}>
                                    <td><Priority priority={r.priority} /></td>
                                    <td style={{ maxWidth: 320 }}>
                                        <div className="table__primary">{requestTitle(r)}</div>
                                        <div className="table__sub" style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
                                             title={r.description}>
                                            {r.description}
                                        </div>
                                    </td>
                                    <td className="hide-sm">
                                        <div style={{ whiteSpace: "nowrap" }}>{dir.residentName(r.residentId)}</div>
                                        <div className="table__sub">{apartmentOf(r)}</div>
                                    </td>
                                    <td className="hide-sm muted" style={{ whiteSpace: "nowrap" }}>{formatDate(r.createdAt)}</td>
                                    <td style={{ whiteSpace: "nowrap" }}>{r.assignedWorkerName ?? <span className="muted">Unassigned</span>}</td>
                                    <td><RequestStatus status={r.status} /></td>
                                    <td className="actions">
                                        <button type="button" onClick={(e) => { e.stopPropagation(); setAssigning(r); }}
                                                className={`btn btn--sm${r.assignedWorkerId ? " btn--ghost" : ""}`}>
                                            {r.assignedWorkerId ? "Reassign" : "Assign"}
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Drawer open={Boolean(viewing)} title={viewing ? requestTitle(viewing) : ""} onClose={() => setViewing(null)}>
                {viewing && (
                    <>
                        <div style={{ display: "flex", gap: 16, marginBottom: 10 }}>
                            <Priority priority={viewing.priority} suffix=" priority" />
                            <RequestStatus status={viewing.status} />
                        </div>
                        <p>{viewing.description}</p>
                        <p className="small muted" style={{ marginTop: 8 }}>
                            Reported by {dir.residentName(viewing.residentId)} · {apartmentOf(viewing)} · {formatDate(viewing.createdAt)}
                        </p>
                        <p className="small muted">
                            {viewing.assignedWorkerName ? `Worker: ${viewing.assignedWorkerName}` : "No worker assigned yet"}
                        </p>
                        <RequestSteps status={viewing.status} />
                        <div className="form-actions">
                            <button type="button" className="btn" onClick={() => { setAssigning(viewing); setViewing(null); }}>
                                {viewing.assignedWorkerId ? "Reassign" : "Assign a worker"}
                            </button>
                        </div>
                        <h3 className="section__title" style={{ marginTop: 20 }}>Comments</h3>
                        <p className="hint">Between the resident and the worker. The board can read them.</p>
                        <CommentThread requestId={viewing.id} viewer="board" canWrite={false} />
                    </>
                )}
            </Drawer>

            <Drawer open={Boolean(assigning)} title={assigning?.assignedWorkerId ? "Change worker" : "Assign a worker"}
                    onClose={() => setAssigning(null)}>
                {assigning && workers.data && (
                    <AssignWorkerForm request={assigning} workers={workers.data}
                                      reporter={`${dir.residentName(assigning.residentId)} · ${apartmentOf(assigning)}`}
                                      onAssign={handleAssign} onCancel={() => setAssigning(null)} />
                )}
            </Drawer>
        </>
    );
}
