import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

/**
 * Asks before an action. `onConfirm` may return a promise; the dialog stays open (with the
 * button busy) until it settles, and closes on success.
 */
export default function ConfirmDialog({ open, title, children, confirmLabel = "Confirm", danger = false,
                                          onConfirm, onCancel }) {
    const [busy, setBusy] = useState(false);
    const cancelRef = useRef(onCancel);
    useEffect(() => { cancelRef.current = onCancel; });

    useEffect(() => {
        if (!open) return undefined;
        const onKey = (e) => { if (e.key === "Escape") cancelRef.current(); };
        document.addEventListener("keydown", onKey);
        return () => document.removeEventListener("keydown", onKey);
    }, [open]);

    if (!open) return null;

    async function handleConfirm() {
        setBusy(true);
        try {
            await onConfirm();
        } finally {
            setBusy(false);
        }
    }

    return createPortal(
        <>
            <div className="overlay" onClick={onCancel} />
            <div className="dialog" role="alertdialog" aria-modal="true" aria-label={title}>
                <h2 className="dialog__title">{title}</h2>
                {children && <div className="muted">{children}</div>}
                <div className="form-actions">
                    <button type="button" className="btn btn--secondary" onClick={onCancel}>Cancel</button>
                    <button type="button" className={`btn${danger ? " btn--danger" : ""}`} disabled={busy}
                            onClick={handleConfirm} autoFocus>
                        {busy ? "Working…" : confirmLabel}
                    </button>
                </div>
            </div>
        </>,
        document.body,
    );
}
