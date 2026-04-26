"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { ArrowRight, History, RotateCcw } from "lucide-react";
import {
  getPhaseTitle,
  phases,
  trainerQuestions,
  type TrainerQuestion,
} from "@/lib/algorithm";
import { ensureSessionId } from "@/lib/session";
import { cn, shuffle } from "@/lib/utils";
import { ProgressBar } from "@/components/ui/ProgressBar";

type AnswerRecord = {
  questionId: string;
  selectedIndex: number;
  correct: boolean;
};

export function TrainerClient() {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [attemptId, setAttemptId] = useState<string | null>(null);
  const [phaseFilter, setPhaseFilter] = useState("all");
  const [questions, setQuestions] = useState<TrainerQuestion[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState<AnswerRecord[]>([]);
  const [status, setStatus] = useState<"ready" | "active" | "finished">("ready");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    ensureSessionId()
      .then((id) => {
        if (!cancelled) {
          setSessionId(id);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError("Нет соединения с БД. Проверьте DATABASE_URL.");
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const questionPool = useMemo(() => {
    if (phaseFilter === "all") {
      return trainerQuestions;
    }

    return trainerQuestions.filter(
      (question) => String(question.phaseId) === phaseFilter,
    );
  }, [phaseFilter]);

  const currentQuestion = questions[currentIndex];
  const currentAnswer = currentQuestion
    ? answers.find((answer) => answer.questionId === currentQuestion.id)
    : undefined;
  const score = answers.filter((answer) => answer.correct).length;
  const progress = questions.length ? (answers.length / questions.length) * 100 : 0;

  async function startAttempt() {
    if (!sessionId || questionPool.length === 0) {
      return;
    }

    setBusy(true);
    setError(null);

    try {
      const orderedQuestions = shuffle(questionPool);
      const response = await fetch("/api/trainer/attempt", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ sessionId, total: orderedQuestions.length }),
      });

      if (!response.ok) {
        throw new Error("attempt create failed");
      }

      const data = (await response.json()) as { attempt: { id: string } };
      setAttemptId(data.attempt.id);
      setQuestions(orderedQuestions);
      setAnswers([]);
      setCurrentIndex(0);
      setStatus("active");
    } catch {
      setError("Не удалось начать попытку. Проверьте подключение к БД.");
    } finally {
      setBusy(false);
    }
  }

  async function answerQuestion(selectedIndex: number) {
    if (!currentQuestion || currentAnswer) {
      return;
    }

    const record = {
      questionId: currentQuestion.id,
      selectedIndex,
      correct: selectedIndex === currentQuestion.correct,
    };

    setAnswers((current) => [...current, record]);

    if (!attemptId) {
      return;
    }

    const response = await fetch(`/api/trainer/attempt/${attemptId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(record),
    });

    if (!response.ok) {
      setError("Ответ показан, но не сохранился в историю.");
    }
  }

  async function finishAttempt(finalAnswers: AnswerRecord[]) {
    if (!attemptId) {
      setStatus("finished");
      return;
    }

    const finalScore = finalAnswers.filter((answer) => answer.correct).length;
    const response = await fetch(`/api/trainer/attempt/${attemptId}/finish`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        score: finalScore,
        total: questions.length,
        answers: finalAnswers,
      }),
    });

    if (!response.ok) {
      setError("Итог показан, но история попытки не обновилась.");
    }

    setStatus("finished");
  }

  function nextQuestion() {
    if (!currentAnswer) {
      return;
    }

    if (currentIndex >= questions.length - 1) {
      void finishAttempt(answers);
      return;
    }

    setCurrentIndex((index) => index + 1);
  }

  function restart() {
    setStatus("ready");
    setAttemptId(null);
    setQuestions([]);
    setAnswers([]);
    setCurrentIndex(0);
    setError(null);
  }

  if (status === "finished") {
    const percent = questions.length ? Math.round((score / questions.length) * 100) : 0;

    return (
      <div className="grid gap-5">
        <section className="border border-border bg-surface p-5 shadow-panel">
          <p className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
            Итог тренажёра
          </p>
          <h2 className="mt-3 text-4xl font-semibold text-text">
            {score} из {questions.length} правильно — {percent}%
          </h2>
          <div className="mt-5 flex flex-wrap gap-2">
            <button
              type="button"
              onClick={restart}
              className="inline-flex h-11 items-center gap-2 border border-cool bg-cool px-4 text-sm font-semibold text-bg transition hover:bg-steel"
            >
              <RotateCcw className="h-4 w-4" aria-hidden />
              Пройти заново
            </button>
            <Link
              href="/trainer/history"
              className="inline-flex h-11 items-center gap-2 border border-border bg-surface-2 px-4 text-sm font-semibold text-text-dim transition hover:border-cool hover:text-cool"
            >
              <History className="h-4 w-4" aria-hidden />
              История попыток
            </Link>
          </div>
        </section>
      </div>
    );
  }

  if (status === "ready") {
    return (
      <div className="grid gap-5">
        <section className="grid gap-4 border border-border bg-surface p-5 shadow-panel">
          <label className="grid gap-2">
            <span className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
              Набор вопросов
            </span>
            <select
              value={phaseFilter}
              onChange={(event) => setPhaseFilter(event.target.value)}
              className="h-12 border border-border bg-bg px-3 text-text outline-none focus:border-cool"
            >
              <option value="all">Все вопросы</option>
              {phases.map((phase) => (
                <option key={phase.id} value={phase.id}>
                  Только этап {phase.id}
                </option>
              ))}
            </select>
          </label>

          <div className="grid gap-2 border border-border bg-surface-2 p-4">
            <p className="text-sm leading-6 text-text-dim">
              В выбранном наборе:{" "}
              <span className="font-mono text-cool">{questionPool.length}</span>{" "}
              вопросов.
            </p>
            {phaseFilter !== "all" ? (
              <p className="text-sm leading-6 text-text-dim">
                {getPhaseTitle(Number(phaseFilter))}
              </p>
            ) : null}
          </div>

          {error ? (
            <div className="border border-danger/50 bg-danger/10 p-4 text-sm text-text">
              {error}
            </div>
          ) : null}

          <button
            type="button"
            onClick={startAttempt}
            disabled={!sessionId || busy || questionPool.length === 0}
            className="inline-flex h-12 w-fit items-center gap-2 border border-cool bg-cool px-5 text-sm font-semibold text-bg transition hover:bg-steel disabled:cursor-not-allowed disabled:opacity-50"
          >
            Начать
            <ArrowRight className="h-4 w-4" aria-hidden />
          </button>
        </section>
      </div>
    );
  }

  return (
    <div className="grid gap-5">
      <section className="border border-border bg-surface p-4 shadow-panel sm:p-5">
        <ProgressBar
          value={progress}
          label={`Вопрос ${currentIndex + 1} из ${questions.length}`}
        />
      </section>

      {error ? (
        <div className="border border-danger/50 bg-danger/10 p-4 text-sm text-text">
          {error}
        </div>
      ) : null}

      {currentQuestion ? (
        <section className="grid gap-5 border border-border bg-surface p-5 shadow-panel">
          <div>
            <p className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
              {getPhaseTitle(currentQuestion.phaseId)}
            </p>
            <h2 className="mt-3 text-2xl font-semibold leading-snug text-text">
              {currentQuestion.question}
            </h2>
          </div>

          <div className="grid gap-2">
            {currentQuestion.options.map((option, index) => {
              const selected = currentAnswer?.selectedIndex === index;
              const correct = currentQuestion.correct === index;
              const revealed = Boolean(currentAnswer);

              return (
                <button
                  key={`${currentQuestion.id}-${option}`}
                  type="button"
                  onClick={() => answerQuestion(index)}
                  disabled={revealed}
                  className={cn(
                    "border p-4 text-left text-sm leading-6 transition",
                    !revealed &&
                      "border-border bg-surface-2 text-text hover:border-cool",
                    revealed &&
                      correct &&
                      "border-safe bg-safe/15 text-text",
                    revealed &&
                      selected &&
                      !correct &&
                      "border-danger bg-danger/15 text-text",
                    revealed && !selected && !correct && "border-border bg-bg text-text-mute",
                  )}
                >
                  <span className="font-mono text-cool">
                    {String.fromCharCode(65 + index)}
                  </span>{" "}
                  {option}
                </button>
              );
            })}
          </div>

          {currentAnswer ? (
            <div className="grid gap-4">
              <div className="border border-border bg-surface-2 p-4 text-sm leading-6 text-text-dim">
                {currentQuestion.explanation}
              </div>
              <button
                type="button"
                onClick={nextQuestion}
                className="inline-flex h-11 w-fit items-center gap-2 border border-cool bg-cool px-4 text-sm font-semibold text-bg transition hover:bg-steel"
              >
                {currentIndex >= questions.length - 1 ? "Завершить" : "Дальше"}
                <ArrowRight className="h-4 w-4" aria-hidden />
              </button>
            </div>
          ) : null}
        </section>
      ) : null}
    </div>
  );
}
