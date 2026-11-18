/** Placeholder rows while data loads. */
export default function Skeleton({ rows = 3 }) {
    return (
        <div className="skeleton" aria-busy="true" aria-label="Loading">
            {Array.from({ length: rows }, (_, i) => <div key={i} className="skeleton__line" />)}
        </div>
    );
}
