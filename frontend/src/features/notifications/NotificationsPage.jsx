import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import { useNotifications } from "./useNotifications";

function formatDateTime(iso) {
    return new Date(iso).toLocaleString("en-GB", {
        day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit",
    });
}

export default function NotificationsPage() {
    const { items, loading, error, markRead } = useNotifications();
    const [saveError, setSaveError] = useState("");

    async function handleMarkRead(id) {
        setSaveError("");
        try {
            await markRead(id);
        } catch {
            setSaveError("Couldn't mark the notification as read.");
        }
    }

    return (
        <>
            <PageHeader eyebrow="Account" title="Notifications" />

            {(error || saveError) && (
                <Notice kind="error">{saveError || "Couldn't load your notifications."}</Notice>
            )}
            {loading && !error && <Notice>Loading...</Notice>}
            {!loading && !error && items.length === 0 && <Notice>You have no notifications.</Notice>}

            {items.length > 0 && (
                <ul className="list list--wide">
                    {items.map((n) => (
                        <li key={n.id} className="list-item">
                            <div className="list-item__top">
                                <span className="list-item__title">{n.message}</span>
                                {!n.read && <Badge variant="info">New</Badge>}
                            </div>
                            <div className="list-item__meta">
                                <span>{formatDateTime(n.createdAt)}</span>
                            </div>
                            {!n.read && (
                                <div className="list-item__actions">
                                    <button className="btn-link" type="button" onClick={() => handleMarkRead(n.id)}>
                                        Mark as read
                                    </button>
                                </div>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
