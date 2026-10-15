import { Outlet } from "react-router-dom";
import PageHeader from "../../components/PageHeader";
import Tabs from "../../components/Tabs";

const TABS = [
    { to: "household", label: "Household" },
    { to: "contact", label: "Contact details" },
];

export default function AccountLayout() {
    return (
        <>
            <PageHeader eyebrow="Residents" title="My account" />
            <Tabs items={TABS} />
            <Outlet />
        </>
    );
}
