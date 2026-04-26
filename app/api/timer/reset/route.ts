import { NextRequest, NextResponse } from "next/server";
import { apiError, badRequest } from "@/lib/api";
import { ensureSession } from "@/lib/db";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  try {
    const body = (await request.json()) as {
      sessionId?: string;
      phaseId?: number;
    };

    if (!body.sessionId || typeof body.phaseId !== "number") {
      return badRequest("Нужны sessionId и phaseId");
    }

    await ensureSession(body.sessionId);

    const timer = await prisma.timerState.upsert({
      where: {
        sessionId_phaseId: {
          sessionId: body.sessionId,
          phaseId: body.phaseId,
        },
      },
      create: {
        sessionId: body.sessionId,
        phaseId: body.phaseId,
        elapsedMs: 0,
      },
      update: {
        startedAt: null,
        pausedAt: null,
        elapsedMs: 0,
        finished: false,
      },
    });

    return NextResponse.json({ timer });
  } catch (error) {
    return apiError(error, "Не удалось сбросить таймер");
  }
}
