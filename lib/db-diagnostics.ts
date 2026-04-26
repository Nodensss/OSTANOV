export function databaseHint(code?: string, message?: string) {
  const text = message?.toLowerCase() ?? "";

  if (!process.env.DATABASE_URL) {
    return {
      code: "MISSING_DATABASE_URL",
      hint: "Добавьте DATABASE_URL в Vercel: Project Settings -> Environment Variables, затем сделайте Redeploy.",
    };
  }

  if (code === "P1000" || text.includes("authentication failed")) {
    return {
      code: code ?? "AUTH_FAILED",
      hint: "Логин или пароль в DATABASE_URL неверные. Скопируйте строку подключения заново из Neon.",
    };
  }

  if (code === "P1001" || text.includes("can't reach database")) {
    return {
      code: code ?? "DATABASE_UNREACHABLE",
      hint: "Vercel не может достучаться до базы. Проверьте host, sslmode=require и что Neon-проект активен.",
    };
  }

  if (code === "P2021" || text.includes("does not exist")) {
    return {
      code: code ?? "TABLES_MISSING",
      hint: "Подключение есть, но таблицы Prisma не созданы. Выполните локально npm run prisma:push с этим же DATABASE_URL.",
    };
  }

  return {
    code: code ?? "DATABASE_ERROR",
    hint: "Проверьте DATABASE_URL, выполните npm run prisma:push и посмотрите Function Logs в Vercel.",
  };
}
