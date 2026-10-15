import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "./errors";
import { getOverview } from "../../api/residents";

export default function OverviewPage() {
    const { token } = useAuth();
    const { data: overview, error, loading } = useAsync(() => getOverview(token), [token]);

    const rows = overview && [
        ["Apartments", overview.totalApartments],
        ["Occupied", overview.occupiedApartments],
        ["Vacant", overview.vacantApartments],
        ["Residents", overview.totalResidents],
    ];

    return (
        <>
            <PageHeader eyebrow="Board" title="Building overview" />
            {error && <Notice kind="error">{describeError(error, "Couldn't load the overview.")}</Notice>}
            {loading && <Notice>Loading...</Notice>}

            {rows && (
                <ul className="rows">
                    {rows.map(([label, value]) => (
                        <li key={label} className="row">
                            <span className="row__label">{label}</span>
                            <span className="row__value row__value--stat">{value}</span>
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
