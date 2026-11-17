import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import LoginPage from "../auth/LoginPage";
import AppLayout from "./AppLayout";
import { RequireAuth, RequireRole } from "./guards";
import HomePage from "../features/home/HomePage";
import AccountLayout from "../features/residents/AccountLayout";
import HouseholdPage from "../features/residents/HouseholdPage";
import ContactPage from "../features/residents/ContactPage";
import OverviewPage from "../features/residents/OverviewPage";
import ManageResidentsPage from "../features/residents/ManageResidentsPage";
import ManageApartmentsPage from "../features/residents/ManageApartmentsPage";
import MyRequestPage from "../features/maintenance/MyRequestPage";
import NewRequestPage from "../features/maintenance/NewRequestPage";
import OpenRequestPage from "../features/maintenance/OpenRequestPage.jsx";
import WorkOrdersPage from "../features/maintenance/WorkOrdersPage";
import NotificationsPage from "../features/notifications/NotificationsPage";

export default function App() {
    const { token, role } = useAuth();

    return (
        <Routes>
            <Route path="/login" element={token ? <Navigate to="/" replace /> : <LoginPage />} />
            <Route path="/worker/login" element={token ? <Navigate to="/" replace /> : <LoginPage worker />} />

            <Route element={<RequireAuth />}>
                <Route element={<AppLayout />}>
                    <Route index element={role === "WORKER" ? <Navigate to="/work-orders" replace /> : <HomePage />} />

                    <Route path="maintenance" element={<MyRequestPage token={token} />} />
                    <Route path="maintenance/new" element={<NewRequestPage token={token} />} />

                    <Route path="account" element={<AccountLayout />}>
                        <Route index element={<Navigate to="household" replace />} />
                        <Route path="household" element={<HouseholdPage />} />
                        <Route path="contact" element={<ContactPage />} />
                    </Route>

                    <Route element={<RequireRole roles={["BOARD"]} />}>
                        <Route path="board/overview" element={<OverviewPage />} />
                        <Route path="board/residents" element={<ManageResidentsPage />} />
                        <Route path="board/apartments" element={<ManageApartmentsPage />} />
                        <Route path="board/maintenance" element={<OpenRequestPage />} />                    </Route>

                    <Route element={<RequireRole roles={["RESIDENT", "BOARD"]} />}>
                        <Route path="notifications" element={<NotificationsPage />} />
                    </Route>

                    <Route element={<RequireRole roles={["WORKER"]} />}>
                        <Route path="work-orders" element={<WorkOrdersPage />} />
                    </Route>

                    <Route path="*" element={<Navigate to="/" replace />} />
                </Route>
            </Route>
        </Routes>
    );
}
