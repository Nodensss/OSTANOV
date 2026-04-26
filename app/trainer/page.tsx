import Link from "next/link";
import { History } from "lucide-react";
import { TrainerClient } from "@/components/trainer/TrainerClient";

export default function TrainerPage() {
  return (
    <div className="grid gap-5">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
            Проверка знания
          </p>
          <h1 className="mt-2 text-3xl font-semibold text-text">Тренажёр</h1>
        </div>
        <Link
          href="/trainer/history"
          className="inline-flex h-11 items-center gap-2 border border-border bg-surface px-4 text-sm font-semibold text-text-dim transition hover:border-cool hover:text-cool"
        >
          <History className="h-4 w-4" aria-hidden />
          История
        </Link>
      </div>
      <TrainerClient />
    </div>
  );
}
