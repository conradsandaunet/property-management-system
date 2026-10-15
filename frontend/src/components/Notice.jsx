export default function Notice({ kind = "muted", children }) {
    return <p className={`notice notice--${kind}`}>{children}</p>;
}
