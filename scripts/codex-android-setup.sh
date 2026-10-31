#!/usr/bin/env bash
# Подготовка окружения для сборки Android-приложения из каталога android/.
# Предназначен для setup-скрипта среды Codex, но работает на любой Linux-машине.
#
# Ставит (если ещё не стоит): JDK 17+, Android SDK (cmdline-tools, platform 35,
# build-tools 35.0.0) и Gradle 8.11.1. Скрипт идемпотентен — повторный запуск
# ничего не ломает.
#
# В Codex переменные окружения из setup-скрипта не доживают до сессии агента,
# поэтому ANDROID_HOME и PATH дописываются в ~/.bashrc.
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_BUILD="11076708"
GRADLE_VERSION="8.11.1"
GRADLE_HOME_DIR="$HOME/gradle-$GRADLE_VERSION"
SDK_PACKAGES=("platform-tools" "platforms;android-35" "build-tools;35.0.0")
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SUDO=""
if [ "$(id -u)" -ne 0 ] && command -v sudo >/dev/null 2>&1; then
  SUDO="sudo"
fi

APT_UPDATED=0
apt_install() {
  if [ "$APT_UPDATED" -eq 0 ]; then
    $SUDO apt-get update -y
    APT_UPDATED=1
  fi
  DEBIAN_FRONTEND=noninteractive $SUDO apt-get install -y --no-install-recommends "$@"
}

java_major() {
  java -version 2>&1 | awk -F'"' '/version "/ {split($2, v, "."); print (v[1] == "1" ? v[2] : v[1]); exit}'
}

echo "==> Проверка утилит"
for tool in curl unzip; do
  command -v "$tool" >/dev/null 2>&1 || apt_install "$tool"
done

echo "==> JDK"
if ! command -v java >/dev/null 2>&1 || [ "$(java_major)" -lt 17 ]; then
  apt_install openjdk-17-jdk-headless
fi
java -version 2>&1 | grep 'version "'

echo "==> Android SDK в $ANDROID_HOME"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf "$tmp"
fi
yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" "${SDK_PACKAGES[@]}" >/dev/null

echo "==> Gradle $GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME_DIR/bin/gradle" ]; then
  tmp="$(mktemp -d)"
  curl -fsSL -o "$tmp/gradle.zip" \
    "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  unzip -q "$tmp/gradle.zip" -d "$HOME"
  rm -rf "$tmp"
fi

echo "==> Переменные окружения в ~/.bashrc"
PROFILE_LINES=(
  "export ANDROID_HOME=\"$ANDROID_HOME\""
  "export ANDROID_SDK_ROOT=\"$ANDROID_HOME\""
  "export PATH=\"$GRADLE_HOME_DIR/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:\$PATH\""
)
touch "$HOME/.bashrc"
for line in "${PROFILE_LINES[@]}"; do
  grep -qxF "$line" "$HOME/.bashrc" || echo "$line" >> "$HOME/.bashrc"
done
export ANDROID_HOME ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$GRADLE_HOME_DIR/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# Если Android-проект уже есть — прописываем SDK и прогреваем кэш зависимостей,
# чтобы агент мог собирать проект даже без доступа в интернет.
if [ -f "$REPO_ROOT/android/settings.gradle.kts" ]; then
  echo "==> Прогрев кэша Gradle для android/"
  echo "sdk.dir=$ANDROID_HOME" > "$REPO_ROOT/android/local.properties"
  (cd "$REPO_ROOT/android" && gradle --no-daemon testDebugUnitTest lintDebug assembleDebug) \
    || echo "Предупреждение: прогревочная сборка не прошла — это не мешает агенту продолжить работу."
fi

echo "==> Готово"
gradle --version | grep -E '^Gradle '
echo "ANDROID_HOME=$ANDROID_HOME"
