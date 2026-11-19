const API = "/api"

export class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

let onUnauthorized = () => {};
export function setUnauthorizedHandler(fn) {
    onUnauthorized = fn;
}

function isExpired(token) {
    try {
        const { exp } = JSON.parse(atob(token.split(".")[1]));
        return exp * 1000 < Date.now();
    } catch {
        return true;
    }
}

// A read that fails because the connection broke or a service didn't answer (502/503/504) is tried
// again a couple of times before the page shows an error. Writes are never repeated, since they
// could be applied twice.
const RETRY_STATUSES = new Set([502, 503, 504]);
const MAX_ATTEMPTS = 3;
const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function send(path, init, token) {
    const attempts = (init.method ?? "GET").toUpperCase() === "GET" ? MAX_ATTEMPTS : 1;
    let response;

    for (let attempt = 1; ; attempt++) {
        try {
            response = await fetch(`${API}${path}`, init);
        } catch {
            if (attempt >= attempts) throw new ApiError(0, "The server can't be reached");
            await wait(400 * attempt);
            continue;
        }
        if (RETRY_STATUSES.has(response.status) && attempt < attempts) {
            await wait(400 * attempt);
            continue;
        }
        break;
    }

    if (token && (response.status == 401 || (response.status == 403 && isExpired(token)))) {
        onUnauthorized();
    }

    if (!response.ok) {
        throw new ApiError(response.status, `Request failed: ${response.status}`);
    }
    return response;
}

/** For binary responses such as PDFs. */
export async function requestBlob(path, { token } = {}) {
    const response = await send(path, {
        headers: { ...(token && { Authorization: `Bearer ${token}` }) },
    }, token);
    return response.blob();
}

export async function request(path, { token, method = "GET", body } = {}) {
    // FormData (file uploads) must go out as-is: the browser sets the multipart Content-Type
    // with its boundary itself.
    const isForm = body instanceof FormData;
    const response = await send(path, {
        method,
        headers: {
            ...(body && !isForm && { "Content-Type": "application/json" }),
            ...(token && { Authorization: `Bearer ${token}` }),
        },
        body: isForm ? body : body && JSON.stringify(body),
    }, token);

    const text = await response.text();
    return text ? JSON.parse(text) : null;
}