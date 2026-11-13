package zw.zimlms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import zw.zimlms.ui.AppPanel
import zw.zimlms.ui.Eyebrow
import zw.zimlms.ui.SectionTitle
import zw.zimlms.ui.MiniStat
import zw.zimlms.ui.StateNotice
import zw.zimlms.core.auth.CachedProfile
import zw.zimlms.core.auth.SessionBroker
import zw.zimlms.core.auth.SessionVault
import zw.zimlms.core.sync.CalendarEventCache
import zw.zimlms.core.sync.CalendarSyncScheduler
import zw.zimlms.core.sync.OfflineDatabase
import zw.zimlms.core.sync.SyncEngine
import zw.zimlms.core.sync.SyncHttpApi
import zw.zimlms.core.sync.SyncQueueEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val harareZone = ZoneId.of("Africa/Harare")
private val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * Calendar-only Phase 1 staff workspace. Event payloads cannot include PII.
 * All edits are inserted into Room before any online request; a failed sync
 * never removes the local queue. Conflict resolutions remain a future gate.
 */
@Composable
fun SchoolCalendarPanel(profile: CachedProfile) {
  if (profile.role !in setOf("teacher", "head", "admin")) {
    Text("Calendar editing is restricted to school staff.")
    return
  }

  val context = LocalContext.current.applicationContext
  val scope = rememberCoroutineScope()
  var title by remember(profile.userId) { mutableStateOf("") }
  var location by remember(profile.userId) { mutableStateOf("") }
  var start by remember(profile.userId) {
    mutableStateOf(LocalDateTime.now(harareZone).plusDays(1).withMinute(0).format(inputFormat))
  }
  var end by remember(profile.userId) {
    mutableStateOf(LocalDateTime.now(harareZone).plusDays(1).plusHours(1).withMinute(0).format(inputFormat))
  }
  var busy by remember { mutableStateOf(false) }
  var showForm by remember { mutableStateOf(false) }
  var notice by remember { mutableStateOf("Offline edits remain on this device until acknowledged.") }
  var cached by remember(profile.userId) { mutableStateOf<List<CalendarEventCache>>(emptyList()) }
  var queued by remember(profile.userId) { mutableStateOf<List<SyncQueueEntity>>(emptyList()) }

  suspend fun reload() {
    val snapshot = withContext(Dispatchers.IO) {
      val db = OfflineDatabase.open(context, profile.userId, profile.schoolId)
      try {
        Pair(db.calendar().allForSchool(profile.schoolId), db.queue().all())
      } finally { db.close() }
    }
    cached = snapshot.first
    queued = snapshot.second
  }

  LaunchedEffect(profile.userId, profile.schoolId) {
    runCatching { reload() }
      .onFailure { notice = "Unable to open the local calendar cache." }
  }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Eyebrow("SCHOOL OPERATIONS")
    Text("Events & calendar", style = MaterialTheme.typography.headlineMedium)
    Text("Plan school activities and keep draft changes safely queued when offline.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MiniStat("Events", cached.count { it.deletedAt == null }.toString(),
        "On this device", Modifier.weight(1f))
      MiniStat("Pending", queued.count { it.status == "queued" }.toString(),
        "Waiting to sync", Modifier.weight(1f))
    }
    StateNotice("Sync status",notice)
    if (queued.any { it.status != "queued" }) {
      StateNotice("Changes need review",
        "${queued.count { it.status != "queued" }} edits were rejected or conflicted. They remain available on this device.")
    }
    Button(onClick={showForm=!showForm}, modifier=Modifier.fillMaxWidth(),
      shape=RoundedCornerShape(14.dp)) {
      Text(if(showForm) "Close new event form" else "+ Create school event")
    }

    if (showForm) {
    AppPanel(Modifier.fillMaxWidth()) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
    SectionTitle("New school event","Dates use Zimbabwe local time")
    OutlinedTextField(value = title, onValueChange = { title = it.take(200) },
      label = { Text("Event title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = start, onValueChange = { start = it },
      label = { Text("Start (YYYY-MM-DD HH:MM), Zimbabwe time") },
      singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = end, onValueChange = { end = it },
      label = { Text("End (YYYY-MM-DD HH:MM), Zimbabwe time") },
      singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = location, onValueChange = { location = it.take(200) },
      label = { Text("Venue (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

    Button(enabled = !busy && title.isNotBlank(), onClick = {
      busy = true
      scope.launch {
        val action = runCatching {
          val startAt = LocalDateTime.parse(start.trim(), inputFormat).atZone(harareZone).toInstant()
          val endAt = LocalDateTime.parse(end.trim(), inputFormat).atZone(harareZone).toInstant()
          require(!endAt.isBefore(startAt) && title.trim().length in 1..200) {
            "Enter a title and an end time after the start"
          }
          withContext(Dispatchers.IO) {
            val database = OfflineDatabase.open(context, profile.userId, profile.schoolId)
            try {
              val opId = UUID.randomUUID().toString()
              val eventId = UUID.randomUUID().toString()
              database.queue().enqueue(SyncQueueEntity(
                opId = opId, entityId = eventId, opType = "insert",
                payloadJson = JSONObject()
                  .put("title", title.trim())
                  .put("location", location.trim())
                  .put("category", "academic")
                  .put("start_at", startAt.toString())
                  .put("end_at", endAt.toString())
                  .toString(),
                clientTs = Instant.now().toString(), baseVersion = null
              ))
            } finally { database.close() }
          }
          if (SessionVault(context).load()?.profile?.userId == profile.userId) {
            CalendarSyncScheduler.requestImmediate(context)
          }
        }
        notice = action.fold(
          onSuccess = { title = ""; location = ""; "Saved locally. Sync requested when connected." },
          onFailure = { it.message ?: "Could not queue this event" }
        )
        runCatching { reload() }
        busy = false
      }
    }, modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(12.dp)) {
      Text(if (busy) "Saving…" else "Save event to this device")
    }
    }
    }
    }
    SectionTitle("Your calendar","Cached items are safe to read without a connection")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(enabled = !busy, onClick = {
        busy = true
        scope.launch {
          val result = runCatching {
            withContext(Dispatchers.IO) {
              val session = SessionVault(context).load()
                ?: error("Sign in online to enable background sync.")
              require(session.profile.userId == profile.userId &&
                session.profile.schoolId == profile.schoolId) { "Wrong school session" }
              val broker = SessionBroker(context,
                BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)
              val db = OfflineDatabase.open(context, profile.userId, profile.schoolId)
              try {
                val transport = SyncHttpApi(
                  BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY,
                  session.deviceId,
                  { broker.accessToken(profile.userId, profile.schoolId) }
                )
                SyncEngine(db,transport,profile.schoolId).syncNow()
              } finally { db.close() }
            }
          }
          notice = result.fold(
            onSuccess = { "Sync complete: ${it.pushed} uploaded, ${it.pulled} received; " +
              "${it.conflicts} conflicts and ${it.rejected} rejected edits retained." },
            onFailure = { "Sync unavailable; queued changes retained. ${it.message ?: "Retry online."}" }
          )
          runCatching { reload() }
          busy = false
        }
      }) { Text("Synchronise") }
      OutlinedButton(enabled = !busy, onClick = {
        scope.launch {
          runCatching { reload() }
            .onFailure { notice = "Could not reload local calendar." }
        }
      }) { Text("Refresh list") }
    }

    if (cached.isEmpty() && queued.isEmpty()) {
      StateNotice("Nothing on the calendar yet",
        "Create your first event or connect to sync existing school events.")
    } else {
      cached.filter { it.deletedAt == null }.take(15).forEach { record ->
        val row = remember(record.rowJson) { runCatching { JSONObject(record.rowJson) }.getOrNull() }
        if (row != null) {
          AppPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
              Eyebrow(row.optString("category","ACADEMIC"))
              Text(row.optString("title","Untitled event"),
                style=MaterialTheme.typography.titleMedium)
              Text(row.optString("start_at","") + " • " +
                row.optString("approval_status","pending"),
                style=MaterialTheme.typography.bodySmall,
                color=MaterialTheme.colorScheme.onSurfaceVariant)
              val venue=row.optString("location","")
              if(venue.isNotBlank() && venue!="null") {
                Text(venue,style=MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }
      if(queued.isNotEmpty()) {
        HorizontalDivider()
        SectionTitle("Local outbox","Edits remain queued until the server confirms")
        queued.take(15).forEach { op ->
          Text("${op.opType.uppercase()} · ${op.status} · ${op.entityId.take(8)}",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}
