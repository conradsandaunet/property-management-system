import { NavLink } from "react-router-dom";

export default function Tabs({ items }) {
    return (
        <nav className="tabs">
            {items.map((item) => (
                <NavLink key={item.to} className="tabs__link" to={item.to} end={item.end}>
                    {item.label}
                </NavLink>
            ))}
        </nav>
    );
}
