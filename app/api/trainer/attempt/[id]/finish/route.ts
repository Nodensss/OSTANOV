import { NextRequest, NextResponse } from "next/server";
import { Prisma } from "@prisma/client";
import { apiError, badRequest, notFound } from "@/lib/api";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } },
) {
  try {
    const body = (await request.json()) as {
      score?: number;
      total?: number;
      answers?: unknown[];
    };

    if (typeof body.score !== "number" || typeof body.total !== "number") {
      return badRequest("Нужны score и total");
    }

    const attempt = await prisma.trainerAttempt.findUnique({
      where: { id: params.id },
    });

    if (!attempt) {
      return notFound("Попытка не найдена");
    }

    const nextAnswers = Array.isArray(body.answers)
      ? body.answers
      : Array.isArray(attempt.answers)
        ? attempt.answers
        : [];

    const updated = await prisma.trainerAttempt.update({
      where: { id: params.id },
      data: {
        score: body.score,
        total: body.total,
        answers: nextAnswers as Prisma.InputJsonValue,
        finishedAt: new Date(),
      },
    });

    return NextResponse.json({ attempt: updated });
  } catch (error) {
    return apiError(error, "Не удалось завершить попытку");
  }
}
