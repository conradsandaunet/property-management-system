const WORKER_PREFIX = "worker:";

function decode(token) {
    try {
        return JSON.parse(atob(token.split(".")[1]));
    } catch {
        return null;
    }
}

// Workers have no resident record, so /auth/me does not work for them.
// Their identity comes straight from the token claims instead.
export function workerFromToken(token) {
    const claims = token && decode(token);
    if (!claims || claims.role !== "WORKER" || !claims.sub?.startsWith(WORKER_PREFIX)) {
        return null;
    }
    return { id: Number(claims.sub.slice(WORKER_PREFIX.length)), email: claims.email };
}
