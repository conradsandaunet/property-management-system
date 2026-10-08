import { request } from "./client.js";

export async function login(email, password) {
    const data = await request("/auth/login", {
        method: "POST",
        body: { email, password },
    });
    return data.token;
}

export function getMe(token) {
    return request("/auth/me", { token });
}