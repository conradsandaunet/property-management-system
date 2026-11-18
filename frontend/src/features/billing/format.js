export const STATUS_LABELS = {
    PENDING: "Pending",
    PAID: "Paid",
    OVERDUE: "Overdue",
};

export const STATUS_TONE = {
    PENDING: "info",
    PAID: "success",
    OVERDUE: "warning",
};

export const CATEGORY_LABELS = {
    SHARED_COST: "Shared costs",
    WATER: "Water",
    ELECTRICITY: "Electricity",
    PARKING: "Parking",
    OTHER: "Other",
};

const money = new Intl.NumberFormat("nb-NO", { style: "currency", currency: "NOK" });

export function formatMoney(amount) {
    return money.format(amount);
}

// "2026-10-15" -> "15 Oct 2026". Parsed as a local date; new Date("2026-10-15") would be UTC.
export function formatDate(isoDate) {
    const [year, month, day] = isoDate.split("-").map(Number);
    return new Date(year, month - 1, day).toLocaleDateString("en-GB", {
        day: "numeric", month: "short", year: "numeric",
    });
}

// "2026-11" -> "November 2026"
export function formatPeriod(period) {
    const [year, month] = period.split("-").map(Number);
    return new Date(year, month - 1, 1).toLocaleDateString("en-GB", { month: "long", year: "numeric" });
}

// Current month as "YYYY-MM", in local time. `offset` moves it, e.g. -1 for last month.
export function currentPeriod(offset = 0) {
    const d = new Date();
    d.setDate(1);
    d.setMonth(d.getMonth() + offset);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

// Saves the PDF through a temporary link. window.open() after an await is often stopped by popup blockers.
export function downloadPdf(blob, filename) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
