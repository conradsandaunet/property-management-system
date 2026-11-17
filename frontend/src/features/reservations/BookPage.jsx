import { useState } from "react";
import { Link } from "react-router-dom";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "../residents/errors";
import { createReservation, getAvailability, getResources } from "../../api/reservations";
import { TYPE_LABELS, formatDay, formatTime, isBlockedNow, today } from "./format";
import "./reservations.css";

export default function BookPage() {
    const { token } = useAuth();
    const resources = useAsync(() => getResources(token), [token]);
    const [resourceId, setResourceId] = useState(null);
    const [date, setDate] = useState(today);
    const [selected, setSelected] = useState(null);
    const [error, setError] = useState("");
    const [booked, setBooked] = useState(null);
    const [saving, setSaving] = useState(false);

    // Default to the first resource once they've loaded.
    const activeId = resourceId ?? resources.data?.[0]?.id ?? null;
    const resource = resources.data?.find((r) => r.id === activeId);

    const slots = useAsync(
        () => (activeId ? getAvailability(token, activeId, date) : Promise.resolve([])),
        [token, activeId, date],
    );

    function choose(changes) {
        setSelected(null);
        setBooked(null);
        setError("");
        if ("resourceId" in changes) setResourceId(changes.resourceId);
        if ("date" in changes) setDate(changes.date || today());
    }

    async function handleBook() {
        setSaving(true);
        setError("");
        try {
            await createReservation(token, {
                resourceId: activeId,
                startTime: selected.startTime,
                endTime: selected.endTime,
            });
            setBooked({ ...selected, resourceName: resource?.name });
            setSelected(null);
        } catch (e) {
            setError(describeError(e, "Couldn't make the reservation.", {
                409: "That time is no longer available. Pick another slot.",
                400: "That time can't be booked. It may be in the past.",
            }));
        } finally {
            setSaving(false);
            slots.reload();
        }
    }

    if (resources.error) return <Notice kind="error">{describeError(resources.error, "Couldn't load resources.")}</Notice>;
    if (resources.loading) return <Notice>Loading...</Notice>;
    if (resources.data.length === 0) return <Notice>There are no shared resources to book yet.</Notice>;

    return (
        <>
            <div className="toolbar">
                <div className="field">
                    <label className="label" htmlFor="resource">Resource</label>
                    <select id="resource" className="input" value={activeId ?? ""}
                            onChange={(e) => choose({ resourceId: Number(e.target.value) })}>
                        {resources.data.map((r) => (
                            <option key={r.id} value={r.id}>
                                {r.name} ({TYPE_LABELS[r.type] ?? r.type})
                            </option>
                        ))}
                    </select>
                </div>
                <div className="field">
                    <label className="label" htmlFor="date">Date</label>
                    <input id="date" className="input" type="date" value={date} min={today()}
                           onChange={(e) => choose({ date: e.target.value })} />
                </div>
            </div>

            {resource && isBlockedNow(resource) && (
                <p className="section-gap"><Badge variant="warning">Closed for maintenance</Badge></p>
            )}

            {booked && (
                <Notice kind="success">
                    Booked {booked.resourceName}, {formatDay(booked.startTime)} {formatTime(booked.startTime)}–{formatTime(booked.endTime)}.{" "}
                    <Link to="../mine">See my reservations</Link>
                </Notice>
            )}
            {error && <Notice kind="error">{error}</Notice>}
            {slots.error && <Notice kind="error">{describeError(slots.error, "Couldn't load available times.")}</Notice>}
            {slots.loading && <Notice>Loading...</Notice>}

            {slots.data && (
                <>
                    {slots.data.every((s) => !s.available) && (
                        <Notice>No available times on {formatDay(date)}. Try another day.</Notice>
                    )}
                    <div className="slots">
                        {slots.data.map((s) => (
                            <button key={s.startTime} type="button" disabled={!s.available}
                                    className={`slot${selected?.startTime === s.startTime ? " slot--selected" : ""}`}
                                    onClick={() => { setSelected(s); setBooked(null); setError(""); }}>
                                {formatTime(s.startTime)}–{formatTime(s.endTime)}
                            </button>
                        ))}
                    </div>
                </>
            )}

            {selected && (
                <div className="form-actions">
                    <button className="btn" type="button" disabled={saving} onClick={handleBook}>
                        {saving ? "Booking…" : `Book ${formatTime(selected.startTime)}–${formatTime(selected.endTime)}`}
                    </button>
                    <button className="btn-link" type="button" onClick={() => setSelected(null)}>Cancel</button>
                </div>
            )}
        </>
    );
}
