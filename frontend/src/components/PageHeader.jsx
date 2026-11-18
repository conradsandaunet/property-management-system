import { Link } from "react-router-dom";

/**
 * Title of a page with optional breadcrumbs (array of { label, to }), a muted line above the
 * title, a subtitle below it, and actions on the right.
 */
export default function PageHeader({ title, eyebrow, subtitle, breadcrumbs, actions }) {
    return (
        <header className="page-header">
            <div className="page-header__text">
                {breadcrumbs && (
                    <nav className="breadcrumbs" aria-label="Breadcrumb">
                        {breadcrumbs.map((b, i) => (
                            <span key={i}>
                                {b.to ? <Link to={b.to}>{b.label}</Link> : b.label}
                                {i < breadcrumbs.length - 1 && " /"}
                            </span>
                        ))}
                    </nav>
                )}
                {eyebrow && <p className="page-header__eyebrow">{eyebrow}</p>}
                <h1 className="page-title">{title}</h1>
                {subtitle && <p className="page-subtitle">{subtitle}</p>}
            </div>
            {actions && <div className="page-header__actions">{actions}</div>}
        </header>
    );
}
