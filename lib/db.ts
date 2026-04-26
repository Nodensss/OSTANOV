import { prisma } from "@/lib/prisma";

export async function ensureSession(sessionId: string) {
  return prisma.session.upsert({
    where: { id: sessionId },
    update: {},
    create: { id: sessionId },
  });
}
