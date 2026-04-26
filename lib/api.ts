import { NextResponse } from "next/server";

export function badRequest(message: string) {
  return NextResponse.json({ error: message }, { status: 400 });
}

export function notFound(message: string) {
  return NextResponse.json({ error: message }, { status: 404 });
}

export function apiError(error: unknown, message: string) {
  console.error(error);
  return NextResponse.json({ error: message }, { status: 500 });
}
