"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";
import { phases } from "@/lib/algorithm";
import { PhaseCard } from "@/components/ui/PhaseCard";
import { SchemeBlock } from "@/components/ui/SchemeBlock";
import { WarningCallout } from "@/components/ui/WarningCallout";

export function ViewClient() {
  const [query, setQuery] = useState("");
  const normalizedQuery = query.trim().toLowerCase();

  const filteredPhases = useMemo(() => {
    if (!normalizedQuery) {
      return phases;
    }

    return phases.filter((phase) =>
      phase.positions.some((position) =>
        position.toLowerCase().includes(normalizedQuery),
      ),
    );
  }, [normalizedQuery]);

  return (
    <div className="grid gap-5">
      <label className="grid gap-2 border border-border bg-surface p-4 shadow-panel">
        <span className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
          Поиск по позициям
        </span>
        <span className="grid grid-cols-[20px_1fr] items-center gap-3 border border-border bg-bg px-3 py-3">
          <Search className="h-5 w-5 text-text-mute" aria-hidden />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Найти позицию: А-152, PV-10603, 14047..."
            className="w-full bg-transparent text-base text-text outline-none placeholder:text-text-mute"
          />
        </span>
      </label>

      <div className="grid gap-3">
        {filteredPhases.map((phase) => (
          <PhaseCard
            key={`${phase.id}-${normalizedQuery ? "search" : "all"}`}
            phase={phase}
            defaultOpen={Boolean(normalizedQuery) || phase.id <= 2}
            rightSlot={
              <span className="hidden border border-border bg-bg px-2 py-1 font-mono text-xs text-text-mute sm:inline">
                {phase.positions.length} поз.
              </span>
            }
          >
            <div className="grid gap-4">
              {phase.positions.length > 0 ? (
                <div className="flex flex-wrap gap-2">
                  {phase.positions.map((position) => (
                    <span
                      key={`${phase.id}-${position}`}
                      className="border border-border bg-bg px-2 py-1 font-mono text-xs text-text-dim"
                    >
                      <Highlight text={position} query={query} />
                    </span>
                  ))}
                </div>
              ) : null}

              {phase.warnings?.map((warning) => (
                <WarningCallout key={`${phase.id}-${warning.title}`} warning={warning} />
              ))}

              {phase.scheme ? <SchemeBlock scheme={phase.scheme} /> : null}

              <div className="grid gap-2">
                {phase.checks.map((check) => (
                  <div
                    key={check.id}
                    className="grid gap-2 border border-border bg-surface-2 p-3 text-sm leading-6 text-text"
                  >
                    <span>
                      <span className="font-mono text-cool">{check.id}</span>{" "}
                      <Highlight text={check.text} query={query} />
                    </span>
                    {check.type === "param" && check.param ? (
                      <span className="w-fit border border-amber-soft/30 bg-amber-soft/10 px-2 py-1 font-mono text-xs text-amber-soft">
                        <Highlight text={check.param} query={query} />
                      </span>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          </PhaseCard>
        ))}
      </div>

      {filteredPhases.length === 0 ? (
        <div className="border border-border bg-surface p-5 text-sm text-text-dim">
          Совпадений по позициям не найдено.
        </div>
      ) : null}
    </div>
  );
}

function Highlight({ text, query }: { text: string; query: string }) {
  const value = query.trim();

  if (!value) {
    return <>{text}</>;
  }

  const parts = text.split(new RegExp(`(${escapeRegExp(value)})`, "ig"));

  return (
    <>
      {parts.map((part, index) =>
        part.toLowerCase() === value.toLowerCase() ? (
          <mark key={`${part}-${index}`} className="bg-warn px-1 text-bg">
            {part}
          </mark>
        ) : (
          <span key={`${part}-${index}`}>{part}</span>
        ),
      )}
    </>
  );
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
