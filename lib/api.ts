import { NextResponse } from "next/server";
import { databaseHint } from "@/lib/db-diagnostics";

export function badRequest(message: string) {
  return NextResponse.json({ error: message }, { status: 400 });
}

export function notFound(message: string) {
  return NextResponse.json({ error: message }, { status: 404 });
}

export function apiError(error: unknown, message: string) {
  console.error(error);
  const details =
    typeof error === "object" && error !== null
      ? databaseHint(
          "code" in error ? String(error.code) : undefined,
          "message" in error ? String(error.message) : undefined,
        )
      : databaseHint();

  return NextResponse.json({ error: message, ...details }, { status: 500 });
}
