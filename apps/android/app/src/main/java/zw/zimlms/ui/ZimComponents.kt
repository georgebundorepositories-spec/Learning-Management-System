package zw.zimlms.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zw.zimlms.portals.WorkspaceTab

/**
 * Reusable, lightweight Compose components; intentionally no icon-font or
 * downloadable-art asset dependency for offline low-memory school devices.
 */
@Composable
fun BrandMark(modifier: Modifier=Modifier.size(42.dp)) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(13.dp))
      .background(Brush.linearGradient(listOf(ZimColors.Forest, Color(0xFF0B9A76)))),
    contentAlignment=Alignment.Center
  ) {
    Text("Z", color=Color.White, fontSize=24.sp, fontWeight=FontWeight.Black)
  }
}

@Composable
fun AppPanel(
  modifier: Modifier=Modifier,
  content: @Composable () -> Unit
) {
  Surface(
    modifier=modifier,
    color=MaterialTheme.colorScheme.surface,
    shape=RoundedCornerShape(20.dp),
    tonalElevation=0.dp,
    shadowElevation=1.dp,
    border=androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Box(Modifier.padding(18.dp)) { content() }
  }
}

@Composable
fun Eyebrow(text: String, color: Color=MaterialTheme.colorScheme.primary) {
  Text(text.uppercase(),style=MaterialTheme.typography.labelSmall.copy(
    letterSpacing=1.3.sp,fontWeight=FontWeight.Bold),color=color)
}

@Composable
fun SectionTitle(title: String, caption: String?=null) {
  Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
    Text(title, style=MaterialTheme.typography.titleLarge)
    if (caption!=null) {
      Text(caption,style=MaterialTheme.typography.bodySmall,
        color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun StateNotice(title: String, detail: String, accent: Color=MaterialTheme.colorScheme.primary) {
  Surface(color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.65f),
    shape=RoundedCornerShape(17.dp),modifier=Modifier.fillMaxWidth()) {
    Row(
      modifier=Modifier.padding(16.dp),
      horizontalArrangement=Arrangement.spacedBy(12.dp),
      verticalAlignment=Alignment.Top
    ) {
      Box(Modifier.size(10.dp).background(accent,CircleShape))
      Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(title,style=MaterialTheme.typography.titleSmall)
        Text(detail,style=MaterialTheme.typography.bodySmall,
          color=MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

@Composable
fun MiniStat(label: String, value: String, subtitle: String, modifier: Modifier=Modifier) {
  AppPanel(modifier=modifier) {
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
      Text(label,style=MaterialTheme.typography.labelMedium,
        color=MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value,style=MaterialTheme.typography.headlineMedium,
        color=MaterialTheme.colorScheme.primary)
      Text(subtitle,style=MaterialTheme.typography.bodySmall,
        color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

@Composable
fun WorkspaceIcon(tab: WorkspaceTab, modifier: Modifier=Modifier.size(23.dp),
                  color: Color=MaterialTheme.colorScheme.onSurfaceVariant) {
  Canvas(modifier=modifier) {
    val scale=size.minDimension/24f
    val stroke=2f*scale
    fun line(a:Float,b:Float,c:Float,d:Float) {
      drawLine(color,Offset(a*scale,b*scale),Offset(c*scale,d*scale),stroke,StrokeCap.Round)
    }
    fun circle(x:Float,y:Float,r:Float) {
      drawCircle(color,r*scale,Offset(x*scale,y*scale),style=Stroke(stroke))
    }
    fun rect(x:Float,y:Float,w:Float,h:Float) {
      drawRoundRect(color,Offset(x*scale,y*scale),Size(w*scale,h*scale),
        CornerRadius(2.2f*scale),style=Stroke(stroke))
    }
    when(tab) {
      WorkspaceTab.HOME -> {
        line(3f,11f,12f,3f);line(12f,3f,21f,11f)
        line(5.5f,10f,5.5f,21f);line(5.5f,21f,18.5f,21f);line(18.5f,21f,18.5f,10f)
        rect(10f,14f,4f,7f)
      }
      WorkspaceTab.LEARNERS,WorkspaceTab.CHILDREN,WorkspaceTab.PARENTS -> {
        circle(12f,8f,3.3f);circle(4.7f,10f,2f);circle(19.3f,10f,2f)
        line(5f,20f,5f,18f);line(5f,18f,8f,16.2f)
        line(19f,20f,19f,18f);line(19f,18f,16f,16.2f)
        line(7.5f,21f,7.5f,18.3f);line(7.5f,18.3f,10f,16f)
        line(10f,16f,14f,16f);line(14f,16f,16.5f,18.3f);line(16.5f,18.3f,16.5f,21f)
      }
      WorkspaceTab.TEACHERS -> {
        circle(12f,7f,3.5f);line(4.7f,20f,5.5f,16f)
        line(5.5f,16f,9f,14f);line(9f,14f,15f,14f)
        line(15f,14f,18.5f,16f);line(18.5f,16f,19.3f,20f)
        line(4.7f,20f,19.3f,20f)
      }
      WorkspaceTab.CLASSES,WorkspaceTab.ASSIGNMENTS,WorkspaceTab.MY_CLASSES -> {
        rect(3f,4f,18f,16f)
        line(3f,10f,21f,10f);line(9f,10f,9f,20f);line(15f,10f,15f,20f)
      }
      WorkspaceTab.LEARNING -> {
        line(12f,6f,12f,21f);line(12f,7f,8f,5f);line(8f,5f,3f,5f)
        line(3f,5f,3f,19f);line(3f,19f,8f,19f);line(8f,19f,12f,21f)
        line(12f,7f,16f,5f);line(16f,5f,21f,5f)
        line(21f,5f,21f,19f);line(21f,19f,16f,19f);line(16f,19f,12f,21f)
      }
      WorkspaceTab.ATTENDANCE -> {
        rect(4f,3f,16f,18f)
        line(7f,9f,10f,12f);line(10f,12f,16f,6f)
        line(7f,17f,17f,17f)
      }
      WorkspaceTab.NOTICES -> {
        rect(3f,4f,18f,16f)
        line(7f,9f,17f,9f);line(7f,13f,17f,13f);line(7f,17f,13f,17f)
      }
      WorkspaceTab.FEES -> {
        rect(5f,3f,14f,18f)
        line(8f,8f,16f,8f);line(8f,12f,15f,12f);line(8f,16f,13f,16f)
      }
      WorkspaceTab.RESOURCES -> {
        rect(4f,4f,16f,16f)
        line(7f,9f,17f,9f);line(7f,13f,17f,13f);line(7f,17f,13f,17f)
      }
      WorkspaceTab.RESULTS -> {
        rect(4f,3f,16f,18f)
        line(8f,9f,11f,12f);line(11f,12f,17f,7f)
        line(8f,17f,16f,17f)
      }
      WorkspaceTab.COURSEWORK -> {
        rect(4f,3f,16f,18f)
        line(8f,9f,16f,9f);line(8f,13f,16f,13f);line(8f,17f,13f,17f)
      }
      WorkspaceTab.CALENDAR -> {
        rect(3f,5f,18f,16f);line(3f,10f,21f,10f)
        line(8f,3f,8f,7f);line(16f,3f,16f,7f)
        circle(9f,15f,1f);circle(15f,15f,1f)
      }
    }
  }
}
