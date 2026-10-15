import { useEffect, useState } from "react";
import PageShell from "./PageShell";
import { describeError } from "./errors";
import { getHousehold } from "../../api/residents";

export default function HouseholdPage({ token }) {
    const [members, setMembers] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        getHousehold(token)
            .then(setMembers)
            .catch((e) => setError(describeError(e, "Couldn't load your household.")));
    }, [token]);

    return (
        <PageShell eyebrow="Residents" title="My household">
            {error && <p className="res-error">{error}</p>}
            {members === null && !error && <p className="res-muted">Loading...</p>}

            {members && members.length === 0 && (
                <p className="res-muted">No one is registered in your apartment.</p>
            )}

            {members && members.length > 0 && (
                <ul className="res-list">
                    {members.map((m) => (
                        <li key={m.id} className="res-item">
                            <span className="res-item-title">{m.firstName} {m.lastName}</span>
                        </li>
                    ))}
                </ul>
            )}
        </PageShell>
    );
}
