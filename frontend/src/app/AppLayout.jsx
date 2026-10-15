import { Outlet } from "react-router-dom";
import NavBar from "./NavBar";

export default function AppLayout() {
    return (
        <div className="app-shell">
            <NavBar />
            <main className="container">
                <Outlet />
            </main>
        </div>
    );
}
