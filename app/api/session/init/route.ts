import { NextRequest, NextResponse } from "next/server";
import { apiError } from "@/lib/api";
import { prisma } from "@/lib/prisma";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

export async function GET(request: NextRequest) {
  try {
    const requestedId = request.headers.get("x-session-id")?.trim();

    if (requestedId) {
      const session = await prisma.session.upsert({
        where: { id: requestedId },
        update: {},
        create: { id: requestedId },
      });

      return NextResponse.json({ sessionId: session.id });
    }

    const session = await prisma.session.create({ data: {} });
    return NextResponse.json({ sessionId: session.id });
  } catch (error) {
    return apiError(error, "Не удалось инициализировать сессию");
  }
}
