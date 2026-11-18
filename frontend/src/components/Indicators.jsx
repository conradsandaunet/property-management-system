// Quiet status indicators in the style of Linear, GitHub and Vercel: the colour lives in a small
// icon or dot, the label stays plain text.

const PRIORITY_BARS = { LOW: 1, MEDIUM: 2, HIGH: 3 };
const PRIORITY_TEXT = { LOW: "Low", MEDIUM: "Medium", HIGH: "High" };

/** Signal bars: one, two or three filled. */
export function Priority({ priority, suffix = "" }) {
    const filled = PRIORITY_BARS[priority] ?? 0;
    return (
        <span className={`indicator priority priority--${priority?.toLowerCase()}`}>
            <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
                {[0, 1, 2].map((i) => (
                    <rect key={i} x={1 + i * 4.5} y={9 - i * 3.5} width="3" height={4 + i * 3.5} rx="1"
                          className={i < filled ? "priority__bar is-on" : "priority__bar"} />
                ))}
            </svg>
            {PRIORITY_TEXT[priority] ?? priority}{suffix}
        </span>
    );
}

const STATUS_TEXT = { SUBMITTED: "Submitted", ASSIGNED: "Assigned", IN_PROGRESS: "In progress", RESOLVED: "Resolved" };

/** Maintenance request status as a small circle: dashed, open, half full, done. */
export function RequestStatus({ status }) {
    return (
        <span className={`indicator request-status request-status--${status?.toLowerCase().replace("_", "-")}`}>
            <svg width="14" height="14" viewBox="0 0 14 14" aria-hidden="true">
                {status === "SUBMITTED" && (
                    <circle cx="7" cy="7" r="5.5" fill="none" stroke="currentColor" strokeWidth="1.5" strokeDasharray="2.2 2" />
                )}
                {status === "ASSIGNED" && <circle cx="7" cy="7" r="5.5" fill="none" stroke="currentColor" strokeWidth="1.5" />}
                {status === "IN_PROGRESS" && (
                    <>
                        <circle cx="7" cy="7" r="5.5" fill="none" stroke="currentColor" strokeWidth="1.5" />
                        <path d="M7 3.5 A3.5 3.5 0 0 1 7 10.5 Z" fill="currentColor" />
                    </>
                )}
                {status === "RESOLVED" && (
                    <>
                        <circle cx="7" cy="7" r="6.25" fill="currentColor" />
                        <path d="M4.3 7.2 6.2 9 9.7 5.2" fill="none" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    </>
                )}
            </svg>
            {STATUS_TEXT[status] ?? status}
        </span>
    );
}

/** A coloured dot and a label. tone: success, warning, danger, info or neutral. */
export function StatusDot({ tone = "neutral", children }) {
    return (
        <span className={`indicator status-dot status-dot--${tone}`}>
            <span className="status-dot__dot" aria-hidden="true" />
            {children}
        </span>
    );
}
