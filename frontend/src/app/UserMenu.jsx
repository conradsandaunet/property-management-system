import { useLocation, useNavigate } from "react-router-dom";
import Menu from "../components/Menu";
import { useAuth } from "../auth/useAuth";
import { initials } from "../lib/format";
import { ROLE_LABELS, isBoardPath } from "./nav";
import { useViewSwitch } from "./viewSwitchContext";

export default function UserMenu({ compact = false, align = "right", direction = "down" }) {
    const { me, role, logout } = useAuth();
    const navigate = useNavigate();
    const switchView = useViewSwitch();
    const { pathname } = useLocation();
    const isWorker = role === "WORKER";
    const inBoardView = isBoardPath(pathname);
    const name = isWorker ? me.email : `${me.firstName} ${me.lastName}`;

    return (
        <Menu
            align={align}
            direction={direction}
            block={!compact}
            trigger={({ open, toggle }) => (
                <button type="button" className="user-button" aria-haspopup="menu" aria-expanded={open}
                        aria-label={compact ? `Account menu for ${name}` : undefined} onClick={toggle}>
                    <span className="avatar" aria-hidden="true">
                        {isWorker ? me.email[0].toUpperCase() : initials(me.firstName, me.lastName)}
                    </span>
                    {!compact && (
                        <span className="user-button__text">
                            <span className="user-button__name" style={{ display: "block" }}>{name}</span>
                            <span className="user-button__role">{ROLE_LABELS[role]}</span>
                        </span>
                    )}
                </button>
            )}
            items={[
                role === "BOARD" && (inBoardView
                    ? { label: "Switch to resident view", onClick: () => switchView("resident") }
                    : { label: "Switch to board view", onClick: () => switchView("board") }),
                role === "BOARD" && "separator",
                // From the board view, the account page means a switch to the resident view.
                !isWorker && { label: "Your account", onClick: () => (inBoardView ? switchView("resident", "/account") : navigate("/account")) },
                !isWorker && "separator",
                { label: "Log out", onClick: logout },
            ]}
        />
    );
}
