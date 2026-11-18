import { createContext, useContext } from "react";

export const ToastContext = createContext(() => {});

/** toast("Saved") or toast("Couldn't save", { kind: "error" }) */
export function useToast() {
    return useContext(ToastContext);
}
