export default function EmptyState({ title, children, action, compact = false }) {
    return (
        <div className={`empty${compact ? " empty--compact" : ""}`}>
            {title && <p className="empty__title">{title}</p>}
            {children && <p>{children}</p>}
            {action}
        </div>
    );
}
