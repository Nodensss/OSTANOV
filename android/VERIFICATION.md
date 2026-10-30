# Проверка Android-приложения

Полный JDK 17, SDK 35, Gradle 8.11.1. Локальная проверка 30.10.2026 UTC.

```text
$ cd android && gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
> Task :app:mergeProjectDexDebug
> Task :app:packageDebug
> Task :app:createDebugApkListingFileRedirect UP-TO-DATE
> Task :app:assembleDebug

> Task :app:lintReportDebug
Wrote HTML report to file:///workspace/OSTANOV/android/app/build/reports/lint-results-debug.html

> Task :app:lintDebug

BUILD SUCCESSFUL in 1m 57s
55 actionable tasks: 27 executed, 28 up-to-date
$ python3 android/check-apk.py
OK: APK 18422337 bytes (18.42 MB), no INTERNET in source/merged manifests
OK: packaged JSON matches source, SHA256 d98d691a69c8f781c3a8961ed7540b5ee0227cac41627edf4d36a1c744d7a013
$ python3 scripts/check-android-content.py
OK: текстов алгоритма в коде нет (проверено файлов: 37).
$ grep -c android.permission.INTERNET android/app/src/main/AndroidManifest.xml
0
$ grep -c android.permission.INTERNET android/app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml
0
JUnit XML: tests=8, failures=0, errors=0, skipped=0
Lint: 0 errors, 1 warnings
OK: workflow YAML, triggers, pinned Gradle, artifacts and main-only release
OK: all Appendix A build files match byte for byte
```

Предупреждение `ObsoleteSdkInt` оставлено: каталог `mipmap-anydpi-v26` прямо требуется разделом 5.4 ТЗ. Проверка не отключена.

Сборки каждого этапа 1–8 выполнены отдельно; этап 3 повторно прошёл после исправления тестового примера `PV-10603`, отсутствующего в реальном JSON. Все проверки контента и алгоритма используют неизменённый общий JSON.

Ветка основана на ещё не слитой ветке ТЗ. Поэтому сравнение с `origin/main` дополнительно включает исходные `AGENTS.md` и два `scripts/` из ТЗ; собственные изменения вне `android/` и `.github/` — только Android-раздел README. Веб-код и JSON не менялись.

На устройстве и эмуляторе не проверены UI, поворот, шрифт 200%, TalkBack, время холодного старта, таймеры при закрытом приложении и после перезагрузки, звук/вибрация и системный обмен. Подписанная release-сборка и публикация Release не выполнялись: ключа нет, публикация предусмотрена workflow после слияния в main. Сценарий приёмки — раздел 15.3 ТЗ.
