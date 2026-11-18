import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import Icon from "./Icon";

/** Side panel for forms and details. Closes on Escape and on a click outside. */
export default function Drawer({ open, title, onClose, children }) {
    const panel = useRef(null);
    // Parents usually pass an inline function; keep the effect tied to `open` only, so focus
    // isn't moved back to the first field on every render.
    const closeRef = useRef(onClose);
    useEffect(() => { closeRef.current = onClose; });

    useEffect(() => {
        if (!open) return undefined;
        const onKey = (e) => { if (e.key === "Escape") closeRef.current(); };
        document.addEventListener("keydown", onKey);
        const previous = document.activeElement;
        // Focus the first field, so a form can be filled in straight away.
        panel.current?.querySelector("input, select, textarea, button:not(.drawer__close)")?.focus();
        return () => {
            document.removeEventListener("keydown", onKey);
            previous?.focus?.();
        };
    }, [open]);

    if (!open) return null;

    return createPortal(
        <>
            <div className="overlay" onClick={onClose} />
            <aside className="drawer" role="dialog" aria-modal="true" aria-label={title} ref={panel}>
                <div className="drawer__head">
                    <h2 className="drawer__title">{title}</h2>
                    <button type="button" className="btn btn--ghost btn--icon drawer__close" aria-label="Close"
                            onClick={onClose}>
                        <Icon name="x" />
                    </button>
                </div>
                <div className="drawer__body">{children}</div>
            </aside>
        </>,
        document.body,
    );
}
