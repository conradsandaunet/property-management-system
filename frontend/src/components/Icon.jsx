// A small set of outline icons (24px grid). Decorative unless a label is given.
const PATHS = {
    home: "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z",
    tool: "M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.4-.6-.6-2.4z",
    receipt: "M6 3h12v18l-3-2-3 2-3-2-3 2z M9 8h6 M9 12h6",
    calendar: "M4 6h16v14H4z M4 10h16 M8 3v4 M16 3v4",
    bell: "M6 16v-5a6 6 0 1 1 12 0v5l2 2H4z M10 20a2 2 0 0 0 4 0",
    overview: "M4 4h7v7H4z M13 4h7v4h-7z M13 10h7v10h-7z M4 13h7v7H4z",
    building: "M5 21V4h10v17 M15 9h4v12 M3 21h18 M8 8h3 M8 12h3 M8 16h3",
    cash: "M3 7h18v10H3z M12 9.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z",
    parking: "M5 3h14v18H5z M10 17V8h3a2.5 2.5 0 0 1 0 5h-3",
    user: "M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M4 21a8 8 0 0 1 16 0",
    logout: "M15 4h4v16h-4 M10 8l-4 4 4 4 M6 12h10",
    plus: "M12 5v14 M5 12h14",
    dots: "M5 12h.01 M12 12h.01 M19 12h.01",
    x: "M6 6l12 12 M18 6 6 18",
    menu: "M4 7h16 M4 12h16 M4 17h16",
    chevron: "M9 6l6 6-6 6",
    check: "M5 12l5 5 9-10",
    photo: "M4 5h16v14H4z M4 16l5-5 4 4 3-3 4 4 M15 9h.01",
    clipboard: "M9 4h6v3H9z M7 5H5v16h14V5h-2 M9 12h6 M9 16h4",
    alert: "M12 3 2 20h20z M12 10v4 M12 17h.01",
};

export default function Icon({ name, size = 18, label }) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
             strokeWidth={name === "dots" ? 3 : 1.75} strokeLinecap="round" strokeLinejoin="round"
             role={label ? "img" : undefined} aria-label={label} aria-hidden={label ? undefined : true}
             style={{ flexShrink: 0 }}>
            <path d={PATHS[name]} />
        </svg>
    );
}
