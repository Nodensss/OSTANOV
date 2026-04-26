import { NextRequest, NextResponse } from "next/server";
import { apiError, badRequest } from "@/lib/api";
import { ensureSession } from "@/lib/db";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function GET(request: NextRequest) {
  const sessionId = request.nextUrl.searchParams.get("sessionId");

  if (!sessionId) {
    return badRequest("sessionId обязателен");
  }

  try {
    await ensureSession(sessionId);

    const [checkboxes, timers] = await prisma.$transaction([
      prisma.checkbox.findMany({
        where: { sessionId },
        select: { checkId: true, checked: true, checkedAt: true },
      }),
      prisma.timerState.findMany({
        where: { sessionId },
        select: {
          phaseId: true,
          startedAt: true,
          pausedAt: true,
          elapsedMs: true,
          finished: true,
        },
      }),
    ]);

    return NextResponse.json({ checkboxes, timers });
  } catch (error) {
    return apiError(error, "Не удалось загрузить прогресс");
  }
}

export async function DELETE(request: NextRequest) {
  const sessionId = request.nextUrl.searchParams.get("sessionId");

  if (!sessionId) {
    return badRequest("sessionId обязателен");
  }

  try {
    await prisma.$transaction([
      prisma.checkbox.deleteMany({ where: { sessionId } }),
      prisma.timerState.deleteMany({ where: { sessionId } }),
    ]);

    return NextResponse.json({ ok: true });
  } catch (error) {
    return apiError(error, "Не удалось сбросить прогресс");
  }
}
