"use client";

import { useEffect, useState } from "react";
import { ensureSessionId } from "@/lib/session";
import { formatDateTime } from "@/lib/utils";

type Attempt = {
  id: string;
  startedAt: string;
  finishedAt: string | null;
  score: number | null;
  total: number;
  answers: unknown;
};

export function TrainerHistoryClient() {
  const [attempts, setAttempts] = useState<Attempt[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const sessionId = await ensureSessionId();
        const response = await fetch(`/api/trainer/history?sessionId=${sessionId}`, {
          cache: "no-store",
        });

        if (!response.ok) {
          throw new Error("history request failed");
        }

        const data = (await response.json()) as { attempts: Attempt[] };
        if (!cancelled) {
          setAttempts(data.attempts);
        }
      } catch {
        if (!cancelled) {
          setError("Не удалось загрузить историю попыток.");
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
    };
  }, []);

  if (loading) {
    return (
      <div className="border border-border bg-surface p-5 text-sm text-text-dim">
        Загрузка истории...
      </div>
    );
  }

  if (error) {
    return (
      <div className="border border-danger/50 bg-danger/10 p-5 text-sm text-text">
        {error}
      </div>
    );
  }

  if (attempts.length === 0) {
    return (
      <div className="border border-border bg-surface p-5 text-sm text-text-dim">
        История этой сессии пока пустая.
      </div>
    );
  }

  return (
    <div className="overflow-x-auto border border-border bg-surface shadow-panel">
      <table className="min-w-full divide-y divide-border text-left text-sm">
        <thead className="bg-surface-2 font-mono text-xs uppercase tracking-[0.16em] text-text-mute">
          <tr>
            <th className="px-4 py-3">Дата</th>
            <th className="px-4 py-3">Статус</th>
            <th className="px-4 py-3">Счёт</th>
            <th className="px-4 py-3">Процент</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {attempts.map((attempt) => {
            const percent =
              attempt.score === null || attempt.total === 0
                ? null
                : Math.round((attempt.score / attempt.total) * 100);

            return (
              <tr key={attempt.id} className="text-text-dim">
                <td className="whitespace-nowrap px-4 py-3 font-mono">
                  {formatDateTime(attempt.startedAt)}
                </td>
                <td className="px-4 py-3">
                  {attempt.finishedAt ? "Завершена" : "Не завершена"}
                </td>
                <td className="whitespace-nowrap px-4 py-3 font-mono text-text">
                  {attempt.score ?? "—"} / {attempt.total}
                </td>
                <td className="whitespace-nowrap px-4 py-3 font-mono text-cool">
                  {percent === null ? "—" : `${percent}%`}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
