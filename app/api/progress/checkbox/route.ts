import { NextRequest, NextResponse } from "next/server";
import { apiError, badRequest } from "@/lib/api";
import { ensureSession } from "@/lib/db";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function PATCH(request: NextRequest) {
  try {
    const body = (await request.json()) as {
      sessionId?: string;
      checkId?: string;
      checked?: boolean;
    };

    if (!body.sessionId || !body.checkId || typeof body.checked !== "boolean") {
      return badRequest("Нужны sessionId, checkId и checked");
    }

    await ensureSession(body.sessionId);

    const checkbox = await prisma.checkbox.upsert({
      where: {
        sessionId_checkId: {
          sessionId: body.sessionId,
          checkId: body.checkId,
        },
      },
      create: {
        sessionId: body.sessionId,
        checkId: body.checkId,
        checked: body.checked,
        checkedAt: body.checked ? new Date() : null,
      },
      update: {
        checked: body.checked,
        checkedAt: body.checked ? new Date() : null,
      },
      select: { checkId: true, checked: true, checkedAt: true },
    });

    return NextResponse.json({ checkbox });
  } catch (error) {
    return apiError(error, "Не удалось сохранить отметку");
  }
}
