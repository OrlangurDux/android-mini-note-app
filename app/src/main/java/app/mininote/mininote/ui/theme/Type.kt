package app.mininote.mininote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

// M3 Typography по умолчанию уже использует системный Roboto на Android — портировать
// набор весов/размеров 1:1 из дизайна не требуется, дефолтная M3-типографика соответствует спеке.
val MiniNoteTypography = Typography()

// Дизайн использует Roboto Mono для чипов/таймштампов/OTP-полей (mini-note-design/android/m3.jsx: MONO).
// Используем системный FontFamily.Monospace как достаточно близкий аналог для v1, чтобы не тащить
// зависимость на загружаемые Google Fonts ради частных элементов интерфейса.
val MonospaceTextStyle = TextStyle(fontFamily = FontFamily.Monospace)
