import PageHeader from "../../components/PageHeader";
import { useAuth } from "../../auth/useAuth";

export default function HomePage() {
    const { me, role } = useAuth();
    const isBoard = role === "BOARD";

    const rows = [
        ["Name", `${me.firstName} ${me.lastName}`],
        ["Email", me.email],
        ["Role", isBoard ? "Board" : "Resident", "accent"],
        ["Apartment", `${me.apartmentNumber} · Floor ${me.floor}`],
        ["Building", me.buildingName],
        ["Status", me.active ? "Active resident" : "Inactive"],
    ];

    return (
        <>
            <PageHeader eyebrow={isBoard ? "Board" : "Resident Portal"} title={`Welcome, ${me.firstName}`} />

            <ul className="rows">
                {rows.map(([label, value, accent]) => (
                    <li key={label} className="row">
                        <span className="row__label">{label}</span>
                        <span className={`row__value${accent ? " row__value--accent" : ""}`}>{value}</span>
                    </li>
                ))}
            </ul>
        </>
    );
}
