import { Link } from "react-router-dom";

/** A number with a label. Pass `to` to make it a link, or `onClick` to make it a filter button. */
export default function StatCard({ label, value, hint, tone, to, onClick, active = false }) {
    const className = `stat${tone ? ` stat--${tone}` : ""}${active ? " is-active" : ""}`;
    const content = (
        <>
            <div className="stat__label">{label}</div>
            <div className="stat__value">{value}</div>
            {hint && <div className="stat__hint">{hint}</div>}
        </>
    );
    if (to) return <Link className={className} to={to}>{content}</Link>;
    if (onClick) {
        return (
            <button type="button" className={className} onClick={onClick} aria-pressed={active}>
                {content}
            </button>
        );
    }
    return <div className={className}>{content}</div>;
}
