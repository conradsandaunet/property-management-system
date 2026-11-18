import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Icon from "../components/Icon";
import { ViewSwitchContext } from "./viewSwitchContext";

// The cover fades in, the view changes behind it, and it fades out again. Long enough to read as a
// deliberate change of role, short enough not to feel like waiting.
const COVER_MS = 200;
const MIN_VISIBLE_MS = 750;
const REVEAL_MS = 260;

const LABELS = {
    board: "Switching to board view",
    resident: "Switching to your home",
};

const START = { board: "/board", resident: "/home" };

/**
 * A short full-screen transition when a board member switches between the board view and their own
 * home, so the change of role is obvious (similar in spirit to Airbnb's switch to hosting).
 */
export default function ViewSwitchProvider({ children }) {
    const navigate = useNavigate();
    const [state, setState] = useState(null); // { target, leaving }
    const timers = useRef([]);

    useEffect(() => () => timers.current.forEach(clearTimeout), []);

    const switchView = useCallback((target, path) => {
        if (state) return;
        timers.current.forEach(clearTimeout);
        setState({ target, leaving: false });
        timers.current = [
            setTimeout(() => navigate(path ?? START[target]), COVER_MS),
            setTimeout(() => setState({ target, leaving: true }), MIN_VISIBLE_MS),
            setTimeout(() => setState(null), MIN_VISIBLE_MS + REVEAL_MS),
        ];
    }, [navigate, state]);

    return (
        <ViewSwitchContext.Provider value={switchView}>
            {children}
            {state && (
                <div className={`view-switch${state.leaving ? " is-leaving" : ""}`} role="status" aria-live="polite">
                    <div className="view-switch__mark" aria-hidden="true">
                        <Icon name={state.target === "board" ? "overview" : "home"} size={28} />
                    </div>
                    <p className="view-switch__label">{LABELS[state.target]}</p>
                    <div className="view-switch__bar" aria-hidden="true"><span /></div>
                </div>
            )}
        </ViewSwitchContext.Provider>
    );
}
