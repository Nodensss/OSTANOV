import { NextResponse } from "next/server";
import { databaseHint } from "@/lib/db-diagnostics";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function GET() {
  if (!process.env.DATABASE_URL) {
    return NextResponse.json(
      {
        ok: false,
        ...databaseHint(),
      },
      { status: 500 },
    );
  }

  try {
    await prisma.session.count();

    return NextResponse.json({
      ok: true,
      code: "DATABASE_OK",
      hint: "DATABASE_URL задан, подключение работает, таблицы Prisma найдены.",
    });
  } catch (error) {
    console.error(error);
    const details =
      typeof error === "object" && error !== null
        ? databaseHint(
            "code" in error ? String(error.code) : undefined,
            "message" in error ? String(error.message) : undefined,
          )
        : databaseHint();

    return NextResponse.json({ ok: false, ...details }, { status: 500 });
  }
}
