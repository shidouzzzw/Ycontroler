package com.ycontroler.gamepad.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.Serializable

/** Uygulama dili. Değişiklik anında tüm ekranlara yansır. */
@Serializable
enum class Language(val code: String, val tr: String, val en: String) {
    TR("tr", "Türkçe", "Turkish"),
    EN("en", "İngilizce", "English");

    companion object {
        fun fromKey(key: String?): Language = entries.firstOrNull { it.code == key } ?: TR
    }
}

val LocalLanguage = compositionLocalOf { Language.TR }

/**
 * İki dilli metin yardımcısı. Tüm kullanıcı metinleri bu fonksiyondan geçer;
 * böylece dil değişimi için yeniden başlatma gerekmez.
 */
@Composable
fun t(tr: String, en: String): String = if (LocalLanguage.current == Language.EN) en else tr

/** Composable olmayan bağlamlarda (ör. ViewModel) kullanılır. */
fun translate(language: Language, tr: String, en: String): String = if (language == Language.EN) en else tr
