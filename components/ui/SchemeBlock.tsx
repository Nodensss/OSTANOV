import type { Scheme } from "@/lib/algorithm";

export function SchemeBlock({ scheme }: { scheme: Scheme }) {
  return (
    <div className="border border-cool/40 bg-cool/10 p-4">
      <p className="mb-3 font-mono text-xs uppercase tracking-[0.18em] text-cool">
        {scheme.title}
      </p>
      <pre className="overflow-x-auto whitespace-pre font-mono text-sm leading-7 text-text">
        {scheme.lines.join("\n")}
      </pre>
    </div>
  );
}
