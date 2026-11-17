import { Outlet } from "react-router-dom";
import PageHeader from "../../components/PageHeader";
import Tabs from "../../components/Tabs";

const TABS = [
    { to: "book", label: "Book" },
    { to: "mine", label: "My reservations" },
];

export default function ReservationsLayout() {
    return (
        <>
            <PageHeader eyebrow="Reservations" title="Shared resources" />
            <Tabs items={TABS} />
            <Outlet />
        </>
    );
}
