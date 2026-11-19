/** A short message. With `onRetry`, it gets a "Try again" button (for data that failed to load). */
export default function Notice({ kind = "muted", onRetry, children }) {
    return (
        <p className={`notice notice--${kind}`} role={kind === "error" ? "alert" : undefined}>
            {children}
            {onRetry && (
                <>
                    {" "}
                    <button type="button" className="btn-link" onClick={onRetry}>Try again</button>
                </>
            )}
        </p>
    );
}
