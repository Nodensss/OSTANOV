export default function HealthPage() {
  return (
    <div className="grid gap-5">
      <div>
        <p className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
          Диагностика
        </p>
        <h1 className="mt-2 text-3xl font-semibold text-text">Проверка базы</h1>
      </div>

      <section className="grid gap-4 border border-border bg-surface p-5 shadow-panel">
        <p className="text-sm leading-6 text-text-dim">
          Откройте endpoint{" "}
          <a className="font-mono text-cool" href="/api/health/db">
            /api/health/db
          </a>
          . Он покажет, задан ли `DATABASE_URL`, доступна ли база и созданы ли
          таблицы Prisma.
        </p>
        <div className="border border-border bg-bg p-4 font-mono text-sm leading-6 text-text-dim">
          DATABASE_OK — всё подключено.
          <br />
          MISSING_DATABASE_URL — переменная не добавлена в Vercel.
          <br />
          TABLES_MISSING — нужно выполнить npm run prisma:push.
        </div>
      </section>
    </div>
  );
}
