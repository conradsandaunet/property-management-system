import { useEffect, useState } from "react";
import PageShell from "./PageShell";
import { describeError } from "./errors";
import { getOverview } from "../../api/residents";

export default function OverviewPage({ token }) {
    const [overview, setOverview] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        getOverview(token)
            .then(setOverview)
            .catch((e) => setError(describeError(e, "Couldn't load the overview.")));
    }, [token]);

    const rows = overview && [
        ["Apartments", overview.totalApartments],
        ["Occupied", overview.occupiedApartments],
        ["Vacant", overview.vacantApartments],
        ["Residents", overview.totalResidents],
    ];

    return (
        <PageShell eyebrow="Board" title="Building overview">
            {error && <p className="res-error">{error}</p>}
            {!overview && !error && <p className="res-muted">Loading...</p>}

            {rows && (
                <ul className="res-rows">
                    {rows.map(([label, value]) => (
                        <li key={label} className="res-row">
                            <span className="res-row-label">{label}</span>
                            <span className="res-row-value">{value}</span>
                        </li>
                    ))}
                </ul>
            )}
        </PageShell>
    );
}
