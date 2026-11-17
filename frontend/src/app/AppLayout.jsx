import { Outlet } from "react-router-dom";
import NavBar from "./NavBar";
import NotificationsProvider from "../features/notifications/NotificationsProvider";

export default function AppLayout() {
    return (
        <NotificationsProvider>
            <div className="app-shell">
                <NavBar />
                <main className="container">
                    <Outlet />
                </main>
            </div>
        </NotificationsProvider>
    );
}
