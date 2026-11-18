package com.bookverse.contacts.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.bookverse.contacts.feature.games.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.data.hangman.HangmanWords
import com.bookverse.contacts.domain.games.GameCompletion
import com.bookverse.contacts.domain.games.GameId
import com.bookverse.contacts.domain.games.GameOutcome
import com.bookverse.contacts.domain.games.GameStatsRepository
import com.bookverse.contacts.domain.games.GameStatsSnapshot
import com.bookverse.contacts.domain.games.HANGMAN_ALPHABET
import com.bookverse.contacts.domain.games.HANGMAN_MAX_MISTAKES
import com.bookverse.contacts.domain.games.HangmanGuessOutcome
import com.bookverse.contacts.domain.games.HangmanGuessState
import com.bookverse.contacts.domain.games.guessHangmanLetter
import com.bookverse.contacts.domain.games.isHangmanAsciiLetter
import com.bookverse.contacts.domain.games.isHangmanLost
import com.bookverse.contacts.domain.games.isHangmanWon
import com.bookverse.contacts.settings.AnimationSpeed
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.RetroGameOptionsDialog
import com.bookverse.contacts.ui.components.GameSetupDialog
import com.bookverse.contacts.ui.components.GameSetupOption
import com.bookverse.contacts.ui.components.ScoreCard
import com.bookverse.contacts.ui.components.ScoreCardEntry
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.retroGameDialogBlur
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.utils.VibrationUtils
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RotateCcw
import java.util.Locale
import kotlin.math.max

private const val HANGMAN_KEY_SIZE_DP = 48
private const val HANGMAN_KEY_SPACING_DP = 4

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HangmanScreen(
    onBackClick: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    gameStatsRepository: GameStatsRepository,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val animationSpeed by appSettingsViewModel.animationSpeed.collectAsStateWithLifecycle()
    val vibrationDuration by appSettingsViewModel.vibrationDuration.collectAsStateWithLifecycle()
    val vibrationPattern by appSettingsViewModel.vibrationPattern.collectAsStateWithLifecycle()
    val stats by gameStatsRepository.stats.collectAsStateWithLifecycle(
        initialValue = GameStatsSnapshot(),
    )
    val context = LocalContext.current
    val resources = LocalResources.current
    val selectedLocale = LocaleController.effectiveLocale(context)
    val customCategory = resources.getString(R.string.hangman_category_custom)
    val gameSession = rememberGameSessionRecorder(GameId.HANGMAN, gameStatsRepository)
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior(expandable = false)

    var showCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var targetWord by remember { mutableStateOf("") }
    var guessedLetters by remember { mutableStateOf(emptySet<Char>()) }
    var mistakes by remember { mutableIntStateOf(0) }
    var hasStarted by remember { mutableStateOf(false) }
    var showGameModeSelection by remember { mutableStateOf(true) }
    var setupSelectedIndex by remember { mutableIntStateOf(0) }
    var isHardMode by remember { mutableStateOf(false) }
    var showCustomWordDialog by remember { mutableStateOf(false) }
    var customWordInput by remember { mutableStateOf("") }
    var customWordError by remember { mutableStateOf<String?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var lastGuessOutcome by remember { mutableStateOf<HangmanGuessOutcome?>(null) }
    var guessFeedback by remember { mutableStateOf<String?>(null) }

    val anyDialogVisible =
        showGameModeSelection || showCategoryDialog || showCustomWordDialog
    val blurRadius = retroGameDialogBlur(anyDialogVisible)
    val isWon = isHangmanWon(targetWord, guessedLetters)
    val isLost = isHangmanLost(mistakes)
    val gameOutcome = when {
        isWon -> GameOutcome.WIN
        isLost -> GameOutcome.LOSS
        else -> null
    }
    val hangmanWins = stats.games[GameId.HANGMAN]?.wins ?: 0
    val selectedCategoryLabel = when (selectedCategory) {
        HangmanWords.ANIMALS -> stringResource(R.string.hangman_category_animals)
        HangmanWords.COLORS -> stringResource(R.string.hangman_category_colors)
        HangmanWords.COUNTRIES -> stringResource(R.string.hangman_category_countries)
        HangmanWords.FRUITS -> stringResource(R.string.hangman_category_fruits)
        HangmanWords.PROFESSIONS -> stringResource(R.string.hangman_category_professions)
        HangmanWords.SPORTS -> stringResource(R.string.hangman_category_sports)
        else -> customCategory
    }

    fun exitGame() {
        gameSession.abandon()
        onBackClick()
    }

    fun resetRound() {
        gameSession.abandon()
        targetWord = ""
        guessedLetters = emptySet()
        mistakes = 0
        hasStarted = false
        selectedCategory = ""
        showCategoryDialog = true
        showGameModeSelection = false
        showCustomWordDialog = false
        showResultDialog = false
        customWordInput = ""
        customWordError = null
        lastGuessOutcome = null
        guessFeedback = null
    }

    fun startRound() {
        hasStarted = true
        lastGuessOutcome = null
        guessFeedback = null
        gameSession.start()
    }

    LaunchedEffect(selectedCategory, selectedLocale) {
        if (selectedCategory.isNotEmpty() && selectedCategory != customCategory) {
            targetWord = HangmanWords.getRandomWord(selectedCategory, selectedLocale)
                .lowercase(selectedLocale)
            guessedLetters = emptySet()
            mistakes = 0
            lastGuessOutcome = null
            guessFeedback = null
        }
    }

    LaunchedEffect(gameOutcome, hasStarted) {
        if (!hasStarted || gameOutcome == null) return@LaunchedEffect

        showResultDialog = true
        gameSession.complete(GameCompletion(outcome = gameOutcome))
    }

    fun submitLetter(letter: Char) {
        if (!hasStarted || targetWord.isEmpty() || isWon || isLost) return

        val result = guessHangmanLetter(
            targetWord = targetWord,
            state = HangmanGuessState(
                guessedLetters = guessedLetters,
                mistakes = mistakes,
            ),
            letter = letter,
            isHardMode = isHardMode,
        )
        guessedLetters = result.state.guessedLetters
        mistakes = result.state.mistakes
        lastGuessOutcome = result.outcome
        guessFeedback = when (result.outcome) {
            HangmanGuessOutcome.CORRECT -> resources.getString(R.string.hangman_feedback_correct)
            HangmanGuessOutcome.INCORRECT -> resources.getString(R.string.hangman_feedback_incorrect)
            HangmanGuessOutcome.DUPLICATE -> resources.getString(R.string.hangman_feedback_duplicate)
        }
        VibrationUtils.vibrate(context, vibrationDuration, vibrationPattern)
    }

    BackHandler(enabled = showCustomWordDialog) {
        showCustomWordDialog = false
        showCategoryDialog = true
        customWordInput = ""
        customWordError = null
    }

    BackHandler(enabled = !showCustomWordDialog && !showResultDialog) {
        exitGame()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.hangman_title),
                onBackClick = ::exitGame,
                scrollBehavior = scrollBehavior,
                actions = {
                    if (targetWord.isNotEmpty() && !anyDialogVisible && !showResultDialog) {
                        IconAction(
                            imageVector = Lucide.RotateCcw,
                            contentDescription = stringResource(R.string.cd_restart_game),
                            onClick = ::resetRound,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .blur(blurRadius),
        ) {
            if (maxWidth >= 600.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HangmanBoardPanel(
                        mistakes = mistakes,
                        animationSpeed = animationSpeed,
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                    )
                    Column(
                        modifier = Modifier.widthIn(min = 260.dp, max = 360.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (selectedCategory.isNotEmpty()) {
                            HangmanCategoryBadge(
                                category = selectedCategoryLabel,
                                accentColor = accentColor,
                            )
                        }
                        HangmanStatsCard(
                            isHardMode = isHardMode,
                            wins = hangmanWins,
                        )
                        HangmanWordPanel(
                            targetWord = targetWord,
                            guessedLetters = guessedLetters,
                            selectedLocale = selectedLocale,
                            accentColor = accentColor,
                            animationSpeed = animationSpeed,
                        )
                        HangmanKeyboardPanel(
                            targetWord = targetWord,
                            guessedLetters = guessedLetters,
                            hasStarted = hasStarted,
                            isWon = isWon,
                            isLost = isLost,
                            isHardMode = isHardMode,
                            selectedLocale = selectedLocale,
                            accentColor = accentColor,
                            animationSpeed = animationSpeed,
                            onLetterClick = ::submitLetter,
                        )
                        HangmanGuessFeedback(
                            feedback = guessFeedback,
                            outcome = lastGuessOutcome,
                            accentColor = accentColor,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (selectedCategory.isNotEmpty()) {
                        HangmanCategoryBadge(
                            category = selectedCategoryLabel,
                            accentColor = accentColor,
                        )
                    }
                    HangmanStatsCard(
                        isHardMode = isHardMode,
                        wins = hangmanWins,
                    )
                    HangmanBoardPanel(
                        mistakes = mistakes,
                        animationSpeed = animationSpeed,
                        accentColor = accentColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    HangmanWordPanel(
                        targetWord = targetWord,
                        guessedLetters = guessedLetters,
                        selectedLocale = selectedLocale,
                        accentColor = accentColor,
                        animationSpeed = animationSpeed,
                    )
                    HangmanKeyboardPanel(
                        targetWord = targetWord,
                        guessedLetters = guessedLetters,
                        hasStarted = hasStarted,
                        isWon = isWon,
                        isLost = isLost,
                        isHardMode = isHardMode,
                        selectedLocale = selectedLocale,
                        accentColor = accentColor,
                        animationSpeed = animationSpeed,
                        onLetterClick = ::submitLetter,
                    )
                    HangmanGuessFeedback(
                        feedback = guessFeedback,
                        outcome = lastGuessOutcome,
                        accentColor = accentColor,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }

    if (showResultDialog && targetWord.isNotEmpty() && hasStarted && (isWon || isLost)) {
        val dialogTitle = if (isWon) {
            stringResource(R.string.hangman_win_title)
        } else {
            stringResource(R.string.lose_title)
        }
        val dialogMessage = if (isWon) {
            stringResource(R.string.hangman_win_msg, targetWord)
        } else {
            stringResource(R.string.hangman_lose_msg, targetWord)
        }
        MessageDialog(
            title = dialogTitle,
            message1 = dialogMessage,
            buttonText = stringResource(R.string.play_again),
            onDismiss = ::resetRound,
            onButtonClick = ::resetRound,
        )
    }

    if (showGameModeSelection) {
        GameSetupDialog(
            title = stringResource(R.string.hangman_setup_title),
            instructions = stringResource(R.string.hangman_instructions_text),
            options = listOf(
                GameSetupOption(
                    title = stringResource(R.string.hangman_mode_easy),
                    description = stringResource(R.string.hangman_mode_easy_description),
                ),
                GameSetupOption(
                    title = stringResource(R.string.hangman_mode_hard),
                    description = stringResource(R.string.hangman_mode_hard_description),
                ),
            ),
            selectedIndex = setupSelectedIndex,
            onOptionSelected = { setupSelectedIndex = it },
            accentColor = accentColor,
            onStart = {
                isHardMode = setupSelectedIndex == 1
                showGameModeSelection = false
                showCategoryDialog = true
            },
            onDismiss = ::exitGame,
        )
        return
    }

    if (showCategoryDialog) {
        val baseCategories = HangmanWords.categories
        val categoryOptions = baseCategories.map { category ->
            when (category) {
                HangmanWords.ANIMALS -> stringResource(R.string.hangman_category_animals)
                HangmanWords.COLORS -> stringResource(R.string.hangman_category_colors)
                HangmanWords.COUNTRIES -> stringResource(R.string.hangman_category_countries)
                HangmanWords.FRUITS -> stringResource(R.string.hangman_category_fruits)
                HangmanWords.PROFESSIONS -> stringResource(R.string.hangman_category_professions)
                HangmanWords.SPORTS -> stringResource(R.string.hangman_category_sports)
                else -> category
            }
        } + customCategory

        RetroGameOptionsDialog(
            title = stringResource(R.string.hangman_select_category),
            options = categoryOptions,
            onOptionSelected = { index ->
                if (index == categoryOptions.lastIndex) {
                    customWordInput = ""
                    customWordError = null
                    showCategoryDialog = false
                    showCustomWordDialog = true
                } else {
                    selectedCategory = baseCategories[index]
                    showCategoryDialog = false
                    startRound()
                }
            },
            onDismiss = ::exitGame,
        )
    }

    if (showCustomWordDialog) {
        HangmanCustomWordDialog(
            value = customWordInput,
            error = customWordError,
            accentColor = accentColor,
            onValueChange = { value ->
                val filtered = value.filter { isHangmanAsciiLetter(it) || it.isWhitespace() }
                val collapsed = filtered.replace(Regex("\\s+"), " ")
                if (collapsed.length <= 20) {
                    customWordInput = collapsed
                    customWordError = null
                }
            },
            onConfirm = {
                val sanitized = customWordInput
                    .filter { isHangmanAsciiLetter(it) || it.isWhitespace() }
                    .trim()
                    .replace(Regex("\\s+"), " ")
                when {
                    sanitized.isEmpty() || !sanitized.any(::isHangmanAsciiLetter) -> {
                        customWordError = resources.getString(
                            R.string.hangman_custom_word_error_empty,
                        )
                    }

                    sanitized.length > 20 -> {
                        customWordError = resources.getString(
                            R.string.hangman_custom_word_error_length,
                        )
                    }

                    else -> {
                        selectedCategory = customCategory
                        targetWord = sanitized.lowercase(selectedLocale)
                        guessedLetters = emptySet()
                        mistakes = 0
                        showCustomWordDialog = false
                        startRound()
                        customWordInput = ""
                        customWordError = null
                    }
                }
            },
            onDismiss = {
                showCustomWordDialog = false
                showCategoryDialog = true
                customWordInput = ""
                customWordError = null
            },
        )
    }
}

@Composable
private fun HangmanStatsCard(
    isHardMode: Boolean,
    wins: Int,
) {
    val levelLabel = if (isHardMode) {
        stringResource(R.string.hangman_level_hard)
    } else {
        stringResource(R.string.hangman_level_easy)
    }
    ScoreCard(
        entries = listOf(
            ScoreCardEntry(
                title = stringResource(R.string.hangman_level_label),
                value = levelLabel,
            ),
            ScoreCardEntry(
                title = stringResource(R.string.hangman_wins_label),
                value = wins.toString(),
            ),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun HangmanCategoryBadge(
    category: String,
    accentColor: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.hangman_category_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HangmanBoardPanel(
    mistakes: Int,
    animationSpeed: AnimationSpeed,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val animatedMistakes by animateFloatAsState(
        targetValue = mistakes.toFloat(),
        animationSpec = hangmanFloatAnimationSpec(animationSpeed),
        label = "hangman_drawing_progress",
    )
    val drawingDescription = stringResource(
        R.string.hangman_drawing_description,
        mistakes,
        HANGMAN_MAX_MISTAKES,
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .aspectRatio(1f)
                .semantics { contentDescription = drawingDescription },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                HangmanDrawing(
                    mistakes = animatedMistakes,
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                )
            }
        }
        HangmanAttemptMeter(
            mistakes = mistakes,
            accentColor = accentColor,
        )
    }
}

@Composable
private fun HangmanAttemptMeter(
    mistakes: Int,
    accentColor: Color,
) {
    val remainingAttempts = (HANGMAN_MAX_MISTAKES - mistakes).coerceAtLeast(0)
    val remainingColor = if (remainingAttempts <= 2) {
        MaterialTheme.colorScheme.error
    } else {
        accentColor
    }
    val meterDescription = stringResource(
        R.string.hangman_attempts_description,
        remainingAttempts,
        HANGMAN_MAX_MISTAKES,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = meterDescription
                liveRegion = LiveRegionMode.Polite
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.hangman_attempts_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.hangman_attempts_value,
                    remainingAttempts,
                    HANGMAN_MAX_MISTAKES,
                ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = remainingColor,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(HANGMAN_MAX_MISTAKES) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (index < remainingAttempts) {
                                remainingColor
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun HangmanWordPanel(
    targetWord: String,
    guessedLetters: Set<Char>,
    selectedLocale: Locale,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
) {
    val maskedWord = targetWord.map { letter ->
        when {
            letter.isWhitespace() -> " "
            letter.lowercaseChar() in guessedLetters -> letter.toString().uppercase(selectedLocale)
            else -> "_"
        }
    }.joinToString("")
    val wordDescription = stringResource(
        R.string.hangman_word_description,
        maskedWord.ifEmpty { "—" },
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.hangman_word_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .semantics { contentDescription = wordDescription },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (targetWord.isEmpty()) {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                } else {
                    targetWord.forEachIndexed { index, letter ->
                        if (letter.isWhitespace()) {
                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            HangmanLetterTile(
                                letter = letter,
                                revealed = letter.lowercaseChar() in guessedLetters,
                                selectedLocale = selectedLocale,
                                accentColor = accentColor,
                                animationSpeed = animationSpeed,
                                index = index,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HangmanLetterTile(
    letter: Char,
    revealed: Boolean,
    selectedLocale: Locale,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
    index: Int,
) {
    val scale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.96f,
        animationSpec = hangmanFloatAnimationSpec(animationSpeed),
        label = "hangman_letter_$index",
    )
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier
            .size(width = 42.dp, height = 52.dp)
            .scale(scale),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (revealed) {
                accentColor.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            },
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (revealed) {
                    letter.toString().uppercase(selectedLocale)
                } else {
                    "_"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (revealed) {
                    accentColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun HangmanKeyboardPanel(
    targetWord: String,
    guessedLetters: Set<Char>,
    hasStarted: Boolean,
    isWon: Boolean,
    isLost: Boolean,
    isHardMode: Boolean,
    selectedLocale: Locale,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
    onLetterClick: (Char) -> Unit,
) {
    val letters = remember { HANGMAN_ALPHABET }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.hangman_keyboard_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = max(
                    1,
                    ((maxWidth.value + HANGMAN_KEY_SPACING_DP) /
                        (HANGMAN_KEY_SIZE_DP + HANGMAN_KEY_SPACING_DP)).toInt(),
                )
                val rows = (letters.size + columns - 1) / columns
                val gridHeight = (
                    rows * HANGMAN_KEY_SIZE_DP +
                        (rows - 1) * HANGMAN_KEY_SPACING_DP
                    ).dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(HANGMAN_KEY_SPACING_DP.dp),
                    verticalArrangement = Arrangement.spacedBy(HANGMAN_KEY_SPACING_DP.dp),
                    userScrollEnabled = false,
                ) {
                    items(
                        items = letters,
                        key = { letter -> letter.code },
                    ) { letter ->
                        HangmanKeyboardKey(
                            letter = letter,
                            targetWord = targetWord,
                            guessedLetters = guessedLetters,
                            showUsedState = !isHardMode,
                            enabled = hasStarted && !isWon && !isLost &&
                                (letter !in guessedLetters || isHardMode),
                            selectedLocale = selectedLocale,
                            accentColor = accentColor,
                            animationSpeed = animationSpeed,
                            onClick = { onLetterClick(letter) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HangmanKeyboardKey(
    letter: Char,
    targetWord: String,
    guessedLetters: Set<Char>,
    showUsedState: Boolean,
    enabled: Boolean,
    selectedLocale: Locale,
    accentColor: Color,
    animationSpeed: AnimationSpeed,
    onClick: () -> Unit,
) {
    val isUsed = showUsedState && letter in guessedLetters
    val isCorrect = isUsed && targetWord.any { it.lowercaseChar() == letter }
    val isIncorrect = isUsed && !isCorrect
    val keyLabel = letter.toString().uppercase(selectedLocale)
    val stateLabel = if (isUsed) {
        val resultLabel = if (isCorrect) {
            stringResource(R.string.hangman_letter_correct)
        } else {
            stringResource(R.string.hangman_letter_incorrect)
        }
        stringResource(R.string.hangman_letter_used_state, resultLabel)
    } else {
        stringResource(R.string.hangman_letter_available)
    }
    val keyDescription = stringResource(
        R.string.hangman_keyboard_letter_description,
        keyLabel,
        stateLabel,
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = hangmanFloatAnimationSpec(animationSpeed),
        label = "hangman_key_$letter",
    )
    val keyColor = when {
        isCorrect -> accentColor.copy(alpha = 0.16f)
        isIncorrect -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isCorrect -> accentColor
        isIncorrect -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when {
        isCorrect -> accentColor.copy(alpha = 0.55f)
        isIncorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HANGMAN_KEY_SIZE_DP.dp)
            .scale(scale)
            .semantics {
                contentDescription = keyDescription
                stateDescription = stateLabel
                role = Role.Button
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(14.dp),
        color = keyColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = keyLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            when {
                isCorrect -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(13.dp),
                    )
                }

                isIncorrect -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HangmanGuessFeedback(
    feedback: String?,
    outcome: HangmanGuessOutcome?,
    accentColor: Color,
) {
    if (feedback == null || outcome == null) return

    val isCorrect = outcome == HangmanGuessOutcome.CORRECT
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(16.dp),
        color = if (isCorrect) {
            accentColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
    ) {
        Text(
            text = feedback,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isCorrect) accentColor else MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HangmanCustomWordDialog(
    value: String,
    error: String?,
    accentColor: Color,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = onSurface.copy(alpha = 0.7f)
    val onAccent = contentColorFor(accentColor)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        titleContentColor = onSurface,
        textContentColor = onSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(R.string.hangman_custom_word_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(stringResource(R.string.hangman_custom_word_label)) },
                    placeholder = { Text(stringResource(R.string.hangman_custom_word_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = onSurface,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = onSurface,
                        cursorColor = accentColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        selectionColors = TextSelectionColors(
                            handleColor = accentColor,
                            backgroundColor = accentColor.copy(alpha = 0.4f),
                        ),
                    ),
                )
                Text(
                    text = stringResource(R.string.hangman_custom_word_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor,
                )
                error?.let { errorText ->
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = onAccent,
                ),
                enabled = value.isNotBlank(),
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = stringResource(R.string.hangman_custom_word_confirm),
                    color = onAccent,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = accentColor),
            ) {
                Text(text = stringResource(CoreUiR.string.cancel))
            }
        },
    )
}

private fun hangmanFloatAnimationSpec(animationSpeed: AnimationSpeed): AnimationSpec<Float> =
    if (animationSpeed == AnimationSpeed.OFF) {
        snap()
    } else {
        tween(
            durationMillis = animationSpeed.durationMs,
            easing = FastOutSlowInEasing,
        )
    }

@Composable
private fun HangmanDrawing(
    mistakes: Float,
    modifier: Modifier = Modifier,
    color: Color,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val scaffoldStroke = w * 0.025f
        val bodyStroke = w * 0.015f
        val roundedCap = StrokeCap.Round

        drawLine(
            color = color,
            start = Offset(w * 0.08f, h * 0.92f),
            end = Offset(w * 0.92f, h * 0.92f),
            strokeWidth = scaffoldStroke,
            cap = roundedCap,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.92f),
            end = Offset(w * 0.18f, h * 0.08f),
            strokeWidth = scaffoldStroke,
            cap = roundedCap,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.08f),
            end = Offset(w * 0.7f, h * 0.08f),
            strokeWidth = scaffoldStroke,
            cap = roundedCap,
        )
        drawLine(
            color = color,
            start = Offset(w * 0.7f, h * 0.08f),
            end = Offset(w * 0.7f, h * 0.2f),
            strokeWidth = w * 0.012f,
            cap = roundedCap,
        )

        fun pieceProgress(step: Int): Float =
            (mistakes - (step - 1)).coerceIn(0f, 1f)

        val headCenter = Offset(w * 0.7f, h * 0.29f)
        val headRadius = w * 0.09f
        val headAlpha = pieceProgress(1)
        if (headAlpha > 0f) {
            drawCircle(
                color = color.copy(alpha = headAlpha),
                radius = headRadius,
                center = headCenter,
                style = Stroke(width = w * 0.015f),
            )
        }

        val bodyAlpha = pieceProgress(2)
        if (bodyAlpha > 0f) {
            drawLine(
                color = color.copy(alpha = bodyAlpha),
                start = Offset(w * 0.7f, h * 0.38f),
                end = Offset(w * 0.7f, h * 0.62f),
                strokeWidth = bodyStroke,
                cap = roundedCap,
            )
        }

        val leftArmAlpha = pieceProgress(3)
        if (leftArmAlpha > 0f) {
            drawLine(
                color = color.copy(alpha = leftArmAlpha),
                start = Offset(w * 0.7f, h * 0.43f),
                end = Offset(w * 0.59f, h * 0.52f),
                strokeWidth = bodyStroke,
                cap = roundedCap,
            )
        }

        val rightArmAlpha = pieceProgress(4)
        if (rightArmAlpha > 0f) {
            drawLine(
                color = color.copy(alpha = rightArmAlpha),
                start = Offset(w * 0.7f, h * 0.43f),
                end = Offset(w * 0.81f, h * 0.52f),
                strokeWidth = bodyStroke,
                cap = roundedCap,
            )
        }

        val leftLegAlpha = pieceProgress(5)
        if (leftLegAlpha > 0f) {
            drawLine(
                color = color.copy(alpha = leftLegAlpha),
                start = Offset(w * 0.7f, h * 0.62f),
                end = Offset(w * 0.6f, h * 0.8f),
                strokeWidth = bodyStroke,
                cap = roundedCap,
            )
        }

        val rightLegAlpha = pieceProgress(6)
        if (rightLegAlpha > 0f) {
            drawLine(
                color = color.copy(alpha = rightLegAlpha),
                start = Offset(w * 0.7f, h * 0.62f),
                end = Offset(w * 0.8f, h * 0.8f),
                strokeWidth = bodyStroke,
                cap = roundedCap,
            )
        }

        val faceAlpha = pieceProgress(7)
        if (faceAlpha > 0f) {
            val eyeOffsetX = headRadius * 0.45f
            val eyeOffsetY = headRadius * 0.35f
            val eyeSize = headRadius * 0.18f
            val faceColor = color.copy(alpha = faceAlpha)

            drawLine(
                color = faceColor,
                start = Offset(
                    headCenter.x - eyeOffsetX - eyeSize,
                    headCenter.y - eyeOffsetY - eyeSize,
                ),
                end = Offset(
                    headCenter.x - eyeOffsetX + eyeSize,
                    headCenter.y - eyeOffsetY + eyeSize,
                ),
                strokeWidth = w * 0.012f,
                cap = roundedCap,
            )
            drawLine(
                color = faceColor,
                start = Offset(
                    headCenter.x - eyeOffsetX - eyeSize,
                    headCenter.y - eyeOffsetY + eyeSize,
                ),
                end = Offset(
                    headCenter.x - eyeOffsetX + eyeSize,
                    headCenter.y - eyeOffsetY - eyeSize,
                ),
                strokeWidth = w * 0.012f,
                cap = roundedCap,
            )
            drawLine(
                color = faceColor,
                start = Offset(
                    headCenter.x + eyeOffsetX - eyeSize,
                    headCenter.y - eyeOffsetY - eyeSize,
                ),
                end = Offset(
                    headCenter.x + eyeOffsetX + eyeSize,
                    headCenter.y - eyeOffsetY + eyeSize,
                ),
                strokeWidth = w * 0.012f,
                cap = roundedCap,
            )
            drawLine(
                color = faceColor,
                start = Offset(
                    headCenter.x + eyeOffsetX - eyeSize,
                    headCenter.y - eyeOffsetY + eyeSize,
                ),
                end = Offset(
                    headCenter.x + eyeOffsetX + eyeSize,
                    headCenter.y - eyeOffsetY - eyeSize,
                ),
                strokeWidth = w * 0.012f,
                cap = roundedCap,
            )
            drawArc(
                color = faceColor,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(
                    headCenter.x - headRadius * 0.5f,
                    headCenter.y + headRadius * 0.4f,
                ),
                size = Size(headRadius, headRadius * 0.5f),
                style = Stroke(width = w * 0.012f),
            )
        }
    }
}
