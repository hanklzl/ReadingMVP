package com.littlemandarin.classics

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.littlemandarin.classics.shared.AppInfo
import com.littlemandarin.classics.shared.AppInfoResourceKeys
import com.littlemandarin.classics.shared.GetAppInfoUseCase
import com.littlemandarin.classics.shared.analytics.Analytics
import com.littlemandarin.classics.shared.analytics.AnalyticsEventName
import com.littlemandarin.classics.shared.analytics.AnalyticsProperties
import com.littlemandarin.classics.shared.analytics.AndroidAnalyticsServiceProvider
import com.littlemandarin.classics.shared.analytics.createPlatformAnalytics
import com.littlemandarin.classics.shared.feedback.AndroidFeedbackServiceProvider
import com.littlemandarin.classics.shared.feedback.FeedbackChildAgeBand as SharedFeedbackChildAgeBand
import com.littlemandarin.classics.shared.feedback.FeedbackIssueType as SharedFeedbackIssueType
import com.littlemandarin.classics.shared.feedback.FeedbackService
import com.littlemandarin.classics.shared.feedback.FeedbackSatisfaction as SharedFeedbackSatisfaction
import com.littlemandarin.classics.shared.feedback.createPlatformFeedbackService
import com.littlemandarin.classics.shared.presentation.AbilityDimension
import com.littlemandarin.classics.shared.presentation.AbilityMap
import com.littlemandarin.classics.shared.presentation.AbilityMapUseCases
import com.littlemandarin.classics.shared.presentation.AbilityProgress
import com.littlemandarin.classics.shared.presentation.AnalyticsEventPayload
import com.littlemandarin.classics.shared.presentation.InteractivePracticeItem
import com.littlemandarin.classics.shared.presentation.InteractivePracticeUseCases
import com.littlemandarin.classics.shared.presentation.AndroidEngagementServiceProvider
import com.littlemandarin.classics.shared.presentation.AdaptiveReadingPathRecommender
import com.littlemandarin.classics.shared.presentation.AndroidReaderSettingsServiceProvider
import com.littlemandarin.classics.shared.presentation.AndroidVocabReviewServiceProvider
import com.littlemandarin.classics.shared.presentation.AssessmentItem
import com.littlemandarin.classics.shared.presentation.StoryCoverTheme
import com.littlemandarin.classics.shared.presentation.StoryCoverUseCases
import com.littlemandarin.classics.shared.presentation.ReadingLevelAssessmentUseCases
import com.littlemandarin.classics.shared.presentation.BuildAiExplanationRequestUseCase
import com.littlemandarin.classics.shared.presentation.BuildFeedbackSubmissionUseCase
import com.littlemandarin.classics.shared.presentation.BuildParentReportSummaryUseCase
import com.littlemandarin.classics.shared.presentation.BuildSpeechTextUseCase
import com.littlemandarin.classics.shared.presentation.WordLookupResult
import com.littlemandarin.classics.shared.presentation.WordLookupUseCase
import com.littlemandarin.classics.shared.presentation.ChildAgeBand
import com.littlemandarin.classics.shared.presentation.FeedbackOption
import com.littlemandarin.classics.shared.presentation.FeedbackPresentationOptions
import com.littlemandarin.classics.shared.presentation.LibraryStoryAction
import com.littlemandarin.classics.shared.presentation.LibraryStoryCard
import com.littlemandarin.classics.shared.presentation.LibraryStoryStatus
import com.littlemandarin.classics.shared.presentation.OnboardingDefaults
import com.littlemandarin.classics.shared.presentation.OnboardingPreferences
import com.littlemandarin.classics.shared.presentation.ParentAdviceType
import com.littlemandarin.classics.shared.presentation.ParentReportSummary
import com.littlemandarin.classics.shared.presentation.ParentWeeklyPlan
import com.littlemandarin.classics.shared.presentation.ParentWeeklyPlanUseCases
import com.littlemandarin.classics.shared.presentation.WeeklyPlanWord
import com.littlemandarin.classics.shared.presentation.WeeklyPlanWordState
import com.littlemandarin.classics.shared.presentation.WeeklyShareCard
import com.littlemandarin.classics.shared.presentation.QuizSessionReducer
import com.littlemandarin.classics.shared.presentation.ReaderAnalyticsEvents
import com.littlemandarin.classics.shared.presentation.ReaderLanguage
import com.littlemandarin.classics.shared.presentation.ReaderSettings
import com.littlemandarin.classics.shared.presentation.ReaderSettingsService
import com.littlemandarin.classics.shared.presentation.KaraokeTimeline
import com.littlemandarin.classics.shared.presentation.ReadAlongKaraokeReducer
import com.littlemandarin.classics.shared.presentation.ReadingSessionReducer
import com.littlemandarin.classics.shared.presentation.ReadingAudioSource
import com.littlemandarin.classics.shared.presentation.ReadingPlaybackStatus
import com.littlemandarin.classics.shared.presentation.ReadingPlaybackSpeed
import com.littlemandarin.classics.shared.presentation.ReadingSessionMode
import com.littlemandarin.classics.shared.presentation.ReadingTextSize
import com.littlemandarin.classics.shared.presentation.SentenceSegment
import com.littlemandarin.classics.shared.presentation.PendingReview
import com.littlemandarin.classics.shared.presentation.ReviewPackUseCase
import com.littlemandarin.classics.shared.presentation.SentenceSegmenter
import com.littlemandarin.classics.shared.presentation.StreakSummary
import com.littlemandarin.classics.shared.presentation.StreakUseCase
import com.littlemandarin.classics.shared.sfx.SfxEventReducer
import com.littlemandarin.classics.shared.presentation.StoryPresentationUseCases
import com.littlemandarin.classics.shared.presentation.StoryProgressStatus
import com.littlemandarin.classics.shared.presentation.VocabReviewAssessment
import com.littlemandarin.classics.shared.presentation.VocabReviewUseCase
import com.littlemandarin.classics.shared.presentation.WordBookItem
import com.littlemandarin.classics.shared.presentation.WordBookSummary
import com.littlemandarin.classics.shared.presentation.AiInteractionLogService
import com.littlemandarin.classics.shared.presentation.AiInteractionOutcome
import com.littlemandarin.classics.shared.presentation.AiInteractionRecord
import com.littlemandarin.classics.shared.presentation.AiSafetyConsolePolicy
import com.littlemandarin.classics.shared.presentation.AiSafetyConsoleUseCases
import com.littlemandarin.classics.shared.presentation.RetellEncouragement
import com.littlemandarin.classics.shared.presentation.RetellGuideUseCases
import com.littlemandarin.classics.shared.presentation.createPlatformAiInteractionLogService
import com.littlemandarin.classics.shared.presentation.createPlatformOnboardingService
import com.littlemandarin.classics.shared.presentation.createPlatformReaderSettingsService
import com.littlemandarin.classics.shared.presentation.createPlatformReviewPackService
import com.littlemandarin.classics.shared.presentation.createPlatformStreakService
import com.littlemandarin.classics.shared.presentation.createPlatformVocabReviewService
import com.littlemandarin.classics.shared.presentation.isMockAiBackend
import com.littlemandarin.classics.shared.presentation.reviewPromptState
import com.littlemandarin.classics.shared.presentation.toLimitedDisplayText
import com.littlemandarin.classics.shared.progress.AndroidProgressServiceProvider
import com.littlemandarin.classics.shared.progress.BuildParentReportUseCase
import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.progress.GetProgressStatsUseCase
import com.littlemandarin.classics.shared.progress.MarkStoryCompletedUseCase
import com.littlemandarin.classics.shared.progress.ParentProgressReport
import com.littlemandarin.classics.shared.progress.ProgressService
import com.littlemandarin.classics.shared.progress.ProgressStats
import com.littlemandarin.classics.shared.progress.createPlatformProgressService
import com.littlemandarin.classics.shared.quiz.MistakeRemediationUseCases
import com.littlemandarin.classics.shared.quiz.QuestionResult
import com.littlemandarin.classics.shared.quiz.ReviewPack
import com.littlemandarin.classics.shared.quiz.ReviewParentTip
import com.littlemandarin.classics.shared.service.AiExplainBackendClient
import com.littlemandarin.classics.shared.service.AiExplanationRequest
import com.littlemandarin.classics.shared.service.AiExplanationResponse
import com.littlemandarin.classics.shared.service.AiServiceConfig
import com.littlemandarin.classics.shared.service.AiQuestionTypes
import com.littlemandarin.classics.shared.service.AiService
import com.littlemandarin.classics.shared.service.AndroidAudioServiceProvider
import com.littlemandarin.classics.shared.service.AudioService
import com.littlemandarin.classics.shared.service.AndroidTtsServiceProvider
import com.littlemandarin.classics.shared.service.TtsService
import com.littlemandarin.classics.shared.service.createAiService
import com.littlemandarin.classics.shared.service.createAudioService
import com.littlemandarin.classics.shared.service.createTtsService
import com.littlemandarin.classics.shared.story.DefaultStoryRepository
import com.littlemandarin.classics.shared.story.LoadStoryAudioManifestUseCase
import com.littlemandarin.classics.shared.story.Paragraph
import com.littlemandarin.classics.shared.story.PinyinCell
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.StoryAudioManifest
import com.littlemandarin.classics.shared.story.Vocab
import com.littlemandarin.classics.shared.usecase.GetStoryListUseCase
import com.littlemandarin.classics.shared.recording.AndroidRecordingStoreProvider
import com.littlemandarin.classics.shared.recording.RecordingAction
import com.littlemandarin.classics.shared.recording.RecordingState
import com.littlemandarin.classics.shared.recording.RecordingStateMachine
import com.littlemandarin.classics.shared.recording.RecordingRetentionPolicy
import com.littlemandarin.classics.shared.recording.VoiceRecording
import com.littlemandarin.classics.shared.recording.VoiceRecordingService
import com.littlemandarin.classics.shared.recording.RecordingStorageResult
import com.littlemandarin.classics.shared.recording.createPlatformVoiceRecordingService
import com.littlemandarin.classics.AndroidVoiceRecordingController
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_LittleMandarinClassics)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAnalyticsServiceProvider.initialize(applicationContext)
        AndroidFeedbackServiceProvider.initialize(applicationContext)
        AndroidProgressServiceProvider.initialize(applicationContext)
        AndroidReaderSettingsServiceProvider.initialize(applicationContext)
        AndroidEngagementServiceProvider.initialize(applicationContext)
        AndroidVocabReviewServiceProvider.initialize(applicationContext)
        AndroidAudioServiceProvider.initialize(applicationContext)
        AndroidTtsServiceProvider.initialize(applicationContext)
        AndroidRecordingStoreProvider.initialize(applicationContext)

        setContent {
            ReaderApp(
                activity = this@MainActivity,
                activityResultRegistryOwner = this@MainActivity,
            )
        }
    }
}

private object ReaderRoutes {
    const val Onboarding = "onboarding"
    const val Today = "today"
    const val Library = "library"
    const val WordBook = "word_book"
    const val Parent = "parent"
    const val Settings = "settings"
    const val Privacy = "privacy"
    const val AiConsole = "ai_console"
    const val PlacementCheck = "placement_check"
    const val Ability = "ability"

    const val Reading = "story/{storyId}/read"
    const val Vocabulary = "story/{storyId}/vocabulary?source={source}"
    const val Quiz = "story/{storyId}/quiz"
    const val WordReview = "word_book/review"
    const val ReviewPack = "review_pack"

    fun reading(storyId: String): String = "story/$storyId/read"

    fun vocabulary(
        storyId: String,
        source: VocabularyEntrySource = VocabularyEntrySource.Reading,
    ): String = "story/$storyId/vocabulary?source=${source.routeValue}"

    fun quiz(storyId: String): String = "story/$storyId/quiz"

    fun isFocusedFlow(route: String?): Boolean =
        route == Onboarding ||
            route == Privacy ||
            route == AiConsole ||
            route == PlacementCheck ||
            route == WordReview ||
            route == ReviewPack ||
            route == Ability ||
            route?.startsWith("story/") == true
}

private enum class VocabularyEntrySource(val routeValue: String) {
    Today("today"),
    Reading("reading"),
    Quiz("quiz"),
    ;

    companion object {
        fun fromRouteValue(value: String?): VocabularyEntrySource =
            entries.firstOrNull { it.routeValue == value } ?: Reading
    }
}

private data class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: LmcIcon,
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(ReaderRoutes.Today, R.string.nav_today, LmcIcon.Today),
    TopLevelDestination(ReaderRoutes.Library, R.string.nav_library, LmcIcon.Library),
    TopLevelDestination(ReaderRoutes.WordBook, R.string.nav_word_book, LmcIcon.Book),
    TopLevelDestination(ReaderRoutes.Parent, R.string.nav_parent, LmcIcon.Parent),
)

@Composable
private fun ReaderApp(
    activity: Activity,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
) {
    val baseContext = LocalContext.current
    val settingsService = remember(baseContext.applicationContext) { createPlatformReaderSettingsService() }
    val onboardingService = remember(baseContext.applicationContext) { createPlatformOnboardingService() }
    val systemLanguage = remember(baseContext) { baseContext.systemReaderLanguage() }
    val appVersion = remember(baseContext.applicationContext) {
        baseContext.applicationContext.appVersionName()
    }
    var settings by remember { mutableStateOf(ReaderSettings()) }
    var onboardingPreferences by remember { mutableStateOf(OnboardingPreferences()) }
    var appInitialized by remember { mutableStateOf(false) }
    val analyticsScope = rememberCoroutineScope()
    val analytics = remember(baseContext.applicationContext, appVersion, settings.language, analyticsScope) {
        ReaderAnalytics(
            delegate = createPlatformAnalytics(
                appVersion = appVersion,
                uiLocale = settings.language.tag,
            ),
            scope = analyticsScope,
        )
    }
    val feedbackService = remember(baseContext.applicationContext) {
        createPlatformFeedbackService()
    }
    var readingProgressVersion by remember { mutableIntStateOf(0) }
    val localizedEnvironment = remember(baseContext, settings.language) {
        baseContext.localizedEnvironment(settings.language)
    }

    LaunchedEffect(settingsService, onboardingService) {
        settings = settingsService.read()
        onboardingPreferences = onboardingService.read()
        appInitialized = true
    }

    CompositionLocalProvider(
        LocalActivity provides activity,
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
        LocalContext provides localizedEnvironment.context,
        LocalConfiguration provides localizedEnvironment.configuration,
    ) {
        LittleMandarinClassicsTheme {
            if (!appInitialized) {
                LoadingScreen()
            } else {
                ReaderAppContent(
                    settings = settings,
                    settingsService = settingsService,
                    onboardingPreferences = onboardingPreferences,
                    analytics = analytics,
                    feedbackService = feedbackService,
                    readingProgressVersion = readingProgressVersion,
                    onOnboardingComplete = { preferences ->
                        analyticsScope.launch {
                            settingsService.setLanguage(preferences.language)
                            onboardingService.complete(preferences)
                            settings = settingsService.read()
                            onboardingPreferences = onboardingService.read()
                        }
                    },
                    onOnboardingSkip = {
                        analyticsScope.launch {
                            onboardingService.skip(OnboardingDefaults.skippedPreferences(systemLanguage))
                            settingsService.setLanguage(systemLanguage)
                            settings = settingsService.read()
                            onboardingPreferences = onboardingService.read()
                        }
                    },
                    onAssessedLevelChange = { level ->
                        analyticsScope.launch {
                            onboardingService.complete(
                                onboardingService.read().copy(assessedReadingLevel = level),
                            )
                            onboardingPreferences = onboardingService.read()
                        }
                    },
                    onLanguageChange = { language ->
                        analyticsScope.launch {
                            settingsService.setLanguage(language)
                            settings = settingsService.read()
                        }
                    },
                    onPinyinDefaultChange = { showPinyin ->
                        analyticsScope.launch {
                            settingsService.setShowPinyinByDefault(showPinyin)
                            settings = settingsService.read()
                        }
                    },
                    onTextSizeChange = { textSize ->
                        analyticsScope.launch {
                            settingsService.setReadingTextSize(textSize)
                            settings = settingsService.read()
                        }
                    },
                    onAiBackendBaseUrlChange = { baseUrl ->
                        analyticsScope.launch {
                            settingsService.setAiBackendBaseUrl(baseUrl)
                            settings = settingsService.read()
                        }
                    },
                    onSfxEnabledChange = { enabled ->
                        analyticsScope.launch {
                            settingsService.setSfxEnabled(enabled)
                            settings = settingsService.read()
                        }
                    },
                    onSfxVolumeChange = { volume ->
                        analyticsScope.launch {
                            settingsService.setSfxVolume(volume)
                            settings = settingsService.read()
                        }
                    },
                    onReadingPositionChange = { storyId, paragraphIndex ->
                        analyticsScope.launch {
                            settingsService.setReadingParagraphIndex(storyId, paragraphIndex)
                            readingProgressVersion += 1
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ReaderAppContent(
    settings: ReaderSettings,
    settingsService: ReaderSettingsService,
    onboardingPreferences: OnboardingPreferences,
    analytics: ReaderAnalytics,
    feedbackService: FeedbackService,
    readingProgressVersion: Int,
    onOnboardingComplete: (OnboardingPreferences) -> Unit,
    onOnboardingSkip: () -> Unit,
    onAssessedLevelChange: (Int?) -> Unit,
    onLanguageChange: (ReaderLanguage) -> Unit,
    onPinyinDefaultChange: (Boolean) -> Unit,
    onTextSizeChange: (ReadingTextSize) -> Unit,
    onAiBackendBaseUrlChange: (String) -> Unit,
    onSfxEnabledChange: (Boolean) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onReadingPositionChange: (String, Int) -> Unit,
) {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val sfxEventReducer = remember { SfxEventReducer() }
    val sfxPlayer = remember(appContext) { AndroidSfxPlayer(appContext) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val appInfo = remember { GetAppInfoUseCase().invoke() }
    val repository = remember { DefaultStoryRepository() }
    val progressService = remember { createPlatformProgressService() }
    val streakService = remember { createPlatformStreakService() }
    val vocabReviewService = remember { createPlatformVocabReviewService() }
    val reviewPackService = remember { createPlatformReviewPackService() }
    val aiInteractionLogService = remember { createPlatformAiInteractionLogService() }
    val voiceRecordingService = remember {
        createPlatformVoiceRecordingService(
            RecordingRetentionPolicy(maxPerStory = 12, maxOverall = 60),
        )
    }
    val audioService = remember { createAudioService() }
    val ttsService = remember { createTtsService() }
    val storyPresentationUseCases = remember { StoryPresentationUseCases() }
    val streakUseCase = remember(streakService) { StreakUseCase(streakService) }
    val reviewPackUseCase = remember(reviewPackService) { ReviewPackUseCase(reviewPackService) }
    val vocabReviewUseCase = remember(repository, progressService, vocabReviewService) {
        VocabReviewUseCase(
            storyRepository = repository,
            progressService = progressService,
            reviewService = vocabReviewService,
        )
    }
    val aiService = remember(settings.aiBackendBaseUrl) {
        createAiService(
            config = AiServiceConfig(
                baseUrl = settings.aiBackendBaseUrl.takeUnless { it.isMockAiBackend() },
            ),
            backendClient = AndroidAiExplainBackendClient(),
        )
    }
    val progressRecords by progressService.records.collectAsState(initial = emptyList())
    val vocabRecords by vocabReviewService.records.collectAsState(initial = emptyList())
    val voiceRecordings by voiceRecordingService.recordings.collectAsState(initial = emptyList())
    var storiesState by remember {
        mutableStateOf<StoriesState>(StoriesState.Loading)
    }
    var progressStats by remember {
        mutableStateOf(
            ProgressStats(
                completedCount = 0,
                vocabLearnedCount = 0,
                correctCount = 0,
                questionCount = 0,
                averageCorrectPercent = 0.0,
            ),
        )
    }
    var parentReport by remember {
        mutableStateOf(
            ParentProgressReport(
                storiesCompletedThisWeek = 0,
                vocabLearnedThisWeek = 0,
                averageCorrectPercent = 0.0,
                recentCompletions = emptyList(),
            ),
        )
    }
    var parentReportSummary by remember {
        mutableStateOf(
            ParentReportSummary(
                storiesCompletedThisWeek = 0,
                readingDaysThisWeek = 0,
                vocabLearnedThisWeek = 0,
                averageCorrectPercent = 0.0,
                correctCount = 0,
                questionCount = 0,
            ),
        )
    }
    var readingPositions by remember {
        mutableStateOf<Map<String, Int>>(emptyMap())
    }
    var wordReviewVersion by remember { mutableIntStateOf(0) }
    var reviewPackVersion by remember { mutableIntStateOf(0) }
    var pendingReview by remember { mutableStateOf<PendingReview?>(null) }
    var activeReviewPack by remember { mutableStateOf<ReviewPack?>(null) }
    var wordBookSummary by remember {
        mutableStateOf(WordBookSummary(totalWords = 0, dueCount = 0, items = emptyList()))
    }
    var streakSummary by remember {
        mutableStateOf(
            StreakSummary(
                currentStreakDays = 0,
                longestStreakDays = 0,
                todayCompletedStories = 0,
                dailyGoalStories = onboardingPreferences.dailyGoalStories,
                todayProgressFraction = 0.0,
                todayGoalMet = false,
            ),
        )
    }
    DisposableEffect(sfxPlayer) {
        onDispose {
            sfxPlayer.release()
        }
    }
    DisposableEffect(lifecycleOwner, sfxPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                sfxPlayer.stopAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(settings.sfxSettings.enabled) {
        if (!settings.sfxSettings.enabled) {
            sfxPlayer.stopAll()
        }
    }

    LaunchedEffect(repository) {
        storiesState = runCatching {
            StoriesState.Ready(GetStoryListUseCase(repository).invoke())
        }.getOrElse {
            StoriesState.Error
        }
    }

    LaunchedEffect(Unit) {
        analytics.track(ReaderAnalyticsEvents.appOpen(openType = "cold_start"))
    }

    LaunchedEffect(progressService, progressRecords, storiesState, wordBookSummary.dueCount) {
        val stories = (storiesState as? StoriesState.Ready)?.stories.orEmpty()
        val completedStoryIds = storyPresentationUseCases.completedStoryIds(progressRecords)
        val nextStoryId = stories.firstOrNull { it.id !in completedStoryIds }?.id
        progressStats = GetProgressStatsUseCase(progressService).invoke()
        parentReport = BuildParentReportUseCase(progressService).invoke(
            nowEpochMillis = System.currentTimeMillis(),
        )
        parentReportSummary = BuildParentReportSummaryUseCase().invoke(
            report = parentReport,
            stats = progressStats,
            nowEpochMillis = System.currentTimeMillis(),
            dueWordCount = wordBookSummary.dueCount,
            nextStoryId = nextStoryId,
        )
        streakSummary = streakUseCase.summary(todayEpochMillis = System.currentTimeMillis())
    }

    LaunchedEffect(storiesState, readingProgressVersion) {
        val stories = (storiesState as? StoriesState.Ready)?.stories ?: return@LaunchedEffect
        readingPositions = stories.associate { story ->
            story.id to settingsService.readReadingParagraphIndex(story.id)
        }
    }

    LaunchedEffect(storiesState, progressRecords, wordReviewVersion) {
        if (storiesState !is StoriesState.Ready) return@LaunchedEffect
        val now = System.currentTimeMillis()
        vocabReviewUseCase.syncLearnedWords(todayEpochMillis = now)
        wordBookSummary = vocabReviewUseCase.wordBook(todayEpochMillis = now)
        streakSummary = streakUseCase.summary(todayEpochMillis = now)
    }

    LaunchedEffect(reviewPackVersion) {
        pendingReview = reviewPackUseCase.pendingReview(todayEpochMillis = System.currentTimeMillis())
    }

    val readingPathRecommender = remember { AdaptiveReadingPathRecommender() }
    val readingPathRecommendation = remember(
        storiesState,
        progressRecords,
        readingPositions,
        wordBookSummary.dueCount,
        onboardingPreferences.recommendedLevel,
    ) {
        val stories = (storiesState as? StoriesState.Ready)?.stories.orEmpty()
        readingPathRecommender.recommend(
            stories = stories,
            readingLevel = onboardingPreferences.recommendedLevel,
            completionRecords = progressRecords,
            readingPositions = readingPositions,
            dueVocabWordCount = wordBookSummary.dueCount,
        )
    }

    val abilityMapUseCases = remember { AbilityMapUseCases() }
    val abilityMap = remember(storiesState, progressRecords) {
        val stories = (storiesState as? StoriesState.Ready)?.stories.orEmpty()
        val recentSessionStoryId = progressRecords
            .maxByOrNull { it.completedAtEpochMillis }
            ?.storyId
        abilityMapUseCases.buildAbilityMap(
            stories = stories,
            completionRecords = progressRecords,
            recentSessionStoryId = recentSessionStoryId,
        )
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomBar = !ReaderRoutes.isFocusedFlow(currentRoute)
    val snackbarHostState = remember { SnackbarHostState() }
    val appScope = rememberCoroutineScope()
    val startDestination = if (onboardingPreferences.completed) {
        ReaderRoutes.Today
    } else {
        ReaderRoutes.Onboarding
    }
    var parentGatePassed by remember { mutableStateOf(false) }
    val previewSfxAtVolume: (Float) -> Unit = { volume ->
        appScope.launch {
            runCatching { sfxPlayer.playPreview(volume) }
        }
    }
    val playQuizAnswerSfx: (Boolean) -> Unit = { isCorrect ->
        appScope.launch {
            val cue = sfxEventReducer.quizAnswerSubmitted(
                isCorrect = isCorrect,
                settings = settings.sfxSettings,
            )
            if (cue != null) {
                runCatching { sfxPlayer.play(cue) }
            }
        }
    }
    val playCompletionSfx: (Int?) -> Unit = { newMilestoneDays ->
        appScope.launch {
            val cue = sfxEventReducer.storyCompleted(
                newMilestoneDays = newMilestoneDays,
                settings = settings.sfxSettings,
            )
            if (cue != null) {
                runCatching { sfxPlayer.play(cue) }
            }
        }
    }
    val previewSfx: () -> Unit = {
        val settingsForPlayback = settings.sfxSettings
        if (settingsForPlayback.enabled) {
            previewSfxAtVolume(settingsForPlayback.volume)
        }
    }
    val handleSfxEnabledChange: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            sfxPlayer.stopAll()
        }
        onSfxEnabledChange(enabled)
        if (enabled) {
            previewSfxAtVolume(settings.sfxSettings.volume)
        }
    }
    val handleSfxVolumePreview: (Float) -> Unit = { volume ->
        if (settings.sfxSettings.enabled) {
            previewSfxAtVolume(volume)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                ReaderBottomNavigation(
                    currentRoute = currentRoute,
                    onDestinationClick = { destination ->
                        if (destination.route == ReaderRoutes.Parent) {
                            analytics.track(ReaderAnalyticsEvents.parentReportOpen("bottom_navigation"))
                        }
                        navController.navigateTopLevel(destination.route)
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(ReaderRoutes.Onboarding) {
                OnboardingScreen(
                    initialLanguage = settings.language,
                    stories = (storiesState as? StoriesState.Ready)?.stories.orEmpty(),
                    onComplete = { preferences ->
                        appScope.launch {
                            streakUseCase.setDailyGoal(preferences.dailyGoalStories)
                            streakSummary = streakUseCase.summary(System.currentTimeMillis())
                        }
                        onOnboardingComplete(preferences)
                        navController.navigate(ReaderRoutes.Today) {
                            popUpTo(ReaderRoutes.Onboarding) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onSkip = {
                        appScope.launch {
                            streakUseCase.setDailyGoal(OnboardingDefaults.DailyGoalStories)
                            streakSummary = streakUseCase.summary(System.currentTimeMillis())
                            onOnboardingSkip()
                            navController.navigate(ReaderRoutes.Today) {
                                popUpTo(ReaderRoutes.Onboarding) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable(ReaderRoutes.Today) {
                StoryStateContent(storiesState = storiesState) { stories ->
                    TodayScreen(
                        appInfo = appInfo,
                        stories = stories,
                        progressStats = progressStats,
                        streakSummary = streakSummary,
                        progressRecords = progressRecords,
                        readingPositions = readingPositions,
                        recommendedStoryId = readingPathRecommendation.nextStory?.id,
                        reviewWordCount = readingPathRecommendation.reviewWordCount,
                        pendingReview = pendingReview,
                        abilityMap = abilityMap,
                        storyPresentationUseCases = storyPresentationUseCases,
                        snackbarHostState = snackbarHostState,
                        onRead = { storyId ->
                            analytics.trackStoryOpen(stories, storyId, openSource = "today")
                            navController.navigate(ReaderRoutes.reading(storyId))
                        },
                        onVocabulary = { storyId ->
                            navController.navigate(ReaderRoutes.vocabulary(storyId, VocabularyEntrySource.Today))
                        },
                        onQuiz = { storyId -> navController.navigate(ReaderRoutes.quiz(storyId)) },
                        onReview = {
                            activeReviewPack = pendingReview?.pack
                            navController.navigate(ReaderRoutes.ReviewPack)
                        },
                        onParent = {
                            analytics.track(ReaderAnalyticsEvents.parentReportOpen("today_header"))
                            navController.navigateTopLevel(ReaderRoutes.Parent)
                        },
                        onAbilityMap = { navController.navigate(ReaderRoutes.Ability) },
                        onSettings = { navController.navigateTopLevel(ReaderRoutes.Settings) },
                    )
                }
            }
            composable(ReaderRoutes.Ability) {
                AbilityMapScreen(
                    abilityMap = abilityMap,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(ReaderRoutes.Library) {
                LibraryStateContent(
                    storiesState = storiesState,
                    onRetry = {
                        appScope.launch {
                            storiesState = StoriesState.Loading
                            storiesState = runCatching {
                                StoriesState.Ready(GetStoryListUseCase(repository).invoke())
                            }.getOrElse {
                                StoriesState.Error
                            }
                        }
                    },
                ) { stories ->
                    LibraryScreen(
                        stories = stories,
                        progressRecords = progressRecords,
                        readingPositions = readingPositions,
                        storyPresentationUseCases = storyPresentationUseCases,
                        onRead = { storyId ->
                            analytics.trackStoryOpen(stories, storyId, openSource = "library")
                            navController.navigate(ReaderRoutes.reading(storyId))
                        },
                        onSettings = { navController.navigateTopLevel(ReaderRoutes.Settings) },
                    )
                }
            }
            composable(ReaderRoutes.WordBook) {
                StoryStateContent(storiesState = storiesState) { stories ->
                    WordBookScreen(
                        wordBookSummary = wordBookSummary,
                        stories = stories,
                        analytics = analytics,
                        ttsService = ttsService,
                        onStartReview = { navController.navigate(ReaderRoutes.WordReview) },
                        onReadToday = { navController.navigateTopLevel(ReaderRoutes.Today) },
                        onSettings = { navController.navigateTopLevel(ReaderRoutes.Settings) },
                    )
                }
            }
            composable(ReaderRoutes.Parent) {
                StoryStateContent(storiesState = storiesState) { stories ->
                    ParentReportScreen(
                        stories = stories,
                        progressStats = progressStats,
                        parentReport = parentReport,
                        parentReportSummary = parentReportSummary,
                        abilityMap = abilityMap,
                        progressRecords = progressRecords,
                        vocabRecords = vocabRecords,
                        streakSummary = streakSummary,
                        readingPositions = readingPositions,
                        storyPresentationUseCases = storyPresentationUseCases,
                        pendingReview = pendingReview,
                        voiceRecordings = voiceRecordings,
                        voiceRecordingService = voiceRecordingService,
                        parentGatePassed = parentGatePassed,
                        onParentGatePassed = { parentGatePassed = true },
                        onStoryClick = { storyId ->
                            analytics.trackStoryOpen(stories, storyId, openSource = "parent_report")
                            navController.navigate(ReaderRoutes.reading(storyId))
                        },
                        onReviewWords = { navController.navigateTopLevel(ReaderRoutes.WordBook) },
                        onOpenReview = {
                            activeReviewPack = pendingReview?.pack
                            navController.navigate(ReaderRoutes.ReviewPack)
                        },
                        onSettings = { navController.navigateTopLevel(ReaderRoutes.Settings) },
                        onPrivacy = { navController.navigate(ReaderRoutes.Privacy) },
                    )
                }
            }
            composable(ReaderRoutes.Settings) {
                SettingsScreen(
                    appInfo = appInfo,
                    settings = settings,
                    feedbackService = feedbackService,
                    onLanguageChange = onLanguageChange,
                    onPinyinDefaultChange = onPinyinDefaultChange,
                    onTextSizeChange = onTextSizeChange,
                    onAiBackendBaseUrlChange = onAiBackendBaseUrlChange,
                    onSfxEnabledChange = handleSfxEnabledChange,
                    onSfxVolumeChange = onSfxVolumeChange,
                    onSfxVolumePreview = handleSfxVolumePreview,
                    onSfxPreview = previewSfx,
                    parentGatePassed = parentGatePassed,
                    onParentGatePassed = { parentGatePassed = true },
                    onParentReport = {
                        analytics.track(ReaderAnalyticsEvents.parentReportOpen("settings"))
                        navController.navigateTopLevel(ReaderRoutes.Parent)
                    },
                    onAiConsole = { navController.navigate(ReaderRoutes.AiConsole) },
                    onRecheckLevel = { navController.navigate(ReaderRoutes.PlacementCheck) },
                    onPrivacy = { navController.navigate(ReaderRoutes.Privacy) },
                )
            }
            composable(ReaderRoutes.PlacementCheck) {
                PlacementCheckFlow(
                    stories = (storiesState as? StoriesState.Ready)?.stories.orEmpty(),
                    onComplete = { level ->
                        onAssessedLevelChange(level)
                        navController.popBackStack()
                    },
                    onSkip = { navController.popBackStack() },
                )
            }
            composable(
                route = ReaderRoutes.Reading,
                arguments = listOf(navArgument("storyId") { type = NavType.StringType }),
            ) { backStackEntry ->
                StoryRouteContent(
                    storiesState = storiesState,
                    storyId = backStackEntry.arguments?.getString("storyId"),
                ) { story, _ ->
            ReadingScreen(
                story = story,
                settings = settings,
                analytics = analytics,
                aiService = aiService,
                aiInteractionLogService = aiInteractionLogService,
                voiceRecordings = voiceRecordings.filter { it.storyId == story.id },
                voiceRecordingService = voiceRecordingService,
                audioService = audioService,
                ttsService = ttsService,
                stopSfx = sfxPlayer::stopAll,
                initialParagraphIndex = readingPositions[story.id] ?: -1,
                onClose = { navController.popBackStack() },
                onTextSizeChange = onTextSizeChange,
                onPinyinDefaultChange = onPinyinDefaultChange,
                        onReadingPositionChange = onReadingPositionChange,
                        onVocabulary = {
                            navController.navigate(ReaderRoutes.vocabulary(story.id, VocabularyEntrySource.Reading))
                        },
                    )
                }
            }
            composable(
                route = ReaderRoutes.Vocabulary,
                arguments = listOf(
                    navArgument("storyId") { type = NavType.StringType },
                    navArgument("source") {
                        type = NavType.StringType
                        defaultValue = VocabularyEntrySource.Reading.routeValue
                    },
                ),
            ) { backStackEntry ->
                StoryRouteContent(
                    storiesState = storiesState,
                    storyId = backStackEntry.arguments?.getString("storyId"),
                ) { story, _ ->
                    val source = VocabularyEntrySource.fromRouteValue(backStackEntry.arguments?.getString("source"))
                    VocabularyScreen(
                        story = story,
                        analytics = analytics,
                        ttsService = ttsService,
                        entrySource = source,
                        onBack = {
                            when (source) {
                                VocabularyEntrySource.Today -> navController.navigateTopLevel(ReaderRoutes.Today)
                                VocabularyEntrySource.Reading,
                                VocabularyEntrySource.Quiz,
                                -> if (!navController.popBackStack()) {
                                    navController.navigate(ReaderRoutes.reading(story.id))
                                }
                            }
                        },
                        onQuiz = { navController.navigate(ReaderRoutes.quiz(story.id)) },
                    )
                }
            }
            composable(ReaderRoutes.Privacy) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
            composable(ReaderRoutes.AiConsole) {
                AiSafetyConsoleScreen(
                    logService = aiInteractionLogService,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = ReaderRoutes.Quiz,
                arguments = listOf(navArgument("storyId") { type = NavType.StringType }),
            ) { backStackEntry ->
                StoryRouteContent(
                    storiesState = storiesState,
                    storyId = backStackEntry.arguments?.getString("storyId"),
                ) { story, storyOrder ->
                    QuizScreen(
                        story = story,
                        storyOrder = storyOrder,
                        analytics = analytics,
                        progressService = progressService,
                        streakUseCase = streakUseCase,
                        vocabReviewUseCase = vocabReviewUseCase,
                        reviewPackUseCase = reviewPackUseCase,
                        voiceRecordingService = voiceRecordingService,
                        dueWords = wordBookSummary.items
                            .filter { it.due }
                            .map { it.word }
                            .toSet(),
                        onAnswerSubmitted = playQuizAnswerSfx,
                        onCompletionSfx = playCompletionSfx,
                        onReviewPack = { pack ->
                            activeReviewPack = pack
                            reviewPackVersion += 1
                            navController.navigate(ReaderRoutes.ReviewPack)
                        },
                        onBack = { navController.popBackStack() },
                        onCompletionRecorded = {
                            wordReviewVersion += 1
                            reviewPackVersion += 1
                        },
                        onReadAgain = {
                            appScope.launch {
                                settingsService.setReadingParagraphIndex(story.id, 0)
                                readingPositions = readingPositions + (story.id to 0)
                                analytics.trackStoryOpen(
                                    story = story,
                                    storyOrder = storyOrder,
                                    openSource = "quiz_completion",
                                )
                                navController.navigate(ReaderRoutes.reading(story.id)) {
                                    popUpTo(ReaderRoutes.Today)
                                }
                            }
                        },
                        onDone = {
                            navController.navigate(ReaderRoutes.Today) {
                                popUpTo(ReaderRoutes.Today) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
            composable(ReaderRoutes.WordReview) {
                WordReviewScreen(
                    initialItems = wordBookSummary.items.filter { it.due },
                    analytics = analytics,
                    ttsService = ttsService,
                    vocabReviewUseCase = vocabReviewUseCase,
                    onReviewChanged = {
                        wordReviewVersion += 1
                    },
                    onClose = {
                        navController.navigateTopLevel(ReaderRoutes.WordBook)
                    },
                )
            }
            composable(ReaderRoutes.ReviewPack) {
                ReviewPackScreen(
                    pack = activeReviewPack ?: pendingReview?.pack,
                    onDone = {
                        appScope.launch {
                            reviewPackUseCase.markCompleted(System.currentTimeMillis())
                            reviewPackVersion += 1
                        }
                        activeReviewPack = null
                        navController.navigate(ReaderRoutes.Today) {
                            popUpTo(ReaderRoutes.Today) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigateTopLevel(ReaderRoutes.Today)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun StoryStateContent(
    storiesState: StoriesState,
    content: @Composable (List<Story>) -> Unit,
) {
    when (storiesState) {
        StoriesState.Error -> CenterStateMessage(text = stringResource(R.string.error_loading_stories))
        StoriesState.Loading -> LoadingScreen()
        is StoriesState.Ready -> {
            if (storiesState.stories.isEmpty()) {
                CenterStateMessage(text = stringResource(R.string.today_no_stories))
            } else {
                content(storiesState.stories)
            }
        }
    }
}

@Composable
private fun LibraryStateContent(
    storiesState: StoriesState,
    onRetry: () -> Unit,
    content: @Composable (List<Story>) -> Unit,
) {
    when (storiesState) {
        StoriesState.Error -> LibraryStateSurface(
            title = stringResource(R.string.library_error_title),
            body = stringResource(R.string.library_error_body),
            actionLabel = stringResource(R.string.common_retry),
            onAction = onRetry,
        )
        StoriesState.Loading -> LibraryStateSurface(
            title = stringResource(R.string.library_loading_title),
            body = stringResource(R.string.library_loading_body),
            actionLabel = stringResource(R.string.common_retry),
            onAction = onRetry,
            loading = true,
        )
        is StoriesState.Ready -> {
            if (storiesState.stories.isEmpty()) {
                LibraryStateSurface(
                    title = stringResource(R.string.library_empty_title),
                    body = stringResource(R.string.library_empty_body),
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = onRetry,
                )
            } else {
                content(storiesState.stories)
            }
        }
    }
}

@Composable
private fun StoryRouteContent(
    storiesState: StoriesState,
    storyId: String?,
    content: @Composable (Story, Int) -> Unit,
) {
    when (storiesState) {
        StoriesState.Error -> CenterStateMessage(text = stringResource(R.string.error_loading_stories))
        StoriesState.Loading -> LoadingScreen()
        is StoriesState.Ready -> {
            val storyIndex = storiesState.stories.indexOfFirst { it.id == storyId }
            val story = storiesState.stories.getOrNull(storyIndex)
            if (story == null) {
                CenterStateMessage(text = stringResource(R.string.error_loading_stories))
            } else {
                content(story, storyIndex + 1)
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    initialLanguage: ReaderLanguage,
    stories: List<Story>,
    onComplete: (OnboardingPreferences) -> Unit,
    onSkip: () -> Unit,
) {
    var selectedAgeBand by remember { mutableStateOf(ChildAgeBand.Age5To6) }
    var selectedLanguage by remember(initialLanguage) { mutableStateOf(initialLanguage) }
    var dailyGoalStories by remember { mutableIntStateOf(1) }
    var showPlacementCheck by remember { mutableStateOf(false) }

    fun basePreferences(assessedLevel: Int?) = OnboardingPreferences(
        completed = true,
        skipped = false,
        childAgeBand = selectedAgeBand,
        language = selectedLanguage,
        dailyGoalStories = dailyGoalStories,
        assessedReadingLevel = assessedLevel,
    )

    if (showPlacementCheck) {
        PlacementCheckFlow(
            stories = stories,
            onComplete = { level -> onComplete(basePreferences(level)) },
            onSkip = { onComplete(basePreferences(null)) },
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = LmcSpacing.ScreenPadding,
            vertical = LmcSpacing.Space6,
        ),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
    ) {
        item {
            TopLevelHeader(
                title = stringResource(R.string.onboarding_title),
                subtitle = stringResource(R.string.onboarding_subtitle),
            )
        }
        item {
            SettingsSection(title = stringResource(R.string.onboarding_age_title)) {
                SelectableSettingsRow(
                    label = stringResource(R.string.onboarding_age_5_6),
                    selected = selectedAgeBand == ChildAgeBand.Age5To6,
                    onClick = { selectedAgeBand = ChildAgeBand.Age5To6 },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SelectableSettingsRow(
                    label = stringResource(R.string.onboarding_age_7_8),
                    selected = selectedAgeBand == ChildAgeBand.Age7To8,
                    onClick = { selectedAgeBand = ChildAgeBand.Age7To8 },
                )
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.onboarding_language_title)) {
                ReaderLanguage.entries.forEachIndexed { index, language ->
                    SelectableSettingsRow(
                        label = stringResource(language.labelRes()),
                        selected = selectedLanguage == language,
                        onClick = { selectedLanguage = language },
                    )
                    if (index < ReaderLanguage.entries.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.onboarding_goal_title)) {
                SelectableSettingsRow(
                    label = stringResource(R.string.onboarding_goal_one_story),
                    selected = dailyGoalStories == 1,
                    onClick = { dailyGoalStories = 1 },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SelectableSettingsRow(
                    label = stringResource(R.string.onboarding_goal_two_stories),
                    selected = dailyGoalStories == 2,
                    onClick = { dailyGoalStories = 2 },
                )
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Column(
                    modifier = Modifier.padding(LmcSpacing.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_day_one_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.onboarding_day_one_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.onboarding_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            SettingsSection(title = stringResource(R.string.placement_section_title)) {
                Column(
                    modifier = Modifier.padding(LmcSpacing.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                ) {
                    Text(
                        text = stringResource(R.string.placement_intro_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { showPlacementCheck = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                        enabled = stories.isNotEmpty(),
                    ) {
                        Text(text = stringResource(R.string.placement_start))
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                ) {
                    Text(text = stringResource(R.string.action_skip))
                }
                Button(
                    onClick = { onComplete(basePreferences(null)) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(text = stringResource(R.string.onboarding_get_started))
                }
            }
        }
    }
}

/**
 * Optional, child-friendly placement check. Shows one vocabulary word at a time with big
 * tappable meaning cards. Non-punitive: no wrong/failure framing, encouraging tone. On the
 * last item it scores via [ReadingLevelAssessmentUseCases.scoreAssessment] and reports the
 * resulting reading level (1..3). A fixed, deterministic seed is used (MVP). The result is a
 * local reading-level preference only — never child PII (AGENTS.md §7).
 */
@Composable
private fun PlacementCheckFlow(
    stories: List<Story>,
    onComplete: (Int) -> Unit,
    onSkip: () -> Unit,
) {
    val assessmentUseCases = remember { ReadingLevelAssessmentUseCases() }
    val items = remember(stories) {
        // Deterministic seed derived from the catalog size keeps selection stable for MVP.
        assessmentUseCases.selectAssessmentItems(stories = stories, seed = stories.size, itemCount = 5)
    }
    var currentIndex by remember(items) { mutableIntStateOf(0) }
    val answers = remember(items) { mutableStateMapOf<String, String>() }
    var assessedLevel by remember(items) { mutableStateOf<Int?>(null) }

    if (items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(LmcSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.placement_no_items),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Button(
                onClick = onSkip,
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.placement_result_continue))
            }
        }
        return
    }

    val resolvedLevel = assessedLevel
    if (resolvedLevel != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(LmcSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.placement_result_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.placement_result_level, resolvedLevel),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.placement_result_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onComplete(resolvedLevel) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.placement_result_continue))
            }
        }
        return
    }

    val item = items[currentIndex]
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = LmcSpacing.ScreenPadding,
            vertical = LmcSpacing.Space6,
        ),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
    ) {
        item {
            TopLevelHeader(
                title = stringResource(R.string.placement_intro_title),
                subtitle = stringResource(R.string.placement_progress, currentIndex + 1, items.size),
            )
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LmcSpacing.CardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                ) {
                    Text(
                        text = item.pinyin,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = item.word,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.placement_question),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        items(item.options, key = { it }) { option ->
            PlacementOptionCard(
                label = option,
                onClick = {
                    answers[item.id] = option
                    if (currentIndex < items.lastIndex) {
                        currentIndex += 1
                    } else {
                        assessedLevel = assessmentUseCases.scoreAssessment(items, answers.toMap()).level
                    }
                },
            )
        }
        item {
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LmcSpacing.ButtonSecondaryHeight),
            ) {
                Text(text = stringResource(R.string.placement_skip))
            }
        }
    }
}

@Composable
private fun PlacementOptionCard(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.ButtonPrimaryHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LmcSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TodayReviewCard(
    isFromPreviousDay: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = stringResource(R.string.today_review_pending_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(
                    if (isFromPreviousDay) {
                        R.string.today_review_pending_body
                    } else {
                        R.string.today_review_fresh_body
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun TodayScreen(
    appInfo: AppInfo,
    stories: List<Story>,
    progressStats: ProgressStats,
    streakSummary: StreakSummary,
    progressRecords: List<CompletionRecord>,
    readingPositions: Map<String, Int>,
    recommendedStoryId: String?,
    reviewWordCount: Int,
    pendingReview: PendingReview?,
    abilityMap: AbilityMap,
    storyPresentationUseCases: StoryPresentationUseCases,
    snackbarHostState: SnackbarHostState,
    onRead: (String) -> Unit,
    onVocabulary: (String) -> Unit,
    onQuiz: (String) -> Unit,
    onReview: () -> Unit,
    onParent: () -> Unit,
    onAbilityMap: () -> Unit,
    onSettings: () -> Unit,
) {
    val completedStoryIds = remember(progressRecords) {
        storyPresentationUseCases.completedStoryIds(progressRecords)
    }
    val todayStories = remember(stories, completedStoryIds, recommendedStoryId) {
        storyPresentationUseCases.selectTodayStories(
            stories = stories,
            completedStoryIds = completedStoryIds,
            recommendedStoryId = recommendedStoryId,
        )
    }
    val todayStory = todayStories.todayStory ?: return
    val upNextStory = todayStories.upNextStory
    val todayProgress = storyPresentationUseCases.storyProgress(
        story = todayStory,
        completedStoryIds = completedStoryIds,
        savedParagraphIndex = readingPositions[todayStory.id] ?: -1,
    )
    val todayCompleted = todayProgress.status == StoryProgressStatus.Completed
    val canOpenTodayQuiz = storyPresentationUseCases.canOpenQuiz(
        story = todayStory,
        completedStoryIds = completedStoryIds,
    )
    val scope = rememberCoroutineScope()
    val readingFirstMessage = stringResource(R.string.today_reading_first)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = LmcSpacing.ScreenPadding,
            vertical = LmcSpacing.Space4,
        ),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
    ) {
        item {
            TopLevelHeader(
                title = stringResource(appInfo.nameResourceKey.toStringResourceId()),
                subtitle = stringResource(R.string.today_title),
                actions = {
                    HeaderIconButton(
                        icon = LmcIcon.Parent,
                        contentDescription = stringResource(R.string.nav_parent),
                        onClick = onParent,
                    )
                    HeaderIconButton(
                        icon = LmcIcon.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        onClick = onSettings,
                    )
                },
            )
        }
        item {
            StoryHeroCard(
                story = todayStory,
                completed = todayCompleted,
                progress = todayProgress.fraction.toFloat(),
                onClick = { onRead(todayStory.id) },
            )
        }
        item {
            DailyGoalStreakCard(streakSummary = streakSummary)
        }
        if (pendingReview != null) {
            item {
                TodayReviewCard(
                    isFromPreviousDay = pendingReview.isFromPreviousDay,
                    onClick = onReview,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
            ) {
                SummaryTile(
                    label = stringResource(R.string.today_words_count, todayStory.vocab.size),
                    value = todayStory.vocab.size.toLocalizedInt(),
                    modifier = Modifier.weight(1f),
                    onClick = { onVocabulary(todayStory.id) },
                )
                SummaryTile(
                    label = stringResource(R.string.today_quiz_count, todayStory.questions.size),
                    value = todayStory.questions.size.toLocalizedInt(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (canOpenTodayQuiz) {
                            onQuiz(todayStory.id)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(readingFirstMessage)
                            }
                        }
                    },
                )
            }
        }
        item {
            ProgressSummaryBanner(
                text = stringResource(
                    R.string.today_completed_count,
                    progressStats.completedCount,
                ),
            )
        }
        if (reviewWordCount > 0) {
            item {
                ProgressSummaryBanner(
                    text = stringResource(R.string.today_review_words, reviewWordCount),
                )
            }
        }
        item {
            AbilityMapTodayCard(
                abilityMap = abilityMap,
                onClick = onAbilityMap,
            )
        }
        if (upNextStory != null) {
            item {
                SectionTitle(text = stringResource(R.string.today_up_next))
                Spacer(modifier = Modifier.height(LmcSpacing.Space2))
                StoryCompactRow(
                    story = upNextStory,
                    progress = storyPresentationUseCases.storyProgress(
                        story = upNextStory,
                        completedStoryIds = completedStoryIds,
                        savedParagraphIndex = readingPositions[upNextStory.id] ?: -1,
                    ).fraction.toFloat(),
                    completed = upNextStory.id in completedStoryIds,
                    onClick = { onRead(upNextStory.id) },
                )
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    stories: List<Story>,
    progressRecords: List<CompletionRecord>,
    readingPositions: Map<String, Int>,
    storyPresentationUseCases: StoryPresentationUseCases,
    onRead: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var selectedLevel by remember { mutableStateOf<Int?>(null) }
    val completedStoryIds = remember(progressRecords) {
        storyPresentationUseCases.completedStoryIds(progressRecords)
    }
    val libraryCards = remember(stories, completedStoryIds, readingPositions) {
        storyPresentationUseCases.libraryStoryCards(
            stories = stories,
            completedStoryIds = completedStoryIds,
            readingPositions = readingPositions,
        )
    }
    val filteredCards = remember(libraryCards, selectedLevel) {
        selectedLevel?.let { level -> libraryCards.filter { it.story.level == level } } ?: libraryCards
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = LmcSpacing.ScreenPadding,
            vertical = LmcSpacing.Space4,
        ),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
    ) {
        item {
            TopLevelHeader(
                title = stringResource(R.string.library_title),
                actions = {
                    HeaderIconButton(
                        icon = LmcIcon.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        onClick = onSettings,
                    )
                },
            )
        }
        item {
            LevelFilterRow(
                selectedLevel = selectedLevel,
                availableLevels = stories.map { it.level }.distinct().sorted(),
                onSelectedLevelChange = { selectedLevel = it },
            )
        }
        if (filteredCards.isEmpty()) {
            item {
                LibraryInlineStateCard(
                    title = stringResource(R.string.library_no_results_title),
                    body = stringResource(R.string.library_no_results_body),
                    actionLabel = stringResource(R.string.action_clear_filter),
                    onAction = { selectedLevel = null },
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    AdaptiveStoryList(
                        cards = filteredCards,
                        onRead = onRead,
                        modifier = Modifier.widthIn(max = LmcSpacing.LibraryMaxWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdaptiveStoryList(
    cards: List<LibraryStoryCard>,
    onRead: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val useTwoColumns = maxWidth >= 720.dp
        if (useTwoColumns) {
            Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4)) {
                cards.chunked(2).forEach { rowStories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
                ) {
                    rowStories.forEach { card ->
                        StoryListCard(
                            card = card,
                            modifier = Modifier.weight(1f),
                            onClick = { onRead(card.story.id) },
                        )
                    }
                    if (rowStories.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4)) {
                cards.forEach { card ->
                    StoryListCard(
                        card = card,
                        onClick = { onRead(card.story.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WordBookScreen(
    wordBookSummary: WordBookSummary,
    stories: List<Story>,
    analytics: ReaderAnalytics,
    ttsService: TtsService,
    onStartReview: () -> Unit,
    onReadToday: () -> Unit,
    onSettings: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf(WordBookFilter.All) }
    val scope = rememberCoroutineScope()
    val storyTitles = remember(stories) { stories.associate { it.id to it.titleZh } }
    val reviewPromptState = wordBookSummary.reviewPromptState()
    val filteredItems = remember(wordBookSummary.items, selectedFilter) {
        wordBookSummary.items.filter { item ->
            when (selectedFilter) {
                WordBookFilter.All -> true
                WordBookFilter.Due -> item.due
                WordBookFilter.Learning -> !item.due && item.reps < 2
                WordBookFilter.Known -> item.reps >= 2
            }
        }
    }

    LaunchedEffect(wordBookSummary.totalWords, wordBookSummary.dueCount) {
        analytics.track(
            ReaderAnalyticsEvents.wordBookOpen(
                entryPoint = "bottom_navigation",
                learnedCount = wordBookSummary.totalWords,
                dueCount = wordBookSummary.dueCount,
            ),
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = LmcSpacing.ScreenPadding,
            vertical = LmcSpacing.Space4,
        ),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
    ) {
        item {
            TopLevelHeader(
                title = stringResource(R.string.word_book_title),
                subtitle = stringResource(R.string.word_book_subtitle),
                actions = {
                    HeaderIconButton(
                        icon = LmcIcon.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        onClick = onSettings,
                    )
                },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
            ) {
                SummaryTile(
                    label = stringResource(R.string.word_book_learned_count),
                    value = wordBookSummary.totalWords.toLocalizedInt(),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedFilter = WordBookFilter.All },
                )
                SummaryTile(
                    label = stringResource(R.string.word_book_due_count),
                    value = wordBookSummary.dueCount.toLocalizedInt(),
                    modifier = Modifier.weight(1f),
                    onClick = { selectedFilter = WordBookFilter.Due },
                )
            }
        }
        if (reviewPromptState.showStartReview) {
            item {
                Button(
                    onClick = onStartReview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(text = stringResource(R.string.action_start_review))
                }
            }
        } else if (wordBookSummary.totalWords > 0) {
            item {
                WordBookEmptyState(
                    title = stringResource(R.string.word_book_no_due_title),
                    body = stringResource(R.string.word_book_no_due_body),
                    actionLabel = stringResource(R.string.action_start_reading),
                    onAction = onReadToday,
                )
            }
        }
        item {
            WordBookFilterRow(
                selectedFilter = selectedFilter,
                onSelectedFilterChange = { selectedFilter = it },
            )
        }
        if (wordBookSummary.totalWords == 0) {
            item {
                WordBookEmptyState(
                    title = stringResource(R.string.word_book_empty_title),
                    body = stringResource(R.string.word_book_empty_body),
                    actionLabel = stringResource(R.string.action_start_reading),
                    onAction = onReadToday,
                )
            }
        } else if (filteredItems.isEmpty()) {
            item {
                WordBookEmptyState(
                    title = stringResource(R.string.word_book_filter_empty_title),
                    body = stringResource(R.string.word_book_filter_empty_body),
                    actionLabel = null,
                    onAction = null,
                )
            }
        } else {
            items(filteredItems, key = { it.word }) { item ->
                LearnedWordRow(
                    item = item,
                    sourceTitle = item.sourceStoryIds.firstOrNull()?.let { storyTitles[it] },
                    onAudioClick = {
                        scope.launch {
                            ttsService.speak(listOfNotNull(item.word, item.example).joinToString(separator = " "))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WordReviewScreen(
    initialItems: List<WordBookItem>,
    analytics: ReaderAnalytics,
    ttsService: TtsService,
    vocabReviewUseCase: VocabReviewUseCase,
    onReviewChanged: () -> Unit,
    onClose: () -> Unit,
) {
    val reviewItems = remember { initialItems }
    var wordIndex by remember { mutableIntStateOf(0) }
    var answerRevealed by remember { mutableStateOf(false) }
    var knownCount by remember { mutableIntStateOf(0) }
    var needsPracticeCount by remember { mutableIntStateOf(0) }
    var complete by remember { mutableStateOf(reviewItems.isEmpty()) }
    val scope = rememberCoroutineScope()
    val currentItem = reviewItems.getOrNull(wordIndex)
    val progress = if (reviewItems.isEmpty()) 1f else wordIndex.toFloat() / reviewItems.size.toFloat()

    LaunchedEffect(complete) {
        if (complete && reviewItems.isNotEmpty()) {
            analytics.track(
                ReaderAnalyticsEvents.wordReviewComplete(
                    sessionSize = reviewItems.size,
                    reviewedCount = knownCount + needsPracticeCount,
                    knownCount = knownCount,
                    needsPracticeCount = needsPracticeCount,
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FlowTopBar(
            title = stringResource(R.string.word_review_title),
            trailing = if (reviewItems.isEmpty()) {
                null
            } else {
                stringResource(
                    R.string.word_review_progress_count,
                    (wordIndex + 1).coerceAtMost(reviewItems.size),
                    reviewItems.size,
                )
            },
            onBack = onClose,
        )
        LmcProgressBar(
            progress = if (complete) 1f else progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LmcSpacing.ScreenPadding),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (complete) {
                WordReviewCompleteContent(
                    reviewedCount = knownCount + needsPracticeCount,
                    onDone = onClose,
                )
            } else if (currentItem == null) {
                WordBookEmptyState(
                    title = stringResource(R.string.word_review_no_due_title),
                    body = stringResource(R.string.word_review_no_due_body),
                    actionLabel = stringResource(R.string.action_done),
                    onAction = onClose,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = LmcSpacing.ReadingMaxWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(LmcSpacing.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
                ) {
                    ReviewWordCard(
                        item = currentItem,
                        answerRevealed = answerRevealed,
                        onReveal = { answerRevealed = true },
                        onAudioClick = {
                            scope.launch {
                                ttsService.speak(listOfNotNull(currentItem.word, currentItem.example).joinToString(separator = " "))
                            }
                        },
                    )
                }
            }
        }
        if (!complete && currentItem != null) {
            BottomActionRow {
                if (!answerRevealed) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { answerRevealed = true },
                        modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                    ) {
                        Text(text = stringResource(R.string.action_show_answer))
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                vocabReviewUseCase.review(
                                    word = currentItem.word,
                                    assessment = VocabReviewAssessment.NeedsPractice,
                                    todayEpochMillis = System.currentTimeMillis(),
                                )
                                needsPracticeCount += 1
                                analytics.trackWordReviewAnswer(
                                    item = currentItem,
                                    rating = "needs_practice",
                                    reviewIndex = wordIndex + 1,
                                )
                                if (wordIndex >= reviewItems.lastIndex) {
                                    complete = true
                                } else {
                                    wordIndex += 1
                                    answerRevealed = false
                                }
                                onReviewChanged()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                    ) {
                        Text(text = stringResource(R.string.action_still_learning))
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                vocabReviewUseCase.review(
                                    word = currentItem.word,
                                    assessment = VocabReviewAssessment.Known,
                                    todayEpochMillis = System.currentTimeMillis(),
                                )
                                knownCount += 1
                                analytics.trackWordReviewAnswer(
                                    item = currentItem,
                                    rating = "known",
                                    reviewIndex = wordIndex + 1,
                                )
                                if (wordIndex >= reviewItems.lastIndex) {
                                    complete = true
                                } else {
                                    wordIndex += 1
                                    answerRevealed = false
                                }
                                onReviewChanged()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                    ) {
                        Text(text = stringResource(R.string.action_got_it))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingScreen(
    story: Story,
    settings: ReaderSettings,
    analytics: ReaderAnalytics,
    aiService: AiService,
    aiInteractionLogService: AiInteractionLogService,
    voiceRecordings: List<VoiceRecording>,
    voiceRecordingService: VoiceRecordingService,
    audioService: AudioService,
    ttsService: TtsService,
    stopSfx: () -> Unit,
    initialParagraphIndex: Int,
    onClose: () -> Unit,
    onTextSizeChange: (ReadingTextSize) -> Unit,
    onPinyinDefaultChange: (Boolean) -> Unit,
    onReadingPositionChange: (String, Int) -> Unit,
    onVocabulary: () -> Unit,
) {
    val readingSessionReducer = remember { ReadingSessionReducer() }
    val karaokeReducer = remember { ReadAlongKaraokeReducer() }
    val loadAudioManifestUseCase = remember { LoadStoryAudioManifestUseCase() }
    var audioManifest by remember(story.id) {
        mutableStateOf(StoryAudioManifest.empty(story.id))
    }
    // Active character ("karaoke") highlight, keyed to the playing sentence.
    var activeCharIndex by remember(story.id) { mutableStateOf<Int?>(null) }
    var activeKaraokeTimeline by remember(story.id) { mutableStateOf(KaraokeTimeline.Empty) }
    var readingState by remember(story.id, initialParagraphIndex) {
        mutableStateOf(
            readingSessionReducer.initialState(
                story = story,
                savedParagraphIndex = initialParagraphIndex,
            ),
        )
    }

    LaunchedEffect(story.id) {
        audioManifest = loadAudioManifestUseCase(story.id)
    }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val paragraphIndex = readingState.paragraphIndex
    val readingContentItems = remember(story.paragraphs) { readingContentItems(story) }
    val readingLayoutPolicy = remember { readingPageLayoutPolicy() }
    val sentenceCountInParagraph = sentenceCountFor(story, paragraphIndex)
    val listState = rememberLazyListState()
    val isListDragged by listState.interactionSource.collectIsDraggedAsState()
    var autoFollowEnabled by remember(story.id) { mutableStateOf(true) }
    var visibleReadingPosition by remember(story.id) {
        mutableStateOf(ReadingContentPosition(paragraphIndex, readingState.sentenceIndex))
    }
    val context = LocalContext.current
    val activity = LocalActivity.current
    val readingPrefs = remember(context) {
        context.getSharedPreferences(ReadingPrefsName, Context.MODE_PRIVATE)
    }
    var showCoachmark by remember(story.id) {
        mutableStateOf(!readingPrefs.getBoolean(ReadingCoachmarkDismissedKey, false))
    }
    var showSettingsSheet by remember(story.id) {
        mutableStateOf(false)
    }
    var playbackState by remember(story.id) {
        mutableStateOf(
            SentencePlaybackUiState(
                paragraphIndex = paragraphIndex,
                sentenceCountInParagraph = sentenceCountInParagraph,
                autoContinue = readingState.autoContinue,
                playbackSpeed = readingState.playbackSpeed,
            ),
        )
    }
    var playbackJob by remember(story.id) { mutableStateOf<Job?>(null) }
    val readingType = readingTypeStyles(settings.readingTextSize)
    var aiState by remember(story.id, paragraphIndex) {
        mutableStateOf<AiUiState>(AiUiState.Idle)
    }
    val aiStubText = stringResource(R.string.ai_answer_stub)
    val aiOutOfScopeText = stringResource(R.string.prompt_story_only_reply)
    val buildAiExplanationRequestUseCase = remember { BuildAiExplanationRequestUseCase() }

    // Append every controlled-AI answer to the local, parent-reviewable safety log.
    // Only story content (query word/sentence + short answer preview) + metadata —
    // never child PII (AGENTS.md §7). Outcome derives from the existing safetyOutcome.
    val logAiInteraction: (String, String, String, String) -> Unit =
        { questionType, query, answerPreview, safetyOutcome ->
            scope.launch {
                aiInteractionLogService.append(
                    AiInteractionRecord(
                        id = "${story.id}|$questionType|${System.currentTimeMillis()}",
                        storyId = story.id,
                        questionType = questionType,
                        query = query,
                        answerPreview = answerPreview,
                        outcome = if (safetyOutcome == "out_of_scope") {
                            AiInteractionOutcome.OutOfScope
                        } else {
                            AiInteractionOutcome.Allowed
                        },
                        epochMillis = System.currentTimeMillis(),
                    ),
                )
            }
        }

    // Tap-word dictionary lookup. Resolving happens in shared (WordLookupUseCase);
    // the AI fallback reuses the same controlled explain flow, targeted at one word.
    val wordLookupUseCase = remember { WordLookupUseCase() }
    var wordLookup by remember(story.id) { mutableStateOf<WordLookupResult?>(null) }
    var wordLookupAiState by remember(story.id) { mutableStateOf<AiUiState>(AiUiState.Idle) }
    val voiceRecordingController = remember(context.applicationContext) {
        AndroidVoiceRecordingController(context)
    }
    val voiceRecordingStateMachine = remember { RecordingStateMachine() }
    var voiceRecordingState by remember(story.id) { mutableStateOf<RecordingState>(RecordingState.Idle) }
    var pendingVoiceRecordingParagraphIndex by remember(story.id) { mutableIntStateOf(-1) }
    var showVoicePermissionDialog by remember(story.id) { mutableStateOf(false) }
    var showVoicePermissionSettingsAction by remember(story.id) { mutableStateOf(false) }
    var voiceRecordingMessageRes by remember(story.id) { mutableStateOf<Int?>(null) }

    DisposableEffect(voiceRecordingController) {
        onDispose { voiceRecordingController.release() }
    }

    LaunchedEffect(story.id, paragraphIndex) {
        onReadingPositionChange(story.id, paragraphIndex)
    }

    LaunchedEffect(story.id, readingContentItems) {
        val initialItemIndex = readingContentItemIndexFor(
            readingContentItems,
            paragraphIndex = paragraphIndex,
            sentenceIndex = readingState.sentenceIndex,
        )
        if (initialItemIndex > 0) {
            listState.scrollToItem(initialItemIndex)
        }
        snapshotFlow {
            readingContentPositionForItemIndex(
                readingContentItems,
                itemIndex = listState.firstVisibleItemIndex,
            )
        }
            .distinctUntilChanged()
            .collect { visiblePosition ->
                visibleReadingPosition = visiblePosition
                onReadingPositionChange(story.id, visiblePosition.paragraphIndex)
                if (playbackState.status == SentencePlaybackStatus.Stopped) {
                    val visibleState = readingSessionReducer.stateFor(
                        story = story,
                        paragraphIndex = visiblePosition.paragraphIndex,
                        sentenceIndex = visiblePosition.sentenceIndex,
                        playbackMode = readingState.playbackMode,
                        autoContinue = readingState.autoContinue,
                        playbackSpeed = readingState.playbackSpeed,
                    )
                    readingState = visibleState
                    playbackState = playbackState.copy(
                        paragraphIndex = visibleState.paragraphIndex,
                        sentenceIndex = visibleState.sentenceIndex,
                        sentenceCountInParagraph = visibleState.segments.size,
                        audioSource = null,
                    )
                }
            }
    }

    LaunchedEffect(story.id, readingState.playbackSpeed, readingState.autoContinue) {
        playbackState = playbackState.copy(
            autoContinue = readingState.autoContinue,
            playbackSpeed = readingState.playbackSpeed,
        )
    }

    LaunchedEffect(
        playbackState.paragraphIndex,
        playbackState.sentenceIndex,
        playbackState.status,
        readingContentItems,
        autoFollowEnabled,
        settings.showPinyinByDefault,
        settings.readingTextSize,
    ) {
        if (
            playbackState.status != SentencePlaybackStatus.Stopped &&
            playbackState.sentenceCountInParagraph > 0 &&
            autoFollowEnabled
        ) {
            listState.animateScrollToItem(
                readingContentItemIndexFor(
                    readingContentItems,
                    paragraphIndex = playbackState.paragraphIndex,
                    sentenceIndex = playbackState.sentenceIndex,
                ),
            )
        }
    }

    LaunchedEffect(isListDragged, playbackState.status) {
        if (isListDragged && playbackState.status != SentencePlaybackStatus.Stopped) {
            autoFollowEnabled = false
        }
    }

    // Karaoke highlight driver for the recorded-audio path: poll the player
    // position and resolve the active character from the manifest timings.
    LaunchedEffect(
        playbackState.status,
        playbackState.paragraphIndex,
        playbackState.sentenceIndex,
        playbackState.audioSource,
    ) {
        if (
            playbackState.status == SentencePlaybackStatus.Playing &&
            playbackState.audioSource == ReadingAudioSource.Recorded &&
            !activeKaraokeTimeline.isEmpty
        ) {
            while (true) {
                val positionMillis = audioService.currentPositionMillis()
                if (positionMillis != null) {
                    activeCharIndex = karaokeReducer.charIndexAt(activeKaraokeTimeline, positionMillis)
                }
                withFrameMillis { }
            }
        }
    }

    // Karaoke highlight driver for the system-TTS fallback path: native range
    // callbacks (onRangeStart) report the spoken UTF-16 range per word/char.
    DisposableEffect(ttsService) {
        ttsService.setRangeListener { utf16Start, _ ->
            val timeline = activeKaraokeTimeline
            if (
                playbackState.audioSource == ReadingAudioSource.Tts &&
                !timeline.isEmpty
            ) {
                val sentenceText = timeline.chars.joinToString(separator = "") { it.character }
                karaokeReducer.charIndexForUtf16Range(sentenceText, utf16Start)?.let { index ->
                    activeCharIndex = index
                }
            }
        }
        onDispose { ttsService.setRangeListener(null) }
    }

    fun clearKaraokeHighlight() {
        activeCharIndex = null
        activeKaraokeTimeline = KaraokeTimeline.Empty
    }

    fun stopAudioTransport() {
        playbackJob?.cancel()
        playbackJob = null
        voiceRecordingController.stopPlayback()
        stopSfx()
        if (voiceRecordingState is RecordingState.Playing) {
            voiceRecordingState = voiceRecordingStateMachine.reduce(
                voiceRecordingState,
                RecordingAction.Completed,
            )
        }
        scope.launch {
            audioService.stop()
            ttsService.stop()
        }
    }

    fun stopVoiceRecording() {
        val activeRecording = voiceRecordingState as? RecordingState.Recording ?: return
        val capture = voiceRecordingController.stopRecording()
        if (capture == null) {
            voiceRecordingState = voiceRecordingStateMachine.reduce(
                voiceRecordingState,
                RecordingAction.Reset,
            )
            voiceRecordingMessageRes = R.string.voice_recording_failed
            return
        }

        val recording = VoiceRecording(
            id = capture.file.nameWithoutExtension,
            storyId = activeRecording.storyId,
            paragraphIndex = activeRecording.paragraphIndex,
            filePath = capture.file.absolutePath,
            durationMs = capture.durationMs,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        voiceRecordingState = voiceRecordingStateMachine.reduce(
            voiceRecordingState,
            RecordingAction.Stopped(recording.id),
        )
        voiceRecordingMessageRes = R.string.voice_recording_saved
        scope.launch {
            val result = voiceRecordingService.register(recording)
            cleanupRecordingFiles(result.removed)
        }
    }

    fun openRecordingSettings() {
        val currentActivity = activity ?: return
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", currentActivity.packageName, null),
        )
        currentActivity.startActivity(intent)
    }

    fun beginVoiceRecording(targetParagraphIndex: Int) {
        if (voiceRecordingState is RecordingState.Recording) {
            stopVoiceRecording()
        }
        stopAudioTransport()
        voiceRecordingMessageRes = null
        showVoicePermissionDialog = false
        showVoicePermissionSettingsAction = false
        voiceRecordingState = voiceRecordingStateMachine.reduce(
            voiceRecordingState,
            RecordingAction.Request(story.id, targetParagraphIndex),
        )
        try {
            voiceRecordingController.startRecording(story.id, targetParagraphIndex)
            voiceRecordingState = voiceRecordingStateMachine.reduce(
                voiceRecordingState,
                RecordingAction.PermissionGranted,
            )
        } catch (_: Throwable) {
            voiceRecordingState = voiceRecordingStateMachine.reduce(
                voiceRecordingState,
                RecordingAction.Reset,
            )
            voiceRecordingMessageRes = R.string.voice_recording_failed
        }
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val targetParagraphIndex = pendingVoiceRecordingParagraphIndex
        if (granted && targetParagraphIndex >= 0) {
            pendingVoiceRecordingParagraphIndex = -1
            showVoicePermissionDialog = false
            showVoicePermissionSettingsAction = false
            beginVoiceRecording(targetParagraphIndex)
        } else {
            voiceRecordingState = voiceRecordingStateMachine.reduce(
                voiceRecordingState,
                RecordingAction.PermissionDenied,
            )
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.RECORD_AUDIO,
                )
            } ?: false
            showVoicePermissionSettingsAction = !shouldShowRationale
            showVoicePermissionDialog = true
            voiceRecordingMessageRes = R.string.voice_mic_denied
        }
    }

    fun requestVoiceRecording(targetParagraphIndex: Int) {
        val activeRecording = voiceRecordingState as? RecordingState.Recording
        if (
            activeRecording != null &&
            activeRecording.storyId == story.id &&
            activeRecording.paragraphIndex == targetParagraphIndex
        ) {
            stopVoiceRecording()
            return
        }

        if (activeRecording != null) {
            stopVoiceRecording()
        }
        pendingVoiceRecordingParagraphIndex = targetParagraphIndex
        voiceRecordingState = voiceRecordingStateMachine.reduce(
            voiceRecordingState,
            RecordingAction.Request(story.id, targetParagraphIndex),
        )
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginVoiceRecording(targetParagraphIndex)
        } else {
            voiceRecordingMessageRes = null
            showVoicePermissionDialog = true
            showVoicePermissionSettingsAction = false
        }
    }

    fun playVoiceRecording(recording: VoiceRecording) {
        if (voiceRecordingState is RecordingState.Recording) {
            stopVoiceRecording()
        }
        stopAudioTransport()
        voiceRecordingMessageRes = null
        voiceRecordingState = voiceRecordingStateMachine.reduce(
            RecordingState.Stopped(
                storyId = recording.storyId,
                paragraphIndex = recording.paragraphIndex,
                activeRecordingId = recording.id,
            ),
            RecordingAction.Playing(recording.id),
        )
        voiceRecordingController.play(
            filePath = recording.filePath,
            onCompletion = {
                voiceRecordingState = voiceRecordingStateMachine.reduce(
                    voiceRecordingState,
                    RecordingAction.Completed,
                )
            },
            onError = {
                voiceRecordingState = voiceRecordingStateMachine.reduce(
                    voiceRecordingState,
                    RecordingAction.Reset,
                )
                voiceRecordingMessageRes = R.string.voice_recording_playback_failed
            },
        )
    }

    fun stopSentencePlayback() {
        stopAudioTransport()
        clearKaraokeHighlight()
        val stopPosition = stoppedPlaybackReadingPosition(
            autoFollowEnabled = autoFollowEnabled,
            visiblePosition = visibleReadingPosition,
            playbackPosition = ReadingContentPosition(
                paragraphIndex = playbackState.paragraphIndex,
                sentenceIndex = playbackState.sentenceIndex,
            ),
        )
        val stoppedState = readingSessionReducer.stop(
            readingSessionReducer.stateFor(
                story = story,
                paragraphIndex = stopPosition.paragraphIndex,
                sentenceIndex = stopPosition.sentenceIndex,
                playbackMode = readingState.playbackMode,
                autoContinue = readingState.autoContinue,
                playbackSpeed = readingState.playbackSpeed,
            ),
        )
        readingState = stoppedState
        playbackState = SentencePlaybackUiState(
            paragraphIndex = stoppedState.paragraphIndex,
            sentenceIndex = stoppedState.sentenceIndex,
            sentenceCountInParagraph = stoppedState.segments.size,
            autoContinue = stoppedState.autoContinue,
            playbackSpeed = stoppedState.playbackSpeed,
        )
    }

    fun startSentencePlayback(
        target: SentencePlaybackTarget,
        playThroughStory: Boolean = readingState.shouldAutoAdvanceAfterSentence,
    ) {
        if (story.paragraphs.getOrNull(target.paragraphIndex) == null) return

        if (voiceRecordingState is RecordingState.Recording) {
            stopVoiceRecording()
        }
        stopAudioTransport()
        playbackJob = scope.launch {
            audioService.stop()
            ttsService.stop()

            var isFirstSentence = true
            var previousTarget: SentencePlaybackTarget? = null

            var nextTarget: SentencePlaybackTarget? = target
            while (nextTarget != null) {
                val activeTarget = nextTarget
                val targetParagraph = story.paragraphs.getOrNull(activeTarget.paragraphIndex) ?: break
                val targetSegments = SentenceSegmenter.segment(targetParagraph.text)
                val sentence = targetSegments.getOrNull(activeTarget.sentenceIndex) ?: break
                val audioSegment = audioManifest.segmentFor(
                    paragraphIndex = activeTarget.paragraphIndex,
                    sentenceIndex = activeTarget.sentenceIndex,
                )
                val hasGeneratedAudio = audioService.hasAudio(audioSegment)
                val audioSource = if (hasGeneratedAudio) {
                    ReadingAudioSource.Recorded
                } else {
                    ReadingAudioSource.Tts
                }
                val speed = readingState.playbackSpeed
                val sameParagraphAsPrevious = previousTarget?.paragraphIndex == activeTarget.paragraphIndex

                if (!isFirstSentence) {
                    delay(if (sameParagraphAsPrevious) 340L else 560L)
                }
                isFirstSentence = false
                previousTarget = activeTarget

                readingState = readingSessionReducer.stateFor(
                    story = story,
                    paragraphIndex = activeTarget.paragraphIndex,
                    sentenceIndex = activeTarget.sentenceIndex,
                    playbackStatus = ReadingPlaybackStatus.Playing,
                    playbackMode = readingState.playbackMode,
                    autoContinue = readingState.autoContinue,
                    playbackSpeed = speed,
                )
                playbackState = SentencePlaybackUiState(
                    status = SentencePlaybackStatus.Playing,
                    paragraphIndex = activeTarget.paragraphIndex,
                    sentenceIndex = activeTarget.sentenceIndex,
                    sentenceCountInParagraph = targetSegments.size,
                    audioSource = audioSource,
                    playThroughStory = playThroughStory,
                    autoContinue = readingState.autoContinue,
                    playbackSpeed = speed,
                )
                // Reset the karaoke highlight for the newly active sentence.
                activeKaraokeTimeline = karaokeTimelineFor(
                    audioManifest = audioManifest,
                    hasGeneratedAudio = hasGeneratedAudio,
                    paragraphIndex = activeTarget.paragraphIndex,
                    sentenceIndex = activeTarget.sentenceIndex,
                    sentenceText = sentence.text,
                    karaokeReducer = karaokeReducer,
                )
                activeCharIndex = if (activeKaraokeTimeline.isEmpty) null else 0
                analytics.track(
                    ReaderAnalyticsEvents.paragraphAudioPlay(
                        storyId = story.id,
                        paragraphIndex = activeTarget.paragraphIndex,
                        audioSource = audioSource.analyticsValue,
                        sentenceIndex = activeTarget.sentenceIndex,
                        playbackSpeedBucket = speed.analyticsBucket,
                    ),
                )

                if (hasGeneratedAudio) {
                    try {
                        audioService.playSegment(
                            segment = requireNotNull(audioSegment),
                            speedMultiplier = speed.multiplier.toFloat(),
                        )
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                        playbackState = playbackState.copy(audioSource = ReadingAudioSource.Tts)
                        analytics.track(
                            ReaderAnalyticsEvents.paragraphAudioPlay(
                                storyId = story.id,
                                paragraphIndex = activeTarget.paragraphIndex,
                                audioSource = ReadingAudioSource.Tts.analyticsValue,
                                sentenceIndex = activeTarget.sentenceIndex,
                                playbackSpeedBucket = speed.analyticsBucket,
                            ),
                        )
                        ttsService.speak(sentence.text, speed.multiplier.toFloat())
                        delay(estimatedSentencePlaybackMillis(sentence.text, speed))
                    }
                } else {
                    ttsService.speak(sentence.text, speed.multiplier.toFloat())
                    delay(estimatedSentencePlaybackMillis(sentence.text, speed))
                }

                nextTarget = if (playThroughStory) {
                    nextSentenceTarget(story, activeTarget)
                } else {
                    null
                }
            }

            if (playbackState.status == SentencePlaybackStatus.Playing) {
                val stopPosition = stoppedPlaybackReadingPosition(
                    autoFollowEnabled = autoFollowEnabled,
                    visiblePosition = visibleReadingPosition,
                    playbackPosition = ReadingContentPosition(
                        paragraphIndex = playbackState.paragraphIndex,
                        sentenceIndex = playbackState.sentenceIndex,
                    ),
                )
                val stoppedState = readingSessionReducer.stop(
                    readingSessionReducer.stateFor(
                        story = story,
                        paragraphIndex = stopPosition.paragraphIndex,
                        sentenceIndex = stopPosition.sentenceIndex,
                        playbackMode = readingState.playbackMode,
                        autoContinue = readingState.autoContinue,
                        playbackSpeed = readingState.playbackSpeed,
                    ),
                )
                readingState = stoppedState
                playbackState = SentencePlaybackUiState(
                    paragraphIndex = stoppedState.paragraphIndex,
                    sentenceIndex = stoppedState.sentenceIndex,
                    sentenceCountInParagraph = stoppedState.segments.size,
                    autoContinue = stoppedState.autoContinue,
                    playbackSpeed = stoppedState.playbackSpeed,
                )
                clearKaraokeHighlight()
            }
            playbackJob = null
        }
    }

    fun startCurrentSentencePlayback() {
        val targetSentenceIndex = if (playbackState.paragraphIndex == paragraphIndex) {
            playbackState.sentenceIndex.coerceIn(0, (sentenceCountInParagraph - 1).coerceAtLeast(0))
        } else {
            0
        }
        if (sentenceCountInParagraph > 0) {
            val nextReadingState = readingSessionReducer.playReadAlong(
                story = story,
                state = readingState,
            )
            readingState = nextReadingState
            autoFollowEnabled = true
            startSentencePlayback(
                target = SentencePlaybackTarget(
                    paragraphIndex = paragraphIndex,
                    sentenceIndex = targetSentenceIndex,
                ),
                playThroughStory = nextReadingState.shouldAutoAdvanceAfterSentence,
            )
        }
    }

    fun pauseSentencePlayback() {
        if (playbackState.status != SentencePlaybackStatus.Playing) return

        playbackJob?.cancel()
        playbackJob = null
        val activePlayback = playbackState
        playbackState = activePlayback.copy(status = SentencePlaybackStatus.Paused)
        readingState = readingSessionReducer.pause(readingState)
        scope.launch {
            if (activePlayback.audioSource == ReadingAudioSource.Recorded) {
                audioService.pause()
            } else {
                ttsService.stop()
            }
        }
    }

    fun requestVoiceRecordingPermission() {
        val targetParagraphIndex = pendingVoiceRecordingParagraphIndex
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            showVoicePermissionDialog = false
            showVoicePermissionSettingsAction = false
            if (targetParagraphIndex >= 0) {
                beginVoiceRecording(targetParagraphIndex)
            }
            return
        }
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun resumeSentencePlayback() {
        readingState = readingSessionReducer.resume(readingState)
        autoFollowEnabled = true
        startSentencePlayback(
            target = SentencePlaybackTarget(
                paragraphIndex = playbackState.paragraphIndex,
                sentenceIndex = playbackState.sentenceIndex,
            ),
            playThroughStory = playbackState.playThroughStory,
        )
    }

    fun returnToReadingPlace() {
        autoFollowEnabled = true
        scope.launch {
            listState.animateScrollToItem(
                readingContentItemIndexFor(
                    readingContentItems,
                    paragraphIndex = playbackState.paragraphIndex,
                    sentenceIndex = playbackState.sentenceIndex,
                ),
            )
        }
    }

    fun playNextSentence() {
        val transition = readingSessionReducer.nextSentence(story, readingState)
        val nextState = transition.state
        if (
            nextState.paragraphIndex == readingState.paragraphIndex &&
            nextState.sentenceIndex == readingState.sentenceIndex
        ) {
            return
        }
        val wasPlaying = playbackState.status == SentencePlaybackStatus.Playing
        val playThrough = playbackState.playThroughStory && readingState.autoContinue
        stopAudioTransport()
        readingState = nextState
        autoFollowEnabled = true
        playbackState = playbackState.copy(
            status = SentencePlaybackStatus.Stopped,
            paragraphIndex = nextState.paragraphIndex,
            sentenceIndex = nextState.sentenceIndex,
            sentenceCountInParagraph = nextState.segments.size,
            audioSource = null,
        )
        if (wasPlaying) {
            startSentencePlayback(
                target = SentencePlaybackTarget(nextState.paragraphIndex, nextState.sentenceIndex),
                playThroughStory = playThrough,
            )
        }
    }

    fun playPreviousSentence() {
        val transition = readingSessionReducer.previousSentence(story, readingState)
        val previousState = transition.state
        if (
            previousState.paragraphIndex == readingState.paragraphIndex &&
            previousState.sentenceIndex == readingState.sentenceIndex
        ) {
            return
        }
        val wasPlaying = playbackState.status == SentencePlaybackStatus.Playing
        val playThrough = playbackState.playThroughStory && readingState.autoContinue
        stopAudioTransport()
        readingState = previousState
        autoFollowEnabled = true
        playbackState = playbackState.copy(
            status = SentencePlaybackStatus.Stopped,
            paragraphIndex = previousState.paragraphIndex,
            sentenceIndex = previousState.sentenceIndex,
            sentenceCountInParagraph = previousState.segments.size,
            audioSource = null,
        )
        if (wasPlaying) {
            startSentencePlayback(
                target = SentencePlaybackTarget(previousState.paragraphIndex, previousState.sentenceIndex),
                playThroughStory = playThrough,
            )
        }
    }

    fun repeatCurrentSentence() {
        val currentMode = readingState.playbackMode
        val repeatState = readingSessionReducer.repeatSentence(story, readingState)
        readingState = repeatState.copy(playbackMode = currentMode)
        autoFollowEnabled = true
        startSentencePlayback(
            target = SentencePlaybackTarget(repeatState.paragraphIndex, repeatState.sentenceIndex),
            playThroughStory = false,
        )
    }

    fun playSentenceFromTap(targetParagraphIndex: Int, sentenceIndex: Int) {
        val selectedState = readingSessionReducer.playSentence(
            story = story,
            state = readingState,
            paragraphIndex = targetParagraphIndex,
            sentenceIndex = sentenceIndex,
            playbackMode = readingState.playbackMode,
        )
        readingState = selectedState
        autoFollowEnabled = true
        startSentencePlayback(
            target = SentencePlaybackTarget(targetParagraphIndex, selectedState.sentenceIndex),
            playThroughStory = selectedState.shouldAutoAdvanceAfterSentence,
        )
    }

    fun replaySentenceOnly(targetParagraphIndex: Int, sentenceIndex: Int) {
        val currentMode = readingState.playbackMode
        val selectedState = readingSessionReducer.playSentence(
            story = story,
            state = readingState,
            paragraphIndex = targetParagraphIndex,
            sentenceIndex = sentenceIndex,
            playbackMode = ReadingSessionMode.TapToListen,
        )
        readingState = selectedState.copy(playbackMode = currentMode)
        autoFollowEnabled = true
        startSentencePlayback(
            target = SentencePlaybackTarget(targetParagraphIndex, selectedState.sentenceIndex),
            playThroughStory = false,
        )
    }

    DisposableEffect(lifecycleOwner, playbackState.status) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && playbackState.status == SentencePlaybackStatus.Playing) {
                pauseSentencePlayback()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val canGoPreviousSentence = previousSentenceTarget(
        story = story,
        target = SentencePlaybackTarget(
            paragraphIndex = playbackState.paragraphIndex,
            sentenceIndex = playbackState.sentenceIndex,
        ),
    ) != null
    val canGoNextSentence = nextSentenceTarget(
        story = story,
        target = SentencePlaybackTarget(
            paragraphIndex = playbackState.paragraphIndex,
            sentenceIndex = playbackState.sentenceIndex,
        ),
    ) != null
    val showReturnToReadingPlace = !autoFollowEnabled &&
        playbackState.status != SentencePlaybackStatus.Stopped &&
        playbackState.sentenceCountInParagraph > 0

    val dockContentPaddingBottom = when (playbackState.status) {
        SentencePlaybackStatus.Stopped -> LmcSpacing.BottomActionHeight
        else -> LmcSpacing.BottomActionHeight + LmcSpacing.Space6
    }

    // Tapping a word resolves it via shared and opens the lookup sheet. It must NOT
    // start audio: per-character cells consume the tap before the sentence surface.
    val onWordTap: (String) -> Unit = remember(story.id) {
        { token ->
            wordLookupAiState = AiUiState.Idle
            wordLookup = wordLookupUseCase.lookup(story, token)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ReadingTopBar(
                story = story,
                onClose = {
                    stopVoiceRecording()
                    stopAudioTransport()
                    onClose()
                },
                onSettingsClick = {
                    showSettingsSheet = true
                },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = LmcSpacing.ReadingMaxWidth)
                        .padding(
                            horizontal = LmcSpacing.ScreenPadding,
                            vertical = LmcSpacing.Space4,
                        ),
                    contentPadding = PaddingValues(
                        bottom = dockContentPaddingBottom +
                            if (showCoachmark) {
                                LmcSpacing.Space8
                            } else {
                                LmcSpacing.Space3
                            },
                    ),
                    verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
                ) {
                    items(
                        items = readingContentItems,
                        key = { item -> item.key },
                    ) { item ->
                        when (item) {
                            is ReadingContentItem.Sentence -> {
                                val sentenceHighlighted =
                                    playbackState.status != SentencePlaybackStatus.Stopped &&
                                        playbackState.paragraphIndex == item.paragraphIndex &&
                                        playbackState.sentenceIndex == item.sentenceIndex
                                SentenceTextBlock(
                                    paragraph = item.paragraph,
                                    sentence = item.sentence,
                                    showPinyin = settings.showPinyinByDefault,
                                    readingType = readingType,
                                    isHighlighted = sentenceHighlighted,
                                    activeCharIndex = if (sentenceHighlighted) activeCharIndex else null,
                                    sentenceIndex = item.sentenceIndex,
                                    sentenceCount = sentenceCountFor(story, item.paragraphIndex),
                                    onSentenceClick = { playSentenceFromTap(item.paragraphIndex, item.sentenceIndex) },
                                    onSpeakerClick = { replaySentenceOnly(item.paragraphIndex, item.sentenceIndex) },
                                    onWordTap = onWordTap,
                                )
                            }
                            is ReadingContentItem.ParagraphBody -> {
                                PinyinTextBlock(
                                    paragraph = item.paragraph,
                                    showPinyin = settings.showPinyinByDefault,
                                    readingType = readingType,
                                    onWordTap = onWordTap,
                                )
                            }
                        }
                    }
                    item(key = "reading-vocabulary") {
                        Button(
                            onClick = {
                                stopAudioTransport()
                                onVocabulary()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                            shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                        ) {
                            Text(text = stringResource(R.string.nav_vocabulary))
                        }
                    }
                    if (readingLayoutPolicy.showVoicePracticeCard) {
                        item(key = "paragraph-$paragraphIndex-voice-recording") {
                            val currentParagraphVoiceRecordings = voiceRecordings
                                .filter { it.storyId == story.id && it.paragraphIndex == paragraphIndex }
                                .sortedByDescending { it.createdAtEpochMillis }
                            VoiceRecordingPracticeCard(
                                latestRecording = currentParagraphVoiceRecordings.firstOrNull(),
                                isRecording = (voiceRecordingState as? RecordingState.Recording)
                                    ?.let { it.storyId == story.id && it.paragraphIndex == paragraphIndex } == true,
                                isPlaying = (voiceRecordingState as? RecordingState.Playing)
                                    ?.let { it.storyId == story.id && it.paragraphIndex == paragraphIndex } == true,
                                showMicRationale = showVoicePermissionDialog &&
                                    pendingVoiceRecordingParagraphIndex == paragraphIndex,
                                messageRes = voiceRecordingMessageRes,
                                onRecordClick = { requestVoiceRecording(paragraphIndex) },
                                onAllowMicClick = {
                                    if (showVoicePermissionSettingsAction) {
                                        openRecordingSettings()
                                    } else {
                                        requestVoiceRecordingPermission()
                                    }
                                },
                                onPlayClick = {
                                    currentParagraphVoiceRecordings.firstOrNull()?.let(::playVoiceRecording)
                                },
                                showPermissionSettingsAction = showVoicePermissionSettingsAction &&
                                    pendingVoiceRecordingParagraphIndex == paragraphIndex,
                            )
                        }
                    }
                    if (readingLayoutPolicy.showParagraphExplanationCard) {
                        item(key = "paragraph-$paragraphIndex-ask") {
                            val paragraph = readingState.currentParagraph
                            if (paragraph != null) {
                                AskExplanationCard(
                                    state = aiState,
                                    enabled = paragraph.text.isNotBlank(),
                                    onAsk = {
                                        val request = buildAiExplanationRequestUseCase.forParagraph(
                                            storyId = story.id,
                                            paragraph = paragraph,
                                        )
                                        if (request == null) {
                                            aiState = AiUiState.Error
                                            return@AskExplanationCard
                                        }
                                        aiState = AiUiState.Loading
                                        scope.launch {
                                            runCatching {
                                                aiService.explain(request)
                                            }.onSuccess { response ->
                                                val answer = response.toLimitedDisplayText(
                                                    stubText = aiStubText,
                                                    outOfScopeText = aiOutOfScopeText,
                                                )
                                                val outcome = response.safetyOutcome(answer, aiOutOfScopeText)
                                                analytics.track(
                                                    ReaderAnalyticsEvents.aiExplainRequest(
                                                        storyId = story.id,
                                                        requestType = AiQuestionTypes.ExplainSentence,
                                                        targetType = "paragraph",
                                                        safetyOutcome = outcome,
                                                    ),
                                                )
                                                logAiInteraction(
                                                    request.questionType,
                                                    request.selectedText,
                                                    answer,
                                                    outcome,
                                                )
                                                aiState = AiUiState.Answer(answer)
                                            }.onFailure {
                                                analytics.track(
                                                    ReaderAnalyticsEvents.aiExplainRequest(
                                                        storyId = story.id,
                                                        requestType = AiQuestionTypes.ExplainSentence,
                                                        targetType = "paragraph",
                                                        safetyOutcome = "error",
                                                    ),
                                                )
                                                aiState = AiUiState.Error
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            ReadingBottomDock(
                playbackStatus = playbackState.status,
                playbackState = playbackState,
                onReadClick = {
                    startCurrentSentencePlayback()
                },
                canGoPreviousSentence = canGoPreviousSentence,
                canGoNextSentence = canGoNextSentence,
                onPreviousSentence = ::playPreviousSentence,
                onNextSentence = ::playNextSentence,
                onRepeatSentence = ::repeatCurrentSentence,
                onPlayPause = {
                    when (playbackState.status) {
                        SentencePlaybackStatus.Playing -> pauseSentencePlayback()
                        SentencePlaybackStatus.Paused -> resumeSentencePlayback()
                        SentencePlaybackStatus.Stopped -> startCurrentSentencePlayback()
                    }
                },
                onStop = {
                    stopSentencePlayback()
                },
                onReturnToReadingPlace = { returnToReadingPlace() },
                showReturnToReadingPlace = showReturnToReadingPlace,
            )
        }
        if (showCoachmark) {
            ReadingAudioCoachmark(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = LmcSpacing.ScreenPadding)
                    .padding(bottom = dockContentPaddingBottom + LmcSpacing.Space3),
            onDismiss = {
                readingPrefs.edit().putBoolean(ReadingCoachmarkDismissedKey, true).apply()
                showCoachmark = false
            },
        )
        }
    }

    if (showSettingsSheet) {
        ReadingSettingsSheet(
            showPinyin = settings.showPinyinByDefault,
            readingTextSize = settings.readingTextSize,
            playbackMode = readingState.playbackMode,
            autoContinue = readingState.autoContinue,
            playbackSpeed = playbackState.playbackSpeed,
            onPinyinChange = { enabled ->
                analytics.track(
                    ReaderAnalyticsEvents.pinyinToggle(
                        storyId = story.id,
                        enabled = enabled,
                        surface = "reading",
                        paragraphIndex = paragraphIndex,
                    ),
                )
                onPinyinDefaultChange(enabled)
            },
            onTextSizeChange = onTextSizeChange,
            onPlaybackModeChange = { mode ->
                readingState = readingSessionReducer.setPlaybackMode(readingState, mode)
            },
            onAutoContinueChange = { enabled ->
                readingState = readingSessionReducer.setAutoContinue(readingState, enabled)
                playbackState = playbackState.copy(autoContinue = enabled)
            },
            onPlaybackSpeedChange = { speed ->
                readingState = readingSessionReducer.setPlaybackSpeed(readingState, speed)
                playbackState = playbackState.copy(playbackSpeed = speed)
            },
            onDismiss = { showSettingsSheet = false },
        )
    }

    wordLookup?.let { lookup ->
        WordLookupSheet(
            lookup = lookup,
            aiState = wordLookupAiState,
            onExplainWithAi = {
                if (lookup !is WordLookupResult.NeedsAi) return@WordLookupSheet
                val request = buildAiExplanationRequestUseCase.forSelectedText(
                    storyId = story.id,
                    selectedText = lookup.token,
                    questionType = AiQuestionTypes.ExplainWord,
                )
                if (request == null) {
                    wordLookupAiState = AiUiState.Error
                    return@WordLookupSheet
                }
                wordLookupAiState = AiUiState.Loading
                scope.launch {
                    runCatching { aiService.explain(request) }
                        .onSuccess { response ->
                            val answer = response.toLimitedDisplayText(
                                stubText = aiStubText,
                                outOfScopeText = aiOutOfScopeText,
                            )
                            val outcome = response.safetyOutcome(answer, aiOutOfScopeText)
                            analytics.track(
                                ReaderAnalyticsEvents.aiExplainRequest(
                                    storyId = story.id,
                                    requestType = AiQuestionTypes.ExplainWord,
                                    targetType = "word",
                                    safetyOutcome = outcome,
                                ),
                            )
                            logAiInteraction(
                                request.questionType,
                                request.selectedText,
                                answer,
                                outcome,
                            )
                            wordLookupAiState = AiUiState.Answer(answer)
                        }
                        .onFailure {
                            analytics.track(
                                ReaderAnalyticsEvents.aiExplainRequest(
                                    storyId = story.id,
                                    requestType = AiQuestionTypes.ExplainWord,
                                    targetType = "word",
                                    safetyOutcome = "error",
                                ),
                            )
                            wordLookupAiState = AiUiState.Error
                        }
                }
            },
            onDismiss = {
                wordLookup = null
                wordLookupAiState = AiUiState.Idle
            },
        )
    }

}

@Composable
private fun VoiceRecordingPracticeCard(
    latestRecording: VoiceRecording?,
    isRecording: Boolean,
    isPlaying: Boolean,
    showMicRationale: Boolean,
    showPermissionSettingsAction: Boolean,
    @StringRes messageRes: Int?,
    onRecordClick: () -> Unit,
    onAllowMicClick: () -> Unit,
    onPlayClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusSm),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = stringResource(R.string.voice_practice_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.voice_practice_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.voice_practice_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onRecordClick,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(
                        text = stringResource(
                            if (isRecording) {
                                R.string.action_stop_recording
                            } else {
                                R.string.action_record_voice
                            },
                        ),
                    )
                }
                OutlinedButton(
                    onClick = onPlayClick,
                    enabled = latestRecording != null && !isRecording,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(
                        text = stringResource(
                            if (isPlaying) {
                                R.string.voice_recording_playing
                            } else {
                                R.string.action_play_recording
                            },
                        ),
                    )
                }
            }
            latestRecording?.let { recording ->
                Text(
                    text = stringResource(
                        R.string.voice_latest_recording_format,
                        voiceRecordingDurationText(recording.durationMs),
                        relativeTimeText(recording.createdAtEpochMillis),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (showMicRationale) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LmcSpacing.RadiusSm),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.padding(LmcSpacing.Space3),
                        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                    ) {
                        Text(
                            text = stringResource(R.string.voice_mic_rationale_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                if (showPermissionSettingsAction) {
                                    R.string.voice_mic_settings_body
                                } else {
                                    R.string.voice_mic_rationale_body
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = onAllowMicClick,
                            modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                            shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                        ) {
                            Text(
                                text = stringResource(
                                    if (showPermissionSettingsAction) {
                                        R.string.action_open_settings
                                    } else {
                                        R.string.voice_mic_allow
                                    },
                                ),
                            )
                        }
                    }
                }
            }
            messageRes?.let { resId ->
                Text(
                    text = stringResource(resId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

private data class SentencePlaybackTarget(
    val paragraphIndex: Int,
    val sentenceIndex: Int,
)

internal sealed interface ReadingContentItem {
    val paragraphIndex: Int
    val key: String

    data class Sentence(
        override val paragraphIndex: Int,
        val sentenceIndex: Int,
        val paragraph: Paragraph,
        val sentence: SentenceSegment,
    ) : ReadingContentItem {
        override val key: String = "paragraph-$paragraphIndex-sentence-$sentenceIndex"
    }

    data class ParagraphBody(
        override val paragraphIndex: Int,
        val paragraph: Paragraph,
    ) : ReadingContentItem {
        override val key: String = "paragraph-$paragraphIndex-body"
    }
}

internal data class ReadingPageLayoutPolicy(
    val showVoicePracticeCard: Boolean = false,
    val showParagraphExplanationCard: Boolean = false,
)

internal fun readingPageLayoutPolicy(): ReadingPageLayoutPolicy =
    ReadingPageLayoutPolicy()

internal data class ReadingContentPosition(
    val paragraphIndex: Int,
    val sentenceIndex: Int,
)

internal fun readingContentItems(story: Story): List<ReadingContentItem> =
    story.paragraphs.flatMapIndexed { paragraphIndex, paragraph ->
        val sentences = SentenceSegmenter.segment(paragraph.text)
        if (sentences.isEmpty()) {
            listOf(
                ReadingContentItem.ParagraphBody(
                    paragraphIndex = paragraphIndex,
                    paragraph = paragraph,
                ),
            )
        } else {
            sentences.mapIndexed { sentenceIndex, sentence ->
                ReadingContentItem.Sentence(
                    paragraphIndex = paragraphIndex,
                    sentenceIndex = sentenceIndex,
                    paragraph = paragraph,
                    sentence = sentence,
                )
            }
        }
    }

internal fun readingContentItemIndexFor(
    items: List<ReadingContentItem>,
    paragraphIndex: Int,
    sentenceIndex: Int,
): Int {
    val sentenceItemIndex = items.indexOfFirst { item ->
        item is ReadingContentItem.Sentence &&
            item.paragraphIndex == paragraphIndex &&
            item.sentenceIndex == sentenceIndex
    }
    if (sentenceItemIndex >= 0) return sentenceItemIndex

    val paragraphItemIndex = items.indexOfFirst { item ->
        item.paragraphIndex == paragraphIndex
    }
    return paragraphItemIndex.coerceAtLeast(0)
}

internal fun readingContentPositionForItemIndex(
    items: List<ReadingContentItem>,
    itemIndex: Int,
): ReadingContentPosition {
    val item = items.getOrNull(itemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    return when (item) {
        is ReadingContentItem.Sentence -> ReadingContentPosition(
            paragraphIndex = item.paragraphIndex,
            sentenceIndex = item.sentenceIndex,
        )
        is ReadingContentItem.ParagraphBody -> ReadingContentPosition(
            paragraphIndex = item.paragraphIndex,
            sentenceIndex = 0,
        )
        null -> ReadingContentPosition(paragraphIndex = 0, sentenceIndex = 0)
    }
}

internal fun stoppedPlaybackReadingPosition(
    autoFollowEnabled: Boolean,
    visiblePosition: ReadingContentPosition,
    playbackPosition: ReadingContentPosition,
): ReadingContentPosition =
    if (autoFollowEnabled) playbackPosition else visiblePosition

private enum class SentencePlaybackStatus {
    Stopped,
    Playing,
    Paused,
}

private const val ReadingCoachmarkDismissedKey = "reading_coachmark_tts_row_dismissed"
private const val ReadingPrefsName = "little_mandarin_reader_settings"
private const val ParentGateExpectedAnswer = "23"

private data class SentencePlaybackUiState(
    val status: SentencePlaybackStatus = SentencePlaybackStatus.Stopped,
    val paragraphIndex: Int = 0,
    val sentenceIndex: Int = 0,
    val sentenceCountInParagraph: Int = 0,
    val audioSource: ReadingAudioSource? = null,
    val playThroughStory: Boolean = true,
    val autoContinue: Boolean = true,
    val playbackSpeed: ReadingPlaybackSpeed = ReadingPlaybackSpeed.DefaultSlow,
)

private fun nextSentenceTarget(
    story: Story,
    target: SentencePlaybackTarget,
): SentencePlaybackTarget? {
    val currentSentenceCount = sentenceCountFor(story, target.paragraphIndex)
    if (target.sentenceIndex + 1 < currentSentenceCount) {
        return target.copy(sentenceIndex = target.sentenceIndex + 1)
    }

    var nextParagraphIndex = target.paragraphIndex + 1
    while (nextParagraphIndex < story.paragraphs.size) {
        if (sentenceCountFor(story, nextParagraphIndex) > 0) {
            return SentencePlaybackTarget(
                paragraphIndex = nextParagraphIndex,
                sentenceIndex = 0,
            )
        }
        nextParagraphIndex += 1
    }

    return null
}

private fun previousSentenceTarget(
    story: Story,
    target: SentencePlaybackTarget,
): SentencePlaybackTarget? {
    if (target.sentenceIndex > 0) {
        return target.copy(sentenceIndex = target.sentenceIndex - 1)
    }

    var previousParagraphIndex = target.paragraphIndex - 1
    while (previousParagraphIndex >= 0) {
        val count = sentenceCountFor(story, previousParagraphIndex)
        if (count > 0) {
            return SentencePlaybackTarget(
                paragraphIndex = previousParagraphIndex,
                sentenceIndex = count - 1,
            )
        }
        previousParagraphIndex -= 1
    }

    return null
}

private fun sentenceCountFor(
    story: Story,
    paragraphIndex: Int,
): Int = story.paragraphs
    .getOrNull(paragraphIndex)
    ?.let { SentenceSegmenter.segment(it.text).size }
    ?: 0

private fun estimatedSentencePlaybackMillis(
    text: String,
    speed: ReadingPlaybackSpeed,
): Long {
    val baseMillis = (text.length.coerceAtLeast(4) * 320L).coerceIn(1_400L, 12_000L)
    return (baseMillis.toDouble() / speed.multiplier.coerceAtLeast(0.5)).toLong().coerceIn(1_400L, 12_000L)
}

private fun karaokeTimelineFor(
    audioManifest: StoryAudioManifest,
    hasGeneratedAudio: Boolean,
    paragraphIndex: Int,
    sentenceIndex: Int,
    sentenceText: String,
    karaokeReducer: ReadAlongKaraokeReducer,
): KaraokeTimeline {
    if (hasGeneratedAudio) {
        val segment = audioManifest.segmentFor(paragraphIndex, sentenceIndex)
        if (segment != null) {
            return karaokeReducer.timelineForSegment(segment)
        }
    }
    // System-TTS fallback (or missing manifest timings): the timeline only needs
    // the per-character text; native onRangeStart callbacks drive the cursor.
    return karaokeReducer.timelineForText(sentenceText, durationMillis = 0L)
}

private fun Paragraph.cellsFor(sentence: SentenceSegment): List<PinyinCell> {
    if (cells.isEmpty()) return emptyList()

    var offset = 0
    return cells.filter { cell ->
        val cellStart = offset
        val cellEnd = cellStart + cell.c.length
        offset = cellEnd
        cellEnd > sentence.startOffset && cellStart < sentence.endOffset
    }
}

@Composable
private fun VocabularyScreen(
    story: Story,
    analytics: ReaderAnalytics,
    ttsService: TtsService,
    entrySource: VocabularyEntrySource,
    onBack: () -> Unit,
    onQuiz: () -> Unit,
) {
    val words = story.vocab
    val wordCount = words.size.coerceAtLeast(1)
    var wordIndex by remember(story.id) { mutableIntStateOf(0) }
    val currentWord = words.getOrNull(wordIndex)
    val scope = rememberCoroutineScope()
    val buildSpeechTextUseCase = remember { BuildSpeechTextUseCase() }

    LaunchedEffect(story.id, wordIndex, currentWord) {
        if (currentWord != null) {
            analytics.track(
                ReaderAnalyticsEvents.vocabOpen(
                    story = story,
                    vocabIndex = wordIndex,
                    openSource = "vocabulary_screen",
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FlowTopBar(
            title = stringResource(R.string.vocab_title),
            trailing = stringResource(
                R.string.vocab_progress_count,
                (wordIndex + 1).coerceAtMost(wordCount),
                wordCount,
            ),
            onBack = onBack,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = LmcSpacing.ReadingMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(LmcSpacing.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
            ) {
                StoryEyebrow(story = story)
                if (currentWord == null) {
                    WordBookEmptyState(
                        title = stringResource(R.string.vocab_empty_title),
                        body = stringResource(R.string.vocab_empty_body),
                        actionLabel = null,
                        onAction = null,
                    )
                } else {
                    VocabularyCard(
                        word = currentWord,
                        onAudioClick = {
                            scope.launch {
                                ttsService.speak(buildSpeechTextUseCase.vocab(currentWord))
                            }
                        },
                    )
                    StepDots(
                        count = words.size,
                        selectedIndex = wordIndex,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
        BottomActionRow {
            if (words.isEmpty()) {
                TextButton(onClick = onBack) {
                    Text(text = stringResource(entrySource.backActionLabelRes()))
                }
                Button(
                    onClick = onQuiz,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(text = stringResource(R.string.action_continue_to_quiz))
                }
            } else {
                TextButton(onClick = onBack) {
                    Text(text = stringResource(R.string.action_back))
                }
                Button(
                    onClick = {
                        if (wordIndex < words.lastIndex) {
                            wordIndex += 1
                        } else {
                            onQuiz()
                        }
                    },
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(
                        text = stringResource(
                            if (wordIndex < words.lastIndex) {
                                R.string.action_next
                            } else {
                                R.string.action_quiz
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizScreen(
    story: Story,
    storyOrder: Int,
    analytics: ReaderAnalytics,
    progressService: ProgressService,
    streakUseCase: StreakUseCase,
    vocabReviewUseCase: VocabReviewUseCase,
    reviewPackUseCase: ReviewPackUseCase,
    voiceRecordingService: VoiceRecordingService,
    dueWords: Set<String>,
    onAnswerSubmitted: (Boolean) -> Unit,
    onCompletionSfx: (Int?) -> Unit,
    onReviewPack: (ReviewPack) -> Unit,
    onBack: () -> Unit,
    onCompletionRecorded: () -> Unit,
    onReadAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val quizSessionReducer = remember { QuizSessionReducer() }
    var quizState by remember(story.id) {
        mutableStateOf(quizSessionReducer.initialState(story))
    }
    val questionState = remember(story.id, quizState) {
        quizSessionReducer.questionState(story, quizState)
    }
    val score = remember(story.id, quizState.answers) {
        quizSessionReducer.score(story, quizState)
    }
    var lastSubmittedQuestionIndex by remember(story.id) {
        mutableIntStateOf(Int.MIN_VALUE)
    }
    var completionHandled by remember(story.id) { mutableStateOf(false) }
    var completionStreakSummary by remember(story.id) { mutableStateOf<StreakSummary?>(null) }
    var completionJustRecorded by remember(story.id) { mutableStateOf(false) }
    var completionReviewPack by remember(story.id) { mutableStateOf<ReviewPack?>(null) }
    val reviewRemediationUseCases = remember { MistakeRemediationUseCases() }

    LaunchedEffect(story.id) {
        analytics.track(ReaderAnalyticsEvents.quizStart(story))
    }

    LaunchedEffect(questionState.submitted, questionState.questionIndex, questionState.result?.questionId) {
        if (
            questionState.submitted &&
            questionState.result != null &&
            lastSubmittedQuestionIndex != questionState.questionIndex
        ) {
            val result = questionState.result ?: return@LaunchedEffect
            lastSubmittedQuestionIndex = questionState.questionIndex
            onAnswerSubmitted(result.isCorrect)
        }
    }

    LaunchedEffect(quizState.isComplete) {
        if (quizState.isComplete && !completionHandled) {
            completionHandled = true
            val now = System.currentTimeMillis()
            val wasAlreadyCompleted = progressService.getRecords().any { it.storyId == story.id }
            completionJustRecorded = !wasAlreadyCompleted
            MarkStoryCompletedUseCase(progressService).invoke(
                quizSessionReducer.completionRecord(
                    story = story,
                    state = quizState,
                    nowEpochMillis = now,
                ),
            )
            vocabReviewUseCase.syncLearnedWords(todayEpochMillis = now)
            val reviewPack = reviewRemediationUseCases.buildReviewPack(
                story = story,
                quizScore = score,
                dueWords = dueWords,
            )
            completionReviewPack = reviewPack
            reviewPackUseCase.savePack(reviewPack, todayEpochMillis = now)
            completionStreakSummary = if (wasAlreadyCompleted) {
                streakUseCase.summary(todayEpochMillis = now)
            } else {
                streakUseCase.recordStoryCompleted(nowEpochMillis = now)
            }
            val newMilestoneDays = completionStreakSummary?.newMilestoneDays
            if (completionJustRecorded || newMilestoneDays != null) {
                onCompletionSfx(newMilestoneDays)
            }
            onCompletionRecorded()
            analytics.track(ReaderAnalyticsEvents.quizComplete(story, score))
            analytics.track(
                ReaderAnalyticsEvents.storyComplete(
                    story = story,
                    storyOrder = storyOrder,
                    quizCompleted = true,
                ),
            )
        }
    }

    if (quizState.isComplete) {
        QuizCompletionScreen(
            story = story,
            correctCount = score.correctCount,
            totalQuestions = score.totalQuestions,
            streakSummary = completionStreakSummary,
            completionJustRecorded = completionJustRecorded,
            reviewPack = completionReviewPack,
            voiceRecordingService = voiceRecordingService,
            onOpenReview = { completionReviewPack?.let(onReviewPack) },
            onReadAgain = onReadAgain,
            onDone = onDone,
        )
        return
    }

    val question = questionState.question

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FlowTopBar(
            title = stringResource(R.string.quiz_title),
            trailing = stringResource(
                R.string.quiz_progress_count,
                (questionState.questionIndex + 1).coerceAtMost(questionState.questionCount),
                questionState.questionCount,
            ),
            onBack = onBack,
        )
        LmcProgressBar(
            progress = questionState.progressFraction.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LmcSpacing.ScreenPadding),
        )
        if (question == null) {
            CenterStateMessage(text = stringResource(R.string.quiz_empty))
        } else {
            QuizQuestionBody(
                question = question,
                selectedAnswer = questionState.selectedAnswer,
                submitted = questionState.submitted,
                result = questionState.result,
                onSelectAnswer = { answer ->
                    quizState = quizSessionReducer.selectAnswer(story, quizState, answer)
                },
                modifier = Modifier.weight(1f),
            )
            BottomActionRow {
                Spacer(modifier = Modifier.weight(1f))
                val submitted = questionState.submitted
                Button(
                    enabled = questionState.canSubmitOrAdvance,
                    onClick = {
                        quizState = quizSessionReducer.submitOrAdvance(story, quizState)
                    },
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(
                        text = stringResource(
                            if (!submitted) {
                                R.string.action_submit
                            } else {
                                R.string.action_next
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentReportScreen(
    stories: List<Story>,
    progressStats: ProgressStats,
    parentReport: ParentProgressReport,
    parentReportSummary: ParentReportSummary,
    abilityMap: AbilityMap,
    progressRecords: List<CompletionRecord>,
    vocabRecords: List<com.littlemandarin.classics.shared.presentation.VocabSrsRecord>,
    streakSummary: StreakSummary,
    readingPositions: Map<String, Int>,
    storyPresentationUseCases: StoryPresentationUseCases,
    pendingReview: PendingReview?,
    voiceRecordings: List<VoiceRecording>,
    voiceRecordingService: VoiceRecordingService,
    parentGatePassed: Boolean,
    onParentGatePassed: () -> Unit,
    onStoryClick: (String) -> Unit,
    onReviewWords: () -> Unit,
    onOpenReview: () -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
) {
    val numberFormat = rememberNumberFormat()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voicePlaybackController = remember(context.applicationContext) {
        AndroidVoiceRecordingController(context)
    }
    var playingVoiceRecordingId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(voicePlaybackController) {
        onDispose { voicePlaybackController.release() }
    }
    val completedStoryIds = remember(progressRecords) {
        storyPresentationUseCases.completedStoryIds(progressRecords)
    }
    val weeklyPlan = remember(stories, progressRecords, vocabRecords, streakSummary) {
        ParentWeeklyPlanUseCases().buildWeeklyPlan(
            stories = stories,
            completionRecords = progressRecords,
            wordStates = vocabRecords.map { record ->
                WeeklyPlanWordState(
                    word = record.word,
                    pinyin = record.pinyin,
                    meaning = record.meaning,
                    intervalDays = record.intervalDays,
                    dueEpochDay = record.dueEpochDay,
                    lapses = record.lapses,
                )
            },
            todayEpochDay = (System.currentTimeMillis() / 86_400_000L).toInt(),
            nowEpochMillis = System.currentTimeMillis(),
            streakDays = streakSummary.currentStreakDays,
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = LmcSpacing.ScreenPadding,
            vertical = LmcSpacing.Space4,
        ),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
    ) {
        item {
            TopLevelHeader(
                title = stringResource(R.string.parent_title),
                actions = {
                    HeaderIconButton(
                        icon = LmcIcon.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        onClick = onSettings,
                    )
                },
            )
        }
        if (!parentGatePassed) {
            item {
                ParentGateCard(onPassed = onParentGatePassed)
            }
            item {
                PrivacyNotice(onPrivacy = onPrivacy)
            }
        } else {
            item {
                SectionTitle(text = stringResource(R.string.parent_this_week))
                Spacer(modifier = Modifier.height(LmcSpacing.Space2))
                MetricGrid(
                    metrics = listOf(
                        Metric(
                            label = stringResource(R.string.parent_stories_read),
                            value = numberFormat.format(parentReportSummary.storiesCompletedThisWeek),
                        ),
                        Metric(
                            label = stringResource(R.string.parent_reading_days),
                            value = numberFormat.format(parentReportSummary.readingDaysThisWeek),
                        ),
                        Metric(
                            label = stringResource(R.string.parent_quiz_correct),
                            value = stringResource(
                                R.string.parent_quiz_correct_count,
                                parentReportSummary.correctCount,
                                parentReportSummary.questionCount,
                            ),
                        ),
                        Metric(
                            label = stringResource(R.string.parent_words_reviewed),
                            value = numberFormat.format(parentReportSummary.vocabLearnedThisWeek),
                        ),
                    ),
                )
            }
            item {
                ParentPractisedSection(abilityMap = abilityMap)
            }
            item {
                ParentAdviceCard(
                    summary = parentReportSummary,
                    stories = stories,
                    onStoryClick = onStoryClick,
                    onReviewWords = onReviewWords,
                )
            }
            item {
                ParentWeeklyPlanSection(plan = weeklyPlan, stories = stories)
            }
            item {
                ParentVoiceRecordingsSection(
                    stories = stories,
                    recordings = voiceRecordings,
                    playingRecordingId = playingVoiceRecordingId,
                    onPlay = { recording ->
                        if (playingVoiceRecordingId == recording.id) {
                            voicePlaybackController.stopPlayback()
                            playingVoiceRecordingId = null
                        } else {
                            voicePlaybackController.play(
                                filePath = recording.filePath,
                                onCompletion = { playingVoiceRecordingId = null },
                                onError = { playingVoiceRecordingId = null },
                            )
                            playingVoiceRecordingId = recording.id
                        }
                    },
                    onDelete = { recording ->
                        if (playingVoiceRecordingId == recording.id) {
                            voicePlaybackController.stopPlayback()
                            playingVoiceRecordingId = null
                        }
                        scope.launch {
                            val result = voiceRecordingService.deleteById(recording.id)
                            cleanupRecordingFiles(result.removed)
                        }
                    },
                    onClearAll = {
                        voicePlaybackController.stopPlayback()
                        playingVoiceRecordingId = null
                        scope.launch {
                            val result = voiceRecordingService.clearAll()
                            cleanupRecordingFiles(result.removed)
                        }
                    },
                )
            }
            if (pendingReview != null) {
                item {
                    ReviewSectionCard(
                        modifier = Modifier.clickable(onClick = onOpenReview),
                        title = stringResource(R.string.parent_review_pack_title),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            text = reviewParentTipText(pendingReview.pack.parentTip),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
            item {
                SectionTitle(text = stringResource(R.string.parent_story_progress))
                Spacer(modifier = Modifier.height(LmcSpacing.Space2))
            }
            items(stories, key = { it.id }) { story ->
                StoryProgressRow(
                    story = story,
                    progress = storyPresentationUseCases.storyProgress(
                        story = story,
                        completedStoryIds = completedStoryIds,
                        savedParagraphIndex = readingPositions[story.id] ?: -1,
                    ).fraction.toFloat(),
                    completed = story.id in completedStoryIds,
                    onClick = { onStoryClick(story.id) },
                )
            }
            item {
                PrivacyNotice(onPrivacy = onPrivacy)
            }
        }
    }
}

@Composable
private fun ParentVoiceRecordingsSection(
    stories: List<Story>,
    recordings: List<VoiceRecording>,
    playingRecordingId: String?,
    onPlay: (VoiceRecording) -> Unit,
    onDelete: (VoiceRecording) -> Unit,
    onClearAll: () -> Unit,
) {
    val storyTitles = remember(stories) { stories.associate { it.id to it.titleZh } }
    ReviewSectionCard(
        title = stringResource(R.string.voice_parent_title),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3)) {
            Text(
                text = stringResource(R.string.voice_parent_retention),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (recordings.isEmpty()) {
                Text(
                    text = stringResource(R.string.voice_parent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(text = stringResource(R.string.action_clear_recordings))
                }
                recordings.forEach { recording ->
                    ParentVoiceRecordingRow(
                        recording = recording,
                        storyTitle = storyTitles[recording.storyId] ?: recording.storyId,
                        isPlaying = playingRecordingId == recording.id,
                        onPlay = { onPlay(recording) },
                        onDelete = { onDelete(recording) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentVoiceRecordingRow(
    recording: VoiceRecording,
    storyTitle: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusSm),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.Space3),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = storyTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (recording.paragraphIndex < 0) {
                    stringResource(
                        R.string.voice_recording_parent_retell_meta_format,
                        voiceRecordingDurationText(recording.durationMs),
                        relativeTimeText(recording.createdAtEpochMillis),
                    )
                } else {
                    stringResource(
                        R.string.voice_recording_parent_meta_format,
                        recording.paragraphIndex + 1,
                        voiceRecordingDurationText(recording.durationMs),
                        relativeTimeText(recording.createdAtEpochMillis),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPlay,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(
                        text = stringResource(
                            if (isPlaying) {
                                R.string.voice_recording_playing
                            } else {
                                R.string.action_play_recording
                            },
                        ),
                    )
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                ) {
                    Text(text = stringResource(R.string.action_delete_recording))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    appInfo: AppInfo,
    settings: ReaderSettings,
    feedbackService: FeedbackService,
    onLanguageChange: (ReaderLanguage) -> Unit,
    onPinyinDefaultChange: (Boolean) -> Unit,
    onTextSizeChange: (ReadingTextSize) -> Unit,
    onAiBackendBaseUrlChange: (String) -> Unit,
    onSfxEnabledChange: (Boolean) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    onSfxVolumePreview: (Float) -> Unit,
    onSfxPreview: () -> Unit,
    parentGatePassed: Boolean,
    onParentGatePassed: () -> Unit,
    onParentReport: () -> Unit,
    onAiConsole: () -> Unit,
    onRecheckLevel: () -> Unit,
    onPrivacy: () -> Unit,
) {
    var showFeedbackForm by remember { mutableStateOf(false) }
    var feedbackSaved by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = LmcSpacing.ScreenPadding,
            vertical = LmcSpacing.Space4,
        ),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
    ) {
        item {
            TopLevelHeader(title = stringResource(R.string.settings_title))
        }
        if (!parentGatePassed) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2)) {
                    SectionTitle(text = stringResource(R.string.settings_grown_ups))
                    ParentGateCard(onPassed = onParentGatePassed)
                }
            }
            item {
                PrivacyNotice(onPrivacy = onPrivacy)
            }
        } else {
            item {
                SettingsSection(title = stringResource(R.string.settings_language)) {
                    ReaderLanguage.entries.forEach { language ->
                        SelectableSettingsRow(
                            label = stringResource(language.labelRes()),
                            selected = settings.language == language,
                            onClick = { onLanguageChange(language) },
                        )
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_reading)) {
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_pinyin_default),
                        checked = settings.showPinyinByDefault,
                        onCheckedChange = onPinyinDefaultChange,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsTextSizeRow(
                        selectedTextSize = settings.readingTextSize,
                        onTextSizeChange = onTextSizeChange,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsValueRow(
                        label = stringResource(R.string.settings_audio_voice),
                        value = stringResource(R.string.settings_audio_system),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        label = stringResource(R.string.settings_sfx_enabled),
                        checked = settings.sfxSettings.enabled,
                        onCheckedChange = onSfxEnabledChange,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSliderRow(
                        label = stringResource(R.string.settings_sfx_volume),
                        value = settings.sfxSettings.volume,
                        enabled = settings.sfxSettings.enabled,
                        onValueChange = onSfxVolumeChange,
                        onValueChangeFinished = onSfxVolumePreview,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsButtonRow(
                        label = stringResource(R.string.settings_sfx_preview),
                        enabled = settings.sfxSettings.enabled,
                        onClick = onSfxPreview,
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_grown_ups)) {
                    SettingsNavigationRow(
                        label = stringResource(R.string.settings_parent_report),
                        onClick = onParentReport,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        label = stringResource(R.string.settings_ai_console),
                        onClick = onAiConsole,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        label = stringResource(R.string.settings_recheck_level),
                        onClick = onRecheckLevel,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        label = stringResource(R.string.settings_privacy),
                        onClick = onPrivacy,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        label = stringResource(R.string.settings_feedback),
                        onClick = {
                            feedbackSaved = false
                            showFeedbackForm = true
                        },
                    )
                }
            }
            if (showFeedbackForm) {
                item {
                    FeedbackForm(
                        feedbackService = feedbackService,
                        onSaved = {
                            feedbackSaved = true
                            showFeedbackForm = false
                        },
                    )
                }
            }
            if (feedbackSaved) {
                item {
                    Text(
                        text = stringResource(R.string.settings_feedback_saved),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LmcColors.Success,
                        modifier = Modifier.padding(horizontal = LmcSpacing.Space2),
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.settings_developer)) {
                    AiBackendBaseUrlRow(
                        value = settings.aiBackendBaseUrl,
                        onValueChange = onAiBackendBaseUrlChange,
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(
                    R.string.settings_app_version,
                    stringResource(appInfo.nameResourceKey.toStringResourceId()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReaderBottomNavigation(
    currentRoute: String?,
    onDestinationClick: (TopLevelDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        TopLevelDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationClick(destination) },
                icon = {
                    LmcCanvasIcon(
                        icon = destination.icon,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun TopLevelHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.TopAppBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        actions()
    }
}

@Composable
private fun FlowTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.TopAppBarHeight)
            .padding(horizontal = LmcSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
    ) {
        HeaderIconButton(
            icon = LmcIcon.Back,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReadingTopBar(
    story: Story,
    onClose: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LmcSpacing.TopAppBarHeight)
                .padding(horizontal = LmcSpacing.Space2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            HeaderIconButton(
                icon = LmcIcon.Close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onClose,
            )
            Text(
                text = story.titleZh,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HeaderIconButton(
                icon = LmcIcon.Settings,
                contentDescription = stringResource(R.string.settings_title),
                onClick = onSettingsClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReadingSettingsSheet(
    showPinyin: Boolean,
    readingTextSize: ReadingTextSize,
    playbackMode: ReadingSessionMode,
    autoContinue: Boolean,
    playbackSpeed: ReadingPlaybackSpeed,
    onPinyinChange: (Boolean) -> Unit,
    onTextSizeChange: (ReadingTextSize) -> Unit,
    onPlaybackModeChange: (ReadingSessionMode) -> Unit,
    onAutoContinueChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (ReadingPlaybackSpeed) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val autoContinueEnabled = playbackMode != ReadingSessionMode.TapToListen
    val speedLabel = stringResource(
        R.string.reading_speed_value,
        stringResource(R.string.reading_playback_speed),
        stringResource(playbackSpeed.labelRes()),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LmcSpacing.Space3)
                .padding(bottom = LmcSpacing.Space8),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            Text(
                text = stringResource(R.string.reading_settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.reading_pinyin),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Switch(
                    checked = showPinyin,
                    onCheckedChange = onPinyinChange,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2)) {
                Text(
                    text = stringResource(R.string.reading_text_size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextSizeChips(
                    selectedTextSize = readingTextSize,
                    onTextSizeChange = onTextSizeChange,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2)) {
                Text(
                    text = stringResource(R.string.reading_audio),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                ) {
                    FilterChip(
                        selected = playbackMode == ReadingSessionMode.ReadAlong,
                        onClick = { onPlaybackModeChange(ReadingSessionMode.ReadAlong) },
                        label = { Text(text = stringResource(R.string.reading_mode_read_along)) },
                        modifier = Modifier.heightIn(min = LmcSpacing.ChipHeight),
                        colors = lmcFilterChipColors(),
                    )
                    FilterChip(
                        selected = playbackMode == ReadingSessionMode.TapToListen,
                        onClick = { onPlaybackModeChange(ReadingSessionMode.TapToListen) },
                        label = { Text(text = stringResource(R.string.reading_mode_tap_to_listen)) },
                        modifier = Modifier.heightIn(min = LmcSpacing.ChipHeight),
                        colors = lmcFilterChipColors(),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.reading_auto_continue),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Switch(
                    checked = autoContinue,
                    onCheckedChange = onAutoContinueChange,
                    enabled = autoContinueEnabled,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.reading_playback_speed),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box {
                    OutlinedButton(
                        onClick = { speedMenuExpanded = true },
                        modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                    ) {
                        Text(text = speedLabel)
                    }
                    DropdownMenu(
                        expanded = speedMenuExpanded,
                        onDismissRequest = { speedMenuExpanded = false },
                    ) {
                        ReadingPlaybackSpeed.entries.forEach { speed ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(speed.labelRes())) },
                                onClick = {
                                    speedMenuExpanded = false
                                    onPlaybackSpeedChange(speed)
                                },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.action_done))
                }
            }
        }
    }
}

@Composable
private fun ReadingBottomDock(
    playbackStatus: SentencePlaybackStatus,
    playbackState: SentencePlaybackUiState,
    onReadClick: () -> Unit,
    canGoPreviousSentence: Boolean,
    canGoNextSentence: Boolean,
    onPreviousSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onRepeatSentence: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onReturnToReadingPlace: () -> Unit,
    showReturnToReadingPlace: Boolean,
) {
    if (playbackStatus == SentencePlaybackStatus.Stopped) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LmcSpacing.BottomActionHeight)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = LmcSpacing.ScreenPadding, vertical = LmcSpacing.Space2),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onReadClick,
                enabled = playbackState.sentenceCountInParagraph > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                LmcCanvasIcon(
                    icon = LmcIcon.Audio,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(LmcSpacing.Space2))
                Text(text = stringResource(R.string.reading_read_all))
            }
        }
    } else {
        ReadingPlayerBar(
            playbackState = playbackState,
            canGoPreviousSentence = canGoPreviousSentence,
            canGoNextSentence = canGoNextSentence,
            onPreviousSentence = onPreviousSentence,
            onNextSentence = onNextSentence,
            onRepeatSentence = onRepeatSentence,
            onPlayPause = onPlayPause,
            onStop = onStop,
            onReturnToReadingPlace = onReturnToReadingPlace,
            showReturnToReadingPlace = showReturnToReadingPlace,
        )
    }
}

@Composable
private fun ReadingPlayerBar(
    playbackState: SentencePlaybackUiState,
    canGoPreviousSentence: Boolean,
    canGoNextSentence: Boolean,
    onPreviousSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onRepeatSentence: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onReturnToReadingPlace: () -> Unit,
    showReturnToReadingPlace: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = LmcSpacing.ScreenPadding, vertical = LmcSpacing.Space2),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space1),
    ) {
        val statusText = stringResource(
            when (playbackState.status) {
                SentencePlaybackStatus.Playing -> R.string.reading_audio_playing
                SentencePlaybackStatus.Paused -> R.string.reading_audio_paused
                SentencePlaybackStatus.Stopped -> R.string.reading_audio_stopped
            },
        )
        val audioSourceText = when (playbackState.audioSource) {
            ReadingAudioSource.Recorded -> stringResource(R.string.reading_audio_source_recorded)
            ReadingAudioSource.Tts -> stringResource(R.string.reading_audio_source_tts)
            null -> null
        }
        val sentenceProgress = stringResource(
            R.string.reading_sentence_progress,
            (playbackState.sentenceIndex + 1).coerceAtLeast(1),
            playbackState.sentenceCountInParagraph.coerceAtLeast(1),
        )
        Text(
            text = if (audioSourceText == null) {
                "$sentenceProgress · $statusText"
            } else {
                "$sentenceProgress · $audioSourceText · $statusText"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showReturnToReadingPlace) {
            Button(
                onClick = onReturnToReadingPlace,
                modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                contentPadding = PaddingValues(
                    horizontal = LmcSpacing.Space3,
                ),
            ) {
                Text(
                    text = stringResource(R.string.reading_back_to_reading_place),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ReadingActionIconButton(
                icon = LmcIcon.Previous,
                contentDescription = stringResource(R.string.reading_previous_sentence),
                onClick = onPreviousSentence,
                enabled = canGoPreviousSentence,
            )
            ReadingActionIconButton(
                icon = if (playbackState.status == SentencePlaybackStatus.Playing) {
                    LmcIcon.PauseAudio
                } else {
                    LmcIcon.Audio
                },
                contentDescription = if (playbackState.status == SentencePlaybackStatus.Playing) {
                    stringResource(R.string.action_pause_audio)
                } else {
                    stringResource(R.string.action_resume_audio)
                },
                onClick = onPlayPause,
                isPrimary = true,
            )
            ReadingActionIconButton(
                icon = LmcIcon.Repeat,
                contentDescription = stringResource(R.string.reading_repeat_sentence),
                onClick = onRepeatSentence,
                enabled = playbackState.sentenceCountInParagraph > 0,
            )
            ReadingActionIconButton(
                icon = LmcIcon.Next,
                contentDescription = stringResource(R.string.reading_next_sentence),
                onClick = onNextSentence,
                enabled = canGoNextSentence,
            )
            ReadingActionIconButton(
                icon = LmcIcon.StopAudio,
                contentDescription = stringResource(R.string.action_stop_audio),
                onClick = onStop,
                enabled = playbackState.sentenceCountInParagraph > 0,
            )
        }
    }
}

@Composable
private fun ReadingActionIconButton(
    icon: LmcIcon,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
) {
    if (isPrimary) {
        Button(
            enabled = enabled,
            onClick = onClick,
            modifier = Modifier
                .size(LmcSpacing.MinTouchTarget + LmcSpacing.Space2)
                .semantics {
                    this.contentDescription = contentDescription
                },
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            LmcCanvasIcon(
                icon = icon,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    } else {
        IconButton(
            enabled = enabled,
            onClick = onClick,
            modifier = Modifier
                .size(LmcSpacing.MinTouchTarget)
                .semantics {
                    this.contentDescription = contentDescription
                },
        ) {
            LmcCanvasIcon(
                icon = icon,
                color = if (enabled) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun ReadingAudioCoachmark(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = stringResource(R.string.reading_coachmark_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.reading_coachmark_sentence),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = LmcSpacing.MinTouchTarget),
            ) {
                Text(text = stringResource(R.string.action_done))
            }
        }
    }
}

@Composable
private fun StoryHeroCard(
    story: Story,
    completed: Boolean,
    progress: Float,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = LmcSpacing.CardElevation),
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoryCover(
                    story = story,
                    coverSize = LmcSpacing.StoryCoverHero,
                )
                StoryCardText(
                    story = story,
                    completed = completed,
                    modifier = Modifier.weight(1f),
                )
            }
            LmcProgressBar(progress = progress)
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                LmcCanvasIcon(
                    icon = LmcIcon.Book,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(LmcSpacing.Space2))
                Text(
                    text = stringResource(
                        if (progress > 0f && !completed) {
                            R.string.action_continue
                        } else {
                            R.string.action_start_reading
                        },
                    ),
                )
            }
        }
    }
}

@Composable
private fun StoryListCard(
    card: LibraryStoryCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val story = card.story
    val progress = card.progress.fraction.toFloat()
    val statusText = stringResource(card.status.labelRes())
    val ctaText = stringResource(card.action.labelRes())
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = LmcSpacing.CardElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(LmcSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoryCover(
                story = story,
                coverSize = LmcSpacing.StoryCoverList,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
            ) {
                Text(
                    text = stringResource(
                        R.string.library_story_order_series,
                        card.sequenceNumber,
                        stringResource(card.seriesKey.seriesLabelRes()),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                StoryCardText(
                    story = story,
                    completed = card.status == LibraryStoryStatus.Done,
                    showMeta = false,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StoryMetaChip(text = statusText, selected = card.status == LibraryStoryStatus.Done)
                    StoryMetaChip(text = stringResource(R.string.story_level, story.level))
                }
                LmcProgressBar(progress = progress)
                Text(
                    text = storyProgressLabel(progress = progress, completed = card.status == LibraryStoryStatus.Done),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                ) {
                    Text(text = ctaText)
                }
            }
        }
    }
}

@Composable
private fun StoryCompactRow(
    story: Story,
    progress: Float,
    completed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        tonalElevation = 0.dp,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = story.titleZh,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = story.titleEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LmcProgressBar(progress = progress)
            Text(
                text = storyProgressLabel(progress = progress, completed = completed),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StoryCardText(
    story: Story,
    completed: Boolean,
    modifier: Modifier = Modifier,
    showMeta: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
    ) {
        Text(
            text = story.titleZh,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = story.titleEn,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (showMeta) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoryMetaChip(text = stringResource(R.string.story_level, story.level))
                StoryMetaChip(text = story.ageRange)
                if (completed) {
                    StoryMetaChip(
                        text = stringResource(R.string.library_completed),
                        selected = true,
                    )
                }
            }
        }
    }
}

/**
 * A warm, kid-friendly storybook palette for a programmatic cover. The gradient is the
 * background; [ink] is used for the prominent Chinese title; [subtitle] for the English line;
 * [badge]/[onBadge] for the small level pill. All pairings are chosen for legible contrast on
 * the gradient's lighter (top-leading) tones. Swap to a real image later by replacing the
 * gradient layer only.
 */
private data class StoryCoverPalette(
    val gradient: List<Color>,
    val ink: Color,
    val subtitle: Color,
    val badge: Color,
    val onBadge: Color,
)

// 6 fixed palettes drawn from the LMC design tokens (vermilion / teal / leaf-gold / rice).
// paletteIndex (0..5) maps 1:1 here, so the same story always gets the same palette.
private val LmcStoryCoverPalettes: List<StoryCoverPalette> = listOf(
    // 0 — warm vermilion
    StoryCoverPalette(
        gradient = listOf(Color(0xFFFFE0D6), Color(0xFFFFC4B2)),
        ink = Color(0xFF3D0E08),
        subtitle = Color(0xFF6E2417),
        badge = Color(0xFFB84535),
        onBadge = Color.White,
    ),
    // 1 — calm teal
    StoryCoverPalette(
        gradient = listOf(Color(0xFFD9F1EE), Color(0xFFB6E3DE)),
        ink = Color(0xFF063432),
        subtitle = Color(0xFF14534F),
        badge = Color(0xFF126B68),
        onBadge = Color.White,
    ),
    // 2 — warm gold
    StoryCoverPalette(
        gradient = listOf(Color(0xFFFFF4D8), Color(0xFFFFE3A8)),
        ink = Color(0xFF5A4000),
        subtitle = Color(0xFF7A5800),
        badge = Color(0xFF8A6100),
        onBadge = Color.White,
    ),
    // 3 — leaf green
    StoryCoverPalette(
        gradient = listOf(Color(0xFFE1F3DC), Color(0xFFC2E6BC)),
        ink = Color(0xFF1E3D1E),
        subtitle = Color(0xFF2E5A2E),
        badge = Color(0xFF3B7A3B),
        onBadge = Color.White,
    ),
    // 4 — soft sky
    StoryCoverPalette(
        gradient = listOf(Color(0xFFDCEEFF), Color(0xFFB9DCF7)),
        ink = Color(0xFF0F2E48),
        subtitle = Color(0xFF1E4E78),
        badge = Color(0xFF2B6CA3),
        onBadge = Color.White,
    ),
    // 5 — rice paper / ink
    StoryCoverPalette(
        gradient = listOf(Color(0xFFFFF8EC), Color(0xFFF1E4C9)),
        ink = Color(0xFF202523),
        subtitle = Color(0xFF4F5E58),
        badge = Color(0xFF8A6100),
        onBadge = Color.White,
    ),
)

private val storyCoverUseCases = StoryCoverUseCases()

@Composable
private fun StoryCover(
    story: Story,
    coverSize: Dp,
) {
    val theme: StoryCoverTheme = remember(story.id, story.level) {
        storyCoverUseCases.coverThemeFor(story, paletteCount = LmcStoryCoverPalettes.size)
    }
    val palette = LmcStoryCoverPalettes[theme.paletteIndex.coerceIn(0, LmcStoryCoverPalettes.lastIndex)]
    val isHero = coverSize >= LmcSpacing.StoryCoverHero
    val coverDescription = stringResource(
        R.string.story_cover_content_description,
        story.titleZh,
        story.titleEn,
    )

    // Scale typography with the cover footprint so it reads well at both hero and list sizes.
    val motifSize = if (isHero) 40.sp else 26.sp
    val titleSize = if (isHero) 24.sp else 16.sp
    val titleLineHeight = if (isHero) 30.sp else 20.sp
    val subtitleSize = if (isHero) 12.sp else 10.sp
    val badgeSize = if (isHero) 12.sp else 10.sp
    val pad = if (isHero) LmcSpacing.Space3 else LmcSpacing.Space2

    Box(
        modifier = Modifier
            .size(coverSize)
            .clip(RoundedCornerShape(LmcSpacing.RadiusSm))
            .background(
                Brush.linearGradient(colors = palette.gradient),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(LmcSpacing.RadiusSm),
            )
            .semantics {
                contentDescription = coverDescription
            },
    ) {
        // Subtle storybook brush strokes behind the content.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = palette.ink.copy(alpha = 0.10f),
                start = Offset(size.width * 0.12f, size.height * 0.22f),
                end = Offset(size.width * 0.88f, size.height * 0.16f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = palette.ink.copy(alpha = 0.08f),
                start = Offset(size.width * 0.14f, size.height * 0.86f),
                end = Offset(size.width * 0.86f, size.height * 0.80f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        // Level badge, top-start.
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(pad),
            color = palette.badge,
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = stringResource(R.string.story_level, theme.level),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = badgeSize),
                color = palette.onBadge,
                modifier = Modifier.padding(horizontal = LmcSpacing.Space2, vertical = 2.dp),
            )
        }
        // Motif glyph, top-end.
        Text(
            text = theme.motif,
            fontSize = motifSize,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(pad),
        )
        // Storybook title + English subtitle, bottom.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(pad),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = story.titleZh,
                fontSize = titleSize,
                lineHeight = titleLineHeight,
                fontWeight = FontWeight.Bold,
                color = palette.ink,
                maxLines = if (isHero) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (isHero) {
                Text(
                    text = story.titleEn,
                    fontSize = subtitleSize,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.subtitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StoryMetaChip(
    text: String,
    selected: Boolean = false,
) {
    Surface(
        color = if (selected) {
            LmcColors.SuccessContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        contentColor = if (selected) {
            LmcColors.Success
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        },
        shape = CircleShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(
                horizontal = LmcSpacing.Space3,
                vertical = LmcSpacing.Space1,
            ),
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressSummaryBanner(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            LmcCanvasIcon(
                icon = LmcIcon.Check,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun AbilityDimension.labelRes(): Int = when (this) {
    AbilityDimension.CharacterRecognition -> R.string.ability_dim_character_recognition
    AbilityDimension.WordMeaning -> R.string.ability_dim_word_meaning
    AbilityDimension.SentenceReading -> R.string.ability_dim_sentence_reading
    AbilityDimension.Listening -> R.string.ability_dim_listening
    AbilityDimension.Comprehension -> R.string.ability_dim_comprehension
    AbilityDimension.Retelling -> R.string.ability_dim_retelling
    AbilityDimension.Culture -> R.string.ability_dim_culture
}

@Composable
private fun AbilityMapTodayCard(
    abilityMap: AbilityMap,
    onClick: () -> Unit,
) {
    val practicedDimensions = abilityMap.dimensions.count { it.practicedStories > 0 }
    val totalDimensions = abilityMap.dimensions.size
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            LmcCanvasIcon(
                icon = LmcIcon.Book,
                color = MaterialTheme.colorScheme.secondary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space1),
            ) {
                Text(
                    text = stringResource(R.string.ability_today_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.ability_today_card_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(
                        R.string.ability_progress_fraction,
                        practicedDimensions,
                        totalDimensions,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.ability_today_card_action),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun AbilityMapScreen(
    abilityMap: AbilityMap,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = LmcSpacing.ScreenPadding,
            vertical = LmcSpacing.Space4,
        ),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
    ) {
        item {
            FlowTopBar(
                title = stringResource(R.string.ability_title),
                onBack = onBack,
            )
        }
        item {
            Text(
                text = stringResource(R.string.ability_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(abilityMap.dimensions, key = { it.dimension.name }) { progress ->
            AbilityDimensionCard(
                progress = progress,
                practicedNow = progress.dimension in abilityMap.recentlyPracticed,
            )
        }
        item {
            ProgressSummaryBanner(text = stringResource(R.string.ability_encourage))
        }
    }
}

@Composable
private fun AbilityDimensionCard(
    progress: AbilityProgress,
    practicedNow: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        color = if (practicedNow) LmcColors.SuccessContainer else MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(progress.dimension.labelRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.ability_progress_fraction,
                        progress.practicedStories,
                        progress.totalStories,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LmcProgressBar(progress = progress.masteryFraction.toFloat())
        }
    }
}

@Composable
private fun ParentPractisedSection(
    abilityMap: AbilityMap,
) {
    val separator = stringResource(R.string.parent_practised_separator)
    val practisedLabels = AbilityDimension.entries
        .filter { it in abilityMap.recentlyPracticed }
        .map { stringResource(it.labelRes()) }
    val accuracyText = abilityMap.comprehensionAccuracy?.let { formatPercent(it.toFloat()) }
    if (practisedLabels.isEmpty() && accuracyText == null) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = stringResource(R.string.parent_practised_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (practisedLabels.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.parent_practised_format,
                        practisedLabels.joinToString(separator),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (accuracyText != null) {
                Text(
                    text = stringResource(
                        R.string.parent_comprehension_accuracy_format,
                        accuracyText,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun DailyGoalStreakCard(
    streakSummary: StreakSummary,
) {
    val numberFormat = rememberNumberFormat()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = if (streakSummary.todayGoalMet) {
            LmcColors.SuccessContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LmcCanvasIcon(
                        icon = LmcIcon.Check,
                        color = if (streakSummary.todayGoalMet) {
                            LmcColors.Success
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space1),
            ) {
                Text(
                    text = stringResource(
                        R.string.streak_day_count,
                        streakSummary.currentStreakDays.coerceAtLeast(1),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.streak_progress_format,
                        numberFormat.format(streakSummary.todayCompletedStories),
                        numberFormat.format(streakSummary.dailyGoalStories),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(
                    if (streakSummary.todayGoalMet) {
                        R.string.streak_goal_complete
                    } else {
                        R.string.streak_goal_continue
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (streakSummary.todayGoalMet) {
                    LmcColors.Success
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun LevelFilterRow(
    selectedLevel: Int?,
    availableLevels: List<Int>,
    onSelectedLevelChange: (Int?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
    ) {
        FilterChip(
            selected = selectedLevel == null,
            onClick = { onSelectedLevelChange(null) },
            label = { Text(text = stringResource(R.string.library_filter_all)) },
            modifier = Modifier.heightIn(min = LmcSpacing.ChipHeight),
            colors = lmcFilterChipColors(),
        )
        availableLevels.forEach { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = { onSelectedLevelChange(level) },
                label = { Text(text = stringResource(R.string.library_filter_level, level)) },
                modifier = Modifier.heightIn(min = LmcSpacing.ChipHeight),
                colors = lmcFilterChipColors(),
            )
        }
    }
}

@Composable
private fun PinyinTextBlock(
    paragraph: Paragraph,
    showPinyin: Boolean,
    readingType: ReadingTypeStyles,
    onWordTap: (String) -> Unit,
) {
    SentenceSurface(isHighlighted = false) {
        if (showPinyin && paragraph.cells.isNotEmpty()) {
            RubyFlowText(
                text = paragraph.text,
                cells = paragraph.cells,
                readingType = readingType,
                isHighlighted = false,
                onWordTap = onWordTap,
            )
        } else {
            TappableHanziText(
                text = paragraph.text,
                style = readingType.hanzi,
                isHighlighted = false,
                activeCharIndex = null,
                onWordTap = onWordTap,
            )
        }
    }
}

@Composable
private fun SentenceTextBlock(
    paragraph: Paragraph,
    sentence: SentenceSegment,
    showPinyin: Boolean,
    readingType: ReadingTypeStyles,
    isHighlighted: Boolean,
    activeCharIndex: Int?,
    sentenceIndex: Int,
    sentenceCount: Int,
    onSentenceClick: () -> Unit,
    onSpeakerClick: () -> Unit,
    onWordTap: (String) -> Unit,
) {
    val sentenceCells = remember(paragraph.cells, sentence.startOffset, sentence.endOffset) {
        paragraph.cellsFor(sentence)
    }
    val speakerDescription = stringResource(
        R.string.reading_sentence_speaker,
        sentenceIndex + 1,
        sentenceCount.coerceAtLeast(1),
    )

    SentenceSurface(
        isHighlighted = isHighlighted,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSentenceClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = sentenceSpeakerTouchTargetSize()),
            ) {
                if (showPinyin && sentenceCells.isNotEmpty()) {
                    RubyFlowText(
                        text = sentence.text,
                        cells = sentenceCells,
                        readingType = readingType,
                        isHighlighted = isHighlighted,
                        activeCharIndex = activeCharIndex,
                        onWordTap = onWordTap,
                    )
                } else {
                    TappableHanziText(
                        text = sentence.text,
                        style = readingType.hanzi,
                        isHighlighted = isHighlighted,
                        activeCharIndex = activeCharIndex,
                        onWordTap = onWordTap,
                    )
                }
            }
            SentenceSpeakerButton(
                contentDescription = speakerDescription,
                onClick = onSpeakerClick,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun SentenceSpeakerButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(sentenceSpeakerTouchTargetSize())
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(sentenceSpeakerVisualSize())
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            LmcCanvasIcon(
                icon = LmcIcon.Audio,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(LmcSpacing.SentenceSpeakerIconSize),
            )
        }
    }
}

@Composable
private fun SentenceSurface(
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.ReadingPanelRadius),
        color = if (isHighlighted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.ReadingPanelPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
            content = content,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RubyFlowText(
    text: String,
    cells: List<PinyinCell>,
    readingType: ReadingTypeStyles,
    isHighlighted: Boolean,
    activeCharIndex: Int? = null,
    onWordTap: ((String) -> Unit)? = null,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = text
            },
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
    ) {
        cells.forEachIndexed { index, cell ->
            RubyCell(
                hanzi = cell.c,
                pinyin = cell.p,
                readingType = readingType,
                isHighlighted = isHighlighted,
                // cells are 1:1 with the sentence code points, so the cell index
                // matches the karaoke character index from shared.
                isActiveChar = isHighlighted && activeCharIndex == index,
                onTap = onWordTap?.takeIf { cell.c.isNotBlank() }?.let { tap ->
                    { tap(cell.c) }
                },
            )
        }
    }
}

@Composable
private fun RubyCell(
    hanzi: String,
    pinyin: String,
    readingType: ReadingTypeStyles,
    isHighlighted: Boolean,
    isActiveChar: Boolean = false,
    onTap: (() -> Unit)? = null,
) {
    val activeCharColor = MaterialTheme.colorScheme.onPrimaryContainer
    val cellBackgroundColor = if (isActiveChar) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val baseModifier =
        Modifier
            .background(
                color = cellBackgroundColor,
                shape = RoundedCornerShape(LmcSpacing.RadiusSm),
            )
            .padding(horizontal = karaokeCellHorizontalPadding(isActiveChar))
    // Per-character tap opens the word lookup WITHOUT starting sentence audio:
    // the cell's clickable consumes the tap before the sentence surface sees it.
    // >=48dp tap target via heightIn on the cell.
    val tapModifier = if (onTap != null) {
        Modifier
            .clip(RoundedCornerShape(LmcSpacing.RadiusSm))
            .clickable(onClick = onTap)
            .heightIn(min = LmcSpacing.MinTouchTarget)
            .widthIn(min = LmcSpacing.MinTouchTarget)
    } else {
        Modifier
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = baseModifier.then(tapModifier),
    ) {
        val hasPinyin = pinyin.isNotBlank()
        Text(
            text = if (hasPinyin) pinyin else " ",
            style = readingType.pinyin,
            color = if (hasPinyin) {
                when {
                    isActiveChar -> activeCharColor
                    isHighlighted -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            } else {
                Color.Transparent
            },
            fontWeight = karaokeTextFontWeight(readingType.pinyin, isActiveChar),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
        Text(
            text = hanzi,
            style = readingType.hanzi,
            color = when {
                isActiveChar -> activeCharColor
                isHighlighted -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = karaokeTextFontWeight(readingType.hanzi, isActiveChar),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Sentence hanzi without ruby, with optional per-character karaoke highlight.
 * [activeCharIndex] is a Unicode code-point index into [text].
 */
@Composable
private fun KaraokeHanziText(
    text: String,
    style: TextStyle,
    isHighlighted: Boolean,
    activeCharIndex: Int?,
) {
    val baseColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val activeColor = MaterialTheme.colorScheme.primary

    if (activeCharIndex == null) {
        Text(text = text, style = style, color = baseColor)
        return
    }

    val annotated = buildAnnotatedString {
        var codePointIndex = 0
        var offset = 0
        while (offset < text.length) {
            val isSurrogatePair = text[offset].isHighSurrogate() &&
                offset + 1 < text.length &&
                text[offset + 1].isLowSurrogate()
            val charLength = if (isSurrogatePair) 2 else 1
            val chunk = text.substring(offset, offset + charLength)
            if (codePointIndex == activeCharIndex) {
                withStyle(SpanStyle(color = activeColor, fontWeight = karaokeTextFontWeight(style, isActiveChar = true))) {
                    append(chunk)
                }
            } else {
                append(chunk)
            }
            offset += charLength
            codePointIndex += 1
        }
    }
    Text(text = annotated, style = style, color = baseColor)
}

/**
 * Hanzi without ruby, where every character is an individual tap target that opens the
 * word lookup. Karaoke highlight is preserved via [activeCharIndex] (code-point index).
 * Each character's tap is consumed by its own cell so sentence audio is NOT triggered.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TappableHanziText(
    text: String,
    style: TextStyle,
    isHighlighted: Boolean,
    activeCharIndex: Int?,
    onWordTap: (String) -> Unit,
) {
    val baseColor = if (isHighlighted) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val activeColor = MaterialTheme.colorScheme.primary
    val codePoints = remember(text) {
        buildList {
            var offset = 0
            while (offset < text.length) {
                val isSurrogatePair = text[offset].isHighSurrogate() &&
                    offset + 1 < text.length &&
                    text[offset + 1].isLowSurrogate()
                val charLength = if (isSurrogatePair) 2 else 1
                add(text.substring(offset, offset + charLength))
                offset += charLength
            }
        }
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = text },
        horizontalArrangement = Arrangement.Start,
    ) {
        codePoints.forEachIndexed { index, chunk ->
            val isActive = isHighlighted && activeCharIndex == index
            val tappable = chunk.isNotBlank()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(LmcSpacing.RadiusSm))
                    .then(
                        if (tappable) {
                            Modifier.clickable { onWordTap(chunk) }
                        } else {
                            Modifier
                        },
                    )
                    .heightIn(min = LmcSpacing.MinTouchTarget),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chunk,
                    style = style,
                    color = if (isActive) activeColor else baseColor,
                    fontWeight = karaokeTextFontWeight(style, isActive),
                )
            }
        }
    }
}

/**
 * Tap-word lookup sheet. Shows a trusted curated entry (from the story's reviewed vocab),
 * or, for a [WordLookupResult.NeedsAi] token, a single controlled "Explain with AI" action
 * whose answer is clearly badged as AI. No free-text input, no open chat (AGENTS.md §7).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordLookupSheet(
    lookup: WordLookupResult,
    aiState: AiUiState,
    onExplainWithAi: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LmcSpacing.ScreenPadding)
                .padding(bottom = LmcSpacing.Space6),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            when (lookup) {
                is WordLookupResult.Curated -> {
                    Text(
                        text = lookup.word,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (lookup.pinyin.isNotBlank()) {
                        Text(
                            text = lookup.pinyin,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    LmcBadge(text = stringResource(R.string.word_lookup_curated_source))
                    Text(
                        text = lookup.meaning,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    lookup.example?.takeIf { it.isNotBlank() }?.let { example ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = stringResource(R.string.word_lookup_example_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is WordLookupResult.NeedsAi -> {
                    Text(
                        text = lookup.token,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.word_lookup_no_entry),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onExplainWithAi,
                        enabled = aiState !is AiUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = LmcSpacing.MinTouchTarget),
                    ) {
                        Text(text = stringResource(R.string.word_lookup_explain_button))
                    }
                    when (val state = aiState) {
                        AiUiState.Idle -> Unit
                        AiUiState.Loading -> Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                text = stringResource(R.string.word_lookup_ai_loading),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        is AiUiState.Answer -> Column(
                            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                        ) {
                            LmcBadge(text = stringResource(R.string.word_lookup_ai_badge))
                            Text(
                                text = state.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.word_lookup_ai_boundary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AiUiState.Error -> Text(
                            text = stringResource(R.string.word_lookup_ai_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LmcBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(LmcSpacing.RadiusSm),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(
                horizontal = LmcSpacing.Space2,
                vertical = LmcSpacing.Space1,
            ),
        )
    }
}

@Composable
private fun AskExplanationCard(
    state: AiUiState,
    enabled: Boolean,
    onAsk: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
            ) {
                Text(
                    text = stringResource(R.string.reading_ask_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    enabled = enabled && state !is AiUiState.Loading,
                    onClick = onAsk,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                ) {
                    Text(text = stringResource(R.string.reading_ask_button))
                }
            }
            Text(
                text = stringResource(R.string.reading_ask_boundary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (state) {
                AiUiState.Idle -> Unit
                AiUiState.Loading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = stringResource(R.string.reading_ask_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is AiUiState.Answer -> Text(
                    text = state.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                AiUiState.Error -> Text(
                    text = stringResource(R.string.reading_ask_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun VocabularyCard(
    word: Vocab,
    onAudioClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = LmcSpacing.CardElevation),
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = word.pinyin,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                HeaderIconButton(
                    icon = LmcIcon.Audio,
                    contentDescription = stringResource(R.string.action_play_audio),
                    onClick = onAudioClick,
                )
            }
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            word.example?.let { example ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = example,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WordBookFilterRow(
    selectedFilter: WordBookFilter,
    onSelectedFilterChange: (WordBookFilter) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
    ) {
        WordBookFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onSelectedFilterChange(filter) },
                label = { Text(text = stringResource(filter.labelRes())) },
                modifier = Modifier.heightIn(min = LmcSpacing.ChipHeight),
                colors = lmcFilterChipColors(),
            )
        }
    }
}

@Composable
private fun LearnedWordRow(
    item: WordBookItem,
    sourceTitle: String?,
    onAudioClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Row(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space1),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                ) {
                    Text(
                        text = item.word,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    WordStatusChip(item = item)
                }
                Text(
                    text = item.pinyin,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = item.meaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (sourceTitle != null) {
                    Text(
                        text = stringResource(R.string.word_book_source_story, sourceTitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HeaderIconButton(
                icon = LmcIcon.Audio,
                contentDescription = stringResource(R.string.action_play_audio),
                onClick = onAudioClick,
            )
        }
    }
}

@Composable
private fun ReviewWordCard(
    item: WordBookItem,
    answerRevealed: Boolean,
    onReveal: () -> Unit,
    onAudioClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !answerRevealed, onClick = onReveal),
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = LmcSpacing.CardElevation),
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            Text(
                text = item.word,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            HeaderIconButton(
                icon = LmcIcon.Audio,
                contentDescription = stringResource(R.string.action_play_audio),
                onClick = onAudioClick,
            )
            if (answerRevealed) {
                Text(
                    text = item.pinyin,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = item.meaning,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                item.example?.let { example ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(LmcSpacing.RadiusMd),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(LmcSpacing.CardPadding),
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.word_review_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun WordReviewCompleteContent(
    reviewedCount: Int,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = LmcSpacing.ReadingMaxWidth)
            .padding(LmcSpacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
    ) {
        Spacer(modifier = Modifier.height(LmcSpacing.Space8))
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = LmcColors.SuccessContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                LmcCanvasIcon(
                    icon = LmcIcon.Check,
                    color = LmcColors.Success,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.word_review_complete_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.word_review_complete_body, reviewedCount),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onDone,
            modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
            shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        ) {
            Text(text = stringResource(R.string.action_done))
        }
    }
}

@Composable
private fun WordBookEmptyState(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
private fun WordStatusChip(item: WordBookItem) {
    val labelRes = when {
        item.due -> R.string.word_book_status_due
        item.reps >= 2 -> R.string.word_book_status_known
        else -> R.string.word_book_status_learning
    }
    val containerColor = when {
        item.due -> MaterialTheme.colorScheme.primaryContainer
        item.reps >= 2 -> LmcColors.SuccessContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when {
        item.due -> MaterialTheme.colorScheme.onPrimaryContainer
        item.reps >= 2 -> LmcColors.Success
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = CircleShape,
        color = containerColor,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = LmcSpacing.Space2, vertical = LmcSpacing.Space1),
        )
    }
}

@Composable
private fun QuizQuestionBody(
    question: Question,
    selectedAnswer: String?,
    submitted: Boolean,
    result: QuestionResult?,
    onSelectAnswer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(LmcSpacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
    ) {
        Text(
            text = question.prompt,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        question.options.forEach { option ->
            QuizOption(
                text = option,
                selected = selectedAnswer == option,
                submitted = submitted,
                isCorrectAnswer = option == result?.correctAnswer,
                onClick = { onSelectAnswer(option) },
            )
        }
        if (!submitted && selectedAnswer == null) {
            Text(
                text = stringResource(R.string.quiz_select_answer_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (submitted) {
            FeedbackMessage(
                correct = result?.isCorrect == true,
                explanation = question.explanation,
            )
        }
    }
}

@Composable
private fun QuizOption(
    text: String,
    selected: Boolean,
    submitted: Boolean,
    isCorrectAnswer: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(LmcSpacing.QuizOptionRadius)
    val borderColor = when {
        submitted && isCorrectAnswer -> LmcColors.Success
        submitted && selected -> MaterialTheme.colorScheme.tertiary
        selected -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    val backgroundColor = when {
        submitted && isCorrectAnswer -> LmcColors.SuccessContainer
        submitted && selected -> MaterialTheme.colorScheme.tertiaryContainer
        selected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.QuizOptionMinHeight)
            .border(
                width = if (selected || (submitted && isCorrectAnswer)) 2.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(enabled = !submitted, onClick = onClick),
        color = backgroundColor,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            if (submitted && isCorrectAnswer) {
                LmcCanvasIcon(icon = LmcIcon.Check, color = LmcColors.Success)
            } else {
                OptionCircle(
                    selected = selected,
                    error = false,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FeedbackMessage(
    correct: Boolean,
    explanation: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusMd),
        color = if (correct) LmcColors.SuccessContainer else MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            LmcCanvasIcon(
                icon = if (correct) LmcIcon.Check else LmcIcon.Book,
                color = if (correct) LmcColors.Success else MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space1)) {
                Text(
                    text = stringResource(
                        if (correct) {
                            R.string.quiz_correct
                        } else {
                            R.string.quiz_incorrect
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun QuizCompletionScreen(
    story: Story,
    correctCount: Int,
    totalQuestions: Int,
    streakSummary: StreakSummary?,
    completionJustRecorded: Boolean,
    reviewPack: ReviewPack?,
    voiceRecordingService: VoiceRecordingService,
    onOpenReview: () -> Unit,
    onReadAgain: () -> Unit,
    onDone: () -> Unit,
) {
    var showPlayMore by remember(story.id) { mutableStateOf(false) }
    if (showPlayMore) {
        PlayMorePracticeScreen(
            story = story,
            onBack = { showPlayMore = false },
            onDone = { showPlayMore = false },
        )
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CompletionCelebrationBurst(
            enabled = completionJustRecorded || streakSummary?.newMilestoneDays != null,
            modifier = Modifier.matchParentSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(LmcSpacing.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
            ) {
                Spacer(modifier = Modifier.height(LmcSpacing.Space8))
                CompletionRewardHero(
                    streakSummary = streakSummary,
                    completionJustRecorded = completionJustRecorded,
                )
                Text(
                    text = stringResource(R.string.quiz_complete_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.completion_encouragement_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.completion_encouragement_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.quiz_score, correctCount, totalQuestions),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (streakSummary != null) {
                    CompletionMilestoneBanner(streakSummary = streakSummary)
                }
                RetellStepCard(
                    story = story,
                    voiceRecordingService = voiceRecordingService,
                )
                if (reviewPack != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shadowElevation = LmcSpacing.CardElevation,
                    ) {
                        Column(
                            modifier = Modifier.padding(LmcSpacing.CardPadding),
                            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                        ) {
                            Text(
                                text = stringResource(R.string.review_pack_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                text = if (reviewPack.missedQuestions.isEmpty()) {
                                    stringResource(R.string.review_pack_all_correct)
                                } else {
                                    stringResource(R.string.review_pack_intro)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Button(
                                onClick = onOpenReview,
                                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                            ) {
                                Text(text = stringResource(R.string.review_pack_view))
                            }
                        }
                    }
                }
                if (remember(story.id) { hasInteractivePractice(story) }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shadowElevation = LmcSpacing.CardElevation,
                    ) {
                        Column(
                            modifier = Modifier.padding(LmcSpacing.CardPadding),
                            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                        ) {
                            Text(
                                text = stringResource(R.string.play_more_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Text(
                                text = stringResource(R.string.play_more_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                            Button(
                                onClick = { showPlayMore = true },
                                modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                            ) {
                                Text(text = stringResource(R.string.play_more_start))
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(
                        horizontal = LmcSpacing.ScreenPadding,
                        vertical = LmcSpacing.Space3,
                    ),
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onReadAgain,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                ) {
                    Text(text = stringResource(R.string.action_read_again))
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(text = stringResource(R.string.action_done))
                }
            }
        }
    }
}

/** Sentinel paragraph index that tags a recording as a story retell (not a paragraph read-aloud). */
private const val RetellParagraphIndex: Int = -1

/**
 * Optional, skippable "Tell the story / 复述故事" step shown on the completion screen. It shows the
 * story-grounded retell prompt, lets the child record a retell by REUSING the exact #4 voice-recording
 * stack (same controller, same local-only [VoiceRecordingService] storage, same parent management — the
 * retell is tagged via [RetellParagraphIndex] so the parent list labels it), and offers a low-pressure
 * self-check (tappable chips, self-report, NO ASR) with always-positive encouragement.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RetellStepCard(
    story: Story,
    voiceRecordingService: VoiceRecordingService,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val guide = remember(story.id) { RetellGuideUseCases().buildGuide(story) }
    val retellUseCases = remember { RetellGuideUseCases() }

    var expanded by remember(story.id) { mutableStateOf(false) }
    val controller = remember(context.applicationContext) { AndroidVoiceRecordingController(context) }
    val stateMachine = remember { RecordingStateMachine() }
    var recordingState by remember(story.id) { mutableStateOf<RecordingState>(RecordingState.Idle) }
    var messageRes by remember(story.id) { mutableStateOf<Int?>(null) }
    var showPermissionDialog by remember(story.id) { mutableStateOf(false) }
    var showPermissionSettingsAction by remember(story.id) { mutableStateOf(false) }
    val checkedItems = remember(story.id) { mutableStateListOf<String>() }
    val revealedHints = remember(story.id) { mutableStateListOf<String>() }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    val isRecording = recordingState is RecordingState.Recording
    val isPlaying = recordingState is RecordingState.Playing

    fun stopRecording() {
        val active = recordingState as? RecordingState.Recording ?: return
        val capture = controller.stopRecording()
        if (capture == null) {
            recordingState = stateMachine.reduce(recordingState, RecordingAction.Reset)
            messageRes = R.string.voice_recording_failed
            return
        }
        val recording = VoiceRecording(
            id = capture.file.nameWithoutExtension,
            storyId = active.storyId,
            paragraphIndex = active.paragraphIndex,
            filePath = capture.file.absolutePath,
            durationMs = capture.durationMs,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        recordingState = stateMachine.reduce(recordingState, RecordingAction.Stopped(recording.id))
        messageRes = R.string.retell_recording_saved
        scope.launch {
            val result = voiceRecordingService.add(recording)
            result.removed.forEach { removed -> runCatching { File(removed.filePath).delete() } }
        }
    }

    fun beginRecording() {
        controller.stopPlayback()
        messageRes = null
        recordingState = stateMachine.reduce(
            recordingState,
            RecordingAction.Request(story.id, RetellParagraphIndex),
        )
        try {
            controller.startRecording(story.id, RetellParagraphIndex)
            recordingState = stateMachine.reduce(recordingState, RecordingAction.PermissionGranted)
        } catch (_: Throwable) {
            recordingState = stateMachine.reduce(recordingState, RecordingAction.Reset)
            messageRes = R.string.voice_recording_failed
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showPermissionDialog = false
            showPermissionSettingsAction = false
            beginRecording()
        } else {
            recordingState = stateMachine.reduce(recordingState, RecordingAction.PermissionDenied)
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.RECORD_AUDIO,
                )
            } ?: false
            // Permanently denied (no system dialog will appear) → offer an app-settings path
            // so the user isn't stuck, matching the reader's read-aloud handler.
            showPermissionSettingsAction = !shouldShowRationale
            showPermissionDialog = true
            messageRes = R.string.voice_mic_denied
        }
    }

    fun requestRecording() {
        if (isRecording) {
            stopRecording()
            return
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            beginRecording()
        } else {
            recordingState = stateMachine.reduce(
                recordingState,
                RecordingAction.Request(story.id, RetellParagraphIndex),
            )
            showPermissionDialog = true
        }
    }

    fun playLatest() {
        val recordingId = recordingState.activeRecordingId ?: return
        if (isRecording) stopRecording()
        controller.stopPlayback()
        messageRes = null
        recordingState = stateMachine.reduce(
            RecordingState.Stopped(story.id, RetellParagraphIndex, recordingId),
            RecordingAction.Playing(recordingId),
        )
        // Resolve the file path from the stored service so playback works after process recreation.
        scope.launch {
            val recording = voiceRecordingService.getRecordings(story.id)
                .firstOrNull { it.id == recordingId }
            if (recording == null) {
                recordingState = stateMachine.reduce(recordingState, RecordingAction.Reset)
                messageRes = R.string.voice_recording_playback_failed
                return@launch
            }
            controller.play(
                filePath = recording.filePath,
                onCompletion = {
                    recordingState = stateMachine.reduce(recordingState, RecordingAction.Completed)
                },
                onError = {
                    recordingState = stateMachine.reduce(recordingState, RecordingAction.Reset)
                    messageRes = R.string.voice_recording_playback_failed
                },
            )
        }
    }

    val hasRecording = recordingState.activeRecordingId != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Text(
                text = stringResource(R.string.retell_step_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = guide.prompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!expanded) {
                Text(
                    text = stringResource(R.string.retell_step_optional),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                ) {
                    Text(text = stringResource(R.string.retell_step_start))
                }
            } else {
                // Record / stop + play back — same local-only recording stack as #4.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { requestRecording() },
                        modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                    ) {
                        Text(
                            text = stringResource(
                                if (isRecording) R.string.retell_stop_recording
                                else R.string.retell_start_recording,
                            ),
                        )
                    }
                    if (hasRecording && !isRecording) {
                        OutlinedButton(
                            onClick = { playLatest() },
                            modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                            shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isPlaying) R.string.voice_recording_playing
                                    else R.string.action_play_recording,
                                ),
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.voice_practice_privacy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                messageRes?.let { res ->
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (guide.checkItems.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.retell_self_check_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                    ) {
                        guide.checkItems.forEach { item ->
                            val selected = checkedItems.contains(item.text)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) checkedItems.remove(item.text)
                                    else checkedItems.add(item.text)
                                },
                                label = { Text(text = item.text) },
                                modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                            )
                        }
                    }
                    val encouragementRes = when (
                        retellUseCases.encouragementFor(checkedItems.size, guide.checkItems.size)
                    ) {
                        RetellEncouragement.JustStarted -> R.string.retell_encouragement_just_started
                        RetellEncouragement.GoodProgress -> R.string.retell_encouragement_good_progress
                        RetellEncouragement.GreatRetell -> R.string.retell_encouragement_great
                    }
                    Text(
                        text = stringResource(encouragementRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    // Optional per-item hints (meaning), revealed on demand.
                    val itemsWithMeaning = guide.checkItems.filter { !it.meaning.isNullOrBlank() }
                    if (itemsWithMeaning.isNotEmpty()) {
                        itemsWithMeaning.forEach { item ->
                            val revealed = revealedHints.contains(item.text)
                            TextButton(
                                onClick = {
                                    if (revealed) revealedHints.remove(item.text)
                                    else revealedHints.add(item.text)
                                },
                                modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                            ) {
                                Text(
                                    text = if (revealed) {
                                        stringResource(
                                            R.string.retell_hint_revealed_format,
                                            item.text,
                                            item.meaning.orEmpty(),
                                        )
                                    } else {
                                        stringResource(R.string.retell_hint_show_format, item.text)
                                    },
                                )
                            }
                        }
                    }
                }

                if (showPermissionDialog) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(LmcSpacing.RadiusSm),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.padding(LmcSpacing.Space3),
                            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                        ) {
                            Text(
                                text = stringResource(R.string.voice_mic_rationale_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    if (showPermissionSettingsAction) {
                                        R.string.voice_mic_settings_body
                                    } else {
                                        R.string.voice_mic_rationale_body
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showPermissionDialog = false
                                        if (showPermissionSettingsAction) {
                                            activity?.let { currentActivity ->
                                                currentActivity.startActivity(
                                                    Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.fromParts("package", currentActivity.packageName, null),
                                                    ),
                                                )
                                            }
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                                    shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                                ) {
                                    Text(
                                        text = stringResource(
                                            if (showPermissionSettingsAction) {
                                                R.string.action_open_settings
                                            } else {
                                                R.string.voice_mic_allow
                                            },
                                        ),
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        showPermissionDialog = false
                                        recordingState = stateMachine.reduce(
                                            recordingState,
                                            RecordingAction.Reset,
                                        )
                                    },
                                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                                ) {
                                    Text(text = stringResource(R.string.action_skip))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun practiceSeed(story: Story): Int = story.id.hashCode()

private fun hasInteractivePractice(story: Story): Boolean =
    InteractivePracticeUseCases().generate(story, seed = practiceSeed(story)).isNotEmpty()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayMorePracticeScreen(
    story: Story,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val useCases = remember { InteractivePracticeUseCases() }
    val items = remember(story.id) {
        useCases.generate(story, seed = practiceSeed(story))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FlowTopBar(
            title = stringResource(R.string.play_more_title),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(LmcSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space5),
        ) {
            Text(
                text = stringResource(R.string.play_more_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.play_more_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            items.forEach { item ->
                when (item) {
                    is InteractivePracticeItem.SentenceOrdering ->
                        PracticeOrderingCard(item = item, useCases = useCases)
                    is InteractivePracticeItem.WordMatching ->
                        PracticeMatchingCard(item = item, useCases = useCases)
                    is InteractivePracticeItem.Cloze ->
                        PracticeClozeCard(item = item, useCases = useCases)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    horizontal = LmcSpacing.ScreenPadding,
                    vertical = LmcSpacing.Space3,
                ),
        ) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.play_more_done))
            }
        }
    }
}

@Composable
private fun PracticeCardShell(
    title: String,
    feedback: PracticeFeedback,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
            PracticeFeedbackRow(feedback = feedback)
        }
    }
}

private enum class PracticeFeedback { None, Correct, TryAgain }

@Composable
private fun PracticeFeedbackRow(feedback: PracticeFeedback) {
    if (feedback == PracticeFeedback.None) return
    val (textRes, color) = when (feedback) {
        PracticeFeedback.Correct ->
            R.string.play_more_correct to MaterialTheme.colorScheme.primary
        PracticeFeedback.TryAgain ->
            R.string.play_more_try_again to MaterialTheme.colorScheme.secondary
        PracticeFeedback.None -> return
    }
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleSmall,
        color = color,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun PracticeOrderingCard(
    item: InteractivePracticeItem.SentenceOrdering,
    useCases: InteractivePracticeUseCases,
) {
    val order = remember(item.id) { mutableStateListOf<String>().apply { addAll(item.shuffled) } }
    var feedback by remember(item.id) { mutableStateOf(PracticeFeedback.None) }
    val moveUpLabel = stringResource(R.string.play_more_move_up)
    val moveDownLabel = stringResource(R.string.play_more_move_down)
    PracticeCardShell(
        title = stringResource(R.string.play_more_ordering_title),
        feedback = feedback,
    ) {
        order.forEachIndexed { index, sentence ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
            ) {
                Text(
                    text = sentence,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        if (index > 0) {
                            val tmp = order[index - 1]
                            order[index - 1] = order[index]
                            order[index] = tmp
                            feedback = PracticeFeedback.None
                        }
                    },
                    enabled = index > 0,
                    modifier = Modifier
                        .size(LmcSpacing.MinTouchTarget)
                        .semantics {
                            contentDescription = moveUpLabel
                        },
                ) {
                    Text(
                        text = "▲",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (index > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                }
                IconButton(
                    onClick = {
                        if (index < order.lastIndex) {
                            val tmp = order[index + 1]
                            order[index + 1] = order[index]
                            order[index] = tmp
                            feedback = PracticeFeedback.None
                        }
                    },
                    enabled = index < order.lastIndex,
                    modifier = Modifier
                        .size(LmcSpacing.MinTouchTarget)
                        .semantics {
                            contentDescription = moveDownLabel
                        },
                ) {
                    Text(
                        text = "▼",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (index < order.lastIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    )
                }
            }
        }
        Button(
            onClick = {
                feedback = if (useCases.scoreOrdering(item, order.toList())) {
                    PracticeFeedback.Correct
                } else {
                    PracticeFeedback.TryAgain
                }
            },
            modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
            shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        ) {
            Text(text = stringResource(R.string.play_more_check))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeMatchingCard(
    item: InteractivePracticeItem.WordMatching,
    useCases: InteractivePracticeUseCases,
) {
    val meanings = remember(item.id) { item.pairs.map { it.meaning }.shuffled() }
    var selectedWord by remember(item.id) { mutableStateOf<String?>(null) }
    val matches = remember(item.id) { mutableStateMapOf<String, String>() }
    var feedback by remember(item.id) { mutableStateOf(PracticeFeedback.None) }
    PracticeCardShell(
        title = stringResource(R.string.play_more_matching_title),
        feedback = feedback,
    ) {
        item.pairs.forEach { pair ->
            val isSelected = selectedWord == pair.word
            val assigned = matches[pair.word]
            OutlinedButton(
                onClick = {
                    selectedWord = if (isSelected) null else pair.word
                    feedback = PracticeFeedback.None
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LmcSpacing.MinTouchTarget),
                colors = if (isSelected) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text(
                    text = if (assigned != null) "${pair.word} → $assigned" else pair.word,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        HorizontalDivider()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2)) {
            meanings.forEach { meaning ->
                val used = matches.values.contains(meaning)
                Button(
                    onClick = {
                        val word = selectedWord ?: return@Button
                        matches.entries.filter { it.value == meaning }.forEach { matches.remove(it.key) }
                        matches[word] = meaning
                        selectedWord = null
                        feedback = PracticeFeedback.None
                    },
                    enabled = selectedWord != null && !used,
                    modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                    shape = RoundedCornerShape(LmcSpacing.RadiusMd),
                ) {
                    Text(text = meaning, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Button(
            onClick = {
                feedback = if (useCases.scoreMatching(item, matches.toMap())) {
                    PracticeFeedback.Correct
                } else {
                    PracticeFeedback.TryAgain
                }
            },
            modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
            shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        ) {
            Text(text = stringResource(R.string.play_more_check))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PracticeClozeCard(
    item: InteractivePracticeItem.Cloze,
    useCases: InteractivePracticeUseCases,
) {
    var selected by remember(item.id) { mutableStateOf<String?>(null) }
    var feedback by remember(item.id) { mutableStateOf(PracticeFeedback.None) }
    PracticeCardShell(
        title = stringResource(R.string.play_more_cloze_title),
        feedback = feedback,
    ) {
        Text(
            text = item.sentenceBefore + (selected ?: "＿＿＿") + item.sentenceAfter,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2)) {
            item.options.forEach { option ->
                val isSelected = selected == option
                Button(
                    onClick = {
                        selected = option
                        feedback = if (useCases.scoreCloze(item, option)) {
                            PracticeFeedback.Correct
                        } else {
                            PracticeFeedback.TryAgain
                        }
                    },
                    modifier = Modifier.heightIn(min = LmcSpacing.MinTouchTarget),
                    shape = RoundedCornerShape(LmcSpacing.RadiusMd),
                    colors = if (isSelected) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    },
                ) {
                    Text(text = option, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun reviewParentTipText(tip: ReviewParentTip): String = stringResource(
    when (tip) {
        ReviewParentTip.ReadTogether -> R.string.review_pack_tip_read_together
        ReviewParentTip.ReviewDueWords -> R.string.review_pack_tip_review_due_words
        ReviewParentTip.PraiseAndOneWord -> R.string.review_pack_tip_praise
    },
)

@Composable
private fun ReviewPackScreen(
    pack: ReviewPack?,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FlowTopBar(
            title = stringResource(R.string.review_pack_title),
            onBack = onBack,
        )
        if (pack == null) {
            CenterStateMessage(text = stringResource(R.string.review_pack_all_correct))
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(LmcSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            Text(
                text = stringResource(R.string.review_pack_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (pack.missedQuestions.isEmpty()) {
                ReviewSectionCard(title = stringResource(R.string.review_pack_missed_title)) {
                    Text(
                        text = stringResource(R.string.review_pack_all_correct),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                ReviewSectionCard(title = stringResource(R.string.review_pack_missed_title)) {
                    pack.missedQuestions.forEach { missed ->
                        Text(
                            text = missed.prompt,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.review_pack_correct_answer_label,
                            ) + ": " + missed.correctAnswer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.review_pack_why_label) + ": " + missed.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (pack.reviewWords.isNotEmpty()) {
                ReviewSectionCard(title = stringResource(R.string.review_pack_words_title)) {
                    pack.reviewWords.forEach { word ->
                        Text(
                            text = "${word.word} · ${word.pinyin}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = word.meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            pack.rereadSentence?.let { sentence ->
                ReviewSectionCard(title = stringResource(R.string.review_pack_reread_title)) {
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            ReviewSectionCard(
                title = stringResource(R.string.review_pack_parent_tip_title),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Text(
                    text = reviewParentTipText(pack.parentTip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    horizontal = LmcSpacing.ScreenPadding,
                    vertical = LmcSpacing.Space3,
                ),
        ) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.review_pack_done))
            }
        }
    }
}

@Composable
private fun ReviewSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = containerColor,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            content()
        }
    }
}

@Composable
private fun CompletionCelebrationBurst(
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    val reduceMotion = rememberReduceMotionEnabled()
    val progress = remember(reduceMotion) { Animatable(if (reduceMotion) 1f else 0f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LmcMotion.CelebrationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    Canvas(
        modifier = modifier.clearAndSetSemantics {},
    ) {
        CompletionParticles.forEachIndexed { index, particle ->
            val start = Offset(size.width * 0.5f, size.height * 0.18f)
            val target = Offset(size.width * particle.x, size.height * particle.y)
            val animated = progress.value
            val lift = if (reduceMotion) 0f else sin(animated * PI).toFloat() * particle.lift.toPx()
            val center = Offset(
                x = start.x + (target.x - start.x) * animated,
                y = start.y + (target.y - start.y) * animated - lift,
            )
            val alpha = if (reduceMotion) {
                0.22f
            } else {
                (0.25f + (1f - animated) * 0.75f).coerceIn(0.25f, 1f)
            }
            val color = particle.color
            if (index % 3 == 0) {
                drawCompletionStar(
                    center = center,
                    radius = particle.size.toPx(),
                    color = color,
                    alpha = alpha,
                )
            } else {
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(center.x - particle.size.toPx() / 2f, center.y - particle.size.toPx() / 2f),
                    size = Size(particle.size.toPx() * 1.4f, particle.size.toPx() * 0.75f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun CompletionRewardHero(
    streakSummary: StreakSummary?,
    completionJustRecorded: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = rememberReduceMotionEnabled()
    val scale = remember(reduceMotion) { Animatable(if (reduceMotion) 1f else 0.88f) }
    val milestoneDays = streakSummary?.newMilestoneDays
    val stampText = when {
        milestoneDays != null -> stringResource(
            R.string.completion_stamp_milestone,
            milestoneDays,
        )
        completionJustRecorded && streakSummary?.didAdvanceStreakOnCompletion() == true ->
            stringResource(R.string.completion_stamp_streak_plus_one)
        completionJustRecorded ->
            stringResource(R.string.completion_stamp_story_plus_one)
        else ->
            stringResource(R.string.completion_stamp_story_done)
    }
    val contentDescription = stringResource(R.string.completion_sticker_content_description)

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            scale.snapTo(1f)
        } else {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LmcMotion.MediumMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    Box(
        modifier = modifier
            .size(width = 152.dp, height = 116.dp)
            .semantics { this.contentDescription = "$contentDescription, $stampText" },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size((92.dp * scale.value)),
            shape = CircleShape,
            color = LmcColors.SuccessContainer,
            shadowElevation = LmcSpacing.CardElevation,
        ) {
            Box(contentAlignment = Alignment.Center) {
                LmcCanvasIcon(
                    icon = LmcIcon.Check,
                    color = LmcColors.Success,
                    modifier = Modifier.size(42.dp),
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .rotate(if (reduceMotion) 0f else -6f),
            shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = LmcSpacing.CardElevation,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = LmcSpacing.Space3,
                    vertical = LmcSpacing.Space2,
                ),
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LmcCanvasIcon(
                    icon = LmcIcon.Check,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stampText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun StreakSummary.didAdvanceStreakOnCompletion(): Boolean =
    todayGoalMet && todayCompletedStories == dailyGoalStories

@Composable
private fun rememberReduceMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}

private fun DrawScope.drawCompletionStar(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    val path = Path()
    repeat(10) { point ->
        val angle = -PI / 2.0 + point * PI / 5.0
        val pointRadius = if (point % 2 == 0) radius else radius * 0.45f
        val x = center.x + cos(angle).toFloat() * pointRadius
        val y = center.y + sin(angle).toFloat() * pointRadius
        if (point == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    drawPath(path, color.copy(alpha = alpha))
}

@Composable
private fun CompletionMilestoneBanner(
    streakSummary: StreakSummary,
) {
    val title = streakSummary.newMilestoneDays?.let { milestone ->
        stringResource(R.string.completion_milestone_streak_title, milestone)
    } ?: stringResource(R.string.completion_milestone_goal_title)
    val body = if (streakSummary.todayGoalMet) {
        stringResource(
            R.string.completion_milestone_goal_body,
            streakSummary.todayCompletedStories,
            streakSummary.dailyGoalStories,
        )
    } else {
        stringResource(R.string.completion_milestone_keep_going)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = LmcColors.SuccessContainer,
    ) {
        Row(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LmcCanvasIcon(icon = LmcIcon.Check, color = LmcColors.Success)
            Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space1)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ParentGateCard(onPassed: () -> Unit) {
    var answer by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Text(
                text = stringResource(R.string.parent_gate_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.parent_gate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.parent_gate_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            OutlinedTextField(
                value = answer,
                onValueChange = {
                    answer = it
                    showError = false
                },
                label = { Text(text = stringResource(R.string.parent_gate_answer_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = showError,
                supportingText = if (showError) {
                    {
                        Text(text = stringResource(R.string.parent_gate_error))
                    }
                } else {
                    null
                },
            )
            Button(
                onClick = {
                    if (answer.trim() == ParentGateExpectedAnswer) {
                        showError = false
                        onPassed()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.parent_gate_button))
            }
        }
    }
}

@Composable
private fun ParentAdviceCard(
    summary: ParentReportSummary,
    stories: List<Story>,
    onStoryClick: (String) -> Unit,
    onReviewWords: () -> Unit,
) {
    val advice = summary.advice
    val story = advice.storyId?.let { storyId -> stories.firstOrNull { it.id == storyId } }
    val body = when (advice.type) {
        ParentAdviceType.ReviewDueWords -> stringResource(
            R.string.parent_advice_review_due_words_format,
            advice.dueWordCount,
        )
        ParentAdviceType.RevisitRecentStory -> story?.let {
            stringResource(R.string.parent_advice_revisit_story_format, it.titleZh)
        } ?: stringResource(R.string.parent_advice_read_together)
        ParentAdviceType.TryNextStory -> story?.let {
            stringResource(R.string.parent_advice_try_next_story_format, it.titleZh)
        } ?: stringResource(R.string.parent_advice_read_together)
        ParentAdviceType.CelebrateStreak -> stringResource(R.string.parent_advice_keep_streak)
        ParentAdviceType.ReadTogetherToday -> stringResource(R.string.parent_advice_read_together)
    }
    val action = when {
        advice.type == ParentAdviceType.ReviewDueWords -> onReviewWords
        story != null -> ({ onStoryClick(story.id) })
        else -> null
    }
    val actionLabel = when {
        advice.type == ParentAdviceType.ReviewDueWords -> stringResource(R.string.parent_advice_action_review_words)
        story != null -> stringResource(R.string.parent_advice_action_open_story)
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Text(
                text = stringResource(R.string.parent_advice_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            story?.retellPrompt?.takeIf { it.isNotBlank() }?.let { prompt ->
                Text(
                    text = stringResource(R.string.parent_advice_retell_prompt_format, prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            if (action != null && actionLabel != null) {
                OutlinedButton(
                    onClick = action,
                    modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ParentWeeklyPlanSection(
    plan: ParentWeeklyPlan,
    stories: List<Story>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            Text(
                text = stringResource(R.string.weekly_plan_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 1. Stories read this week
            WeeklyPlanBlock(title = stringResource(R.string.weekly_plan_stories_read)) {
                if (plan.storiesReadThisWeek.isEmpty()) {
                    WeeklyPlanBody(stringResource(R.string.weekly_plan_stories_empty))
                } else {
                    plan.storiesReadThisWeek.forEach { story ->
                        WeeklyPlanBody(
                            stringResource(
                                R.string.weekly_plan_story_titles_format,
                                story.titleZh,
                                story.titleEn,
                            ),
                        )
                    }
                }
            }

            // 2. Mastered vs needs-practice words
            WeeklyPlanBlock(title = stringResource(R.string.weekly_plan_mastered_words)) {
                WeeklyPlanWordList(
                    words = plan.masteredWords,
                    emptyText = stringResource(R.string.weekly_plan_mastered_empty),
                )
            }
            WeeklyPlanBlock(title = stringResource(R.string.weekly_plan_weak_words)) {
                WeeklyPlanWordList(
                    words = plan.weakWords,
                    emptyText = stringResource(R.string.weekly_plan_weak_empty),
                )
            }

            // 3. One sentence to re-read (only when present)
            plan.rereadSentence?.takeIf { it.isNotBlank() }?.let { sentence ->
                WeeklyPlanBlock(title = stringResource(R.string.weekly_plan_reread_title)) {
                    WeeklyPlanBody(sentence)
                    val rereadStory = plan.rereadStoryId?.let { id -> stories.firstOrNull { it.id == id } }
                    rereadStory?.let {
                        WeeklyPlanBody(
                            text = stringResource(R.string.weekly_plan_reread_from_format, it.titleZh),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // 4. Weekend retell question (only when present)
            plan.weekendRetellPrompt?.takeIf { it.isNotBlank() }?.let { prompt ->
                WeeklyPlanBlock(title = stringResource(R.string.weekly_plan_retell_title)) {
                    WeeklyPlanBody(prompt)
                }
            }

            // 5. Top suggestion
            WeeklyPlanBlock(title = stringResource(R.string.weekly_plan_advice_title)) {
                WeeklyPlanBody(weeklyPlanAdviceText(plan.topAdvice))
            }

            // 6. Shareable, PII-free card
            WeeklyPlanShareCard(card = plan.shareCard)
        }
    }
}

@Composable
private fun weeklyPlanAdviceText(advice: ParentAdviceType): String = when (advice) {
    ParentAdviceType.ReviewDueWords -> stringResource(R.string.weekly_plan_advice_review_due_words)
    ParentAdviceType.ReadTogetherToday -> stringResource(R.string.weekly_plan_advice_read_together)
    ParentAdviceType.RevisitRecentStory -> stringResource(R.string.weekly_plan_advice_revisit_story)
    ParentAdviceType.TryNextStory -> stringResource(R.string.weekly_plan_advice_try_next_story)
    ParentAdviceType.CelebrateStreak -> stringResource(R.string.weekly_plan_advice_celebrate_streak)
}

@Composable
private fun WeeklyPlanBlock(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space1)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun WeeklyPlanBody(
    text: String,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WeeklyPlanWordList(
    words: List<WeeklyPlanWord>,
    emptyText: String,
) {
    if (words.isEmpty()) {
        WeeklyPlanBody(emptyText)
    } else {
        words.forEach { word ->
            WeeklyPlanBody(
                stringResource(
                    R.string.weekly_plan_word_line_format,
                    word.word,
                    word.pinyin,
                    word.meaning,
                ),
            )
        }
    }
}

@Composable
private fun WeeklyPlanShareCard(card: WeeklyShareCard) {
    val context = LocalContext.current
    val summary = stringResource(
        R.string.weekly_plan_share_summary_format,
        card.storiesThisWeek,
        card.wordsInNotebook,
        card.masteredWords,
        card.streakDays,
    )
    val chooserTitle = stringResource(R.string.weekly_plan_share_chooser_title)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusMd),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = stringResource(R.string.weekly_plan_share_card_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            OutlinedButton(
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, summary)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
                },
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
            ) {
                Text(text = stringResource(R.string.weekly_plan_share_button))
            }
        }
    }
}

@Composable
private fun PrivacyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FlowTopBar(
            title = stringResource(R.string.privacy_title),
            onBack = onBack,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LmcSpacing.ScreenPadding)
                .widthIn(max = LmcSpacing.ReadingMaxWidth)
                .align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            Text(
                text = stringResource(R.string.privacy_summary),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            PrivacyPoint(
                title = stringResource(R.string.privacy_coppa_gdprk_title),
                body = stringResource(R.string.privacy_coppa_gdprk),
            )
            PrivacyPoint(
                title = stringResource(R.string.privacy_parent_account_title),
                body = stringResource(R.string.privacy_parent_account_only),
            )
            PrivacyPoint(
                title = stringResource(R.string.privacy_no_child_details_title),
                body = stringResource(R.string.privacy_no_child_details),
            )
            PrivacyPoint(
                title = stringResource(R.string.privacy_ai_scope_title),
                body = stringResource(R.string.privacy_ai_scope),
            )
            PrivacyPoint(
                title = stringResource(R.string.privacy_analytics_title),
                body = stringResource(R.string.privacy_analytics_anonymous),
            )
            PrivacyPoint(
                title = stringResource(R.string.privacy_local_progress_title),
                body = stringResource(R.string.privacy_local_progress),
            )
            PrivacyPoint(
                title = stringResource(R.string.privacy_content_title),
                body = stringResource(R.string.privacy_public_domain_content),
            )
            PrivacyPoint(
                title = stringResource(R.string.privacy_network_title),
                body = stringResource(R.string.privacy_in_app_no_external_page),
            )
            Button(
                onClick = onBack,
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.action_done))
            }
        }
    }
}

@Composable
private fun AiSafetyConsoleScreen(
    logService: AiInteractionLogService,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val summaryUseCases = remember { AiSafetyConsoleUseCases() }
    val records by logService.records.collectAsState(initial = emptyList())

    // Re-read from disk when the console opens so the parent always sees the latest log.
    LaunchedEffect(Unit) { logService.read() }

    val summary = remember(records) { summaryUseCases.summary(records) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FlowTopBar(
            title = stringResource(R.string.ai_console_title),
            onBack = onBack,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = LmcSpacing.ScreenPadding,
                vertical = LmcSpacing.Space4,
            ),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LmcSpacing.RadiusSm),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = LmcSpacing.CardElevation,
                ) {
                    Column(
                        modifier = Modifier.padding(LmcSpacing.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
                    ) {
                        Text(
                            text = stringResource(R.string.ai_console_summary_total, summary.totalCount),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.ai_console_summary_declined,
                                summary.outOfScopeCount,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val lastText = summary.lastEpochMillis?.let { relativeTimeText(it) }
                            ?: stringResource(R.string.ai_console_last_none)
                        Text(
                            text = stringResource(R.string.ai_console_summary_last, lastText),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.ai_console_retention),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (records.isNotEmpty()) {
                item {
                    OutlinedButton(
                        onClick = { scope.launch { logService.clear() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
                    ) {
                        Text(text = stringResource(R.string.ai_console_clear_all))
                    }
                }
            }
            if (records.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.ai_console_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(records, key = { it.id }) { record ->
                    AiInteractionRow(
                        record = record,
                        onDelete = { scope.launch { logService.deleteById(record.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun AiInteractionRow(
    record: AiInteractionRecord,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusSm),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
            ) {
                AiOutcomeBadge(outcome = record.outcome)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = relativeTimeText(record.epochMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.ai_console_query_label, record.query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.ai_console_answer_label, record.answerPreview),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.ai_console_delete))
            }
        }
    }
}

@Composable
private fun AiOutcomeBadge(outcome: AiInteractionOutcome) {
    val isDeclined = outcome == AiInteractionOutcome.OutOfScope
    val container = if (isDeclined) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (isDeclined) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val labelRes = if (isDeclined) {
        R.string.ai_console_outcome_declined
    } else {
        R.string.ai_console_outcome_allowed
    }
    Surface(
        shape = RoundedCornerShape(LmcSpacing.RadiusSm),
        color = container,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(
                horizontal = LmcSpacing.Space2,
                vertical = LmcSpacing.Space1,
            ),
        )
    }
}

@Composable
private fun relativeTimeText(epochMillis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()

private fun cleanupRecordingFiles(recordings: List<VoiceRecording>) {
    recordings.forEach { recording ->
        runCatching {
            File(recording.filePath).delete()
        }
    }
}

private suspend fun VoiceRecordingService.register(
    recording: VoiceRecording,
): RecordingStorageResult = add(recording)

private suspend fun VoiceRecordingService.deleteById(
    recordingId: String,
): RecordingStorageResult = delete(recordingId)

@Composable
private fun voiceRecordingDurationText(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000L).coerceAtLeast(1L)
    return if (totalSeconds < 60L) {
        stringResource(R.string.voice_recording_duration_seconds, totalSeconds)
    } else {
        stringResource(
            R.string.voice_recording_duration_minutes,
            totalSeconds / 60L,
            totalSeconds % 60L,
        )
    }
}

@Composable
private fun PrivacyPoint(
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusSm),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricGrid(metrics: List<Metric>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 720.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3)) {
            metrics.chunked(columns).forEach { rowMetrics ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
                ) {
                    rowMetrics.forEach { metric ->
                        MetricTile(
                            metric = metric,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowMetrics.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    metric: Metric,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 104.dp),
        shape = RoundedCornerShape(LmcSpacing.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = metric.value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StoryProgressRow(
    story: Story,
    progress: Float,
    completed: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LmcSpacing.RadiusSm),
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = story.titleZh,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = storyProgressLabel(progress = progress, completed = completed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LmcProgressBar(progress = progress)
        }
    }
}

@Composable
private fun PrivacyNotice(onPrivacy: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            Text(
                text = stringResource(R.string.parent_privacy_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.parent_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onPrivacy,
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
            ) {
                Text(text = stringResource(R.string.action_open_privacy))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2)) {
        SectionTitle(text = title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LmcSpacing.CardRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = LmcSpacing.CardElevation,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SelectableSettingsRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.MinTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = LmcSpacing.CardPadding, vertical = LmcSpacing.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            LmcCanvasIcon(icon = LmcIcon.Check, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.MinTouchTarget)
            .padding(horizontal = LmcSpacing.CardPadding, vertical = LmcSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit = {},
) {
    var sliderValue by remember(value) { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.MinTouchTarget),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LmcSpacing.CardPadding, vertical = LmcSpacing.Space3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.settings_sfx_volume_percent,
                    (sliderValue * 100f).roundToInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue
                onValueChange(newValue)
            },
            onValueChangeFinished = { onValueChangeFinished(sliderValue) },
            enabled = enabled,
            valueRange = 0f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LmcSpacing.CardPadding),
        )
    }
}

@Composable
private fun SettingsButtonRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = LmcSpacing.Space3, vertical = LmcSpacing.Space2),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SettingsTextSizeRow(
    selectedTextSize: ReadingTextSize,
    onTextSizeChange: (ReadingTextSize) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.MinTouchTarget)
            .padding(horizontal = LmcSpacing.CardPadding, vertical = LmcSpacing.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
    ) {
        Text(
            text = stringResource(R.string.settings_text_size),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        TextSizeChips(
            selectedTextSize = selectedTextSize,
            onTextSizeChange = onTextSizeChange,
        )
    }
}

@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.MinTouchTarget)
            .padding(horizontal = LmcSpacing.CardPadding, vertical = LmcSpacing.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.MinTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = LmcSpacing.CardPadding, vertical = LmcSpacing.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        LmcCanvasIcon(
            icon = LmcIcon.Next,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeedbackForm(
    feedbackService: FeedbackService,
    onSaved: () -> Unit,
) {
    val satisfactionChoices = remember { FeedbackPresentationOptions.satisfaction }
    val ageBandChoices = remember { FeedbackPresentationOptions.childAgeBands }
    val issueTypeChoices = remember { FeedbackPresentationOptions.issueTypes }
    var satisfaction by remember { mutableStateOf(satisfactionChoices.first()) }
    var ageBand by remember { mutableStateOf(ageBandChoices.first()) }
    var issueType by remember { mutableStateOf(issueTypeChoices.first()) }
    var suggestion by remember { mutableStateOf("") }
    var parentContact by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val buildFeedbackSubmissionUseCase = remember { BuildFeedbackSubmissionUseCase() }

    SettingsSection(title = stringResource(R.string.settings_feedback)) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            Text(
                text = stringResource(R.string.feedback_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FeedbackChoiceGroup(
                label = stringResource(R.string.feedback_satisfaction),
                choices = satisfactionChoices.map { choice ->
                    FeedbackChoice(choice, choice.satisfactionLabelRes())
                },
                selected = satisfaction,
                onSelected = { satisfaction = it },
            )
            FeedbackChoiceGroup(
                label = stringResource(R.string.feedback_child_age_band),
                choices = ageBandChoices.map { choice ->
                    FeedbackChoice(choice, choice.ageBandLabelRes())
                },
                selected = ageBand,
                onSelected = { ageBand = it },
            )
            FeedbackChoiceGroup(
                label = stringResource(R.string.feedback_issue_type),
                choices = issueTypeChoices.map { choice ->
                    FeedbackChoice(choice, choice.issueTypeLabelRes())
                },
                selected = issueType,
                onSelected = { issueType = it },
            )
            OutlinedTextField(
                value = suggestion,
                onValueChange = { suggestion = it },
                label = { Text(text = stringResource(R.string.feedback_suggestion)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
            )
            OutlinedTextField(
                value = parentContact,
                onValueChange = { parentContact = it },
                label = { Text(text = stringResource(R.string.feedback_parent_contact_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(text = stringResource(R.string.feedback_parent_contact_warning))
                },
            )
            Button(
                enabled = buildFeedbackSubmissionUseCase.canSubmit(suggestion),
                onClick = {
                    scope.launch {
                        feedbackService.submit(
                            buildFeedbackSubmissionUseCase(
                                satisfaction = satisfaction.value,
                                childAgeBand = ageBand.value,
                                issueType = issueType.value,
                                suggestion = suggestion,
                                parentContact = parentContact,
                            ),
                        )
                        onSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.feedback_submit))
            }
        }
    }
}

@Composable
private fun <T> FeedbackChoiceGroup(
    label: String,
    choices: List<FeedbackChoice<T>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        ) {
            choices.forEach { choice ->
                FilterChip(
                    selected = selected == choice.value,
                    onClick = { onSelected(choice.value) },
                    label = { Text(text = stringResource(choice.labelRes)) },
                    modifier = Modifier.heightIn(min = LmcSpacing.ChipHeight),
                    colors = lmcFilterChipColors(),
                )
            }
        }
    }
}

@Composable
private fun AiBackendBaseUrlRow(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(LmcSpacing.CardPadding),
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
    ) {
        Text(
            text = stringResource(R.string.settings_ai_backend_base_url),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = stringResource(R.string.settings_ai_backend_base_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Text(
            text = stringResource(R.string.settings_ai_backend_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TextSizeChips(
    selectedTextSize: ReadingTextSize,
    onTextSizeChange: (ReadingTextSize) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space1)) {
        ReadingTextSize.entries.forEach { textSize ->
            FilterChip(
                selected = selectedTextSize == textSize,
                onClick = { onTextSizeChange(textSize) },
                label = { Text(text = stringResource(textSize.labelRes())) },
                modifier = Modifier.heightIn(min = LmcSpacing.ChipHeight),
                colors = lmcFilterChipColors(),
            )
        }
    }
}

@Composable
private fun BottomActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LmcSpacing.BottomActionHeight)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = LmcSpacing.ScreenPadding, vertical = LmcSpacing.Space2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun StoryEyebrow(story: Story) {
    Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space1)) {
        Text(
            text = story.titleZh,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = story.titleEn,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun HeaderIconButton(
    icon: LmcIcon,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(LmcSpacing.MinTouchTarget)
            .semantics {
                this.contentDescription = contentDescription
            },
    ) {
        LmcCanvasIcon(
            icon = icon,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LmcProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    progressHeight: Dp = LmcSpacing.ProgressHeight,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(progressHeight)
            .clip(CircleShape),
        color = if (progress >= 1f) LmcColors.Success else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.tertiaryContainer,
    )
}

@Composable
private fun StepDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = LmcSpacing.MinTouchTarget)
            .wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selectedIndex) 12.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == selectedIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Composable
private fun OptionCircle(
    selected: Boolean,
    error: Boolean,
) {
    val color = when {
        error -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    Canvas(modifier = Modifier.size(24.dp)) {
        drawCircle(
            color = color,
            style = Stroke(width = 2.dp.toPx()),
        )
        if (selected) {
            drawCircle(
                color = color,
                radius = size.minDimension * 0.25f,
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(112.dp),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CenterStateMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(LmcSpacing.ScreenPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LibraryStateSurface(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    loading: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(LmcSpacing.ScreenPadding),
        contentAlignment = Alignment.Center,
    ) {
        LibraryInlineStateCard(
            title = title,
            body = body,
            actionLabel = actionLabel,
            onAction = onAction,
            loading = loading,
            modifier = Modifier.widthIn(max = LmcSpacing.ReadingMaxWidth),
        )
    }
}

@Composable
private fun LibraryInlineStateCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.RadiusLg),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = LmcSpacing.CardElevation,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun LmcCanvasIcon(
    icon: LmcIcon,
    color: Color,
    modifier: Modifier = Modifier.size(LmcSpacing.IconSize),
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = 2.4.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        when (icon) {
            LmcIcon.Audio -> {
                drawLine(color, Offset(size.width * 0.18f, size.height * 0.5f), Offset(size.width * 0.36f, size.height * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.36f, size.height * 0.34f), Offset(size.width * 0.54f, size.height * 0.24f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.54f, size.height * 0.24f), Offset(size.width * 0.54f, size.height * 0.76f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.54f, size.height * 0.76f), Offset(size.width * 0.36f, size.height * 0.66f), stroke.width, StrokeCap.Round)
                drawArc(color, -35f, 70f, false, Offset(size.width * 0.54f, size.height * 0.28f), Size(size.width * 0.28f, size.height * 0.44f), style = stroke)
            }
            LmcIcon.Back -> {
                drawLine(color, Offset(size.width * 0.62f, size.height * 0.22f), Offset(size.width * 0.34f, size.height * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.34f, size.height * 0.5f), Offset(size.width * 0.62f, size.height * 0.78f), stroke.width, StrokeCap.Round)
            }
            LmcIcon.Book,
            LmcIcon.Today -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.22f),
                    size = Size(size.width * 0.68f, size.height * 0.58f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    style = stroke,
                )
                drawLine(color, Offset(size.width * 0.5f, size.height * 0.24f), Offset(size.width * 0.5f, size.height * 0.78f), stroke.width, StrokeCap.Round)
            }
            LmcIcon.Check -> {
                drawLine(color, Offset(size.width * 0.22f, size.height * 0.54f), Offset(size.width * 0.42f, size.height * 0.72f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.42f, size.height * 0.72f), Offset(size.width * 0.78f, size.height * 0.28f), stroke.width, StrokeCap.Round)
            }
            LmcIcon.Close -> {
                drawLine(color, Offset(size.width * 0.28f, size.height * 0.28f), Offset(size.width * 0.72f, size.height * 0.72f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.72f, size.height * 0.28f), Offset(size.width * 0.28f, size.height * 0.72f), stroke.width, StrokeCap.Round)
            }
            LmcIcon.Library -> {
                drawRoundRect(color, Offset(size.width * 0.18f, size.height * 0.22f), Size(size.width * 0.64f, size.height * 0.12f), style = stroke)
                drawRoundRect(color, Offset(size.width * 0.18f, size.height * 0.44f), Size(size.width * 0.64f, size.height * 0.12f), style = stroke)
                drawRoundRect(color, Offset(size.width * 0.18f, size.height * 0.66f), Size(size.width * 0.64f, size.height * 0.12f), style = stroke)
            }
            LmcIcon.Next -> {
                drawLine(color, Offset(size.width * 0.38f, size.height * 0.22f), Offset(size.width * 0.66f, size.height * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.66f, size.height * 0.5f), Offset(size.width * 0.38f, size.height * 0.78f), stroke.width, StrokeCap.Round)
            }
            LmcIcon.Previous -> {
                drawLine(color, Offset(size.width * 0.62f, size.height * 0.22f), Offset(size.width * 0.34f, size.height * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.34f, size.height * 0.5f), Offset(size.width * 0.62f, size.height * 0.78f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.24f, size.height * 0.24f), Offset(size.width * 0.24f, size.height * 0.76f), stroke.width, StrokeCap.Round)
            }
            LmcIcon.Repeat -> {
                drawArc(color, 35f, 270f, false, Offset(size.width * 0.20f, size.height * 0.18f), Size(size.width * 0.60f, size.height * 0.54f), style = stroke)
                drawLine(color, Offset(size.width * 0.72f, size.height * 0.18f), Offset(size.width * 0.82f, size.height * 0.32f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.72f, size.height * 0.18f), Offset(size.width * 0.58f, size.height * 0.26f), stroke.width, StrokeCap.Round)
            }
            LmcIcon.Parent -> {
                drawRoundRect(color, Offset(size.width * 0.18f, size.height * 0.18f), Size(size.width * 0.64f, size.height * 0.64f), style = stroke)
                drawLine(color, Offset(size.width * 0.32f, size.height * 0.68f), Offset(size.width * 0.32f, size.height * 0.52f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.5f, size.height * 0.68f), Offset(size.width * 0.5f, size.height * 0.36f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.68f, size.height * 0.68f), Offset(size.width * 0.68f, size.height * 0.46f), stroke.width, StrokeCap.Round)
            }
            LmcIcon.PauseAudio -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.30f, size.height * 0.24f),
                    size = Size(size.width * 0.12f, size.height * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.58f, size.height * 0.24f),
                    size = Size(size.width * 0.12f, size.height * 0.52f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                )
            }
            LmcIcon.Settings -> {
                drawCircle(color = color, radius = size.minDimension * 0.22f, style = stroke)
                repeat(8) { tick ->
                    val angle = Math.toRadians((tick * 45).toDouble())
                    val inner = size.minDimension * 0.34f
                    val outer = size.minDimension * 0.42f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawLine(
                        color = color,
                        start = Offset(center.x + kotlin.math.cos(angle).toFloat() * inner, center.y + kotlin.math.sin(angle).toFloat() * inner),
                        end = Offset(center.x + kotlin.math.cos(angle).toFloat() * outer, center.y + kotlin.math.sin(angle).toFloat() * outer),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                    )
                }
            }
            LmcIcon.StopAudio -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.32f, size.height * 0.32f),
                    size = Size(size.width * 0.36f, size.height * 0.36f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun lmcFilterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun storyProgressLabel(
    progress: Float,
    completed: Boolean,
): String {
    return when {
        completed -> stringResource(R.string.library_completed)
        progress > 0f -> stringResource(R.string.story_progress_percent, formatPercent(progress))
        else -> stringResource(R.string.library_not_started)
    }
}

@StringRes
private fun LibraryStoryStatus.labelRes(): Int = when (this) {
    LibraryStoryStatus.New -> R.string.library_status_new
    LibraryStoryStatus.Continue -> R.string.library_status_continue
    LibraryStoryStatus.Done -> R.string.library_status_done
}

@StringRes
private fun LibraryStoryAction.labelRes(): Int = when (this) {
    LibraryStoryAction.Start -> R.string.action_start_reading
    LibraryStoryAction.Continue -> R.string.action_continue
    LibraryStoryAction.ReadAgain -> R.string.action_read_again_short
}

@StringRes
private fun String.seriesLabelRes(): Int = when (this) {
    "series_three_kingdoms" -> R.string.series_three_kingdoms
    else -> R.string.series_three_kingdoms
}

@StringRes
private fun VocabularyEntrySource.backActionLabelRes(): Int = when (this) {
    VocabularyEntrySource.Today -> R.string.action_back_to_today
    VocabularyEntrySource.Reading,
    VocabularyEntrySource.Quiz,
    -> R.string.action_back_to_reading
}

@Composable
private fun formatPercent(value: Float): String {
    val locale = currentLocale()
    val formatter = remember(locale) { NumberFormat.getPercentInstance(locale) }
    return formatter.format(value.toDouble())
}

@Composable
private fun rememberNumberFormat(): NumberFormat {
    val locale = currentLocale()
    return remember(locale) { NumberFormat.getIntegerInstance(locale) }
}

@Composable
private fun Int.toLocalizedInt(): String = rememberNumberFormat().format(this)

@Composable
private fun currentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return configuration.locales[0] ?: Locale.getDefault()
}

private fun Context.systemReaderLanguage(): ReaderLanguage {
    val locale = resources.configuration.locales[0] ?: Locale.getDefault()
    return if (locale.language.equals("zh", ignoreCase = true)) {
        ReaderLanguage.ChineseSimplified
    } else {
        ReaderLanguage.English
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(ReaderRoutes.Today) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@StringRes
private fun String.toStringResourceId(): Int = when (this) {
    AppInfoResourceKeys.AppName -> R.string.app_name
    else -> R.string.app_name
}

@StringRes
private fun ReaderLanguage.labelRes(): Int = when (this) {
    ReaderLanguage.English -> R.string.settings_language_en
    ReaderLanguage.ChineseSimplified -> R.string.settings_language_zh_hans
}

@StringRes
private fun ReadingTextSize.labelRes(): Int = when (this) {
    ReadingTextSize.Small -> R.string.reading_text_size_small
    ReadingTextSize.Medium -> R.string.reading_text_size_medium
    ReadingTextSize.Large -> R.string.reading_text_size_large
}

@StringRes
private fun ReadingPlaybackSpeed.labelRes(): Int = when (this) {
    ReadingPlaybackSpeed.Slow -> R.string.reading_speed_slow
    ReadingPlaybackSpeed.DefaultSlow -> R.string.reading_speed_default
    ReadingPlaybackSpeed.Normal -> R.string.reading_speed_normal
}

private enum class WordBookFilter {
    All,
    Due,
    Learning,
    Known,
}

@StringRes
private fun WordBookFilter.labelRes(): Int = when (this) {
    WordBookFilter.All -> R.string.word_book_filter_all
    WordBookFilter.Due -> R.string.word_book_filter_due
    WordBookFilter.Learning -> R.string.word_book_filter_learning
    WordBookFilter.Known -> R.string.word_book_filter_known
}

private sealed interface StoriesState {
    data object Loading : StoriesState
    data object Error : StoriesState
    data class Ready(val stories: List<Story>) : StoriesState
}

private data class Metric(
    val label: String,
    val value: String,
)

private const val UnknownAppVersion = "unknown"

private class ReaderAnalytics(
    private val delegate: Analytics,
    private val scope: CoroutineScope,
) {
    fun track(payload: AnalyticsEventPayload) {
        scope.launch {
            runCatching {
                delegate.track(
                    eventName = payload.eventName,
                    properties = payload.properties,
                )
            }
        }
    }

    fun track(
        eventName: AnalyticsEventName,
        properties: Map<String, Any?> = emptyMap(),
    ) {
        scope.launch {
            runCatching {
                delegate.track(
                    eventName = eventName,
                    properties = properties.toAnalyticsProperties(),
                )
            }
        }
    }
}

private fun ReaderAnalytics.trackStoryOpen(
    stories: List<Story>,
    storyId: String,
    openSource: String,
) {
    val storyIndex = stories.indexOfFirst { it.id == storyId }
    val story = stories.getOrNull(storyIndex) ?: return
    trackStoryOpen(
        story = story,
        storyOrder = storyIndex + 1,
        openSource = openSource,
    )
}

private fun ReaderAnalytics.trackStoryOpen(
    story: Story,
    storyOrder: Int,
    openSource: String,
) {
    track(
        ReaderAnalyticsEvents.storyOpen(
            story = story,
            storyOrder = storyOrder,
            openSource = openSource,
        ),
    )
}

private fun ReaderAnalytics.trackWordReviewAnswer(
    item: WordBookItem,
    rating: String,
    reviewIndex: Int,
) {
    val storyId = item.sourceStoryIds.firstOrNull().orEmpty()
    track(
        ReaderAnalyticsEvents.wordReviewAnswer(
            storyId = storyId,
            vocabId = "$storyId:word_book",
            rating = rating,
            reviewIndex = reviewIndex,
            nextIntervalDays = item.intervalDays,
        ),
    )
}

private class AndroidAiExplainBackendClient : AiExplainBackendClient {
    override suspend fun postExplain(
        baseUrl: String,
        apiKey: String?,
        request: AiExplanationRequest,
    ): AiExplanationResponse = withContext(Dispatchers.IO) {
        val connection = (URL("${baseUrl.trimEnd('/')}/ai/explain").openConnection() as HttpURLConnection)
            .apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (!apiKey.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
            }

        try {
            val body = JSONObject()
                .put("story_id", request.storyId)
                .put("selected_text", request.selectedText)
                .put("question_type", request.questionType)
                .put("child_age", request.childAge)
                .toString()
                .toByteArray(StandardCharsets.UTF_8)

            connection.outputStream.use { output ->
                output.write(body)
            }

            val responseCode = connection.responseCode
            val responseBody = (if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
                reader.readText()
            }.orEmpty()

            if (responseCode !in 200..299) {
                throw IOException("AI backend returned HTTP $responseCode")
            }

            val responseJson = JSONObject(responseBody)
            val answer = responseJson.optString("answer")

            if (answer.isBlank()) {
                throw IOException("AI backend response missing answer")
            }

            AiExplanationResponse(answer = answer)
        } finally {
            connection.disconnect()
        }
    }
}

private fun Map<String, Any?>.toAnalyticsProperties() =
    mapNotNull { (key, value) ->
        val element = when (value) {
            is Boolean -> AnalyticsProperties.boolean(value)
            is Int -> AnalyticsProperties.int(value)
            is Long -> AnalyticsProperties.long(value)
            is Double -> AnalyticsProperties.double(value)
            is Float -> AnalyticsProperties.double(value.toDouble())
            is String -> AnalyticsProperties.string(value)
            null -> null
            else -> AnalyticsProperties.string(value.toString())
        }
        element?.let { key to it }
    }.toMap()

private fun Context.appVersionName(): String =
    runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName ?: UnknownAppVersion
    }.getOrDefault(UnknownAppVersion)

private sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Answer(val text: String) : AiUiState
    data object Error : AiUiState
}

private data class FeedbackChoice<T>(
    val value: T,
    @StringRes val labelRes: Int,
)

@StringRes
private fun FeedbackOption<SharedFeedbackSatisfaction>.satisfactionLabelRes(): Int =
    when (id) {
        "satisfied" -> R.string.feedback_satisfaction_satisfied
        "okay" -> R.string.feedback_satisfaction_okay
        else -> R.string.feedback_satisfaction_not_satisfied
    }

@StringRes
private fun FeedbackOption<SharedFeedbackChildAgeBand>.ageBandLabelRes(): Int =
    when (id) {
        "5_6" -> R.string.feedback_age_5_6
        "7_8" -> R.string.feedback_age_7_8
        else -> R.string.feedback_age_other
    }

@StringRes
private fun FeedbackOption<SharedFeedbackIssueType>.issueTypeLabelRes(): Int =
    when (id) {
        "content" -> R.string.feedback_issue_content
        "audio" -> R.string.feedback_issue_audio
        "ai" -> R.string.feedback_issue_ai
        "progress" -> R.string.feedback_issue_progress
        else -> R.string.feedback_issue_other
    }

private data class LocalizedEnvironment(
    val context: Context,
    val configuration: Configuration,
)

private fun Context.localizedEnvironment(language: ReaderLanguage): LocalizedEnvironment {
    val locale = Locale.forLanguageTag(language.tag)
    Locale.setDefault(locale)
    val configuration = Configuration(resources.configuration).apply {
        setLocales(LocaleList(locale))
    }
    return LocalizedEnvironment(
        context = createConfigurationContext(configuration),
        configuration = configuration,
    )
}

internal fun karaokeTextFontWeight(
    style: TextStyle,
    isActiveChar: Boolean,
): FontWeight? = style.fontWeight

internal fun karaokeCellHorizontalPadding(isActiveChar: Boolean): Dp =
    LmcSpacing.Space1

internal fun sentenceSpeakerTouchTargetSize(): Dp =
    LmcSpacing.MinTouchTarget

internal fun sentenceSpeakerVisualSize(): Dp =
    LmcSpacing.SentenceSpeakerButtonSize

private data class ReadingTypeStyles(
    val hanzi: TextStyle,
    val pinyin: TextStyle,
)

@Composable
private fun readingTypeStyles(textSize: ReadingTextSize): ReadingTypeStyles {
    return when (textSize) {
        ReadingTextSize.Small -> ReadingTypeStyles(
            hanzi = TextStyle(fontSize = 22.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium),
            pinyin = TextStyle(fontSize = 13.sp, lineHeight = 22.sp),
        )
        ReadingTextSize.Medium -> ReadingTypeStyles(
            hanzi = TextStyle(fontSize = 26.sp, lineHeight = 42.sp, fontWeight = FontWeight.Medium),
            pinyin = TextStyle(fontSize = 15.sp, lineHeight = 24.sp),
        )
        ReadingTextSize.Large -> ReadingTypeStyles(
            hanzi = TextStyle(fontSize = 30.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium),
            pinyin = TextStyle(fontSize = 17.sp, lineHeight = 28.sp),
        )
    }
}

private enum class LmcIcon {
    Audio,
    Back,
    Book,
    Check,
    Close,
    Library,
    Next,
    Parent,
    PauseAudio,
    Previous,
    Repeat,
    Settings,
    StopAudio,
    Today,
}

private object LmcColors {
    val Success = Color(0xFF3B7A3B)
    val SuccessContainer = Color(0xFFE1F3DC)
}

private object LmcMotion {
    const val MediumMillis = 180
    const val CelebrationMillis = 720
}

private data class CompletionParticle(
    val x: Float,
    val y: Float,
    val size: Dp,
    val lift: Dp,
    val color: Color,
)

private val CompletionParticles = listOf(
    CompletionParticle(0.16f, 0.18f, 8.dp, 34.dp, Color(0xFFB84535)),
    CompletionParticle(0.28f, 0.12f, 7.dp, 28.dp, Color(0xFF126B68)),
    CompletionParticle(0.40f, 0.20f, 6.dp, 30.dp, Color(0xFF8A6100)),
    CompletionParticle(0.62f, 0.14f, 8.dp, 34.dp, Color(0xFF3B7A3B)),
    CompletionParticle(0.76f, 0.22f, 7.dp, 28.dp, Color(0xFFB84535)),
    CompletionParticle(0.86f, 0.32f, 6.dp, 24.dp, Color(0xFF126B68)),
    CompletionParticle(0.12f, 0.38f, 6.dp, 20.dp, Color(0xFF8A6100)),
    CompletionParticle(0.30f, 0.42f, 8.dp, 26.dp, Color(0xFF3B7A3B)),
    CompletionParticle(0.72f, 0.46f, 8.dp, 24.dp, Color(0xFFB84535)),
    CompletionParticle(0.90f, 0.52f, 7.dp, 22.dp, Color(0xFF126B68)),
    CompletionParticle(0.20f, 0.64f, 7.dp, 16.dp, Color(0xFF3B7A3B)),
    CompletionParticle(0.82f, 0.68f, 6.dp, 18.dp, Color(0xFF8A6100)),
)

private object LmcSpacing {
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val ScreenPadding = 16.dp
    val ReadingMaxWidth = 720.dp
    val LibraryMaxWidth = 960.dp
    val CardPadding = 16.dp
    val ReadingPanelPadding = 16.dp
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val CardRadius = 12.dp
    val ReadingPanelRadius = 16.dp
    val QuizOptionRadius = 12.dp
    val ButtonPrimaryHeight = 56.dp
    val ButtonSecondaryHeight = 48.dp
    val BottomActionHeight = 72.dp
    val ChipHeight = 40.dp
    val MinTouchTarget = 48.dp
    val SentenceSpeakerButtonSize = 36.dp
    val SentenceSpeakerIconSize = 18.dp
    val TopAppBarHeight = 64.dp
    val IconSize = 24.dp
    val StoryCoverList = 96.dp
    val StoryCoverHero = 148.dp
    val ProgressHeight = 8.dp
    val QuizOptionMinHeight = 56.dp
    val CardElevation = 1.dp
}

private val LmcLightColorScheme = lightColorScheme(
    primary = Color(0xFFB84535),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0D6),
    onPrimaryContainer = Color(0xFF3D0E08),
    secondary = Color(0xFF126B68),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F1EE),
    onSecondaryContainer = Color(0xFF063432),
    tertiary = Color(0xFF8A6100),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF4D8),
    onTertiaryContainer = Color(0xFF7A4E00),
    background = Color(0xFFFFF8EC),
    onBackground = Color(0xFF202523),
    surface = Color.White,
    onSurface = Color(0xFF202523),
    surfaceVariant = Color(0xFFF3F7F1),
    onSurfaceVariant = Color(0xFF4F5E58),
    outline = Color(0xFF9AA7A0),
    outlineVariant = Color(0xFFD7DED8),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val LmcTypography = Typography(
    displaySmall = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

private val LmcShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
private fun LittleMandarinClassicsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LmcLightColorScheme,
        typography = LmcTypography,
        shapes = LmcShapes,
        content = content,
    )
}

private const val SevenDaysMillis: Long = 7L * 24L * 60L * 60L * 1_000L
