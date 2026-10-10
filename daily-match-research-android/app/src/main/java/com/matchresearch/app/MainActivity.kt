package com.matchresearch.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln

private const val CHANNEL_ID = "match_research_reminders"
private const val PREFS = "match_research_data"

data class MatchItem(
    val id: Long,
    val sport: String,
    val event: String,
    val competition: String,
    val kickoff: Long,
    val score: Int,
    val notes: String,
    val selected: Boolean = false
)

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channel = NotificationChannel(CHANNEL_ID, "Match research reminders", NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { MatchResearchApp(this) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Match research reminder"
        val body = intent.getStringExtra("body") ?: "Review current match information."
        val id = intent.getIntExtra("id", 1)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title).setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build()
        if (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}

@Composable
fun MatchResearchApp(context: Context) {
    var tab by remember { mutableStateOf("Football") }
    var matches by remember { mutableStateOf(loadMatches(context)) }
    var showAdd by remember { mutableStateOf(false) }
    var event by remember { mutableStateOf("") }
    var competition by remember { mutableStateOf("") }
    var kickoff by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("Football") }
    var goalsFor by remember { mutableStateOf("2.2") }
    var goalsAgainst by remember { mutableStateOf("1.6") }
    var odds by remember { mutableStateOf("1.55") }
    var rankGap by remember { mutableStateOf("20") }
    var status by remember { mutableStateOf("Add a verified fixture to begin. Live data is not connected yet.") }
    val filtered = matches.filter { it.sport == tab }.sortedByDescending { it.score }
    val footballSelected = matches.count { it.sport == "Football" && it.selected }
    val tennisSelected = matches.count { it.sport == "Tennis" && it.selected }

    MaterialTheme {
        Scaffold(
            topBar = {
                Surface(color = MaterialTheme.colorScheme.primary) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("MATCH RESEARCH", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f))
                        Text("Daily Football & Tennis", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Evidence-led shortlist • reminders you control", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .9f))
                    }
                }
            },
            floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Text("+") } }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (tab == "Football") Button(onClick = { tab = "Football" }, modifier = Modifier.weight(1f)) { Text("⚽ Football") }
                    else OutlinedButton(onClick = { tab = "Football" }, modifier = Modifier.weight(1f)) { Text("⚽ Football") }
                    if (tab == "Tennis") Button(onClick = { tab = "Tennis" }, modifier = Modifier.weight(1f)) { Text("🎾 Tennis") }
                    else OutlinedButton(onClick = { tab = "Tennis" }, modifier = Modifier.weight(1f)) { Text("🎾 Tennis") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Football selected", "$footballSelected / 2", Modifier.weight(1f))
                    StatCard("Tennis selected", "$tennisSelected / 2", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text("Today's shortlist", style = MaterialTheme.typography.titleLarge)
                Text("Scores are heuristic screening scores, not probabilities or guarantees.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No " + tab.lowercase() + " fixtures added yet. Tap + to add a verified fixture.")
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        items(filtered, key = { it.id }) { match ->
                            MatchCard(match,
                                onToggle = {
                                    val count = matches.count { it.sport == match.sport && it.selected }
                                    if (!match.selected && count >= 2) status = "Limit reached: at most two " + match.sport.lowercase() + " selections."
                                    else {
                                        matches = matches.map { if (it.id == match.id) it.copy(selected = !it.selected) else it }
                                        saveMatches(context, matches)
                                        status = if (!match.selected) "Selected. Review details before scheduling." else "Selection removed."
                                    }
                                },
                                onSchedule = {
                                    if (!match.selected) status = "Select the match and review it before scheduling."
                                    else {
                                        schedule(context, match, 0L, null)
                                        if (match.sport == "Football") schedule(context, match, 60 * 60 * 1000L, "Second-half review")
                                        else schedule(context, match, 10 * 60 * 1000L, "Tennis 2-game check")
                                        status = "Reminder requested for " + match.event + ". Confirm notification permission is enabled."
                                    }
                                },
                                onDelete = {
                                    matches = matches.filterNot { it.id == match.id }
                                    saveMatches(context, matches)
                                    status = "Fixture removed."
                                }
                            )
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Text(status, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Text("Research only. No live feed, betting, or guaranteed outcomes.", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
            }
        }

        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                title = { Text("Add verified fixture") },
                text = {
                    Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Use reliable sources. Time format: yyyy-MM-dd HH:mm (local time).")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = sport == "Football", onClick = { sport = "Football" }, label = { Text("Football") })
                            FilterChip(selected = sport == "Tennis", onClick = { sport = "Tennis" }, label = { Text("Tennis") })
                        }
                        OutlinedTextField(event, { event = it }, label = { Text("Fixture / players") }, singleLine = true)
                        OutlinedTextField(competition, { competition = it }, label = { Text("Competition") }, singleLine = true)
                        OutlinedTextField(kickoff, { kickoff = it }, label = { Text("Start time") }, singleLine = true)
                        if (sport == "Football") {
                            OutlinedTextField(goalsFor, { goalsFor = it }, label = { Text("Combined recent goals scored avg") }, singleLine = true)
                            OutlinedTextField(goalsAgainst, { goalsAgainst = it }, label = { Text("Combined recent goals conceded avg") }, singleLine = true)
                            Text("Also check defence stability, early goals, tactics and goalkeeper/defender availability.")
                        } else {
                            OutlinedTextField(odds, { odds = it }, label = { Text("Favourite decimal odds") }, singleLine = true)
                            OutlinedTextField(rankGap, { rankGap = it }, label = { Text("Opponent rank minus favourite rank") }, singleLine = true)
                            Text("Also check surface form, fitness, recent results, rest and opponent quality.")
                        }
                        OutlinedTextField(notes, { notes = it }, label = { Text("Evidence and cautions") }, minLines = 2)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val start = parseKickoff(kickoff)
                        if (event.isBlank() || start == null) status = "Enter fixture name and valid local time (yyyy-MM-dd HH:mm)."
                        else {
                            val score = if (sport == "Football") footballScore(goalsFor.toDoubleOrNull(), goalsAgainst.toDoubleOrNull())
                                        else tennisScore(odds.toDoubleOrNull(), rankGap.toDoubleOrNull())
                            val newMatch = MatchItem(System.currentTimeMillis(), sport, event.trim(), competition.trim(), start, score, notes.trim())
                            matches = (matches + newMatch).sortedByDescending { it.score }
                            saveMatches(context, matches)
                            status = "Fixture added: screening score " + score + "/100. This is not a probability."
                            event = ""; competition = ""; kickoff = ""; notes = ""; showAdd = false
                        }
                    }) { Text("Add fixture") }
                },
                dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun MatchCard(match: MatchItem, onToggle: () -> Unit, onSchedule: () -> Unit, onDelete: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (match.sport == "Football") "⚽" else "🎾", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(match.event, style = MaterialTheme.typography.titleMedium)
                    Text(match.competition.ifBlank { match.sport }, style = MaterialTheme.typography.bodySmall)
                }
                Text(match.score.toString() + "/100", style = MaterialTheme.typography.titleMedium)
            }
            Text("Start: " + SimpleDateFormat("EEE d MMM, HH:mm", Locale.UK).format(Date(match.kickoff)))
            if (match.notes.isNotBlank()) Text(match.notes, style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = match.selected, onCheckedChange = { onToggle() })
                Text("Select", modifier = Modifier.weight(1f))
                TextButton(onClick = onDelete) { Text("Remove") }
                Button(onClick = onSchedule) { Text("Schedule") }
            }
        }
    }
}

private fun footballScore(goalsFor: Double?, goalsAgainst: Double?): Int {
    var score = 48
    if (goalsFor != null) score += when { goalsFor <= 2.2 -> 12; goalsFor <= 2.8 -> 6; goalsFor >= 3.5 -> -10; else -> 0 } else score -= 8
    if (goalsAgainst != null) score += when { goalsAgainst <= 1.6 -> 10; goalsAgainst <= 2.0 -> 5; goalsAgainst >= 2.8 -> -8; else -> 0 } else score -= 8
    return score.coerceIn(0, 100)
}

private fun tennisScore(odds: Double?, rankGap: Double?): Int {
    var score = 48
    if (odds != null && odds > 1.0) score += (((1.0 / odds) - .5) * 32).toInt().coerceIn(-5, 16) else score -= 8
    if (rankGap != null) score += (ln(rankGap.coerceAtLeast(0.0) + 1.0) * 3).toInt().coerceIn(0, 10) else score -= 8
    return score.coerceIn(0, 100)
}

private fun parseKickoff(raw: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.UK).apply { isLenient = false }.parse(raw)?.time
} catch (_: Exception) { null }

private fun saveMatches(context: Context, matches: List<MatchItem>) {
    val array = JSONArray()
    matches.forEach { match ->
        array.put(JSONObject().put("id", match.id).put("sport", match.sport).put("event", match.event)
            .put("competition", match.competition).put("kickoff", match.kickoff).put("score", match.score)
            .put("notes", match.notes).put("selected", match.selected))
    }
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("matches", array.toString()).apply()
}

private fun loadMatches(context: Context): List<MatchItem> = try {
    val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("matches", "[]"))
    (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        MatchItem(obj.getLong("id"), obj.getString("sport"), obj.getString("event"), obj.optString("competition"),
            obj.getLong("kickoff"), obj.getInt("score"), obj.optString("notes"), obj.optBoolean("selected"))
    }
} catch (_: Exception) { emptyList() }

private fun schedule(context: Context, match: MatchItem, offset: Long, suffix: String?) {
    val triggerAt = match.kickoff + offset
    if (triggerAt <= System.currentTimeMillis()) return
    val requestCode = ((match.id % 1000000000L) + (offset / 60000L)).toInt()
    val intent = Intent(context, ReminderReceiver::class.java)
        .putExtra("title", (suffix?.plus(": ") ?: "Match research: ") + match.event)
        .putExtra("body", if (match.sport == "Football") "Review match state and current information. This is not a guarantee." else "Review player status and current match information.")
        .putExtra("id", requestCode)
    val pending = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
}
