import { useCallback, useEffect, useMemo, useState } from "react";
import { AuthContext } from "./authContext";
import { getMe } from "../api/auth";
import { setUnauthorizedHandler } from "../api/client";

export default function AuthProvider({ children }) {
    const [token, setToken] = useState(() => localStorage.getItem("token"));
    const [me, setMe] = useState(null);
    const [meError, setMeError] = useState(false);

    const login = useCallback((newToken) => {
        localStorage.setItem("token", newToken);
        setMeError(false);
        setToken(newToken);
    }, []);

    const logout = useCallback(() => {
        localStorage.removeItem("token");
        setToken(null);
        setMe(null);
        setMeError(false);
    }, []);

    useEffect(() => {
        setUnauthorizedHandler(logout);
    }, [logout]);

    useEffect(() => {
        if (!token) return;
        let cancelled = false;
        getMe(token)
            .then((data) => {
                if (!cancelled) setMe(data);
            })
            .catch(() => {
                if (!cancelled) setMeError(true);
            });
        return () => {
            cancelled = true;
        };
    }, [token]);

    const refreshMe = useCallback(
        () => getMe(token).then(setMe).catch(() => {}),
        [token],
    );

    const value = useMemo(() => {
        const role = me ? (me.manager ? "BOARD" : "RESIDENT") : null;
        return { token, me, role, meError, loading: Boolean(token) && !me && !meError, login, logout, refreshMe };
    }, [token, me, meError, login, logout, refreshMe]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
