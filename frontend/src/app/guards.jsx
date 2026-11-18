import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import Notice from "../components/Notice";
import PageHeader from "../components/PageHeader";
import Skeleton from "../components/Skeleton";

export function RequireAuth() {
    const { token, loading, meError, logout } = useAuth();

    if (!token) {
        return <Navigate to="/login" replace />;
    }
    if (meError) {
        return (
            <div className="container">
                <Notice kind="error">Couldn't load your account.</Notice>
                <button className="btn btn--secondary" type="button" onClick={logout}>Log out</button>
            </div>
        );
    }
    if (loading) {
        return (
            <div className="container">
                <Skeleton rows={4} />
            </div>
        );
    }
    return <Outlet />;
}

export function RequireRole({ roles }) {
    const { role } = useAuth();

    if (!roles.includes(role)) {
        return (
            <>
                <PageHeader title="No access" />
                <Notice kind="error">You don't have access to this page.</Notice>
            </>
        );
    }
    return <Outlet />;
}
