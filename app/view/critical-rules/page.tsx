import { ShieldAlert } from "lucide-react";
import { criticalRules } from "@/lib/algorithm";

export default function CriticalRulesPage() {
  return (
    <div className="grid gap-5">
      <div>
        <p className="font-mono text-xs uppercase tracking-[0.18em] text-danger">
          Контрольные ограничения
        </p>
        <h1 className="mt-2 text-3xl font-semibold text-text">
          Критические правила
        </h1>
      </div>

      <div className="grid gap-3 md:grid-cols-2">
        {criticalRules.map((rule) => (
          <article
            key={rule.id}
            className="border border-danger/50 bg-danger/10 p-5 shadow-panel"
          >
            <div className="flex gap-3">
              <ShieldAlert className="mt-1 h-5 w-5 shrink-0 text-danger" aria-hidden />
              <div>
                <p className="font-mono text-xs uppercase tracking-[0.18em] text-danger">
                  {rule.id}
                </p>
                <h2 className="mt-2 text-xl font-semibold text-text">{rule.title}</h2>
                <p className="mt-4 text-sm leading-6 text-text">{rule.rule}</p>
                {rule.consequence ? (
                  <p className="mt-4 border border-border bg-bg/50 p-3 text-sm leading-6 text-text-dim">
                    {rule.consequence}
                  </p>
                ) : null}
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
