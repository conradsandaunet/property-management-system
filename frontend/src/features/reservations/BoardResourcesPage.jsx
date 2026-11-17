import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "../residents/errors";
import { searchResidents } from "../../api/residents";
import {
    blockResource,
    getReservationsForResource,
    getResources,
    setResourceStatus,
    unblockResource,
} from "../../api/reservations";
import { TYPE_LABELS, formatDay, formatRange, formatTime, isBlockedNow } from "./format";

function BlockForm({ onSubmit, onCancel }) {
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        if (from >= to) {
            setError("The end must be after the start.");
            return;
        }
        setError("");
        setSaving(true);
        try {
            await onSubmit(from, to);
        } catch (err) {
            setError(describeError(err, "Couldn't block the resource."));
            setSaving(false);
        }
    }

    return (
        <form className="form-card section-gap" onSubmit={handleSubmit}>
            <div className="field-row">
                <div className="field">
                    <label className="label" htmlFor="block-from">From</label>
                    <input id="block-from" className="input" type="datetime-local" value={from}
                           onChange={(e) => setFrom(e.target.value)} required />
                </div>
                <div className="field">
                    <label className="label" htmlFor="block-to">To</label>
                    <input id="block-to" className="input" type="datetime-local" value={to}
                           onChange={(e) => setTo(e.target.value)} required />
                </div>
            </div>
            <p className="hint section-gap">
                Residents can't book the resource in this period. Existing bookings are not cancelled automatically.
            </p>
            {error && <Notice kind="error">{error}</Notice>}
            <div className="form-actions">
                <button className="btn" type="submit" disabled={saving}>
                    {saving ? "Saving…" : "Block resource"}
                </button>
                <button className="btn-link" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}

function ResourceReservations({ token, resourceId, residentName }) {
    const { data, error, loading } = useAsync(() => getReservationsForResource(token, resourceId), [token, resourceId]);

    if (error) return <Notice kind="error">{describeError(error, "Couldn't load the reservations.")}</Notice>;
    if (loading) return <Notice>Loading...</Notice>;
    if (data.length === 0) return <Notice>No active reservations.</Notice>;

    const sorted = [...data].sort((a, b) => a.startTime.localeCompare(b.startTime));
    return (
        <ul className="list">
            {sorted.map((r) => (
                <li key={r.id} className="list-item">
                    <span className="list-item__title">{formatRange(r.startTime, r.endTime)}</span>
                    <div className="list-item__meta">
                        <span>{residentName(r.residentId)}</span>
                    </div>
                </li>
            ))}
        </ul>
    );
}

export default function BoardResourcesPage() {
    const { token } = useAuth();
    const { data: resources, error: loadError, loading, reload } = useAsync(() => getResources(token), [token]);
    // Reservations only store a resident id; names come from resident-service.
    const residents = useAsync(() => searchResidents(token, "", true), [token]);
    const [expandedId, setExpandedId] = useState(null);
    const [blockingId, setBlockingId] = useState(null);
    const [error, setError] = useState("");

    function residentName(id) {
        const r = residents.data?.find((x) => x.id === id);
        return r ? `${r.firstName} ${r.lastName} · apartment ${r.apartmentNumber}` : `Resident #${id}`;
    }

    async function run(action, fallback) {
        setError("");
        try {
            await action();
            reload();
        } catch (e) {
            setError(describeError(e, fallback));
        }
    }

    async function handleBlock(id, from, to) {
        await blockResource(token, id, from, to);
        setBlockingId(null);
        reload();
    }

    return (
        <>
            <PageHeader eyebrow="Board" title="Shared resources" />

            {(error || loadError) && (
                <Notice kind="error">{error || describeError(loadError, "Couldn't load resources.")}</Notice>
            )}
            {loading && <Notice>Loading...</Notice>}
            {resources && resources.length === 0 && <Notice>There are no shared resources yet.</Notice>}

            {resources && resources.length > 0 && (
                <ul className="list list--wide">
                    {resources.map((res) => (
                        <li key={res.id} className="list-item">
                            <div className="list-item__top">
                                <span className="list-item__title">{res.name}</span>
                                <span className="badges">
                                    <Badge>{TYPE_LABELS[res.type] ?? res.type}</Badge>
                                    {isBlockedNow(res)
                                        ? <Badge variant="warning">Closed</Badge>
                                        : <Badge variant="success">Open</Badge>}
                                </span>
                            </div>

                            <div className="list-item__meta">
                                {res.status === "MAINTENANCE" && <span>Closed until reopened</span>}
                                {res.maintenanceFrom && res.maintenanceTo && (
                                    <span>
                                        Blocked {formatDay(res.maintenanceFrom)} {formatTime(res.maintenanceFrom)}
                                        {" – "}
                                        {formatDay(res.maintenanceTo)} {formatTime(res.maintenanceTo)}
                                    </span>
                                )}
                            </div>

                            <div className="list-item__actions">
                                <button className="btn-link" type="button"
                                        onClick={() => setExpandedId(expandedId === res.id ? null : res.id)}>
                                    {expandedId === res.id ? "Hide reservations" : "Reservations"}
                                </button>
                                {res.maintenanceFrom ? (
                                    <button className="btn-link" type="button"
                                            onClick={() => run(() => unblockResource(token, res.id), "Couldn't remove the block.")}>
                                        Remove block
                                    </button>
                                ) : (
                                    blockingId !== res.id && (
                                        <button className="btn-link" type="button" onClick={() => setBlockingId(res.id)}>
                                            Block for a period
                                        </button>
                                    )
                                )}
                                <button className="btn-link" type="button"
                                        onClick={() => run(
                                            () => setResourceStatus(token, res.id,
                                                res.status === "MAINTENANCE" ? "AVAILABLE" : "MAINTENANCE"),
                                            "Couldn't change the status.")}>
                                    {res.status === "MAINTENANCE" ? "Reopen" : "Close until further notice"}
                                </button>
                            </div>

                            {blockingId === res.id && (
                                <BlockForm onSubmit={(from, to) => handleBlock(res.id, from, to)}
                                           onCancel={() => setBlockingId(null)} />
                            )}

                            {expandedId === res.id && (
                                <div className="section-gap">
                                    <ResourceReservations token={token} resourceId={res.id} residentName={residentName} />
                                </div>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
