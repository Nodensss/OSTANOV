"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { RotateCcw } from "lucide-react";
import type { Phase } from "@/lib/algorithm";
import { phases, sections, totalChecks } from "@/lib/algorithm";
import { ensureSessionId } from "@/lib/session";
import { CheckRow } from "@/components/checklist/CheckRow";
import { PhaseCard } from "@/components/ui/PhaseCard";
import { ProgressBar } from "@/components/ui/ProgressBar";
import { SchemeBlock } from "@/components/ui/SchemeBlock";
import { Timer, type TimerSnapshot } from "@/components/ui/Timer";
import { WarningCallout } from "@/components/ui/WarningCallout";

type ProgressPayload = {
  checkboxes: Array<{ checkId: string; checked: boolean; checkedAt: string | null }>;
  timers: TimerSnapshot[];
};

export function ChecklistClient() {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [timers, setTimers] = useState<Record<number, TimerSnapshot>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const pendingPatch = useRef<Record<string, number>>({});

  useEffect(() => {
    let cancelled = false;
    const pending = pendingPatch.current;

    async function load() {
      try {
        const id = await ensureSessionId();
        if (cancelled) return;
        setSessionId(id);

        const response = await fetch(`/api/progress?sessionId=${id}`, {
          cache: "no-store",
        });

        if (!response.ok) {
          throw new Error("progress request failed");
        }

        const data = (await response.json()) as ProgressPayload;
        if (cancelled) return;

        setChecked(
          Object.fromEntries(
            data.checkboxes.map((item) => [item.checkId, item.checked]),
          ),
        );
        setTimers(
          Object.fromEntries(
            data.timers.map((timer) => [
              timer.phaseId,
              {
                ...timer,
                startedAt: timer.startedAt
                  ? new Date(timer.startedAt).toISOString()
                  : null,
                pausedAt: timer.pausedAt ? new Date(timer.pausedAt).toISOString() : null,
              },
            ]),
          ),
        );
      } catch {
        if (!cancelled) {
          setError("Нет соединения с БД. Проверьте DATABASE_URL и prisma db push.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
      Object.values(pending).forEach(window.clearTimeout);
    };
  }, []);

  const completedCount = useMemo(
    () =>
      phases.reduce(
        (sum, phase) =>
          sum + phase.checks.filter((check) => checked[check.id]).length,
        0,
      ),
    [checked],
  );

  const progress = totalChecks === 0 ? 0 : (completedCount / totalChecks) * 100;

  function isPhaseDone(phase: Phase) {
    return phase.checks.length > 0 && phase.checks.every((check) => checked[check.id]);
  }

  function toggleCheck(checkId: string, nextChecked: boolean) {
    setChecked((current) => ({ ...current, [checkId]: nextChecked }));

    if (!sessionId) {
      return;
    }

    window.clearTimeout(pendingPatch.current[checkId]);
    pendingPatch.current[checkId] = window.setTimeout(async () => {
      try {
        const response = await fetch("/api/progress/checkbox", {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ sessionId, checkId, checked: nextChecked }),
        });

        if (!response.ok) {
          throw new Error("checkbox patch failed");
        }
      } catch {
        setError("Не удалось сохранить отметку. Проверьте подключение к БД.");
      }
    }, 350);
  }

  async function resetProgress() {
    if (!sessionId) {
      return;
    }

    const confirmed = window.confirm("Сбросить все отметки и таймеры этой сессии?");
    if (!confirmed) {
      return;
    }

    const response = await fetch(`/api/progress?sessionId=${sessionId}`, {
      method: "DELETE",
    });

    if (response.ok) {
      setChecked({});
      setTimers({});
      setError(null);
    } else {
      setError("Не удалось сбросить прогресс.");
    }
  }

  return (
    <div className="grid gap-5">
      <section className="grid gap-4 border border-border bg-surface p-4 shadow-panel sm:grid-cols-[1fr_auto] sm:p-5">
        <ProgressBar
          value={progress}
          label={`${completedCount} из ${totalChecks} шагов`}
        />
        <div className="flex items-end gap-2">
          <button
            type="button"
            onClick={resetProgress}
            disabled={!sessionId || loading}
            className="inline-flex h-11 items-center gap-2 border border-border bg-surface-2 px-4 text-sm font-semibold text-text-dim transition hover:border-danger hover:text-danger disabled:cursor-not-allowed disabled:opacity-50"
          >
            <RotateCcw className="h-4 w-4" aria-hidden />
            Сбросить прогресс
          </button>
        </div>
      </section>

      {error ? (
        <div className="border border-danger/50 bg-danger/10 p-4 text-sm leading-6 text-text">
          {error}
        </div>
      ) : null}

      {sections.map((section) => (
        <section key={section} className="grid gap-3">
          <h2 className="font-mono text-xs uppercase tracking-[0.22em] text-text-mute">
            {section}
          </h2>
          {phases
            .filter((phase) => phase.section === section)
            .map((phase) => {
              const done = isPhaseDone(phase);

              return (
                <PhaseCard
                  key={phase.id}
                  phase={phase}
                  completed={done}
                  defaultOpen={phase.id <= 2}
                  rightSlot={
                    <span className="hidden border border-border bg-bg px-2 py-1 font-mono text-xs text-text-mute sm:inline">
                      {phase.checks.filter((check) => checked[check.id]).length}/
                      {phase.checks.length}
                    </span>
                  }
                >
                  <div className="grid gap-4">
                    {phase.timer ? (
                      <Timer
                        phaseId={phase.id}
                        label={phase.timer.label}
                        minutes={phase.timer.minutes}
                        sessionId={sessionId}
                        value={timers[phase.id]}
                        onChange={(timer) =>
                          setTimers((current) => ({
                            ...current,
                            [phase.id]: timer,
                          }))
                        }
                      />
                    ) : null}

                    {phase.warnings?.map((warning) => (
                      <WarningCallout
                        key={`${phase.id}-${warning.title}`}
                        warning={warning}
                      />
                    ))}

                    {phase.scheme ? <SchemeBlock scheme={phase.scheme} /> : null}

                    <div className="grid gap-2">
                      {phase.checks.map((check) => (
                        <CheckRow
                          key={check.id}
                          check={check}
                          checked={Boolean(checked[check.id])}
                          onToggle={(nextChecked) =>
                            toggleCheck(check.id, nextChecked)
                          }
                        />
                      ))}
                    </div>
                  </div>
                </PhaseCard>
              );
            })}
        </section>
      ))}
    </div>
  );
}
