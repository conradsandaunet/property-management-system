import { useEffect, useState } from "react";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAuth } from "../../auth/useAuth";
import { addComment, getComments } from "../../api/maintenance";
import { formatDateTime } from "../../lib/format";

/**
 * Comments on a maintenance request. `viewer` is "resident", "worker" or "board" and decides how
 * authors are named ("You" for your own side). Only residents (own open requests) and the assigned
 * worker can write; the board reads.
 */
export default function CommentThread({ requestId, viewer, canWrite }) {
    const { token } = useAuth();
    const toast = useToast();
    const [comments, setComments] = useState(null);
    const [text, setText] = useState("");
    const [sending, setSending] = useState(false);

    useEffect(() => {
        let cancelled = false;
        getComments(token, requestId)
            .then((data) => { if (!cancelled) setComments(data); })
            .catch(() => { if (!cancelled) setComments([]); });
        return () => { cancelled = true; };
    }, [token, requestId]);

    function author(c) {
        if (c.authorType === "RESIDENT") return viewer === "resident" ? "You" : "Resident";
        return viewer === "worker" ? "You" : c.authorName;
    }

    async function handleSend(e) {
        e.preventDefault();
        if (!text.trim()) return;
        setSending(true);
        try {
            const created = await addComment(token, requestId, text.trim());
            setComments((current) => [...(current ?? []), created]);
            setText("");
        } catch (err) {
            toast(err.status === 409 ? "The request is resolved, so it's closed for comments" : "Couldn't send the comment",
                { kind: "error" });
        } finally {
            setSending(false);
        }
    }

    return (
        <div className="comments">
            {comments === null && <Skeleton rows={1} />}
            {comments && comments.length === 0 && (
                <p className="small muted" style={{ marginBottom: 10 }}>
                    {canWrite ? "No comments yet. Add details or ask a question." : "No comments."}
                </p>
            )}
            {(comments ?? []).map((c) => (
                <div key={c.id} className="comment">
                    <div className="comment__head">
                        <span className="comment__author">{author(c)}</span>
                        <span className="muted">{formatDateTime(c.createdAt)}</span>
                    </div>
                    <p className="comment__text">{c.text}</p>
                </div>
            ))}
            {canWrite && (
                <form className="comment-form" onSubmit={handleSend}>
                    <textarea className="input" rows={1} maxLength={1000} value={text} aria-label="Write a comment"
                              placeholder="Write a comment" onChange={(e) => setText(e.target.value)} />
                    <button className="btn btn--secondary" type="submit" disabled={sending || !text.trim()}>
                        {sending ? "Sending…" : "Send"}
                    </button>
                </form>
            )}
        </div>
    );
}
