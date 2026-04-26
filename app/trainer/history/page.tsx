import { TrainerHistoryClient } from "@/components/trainer/TrainerHistoryClient";

export default function TrainerHistoryPage() {
  return (
    <div className="grid gap-5">
      <div>
        <p className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
          Результаты тренажёра
        </p>
        <h1 className="mt-2 text-3xl font-semibold text-text">История попыток</h1>
      </div>
      <TrainerHistoryClient />
    </div>
  );
}
