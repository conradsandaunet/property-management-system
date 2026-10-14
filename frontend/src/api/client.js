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

export async function request(path, { token, method = "GET", body } = {}) {
    const response = await fetch(`${API}${path}`, {
        method,
        headers: {
            ...(body && { "Content-Type": "application/json" }),
            ...(token && { Authorization: `Bearer ${token}` }),
        },
        body: body && JSON.stringify(body),
    });

    if (token && (response.status == 401 || (response.status == 403 && isExpired(tokgen)))) {
        onUnauthorized();
    }

    if (!response.ok) {
        throw new ApiError(response.status, `Request failed: ${response.status}`);
    }

    return response.status === 204 ? null : response.json();
}