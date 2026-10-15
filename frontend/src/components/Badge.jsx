export default function Badge({ variant, children }) {
    return <span className={`badge${variant ? ` badge--${variant}` : ""}`}>{children}</span>;
}
