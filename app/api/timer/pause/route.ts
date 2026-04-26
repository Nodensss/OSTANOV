import { NextRequest, NextResponse } from "next/server";
import { apiError, badRequest, notFound } from "@/lib/api";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  try {
    const body = (await request.json()) as {
      sessionId?: string;
      phaseId?: number;
      finished?: boolean;
      durationMs?: number;
    };

    if (!body.sessionId || typeof body.phaseId !== "number") {
      return badRequest("Нужны sessionId и phaseId");
    }

    const existing = await prisma.timerState.findUnique({
      where: {
        sessionId_phaseId: {
          sessionId: body.sessionId,
          phaseId: body.phaseId,
        },
      },
    });

    if (!existing) {
      return notFound("Таймер не найден");
    }

    const now = new Date();
    const extraMs = existing.startedAt
      ? now.getTime() - existing.startedAt.getTime()
      : 0;
    const nextElapsed = Math.max(0, existing.elapsedMs + extraMs);

    const timer = await prisma.timerState.update({
      where: { id: existing.id },
      data: {
        startedAt: null,
        pausedAt: now,
        elapsedMs:
          body.finished && typeof body.durationMs === "number"
            ? Math.max(body.durationMs, nextElapsed)
            : nextElapsed,
        finished: body.finished ?? existing.finished,
      },
    });

    return NextResponse.json({ timer });
  } catch (error) {
    return apiError(error, "Не удалось поставить таймер на паузу");
  }
}
