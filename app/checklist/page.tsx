import { ChecklistClient } from "@/components/checklist/ChecklistClient";

export default function ChecklistPage() {
  return (
    <div className="grid gap-5">
      <div>
        <p className="font-mono text-xs uppercase tracking-[0.18em] text-text-mute">
          Режим выполнения
        </p>
        <h1 className="mt-2 text-3xl font-semibold text-text">Чек-лист остановки</h1>
      </div>
      <ChecklistClient />
    </div>
  );
}
