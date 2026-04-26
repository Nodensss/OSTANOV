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

    const attempts = await prisma.trainerAttempt.findMany({
      where: { sessionId },
      orderBy: { startedAt: "desc" },
      select: {
        id: true,
        startedAt: true,
        finishedAt: true,
        score: true,
        total: true,
        answers: true,
      },
    });

    return NextResponse.json({ attempts });
  } catch (error) {
    return apiError(error, "Не удалось загрузить историю");
  }
}
