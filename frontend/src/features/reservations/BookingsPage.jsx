import { useState } from "react";
import { StatusDot } from "../../components/Indicators";
import ConfirmDialog from "../../components/ConfirmDialog";
import EmptyState from "../../components/EmptyState";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { cancelReservation, createReservation, getAvailability, getMyReservations, getResources } from "../../api/reservations";
import { formatRelativeDay } from "../../lib/format";
import { TYPE_LABELS, formatDay, formatRange, formatTime, isBlockedNow, today, upcomingBlock } from "./format";

const DAYS_AHEAD = 7;

function nextDays() {
    const days = [];
    const d = new Date();
    for (let i = 0; i < DAYS_AHEAD; i++) {
        days.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`);
        d.setDate(d.getDate() + 1);
    }
    return days;
}

function resourceNote(r) {
    if (r.status === "MAINTENANCE") return "Closed until further notice";
    if (isBlockedNow(r)) return "Closed for maintenance";
    const block = upcomingBlock(r);
    if (block) return `Closed ${formatDay(block.from)} – ${formatDay(block.to)}`;
    return TYPE_LABELS[r.type] ?? r.type;
}

function BookingFlow({ token, onBooked }) {
    const toast = useToast();
    const resources = useAsync(() => getResources(token), [token]);
    const [resourceId, setResourceId] = useState(null);
    const [date, setDate] = useState(today);
    const [selected, setSelected] = useState(null);
    const [saving, setSaving] = useState(false);

    const activeId = resourceId ?? resources.data?.[0]?.id ?? null;
    const resource = resources.data?.find((r) => r.id === activeId);
    const slots = useAsync(
        () => (activeId ? getAvailability(token, activeId, date) : Promise.resolve([])),
        [token, activeId, date],
    );

    function pick(changes) {
        setSelected(null);
        if ("resourceId" in changes) setResourceId(changes.resourceId);
        if ("date" in changes) setDate(changes.date || today());
    }

    async function handleBook() {
        setSaving(true);
        try {
            await createReservation(token, { resourceId: activeId, startTime: selected.startTime, endTime: selected.endTime });
            toast(`Booked ${resource.name}, ${formatRelativeDay(selected.startTime).toLowerCase()} ${formatTime(selected.startTime)}–${formatTime(selected.endTime)}`);
            setSelected(null);
            onBooked();
        } catch (e) {
            toast(e.status === 409 ? "That time was just taken. Pick another." : "Couldn't make the booking", { kind: "error" });
        } finally {
            setSaving(false);
            slots.reload();
        }
    }

    if (resources.error) return <Notice kind="error">Couldn't load the resources.</Notice>;
    if (resources.loading) return <Skeleton rows={2} />;
    if (resources.data.length === 0) return <EmptyState title="Nothing to book yet">The board hasn't added any shared resources.</EmptyState>;

    const days = nextDays();
    const free = slots.data?.filter((s) => s.available) ?? [];

    return (
        <>
            <div className="field">
                <span className="label">1. Choose what to book</span>
                <div className="resource-cards">
                    {resources.data.map((r) => (
                        <button key={r.id} type="button" onClick={() => pick({ resourceId: r.id })}
                                className={`resource-card${r.id === activeId ? " is-selected" : ""}`}
                                aria-pressed={r.id === activeId}>
                            <div className="resource-card__name">{r.name}</div>
                            <div className={`small ${isBlockedNow(r) ? "" : "muted"}`}
                                 style={isBlockedNow(r) ? { color: "var(--warning)" } : undefined}>
                                {resourceNote(r)}
                            </div>
                        </button>
                    ))}
                </div>
            </div>

            <div className="field">
                <span className="label">2. Pick a day</span>
                <div className="toolbar" style={{ marginBottom: 0 }}>
                    <div className="chips">
                        {days.map((d) => (
                            <button key={d} type="button" className={`chip${d === date ? " is-active" : ""}`}
                                    onClick={() => pick({ date: d })}>
                                {formatRelativeDay(d)}
                            </button>
                        ))}
                    </div>
                    <input className="input" type="date" aria-label="Other date" value={date} min={today()}
                           style={{ width: 160 }} onChange={(e) => pick({ date: e.target.value })} />
                </div>
            </div>

            <div className="field">
                <span className="label">3. Choose a time</span>
                {slots.loading && <Skeleton rows={1} />}
                {slots.error && <Notice kind="error">Couldn't load the available times.</Notice>}
                {slots.data && free.length === 0 && (
                    <Notice>{resource && isBlockedNow(resource)
                        ? `${resource.name} is closed for maintenance.`
                        : `Nothing available ${formatRelativeDay(date).toLowerCase()}. Try another day.`}</Notice>
                )}
                {slots.data && free.length > 0 && (
                    <div className="slots">
                        {slots.data.map((s) => (
                            <button key={s.startTime} type="button" disabled={!s.available}
                                    className={`slot${selected?.startTime === s.startTime ? " slot--selected" : ""}`}
                                    aria-pressed={selected?.startTime === s.startTime}
                                    onClick={() => setSelected(s)}>
                                {formatTime(s.startTime)}–{formatTime(s.endTime)}
                            </button>
                        ))}
                    </div>
                )}
            </div>

            {selected && (
                <div className="confirm-bar">
                    <div className="confirm-bar__text">
                        <strong>{resource.name}</strong> · {formatDay(selected.startTime)} · {formatTime(selected.startTime)}–{formatTime(selected.endTime)}
                    </div>
                    <button type="button" className="btn btn--ghost" onClick={() => setSelected(null)}>Clear</button>
                    <button type="button" className="btn" disabled={saving} onClick={handleBook}>
                        {saving ? "Booking…" : "Book"}
                    </button>
                </div>
            )}
        </>
    );
}

export default function BookingsPage() {
    const { token } = useAuth();
    const toast = useToast();
    const mine = useAsync(() => getMyReservations(token), [token]);
    const [cancelling, setCancelling] = useState(null);
    const [showPast, setShowPast] = useState(false);

    async function handleCancel() {
        try {
            await cancelReservation(token, cancelling.id);
            toast(`Cancelled ${cancelling.resourceName}`);
            mine.reload();
        } catch {
            toast("Couldn't cancel the booking", { kind: "error" });
        }
        setCancelling(null);
    }

    const now = new Date();
    const isUpcoming = (r) => r.status === "ACTIVE" && new Date(r.endTime) > now;
    const upcoming = (mine.data ?? []).filter(isUpcoming).sort((a, b) => a.startTime.localeCompare(b.startTime));
    const past = (mine.data ?? []).filter((r) => !isUpcoming(r)).sort((a, b) => b.startTime.localeCompare(a.startTime));

    return (
        <>
            <PageHeader title="Bookings" subtitle="Reserve shared resources in the building, one hour at a time." />

            <div className="split">
                <section className="card">
                    <h2 className="section__title" style={{ marginBottom: 14 }}>New booking</h2>
                    <BookingFlow token={token} onBooked={mine.reload} />
                </section>

                <section>
                    <div className="section__head"><h2 className="section__title">Your bookings</h2></div>
                    {mine.loading && <Skeleton rows={2} />}
                    {mine.error && <Notice kind="error">Couldn't load your bookings.</Notice>}
                    {mine.data && upcoming.length === 0 && (
                        <EmptyState compact>No upcoming bookings.</EmptyState>
                    )}
                    {upcoming.length > 0 && (
                        <ul className="item-list card card--flush">
                            {upcoming.map((r) => (
                                <li key={r.id} className="item">
                                    <div className="item__body">
                                        <div className="item__title">{r.resourceName}</div>
                                        <div className="item__meta">
                                            {formatRelativeDay(r.startTime)} · {formatTime(r.startTime)}–{formatTime(r.endTime)}
                                        </div>
                                    </div>
                                    <button type="button" className="btn btn--ghost btn--sm" onClick={() => setCancelling(r)}>
                                        Cancel
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}

                    {past.length > 0 && (
                        <div style={{ marginTop: 16 }}>
                            <button type="button" className="btn-link" onClick={() => setShowPast((s) => !s)}>
                                {showPast ? "Hide" : "Show"} past and cancelled ({past.length})
                            </button>
                            {showPast && (
                                <ul className="item-list card card--flush" style={{ marginTop: 8 }}>
                                    {past.map((r) => (
                                        <li key={r.id} className="item">
                                            <div className="item__body">
                                                <div className="item__title muted">{r.resourceName}</div>
                                                <div className="item__meta">{formatRange(r.startTime, r.endTime)}</div>
                                            </div>
                                            <StatusDot>{r.status === "CANCELLED" ? "Cancelled" : "Done"}</StatusDot>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    )}
                </section>
            </div>

            <ConfirmDialog open={Boolean(cancelling)} title="Cancel this booking?" confirmLabel="Cancel booking" danger
                           onConfirm={handleCancel} onCancel={() => setCancelling(null)}>
                {cancelling && `${cancelling.resourceName}, ${formatRange(cancelling.startTime, cancelling.endTime)}.`}
            </ConfirmDialog>
        </>
    );
}
