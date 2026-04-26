"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  ClipboardCheck,
  Gauge,
  History,
  Home,
  ListChecks,
  ShieldAlert,
} from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/", label: "Меню", icon: Home },
  { href: "/checklist", label: "Чек-лист", icon: ClipboardCheck },
  { href: "/view", label: "Просмотр", icon: ListChecks },
  { href: "/view/critical-rules", label: "Правила", icon: ShieldAlert },
  { href: "/trainer", label: "Тренажёр", icon: Gauge },
  { href: "/trainer/history", label: "История", icon: History },
];

export function AppFrame({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-border bg-bg/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl flex-col gap-3 px-4 py-3 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between gap-4">
            <Link href="/" className="group flex min-w-0 items-center gap-3">
              <span className="grid h-10 w-10 shrink-0 place-items-center border border-cool/60 bg-cool/10 font-display text-sm text-cool">
                УПЭ
              </span>
              <span className="min-w-0">
                <span className="block truncate text-sm font-semibold uppercase tracking-[0.18em] text-text-mute">
                  Стоп-алгоритм
                </span>
                <span className="block truncate text-lg font-semibold text-text group-hover:text-cool">
                  Остановка технологической линии
                </span>
              </span>
            </Link>
            <div className="hidden rounded border border-border bg-surface px-3 py-2 font-mono text-xs text-text-dim sm:block">
              sessionId хранится локально, прогресс в БД
            </div>
          </div>
          <nav className="flex gap-2 overflow-x-auto pb-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const active =
                item.href === "/"
                  ? pathname === "/"
                  : pathname === item.href || pathname.startsWith(`${item.href}/`);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  title={item.label}
                  className={cn(
                    "inline-flex h-10 shrink-0 items-center gap-2 border px-3 text-sm font-medium transition",
                    active
                      ? "border-cool bg-cool text-bg"
                      : "border-border bg-surface text-text-dim hover:border-cool/70 hover:text-text",
                  )}
                >
                  <Icon className="h-4 w-4" aria-hidden />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        {children}
      </main>
    </div>
  );
}
