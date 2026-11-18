import { useNavigate } from "react-router-dom";
import Icon from "../components/Icon";
import Menu from "../components/Menu";
import { useNotifications } from "../features/notifications/useNotifications";
import { formatDateTime } from "../lib/format";

const SHOWN = 5;

export default function NotificationBell({ align = "right" }) {
    const { items, unread, markRead } = useNotifications();
    const navigate = useNavigate();

    function open(n) {
        if (!n.read) markRead(n.id).catch(() => {});
    }

    return (
        <Menu
            align={align}
            trigger={({ open: isOpen, toggle }) => (
                <button type="button" className="btn btn--ghost btn--icon bell" onClick={toggle}
                        aria-haspopup="menu" aria-expanded={isOpen}
                        aria-label={unread > 0 ? `Notifications, ${unread} unread` : "Notifications"}>
                    <Icon name="bell" />
                    {unread > 0 && <span className="bell__count" aria-hidden="true">{unread}</span>}
                </button>
            )}
            items={[]}
            panel={({ close }) => (
                <div className="bell__panel">
                    <div className="bell__head">
                        Notifications
                        <button type="button" className="btn-link"
                                onClick={() => { close(); navigate("/notifications"); }}>
                            See all
                        </button>
                    </div>
                    {items.length === 0 && <p className="bell__empty">You're all caught up.</p>}
                    {items.slice(0, SHOWN).map((n) => (
                        <button key={n.id} type="button" className={`bell__item${n.read ? "" : " is-unread"}`}
                                onClick={() => open(n)}>
                            <div>{n.message}</div>
                            <div className="small muted">{formatDateTime(n.createdAt)}</div>
                        </button>
                    ))}
                </div>
            )}
        />
    );
}
