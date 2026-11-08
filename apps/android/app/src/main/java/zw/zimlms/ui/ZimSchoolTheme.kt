package zw.zimlms.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * ZimSchool design tokens. Neutral layered surfaces, an accessible forest-teal
 * action color, amber reserved for learning progress, and clear type hierarchy.
 * No network font downloads or oversized icon dependencies on 2GB devices.
 */
object ZimColors {
  val Forest = Color(0xFF086F59)
  val ForestDark = Color(0xFF064A3C)
  val ForestLight = Color(0xFF6EE1BF)
  val Slate = Color(0xFF1B2935)
  val Muted = Color(0xFF687782)
  val Mist = Color(0xFFF4F7F6)
  val Line = Color(0xFFE0E8E5)
  val Gold = Color(0xFFE6A44C)
  val GoldLight = Color(0xFFFFEAC8)
}

private val lightColors = lightColorScheme(
  primary = ZimColors.Forest,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD7F3E6),
  onPrimaryContainer = Color(0xFF063F32),
  secondary = Color(0xFF506C67),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE0ECE8),
  onSecondaryContainer = Color(0xFF193B34),
  tertiary = Color(0xFF92631C),
  onTertiary = Color.White,
  tertiaryContainer = ZimColors.GoldLight,
  onTertiaryContainer = Color(0xFF54370C),
  background = ZimColors.Mist,
  onBackground = ZimColors.Slate,
  surface = Color.White,
  onSurface = ZimColors.Slate,
  surfaceVariant = Color(0xFFEAF0EE),
  onSurfaceVariant = Color(0xFF51615F),
  outline = Color(0xFFB8C7C2),
  outlineVariant = ZimColors.Line,
  error = Color(0xFFB3261E)
)
private val darkColors = darkColorScheme(
  primary = ZimColors.ForestLight,
  onPrimary = Color(0xFF003C2E),
  primaryContainer = Color(0xFF145E4F),
  onPrimaryContainer = Color(0xFFD4F8E9),
  secondary = Color(0xFFB1C9C1),
  onSecondary = Color(0xFF18392F),
  secondaryContainer = Color(0xFF294B42),
  onSecondaryContainer = Color(0xFFDCEBE5),
  tertiary = Color(0xFFFFC579),
  onTertiary = Color(0xFF452900),
  tertiaryContainer = Color(0xFF64451C),
  onTertiaryContainer = Color(0xFFFFE7BF),
  background = Color(0xFF0C1217),
  onBackground = Color(0xFFE9F0EE),
  surface = Color(0xFF182127),
  onSurface = Color(0xFFE9F0EE),
  surfaceVariant = Color(0xFF24332F),
  onSurfaceVariant = Color(0xFFC1CEC8),
  outline = Color(0xFF738781),
  outlineVariant = Color(0xFF354640),
  error = Color(0xFFFFB4A9)
)

private val zimTypography = Typography(
  headlineLarge = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=31.sp,lineHeight=37.sp,letterSpacing=(-0.7).sp),
  headlineMedium = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=26.sp,lineHeight=32.sp,letterSpacing=(-0.5).sp),
  headlineSmall = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=22.sp,lineHeight=28.sp,letterSpacing=(-0.4).sp),
  titleLarge = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=19.sp,lineHeight=25.sp,letterSpacing=(-0.3).sp),
  titleMedium = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=22.sp),
  titleSmall = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp),
  bodyLarge = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Normal,fontSize=16.sp,lineHeight=24.sp),
  bodyMedium = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Normal,fontSize=14.sp,lineHeight=21.sp),
  bodySmall = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Normal,fontSize=12.sp,lineHeight=18.sp),
  labelLarge = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp),
  labelMedium = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=12.sp,lineHeight=17.sp),
  labelSmall = TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=11.sp,lineHeight=16.sp)
)

@Composable
fun ZimSchoolTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme=if(darkTheme) darkColors else lightColors,
    typography=zimTypography,
    content=content
  )
}
