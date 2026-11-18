// Dates from the backend are either "2026-11-20" (LocalDate) or "2026-11-20T08:00:00"
// (LocalDateTime, no zone). Both are read as local time; new Date("2026-11-20") would be UTC.
function toLocalDate(iso) {
    const [year, month, day] = iso.slice(0, 10).split("-").map(Number);
    return new Date(year, month - 1, day);
}

export function formatDate(iso) {
    return toLocalDate(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });
}

/** "Today", "Tomorrow", "Yesterday", or e.g. "Thu 20 Nov". */
export function formatRelativeDay(iso) {
    const day = toLocalDate(iso);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diff = Math.round((day - today) / 86_400_000);
    if (diff === 0) return "Today";
    if (diff === 1) return "Tomorrow";
    if (diff === -1) return "Yesterday";
    return day.toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short" });
}

/** Timestamps with a time zone (e.g. notifications) or local date-times. */
export function formatDateTime(iso) {
    return new Date(iso).toLocaleString("en-GB", {
        day: "numeric", month: "short", hour: "2-digit", minute: "2-digit",
    });
}

export function initials(first = "", last = "") {
    return `${first[0] ?? ""}${last[0] ?? ""}`.toUpperCase();
}

/** "102 · Solsiden Borettslag" */
export function apartmentLabel(apartment) {
    if (!apartment) return "";
    return apartment.buildingName ? `${apartment.apartmentNumber} · ${apartment.buildingName}` : apartment.apartmentNumber;
}
