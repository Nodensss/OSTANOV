export function ProgressBar({
  value,
  label,
}: {
  value: number;
  label?: string;
}) {
  const normalized = Math.min(100, Math.max(0, value));

  return (
    <div className="grid gap-2">
      <div className="flex items-center justify-between gap-3 font-mono text-xs uppercase tracking-[0.16em] text-text-mute">
        <span>{label ?? "Прогресс"}</span>
        <span className="text-cool">{Math.round(normalized)}%</span>
      </div>
      <div className="h-3 overflow-hidden border border-border bg-bg">
        <div
          className="h-full bg-cool transition-[width]"
          style={{ width: `${normalized}%` }}
        />
      </div>
    </div>
  );
}
