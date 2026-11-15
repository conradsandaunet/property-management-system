import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { changeMaintenanceStatus, getAssignedMaintenance } from "../../api/maintenance";

const STATUS_LABELS = {
    SUBMITTED: "Submitted",
    ASSIGNED: "Assigned",
    IN_PROGRESS: "In progress",
    RESOLVED: "Resolved",
};

const STATUS_VARIANT = {
    ASSIGNED: "info",
    IN_PROGRESS: "warning",
    RESOLVED: "success",
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

// The only transition a worker can make from each status (same rules as the backend).
const NEXT_STEP = {
    ASSIGNED: { status: "IN_PROGRESS", label: "Start work" },
    IN_PROGRESS: { status: "RESOLVED", label: "Mark as done" },
};

function formatDate(iso) {
    return new Date(iso).toLocaleDateString("en-GB", {
        day: "numeric", month: "short", year: "numeric",
    });
}

export default function WorkOrdersPage() {
    const { token } = useAuth();
    const orders = useAsync(() => getAssignedMaintenance(token), [token]);
    const [error, setError] = useState("");
    const [savingId, setSavingId] = useState(null);

    async function handleAdvance(order) {
        const step = NEXT_STEP[order.status];
        if (!step) return;
        setSavingId(order.id);
        setError("");
        try {
            await changeMaintenanceStatus(token, order.id, step.status);
            orders.reload();
        } catch (e) {
            setError(
                e.status === 409
                    ? "That status change isn't allowed. Reloading the list."
                    : "Couldn't update the status.",
            );
            orders.reload();
        } finally {
            setSavingId(null);
        }
    }

    return (
        <>
            <PageHeader eyebrow="Maintenance" title="My work orders" />

            {error && <Notice kind="error">{error}</Notice>}
            {orders.error && <Notice kind="error">Couldn't load your work orders.</Notice>}
            {orders.loading && !orders.data && !orders.error && <Notice>Loading...</Notice>}
            {orders.data && orders.data.length === 0 && <Notice>No work orders assigned to you.</Notice>}

            {orders.data && orders.data.length > 0 && (
                <ul className="list list--wide">
                    {orders.data.map((o) => {
                        const step = NEXT_STEP[o.status];
                        return (
                            <li key={o.id} className="list-item">
                                <div className="list-item__top">
                                    <span className="list-item__title">
                                        {CATEGORY_LABELS[o.category] ?? o.category} · {o.location}
                                    </span>
                                    <span className="badges">
                                        <Badge variant={PRIORITY_VARIANT[o.priority]}>
                                            {PRIORITY_LABELS[o.priority] ?? o.priority}
                                        </Badge>
                                        <Badge variant={STATUS_VARIANT[o.status]}>
                                            {STATUS_LABELS[o.status] ?? o.status}
                                        </Badge>
                                    </span>
                                </div>
                                <p>{o.description}</p>
                                <div className="list-item__meta">
                                    <span>Reported {formatDate(o.createdAt)}</span>
                                </div>
                                {step && (
                                    <div className="list-item__actions">
                                        <button
                                            className="btn"
                                            type="button"
                                            disabled={savingId === o.id}
                                            onClick={() => handleAdvance(o)}
                                        >
                                            {step.label}
                                        </button>
                                    </div>
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
        </>
    );
}
