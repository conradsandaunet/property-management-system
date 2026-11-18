/** Lets the board sidebar refresh its "unassigned" count right after an assignment. */
export const MAINTENANCE_CHANGED = "maintenance:changed";

export function notifyMaintenanceChanged() {
    window.dispatchEvent(new Event(MAINTENANCE_CHANGED));
}

export const STATUS_LABELS = {
    SUBMITTED: "Submitted",
    ASSIGNED: "Assigned",
    IN_PROGRESS: "In progress",
    RESOLVED: "Resolved",
};

export const STATUS_ORDER = ["SUBMITTED", "ASSIGNED", "IN_PROGRESS", "RESOLVED"];

export const CATEGORY_LABELS = {
    PLUMBING: "Plumbing",
    ELECTRICAL: "Electrical",
    HEATING: "Heating",
    APPLIANCE: "Appliance",
    OTHER: "Other",
};

export const PRIORITY_LABELS = {
    LOW: "Low",
    MEDIUM: "Medium",
    HIGH: "High",
};

export const PRIORITY_RANK = { HIGH: 0, MEDIUM: 1, LOW: 2 };

export function requestTitle(r) {
    return `${CATEGORY_LABELS[r.category] ?? r.category} · ${r.location}`;
}
