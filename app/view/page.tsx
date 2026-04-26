import Link from "next/link";
import { ShieldAlert } from "lucide-react";
import { ViewClient } from "@/components/view/ViewClient";

export default function ViewPage() {
  return (
    <div className="grid gap-5">
      <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
            Справочный режим
          </p>
          <h1 className="mt-2 text-3xl font-semibold text-text">
            Просмотр алгоритма
          </h1>
        </div>
        <Link
          href="/view/critical-rules"
          className="inline-flex h-11 items-center gap-2 border border-danger/50 bg-danger/10 px-4 text-sm font-semibold text-text transition hover:border-danger hover:text-danger"
        >
          <ShieldAlert className="h-4 w-4" aria-hidden />
          Критические правила
        </Link>
      </div>
      <ViewClient />
    </div>
  );
}
