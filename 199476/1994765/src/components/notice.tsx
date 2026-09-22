export function NoticeBox({
  children,
  tone = "info",
}: {
  children: React.ReactNode;
  tone?: "info" | "warn";
}) {
  const styles =
    tone === "warn"
      ? "border-amber-200 bg-amber-50 text-amber-900"
      : "border-slate-200 bg-slate-50 text-slate-600";
  const icon = tone === "warn" ? "⚠️" : "ℹ️";
  return (
    <div className={`rounded-xl border p-4 text-sm leading-relaxed ${styles}`}>
      <span aria-hidden className="mr-1.5">
        {icon}
      </span>
      {children}
    </div>
  );
}

export function SectionHeading({
  title,
  description,
  more,
}: {
  title: string;
  description?: string;
  more?: { href: string; label: string };
}) {
  return (
    <div className="mb-5 flex flex-wrap items-end justify-between gap-2">
      <div>
        <h2 className="text-xl font-bold text-slate-900 md:text-2xl">{title}</h2>
        {description && (
          <p className="mt-1 text-sm text-slate-500">{description}</p>
        )}
      </div>
      {more && (
        <a
          href={more.href}
          className="text-sm font-medium text-teal-700 hover:text-teal-800"
        >
          {more.label} →
        </a>
      )}
    </div>
  );
}
