"use client";

const SESSION_KEY = "upe-stop-session-id";

export async function ensureSessionId() {
  const stored = window.localStorage.getItem(SESSION_KEY);
  const response = await fetch("/api/session/init", {
    headers: stored ? { "x-session-id": stored } : undefined,
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error("Не удалось создать или получить рабочую сессию");
  }

  const data = (await response.json()) as { sessionId: string };
  window.localStorage.setItem(SESSION_KEY, data.sessionId);
  return data.sessionId;
}

export function getStoredSessionId() {
  return window.localStorage.getItem(SESSION_KEY);
}
