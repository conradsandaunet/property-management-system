export default function PageHeader({ eyebrow, title }) {
    return (
        <header className="page-header">
            <div className="eyebrow">
                <span className="eyebrow__mark" />
                <span className="eyebrow__text">{eyebrow}</span>
            </div>
            <h1 className="page-title">{title}</h1>
            <hr className="page-rule" />
        </header>
    );
}
