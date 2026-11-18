import { useEffect, useRef, useState } from "react";
import Icon from "./Icon";

/**
 * Overflow menu. `items` is a list of { label, onClick, danger } or "separator" (falsy entries are
 * skipped). Pass `panel` ({ close }) => content instead for a custom popover, such as notifications.
 * Closes on outside click, Escape, and after choosing an item.
 */
export default function Menu({ items = [], label = "More actions", trigger, align = "right", direction = "down", block = false, panel }) {
    const [open, setOpen] = useState(false);
    const root = useRef(null);

    useEffect(() => {
        if (!open) return undefined;
        const onDown = (e) => { if (!root.current?.contains(e.target)) setOpen(false); };
        const onKey = (e) => { if (e.key === "Escape") setOpen(false); };
        document.addEventListener("mousedown", onDown);
        document.addEventListener("keydown", onKey);
        return () => {
            document.removeEventListener("mousedown", onDown);
            document.removeEventListener("keydown", onKey);
        };
    }, [open]);

    const visible = items.filter(Boolean);

    return (
        <div className={`menu${block ? " menu--block" : ""}`} ref={root} onClick={(e) => e.stopPropagation()}>
            {trigger ? trigger({ open, toggle: () => setOpen((o) => !o) }) : (
                <button type="button" className="btn btn--ghost btn--icon" aria-label={label}
                        aria-haspopup="menu" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
                    <Icon name="dots" />
                </button>
            )}
            {open && (
                <div className={`menu__list${align === "left" ? " menu__list--left" : ""}${direction === "up" ? " menu__list--up" : ""}`}
                     role={panel ? "dialog" : "menu"}>
                    {panel && panel({ close: () => setOpen(false) })}
                    {!panel && visible.map((item, i) => item === "separator" ? (
                        <div key={i} className="menu__sep" role="separator" />
                    ) : (
                        <button key={item.label} type="button" role="menuitem"
                                className={`menu__item${item.danger ? " menu__item--danger" : ""}`}
                                onClick={() => { setOpen(false); item.onClick(); }}>
                            {item.label}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
