import { STATUS_LABELS, STATUS_ORDER } from "./labels";

/** Submitted → Assigned → In progress → Resolved, with the current step highlighted. */
export default function RequestSteps({ status }) {
    const current = STATUS_ORDER.indexOf(status);
    return (
        <div className="steps" aria-label={`Status: ${STATUS_LABELS[status]}`}>
            {STATUS_ORDER.map((s, i) => (
                <div key={s} className={`steps__step${i <= current ? " is-done" : ""}${i === current ? " is-current" : ""}`}>
                    <div className="steps__bar" />
                    <div className="steps__label">{STATUS_LABELS[s]}</div>
                </div>
            ))}
        </div>
    );
}
