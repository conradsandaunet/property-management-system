// Pages for a resident's own home. Board members live in the building too and reach these through
// "Switch to resident view".
export const HOME_NAV = [
    { label: "Home", to: "/home", icon: "home" },
    { label: "Maintenance", to: "/maintenance", icon: "tool" },
    { label: "Invoices", to: "/invoices", icon: "receipt" },
    { label: "Bookings", to: "/bookings", icon: "calendar" },
];

export const BOARD_NAV = [
    { label: "Overview", to: "/board", icon: "overview", end: true },
    { label: "Property", to: "/board/property", icon: "building" },
    { label: "Maintenance", to: "/board/maintenance", icon: "tool", count: "unassigned" },
    { label: "Billing", to: "/board/billing", icon: "cash" },
    { label: "Resources", to: "/board/resources", icon: "parking" },
];

export const WORKER_NAV = [
    { label: "Work orders", to: "/work-orders", icon: "clipboard" },
];

export const ROLE_LABELS = { BOARD: "Board member", WORKER: "Maintenance worker", RESIDENT: "Resident" };

/** Where each role starts after logging in. */
export const START_PAGE = { BOARD: "/board", RESIDENT: "/home", WORKER: "/work-orders" };

/** The board view is everything under /board; every other page is the resident view. */
export function isBoardPath(pathname) {
    return pathname === "/board" || pathname.startsWith("/board/");
}
