import { NextRequest, NextResponse } from "next/server";
import { Prisma } from "@prisma/client";
import { apiError, badRequest, notFound } from "@/lib/api";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

type AnswerRecord = {
  questionId: string;
  selectedIndex: number;
  correct: boolean;
};

export async function PATCH(
  request: NextRequest,
  { params }: { params: { id: string } },
) {
  try {
    const body = (await request.json()) as Partial<AnswerRecord>;

    if (
      !body.questionId ||
      typeof body.selectedIndex !== "number" ||
      typeof body.correct !== "boolean"
    ) {
      return badRequest("Нужны questionId, selectedIndex и correct");
    }

    const attempt = await prisma.trainerAttempt.findUnique({
      where: { id: params.id },
    });

    if (!attempt) {
      return notFound("Попытка не найдена");
    }

    const answers = Array.isArray(attempt.answers)
      ? (attempt.answers as AnswerRecord[])
      : [];
    const nextAnswers = [
      ...answers.filter((answer) => answer.questionId !== body.questionId),
      {
        questionId: body.questionId,
        selectedIndex: body.selectedIndex,
        correct: body.correct,
      },
    ];

    const updated = await prisma.trainerAttempt.update({
      where: { id: params.id },
      data: { answers: nextAnswers as Prisma.InputJsonValue },
      select: { id: true, answers: true },
    });

    return NextResponse.json({ attempt: updated });
  } catch (error) {
    return apiError(error, "Не удалось сохранить ответ");
  }
}
