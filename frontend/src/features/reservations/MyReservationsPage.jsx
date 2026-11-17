import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "../residents/errors";
import { cancelReservation, getMyReservations } from "../../api/reservations";
import { formatRange } from "./format";

export default function MyReservationsPage() {
    const { token } = useAuth();
    const { data: reservations, error: loadError, loading, reload } =
        useAsync(() => getMyReservations(token), [token]);
    const [confirmingId, setConfirmingId] = useState(null);
    const [error, setError] = useState("");

    async function handleCancel(id) {
        setError("");
        try {
            await cancelReservation(token, id);
            reload();
        } catch (e) {
            setError(describeError(e, "Couldn't cancel the reservation.", {
                409: "This reservation is already cancelled.",
            }));
        } finally {
            setConfirmingId(null);
        }
    }

    if (loadError) return <Notice kind="error">{describeError(loadError, "Couldn't load your reservations.")}</Notice>;
    if (loading) return <Notice>Loading...</Notice>;
    if (reservations.length === 0) return <Notice>You haven't booked anything yet.</Notice>;

    const now = new Date();
    const byStart = (a, b) => a.startTime.localeCompare(b.startTime);
    // Upcoming: soonest first. Past and cancelled: most recent first.
    const isUpcoming = (r) => r.status === "ACTIVE" && new Date(r.endTime) > now;
    const upcoming = reservations.filter(isUpcoming).sort(byStart);
    const past = reservations.filter((r) => !isUpcoming(r)).sort((a, b) => byStart(b, a));

    return (
        <>
            {error && <Notice kind="error">{error}</Notice>}

            {upcoming.length === 0 && <Notice>No upcoming reservations.</Notice>}
            {upcoming.length > 0 && (
                <ul className="list section-gap">
                    {upcoming.map((r) => (
                        <li key={r.id} className="list-item">
                            <div className="list-item__top">
                                <span className="list-item__title">{r.resourceName}</span>
                                <Badge variant="success">Booked</Badge>
                            </div>
                            <div className="list-item__meta">
                                <span>{formatRange(r.startTime, r.endTime)}</span>
                            </div>
                            <div className="list-item__actions">
                                {confirmingId !== r.id ? (
                                    <button className="btn-link btn-link--danger" type="button"
                                            onClick={() => setConfirmingId(r.id)}>
                                        Cancel reservation
                                    </button>
                                ) : (
                                    <span className="list-item__confirm">
                                        Cancel this reservation?
                                        <button className="btn-link btn-link--danger" type="button"
                                                onClick={() => handleCancel(r.id)}>
                                            Yes, cancel
                                        </button>
                                        <button className="btn-link" type="button"
                                                onClick={() => setConfirmingId(null)}>
                                            Keep it
                                        </button>
                                    </span>
                                )}
                            </div>
                        </li>
                    ))}
                </ul>
            )}

            {past.length > 0 && (
                <>
                    <h2 className="list-item__title section-gap">Past and cancelled</h2>
                    <ul className="list">
                        {past.map((r) => (
                            <li key={r.id} className="list-item">
                                <div className="list-item__top">
                                    <span className="list-item__title list-item__sub">{r.resourceName}</span>
                                    <Badge>{r.status === "CANCELLED" ? "Cancelled" : "Done"}</Badge>
                                </div>
                                <div className="list-item__meta">
                                    <span>{formatRange(r.startTime, r.endTime)}</span>
                                </div>
                            </li>
                        ))}
                    </ul>
                </>
            )}
        </>
    );
}
