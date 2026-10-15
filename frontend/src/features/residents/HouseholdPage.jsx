import Notice from "../../components/Notice";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "./errors";
import { getHousehold } from "../../api/residents";

export default function HouseholdPage() {
    const { token } = useAuth();
    const { data: members, error, loading } = useAsync(() => getHousehold(token), [token]);

    if (error) return <Notice kind="error">{describeError(error, "Couldn't load your household.")}</Notice>;
    if (loading) return <Notice>Loading...</Notice>;
    if (members.length === 0) return <Notice>No one is registered in your apartment.</Notice>;

    return (
        <ul className="list">
            {members.map((m) => (
                <li key={m.id} className="list-item">
                    <span className="list-item__title">{m.firstName} {m.lastName}</span>
                </li>
            ))}
        </ul>
    );
}
