import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "../auth/useAuth";
import LoginPage from "../auth/LoginPage";
import AppLayout from "./AppLayout";
import { RequireAuth, RequireRole } from "./guards";
import { START_PAGE } from "./nav";
import HomePage from "../features/home/HomePage";
import AccountPage from "../features/residents/AccountPage";
import NotificationsPage from "../features/notifications/NotificationsPage";
import MyRequestsPage from "../features/maintenance/MyRequestsPage";
import InvoicesPage from "../features/billing/InvoicesPage";
import BookingsPage from "../features/reservations/BookingsPage";
import OverviewPage from "../features/board/OverviewPage";
import PropertyPage from "../features/property/PropertyPage";
import ApartmentDetailPage from "../features/property/ApartmentDetailPage";
import BuildingPage from "../features/property/BuildingPage";
import MaintenanceQueuePage from "../features/maintenance/MaintenanceQueuePage";
import BillingPage from "../features/billing/BillingPage";
import ResourcesPage from "../features/reservations/ResourcesPage";
import WorkOrdersPage from "../features/maintenance/WorkOrdersPage";

export default function App() {
    const { token, role } = useAuth();

    return (
        <Routes>
            <Route path="/login" element={token ? <Navigate to="/" replace /> : <LoginPage />} />
            <Route path="/worker/login" element={token ? <Navigate to="/" replace /> : <LoginPage worker />} />

            <Route element={<RequireAuth />}>
                <Route element={<AppLayout />}>
                    <Route index element={<Navigate to={START_PAGE[role] ?? "/home"} replace />} />

                    <Route element={<RequireRole roles={["RESIDENT", "BOARD"]} />}>
                        <Route path="home" element={<HomePage />} />
                        <Route path="maintenance" element={<MyRequestsPage />} />
                        <Route path="invoices" element={<InvoicesPage />} />
                        <Route path="bookings" element={<BookingsPage />} />
                        <Route path="account" element={<AccountPage />} />
                        <Route path="notifications" element={<NotificationsPage />} />

                        {/* Old addresses, kept so links and bookmarks still work. */}
                        <Route path="maintenance/new" element={<Navigate to="/maintenance?new=1" replace />} />
                        <Route path="reservations/*" element={<Navigate to="/bookings" replace />} />
                        <Route path="account/*" element={<Navigate to="/account" replace />} />
                    </Route>

                    <Route element={<RequireRole roles={["BOARD"]} />}>
                        <Route path="board" element={<OverviewPage />} />
                        <Route path="board/property" element={<PropertyPage />} />
                        <Route path="board/property/buildings/:id" element={<BuildingPage />} />
                        <Route path="board/property/apartments/:id" element={<ApartmentDetailPage />} />
                        <Route path="board/maintenance" element={<MaintenanceQueuePage />} />
                        <Route path="board/billing" element={<BillingPage />} />
                        <Route path="board/resources" element={<ResourcesPage />} />

                        <Route path="board/overview" element={<Navigate to="/board" replace />} />
                        <Route path="board/residents" element={<Navigate to="/board/property?tab=residents" replace />} />
                        <Route path="board/apartments" element={<Navigate to="/board/property" replace />} />
                        <Route path="board/buildings" element={<Navigate to="/board/property" replace />} />
                        <Route path="board/invoices" element={<Navigate to="/board/billing" replace />} />
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
