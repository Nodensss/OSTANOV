"use client";

import { Check, Minus } from "lucide-react";
import type { CheckItem } from "@/lib/algorithm";
import { cn } from "@/lib/utils";

export function CheckRow({
  check,
  checked,
  onToggle,
}: {
  check: CheckItem;
  checked: boolean;
  onToggle: (checked: boolean) => void;
}) {
  return (
    <label className="group grid cursor-pointer grid-cols-[28px_1fr] gap-3 border border-border bg-surface-2 p-3 transition hover:border-cool/60">
      <span
        className={cn(
          "mt-0.5 grid h-6 w-6 place-items-center border text-bg transition",
          checked ? "border-cool bg-cool" : "border-border bg-bg",
        )}
      >
        {checked ? <Check className="h-4 w-4" aria-hidden /> : null}
      </span>
      <span className="min-w-0">
        <input
          type="checkbox"
          className="sr-only"
          checked={checked}
          onChange={(event) => onToggle(event.target.checked)}
        />
        <span
          className={cn(
            "flex flex-col gap-2 text-sm leading-6 text-text sm:flex-row sm:items-start sm:justify-between",
            checked && "text-text-mute line-through",
          )}
        >
          <span className="flex min-w-0 gap-2">
            <Minus className="mt-2 h-3 w-3 shrink-0 text-cool" aria-hidden />
            <span>{check.text}</span>
          </span>
          {check.type === "param" && check.param ? (
            <span className="shrink-0 border border-amber-soft/30 bg-amber-soft/10 px-2 py-1 font-mono text-xs text-amber-soft no-underline">
              {check.param}
            </span>
          ) : null}
        </span>
      </span>
    </label>
  );
}
