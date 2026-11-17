import { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../../auth/useAuth";
import { getMyNotifications, markNotificationRead } from "../../api/notifications";
import { NotificationsContext } from "./notificationsContext";

const POLL_MS = 30_000;

// Workers have no resident record, so the notification endpoints are closed to them.
const ROLES_WITH_NOTIFICATIONS = ["RESIDENT", "BOARD"];

export default function NotificationsProvider({ children }) {
    const { token, role } = useAuth();
    const enabled = ROLES_WITH_NOTIFICATIONS.includes(role);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(false);

    const reload = useCallback(async () => {
        if (!enabled) return;
        try {
            setItems(await getMyNotifications(token));
            setError(false);
        } catch {
            setError(true);
        } finally {
            setLoading(false);
        }
    }, [token, enabled]);

    useEffect(() => {
        if (!enabled) return;
        reload();
        const id = setInterval(reload, POLL_MS);
        return () => clearInterval(id);
    }, [enabled, reload]);

    const markRead = useCallback(
        async (id) => {
            const updated = await markNotificationRead(token, id);
            setItems((current) => current.map((n) => (n.id === id ? updated : n)));
        },
        [token],
    );

    const value = useMemo(
        () => ({
            items,
            loading: enabled && loading,
            error,
            unread: items.filter((n) => !n.read).length,
            reload,
            markRead,
        }),
        [items, enabled, loading, error, reload, markRead],
    );

    return <NotificationsContext.Provider value={value}>{children}</NotificationsContext.Provider>;
}
