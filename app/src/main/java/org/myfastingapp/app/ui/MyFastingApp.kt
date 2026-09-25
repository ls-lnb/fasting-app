package org.myfastingapp.app.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.NumberPicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.myfastingapp.app.domain.FastPlan
import org.myfastingapp.app.domain.FastPlans
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.FastingPhases
import org.myfastingapp.app.domain.ThemeMode
import org.myfastingapp.app.domain.TimerMath
import org.myfastingapp.app.domain.UserSettings
import org.myfastingapp.app.domain.WeightEntry
import org.myfastingapp.app.domain.WeightTrend
import org.myfastingapp.app.domain.WeightTrendCalculator
import org.myfastingapp.app.domain.WeightUnit
import org.myfastingapp.app.domain.kgToLb
import org.myfastingapp.app.domain.lbToKg
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun MyFastingApp(viewModel: MyFastingAppViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Timer) }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
        }
    }

    MyFastingAppTheme(themeMode = uiState.settings.themeMode) {
        Scaffold(
            containerColor = Cream,
            bottomBar = {
                NavigationBar(containerColor = CardSurface.copy(alpha = 0.96f)) {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Cream)
                    .padding(padding),
            ) {
                when (selectedTab) {
                    AppTab.Timer -> TimerScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onOpenPlans = { selectedTab = AppTab.Fasts },
                    )
                    AppTab.Fasts -> FastsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onPlanChosen = { selectedTab = AppTab.Timer },
                    )
                    AppTab.Trends -> TrendsScreen(uiState, viewModel)
                    AppTab.History -> HistoryScreen(uiState, viewModel)
                    AppTab.Settings -> SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        showMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerScreen(
    uiState: MyFastingAppUiState,
    viewModel: MyFastingAppViewModel,
    onOpenPlans: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var editing by remember { mutableStateOf<FastSession?>(null) }
    var starting by remember { mutableStateOf<FastPlan?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(uiState.activeSession?.id, lifecycleOwner) {
        now = System.currentTimeMillis()
        if (uiState.activeSession != null) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    now = System.currentTimeMillis()
                    delay(1_000L)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("MyFastingApp", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                if (uiState.activeSession == null) "Choose a plan and begin" else "You're fasting",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
            )
        }
        SelectedPlanStrip(
            session = uiState.activeSession,
            selectedPlan = uiState.selectedPlan,
            onClick = onOpenPlans,
        )
        TimerHero(
            session = uiState.activeSession,
            selectedPlan = uiState.selectedPlan,
            now = now,
            onStart = { starting = uiState.selectedPlan },
            onEnd = viewModel::endFast,
        )
        TimerStatsStrip(uiState = uiState, now = now)
        FastTimingPanel(
            session = uiState.activeSession,
            selectedPlan = uiState.selectedPlan,
            now = now,
            onEdit = { uiState.activeSession?.let { editing = it } },
        )
    }

    editing?.let { session ->
        EditFastDialog(
            session = session,
            onDismiss = { editing = null },
            onSave = { start, end ->
                viewModel.editFast(session.id, start, end)
                editing = null
            },
        )
    }

    starting?.let { plan ->
        StartFastDialog(
            plan = plan,
            onDismiss = { starting = null },
            onStart = { startMillis ->
                viewModel.startFast(plan, startMillis)
                starting = null
            },
        )
    }
}

@Composable
private fun TimerStatsStrip(uiState: MyFastingAppUiState, now: Long) {
    val activeSeconds = uiState.activeSession?.durationMillis(now)?.div(1_000L) ?: 0L
    val latestWeight = remember(uiState.weights) { uiState.weights.maxByOrNull { it.recordedEpochMillis } }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimerStatPill(
            label = "Streak",
            value = "${uiState.stats.currentStreakDays}d",
            modifier = Modifier.weight(1f),
        )
        TimerStatPill(
            label = "Fasted",
            value = formatTotalFastedHours(uiState.stats.totalSeconds + activeSeconds),
            modifier = Modifier.weight(1f),
        )
        TimerStatPill(
            label = "Weight",
            value = latestWeight?.let { formatWeight(it.weightKg, uiState.settings.weightUnit, 1) } ?: "--",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimerStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        color = Cream.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, StatPillBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TimerHero(
    session: FastSession?,
    selectedPlan: FastPlan,
    now: Long,
    onStart: () -> Unit,
    onEnd: () -> Unit,
) {
    val elapsedMillis = session?.durationMillis(now) ?: 0L
    val targetSeconds = session?.displayTargetSeconds() ?: selectedPlan.fastingMinutes * 60L
    val start = session?.startEpochMillis ?: now
    val progress = TimerMath.progress(start, targetSeconds, now)
    val phase = session?.let { FastingPhases.forElapsed(elapsedMillis) }
    val phaseColor = phase?.let { Color(it.colorArgb) } ?: Brand

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularTimer(
                progressFraction = if (session == null) 0f else progress.progressFraction,
                showStartedProgress = session != null && progress.elapsedMillis > 0L,
                progressColor = phaseColor,
            )
            Column(
                modifier = Modifier.fillMaxWidth(0.58f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (phase != null) {
                    Text(
                        phase.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = phaseColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        phase.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    if (session == null) {
                        "Ready"
                    } else {
                        "Elapsed of ${TimerMath.formatMinutes((targetSeconds / 60L).toInt())} (${TimerMath.formatProgressPercent(progress.progressFraction)})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = Muted,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (session == null) TimerMath.formatMinutes(selectedPlan.fastingMinutes) else TimerMath.formatDurationWithSeconds(elapsedMillis),
                    style = if (session == null) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                Text(
                    when {
                        session == null -> "Target"
                        progress.targetReached -> "Extended by ${TimerMath.formatDuration(elapsedMillis - progress.targetMillis)}"
                        else -> "${TimerMath.formatDuration(progress.remainingMillis)} remaining"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Button(
            onClick = if (session == null) onStart else onEnd,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (session == null) Brand else Color.White,
                contentColor = if (session == null) Color.White else phaseColor,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
        ) {
            Text(if (session == null) "Start fast" else "End fast now", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CircularTimer(progressFraction: Float, showStartedProgress: Boolean, progressColor: Color) {
    val trackColor = TimerTrack
    val wineColor = Wine
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .aspectRatio(1f)
            .padding(10.dp),
    ) {
        val strokeWidth = 30.dp.toPx()
        val trackStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val progress = progressFraction.coerceAtLeast(0f)
        val baseProgress = progress.coerceIn(0f, 1f)
        val visualProgress = if (showStartedProgress && baseProgress in 0f..0.012f) 0.012f else baseProgress
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        drawCircle(
            color = trackColor,
            radius = (size.minDimension - strokeWidth) / 2f,
            center = center,
            style = trackStroke,
        )
        if (visualProgress > 0f) {
            drawArc(
                brush = Brush.sweepGradient(listOf(progressColor, progressColor.copy(alpha = 0.72f), progressColor)),
                startAngle = 90f,
                sweepAngle = 360f * visualProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = if (visualProgress >= 0.999f) StrokeCap.Butt else StrokeCap.Round),
            )
        }
        if (progress > 1f) {
            val extraProgress = ((progress - 1f) % 1f).takeIf { it > 0.01f } ?: 1f
            val extraInset = 4.dp.toPx()
            drawArc(
                color = wineColor,
                startAngle = 90f,
                sweepAngle = 360f * extraProgress,
                useCenter = false,
                topLeft = Offset(inset - extraInset, inset - extraInset),
                size = Size(arcSize.width + extraInset * 2f, arcSize.height + extraInset * 2f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun FastTimingPanel(
    session: FastSession?,
    selectedPlan: FastPlan,
    now: Long,
    onEdit: () -> Unit,
) {
    val start = session?.startEpochMillis ?: now
    val plannedEnd = start + (session?.displayTargetSeconds() ?: selectedPlan.fastingMinutes * 60L) * 1_000L

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                TimingBlock(
                    label = "Started fasting",
                    value = if (session == null) "--" else formatTimerPanelDateTime(start),
                    onEdit = onEdit.takeIf { session != null },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                TimingBlock(
                    label = "Fast ending",
                    value = if (session == null) "--" else formatTimerPanelDateTime(plannedEnd),
                    onEdit = onEdit.takeIf { session != null },
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun TimingBlock(
    label: String,
    value: String,
    onEdit: (() -> Unit)?,
    modifier: Modifier = Modifier,
    textAlign: TextAlign,
) {
    val horizontalAlignment = if (textAlign == TextAlign.End) Alignment.End else Alignment.Start
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = horizontalAlignment,
        modifier = modifier,
    ) {
        Text(
            label.uppercase(),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            fontWeight = FontWeight.Bold,
            textAlign = textAlign,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
                maxLines = 2,
                textAlign = textAlign,
                modifier = Modifier.weight(1f),
            )
            if (onEdit != null) {
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Coral, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectedPlanStrip(session: FastSession?, selectedPlan: FastPlan, onClick: () -> Unit) {
    val label = if (session != null) {
        val target = TimerMath.formatMinutes((session.displayTargetSeconds() / 60L).toInt())
        "${session.planName.uppercase()} FAST - $target"
    } else {
        "${selectedPlan.name.uppercase()} FAST - ${TimerMath.formatMinutes(selectedPlan.fastingMinutes)}"
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = CardSurface.copy(alpha = 0.82f),
        shadowElevation = 2.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Slate,
        )
    }
}

@Composable
private fun FastsScreen(
    uiState: MyFastingAppUiState,
    viewModel: MyFastingAppViewModel,
    onPlanChosen: () -> Unit,
) {
    var customMinutes by rememberSaveable { mutableStateOf(uiState.settings.customFastingMinutes) }
    val colors = listOf(PurpleCard, CoralCard, TealCard, GoldCard, BlueCard, GreenCard)

    LaunchedEffect(uiState.settings.customFastingMinutes) {
        customMinutes = uiState.settings.customFastingMinutes
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(title = "Fasts", action = null)
        CustomDurationCard(
            minutes = customMinutes,
            onMinutesChange = { customMinutes = it.coerceIn(30, 7 * 24 * 60) },
            onUse = {
                viewModel.setCustomPlan(customMinutes)
                onPlanChosen()
            },
            onStart = {
                viewModel.setCustomPlan(customMinutes)
                viewModel.startFast(FastPlans.resolve(FastPlans.CUSTOM_ID, customMinutes))
                onPlanChosen()
            },
        )
        FastPlans.builtIns.withIndex().chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { indexed ->
                    val plan = indexed.value
                    CompactPlanCard(
                        plan = plan,
                        color = colors[indexed.index % colors.size],
                        selected = uiState.settings.defaultPlanId == plan.id,
                        onSelect = {
                            viewModel.selectPlan(plan.id)
                            onPlanChosen()
                        },
                        onStart = {
                            viewModel.startFast(plan)
                            onPlanChosen()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CustomDurationCard(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    onUse: () -> Unit,
    onStart: () -> Unit,
) {
    val hours = minutes / 60
    val minuteRemainder = minutes % 60

    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardSurface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Custom", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text(TimerMath.formatMinutes(minutes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Brand)
                }
                Surface(shape = RoundedCornerShape(16.dp), color = TintSurface) {
                    Text("Goal", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Brand, fontWeight = FontWeight.Bold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DurationPresetRow(listOf(12 * 60, 14 * 60, 16 * 60, 18 * 60), minutes, onMinutesChange)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DurationStepper(
                    label = "Hours",
                    value = hours.toString(),
                    onDecrease = { onMinutesChange(minutes - 60) },
                    onIncrease = { onMinutesChange(minutes + 60) },
                    modifier = Modifier.weight(1f),
                )
                DurationStepper(
                    label = "Minutes",
                    value = minuteRemainder.toString().padStart(2, '0'),
                    onDecrease = { onMinutesChange(minutes - 15) },
                    onIncrease = { onMinutesChange(minutes + 15) },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onUse, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).height(40.dp)) {
                    Text("Use")
                }
                Button(onClick = onStart, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).height(40.dp)) {
                    Text("Start now")
                }
            }
        }
    }
}

@Composable
private fun DurationPresetRow(presets: List<Int>, selectedMinutes: Int, onSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { minutes ->
            val selected = selectedMinutes == minutes
            Surface(
                onClick = { onSelect(minutes) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = if (selected) Wine else ChipSurface,
            ) {
                Text(
                    text = TimerMath.formatMinutes(minutes),
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    color = if (selected) Color.White else Slate,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun DurationStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = SubtleSurface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Remove, contentDescription = "Decrease $label", tint = Slate)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = "Increase $label", tint = Slate)
            }
        }
    }
}

@Composable
private fun CompactPlanCard(
    plan: FastPlan,
    color: Color,
    selected: Boolean,
    onSelect: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onSelect,
        modifier = modifier.height(86.dp),
        shape = RoundedCornerShape(18.dp),
        color = color,
        shadowElevation = if (selected) 6.dp else 2.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(plan.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(TimerMath.formatMinutes(plan.fastingMinutes), color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
                }
                Text(if (selected) "On" else "Start", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = color),
            ) {
                Text("Start", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TrendsScreen(uiState: MyFastingAppUiState, viewModel: MyFastingAppViewModel) {
    var loggingWeight by remember { mutableStateOf(false) }
    var loggingFast by remember { mutableStateOf(false) }
    var selectedPeriod by rememberSaveable { mutableStateOf(TrendPeriod.Week) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(title = "Trends", action = null)
        PeriodTabs(selectedPeriod = selectedPeriod, onPeriodSelected = { selectedPeriod = it })
        RecentFastsTrendCard(
            sessions = uiState.sessions,
            period = selectedPeriod,
            onLogFast = { loggingFast = true },
        )
        WeightTrendCard(
            weights = uiState.weights,
            unit = uiState.settings.weightUnit,
            targetWeightKg = uiState.settings.targetWeightKg,
            period = selectedPeriod,
            onLogWeight = { loggingWeight = true },
        )
    }

    if (loggingWeight) {
        LogWeightDialog(
            unit = uiState.settings.weightUnit,
            onDismiss = { loggingWeight = false },
            onSave = { weightKg, recordedAt ->
                viewModel.addWeight(weightKg, recordedAt)
                loggingWeight = false
            },
        )
    }
    if (loggingFast) {
        LogFastDialog(
            selectedPlan = uiState.selectedPlan,
            onDismiss = { loggingFast = false },
            onSave = { planName, targetSeconds, start, end ->
                viewModel.addCompletedFast(planName, targetSeconds, start, end)
                loggingFast = false
            },
        )
    }
}

@Composable
private fun PeriodTabs(selectedPeriod: TrendPeriod, onPeriodSelected: (TrendPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BarsChartSurface, RoundedCornerShape(14.dp))
            .padding(3.dp),
    ) {
        TrendPeriod.entries.forEach { period ->
            val selected = period == selectedPeriod
            Surface(
                onClick = { onPeriodSelected(period) },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (selected) Color.White else Color.Transparent,
                shadowElevation = if (selected) 1.dp else 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(period.label, color = if (selected) Coral else Muted, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun RecentFastsTrendCard(
    sessions: List<FastSession>,
    period: TrendPeriod,
    onLogFast: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val completed = sessions.completedInPeriod(period, zone)
    val averageSeconds = completed
        .takeIf { it.isNotEmpty() }
        ?.map { it.durationMillis(it.endEpochMillis ?: it.startEpochMillis) / 1_000L }
        ?.average()
        ?.toLong()
        ?: 0L
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = CardSurface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Recent Fasts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Average", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text(TimerMath.formatDuration(averageSeconds * 1_000L), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                }
                Text(trendRangeLabel(period), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Slate)
            }
            FastBarsChart(sessions = sessions, period = period)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                ChartLegend(Coral, "Goal met")
                Spacer(Modifier.width(14.dp))
                ChartLegend(MutedPink, "Goal not met")
            }
            Button(
                onClick = onLogFast,
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .align(Alignment.CenterHorizontally)
                    .height(38.dp),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Wine, contentColor = Color.White),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Fast", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FastBarsChart(sessions: List<FastSession>, period: TrendPeriod) {
    val buckets = fastTrendBuckets(sessions, period)
    val maxHours = buckets.maxOfOrNull { it.hours }?.coerceAtLeast(24.0) ?: 24.0
    val axisMax = ceil(maxHours / 6.0).coerceAtLeast(4.0) * 6.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChartYAxis(maxLabel = "${axisMax.roundToInt()}h", midLabel = "${(axisMax / 2.0).roundToInt()}h", minLabel = "0h", height = 72.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            ) {
                ChartGrid()
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(if (buckets.size > 8) 4.dp else 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    buckets.forEach { bucket ->
                        val barHeight = if (bucket.hours <= 0.0) 4.dp else ((bucket.hours / axisMax) * 66.0).coerceAtLeast(10.0).toFloat().dp
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(if (buckets.size > 8) 6.dp else 9.dp)
                                    .height(barHeight)
                                    .background(if (bucket.goalMet) Coral else MutedPink, RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
            }
            if (period == TrendPeriod.Year) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    trendXAxisLabels(period, ZoneId.systemDefault()).forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted, textAlign = TextAlign.Center)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (buckets.size > 8) 4.dp else 8.dp)) {
                    buckets.forEach { bucket ->
                        Text(
                            bucket.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = Muted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartYAxis(maxLabel: String, midLabel: String, minLabel: String, height: Dp = 118.dp) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .height(height),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
    ) {
        Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = Muted)
        Text(midLabel, style = MaterialTheme.typography.labelSmall, color = Muted)
        Text(minLabel, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun ChartGrid() {
    val gridColor = GridLine
    Canvas(modifier = Modifier.fillMaxSize()) {
        repeat(3) { index ->
            val y = size.height * index / 2f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
        }
    }
}

@Composable
private fun WeightTrendCard(
    weights: List<WeightEntry>,
    unit: WeightUnit,
    targetWeightKg: Double?,
    period: TrendPeriod,
    onLogWeight: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val periodWeights = weights.filterInPeriod(period, zone)
    val latest = periodWeights.maxByOrNull { it.recordedEpochMillis }
    val average = periodWeights.takeIf { it.isNotEmpty() }?.map { it.weightKg }?.average()
    val trend = WeightTrendCalculator.calculate(periodWeights, targetWeightKg)
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = CardSurface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Weight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Average", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text(average?.let { formatWeight(it, unit, decimals = 1) } ?: "-- ${unit.label}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
                }
                Text(trendRangeLabel(period), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Slate)
            }
            WeightGraph(weights = periodWeights, unit = unit, targetWeightKg = targetWeightKg, period = period)
            Text(weightTrendSummary(trend, targetWeightKg, unit), style = MaterialTheme.typography.bodySmall, color = Slate, maxLines = 2)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(latest?.let { "Last ${formatWeight(it.weightKg, unit, 1)}" } ?: "No weights logged", color = Muted, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onLogWeight,
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Wine, contentColor = Color.White),
                ) {
                    Text("Log Weight", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun WeightGraph(weights: List<WeightEntry>, unit: WeightUnit, targetWeightKg: Double?, period: TrendPeriod) {
    val zone = ZoneId.systemDefault()
    val points = consolidatedWeightPoints(weights, period, zone)
    val targetValue = targetWeightKg?.let { displayWeightValue(it, unit) }
    val values = points.map { displayWeightValue(it.weightKg, unit) }
    val allValues = values + listOfNotNull(targetValue)
    val rawMin = allValues.minOrNull()
    val rawMax = allValues.maxOrNull()
    val axisMin = rawMin?.minus(1.0) ?: 0.0
    val axisMax = rawMax?.plus(1.0) ?: 1.0
    val axisMid = (axisMin + axisMax) / 2.0
    val labels = trendXAxisLabels(period, zone)
    val range = trendMillisRange(period, zone)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ChartYAxis(
            maxLabel = formatWeightAxisValue(axisMax, unit),
            midLabel = formatWeightAxisValue(axisMid, unit),
            minLabel = formatWeightAxisValue(axisMin, unit),
            height = 104.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val gridColor = GridLine
            val coralColor = Coral
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(GraphSurface, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                val left = 20f
                val right = size.width - 20f
                val top = 16f
                val bottom = size.height - 18f

                repeat(4) { index ->
                    val y = top + ((bottom - top) * index / 3f)
                    drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 2f)
                }

                val valueRange = (axisMax - axisMin).takeIf { it > 0.1 } ?: 1.0
                val timeRange = (range.second - range.first).takeIf { it > 1L } ?: 1L
                fun yFor(value: Double): Float = bottom - (((value - axisMin) / valueRange).toFloat() * (bottom - top))
                fun xFor(epochMillis: Long): Float = left + (((epochMillis - range.first).toDouble() / timeRange.toDouble()).toFloat() * (right - left))

                if (points.isEmpty()) {
                    drawLine(coralColor, Offset(left, (top + bottom) / 2f), Offset(right, (top + bottom) / 2f), strokeWidth = 4f, cap = StrokeCap.Round)
                    return@Canvas
                }

                val offsets = points.map { point ->
                    Offset(
                        xFor(point.epochMillis).coerceIn(left, right),
                        yFor(displayWeightValue(point.weightKg, unit)).coerceIn(top, bottom),
                    )
                }

                targetValue?.let {
                    val y = yFor(it).coerceIn(top, bottom)
                    drawLine(GoldCard, Offset(left, y), Offset(right, y), strokeWidth = 3f, cap = StrokeCap.Round)
                }

                offsets.zipWithNext().forEach { (start, end) ->
                    drawLine(coralColor, start, end, strokeWidth = 6f, cap = StrokeCap.Round)
                }
                offsets.forEach { point ->
                    drawCircle(Color.White, radius = 10f, center = point)
                    drawCircle(coralColor, radius = 6f, center = point)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { label ->
                    Text(label, style = MaterialTheme.typography.labelSmall, color = Muted, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun HistoryScreen(uiState: MyFastingAppUiState, viewModel: MyFastingAppViewModel) {
    var editing by remember { mutableStateOf<FastSession?>(null) }
    var loggingFast by remember { mutableStateOf(false) }
    val visibleSessions = uiState.sessions.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(
            title = "History",
            action = {
                Button(
                    onClick = { loggingFast = true },
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(19.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Log")
                }
            },
        )
        if (uiState.sessions.isEmpty()) {
            EmptyState("No fasts yet")
        } else {
            Surface(shape = RoundedCornerShape(18.dp), color = CardSurface, shadowElevation = 1.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("${uiState.sessions.size} saved fasts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                        Text("Showing latest ${visibleSessions.size}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Text("${uiState.stats.currentStreakDays}d streak", color = Brand, fontWeight = FontWeight.Bold)
                }
            }
            visibleSessions.forEach { session ->
                CompactHistoryRow(
                    session = session,
                    onEdit = { editing = session },
                    onDelete = { viewModel.deleteFast(session.id) },
                )
            }
        }
    }

    editing?.let { session ->
        EditFastDialog(
            session = session,
            onDismiss = { editing = null },
            onSave = { start, end ->
                viewModel.editFast(session.id, start, end)
                editing = null
            },
        )
    }
    if (loggingFast) {
        LogFastDialog(
            selectedPlan = uiState.selectedPlan,
            onDismiss = { loggingFast = false },
            onSave = { planName, targetSeconds, start, end ->
                viewModel.addCompletedFast(planName, targetSeconds, start, end)
                loggingFast = false
            },
        )
    }
}

@Composable
private fun CompactHistoryRow(session: FastSession, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = CardSurface, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(session.planName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                Text(
                    "${formatFriendlyDateTime(session.startEpochMillis)} -> ${session.endEpochMillis?.let(::formatFriendlyDateTime) ?: "Now"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                    maxLines = 1,
                )
            }
            Text(
                if (session.isActive) "Active" else TimerMath.formatDuration(session.durationMillis(session.endEpochMillis ?: System.currentTimeMillis())),
                color = Brand,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit fast", tint = Slate)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete fast", tint = Color(0xFFB3261E))
            }
        }
    }
}

private data class BackdateOption(val label: String, val millis: Long)

private val BACKDATE_OPTIONS = listOf(
    BackdateOption("Now", 0L),
    BackdateOption("30 min", 30L * 60_000L),
    BackdateOption("1 h", 60L * 60_000L),
    BackdateOption("2 h", 2L * 60L * 60_000L),
    BackdateOption("3 h", 3L * 60L * 60_000L),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartFastDialog(
    plan: FastPlan,
    onDismiss: () -> Unit,
    onStart: (Long) -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    var startMillis by remember { mutableLongStateOf(now) }
    val targetSeconds = plan.fastingMinutes * 60L
    val safeStart = startMillis.coerceAtMost(now)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start ${plan.name} fast") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Start now, or backdate if the fast already began.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BACKDATE_OPTIONS.forEach { option ->
                        val selected = abs((now - safeStart) - option.millis) <= 30_000L
                        FilterChip(
                            selected = selected,
                            onClick = { startMillis = now - option.millis },
                            label = {
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Wine,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
                EditableDateTimeBlock(
                    label = "Started fasting",
                    epochMillis = safeStart,
                    onEpochChange = { picked -> startMillis = picked.coerceAtMost(now) },
                )
                Text(
                    "Elapsed ${TimerMath.formatDuration(now - safeStart)} - planned ending ${formatFriendlyDateTime(safeStart + targetSeconds * 1_000L)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(safeStart) }) {
                Text("Start fast", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(28.dp),
        containerColor = DialogSurface,
        titleContentColor = Ink,
        textContentColor = Slate,
        tonalElevation = 0.dp,
    )
}

@Composable
private fun LogFastDialog(
    selectedPlan: FastPlan,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long, Long) -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val defaultStart = now - selectedPlan.fastingMinutes * 60_000L
    var planName by remember { mutableStateOf(selectedPlan.name) }
    var targetHours by remember { mutableStateOf((selectedPlan.fastingMinutes / 60.0).trimNumber()) }
    var startMillis by remember { mutableLongStateOf(defaultStart) }
    var endMillis by remember { mutableLongStateOf(now) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log fast") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = planName,
                    onValueChange = { planName = it.take(40) },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                )
                OutlinedTextField(
                    value = targetHours,
                    onValueChange = { targetHours = it.filter { char -> char.isDigit() || char == '.' }.take(5) },
                    label = { Text("Target hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = appTextFieldColors(),
                )
                EditableDateTimeBlock("Started", startMillis) {
                    startMillis = it
                    error = null
                }
                EditableDateTimeBlock("Ended", endMillis) {
                    endMillis = it
                    error = null
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetHours.toDoubleOrNull()?.let { (it * 3_600.0).roundToInt().toLong() }
                    if (endMillis <= startMillis || target == null) {
                        error = "Use a valid time range and target."
                    } else {
                        onSave(planName, target, startMillis, endMillis)
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(28.dp),
        containerColor = DialogSurface,
        titleContentColor = Ink,
        textContentColor = Slate,
        tonalElevation = 0.dp,
    )
}

@Composable
private fun EditFastDialog(session: FastSession, onDismiss: () -> Unit, onSave: (Long, Long) -> Unit) {
    val targetSeconds = session.displayTargetSeconds()
    val plannedEnd = session.endEpochMillis ?: (session.startEpochMillis + targetSeconds * 1_000L)
    var startMillis by remember(session.id) { mutableLongStateOf(session.startEpochMillis) }
    var endMillis by remember(session.id) { mutableLongStateOf(plannedEnd) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (session.isActive) "Edit active fast" else "Edit fast") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EditableDateTimeBlock(
                    label = "Started fasting",
                    epochMillis = startMillis,
                    onEpochChange = { pickedMillis ->
                        val safeStart = pickedMillis.coerceAtMost(System.currentTimeMillis())
                        startMillis = safeStart
                        if (session.isActive) {
                            endMillis = safeStart + targetSeconds * 1_000L
                        }
                        error = null
                    },
                )
                EditableDateTimeBlock(
                    label = if (session.isActive) "Planned ending" else "Fast ended",
                    epochMillis = endMillis,
                    onEpochChange = {
                        endMillis = it
                        error = null
                    },
                )
                Text(
                    "Duration ${TimerMath.formatDurationWithSeconds((endMillis - startMillis).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeStart = startMillis.coerceAtMost(System.currentTimeMillis())
                    val safeEnd = if (session.isActive) {
                        safeStart + targetSeconds * 1_000L
                    } else {
                        endMillis
                    }
                    startMillis = safeStart
                    endMillis = safeEnd
                    if (safeEnd <= safeStart) {
                        error = "End time must be after start time."
                    } else {
                        onSave(safeStart, safeEnd)
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(28.dp),
        containerColor = DialogSurface,
        titleContentColor = Ink,
        textContentColor = Slate,
        tonalElevation = 0.dp,
    )
}

@Composable
private fun EditableDateTimeBlock(
    label: String,
    epochMillis: Long,
    onEpochChange: (Long) -> Unit,
) {
    var pickingDateTime by remember { mutableStateOf(false) }

    Surface(
        onClick = { pickingDateTime = true },
        shape = RoundedCornerShape(18.dp),
        color = CardSurface.copy(alpha = 0.72f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted, fontWeight = FontWeight.Bold)
                Text(formatDateTime(epochMillis), style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.SemiBold)
            }
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Change $label",
                tint = Brand,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    if (pickingDateTime) {
        DateTimeWheelSheet(
            title = label,
            epochMillis = epochMillis,
            onDismiss = { pickingDateTime = false },
            onPicked = {
                onEpochChange(it)
                pickingDateTime = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeWheelSheet(
    title: String,
    epochMillis: Long,
    onDismiss: () -> Unit,
    onPicked: (Long) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initial = remember(epochMillis) { Instant.ofEpochMilli(epochMillis).atZone(zone) }
    val dates = remember(epochMillis) {
        val selectedDate = initial.toLocalDate()
        (-60..60).map { selectedDate.plusDays(it.toLong()) }
    }
    val dateLabels = remember(dates) { dateWheelLabels(dates, LocalDate.now(zone)) }
    var selectedDateIndex by remember(epochMillis) { mutableStateOf(60) }
    var selectedHour by remember(epochMillis) { mutableStateOf(hour12(initial.hour)) }
    var selectedMinute by remember(epochMillis) { mutableStateOf(initial.minute) }
    var selectedPeriod by remember(epochMillis) { mutableStateOf(if (initial.hour < 12) 0 else 1) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pickedMillis = remember(selectedDateIndex, selectedHour, selectedMinute, selectedPeriod, epochMillis) {
        val hour24 = hour24(selectedHour, selectedPeriod)
        dates[selectedDateIndex]
            .atTime(hour24, selectedMinute, initial.second, initial.nano)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = CardSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Slate)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(
                    values = dateLabels,
                    selectedIndex = selectedDateIndex,
                    onSelected = { selectedDateIndex = it },
                    modifier = Modifier.weight(1.7f),
                )
                WheelPicker(
                    values = HOURS_12,
                    selectedIndex = selectedHour - 1,
                    onSelected = { selectedHour = it + 1 },
                    modifier = Modifier.weight(0.8f),
                )
                WheelPicker(
                    values = MINUTES,
                    selectedIndex = selectedMinute,
                    onSelected = { selectedMinute = it },
                    modifier = Modifier.weight(0.8f),
                )
                WheelPicker(
                    values = AM_PM,
                    selectedIndex = selectedPeriod,
                    onSelected = { selectedPeriod = it },
                    modifier = Modifier.weight(0.8f),
                )
            }
            Button(
                onClick = { onPicked(pickedMillis) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Wine, contentColor = Color.White),
            ) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun WheelPicker(
    values: Array<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inkColor = Ink
    AndroidView(
        modifier = modifier.height(118.dp),
        factory = { context ->
            NumberPicker(context).apply {
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                wrapSelectorWheel = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setSelectionDividerHeight((1.5f * context.resources.displayMetrics.density).roundToInt())
                    setTextColor(inkColor.toArgb())
                }
            }
        },
        update = { picker ->
            picker.displayedValues = null
            picker.minValue = 0
            picker.maxValue = values.lastIndex
            picker.displayedValues = values
            picker.value = selectedIndex.coerceIn(0, values.lastIndex)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                picker.setTextColor(inkColor.toArgb())
            }
            picker.setOnValueChangedListener { _, _, newValue -> onSelected(newValue) }
        },
    )
}

private fun dateWheelLabels(dates: List<LocalDate>, today: LocalDate): Array<String> {
    return dates.map { date ->
        when (date) {
            today.minusDays(1) -> "Yesterday"
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(wheelDateFormatter)
        }
    }.toTypedArray()
}

private fun hour12(hour: Int): Int {
    val hourMod = hour % 12
    return if (hourMod == 0) 12 else hourMod
}

private fun hour24(hour12: Int, periodIndex: Int): Int {
    return when {
        periodIndex == 0 && hour12 == 12 -> 0
        periodIndex == 0 -> hour12
        hour12 == 12 -> 12
        else -> hour12 + 12
    }
}

@Composable
private fun LogWeightDialog(
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (Double, Long) -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    var weightText by remember { mutableStateOf("") }
    var recordedMillis by remember { mutableLongStateOf(now) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { value -> weightText = value.filter { it.isDigit() || it == '.' }.take(6) },
                    label = { Text("Weight (${unit.label})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = appTextFieldColors(),
                )
                EditableDateTimeBlock("Logged at", recordedMillis) {
                    recordedMillis = it
                    error = null
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = weightText.toDoubleOrNull()
                    if (value == null) {
                        error = "Enter a valid weight."
                    } else {
                        onSave(inputWeightToKg(value, unit), recordedMillis)
                    }
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(28.dp),
        containerColor = DialogSurface,
        titleContentColor = Ink,
        textContentColor = Slate,
        tonalElevation = 0.dp,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SettingsScreen(
    uiState: MyFastingAppUiState,
    viewModel: MyFastingAppViewModel,
    showMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetText by rememberSaveable(uiState.settings.weightUnit, uiState.settings.targetWeightKg) {
        mutableStateOf(uiState.settings.targetWeightKg?.let { displayWeightValue(it, uiState.settings.weightUnit).trimNumber() } ?: "")
    }
    var deleteStep by remember { mutableStateOf(0) }
    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { context.writeText(uri, viewModel.exportJson()) }
                    .onSuccess { showMessage("Backup exported with fasts, weights, and settings.") }
                    .onFailure { showMessage(it.message ?: "Export failed.") }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { context.readText(uri) }
                    .onSuccess { viewModel.importJson(it) }
                    .onFailure { showMessage(it.message ?: "Import failed.") }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(title = "Settings", action = null)
        SettingsCompactCard(title = "Appearance") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val selected = uiState.settings.themeMode == mode
                    Button(
                        onClick = { viewModel.setThemeMode(mode) },
                        modifier = Modifier
                            .weight(if (mode == ThemeMode.SYSTEM) 1.3f else 1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Wine else OptionSurface,
                            contentColor = if (selected) Color.White else Slate,
                        ),
                    ) {
                        Text(
                            mode.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(
                uiState.settings.themeMode.description,
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
        SettingsCompactCard(title = "Weight") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                WeightUnit.entries.forEach { unit ->
                    val selected = uiState.settings.weightUnit == unit
                    Button(
                        onClick = { viewModel.setWeightUnit(unit) },
                        modifier = Modifier
                            .weight(0.6f)
                            .height(36.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) Wine else OptionSurface,
                            contentColor = if (selected) Color.White else Slate,
                        ),
                    ) {
                        Text(unit.label.uppercase(), style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { char -> char.isDigit() || char == '.' }.take(6) },
                    label = { Text("Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1.4f),
                    colors = appTextFieldColors(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val targetKg = targetText.toDoubleOrNull()?.let { inputWeightToKg(it, uiState.settings.weightUnit) }
                        viewModel.setTargetWeightKg(targetKg)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text("Save target", style = MaterialTheme.typography.labelLarge)
                }
                TextButton(
                    onClick = {
                        targetText = ""
                        viewModel.setTargetWeightKg(null)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                ) {
                    Text("Clear")
                }
            }
            Text(
                weightTrendSummary(uiState.weightTrend, uiState.settings.targetWeightKg, uiState.settings.weightUnit),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
        SettingsCompactCard(title = "Notifications") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Progress alerts", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (uiState.settings.milestoneAlertsEnabled) {
                            milestoneAlertSummary(uiState.settings.milestonePercents)
                        } else {
                            "Milestone notifications off"
                        },
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = uiState.settings.milestoneAlertsEnabled,
                    onCheckedChange = { viewModel.setMilestoneAlerts(it, uiState.settings.milestonePercents) },
                    colors = settingsSwitchColors(),
                )
            }
            if (uiState.settings.milestoneAlertsEnabled) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    UserSettings.MILESTONE_OPTIONS.forEach { percent ->
                        val selected = percent in uiState.settings.milestonePercents
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val updated = if (selected) {
                                    uiState.settings.milestonePercents - percent
                                } else {
                                    uiState.settings.milestonePercents + percent
                                }
                                viewModel.setMilestoneAlerts(true, updated)
                            },
                            label = {
                                Text(
                                    "$percent%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Wine,
                                selectedLabelColor = Color.White,
                            ),
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Local reminder", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("${uiState.settings.reminderLeadMinutes} min before target", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = uiState.settings.remindersEnabled,
                    onCheckedChange = { viewModel.setReminders(it, uiState.settings.reminderLeadMinutes) },
                    colors = settingsSwitchColors(),
                )
            }
        }
        SettingsCompactCard(title = "Backup") {
            Text("JSON includes fasts, weights, target, unit, and app settings.", color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { exportJsonLauncher.launch("myfastingapp-backup.json") },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(19.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text("Export", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(19.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text("Import", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        SettingsCompactCard(title = "Local data") {
            Text("MyFastingApp only stores data on this device.", color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Button(
                onClick = { deleteStep = 1 },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                shape = RoundedCornerShape(19.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
            ) {
                Text("Delete local data", style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (deleteStep == 1) {
        AlertDialog(
            onDismissRequest = { deleteStep = 0 },
            title = { Text("Delete local data?") },
            text = { Text("This will delete fast history, weight logs, targets, and settings stored on this device only.") },
            confirmButton = { Button(onClick = { deleteStep = 2 }) { Text("Yes, continue") } },
            dismissButton = { TextButton(onClick = { deleteStep = 0 }) { Text("Cancel") } },
            shape = RoundedCornerShape(28.dp),
            containerColor = DialogSurface,
            titleContentColor = Ink,
            textContentColor = Slate,
            tonalElevation = 0.dp,
        )
    }
    if (deleteStep == 2) {
        AlertDialog(
            onDismissRequest = { deleteStep = 0 },
            title = { Text("Are you absolutely sure?") },
            text = { Text("There is no cloud backup in MyFastingApp. This removes only the data on this device, and it cannot be undone unless you exported JSON first.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllLocalData()
                        deleteStep = 0
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
                ) {
                    Text("Delete everything")
                }
            },
            dismissButton = { TextButton(onClick = { deleteStep = 0 }) { Text("Cancel") } },
            shape = RoundedCornerShape(28.dp),
            containerColor = DialogSurface,
            titleContentColor = Ink,
            textContentColor = Slate,
            tonalElevation = 0.dp,
        )
    }
}

private fun milestoneAlertSummary(percents: Set<Int>): String {
    if (percents.isEmpty()) return "No milestones selected"
    return "At " + percents.sorted().joinToString(", ") { "$it%" }
}

@Composable
private fun settingsSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Brand,
    uncheckedThumbColor = Muted,
    uncheckedTrackColor = SwitchTrackOff,
    uncheckedBorderColor = Color.Transparent,
)

@Composable
private fun SettingsCompactCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardSurface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: (@Composable () -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
        action?.invoke()
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = CardSurface, modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center, color = Muted)
    }
}

@Composable
private fun MyFastingAppTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = palette.brand,
            onPrimary = Color.White,
            primaryContainer = palette.brandTint,
            onPrimaryContainer = Color(0xFFC6C4FF),
            secondary = palette.brandSoft,
            onSecondary = Color.White,
            secondaryContainer = palette.brandTint,
            onSecondaryContainer = Color(0xFFC6C4FF),
            tertiary = BlueCard,
            onTertiary = Color.White,
            surface = palette.background,
            surfaceContainer = palette.card,
            surfaceVariant = palette.fieldSurface,
            background = palette.background,
            onBackground = palette.ink,
            onSurface = palette.ink,
            onSurfaceVariant = palette.slate,
            outline = palette.border,
            outlineVariant = palette.subtleBorder,
            error = palette.error,
            onError = Color(0xFF460605),
        )
    } else {
        lightColorScheme(
            primary = palette.brand,
            onPrimary = Color.White,
            primaryContainer = palette.brandTint,
            onPrimaryContainer = palette.wine,
            secondary = palette.brandSoft,
            onSecondary = Color.White,
            secondaryContainer = palette.brandTint,
            onSecondaryContainer = palette.wine,
            tertiary = BlueCard,
            onTertiary = Color.White,
            surface = palette.background,
            surfaceContainer = palette.card,
            surfaceVariant = palette.fieldSurface,
            background = palette.background,
            onBackground = palette.ink,
            onSurface = palette.ink,
            onSurfaceVariant = palette.slate,
            outline = palette.border,
            outlineVariant = palette.subtleBorder,
            error = palette.error,
            onError = Color.White,
        )
    }
    // Keep system bars in step with the in-app theme. The platform ignores these
    // setters on API 35+ (enforced edge-to-edge); on older devices they mirror
    // the values/ and values-night/ styles regardless of the system setting.
    val context = LocalContext.current
    SideEffect {
        val window = (context as? Activity)?.window
        if (window != null) {
            val barColor = if (darkTheme) palette.background else Color(0xFFF7FAF8)
            @Suppress("DEPRECATION")
            window.statusBarColor = barColor.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = barColor.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedContainerColor = FieldSurface,
    unfocusedContainerColor = FieldSurface,
    disabledContainerColor = FieldSurface,
    cursorColor = Brand,
    focusedBorderColor = Brand,
    unfocusedBorderColor = Border,
    focusedLabelColor = Brand,
    unfocusedLabelColor = Slate,
    focusedPlaceholderColor = Muted,
    unfocusedPlaceholderColor = Muted,
    errorBorderColor = ErrorRed,
    errorLabelColor = ErrorRed,
)

private enum class AppTab(val label: String, val icon: ImageVector) {
    Timer("Timer", Icons.Outlined.Timer),
    Fasts("Fasts", Icons.Outlined.Restaurant),
    Trends("Trends", Icons.AutoMirrored.Outlined.ShowChart),
    History("History", Icons.Outlined.History),
    Settings("Settings", Icons.Outlined.Settings),
}

private enum class TrendPeriod(val label: String) {
    Week("Week"),
    Month("Month"),
    Year("Year"),
}

private data class FastTrendBucket(
    val label: String,
    val hours: Double,
    val goalMet: Boolean,
)

private data class WeightGraphPoint(
    val epochMillis: Long,
    val weightKg: Double,
)

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val friendlyFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
private val timerPanelDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val timerPanelTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val wheelDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
private val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
private val HOURS_12 = Array(12) { (it + 1).toString() }
private val MINUTES = Array(60) { it.toString().padStart(2, '0') }
private val AM_PM = arrayOf("AM", "PM")

private fun trendDateRange(period: TrendPeriod, zone: ZoneId = ZoneId.systemDefault()): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now(zone)
    return when (period) {
        TrendPeriod.Week -> today.minusDays(6L) to today
        TrendPeriod.Month -> today.minusDays(29L) to today
        TrendPeriod.Year -> YearMonth.from(today).minusMonths(11L).atDay(1) to today
    }
}

private fun trendMillisRange(period: TrendPeriod, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    val (start, end) = trendDateRange(period, zone)
    val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = end.plusDays(1L).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
    return startMillis to endMillis
}

private fun trendRangeLabel(period: TrendPeriod): String {
    val (start, end) = trendDateRange(period)
    return when (period) {
        TrendPeriod.Year -> "${YearMonth.from(start).format(monthYearFormatter)} - ${YearMonth.from(end).format(monthYearFormatter)}"
        else -> "${start.format(shortDateFormatter)} - ${end.format(shortDateFormatter)}"
    }
}

private fun trendXAxisLabels(period: TrendPeriod, zone: ZoneId): List<String> {
    val (start, end) = trendDateRange(period, zone)
    val mid = start.plusDays(((end.toEpochDay() - start.toEpochDay()) / 2L).coerceAtLeast(0L))
    return when (period) {
        TrendPeriod.Year -> listOf(
            YearMonth.from(start).format(monthFormatter),
            YearMonth.from(mid).format(monthFormatter),
            YearMonth.from(end).format(monthFormatter),
        )
        else -> listOf(start.format(shortDateFormatter), mid.format(shortDateFormatter), end.format(shortDateFormatter))
    }
}

private fun List<FastSession>.completedInPeriod(period: TrendPeriod, zone: ZoneId): List<FastSession> {
    val (start, end) = trendDateRange(period, zone)
    return filter { session ->
        val endMillis = session.endEpochMillis ?: return@filter false
        val localDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
        localDate in start..end
    }
}

private fun List<WeightEntry>.filterInPeriod(period: TrendPeriod, zone: ZoneId): List<WeightEntry> {
    val (start, end) = trendDateRange(period, zone)
    return filter { entry ->
        val localDate = Instant.ofEpochMilli(entry.recordedEpochMillis).atZone(zone).toLocalDate()
        localDate in start..end
    }
}

private fun consolidatedWeightPoints(weights: List<WeightEntry>, period: TrendPeriod, zone: ZoneId): List<WeightGraphPoint> {
    val grouped = when (period) {
        TrendPeriod.Week -> weights.groupBy { entry ->
            Instant.ofEpochMilli(entry.recordedEpochMillis).atZone(zone).toLocalDate()
        }
        TrendPeriod.Month -> weights.groupBy { entry ->
            val date = Instant.ofEpochMilli(entry.recordedEpochMillis).atZone(zone).toLocalDate()
            date.minusDays((date.dayOfWeek.value - 1).toLong())
        }
        TrendPeriod.Year -> weights.groupBy { entry ->
            YearMonth.from(Instant.ofEpochMilli(entry.recordedEpochMillis).atZone(zone)).atDay(1)
        }
    }
    return grouped.entries
        .map { (_, entries) ->
            WeightGraphPoint(
                epochMillis = entries.map { it.recordedEpochMillis }.average().toLong(),
                weightKg = entries.map { it.weightKg }.average(),
            )
        }
        .sortedBy { it.epochMillis }
}

private fun fastTrendBuckets(sessions: List<FastSession>, period: TrendPeriod, zone: ZoneId = ZoneId.systemDefault()): List<FastTrendBucket> {
    val completed = sessions.filter { it.endEpochMillis != null }
    return when (period) {
        TrendPeriod.Week -> {
            val today = LocalDate.now(zone)
            (6 downTo 0).map { offset ->
                val day = today.minusDays(offset.toLong())
                val bucketSessions = completed.filter { session ->
                    session.endEpochMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == day } == true
                }
                FastTrendBucket(
                    label = day.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                    hours = bucketSessions.totalFastHours(),
                    goalMet = bucketSessions.anyTargetMet(),
                )
            }
        }
        TrendPeriod.Month -> {
            val today = LocalDate.now(zone)
            val currentWeekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
            (4 downTo 0).map { offset ->
                val start = currentWeekStart.minusDays(offset * 7L)
                val end = start.plusDays(6L)
                val bucketSessions = completed.filter { session ->
                    val date = Instant.ofEpochMilli(session.endEpochMillis!!).atZone(zone).toLocalDate()
                    date in start..end
                }
                FastTrendBucket(
                    label = start.format(shortDateFormatter),
                    hours = bucketSessions.totalFastHours(),
                    goalMet = bucketSessions.anyTargetMet(),
                )
            }
        }
        TrendPeriod.Year -> {
            val thisMonth = YearMonth.now(zone)
            (11 downTo 0).mapIndexed { index, offset ->
                val month = thisMonth.minusMonths(offset.toLong())
                val bucketSessions = completed.filter { session ->
                    YearMonth.from(Instant.ofEpochMilli(session.endEpochMillis!!).atZone(zone)) == month
                }
                FastTrendBucket(
                    label = if (index in setOf(0, 3, 6, 9, 11)) month.format(monthFormatter) else "",
                    hours = bucketSessions.totalFastHours(),
                    goalMet = bucketSessions.anyTargetMet(),
                )
            }
        }
    }
}

private fun List<FastSession>.totalFastHours(): Double {
    return sumOf { session -> session.durationMillis(session.endEpochMillis ?: session.startEpochMillis).toDouble() / 3_600_000.0 }
}

private fun List<FastSession>.anyTargetMet(): Boolean {
    return any { session -> session.durationMillis(session.endEpochMillis ?: session.startEpochMillis) / 1_000L >= session.targetSeconds }
}

private fun formatDateTime(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)
}

private fun formatFriendlyDateTime(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(friendlyFormatter)
}

private fun formatTimerPanelDateTime(epochMillis: Long): String {
    val local = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    return "${local.format(timerPanelDateFormatter)}\n${local.format(timerPanelTimeFormatter)}"
}

private fun FastSession.displayTargetSeconds(): Long {
    return FastPlans.builtInById(planId)?.fastingMinutes?.times(60L) ?: targetSeconds
}

private fun formatTotalFastedHours(totalSeconds: Long): String {
    val hours = (totalSeconds / 3_600L).coerceAtLeast(0L)
    return when {
        hours >= 10_000L -> "${hours / 1_000L}k h"
        hours >= 1_000L -> "%.1fk h".format(hours / 1_000.0)
        else -> "${hours}h"
    }
}

private fun displayWeightValue(weightKg: Double, unit: WeightUnit): Double {
    return when (unit) {
        WeightUnit.KG -> weightKg
        WeightUnit.LB -> weightKg.kgToLb()
    }
}

private fun inputWeightToKg(value: Double, unit: WeightUnit): Double {
    return when (unit) {
        WeightUnit.KG -> value
        WeightUnit.LB -> value.lbToKg()
    }
}

private fun formatWeight(weightKg: Double, unit: WeightUnit, decimals: Int): String {
    val value = displayWeightValue(weightKg, unit)
    val pattern = "%.${decimals}f"
    return "${pattern.format(value)} ${unit.label}"
}

private fun formatWeightAxisValue(value: Double, unit: WeightUnit): String {
    val rounded = if (unit == WeightUnit.KG) "%.1f".format(value) else value.roundToInt().toString()
    return "$rounded ${unit.label}"
}

private fun weightTrendSummary(trend: WeightTrend, targetWeightKg: Double?, unit: WeightUnit): String {
    if (!trend.hasEnoughData) return "Log at least two weights to see a trend."
    val change = formatWeight(kotlin.math.abs(trend.changeKg), unit, 1)
    val direction = when {
        trend.changeKg < -0.05 -> "lost"
        trend.changeKg > 0.05 -> "gained"
        else -> "held steady within"
    }
    val pace = formatWeight(kotlin.math.abs(trend.slopeKgPerDay) * 7.0, unit, 2)
    val target = targetWeightKg?.let { formatWeight(it, unit, 1) }
    val targetSentence = when {
        target == null -> "Set a target weight to estimate a date."
        trend.predictedTargetEpochMillis != null -> "At this pace, $target may land around ${formatFriendlyDate(trend.predictedTargetEpochMillis)}."
        else -> "Current trend is not moving toward $target yet."
    }
    return "You have $direction $change. Pace is about $pace per week. $targetSentence"
}

private fun formatFriendlyDate(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(shortDateFormatter)
}

private fun Double.trimNumber(): String {
    val rounded = ceil(this)
    return if (kotlin.math.abs(this - rounded) < 0.001) rounded.roundToInt().toString() else "%.1f".format(this)
}

private fun Context.writeText(uri: Uri, text: String) {
    contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
        ?: error("Could not open output document.")
}

private fun Context.readText(uri: Uri): String {
    return contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        ?: error("Could not open input document.")
}

/**
 * App color palette. [LightPalette] preserves the original design exactly;
 * [DarkPalette] is the matching dark variant (deep navy surfaces, brightened
 * brand indigo, light text). Tokens below expose the active palette through a
 * CompositionLocal, so call sites stay theme-agnostic.
 */
private data class AppPalette(
    val background: Color,
    val card: Color,
    val ink: Color,
    val slate: Color,
    val muted: Color,
    val brand: Color,
    val brandSoft: Color,
    val brandTint: Color,
    val wine: Color,
    val dialogSurface: Color,
    val fieldSurface: Color,
    val border: Color,
    val subtleBorder: Color,
    val mutedPink: Color,
    val statPillBorder: Color,
    val timerTrack: Color,
    val tintSurface: Color,
    val chipSurface: Color,
    val optionSurface: Color,
    val subtleSurface: Color,
    val barsChartSurface: Color,
    val graphSurface: Color,
    val gridLine: Color,
    val switchTrackOff: Color,
    val error: Color,
)

private val LightPalette = AppPalette(
    background = Color(0xFFFAF7F0),
    card = Color.White,
    ink = Color(0xFF26394D),
    slate = Color(0xFF65707C),
    muted = Color(0xFF9D969B),
    brand = Color(0xFF3230B8),
    brandSoft = Color(0xFF6D68F2),
    brandTint = Color(0xFFE9E8FF),
    wine = Color(0xFF242184),
    dialogSurface = Color(0xFFFFFCF8),
    fieldSurface = Color(0xFFFFFBF6),
    border = Color(0xFFDAD1CA),
    subtleBorder = Color(0xFFECE4DC),
    mutedPink = Color(0xFFB5A7AE),
    statPillBorder = Color(0xFFE7DED6),
    timerTrack = Color(0xFFF0ECE6),
    tintSurface = Color(0xFFEDEBFF),
    chipSurface = Color(0xFFF4EFEC),
    optionSurface = Color(0xFFF1EEEE),
    subtleSurface = Color(0xFFFAF5F1),
    barsChartSurface = Color(0xFFE9E6E3),
    graphSurface = Color(0xFFFFF7F1),
    gridLine = Color(0xFFE9DDD7),
    switchTrackOff = Color(0xFFE8E2DC),
    error = Color(0xFFB3261E),
)

private val DarkPalette = AppPalette(
    background = Color(0xFF10141C),
    card = Color(0xFF1A1F29),
    ink = Color(0xFFE8ECF3),
    slate = Color(0xFFA3ACB9),
    muted = Color(0xFF7B8593),
    brand = Color(0xFF6E6AF0),
    brandSoft = Color(0xFF9A96FF),
    brandTint = Color(0xFF242655),
    wine = Color(0xFF4C48C9),
    dialogSurface = Color(0xFF1A1F29),
    fieldSurface = Color(0xFF222836),
    border = Color(0xFF39414F),
    subtleBorder = Color(0xFF2A313D),
    mutedPink = Color(0xFFA79FAB),
    statPillBorder = Color(0xFF333B47),
    timerTrack = Color(0xFF242B37),
    tintSurface = Color(0xFF1F2149),
    chipSurface = Color(0xFF242A35),
    optionSurface = Color(0xFF242A35),
    subtleSurface = Color(0xFF1E232D),
    barsChartSurface = Color(0xFF171C25),
    graphSurface = Color(0xFF181D26),
    gridLine = Color(0xFF2C333F),
    switchTrackOff = Color(0xFF39414F),
    error = Color(0xFFFFB4AB),
)

private val LocalAppPalette = compositionLocalOf { LightPalette }

private val Cream: Color @Composable get() = LocalAppPalette.current.background
private val CardSurface: Color @Composable get() = LocalAppPalette.current.card
private val Ink: Color @Composable get() = LocalAppPalette.current.ink
private val Slate: Color @Composable get() = LocalAppPalette.current.slate
private val Muted: Color @Composable get() = LocalAppPalette.current.muted
private val Brand: Color @Composable get() = LocalAppPalette.current.brand
private val BrandSoft: Color @Composable get() = LocalAppPalette.current.brandSoft
private val BrandTint: Color @Composable get() = LocalAppPalette.current.brandTint
private val Coral: Color @Composable get() = LocalAppPalette.current.brand
private val Orange: Color @Composable get() = LocalAppPalette.current.brandSoft
private val Wine: Color @Composable get() = LocalAppPalette.current.wine
private val DialogSurface: Color @Composable get() = LocalAppPalette.current.dialogSurface
private val FieldSurface: Color @Composable get() = LocalAppPalette.current.fieldSurface
private val Border: Color @Composable get() = LocalAppPalette.current.border
private val SubtleBorder: Color @Composable get() = LocalAppPalette.current.subtleBorder
private val MutedPink: Color @Composable get() = LocalAppPalette.current.mutedPink
private val StatPillBorder: Color @Composable get() = LocalAppPalette.current.statPillBorder
private val TimerTrack: Color @Composable get() = LocalAppPalette.current.timerTrack
private val TintSurface: Color @Composable get() = LocalAppPalette.current.tintSurface
private val ChipSurface: Color @Composable get() = LocalAppPalette.current.chipSurface
private val OptionSurface: Color @Composable get() = LocalAppPalette.current.optionSurface
private val SubtleSurface: Color @Composable get() = LocalAppPalette.current.subtleSurface
private val BarsChartSurface: Color @Composable get() = LocalAppPalette.current.barsChartSurface
private val GraphSurface: Color @Composable get() = LocalAppPalette.current.graphSurface
private val GridLine: Color @Composable get() = LocalAppPalette.current.gridLine
private val SwitchTrackOff: Color @Composable get() = LocalAppPalette.current.switchTrackOff
private val ErrorRed: Color @Composable get() = LocalAppPalette.current.error

// Accent colors are intentionally identical in both themes.
private val PurpleCard = Color(0xFF6B5A95)
private val CoralCard = Color(0xFFD98273)
private val TealCard = Color(0xFF4F8E8E)
private val GoldCard = Color(0xFFE2AE62)
private val BlueCard = Color(0xFF2478A8)
private val GreenCard = Color(0xFF5D9779)
