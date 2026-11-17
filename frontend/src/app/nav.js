export const NAV_ITEMS = {
    RESIDENT: [
        { label: "Home", to: "/", end: true },
        { label: "Maintenance", to: "/maintenance" },
        { label: "Invoices", to: "/invoices" },
        { label: "Reservations", to: "/reservations" },
        { label: "Notifications", to: "/notifications", unreadCount: true },
        { label: "Account", to: "/account" },
    ],
    BOARD: [
        { label: "Home", to: "/", end: true },
        { label: "Overview", to: "/board/overview" },
        { label: "Residents", to: "/board/residents" },
        { label: "Apartments", to: "/board/apartments" },
        { label: "Maintenance", to: "/board/maintenance" },
        { label: "Invoices", to: "/board/invoices" },
        { label: "Resources", to: "/board/resources" },
        { label: "Notifications", to: "/notifications", unreadCount: true },
    ],
    WORKER: [
        { label: "Work orders", to: "/work-orders" },
    ],
};
