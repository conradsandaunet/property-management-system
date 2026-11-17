export const TYPE_LABELS = {
    PARKING: "Guest parking",
    COMMON_ROOM: "Common room",
};

// Today as "YYYY-MM-DD", in local time (toISOString() would give UTC and can be off by a day).
export function today() {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

// The backend sends LocalDateTime without a time zone, e.g. "2026-11-20T08:00:00".
export function formatTime(iso) {
    return iso.slice(11, 16);
}

// Accepts "2026-11-20" or "2026-11-20T08:00:00"; only the date part is used, read as a local date.
export function formatDay(iso) {
    const [year, month, day] = iso.slice(0, 10).split("-").map(Number);
    return new Date(year, month - 1, day).toLocaleDateString("en-GB", {
        weekday: "short", day: "numeric", month: "short", year: "numeric",
    });
}

export function formatRange(start, end) {
    return `${formatDay(start)} · ${formatTime(start)}–${formatTime(end)}`;
}

export function isBlockedNow(resource) {
    if (resource.status === "MAINTENANCE") return true;
    if (!resource.maintenanceFrom || !resource.maintenanceTo) return false;
    const now = new Date();
    return new Date(resource.maintenanceFrom) <= now && now < new Date(resource.maintenanceTo);
}
