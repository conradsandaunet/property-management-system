import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import Icon from "../components/Icon";
import { useAuth } from "../auth/useAuth";
import { useAsync } from "../hooks/useAsync";
import { getOpenMaintenance } from "../api/maintenance";
import { MAINTENANCE_CHANGED } from "../features/maintenance/labels";
import NotificationsProvider from "../features/notifications/NotificationsProvider";
import NotificationBell from "./NotificationBell";
import UserMenu from "./UserMenu";
import ViewSwitchProvider from "./ViewSwitch";
import { useViewSwitch } from "./viewSwitchContext";
import { BOARD_NAV, HOME_NAV, START_PAGE, WORKER_NAV, isBoardPath } from "./nav";

function Brand() {
    const { role } = useAuth();
    const { pathname } = useLocation();
    const to = isBoardPath(pathname) ? "/board" : role === "BOARD" ? "/home" : START_PAGE[role] ?? "/";
    return (
        <Link className="brand" to={to}>
            <span className="brand__mark" aria-hidden="true"><Icon name="building" size={14} /></span>
            Resident Portal
        </Link>
    );
}

/** Residents and workers: a few pages, so a top bar is enough. */
function TopShell() {
    const { role } = useAuth();
    const switchView = useViewSwitch();
    const items = role === "WORKER" ? WORKER_NAV : HOME_NAV;

    return (
        <>
            <header className="topbar">
                <div className="topbar__inner">
                    <Brand />
                    <nav className="topnav" aria-label="Main">
                        {items.map((item) => (
                            <NavLink key={item.to} className="topnav__link" to={item.to} end={item.end}>
                                {item.label}
                            </NavLink>
                        ))}
                    </nav>
                    <div className="topbar__end">
                        {role === "BOARD" && (
                            <button type="button" className="btn btn--secondary btn--sm" onClick={() => switchView("board")}>
                                Board view
                            </button>
                        )}
                        {role !== "WORKER" && <NotificationBell />}
                        <UserMenu compact />
                    </div>
                </div>
            </header>
            <main className="container view-enter">
                <Outlet />
            </main>
        </>
    );
}

function SidebarLink({ item, count }) {
    return (
        <NavLink className="sidebar__link" to={item.to} end={item.end}>
            <Icon name={item.icon} />
            {item.label}
            {count > 0 && <span className="sidebar__count" aria-label={`${count} unassigned`}>{count}</span>}
        </NavLink>
    );
}

/** Board members: their own home plus the board's tools, grouped in a sidebar. */
function BoardShell() {
    const { token } = useAuth();
    const location = useLocation();
    const [menuOpen, setMenuOpen] = useState(false);
    const switchView = useViewSwitch();
    // Re-read on navigation, so the count drops right after assigning a worker.
    const open = useAsync(() => getOpenMaintenance(token), [token, location.pathname]);
    const unassigned = open.data?.filter((r) => r.status === "SUBMITTED").length ?? 0;
    const reloadOpen = open.reload;

    useEffect(() => {
        window.addEventListener(MAINTENANCE_CHANGED, reloadOpen);
        return () => window.removeEventListener(MAINTENANCE_CHANGED, reloadOpen);
    }, [reloadOpen]);


    return (
        <div className={`shell${menuOpen ? " is-menu-open" : ""}`}>
            {/* On small screens the sidebar slides in; close it once a link is chosen. */}
            <aside className="sidebar" aria-label="Main"
                   onClick={(e) => { if (e.target.closest("a")) setMenuOpen(false); }}>
                <div className="sidebar__head">
                    <Brand />
                </div>
                <nav>
                    <div className="sidebar__group">
                        <div className="sidebar__heading">Board</div>
                        {BOARD_NAV.map((item) => (
                            <SidebarLink key={item.to} item={item} count={item.count ? unassigned : 0} />
                        ))}
                    </div>
                </nav>
                <div className="sidebar__foot">
                    <button type="button" className="sidebar__link" style={{ marginBottom: 6 }}
                            onClick={() => switchView("resident")}>
                        <Icon name="home" /> Your home
                    </button>
                    <UserMenu align="left" direction="up" />
                </div>
            </aside>
            {menuOpen && <div className="overlay" onClick={() => setMenuOpen(false)} />}
            <div className="shell__main">
                <div className="mobilebar">
                    <button type="button" className="btn btn--ghost btn--icon" aria-label="Open menu"
                            onClick={() => setMenuOpen(true)}>
                        <Icon name="menu" />
                    </button>
                    <Brand />
                </div>
                <main className="container view-enter">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}

export default function AppLayout() {
    const { role } = useAuth();
    const { pathname } = useLocation();
    return (
        <NotificationsProvider>
            <ViewSwitchProvider>
                {role === "BOARD" && isBoardPath(pathname) ? <BoardShell /> : <TopShell />}
            </ViewSwitchProvider>
        </NotificationsProvider>
    );
}
