import { request } from "./client";

export const createMaintenance = (token, data) =>
    request("/maintenance", { token, method: "POST", body: data});

export const getMyMaintenance = (token) =>
    request("/maintenance/my", { token });

export const getMaintenance = (token, id) =>
    request(`/maintenance/${id}`, { token });

export const getOpenMaintenance = (token) =>
    request("/maintenance/open", { token });

export const getWorkers = (token) =>
    request("/workers", { token });

export const assignWorker = (token, requestId, workerId) =>
    request(`/maintenance/${requestId}/assignment`, {token, method: "PUT", body: {
        workerId }});
