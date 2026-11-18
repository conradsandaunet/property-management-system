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

// The API sends the scheduled block as `from` / `to` (both null when there is none).
export function isBlockedNow(resource) {
    if (resource.status === "MAINTENANCE") return true;
    if (!resource.from || !resource.to) return false;
    const now = new Date();
    return new Date(resource.from) <= now && now < new Date(resource.to);
}

/** A scheduled block that hasn't ended yet, or null. */
export function upcomingBlock(resource) {
    if (!resource.from || !resource.to || new Date(resource.to) <= new Date()) return null;
    return { from: resource.from, to: resource.to };
}
