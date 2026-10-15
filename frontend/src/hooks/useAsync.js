import { useCallback, useEffect, useRef, useState } from "react";

export function useAsync(fn, deps = []) {
    const [state, setState] = useState({ data: null, error: null, loading: true });
    const [tick, setTick] = useState(0);

    const fnRef = useRef(fn);
    useEffect(() => {
        fnRef.current = fn;
    });

    const depsKey = JSON.stringify(deps);

    useEffect(() => {
        let cancelled = false;
        fnRef.current()
            .then((data) => {
                if (!cancelled) setState({ data, error: null, loading: false });
            })
            .catch((error) => {
                if (!cancelled) setState({ data: null, error, loading: false });
            });
        return () => {
            cancelled = true;
        };
    }, [depsKey, tick]);

    const reload = useCallback(() => setTick((t) => t + 1), []);
    return { ...state, reload };
}
