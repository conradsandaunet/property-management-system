import { request, requestBlob } from "./client";

// ---- resident: always their own apartment ----

export const getInvoices = (token) =>
    request("/invoices", { token });

export const payInvoice = (token, id) =>
    request(`/invoices/${id}/pay`, { token, method: "POST" });

export const getInvoicePdf = (token, id) =>
    requestBlob(`/invoices/${id}/pdf`, { token });

// ---- board: whole building. period is "YYYY-MM" ----

export const generateInvoices = (token, period) =>
    request(`/invoices/generate?period=${period}`, { token, method: "POST" });

export const getPaymentOverview = (token, period) =>
    request(`/invoices/overview?period=${period}`, { token });

export const markInvoicePaid = (token, id) =>
    request(`/invoices/${id}/mark-paid`, { token, method: "PATCH" });
