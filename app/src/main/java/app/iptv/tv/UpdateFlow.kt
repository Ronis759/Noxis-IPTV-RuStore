package app.iptv.tv

import android.app.Activity
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Версия для RuStore: приложение не скачивает и не устанавливает APK (магазин это запрещает),
 * поэтому проверки обновлений и разрешения REQUEST_INSTALL_PACKAGES здесь нет.
 * Вместо установки показывается подсказка «Обновите через RuStore».
 */
class UpdateFlow(
    private val a: Activity,
    private val prefs: SharedPreferences,
    private val toast: (String) -> Unit,
    private val onChange: () -> Unit
) {
    /** Найденного обновления здесь не бывает: значок «⇧» в меню не показывается. */
    val available: Any? = null

    /** Текст для Настройки → Обновление. */
    val status: String = L.t("обновления устанавливаются через RuStore")

    /** Значок «⇧» (если его вызовут): объясняет, где обновляться, и в RuStore-версии ничего не качает. */
    fun promptInstall() = toast(L.t("Обновите приложение через RuStore"))

    /** Кнопка «Проверить обновления» в настройках: в RuStore-версии проверок нет. */
    fun check(manual: Boolean) {
        if (manual) {
            onChange()
            toast(L.t("Обновите приложение через RuStore"))
        }
    }
}
