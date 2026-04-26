"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Pause, Play, RotateCcw } from "lucide-react";
import { cn } from "@/lib/utils";

const BEEP_SRC =
  "data:audio/wav;base64,UklGRsQFAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YaAFAACArcW9mmpFO1B8qcS/nm9HOk13psPBonNKOkpzosHDpndNOkdvnr/EqXxQO0Vqmr3FrYBTO0NmlrvFsIRXPEFikbnGs4laPT9ejbbGto1ePz1aibPGuZFiQTxXhLDFu5ZmQztTgK3FvZpqRTtQfKnEv55vRzpNd6bDwaJzSjpKc6LBw6Z3TTpHb56/xKl8UDtFapq9xa2AUztDZpa7xbCEVzxBYpG5xrOJWj0/Xo22xraNXj89WomzxrmRYkE8V4SwxbuWZkM7U4Ctxb2aakU7UHypxL+eb0c6TXemw8Gic0o6SnOiwcOmd006R2+ev8SpfFA7RWqavcWtgFM7Q2aWu8WwhFc8QWKRucaziVo9P16Ntsa2jV4/PVqJs8a5kWJBPFeEsMW7lmZDO1OArcW9mmpFO1B8qcS/nm9HOk13psPBonNKOkpzosHDpndNOkdvnr/EqXxQO0Vqmr3FrYBTO0NmlrvFsIRXPEFikbnGs4laPT9ejbbGto1ePz1aibPGuZFiQTxXhLDFu5ZmQztTgK3FvZpqRTtQfKnEv55vRzpNd6bDwaJzSjpKc6LBw6Z3TTpHb56/xKl8UDtFapq9xa2AUztDZpa7xbCEVzxBYpG5xrOJWj0/Xo22xraNXj89WomzxrmRYkE8V4SwxbuWZkM7U4Ctxb2aakU7UHypxL+eb0c6TXemw8Gic0o6SnOiwcOmd006R2+ev8Q=";

export type TimerSnapshot = {
  phaseId: number;
  startedAt: string | null;
  pausedAt: string | null;
  elapsedMs: number;
  finished: boolean;
};

type TimerResponse = {
  timer: TimerSnapshot;
};

export function Timer({
  phaseId,
  label,
  minutes,
  sessionId,
  value,
  onChange,
}: {
  phaseId: number;
  label: string;
  minutes: number;
  sessionId: string | null;
  value?: TimerSnapshot;
  onChange: (timer: TimerSnapshot) => void;
}) {
  const durationMs = minutes * 60 * 1000;
  const [now, setNow] = useState(Date.now());
  const [busy, setBusy] = useState(false);
  const completedRef = useRef(false);
  const timer = value ?? {
    phaseId,
    startedAt: null,
    pausedAt: null,
    elapsedMs: 0,
    finished: false,
  };
  const elapsedMs = getElapsedMs(timer, now);
  const remainingMs = Math.max(0, durationMs - elapsedMs);
  const progress = Math.min(100, (elapsedMs / durationMs) * 100);
  const running = Boolean(timer.startedAt) && !timer.finished;
  const display = formatTime(remainingMs);

  useEffect(() => {
    const id = window.setInterval(() => setNow(Date.now()), 500);
    return () => window.clearInterval(id);
  }, []);

  useEffect(() => {
    completedRef.current = timer.finished;
  }, [timer.finished]);

  const ringStyle = useMemo(
    () => ({
      background: `conic-gradient(#60a5fa ${progress * 3.6}deg, #1a2125 0deg)`,
    }),
    [progress],
  );

  const requestTimer = useCallback(
    async (endpoint: "start" | "pause" | "reset", extra = {}) => {
    if (!sessionId) {
      return;
    }

    setBusy(true);
    try {
      const response = await fetch(`/api/timer/${endpoint}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId, phaseId, ...extra }),
      });

      if (!response.ok) {
        throw new Error("timer request failed");
      }

      const data = (await response.json()) as TimerResponse;
      onChange(normalizeTimer(data.timer));
    } finally {
      setBusy(false);
    }
  },
    [onChange, phaseId, sessionId],
  );

  const completeTimer = useCallback(async () => {
    playBeep();
    await requestTimer("pause", { finished: true, durationMs });
  }, [durationMs, requestTimer]);

  useEffect(() => {
    if (!running || remainingMs > 0 || completedRef.current) {
      return;
    }

    completedRef.current = true;
    void completeTimer();
  }, [completeTimer, remainingMs, running]);

  return (
    <div
      className={cn(
        "grid gap-4 border border-cool/40 bg-cool/10 p-4 sm:grid-cols-[1fr_auto]",
        timer.finished && "animate-pulse border-danger/60 bg-danger/10",
      )}
    >
      <div>
        <p className="font-mono text-xs uppercase tracking-[0.18em] text-cool">
          {label}
        </p>
        <div className="mt-3 flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={() => requestTimer(running ? "pause" : "start")}
            disabled={busy || !sessionId}
            className="inline-flex h-11 items-center gap-2 border border-cool bg-cool px-4 text-sm font-semibold text-bg transition hover:bg-steel disabled:cursor-not-allowed disabled:opacity-50"
          >
            {running ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
            {running ? "Пауза" : "Запустить таймер"}
          </button>
          <button
            type="button"
            onClick={() => requestTimer("reset")}
            disabled={busy || !sessionId}
            title="Сбросить таймер"
            className="grid h-11 w-11 place-items-center border border-border bg-surface text-text-dim transition hover:border-cool hover:text-cool disabled:cursor-not-allowed disabled:opacity-50"
          >
            <RotateCcw className="h-4 w-4" aria-hidden />
          </button>
          <span className="font-mono text-xs text-text-mute">
            План: {minutes} мин.
          </span>
        </div>
      </div>
      <div className="flex items-center gap-4">
        <div
          className="grid h-24 w-24 place-items-center rounded-full p-1"
          style={ringStyle}
        >
          <div className="grid h-full w-full place-items-center rounded-full bg-bg">
            <span
              className={cn(
                "font-mono text-xl font-semibold text-text",
                timer.finished && "text-danger",
              )}
            >
              {display}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

function normalizeTimer(timer: TimerSnapshot): TimerSnapshot {
  return {
    ...timer,
    startedAt: timer.startedAt ? new Date(timer.startedAt).toISOString() : null,
    pausedAt: timer.pausedAt ? new Date(timer.pausedAt).toISOString() : null,
  };
}

function getElapsedMs(timer: TimerSnapshot, now: number) {
  if (timer.startedAt && !timer.finished) {
    return timer.elapsedMs + Math.max(0, now - new Date(timer.startedAt).getTime());
  }

  return timer.elapsedMs;
}

function formatTime(ms: number) {
  const totalSeconds = Math.ceil(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function playBeep() {
  try {
    const audio = new Audio(BEEP_SRC);
    void audio.play();
  } catch {
    // Browser audio policies can block playback before user interaction.
  }
}
