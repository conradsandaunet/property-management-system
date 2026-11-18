import { createContext, useContext } from "react";

export const ViewSwitchContext = createContext(() => {});

/** switchView("board") or switchView("resident", "/account"): plays the transition, then navigates. */
export function useViewSwitch() {
    return useContext(ViewSwitchContext);
}
