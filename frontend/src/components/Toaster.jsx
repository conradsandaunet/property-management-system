import { useCallback, useRef, useState } from "react";
import { ToastContext } from "./toastContext";

const DURATION_MS = 3500;

/** Short confirmations after an action, bottom right. */
export default function Toaster({ children }) {
    const [toasts, setToasts] = useState([]);
    const nextId = useRef(1);

    const toast = useCallback((message, { kind = "success" } = {}) => {
        const id = nextId.current++;
        setToasts((current) => [...current.slice(-2), { id, message, kind }]);
        setTimeout(() => setToasts((current) => current.filter((t) => t.id !== id)), DURATION_MS);
    }, []);

    return (
        <ToastContext.Provider value={toast}>
            {children}
            <div className="toasts" role="status" aria-live="polite">
                {toasts.map((t) => (
                    <div key={t.id} className={`toast${t.kind === "error" ? " toast--error" : ""}`}>
                        <span className="toast__dot" aria-hidden="true" />
                        {t.message}
                    </div>
                ))}
            </div>
        </ToastContext.Provider>
    );
}
