import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { assignWorker, getOpenMaintenance, getWorkers } from "../../api/maintenance";

const STATUS_LABELS = {
    SUBMITTED: "Submitted",
    ASSIGNED: "Assigned",
    IN_PROGRESS: "In progress",
    RESOLVED: "Resolved",
};

const CATEGORY_LABELS = {
    PLUMBING: "Plumbing",
    ELECTRICAL: "Electrical",
    HEATING: "Heating",
    APPLIANCE: "Appliance",
    OTHER: "Other",
};

const PRIORITY_LABELS = {
    LOW: "Low",
    MEDIUM: "Medium",
    HIGH: "High",
};

const PRIORITY_VARIANT = {
    HIGH: "warning",
    MEDIUM: "info",
    LOW: undefined,
};

function formatDate(iso) {
    return new Date(iso).toLocaleDateString("en-GB", {
        day: "numeric", month: "short", year: "numeric",
    });
}

function splitWorkers(workers, category) {
    return {
        matching: workers.filter((w) => w.specialties.includes(category)),
        others: workers.filter((w) => !w.specialties.includes(category)),
    };
}

export default function OpenRequestPage() {
    const { token } = useAuth();
    const requests = useAsync(() => getOpenMaintenance(token), [token]);
    const workers = useAsync(() => getWorkers(token), [token]);
    const [error, setError] = useState("");
    const [savingId, setSavingId] = useState(null);

    async function handleAssign(requestId, workerId) {
        if (!workerId) return;
        setSavingId(requestId);
        setError("");
        try {
            await assignWorker(token, requestId, Number(workerId));
            requests.reload();
        } catch {
            setError("Couldn't assign the worker.");
        } finally {
            setSavingId(null);
        }
    }

    const loadError = requests.error || workers.error;

    return (
        <>
            <PageHeader eyebrow="Board" title="Open requests" />

            {(error || loadError) && (
                <Notice kind="error">{error || "Couldn't load maintenance requests."}</Notice>
            )}
            {(requests.loading || workers.loading) && !loadError && <Notice>Loading...</Notice>}
            {requests.data && requests.data.length === 0 && <Notice>No open requests.</Notice>}

            {requests.data && workers.data && requests.data.length > 0 && (
                <ul className="list list--wide">
                    {requests.data.map((r) => (
                        <li key={r.id} className="list-item">
                            <div className="list-item__top">
                                <span className="list-item__title">
                                    {CATEGORY_LABELS[r.category] ?? r.category} · {r.location}
                                </span>
                                <span className="badges">
                                    <Badge variant={PRIORITY_VARIANT[r.priority]}>
                                        {PRIORITY_LABELS[r.priority] ?? r.priority}
                                    </Badge>
                                    <Badge variant={r.status === "SUBMITTED" ? undefined : "success"}>
                                        {STATUS_LABELS[r.status] ?? r.status}
                                    </Badge>
                                </span>
                            </div>
                            <p>{r.description}</p>
                            <div className="list-item__meta">
                                <span>{formatDate(r.createdAt)}</span>
                                <span>
                                    {r.assignedWorkerName
                                        ? `Assigned to ${r.assignedWorkerName}`
                                        : "Unassigned"}
                                </span>
                            </div>
                            <div className="list-item__actions">
                                <select
                                    className="input"
                                    aria-label="Assign worker"
                                    value=""
                                    disabled={savingId === r.id}
                                    onChange={(e) => handleAssign(r.id, e.target.value)}
                                >
                                    <option value="">
                                        {r.assignedWorkerName ? "Reassign..." : "Assign worker..."}
                                    </option>
                                    {(() => {
                                        const { matching, others } = splitWorkers(workers.data, r.category);
                                        const categoryLabel = CATEGORY_LABELS[r.category] ?? r.category;
                                        return (
                                            <>
                                                {matching.length > 0 && (
                                                    <optgroup label={`Handles ${categoryLabel.toLowerCase()}`}>
                                                        {matching.map((w) => (
                                                            <option key={w.id} value={w.id}>{w.name}</option>
                                                        ))}
                                                    </optgroup>
                                                )}
                                                {others.length > 0 && (
                                                    <optgroup label="Other workers">
                                                        {others.map((w) => (
                                                            <option key={w.id} value={w.id}>{w.name}</option>
                                                        ))}
                                                    </optgroup>
                                                )}
                                            </>
                                        );
                                    })()}
                                </select>
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}