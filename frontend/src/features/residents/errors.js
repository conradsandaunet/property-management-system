const DEFAULTS = {
    400: "Please check the values and try again.",
    403: "You don't have access to this.",
    404: "Not found.",
};

export function describeError(err, fallback, overrides = {}) {
    return overrides[err?.status] ?? DEFAULTS[err?.status] ?? fallback;
}
