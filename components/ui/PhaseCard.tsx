"use client";

import { useState } from "react";
import { ChevronDown } from "lucide-react";
import type { Phase } from "@/lib/algorithm";
import { phaseLabel, phaseNumber } from "@/lib/algorithm";
import { cn } from "@/lib/utils";

export function PhaseCard({
  phase,
  completed,
  defaultOpen = false,
  children,
  rightSlot,
}: {
  phase: Phase;
  completed?: boolean;
  defaultOpen?: boolean;
  children: React.ReactNode;
  rightSlot?: React.ReactNode;
}) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <article className="border border-border bg-surface shadow-panel">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="grid w-full grid-cols-[56px_1fr_auto] items-center gap-4 p-4 text-left sm:grid-cols-[76px_1fr_auto] sm:p-5"
      >
        <span
          className={cn(
            "font-display text-4xl leading-none text-cool sm:text-5xl",
            completed && "text-safe",
          )}
        >
          {phaseNumber(phase.id)}
        </span>
        <span className="min-w-0">
          <span className="block truncate font-mono text-[10px] uppercase tracking-[0.2em] text-text-mute">
            {phaseLabel(phase)}
          </span>
          <span
            className={cn(
              "mt-1 block text-lg font-semibold text-text sm:text-xl",
              completed && "text-safe",
            )}
          >
            {phase.title}
          </span>
          {phase.condition ? (
            <span className="mt-2 block font-mono text-xs text-amber-soft">
              {phase.condition}
            </span>
          ) : null}
        </span>
        <span className="flex items-center gap-2">
          {rightSlot}
          <ChevronDown
            className={cn("h-5 w-5 text-text-mute transition", open && "rotate-180")}
            aria-hidden
          />
        </span>
      </button>
      {open ? <div className="border-t border-border p-4 sm:p-5">{children}</div> : null}
    </article>
  );
}
