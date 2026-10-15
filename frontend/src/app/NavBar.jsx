import { Link, NavLink } from "react-router-dom";
import Badge from "../components/Badge";
import { useAuth } from "../auth/useAuth";
import { NAV_ITEMS } from "./nav";

export default function NavBar() {
    const { me, role, logout } = useAuth();
    const items = NAV_ITEMS[role] ?? [];

    return (
        <header className="navbar">
            <div className="navbar__inner">
                <Link className="navbar__brand" to="/">
                    <span className="navbar__mark" />
                    <span className="navbar__brand-text">Resident Portal</span>
                </Link>

                <nav className="navbar__links" aria-label="Main">
                    {items.map((item) => (
                        <NavLink key={item.to} className="navbar__link" to={item.to} end={item.end}>
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                <div className="navbar__user">
                    <span className="navbar__name">{me.firstName} {me.lastName}</span>
                    <Badge variant={role === "BOARD" ? "info" : undefined}>
                        {role === "BOARD" ? "Board" : "Resident"}
                    </Badge>
                    <button className="btn-link" type="button" onClick={logout}>Log out</button>
                </div>
            </div>
        </header>
    );
}
