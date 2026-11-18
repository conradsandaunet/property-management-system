import EmptyState from "../../components/EmptyState";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { formatDateTime } from "../../lib/format";
import { useNotifications } from "./useNotifications";

export default function NotificationsPage() {
    const { items, loading, error, unread, markRead } = useNotifications();
    const toast = useToast();

    async function markAllRead() {
        try {
            await Promise.all(items.filter((n) => !n.read).map((n) => markRead(n.id)));
        } catch {
            toast("Couldn't mark everything as read", { kind: "error" });
        }
    }

    return (
        <>
            <PageHeader
                title="Notifications"
                subtitle="Updates about your maintenance requests."
                actions={unread > 0 && (
                    <button type="button" className="btn btn--secondary" onClick={markAllRead}>Mark all as read</button>
                )}
            />

            {error && <Notice kind="error">Couldn't load your notifications.</Notice>}
            {loading && !error && <Skeleton rows={3} />}
            {!loading && !error && items.length === 0 && (
                <EmptyState title="You're all caught up">
                    You'll get a notification here when a worker completes one of your requests.
                </EmptyState>
            )}

            {items.length > 0 && (
                <ul className="item-list card card--flush" style={{ maxWidth: "var(--w-page)" }}>
                    {items.map((n) => (
                        <li key={n.id} className="item" style={n.read ? undefined : { background: "var(--blue-pale)" }}>
                            <div className="item__body">
                                <div style={{ fontWeight: n.read ? 400 : 500 }}>{n.message}</div>
                                <div className="item__meta">{formatDateTime(n.createdAt)}</div>
                            </div>
                            {!n.read && (
                                <button type="button" className="btn btn--ghost btn--sm"
                                        onClick={() => markRead(n.id).catch(() => toast("Couldn't update", { kind: "error" }))}>
                                    Mark as read
                                </button>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
