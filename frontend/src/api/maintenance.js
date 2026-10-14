import { request } from "./client";

export const createMaintenance = (token, data) =>
    request("/maintenance", { token, method: "POST", body: data});

export const getMyMaintenance = (token) =>
    request("/maintenance/my", { token });

export const getMaintenance = (token, id) =>
    request(`/maintenance/${id}`, { token });
