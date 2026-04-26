import { AlertTriangle } from "lucide-react";
import type { WarningItem } from "@/lib/algorithm";

export function WarningCallout({ warning }: { warning: WarningItem }) {
  return (
    <div className="border border-warn/40 border-l-warn bg-warn/10 p-4">
      <div className="flex gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-warn" aria-hidden />
        <div>
          <p className="font-semibold text-text">{warning.title}</p>
          <p className="mt-1 text-sm leading-6 text-text-dim">{warning.text}</p>
        </div>
      </div>
    </div>
  );
}
