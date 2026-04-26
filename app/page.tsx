import Link from "next/link";
import {
  ArrowRight,
  ClipboardCheck,
  Gauge,
  ListChecks,
  ShieldAlert,
} from "lucide-react";
import {
  algorithm,
  criticalRules,
  phases,
  totalChecks,
  trainerQuestions,
} from "@/lib/algorithm";

export default function Home() {
  const modes = [
    {
      href: "/checklist",
      title: "Чек-лист",
      note: `${totalChecks} операций · таймеры этапов 9 и 10`,
      icon: ClipboardCheck,
    },
    {
      href: "/view",
      title: "Просмотр",
      note: `${phases.length} этапов · поиск по позициям`,
      icon: ListChecks,
    },
    {
      href: "/trainer",
      title: "Тренажёр",
      note: `${trainerQuestions.length} вопросов · история попыток`,
      icon: Gauge,
    },
  ];

  return (
    <div className="grid gap-6">
      <section className="grid gap-5 border border-border bg-surface/90 p-5 shadow-panel sm:p-6 lg:grid-cols-[1fr_360px]">
        <div className="min-w-0">
          <p className="mb-3 font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
            Полиэтилен высокого давления · УПЭ · ТНХК
          </p>
          <h1 className="max-w-4xl text-3xl font-semibold text-text sm:text-5xl">
            {algorithm.title}
          </h1>
          <p className="mt-4 max-w-3xl text-base leading-7 text-text-dim">
            Цифровой рабочий инструмент для выполнения, просмотра и проверки
            знания алгоритма остановки. Содержимое берётся из
            <span className="font-mono text-cool"> data/stop-algorithm.json</span>.
          </p>
        </div>
        <div className="grid content-start gap-3 border border-border bg-surface-2 p-4">
          <div className="flex items-start gap-3">
            <ShieldAlert className="mt-1 h-5 w-5 shrink-0 text-warn" aria-hidden />
            <div>
              <p className="font-semibold text-text">Только утверждённые данные</p>
              <p className="mt-1 text-sm leading-6 text-text-dim">
                Интерфейс не добавляет процедурные шаги: этапы, параметры,
                позиции и вопросы загружаются из JSON.
              </p>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-2 pt-2">
            <Metric value={phases.length} label="этапов" />
            <Metric value={criticalRules.length} label="правил" />
            <Metric value={trainerQuestions.length} label="вопросов" />
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        {modes.map((mode) => {
          const Icon = mode.icon;

          return (
            <Link
              key={mode.href}
              href={mode.href}
              className="group grid min-h-52 content-between border border-border bg-surface p-5 transition hover:border-cool hover:bg-surface-2"
            >
              <div>
                <div className="mb-5 flex items-center justify-between gap-3">
                  <span className="grid h-12 w-12 place-items-center border border-cool/50 bg-cool/10 text-cool">
                    <Icon className="h-6 w-6" aria-hidden />
                  </span>
                  <ArrowRight
                    className="h-5 w-5 text-text-mute transition group-hover:translate-x-1 group-hover:text-cool"
                    aria-hidden
                  />
                </div>
                <h2 className="text-2xl font-semibold text-text">{mode.title}</h2>
                <p className="mt-3 text-sm leading-6 text-text-dim">{mode.note}</p>
              </div>
              <span className="mt-6 font-mono text-xs uppercase tracking-[0.18em] text-text-mute group-hover:text-cool">
                Открыть режим
              </span>
            </Link>
          );
        })}
      </section>
    </div>
  );
}

function Metric({ value, label }: { value: number; label: string }) {
  return (
    <div className="border border-border bg-bg/40 p-3">
      <div className="font-display text-2xl text-cool">{value}</div>
      <div className="mt-1 font-mono text-[10px] uppercase tracking-[0.16em] text-text-mute">
        {label}
      </div>
    </div>
  );
}
