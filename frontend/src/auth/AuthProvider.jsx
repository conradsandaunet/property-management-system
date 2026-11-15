import { useCallback, useEffect, useMemo, useState } from "react";
import { AuthContext } from "./authContext";
import { getMe } from "../api/auth";
import { setUnauthorizedHandler } from "../api/client";
import { workerFromToken } from "./token";

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

    const worker = useMemo(() => workerFromToken(token), [token]);

    useEffect(() => {
        if (!token || worker) return;
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
    }, [token, worker]);

    const refreshMe = useCallback(
        () => getMe(token).then(setMe).catch(() => {}),
        [token],
    );

    const value = useMemo(() => {
        const currentMe = worker ?? me;
        const role = worker ? "WORKER" : me ? (me.manager ? "BOARD" : "RESIDENT") : null;
        return {
            token,
            me: currentMe,
            role,
            meError: worker ? false : meError,
            loading: Boolean(token) && !currentMe && !meError,
            login,
            logout,
            refreshMe,
        };
    }, [token, worker, me, meError, login, logout, refreshMe]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
