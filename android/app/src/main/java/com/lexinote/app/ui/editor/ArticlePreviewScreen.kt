package com.lexinote.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexinote.app.dictionary.DictionaryLemmaCard
import com.lexinote.app.ui.vocabulary.WordLibraryViewModel
import com.lexinote.app.ui.vocabulary.DictionaryDialogTitle


@Composable
fun ArticlePreviewScreen(id: Long, vm: ArticleViewModel, wordVm: WordLibraryViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val article by vm.article.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val lookup by vm.lookup.collectAsStateWithLifecycle()
    val annotationState by vm.annotations.collectAsStateWithLifecycle()
    val missingPhonetic by vm.missingPhonetic.collectAsStateWithLifecycle()
    val annotationError by vm.annotationError.collectAsStateWithLifecycle()
    val phoneticFallback by vm.phoneticFallback.collectAsStateWithLifecycle()
    val savingPhonetic by vm.savingPhonetic.collectAsStateWithLifecycle()
    val starred by wordVm.starred.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPhonetics by remember { mutableStateOf(context.getSharedPreferences("reader_preferences", 0).getBoolean("show_phonetics", true)) }
    var showTranslations by remember { mutableStateOf(context.getSharedPreferences("reader_preferences", 0).getBoolean("show_translations", false)) }
    var readerMode by remember {
        mutableStateOf(if (context.getSharedPreferences("reader_preferences", 0).getBoolean("quick_reader_mode", false)) ArticleReaderMode.Quick else ArticleReaderMode.Standard)
    }
    var modeSnackbar by remember(id) { mutableStateOf<ArticleReaderMode?>(null) }
    var showPhoneticDisplayReminder by remember(id) { mutableStateOf(false) }
    var displayReminderDismissedThisSession by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id, modeSnackbar) {
        if (modeSnackbar != null) {
            kotlinx.coroutines.delay(2_000)
            modeSnackbar = null
        }
    }
    LaunchedEffect(id) { vm.clearPhoneticPrompts(); vm.clearLookup(); vm.openPreview(id) }
    LaunchedEffect(id, readerMode) { vm.setQuickModeActive(readerMode == ArticleReaderMode.Quick) }
    LaunchedEffect(vm, id) {
        vm.phoneticSaveEvents.collect { event ->
            if (event is PhoneticSaveEvent.Saved && event.articleId == id && event.changed && !showPhonetics && !displayReminderDismissedThisSession) {
                showPhoneticDisplayReminder = true
            }
        }
    }
    DisposableEffect(id) {
        vm.observeAnnotations(id)
        onDispose {
            vm.setQuickModeActive(false)
            vm.stopObservingAnnotations(id)
        }
    }
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text("‹ 返回") }
                TextButton(onClick = onEdit, enabled = article?.id == id) { Text("重新编辑") }
            }
            ReaderToolbarControls(
                mode = readerMode,
                onModeSelected = { selected ->
                    if (selected != readerMode) {
                        readerMode = selected
                        context.getSharedPreferences("reader_preferences", 0).edit()
                            .putBoolean("quick_reader_mode", selected == ArticleReaderMode.Quick).apply()
                        modeSnackbar = selected
                    }
                },
                showPhonetics = showPhonetics,
                onPhoneticsChanged = {
                    showPhonetics = it
                    context.getSharedPreferences("reader_preferences", 0).edit().putBoolean("show_phonetics", it).apply()
                },
                showTranslations = showTranslations,
                onTranslationsChanged = {
                    showTranslations = it
                    context.getSharedPreferences("reader_preferences", 0).edit().putBoolean("show_translations", it).apply()
                }
            )
            Spacer(Modifier.height(8.dp))
            annotationState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when {
                loading -> CircularProgressIndicator()
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                article?.id != id -> Text("找不到这篇文章")
                else -> {
                    Text(article!!.title.ifBlank { "未命名文章" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    key(id, article!!.content) {
                        ReaderBody(id, article!!.content, vm, showPhonetics, showTranslations, readerMode)
                    }
                }
            }
        }
        modeSnackbar?.let { mode ->
            ReaderModeSnackbar(
                mode = mode,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
    lookup?.takeIf { phoneticFallback == null && annotationError == null }?.let { state ->
        AlertDialog(
            onDismissRequest = vm::clearLookup,
            title = {
                Column(Modifier.fillMaxWidth()) {
                    val isStarred = starred.any { it.normalizedWord == state.word.trim().lowercase(java.util.Locale.ROOT) }
                    DictionaryDialogTitle(state.word, isStarred, { wordVm.toggleStar(state.word) }, vm::clearLookup)
                    val entry = (state as? LookupState.Result)?.entry
                    Text(formatDictionaryPhonetic(entry?.phonetic), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                var expandedLemmas by remember(state.word) { mutableStateOf(emptySet<String>()) }
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    when (state) {
                        is LookupState.Loading -> CircularProgressIndicator()
                        is LookupState.Failure -> Text(state.message, color = MaterialTheme.colorScheme.error)
                        is LookupState.Result -> {
                            val details = state.details
                            val entry = details.currentEntry
                            if (entry == null && details.lemmaCards.isEmpty()) Text("本地词典暂未收录")
                            else if (entry == null) Text("当前词形暂无独立释义")
                            else if (entry.senses.isEmpty()) Text("暂无独立释义")
                            else {
                                entry.senses.groupBy { it.pos }.entries.toList().forEachIndexed { index, (pos, senses) ->
                                    if (index > 0) HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Text(if (pos == "unknown") "词性暂无" else formatDictionaryPartOfSpeech(pos), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (index == 0) 8.dp else 0.dp))
                                    senses.sortedBy { it.order }.forEach { sense ->
                                        Text(sense.meaning, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            }
                            if (details.currentRelations.isNotEmpty()) {
                                Text("词形变化关系", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                                details.currentRelations.distinctBy { listOf(it.lemma.lowercase(), it.form.lowercase(), it.pos, it.formType) }.forEach { relation ->
                                    Text(
                                        "${relation.lemma} → ${relation.form}（${formTypeLabel(relation.formType)}）",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                            if (details.lemmaCards.isNotEmpty()) {
                                Text("原形词典", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                                details.lemmaCards.forEach { lemmaCard ->
                                    val lemma = lemmaCard.entry?.word ?: return@forEach
                                    val expanded = lemma.lowercase() in expandedLemmas
                                    LemmaDictionaryCardSection(
                                        lemmaCard = lemmaCard,
                                        expanded = expanded,
                                        onToggle = { expandedLemmas = if (expanded) expandedLemmas - lemma.lowercase() else expandedLemmas + lemma.lowercase() }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                    val result = state as? LookupState.Result
                    val annotationsReady = annotationState.articleId == id && !annotationState.loading && annotationState.error == null
                    val hasSavedPhonetic = result?.let { selected ->
                        annotationsReady && annotationState.rows.any {
                            it.startOffset == selected.start && it.endOffset == selected.end &&
                                it.word.equals(selected.word, ignoreCase = true) && it.phonetic.isNotBlank()
                        }
                    } == true
                    TextButton(
                        onClick = { if (hasSavedPhonetic) vm.clearCurrentPhonetic(id) else vm.requestSaveCurrentPhonetic(id) },
                        enabled = result != null && annotationsReady && !savingPhonetic
                    ) {
                        Text(when {
                            savingPhonetic -> "处理中…"
                            !annotationsReady -> "加载标注…"
                            hasSavedPhonetic -> "取消音标"
                            else -> "保留音标"
                        })
                    }
            }
        )
    }
    phoneticFallback?.let { request ->
        var selectedLemma by remember(request) { mutableStateOf(request.choices.firstOrNull()?.lemma) }
        val selectedChoice = request.choices.firstOrNull { it.lemma == selectedLemma }
        AlertDialog(
            onDismissRequest = vm::dismissPhoneticFallback,
            title = { Text("当前词形暂无音标") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前单词 ${request.word} 暂无独立音标，已找到关联原形 ${selectedChoice?.lemma ?: request.choices.first().lemma}。")
                    if (request.choices.size > 1) {
                        request.choices.forEach { choice ->
                            Row(
                                Modifier.fillMaxWidth().selectable(selectedLemma == choice.lemma, role = Role.RadioButton, onClick = { selectedLemma = choice.lemma }).padding(vertical = 3.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedLemma == choice.lemma, onClick = { selectedLemma = choice.lemma })
                                Column(Modifier.padding(start = 6.dp)) {
                                    Text("原形：${choice.lemma}")
                                    Text("音标：${formatDictionaryPhonetic(choice.phonetic)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } else {
                        val choice = request.choices.first()
                        Text("原形：${choice.lemma}")
                        Text("音标：${formatDictionaryPhonetic(choice.phonetic)}")
                    }
                    Text("原形音标可能与当前词形的实际发音不同，是否保留原形音标作为参考？", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLemma?.let(vm::confirmFallbackPhonetic) }, enabled = selectedLemma != null && !savingPhonetic) { Text(if (savingPhonetic) "保存中…" else "保留原形音标") }
            },
            dismissButton = { TextButton(onClick = vm::dismissPhoneticFallback) { Text("取消") } }
        )
    } ?: if (missingPhonetic) {
        AlertDialog(
            onDismissRequest = vm::dismissMissingPhonetic,
            title = { Text("暂无可用音标") },
            text = { Text("当前词形和已找到的关联原形均没有可用音标，未保存空标注。") },
            confirmButton = { TextButton(onClick = vm::dismissMissingPhonetic) { Text("关闭") } }
        )
    } else if (annotationError != null) {
        AlertDialog(onDismissRequest = vm::dismissAnnotationError, title = { Text("保存失败") }, text = { Text(annotationError!!) }, confirmButton = { TextButton(onClick = vm::dismissAnnotationError) { Text("关闭") } })
    } else if (showPhoneticDisplayReminder) {
        AlertDialog(
            onDismissRequest = { showPhoneticDisplayReminder = false; displayReminderDismissedThisSession = true },
            title = { Text("音标显示已关闭") },
            text = { Text("音标已成功保存，是否开启显示？") },
            confirmButton = {
                TextButton(onClick = {
                    showPhonetics = true
                    context.getSharedPreferences("reader_preferences", 0).edit().putBoolean("show_phonetics", true).apply()
                    showPhoneticDisplayReminder = false
                }) { Text("开启显示") }
            },
            dismissButton = { TextButton(onClick = { showPhoneticDisplayReminder = false; displayReminderDismissedThisSession = true }) { Text("暂不开启") } }
        )
    } else Unit
}

@Composable
private fun ReaderToolbarControls(
    mode: ArticleReaderMode,
    onModeSelected: (ArticleReaderMode) -> Unit,
    showPhonetics: Boolean,
    onPhoneticsChanged: (Boolean) -> Unit,
    showTranslations: Boolean,
    onTranslationsChanged: (Boolean) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        if (maxWidth >= 280.dp && fontScale <= 1.35f) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ReaderModeButton(Modifier.weight(1f), ArticleReaderMode.Standard, mode == ArticleReaderMode.Standard, onModeSelected)
                    ReaderModeButton(Modifier.weight(1f), ArticleReaderMode.Quick, mode == ArticleReaderMode.Quick, onModeSelected)
                }
                Spacer(Modifier.width(4.dp))
                VerticalDivider(Modifier.height(22.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.width(4.dp))
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    MiniReaderSwitch(Modifier.weight(1f), "音标", showPhonetics, onPhoneticsChanged)
                    MiniReaderSwitch(Modifier.weight(1f), "翻译", showTranslations, onTranslationsChanged)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ReaderModeButton(Modifier.weight(1f), ArticleReaderMode.Standard, mode == ArticleReaderMode.Standard, onModeSelected)
                    ReaderModeButton(Modifier.weight(1f), ArticleReaderMode.Quick, mode == ArticleReaderMode.Quick, onModeSelected)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    MiniReaderSwitch(Modifier.weight(1f), "音标", showPhonetics, onPhoneticsChanged)
                    MiniReaderSwitch(Modifier.weight(1f), "翻译", showTranslations, onTranslationsChanged)
                }
            }
        }
    }
}

@Composable
private fun ReaderModeButton(
    modifier: Modifier,
    mode: ArticleReaderMode,
    selected: Boolean,
    onSelected: (ArticleReaderMode) -> Unit
) {
    val shape = RoundedCornerShape(11.dp)
    val selectedColor = if (mode == ArticleReaderMode.Standard) Color(0xFF3067DF) else Color(0xFFE66A32)
    val targetBackground = if (selected) selectedColor else Color.Transparent
    val targetForeground = if (selected) Color.White else Color(0xFF303030)
    val background by animateColorAsState(targetBackground, animationSpec = tween(180), label = "readerModeBackground")
    val foreground by animateColorAsState(targetForeground, animationSpec = tween(180), label = "readerModeForeground")
    val border by animateColorAsState(if (selected) Color.Transparent else Color(0xFF303030), animationSpec = tween(180), label = "readerModeBorder")

    Box(
        modifier = modifier.height(48.dp).clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelected(mode) }),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(32.dp),
            shape = shape,
            color = background,
            border = BorderStroke(1.dp, border)
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReaderModeGlyph(mode, foreground, Modifier.size(13.dp))
                Spacer(Modifier.width(2.dp))
                Text(
                    if (mode == ArticleReaderMode.Standard) "标准" else "快速",
                    color = foreground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun ReaderModeGlyph(mode: ArticleReaderMode, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        if (mode == ArticleReaderMode.Standard) {
            val book = Path().apply {
                moveTo(3.5f * unit, 4f * unit)
                lineTo(8.5f * unit, 4f * unit)
                quadraticTo(12f * unit, 4f * unit, 12f * unit, 7f * unit)
                lineTo(12f * unit, 20f * unit)
                quadraticTo(10.5f * unit, 17.5f * unit, 7.5f * unit, 17.5f * unit)
                lineTo(3.5f * unit, 17.5f * unit)
                close()
                moveTo(20.5f * unit, 4f * unit)
                lineTo(15.5f * unit, 4f * unit)
                quadraticTo(12f * unit, 4f * unit, 12f * unit, 7f * unit)
                lineTo(12f * unit, 20f * unit)
                quadraticTo(13.5f * unit, 17.5f * unit, 16.5f * unit, 17.5f * unit)
                lineTo(20.5f * unit, 17.5f * unit)
                close()
            }
            drawPath(book, color, style = Stroke(width = 1.8f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            val bolt = Path().apply {
                moveTo(13.5f * unit, 2f * unit)
                lineTo(5f * unit, 13f * unit)
                lineTo(10.5f * unit, 13f * unit)
                lineTo(9.5f * unit, 22f * unit)
                lineTo(19f * unit, 9f * unit)
                lineTo(13f * unit, 9f * unit)
                close()
            }
            drawPath(bolt, color)
        }
    }
}

@Composable
private fun MiniReaderSwitch(modifier: Modifier, label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(if (checked) 1f else 0f, animationSpec = tween(150), label = "${label}SwitchPosition")
    val trackColor by animateColorAsState(
        if (checked) colors.primary else colors.surfaceVariant,
        animationSpec = tween(150),
        label = "${label}SwitchTrack"
    )
    val thumbColor by animateColorAsState(
        if (checked) colors.onPrimary else colors.outline,
        animationSpec = tween(150),
        label = "${label}SwitchThumb"
    )
    Row(
        modifier = modifier.heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, fontSize = 12.sp, maxLines = 1, softWrap = false)
        Canvas(Modifier.size(width = 28.dp, height = 16.dp)) {
            drawRoundRect(
                color = trackColor,
                size = size,
                cornerRadius = CornerRadius(size.height / 2f)
            )
            val radius = 6.dp.toPx()
            val inset = 2.dp.toPx()
            val centerX = radius + inset + (size.width - 2f * (radius + inset)) * progress
            drawCircle(thumbColor, radius = radius, center = Offset(centerX, size.height / 2f))
        }
    }
}

@Composable
private fun ReaderModeSnackbar(mode: ArticleReaderMode, modifier: Modifier = Modifier) {
    val standard = mode == ArticleReaderMode.Standard
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF323232),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderModeGlyph(mode, if (standard) Color(0xFF82A7FF) else Color(0xFFFF9A6C), Modifier.size(20.dp))
            Column {
                Text(
                    if (standard) "已切换到标准模式" else "已切换到快速模式",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    if (standard) "点击单词查看词典卡片" else "轻点保留或取消音标，长按查看词典",
                    color = Color(0xFFD0D0D0),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun LemmaDictionaryCardSection(lemmaCard: DictionaryLemmaCard, expanded: Boolean, onToggle: () -> Unit) {
    val entry = lemmaCard.entry ?: return
    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(if (expanded) "收起　⌃" else "展开　⌄", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            if (lemmaCard.relationTypes.isNotEmpty()) {
                Text("当前词形：${lemmaCard.relationTypes.distinct().joinToString(" / ", transform = ::formTypeLabel)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (expanded) {
                Text(formatDictionaryPhonetic(entry.phonetic), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (entry.senses.isEmpty()) Text("暂无独立释义", style = MaterialTheme.typography.bodyMedium)
                else entry.senses.groupBy { it.pos }.entries.forEachIndexed { index, (pos, senses) ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 5.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(if (pos == "unknown") "词性暂无" else formatDictionaryPartOfSpeech(pos), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    senses.sortedBy { it.order }.forEach { Text(it.meaning, style = MaterialTheme.typography.bodySmall) }
                }
                if (lemmaCard.forms.isNotEmpty()) {
                    Text("相关词形", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    lemmaCard.forms.distinctBy { listOf(it.form.lowercase(), it.pos, it.formType) }.forEach { form ->
                        Text("${form.form}　${formTypeLabel(form.formType)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun formTypeLabel(type: String): String = when (type) {
    "plural" -> "复数"
    "past" -> "过去式"
    "past_participle" -> "过去分词"
    "present_participle" -> "现在分词"
    "third_person" -> "第三人称单数"
    "comparative" -> "比较级"
    "superlative" -> "最高级"
    else -> type
}
