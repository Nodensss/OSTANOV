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
      total?: number;
    };

    if (!body.sessionId || typeof body.total !== "number") {
      return badRequest("Нужны sessionId и total");
    }

    await ensureSession(body.sessionId);

    const attempt = await prisma.trainerAttempt.create({
      data: {
        sessionId: body.sessionId,
        total: body.total,
        answers: [],
      },
      select: { id: true, startedAt: true, total: true },
    });

    return NextResponse.json({ attempt });
  } catch (error) {
    return apiError(error, "Не удалось создать попытку");
  }
}
