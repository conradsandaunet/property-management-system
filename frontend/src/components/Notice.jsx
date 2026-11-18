export default function Notice({ kind = "muted", children }) {
    return <p className={`notice notice--${kind}`} role={kind === "error" ? "alert" : undefined}>{children}</p>;
}
