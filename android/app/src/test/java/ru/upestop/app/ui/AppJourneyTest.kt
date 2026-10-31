package ru.upestop.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.upestop.app.MainActivity
import ru.upestop.app.domain.TestData
import java.io.File

/**
 * Сквозной сценарий по всем экранам на Robolectric: приложение целиком (Application, Room,
 * DataStore, навигация, экраны) на данных data/stop-algorithm.json. По пути сохраняются
 * скриншоты в app/build/screenshots/ — их выкладывает CI вместе с APK.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xhdpi")
class AppJourneyTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val algorithm = TestData.repositoryAlgorithm

    @Test
    fun fullJourneyThroughAllScreens() {
        // Главная: данные из JSON, счётчики, кнопка начала.
        waitForText(algorithm.title)
        waitForText("Начать остановку")
        screenshot("01-home")

        // Чек-лист: первый этап раскрыт, отметка операции меняет прогресс.
        tab("Чек-лист").performClick()
        val first = algorithm.phases.first().checks.first()
        waitForText(first.text)
        compose.onNodeWithText(first.text).performClick()
        waitForText("1 из ${algorithm.phases.sumOf { it.checks.size }} шагов")
        screenshot("02-checklist")

        // Таймер этапа: запуск переводит кнопку в «Пауза».
        val timerPhase = algorithm.phases.first { it.timer != null }
        compose.onNode(hasScrollToKeyAction()).performScrollToKey("phase:${timerPhase.id}")
        compose.onNodeWithText(timerPhase.title).performClick()
        waitForText("Запустить")
        compose.onNodeWithText("Запустить").performClick()
        waitForText("Пауза")
        screenshot("03-checklist-timer")

        // Пошаговый режим: курсор на следующей неотмеченной операции, «Выполнено» двигает дальше.
        compose.onNodeWithContentDescription("Пошаговый режим").performClick()
        val flat = algorithm.phases.flatMap { it.checks }
        waitForText(flat[1].text)
        screenshot("04-step")
        compose.onNodeWithText("Выполнено").performClick()
        waitForText(flat[2].text)
        compose.onNodeWithContentDescription("Назад").performClick()

        // Справочник: латинский запрос находит кириллическую позицию.
        tab("Справочник").performClick()
        waitForText("Поиск по позициям", ignoreCase = true)
        compose.onNode(hasSetTextAction()).performTextInput("a-401")
        waitForText("А-401")
        screenshot("05-reference-search")

        // Критические правила.
        compose.onNodeWithContentDescription("Критические правила").performClick()
        waitForText(algorithm.criticalRules.first().title)
        screenshot("06-rules")
        compose.onNodeWithContentDescription("Назад").performClick()

        // Тренажёр: старт набора, ответ, пояснение.
        tab("Тренажёр").performClick()
        waitForText("Начать")
        screenshot("07-trainer-setup")
        compose.onNodeWithText("Начать").performClick()
        waitForText("Вопрос 1 из ${algorithm.trainerQuestions.size}")
        val question = algorithm.trainerQuestions.first { exists(it.question) }
        compose.onNodeWithText(question.options[question.correct]).performScrollTo().performClick()
        waitForText("Верно")
        screenshot("08-trainer-answer")

        // Журнал остановки из меню чек-листа.
        tab("Чек-лист").performClick()
        compose.onNodeWithContentDescription("Ещё").performClick()
        compose.onNodeWithText("Журнал остановки").performClick()
        waitForText("ЖУРНАЛ ОСТАНОВКИ", substring = true)
        screenshot("09-journal")
        compose.onNodeWithContentDescription("Назад").performClick()

        // Главная с прогрессом и идущим таймером, затем настройки.
        tab("Главная").performClick()
        waitForText("Продолжить")
        screenshot("10-home-progress")
        compose.onNodeWithContentDescription("Настройки").performClick()
        waitForText("О приложении", ignoreCase = true)
        screenshot("11-settings")
    }

    /** Пункт нижней навигации (у него роль Tab, в отличие от одноимённых плиток и заголовков). */
    private fun tab(label: String) =
        compose.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    private fun exists(text: String, substring: Boolean = false, ignoreCase: Boolean = false) =
        compose.onAllNodesWithText(text, substring = substring, ignoreCase = ignoreCase).fetchSemanticsNodes().isNotEmpty()

    private fun waitForText(text: String, substring: Boolean = false, ignoreCase: Boolean = false) {
        compose.waitUntil(timeoutMillis = 15_000) { exists(text, substring, ignoreCase) }
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
