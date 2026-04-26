import algorithmData from "@/data/stop-algorithm.json";

export type CheckItem = {
  id: string;
  text: string;
  type: "check" | "param";
  param?: string;
};

export type WarningItem = {
  title: string;
  text: string;
};

export type Scheme = {
  title: string;
  lines: string[];
};

export type TimerSpec = {
  minutes: number;
  label: string;
};

export type Phase = {
  id: number;
  section: string;
  title: string;
  subtitle: string;
  condition?: string;
  checks: CheckItem[];
  warnings?: WarningItem[];
  positions: string[];
  timer?: TimerSpec;
  scheme?: Scheme;
};

export type CriticalRule = {
  id: string;
  title: string;
  rule: string;
  consequence: string;
};

export type TrainerQuestion = {
  id: string;
  phaseId: number | null;
  question: string;
  options: string[];
  correct: number;
  explanation: string;
};

export type StopAlgorithm = {
  title: string;
  subtitle: string;
  sources: string[];
  phases: Phase[];
  criticalRules: CriticalRule[];
  trainerQuestions: TrainerQuestion[];
};

export const algorithm = algorithmData as StopAlgorithm;
export const phases = algorithm.phases;
export const criticalRules = algorithm.criticalRules;
export const trainerQuestions = algorithm.trainerQuestions;
export const sections = Array.from(new Set(phases.map((phase) => phase.section)));

export const totalChecks = phases.reduce(
  (total, phase) => total + phase.checks.length,
  0,
);

export function phaseNumber(id: number) {
  return String(id).padStart(2, "0");
}

export function phaseLabel(phase: Phase) {
  return `Этап ${phase.id} · ${phase.section}`;
}

export function getPhaseTitle(phaseId: number | null) {
  if (phaseId === null) {
    return "Критические правила";
  }

  return phases.find((phase) => phase.id === phaseId)?.title ?? `Этап ${phaseId}`;
}
