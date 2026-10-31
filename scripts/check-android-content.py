#!/usr/bin/env python3
"""Проверяет, что тексты алгоритма не захардкожены в исходниках Android-приложения.

Содержание (операции, этапы, правила, вопросы) должно браться только из
data/stop-algorithm.json. Скрипт ищет эти тексты в android/app/src/main
(*.kt, *.xml) и завершается с кодом 1, если что-то найдено.

Запуск из корня репозитория: python3 scripts/check-android-content.py
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / "data" / "stop-algorithm.json"
SOURCES = ROOT / "android" / "app" / "src" / "main"
# Короткие строки («Подготовка», «20%») могут законно совпасть с UI-текстом.
MIN_LENGTH = 16


def algorithm_texts(data):
    for phase in data["phases"]:
        yield phase["title"]
        yield phase["subtitle"]
        if phase.get("condition"):
            yield phase["condition"]
        for check in phase["checks"]:
            yield check["text"]
        for warning in phase.get("warnings") or []:
            yield warning["text"]
        if phase.get("timer"):
            yield phase["timer"]["label"]
    for rule in data["criticalRules"]:
        yield rule["rule"]
        yield rule["consequence"]
    for question in data["trainerQuestions"]:
        yield question["question"]
        yield question["explanation"]


def main():
    data = json.loads(DATA.read_text(encoding="utf-8"))
    files = [p for p in SOURCES.rglob("*") if p.suffix in {".kt", ".xml"}] if SOURCES.exists() else []
    if not files:
        print(f"Нет исходников в {SOURCES.relative_to(ROOT)} — проверять нечего.")
        return 0

    texts = {t for t in algorithm_texts(data) if t and len(t) >= MIN_LENGTH}
    leaks = []
    for path in files:
        content = path.read_text(encoding="utf-8", errors="ignore")
        leaks.extend((path, text) for text in texts if text in content)

    if leaks:
        print("Найдены тексты алгоритма в исходниках (должны браться из JSON):")
        for path, text in leaks:
            print(f"  {path.relative_to(ROOT)}: {text}")
        return 1

    print(f"OK: текстов алгоритма в коде нет (проверено файлов: {len(files)}).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
