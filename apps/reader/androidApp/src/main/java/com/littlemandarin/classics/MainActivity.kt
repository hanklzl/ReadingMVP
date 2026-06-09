package com.littlemandarin.classics

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.littlemandarin.classics.shared.feedback.FeedbackSubmission
import com.littlemandarin.classics.shared.feedback.FeedbackSatisfaction as SharedFeedbackSatisfaction
import com.littlemandarin.classics.shared.feedback.createPlatformFeedbackService
import com.littlemandarin.classics.shared.progress.AndroidProgressServiceProvider
import com.littlemandarin.classics.shared.progress.BuildParentReportUseCase
import com.littlemandarin.classics.shared.progress.CompletionRecord
import com.littlemandarin.classics.shared.progress.GetProgressStatsUseCase
import com.littlemandarin.classics.shared.progress.MarkStoryCompletedUseCase
import com.littlemandarin.classics.shared.progress.ParentProgressReport
import com.littlemandarin.classics.shared.progress.ProgressService
import com.littlemandarin.classics.shared.progress.ProgressStats
import com.littlemandarin.classics.shared.progress.createPlatformProgressService
import com.littlemandarin.classics.shared.quiz.ScoreQuizUseCase
import com.littlemandarin.classics.shared.service.AiExplainBackendClient
import com.littlemandarin.classics.shared.service.AiExplanationRequest
import com.littlemandarin.classics.shared.service.AiExplanationResponse
import com.littlemandarin.classics.shared.service.AiServiceConfig
import com.littlemandarin.classics.shared.service.AiMessageKeys
import com.littlemandarin.classics.shared.service.AiQuestionTypes
import com.littlemandarin.classics.shared.service.AiService
import com.littlemandarin.classics.shared.service.AndroidTtsServiceProvider
import com.littlemandarin.classics.shared.service.TtsService
import com.littlemandarin.classics.shared.service.createAiService
import com.littlemandarin.classics.shared.service.createTtsService
import com.littlemandarin.classics.shared.story.DefaultStoryRepository
import com.littlemandarin.classics.shared.story.Question
import com.littlemandarin.classics.shared.story.Story
import com.littlemandarin.classics.shared.story.Vocab
import com.littlemandarin.classics.shared.usecase.GetStoryListUseCase
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAnalyticsServiceProvider.initialize(applicationContext)
        AndroidFeedbackServiceProvider.initialize(applicationContext)
        AndroidProgressServiceProvider.initialize(applicationContext)
        AndroidTtsServiceProvider.initialize(applicationContext)

        setContent {
            ReaderApp()
        }
    }
}

private object ReaderRoutes {
    const val Today = "today"
    const val Library = "library"
    const val Parent = "parent"
    const val Settings = "settings"

    const val Reading = "story/{storyId}/read"
    const val Vocabulary = "story/{storyId}/vocabulary"
    const val Quiz = "story/{storyId}/quiz"

    fun reading(storyId: String): String = "story/$storyId/read"

    fun vocabulary(storyId: String): String = "story/$storyId/vocabulary"

    fun quiz(storyId: String): String = "story/$storyId/quiz"

    fun isReadingFlow(route: String?): Boolean = route?.startsWith("story/") == true
}

private data class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: LmcIcon,
)

private val TopLevelDestinations = listOf(
    TopLevelDestination(ReaderRoutes.Today, R.string.nav_today, LmcIcon.Today),
    TopLevelDestination(ReaderRoutes.Library, R.string.nav_library, LmcIcon.Library),
    TopLevelDestination(ReaderRoutes.Parent, R.string.nav_parent, LmcIcon.Parent),
    TopLevelDestination(ReaderRoutes.Settings, R.string.nav_settings, LmcIcon.Settings),
)

@Composable
private fun ReaderApp() {
    val baseContext = LocalContext.current
    val settingsStore = remember(baseContext.applicationContext) {
        ReaderSettingsStore(baseContext.applicationContext)
    }
    val appVersion = remember(baseContext.applicationContext) {
        baseContext.applicationContext.appVersionName()
    }
    var settings by remember { mutableStateOf(settingsStore.read()) }
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

    CompositionLocalProvider(
        LocalContext provides localizedEnvironment.context,
        LocalConfiguration provides localizedEnvironment.configuration,
    ) {
        LittleMandarinClassicsTheme {
            ReaderAppContent(
                settings = settings,
                settingsStore = settingsStore,
                analytics = analytics,
                feedbackService = feedbackService,
                readingProgressVersion = readingProgressVersion,
                onLanguageChange = { language ->
                    settingsStore.setLanguage(language)
                    settings = settings.copy(language = language)
                },
                onPinyinDefaultChange = { showPinyin ->
                    settingsStore.setShowPinyinByDefault(showPinyin)
                    settings = settings.copy(showPinyinByDefault = showPinyin)
                },
                onTextSizeChange = { textSize ->
                    settingsStore.setReadingTextSize(textSize)
                    settings = settings.copy(readingTextSize = textSize)
                },
                onAiBackendBaseUrlChange = { baseUrl ->
                    settingsStore.setAiBackendBaseUrl(baseUrl)
                    settings = settings.copy(aiBackendBaseUrl = baseUrl)
                },
                onReadingPositionChange = { storyId, paragraphIndex ->
                    settingsStore.setReadingParagraphIndex(storyId, paragraphIndex)
                    readingProgressVersion += 1
                },
            )
        }
    }
}

@Composable
private fun ReaderAppContent(
    settings: ReaderSettings,
    settingsStore: ReaderSettingsStore,
    analytics: ReaderAnalytics,
    feedbackService: FeedbackService,
    readingProgressVersion: Int,
    onLanguageChange: (ReaderLanguage) -> Unit,
    onPinyinDefaultChange: (Boolean) -> Unit,
    onTextSizeChange: (ReadingTextSize) -> Unit,
    onAiBackendBaseUrlChange: (String) -> Unit,
    onReadingPositionChange: (String, Int) -> Unit,
) {
    val navController = rememberNavController()
    val appInfo = remember { GetAppInfoUseCase().invoke() }
    val repository = remember { DefaultStoryRepository() }
    val progressService = remember { createPlatformProgressService() }
    val ttsService = remember { createTtsService() }
    val aiService = remember(settings.aiBackendBaseUrl) {
        createAiService(
            config = AiServiceConfig(
                baseUrl = settings.aiBackendBaseUrl.takeUnless { it.isMockAiBackend() },
            ),
            backendClient = AndroidAiExplainBackendClient(),
        )
    }
    val progressRecords by progressService.records.collectAsState(initial = emptyList())
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

    LaunchedEffect(repository) {
        storiesState = runCatching {
            StoriesState.Ready(GetStoryListUseCase(repository).invoke())
        }.getOrElse {
            StoriesState.Error
        }
    }

    LaunchedEffect(Unit) {
        analytics.track(
            eventName = AnalyticsEventName.AppOpen,
            properties = mapOf("open_type" to "cold_start"),
        )
    }

    LaunchedEffect(progressService, progressRecords) {
        progressStats = GetProgressStatsUseCase(progressService).invoke()
        parentReport = BuildParentReportUseCase(progressService).invoke(
            nowEpochMillis = System.currentTimeMillis(),
        )
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomBar = !ReaderRoutes.isReadingFlow(currentRoute)
    val snackbarHostState = remember { SnackbarHostState() }

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
                            analytics.track(
                                eventName = AnalyticsEventName.ParentReportOpen,
                                properties = mapOf(
                                    "entry_point" to "bottom_navigation",
                                    "report_period" to "week",
                                ),
                            )
                        }
                        navController.navigateTopLevel(destination.route)
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ReaderRoutes.Today,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(ReaderRoutes.Today) {
                StoryStateContent(storiesState = storiesState) { stories ->
                    TodayScreen(
                        appInfo = appInfo,
                        stories = stories,
                        progressStats = progressStats,
                        progressRecords = progressRecords,
                        settingsStore = settingsStore,
                        readingProgressVersion = readingProgressVersion,
                        snackbarHostState = snackbarHostState,
                        onRead = { storyId ->
                            analytics.trackStoryOpen(stories, storyId, openSource = "today")
                            navController.navigate(ReaderRoutes.reading(storyId))
                        },
                        onVocabulary = { storyId ->
                            navController.navigate(ReaderRoutes.vocabulary(storyId))
                        },
                        onQuiz = { storyId -> navController.navigate(ReaderRoutes.quiz(storyId)) },
                        onParent = {
                            analytics.track(
                                eventName = AnalyticsEventName.ParentReportOpen,
                                properties = mapOf(
                                    "entry_point" to "today_header",
                                    "report_period" to "week",
                                ),
                            )
                            navController.navigateTopLevel(ReaderRoutes.Parent)
                        },
                        onSettings = { navController.navigateTopLevel(ReaderRoutes.Settings) },
                    )
                }
            }
            composable(ReaderRoutes.Library) {
                StoryStateContent(storiesState = storiesState) { stories ->
                    LibraryScreen(
                        stories = stories,
                        progressRecords = progressRecords,
                        settingsStore = settingsStore,
                        readingProgressVersion = readingProgressVersion,
                        onRead = { storyId ->
                            analytics.trackStoryOpen(stories, storyId, openSource = "library")
                            navController.navigate(ReaderRoutes.reading(storyId))
                        },
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
                        progressRecords = progressRecords,
                        settingsStore = settingsStore,
                        readingProgressVersion = readingProgressVersion,
                        onStoryClick = { storyId ->
                            analytics.trackStoryOpen(stories, storyId, openSource = "parent_report")
                            navController.navigate(ReaderRoutes.reading(storyId))
                        },
                        onSettings = { navController.navigateTopLevel(ReaderRoutes.Settings) },
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
                    onParentReport = {
                        analytics.track(
                            eventName = AnalyticsEventName.ParentReportOpen,
                            properties = mapOf(
                                "entry_point" to "settings",
                                "report_period" to "week",
                            ),
                        )
                        navController.navigateTopLevel(ReaderRoutes.Parent)
                    },
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
                        ttsService = ttsService,
                        initialParagraphIndex = settingsStore.readReadingParagraphIndex(story.id)
                            .coerceAtLeast(0),
                        onClose = { navController.popBackStack() },
                        onTextSizeChange = onTextSizeChange,
                        onPinyinDefaultChange = onPinyinDefaultChange,
                        onReadingPositionChange = onReadingPositionChange,
                        onVocabulary = { navController.navigate(ReaderRoutes.vocabulary(story.id)) },
                    )
                }
            }
            composable(
                route = ReaderRoutes.Vocabulary,
                arguments = listOf(navArgument("storyId") { type = NavType.StringType }),
            ) { backStackEntry ->
                StoryRouteContent(
                    storiesState = storiesState,
                    storyId = backStackEntry.arguments?.getString("storyId"),
                ) { story, _ ->
                    VocabularyScreen(
                        story = story,
                        analytics = analytics,
                        ttsService = ttsService,
                        onBack = { navController.popBackStack() },
                        onQuiz = { navController.navigate(ReaderRoutes.quiz(story.id)) },
                    )
                }
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
                        onBack = { navController.popBackStack() },
                        onReadAgain = {
                            analytics.trackStoryOpen(
                                story = story,
                                storyOrder = storyOrder,
                                openSource = "quiz_completion",
                            )
                            navController.navigate(ReaderRoutes.reading(story.id)) {
                                popUpTo(ReaderRoutes.Today)
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
private fun TodayScreen(
    appInfo: AppInfo,
    stories: List<Story>,
    progressStats: ProgressStats,
    progressRecords: List<CompletionRecord>,
    settingsStore: ReaderSettingsStore,
    readingProgressVersion: Int,
    snackbarHostState: SnackbarHostState,
    onRead: (String) -> Unit,
    onVocabulary: (String) -> Unit,
    onQuiz: (String) -> Unit,
    onParent: () -> Unit,
    onSettings: () -> Unit,
) {
    val todayStory = stories.first()
    val upNextStory = stories.drop(1).firstOrNull()
    val todayCompleted = progressRecords.any { it.storyId == todayStory.id }
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
                progress = progressFraction(
                    story = todayStory,
                    progressRecords = progressRecords,
                    settingsStore = settingsStore,
                    readingProgressVersion = readingProgressVersion,
                ),
                onClick = { onRead(todayStory.id) },
            )
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
                        if (todayCompleted) {
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
        if (upNextStory != null) {
            item {
                SectionTitle(text = stringResource(R.string.today_up_next))
                Spacer(modifier = Modifier.height(LmcSpacing.Space2))
                StoryCompactRow(
                    story = upNextStory,
                    progress = progressFraction(
                        story = upNextStory,
                        progressRecords = progressRecords,
                        settingsStore = settingsStore,
                        readingProgressVersion = readingProgressVersion,
                    ),
                    completed = progressRecords.any { it.storyId == upNextStory.id },
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
    settingsStore: ReaderSettingsStore,
    readingProgressVersion: Int,
    onRead: (String) -> Unit,
    onSettings: () -> Unit,
) {
    var selectedLevel by remember { mutableStateOf<Int?>(null) }
    val filteredStories = remember(stories, selectedLevel) {
        selectedLevel?.let { level -> stories.filter { it.level == level } } ?: stories
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
        if (filteredStories.isEmpty()) {
            item {
                CenterStateMessage(text = stringResource(R.string.library_empty))
            }
        } else {
            item {
                AdaptiveStoryList(
                    stories = filteredStories,
                    progressRecords = progressRecords,
                    settingsStore = settingsStore,
                    readingProgressVersion = readingProgressVersion,
                    onRead = onRead,
                )
            }
        }
    }
}

@Composable
private fun AdaptiveStoryList(
    stories: List<Story>,
    progressRecords: List<CompletionRecord>,
    settingsStore: ReaderSettingsStore,
    readingProgressVersion: Int,
    onRead: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useTwoColumns = maxWidth >= 720.dp
        if (useTwoColumns) {
            Column(verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space4)) {
                stories.chunked(2).forEach { rowStories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
                    ) {
                        rowStories.forEach { story ->
                            StoryListCard(
                                story = story,
                                completed = progressRecords.any { it.storyId == story.id },
                                progress = progressFraction(
                                    story = story,
                                    progressRecords = progressRecords,
                                    settingsStore = settingsStore,
                                    readingProgressVersion = readingProgressVersion,
                                ),
                                modifier = Modifier.weight(1f),
                                onClick = { onRead(story.id) },
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
                stories.forEach { story ->
                    StoryListCard(
                        story = story,
                        completed = progressRecords.any { it.storyId == story.id },
                        progress = progressFraction(
                            story = story,
                            progressRecords = progressRecords,
                            settingsStore = settingsStore,
                            readingProgressVersion = readingProgressVersion,
                        ),
                        onClick = { onRead(story.id) },
                    )
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
    ttsService: TtsService,
    initialParagraphIndex: Int,
    onClose: () -> Unit,
    onTextSizeChange: (ReadingTextSize) -> Unit,
    onPinyinDefaultChange: (Boolean) -> Unit,
    onReadingPositionChange: (String, Int) -> Unit,
    onVocabulary: () -> Unit,
) {
    val paragraphCount = story.paragraphs.size.coerceAtLeast(1)
    var paragraphIndex by remember(story.id) {
        mutableIntStateOf(initialParagraphIndex.coerceIn(0, paragraphCount - 1))
    }
    var isSpeaking by remember(story.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val paragraph = story.paragraphs.getOrNull(paragraphIndex)
    val readingType = readingTypeStyles(settings.readingTextSize)
    var aiState by remember(story.id, paragraphIndex) {
        mutableStateOf<AiUiState>(AiUiState.Idle)
    }
    val aiStubText = stringResource(R.string.ai_answer_stub)
    val aiOutOfScopeText = stringResource(R.string.prompt_story_only_reply)

    LaunchedEffect(story.id, paragraphIndex) {
        onReadingPositionChange(story.id, paragraphIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ReadingTopBar(
            story = story,
            paragraphIndex = paragraphIndex,
            paragraphCount = paragraphCount,
            isSpeaking = isSpeaking,
            onClose = {
                scope.launch {
                    ttsService.stop()
                }
                onClose()
            },
            onAudioClick = {
                val textToRead = paragraph?.text.orEmpty()
                scope.launch {
                    if (isSpeaking) {
                        ttsService.stop()
                        isSpeaking = false
                    } else {
                        analytics.trackParagraphAudioPlay(
                            storyId = story.id,
                            paragraphIndex = paragraphIndex,
                            targetType = "paragraph",
                        )
                        isSpeaking = true
                        ttsService.speak(textToRead)
                    }
                }
            },
        )
        ReadingControls(
            showPinyin = settings.showPinyinByDefault,
            readingTextSize = settings.readingTextSize,
            onPinyinChange = { enabled ->
                analytics.track(
                    eventName = AnalyticsEventName.PinyinToggle,
                    properties = mapOf(
                        "story_id" to story.id,
                        "enabled" to enabled,
                        "surface" to "reading",
                        "paragraph_index" to paragraphIndex,
                    ),
                )
                onPinyinDefaultChange(enabled)
            },
            onTextSizeChange = onTextSizeChange,
        )
        ReadingFullAudioRow(
            isSpeaking = isSpeaking,
            onClick = {
                scope.launch {
                    if (isSpeaking) {
                        ttsService.stop()
                        isSpeaking = false
                    } else {
                        analytics.trackParagraphAudioPlay(
                            storyId = story.id,
                            paragraphIndex = paragraphIndex,
                            targetType = "full_story",
                        )
                        isSpeaking = true
                        ttsService.speak(story.paragraphs.joinToString(separator = "\n") { it.text })
                    }
                }
            },
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
                    .padding(
                        horizontal = LmcSpacing.ScreenPadding,
                        vertical = LmcSpacing.Space4,
                    ),
            ) {
                if (paragraph != null) {
                    PinyinTextBlock(
                        text = paragraph.text,
                        pinyin = paragraph.pinyin,
                        showPinyin = settings.showPinyinByDefault,
                        readingType = readingType,
                    )
                    Spacer(modifier = Modifier.height(LmcSpacing.Space4))
                    AskExplanationCard(
                        state = aiState,
                        enabled = paragraph.text.isNotBlank(),
                        onAsk = {
                            val selectedText = paragraph.text.take(AiSelectedTextMaxChars)
                            aiState = AiUiState.Loading
                            scope.launch {
                                val request = AiExplanationRequest(
                                    storyId = story.id,
                                    selectedText = selectedText,
                                    questionType = AiQuestionTypes.ExplainSentence,
                                )
                                runCatching {
                                    aiService.explain(request)
                                }.onSuccess { response ->
                                    val answer = response.toDisplayText(
                                        stubText = aiStubText,
                                        outOfScopeText = aiOutOfScopeText,
                                    ).limitAiAnswer()
                                    analytics.trackAiExplainRequest(
                                        storyId = story.id,
                                        requestType = AiQuestionTypes.ExplainSentence,
                                        targetType = "paragraph",
                                        safetyOutcome = response.safetyOutcome(answer, aiOutOfScopeText),
                                    )
                                    aiState = AiUiState.Answer(answer)
                                }.onFailure {
                                    analytics.trackAiExplainRequest(
                                        storyId = story.id,
                                        requestType = AiQuestionTypes.ExplainSentence,
                                        targetType = "paragraph",
                                        safetyOutcome = "error",
                                    )
                                    aiState = AiUiState.Error
                                }
                            }
                        },
                    )
                }
            }
        }
        BottomActionRow {
            OutlinedButton(
                enabled = paragraphIndex > 0,
                onClick = { paragraphIndex -= 1 },
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
            ) {
                Text(text = stringResource(R.string.action_previous))
            }
            Button(
                onClick = {
                    if (paragraphIndex < paragraphCount - 1) {
                        paragraphIndex += 1
                    } else {
                        scope.launch {
                            ttsService.stop()
                            isSpeaking = false
                        }
                        onVocabulary()
                    }
                },
                modifier = Modifier.heightIn(min = LmcSpacing.ButtonPrimaryHeight),
                shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            ) {
                Text(text = stringResource(R.string.action_next))
            }
        }
    }
}

@Composable
private fun VocabularyScreen(
    story: Story,
    analytics: ReaderAnalytics,
    ttsService: TtsService,
    onBack: () -> Unit,
    onQuiz: () -> Unit,
) {
    val words = story.vocab
    val wordCount = words.size.coerceAtLeast(1)
    var wordIndex by remember(story.id) { mutableIntStateOf(0) }
    val currentWord = words.getOrNull(wordIndex)
    val scope = rememberCoroutineScope()

    LaunchedEffect(story.id, wordIndex, currentWord) {
        if (currentWord != null) {
            analytics.track(
                eventName = AnalyticsEventName.VocabOpen,
                properties = mapOf(
                    "story_id" to story.id,
                    "vocab_id" to story.vocabIdFor(wordIndex),
                    "open_source" to "vocabulary_screen",
                    "content_level" to story.level,
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
                    CenterStateMessage(text = stringResource(R.string.vocab_empty))
                } else {
                    VocabularyCard(
                        word = currentWord,
                        onAudioClick = {
                            scope.launch {
                                ttsService.speak(currentWord.speechText())
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

@Composable
private fun QuizScreen(
    story: Story,
    storyOrder: Int,
    analytics: ReaderAnalytics,
    progressService: ProgressService,
    onBack: () -> Unit,
    onReadAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val questions = story.questions
    val questionCount = questions.size.coerceAtLeast(1)
    var questionIndex by remember(story.id) { mutableIntStateOf(0) }
    var answers by remember(story.id) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var submittedQuestionIds by remember(story.id) { mutableStateOf<Set<String>>(emptySet()) }
    var showCompletion by remember(story.id) { mutableStateOf(false) }
    val score = remember(story.id, answers) {
        ScoreQuizUseCase().invoke(story, answers)
    }

    LaunchedEffect(story.id) {
        analytics.track(
            eventName = AnalyticsEventName.QuizStart,
            properties = mapOf(
                "story_id" to story.id,
                "question_count" to questions.size,
            ),
        )
    }

    LaunchedEffect(showCompletion) {
        if (showCompletion) {
            MarkStoryCompletedUseCase(progressService).invoke(
                CompletionRecord(
                    storyId = story.id,
                    completedAtEpochMillis = System.currentTimeMillis(),
                    vocabCount = story.vocab.size,
                    correctCount = score.correctCount,
                    questionCount = score.totalQuestions,
                ),
            )
            analytics.track(
                eventName = AnalyticsEventName.QuizComplete,
                properties = mapOf(
                    "story_id" to story.id,
                    "question_count" to score.totalQuestions,
                    "correct_count" to score.correctCount,
                ),
            )
            analytics.track(
                eventName = AnalyticsEventName.StoryComplete,
                properties = mapOf(
                    "story_id" to story.id,
                    "story_order" to storyOrder,
                    "content_level" to story.level,
                    "quiz_completed" to true,
                ),
            )
        }
    }

    if (showCompletion) {
        QuizCompletionScreen(
            story = story,
            correctCount = score.correctCount,
            totalQuestions = score.totalQuestions,
            onReadAgain = onReadAgain,
            onDone = onDone,
        )
        return
    }

    val question = questions.getOrNull(questionIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        FlowTopBar(
            title = stringResource(R.string.quiz_title),
            trailing = stringResource(
                R.string.quiz_progress_count,
                (questionIndex + 1).coerceAtMost(questionCount),
                questionCount,
            ),
            onBack = onBack,
        )
        LmcProgressBar(
            progress = (questionIndex + 1).toFloat() / questionCount.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LmcSpacing.ScreenPadding),
        )
        if (question == null) {
            CenterStateMessage(text = stringResource(R.string.quiz_empty))
        } else {
            QuizQuestionBody(
                question = question,
                selectedAnswer = answers[question.id],
                submitted = submittedQuestionIds.contains(question.id),
                onSelectAnswer = { answer ->
                    if (!submittedQuestionIds.contains(question.id)) {
                        answers = answers + (question.id to answer)
                    }
                },
                modifier = Modifier.weight(1f),
            )
            BottomActionRow {
                Spacer(modifier = Modifier.weight(1f))
                val submitted = submittedQuestionIds.contains(question.id)
                Button(
                    enabled = submitted || answers[question.id] != null,
                    onClick = {
                        if (!submitted) {
                            submittedQuestionIds = submittedQuestionIds + question.id
                        } else if (questionIndex < questions.lastIndex) {
                            questionIndex += 1
                        } else {
                            showCompletion = true
                        }
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
    progressRecords: List<CompletionRecord>,
    settingsStore: ReaderSettingsStore,
    readingProgressVersion: Int,
    onStoryClick: (String) -> Unit,
    onSettings: () -> Unit,
) {
    val numberFormat = rememberNumberFormat()
    val readingDaysThisWeek = remember(parentReport) {
        parentReport.recentCompletions
            .filter {
                it.completedAtEpochMillis >= System.currentTimeMillis() - SevenDaysMillis
            }
            .map { TimeUnit.MILLISECONDS.toDays(it.completedAtEpochMillis) }
            .distinct()
            .size
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
        item {
            ParentGateNotice()
        }
        item {
            SectionTitle(text = stringResource(R.string.parent_this_week))
            Spacer(modifier = Modifier.height(LmcSpacing.Space2))
            MetricGrid(
                metrics = listOf(
                    Metric(
                        label = stringResource(R.string.parent_stories_read),
                        value = numberFormat.format(parentReport.storiesCompletedThisWeek),
                    ),
                    Metric(
                        label = stringResource(R.string.parent_reading_days),
                        value = numberFormat.format(readingDaysThisWeek),
                    ),
                    Metric(
                        label = stringResource(R.string.parent_quiz_correct),
                        value = stringResource(
                            R.string.parent_quiz_correct_count,
                            progressStats.correctCount,
                            progressStats.questionCount,
                        ),
                    ),
                    Metric(
                        label = stringResource(R.string.parent_words_reviewed),
                        value = numberFormat.format(parentReport.vocabLearnedThisWeek),
                    ),
                ),
            )
        }
        item {
            SectionTitle(text = stringResource(R.string.parent_story_progress))
            Spacer(modifier = Modifier.height(LmcSpacing.Space2))
        }
        items(stories, key = { it.id }) { story ->
            StoryProgressRow(
                story = story,
                progress = progressFraction(
                    story = story,
                    progressRecords = progressRecords,
                    settingsStore = settingsStore,
                    readingProgressVersion = readingProgressVersion,
                ),
                completed = progressRecords.any { it.storyId == story.id },
                onClick = { onStoryClick(story.id) },
            )
        }
        item {
            PrivacyNotice()
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
    onParentReport: () -> Unit,
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
        item {
            SettingsSection(title = stringResource(R.string.settings_language)) {
                ReaderLanguage.entries.forEach { language ->
                    SelectableSettingsRow(
                        label = stringResource(language.labelRes),
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
            }
        }
        item {
            SettingsSection(title = stringResource(R.string.settings_parent)) {
                SettingsNavigationRow(
                    label = stringResource(R.string.settings_parent_report),
                    onClick = onParentReport,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsValueRow(
                    label = stringResource(R.string.settings_privacy),
                    value = stringResource(R.string.settings_privacy_summary),
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
            SettingsSection(title = stringResource(R.string.settings_ai)) {
                AiBackendBaseUrlRow(
                    value = settings.aiBackendBaseUrl,
                    onValueChange = onAiBackendBaseUrlChange,
                )
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
    paragraphIndex: Int,
    paragraphCount: Int,
    isSpeaking: Boolean,
    onClose: () -> Unit,
    onAudioClick: () -> Unit,
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
                icon = if (isSpeaking) LmcIcon.StopAudio else LmcIcon.Audio,
                contentDescription = stringResource(
                    if (isSpeaking) {
                        R.string.action_stop_audio
                    } else {
                        R.string.action_play_audio
                    },
                ),
                onClick = onAudioClick,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LmcSpacing.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            LmcProgressBar(
                progress = (paragraphIndex + 1).toFloat() / paragraphCount.toFloat(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.reading_progress_count,
                    paragraphIndex + 1,
                    paragraphCount,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(LmcSpacing.Space3))
    }
}

@Composable
private fun ReadingControls(
    showPinyin: Boolean,
    readingTextSize: ReadingTextSize,
    onPinyinChange: (Boolean) -> Unit,
    onTextSizeChange: (ReadingTextSize) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = LmcSpacing.ScreenPadding, vertical = LmcSpacing.Space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space4),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
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
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.reading_text_size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextSizeChips(
                selectedTextSize = readingTextSize,
                onTextSizeChange = onTextSizeChange,
            )
        }
    }
}

@Composable
private fun ReadingFullAudioRow(
    isSpeaking: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = LmcSpacing.ScreenPadding)
            .padding(bottom = LmcSpacing.Space3),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.heightIn(min = LmcSpacing.ButtonSecondaryHeight),
        ) {
            LmcCanvasIcon(
                icon = if (isSpeaking) LmcIcon.StopAudio else LmcIcon.Audio,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(LmcSpacing.Space2))
            Text(
                text = stringResource(
                    if (isSpeaking) {
                        R.string.action_stop_audio
                    } else {
                        R.string.reading_read_all
                    },
                ),
            )
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
    story: Story,
    completed: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                StoryCardText(story = story, completed = completed)
                LmcProgressBar(progress = progress)
                Text(
                    text = storyProgressLabel(progress = progress, completed = completed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

@Composable
private fun StoryCover(
    story: Story,
    coverSize: Dp,
) {
    val coverDescription = stringResource(
        R.string.story_cover_content_description,
        story.titleZh,
        story.titleEn,
    )

    Box(
        modifier = Modifier
            .size(coverSize)
            .clip(RoundedCornerShape(LmcSpacing.RadiusSm))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(LmcSpacing.RadiusSm),
            )
            .semantics {
                contentDescription = coverDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(
                color = Color(0x33B84535),
                start = Offset(size.width * 0.18f, size.height * 0.26f),
                end = Offset(size.width * 0.82f, size.height * 0.22f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color(0x33126B68),
                start = Offset(size.width * 0.22f, size.height * 0.74f),
                end = Offset(size.width * 0.78f, size.height * 0.68f),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
        Text(
            text = story.titleZh.take(1),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
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
    text: String,
    pinyin: String,
    showPinyin: Boolean,
    readingType: ReadingTypeStyles,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LmcSpacing.ReadingPanelRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(LmcSpacing.ReadingPanelPadding),
            verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            if (showPinyin) {
                Text(
                    text = pinyin,
                    style = readingType.pinyin,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = text,
                style = readingType.hanzi,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
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
private fun QuizQuestionBody(
    question: Question,
    selectedAnswer: String?,
    submitted: Boolean,
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
                isCorrectAnswer = option == question.answer,
                onClick = { onSelectAnswer(option) },
            )
        }
        if (submitted) {
            val isCorrect = selectedAnswer == question.answer
            FeedbackMessage(
                correct = isCorrect,
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
        submitted && selected -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }
    val backgroundColor = when {
        submitted && isCorrectAnswer -> LmcColors.SuccessContainer
        submitted && selected -> MaterialTheme.colorScheme.errorContainer
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
                    error = submitted && selected && !isCorrectAnswer,
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
        color = if (correct) LmcColors.SuccessContainer else MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(LmcSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(LmcSpacing.Space3),
        ) {
            LmcCanvasIcon(
                icon = if (correct) LmcIcon.Check else LmcIcon.Close,
                color = if (correct) LmcColors.Success else MaterialTheme.colorScheme.error,
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
    onReadAgain: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(LmcSpacing.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space6),
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
            text = stringResource(R.string.quiz_complete_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.quiz_score, correctCount, totalQuestions),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LmcSpacing.RadiusLg),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = LmcSpacing.CardElevation,
        ) {
            Column(
                modifier = Modifier.padding(LmcSpacing.CardPadding),
                verticalArrangement = Arrangement.spacedBy(LmcSpacing.Space2),
            ) {
                Text(
                    text = stringResource(R.string.quiz_retell),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = story.retellPrompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun ParentGateNotice() {
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
                text = stringResource(R.string.parent_gate_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.parent_gate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
private fun PrivacyNotice() {
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
    var satisfaction by remember { mutableStateOf(FeedbackSatisfaction.Satisfied) }
    var ageBand by remember { mutableStateOf(FeedbackAgeBand.AgeFiveSix) }
    var issueType by remember { mutableStateOf(FeedbackIssueType.Content) }
    var suggestion by remember { mutableStateOf("") }
    var parentContact by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
                choices = FeedbackSatisfaction.entries.map { choice ->
                    FeedbackChoice(choice, choice.labelRes)
                },
                selected = satisfaction,
                onSelected = { satisfaction = it },
            )
            FeedbackChoiceGroup(
                label = stringResource(R.string.feedback_child_age_band),
                choices = FeedbackAgeBand.entries.map { choice ->
                    FeedbackChoice(choice, choice.labelRes)
                },
                selected = ageBand,
                onSelected = { ageBand = it },
            )
            FeedbackChoiceGroup(
                label = stringResource(R.string.feedback_issue_type),
                choices = FeedbackIssueType.entries.map { choice ->
                    FeedbackChoice(choice, choice.labelRes)
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
            )
            Button(
                enabled = suggestion.isNotBlank(),
                onClick = {
                    scope.launch {
                        feedbackService.submit(
                            FeedbackSubmission(
                                satisfaction = satisfaction.toSharedFeedbackSatisfaction(),
                                childAgeBand = ageBand.toSharedFeedbackChildAgeBand(),
                                issueType = issueType.toSharedFeedbackIssueType(),
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
                label = { Text(text = stringResource(textSize.labelRes)) },
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
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(LmcSpacing.ProgressHeight)
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
            LmcIcon.Parent -> {
                drawRoundRect(color, Offset(size.width * 0.18f, size.height * 0.18f), Size(size.width * 0.64f, size.height * 0.64f), style = stroke)
                drawLine(color, Offset(size.width * 0.32f, size.height * 0.68f), Offset(size.width * 0.32f, size.height * 0.52f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.5f, size.height * 0.68f), Offset(size.width * 0.5f, size.height * 0.36f), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(size.width * 0.68f, size.height * 0.68f), Offset(size.width * 0.68f, size.height * 0.46f), stroke.width, StrokeCap.Round)
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

private fun progressFraction(
    story: Story,
    progressRecords: List<CompletionRecord>,
    settingsStore: ReaderSettingsStore,
    readingProgressVersion: Int,
): Float {
    if (progressRecords.any { it.storyId == story.id }) return 1f
    if (story.paragraphs.isEmpty()) return 0f

    val savedParagraphIndex = settingsStore.readReadingParagraphIndex(story.id)
    return if (savedParagraphIndex < 0) {
        0f
    } else {
        ((savedParagraphIndex + 1).toFloat() / story.paragraphs.size.toFloat())
            .coerceIn(0f, 0.99f)
    }.also {
        readingProgressVersion.hashCode()
    }
}

private fun Vocab.speechText(): String =
    listOfNotNull(word, example).joinToString(separator = "。")

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

private sealed interface StoriesState {
    data object Loading : StoriesState
    data object Error : StoriesState
    data class Ready(val stories: List<Story>) : StoriesState
}

private data class Metric(
    val label: String,
    val value: String,
)

private const val MockAiBackendBaseUrl = "mock"
private const val AiAnswerMaxChars = 100
private const val AiSelectedTextMaxChars = 120
private const val UnknownAppVersion = "unknown"

private class ReaderAnalytics(
    private val delegate: Analytics,
    private val scope: CoroutineScope,
) {
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
        eventName = AnalyticsEventName.StoryOpen,
        properties = mapOf(
            "story_id" to story.id,
            "story_order" to storyOrder,
            "content_level" to story.level,
            "open_source" to openSource,
        ),
    )
}

private fun ReaderAnalytics.trackParagraphAudioPlay(
    storyId: String,
    paragraphIndex: Int,
    targetType: String,
) {
    track(
        eventName = AnalyticsEventName.ParagraphAudioPlay,
        properties = mapOf(
            "story_id" to storyId,
            "paragraph_index" to paragraphIndex,
            "audio_source" to "tts",
        ),
    )
}

private fun ReaderAnalytics.trackAiExplainRequest(
    storyId: String,
    requestType: String,
    targetType: String,
    safetyOutcome: String,
) {
    track(
        eventName = AnalyticsEventName.AiExplainRequest,
        properties = mapOf(
            "story_id" to storyId,
            "request_type" to requestType,
            "safety_outcome" to safetyOutcome,
            "target_type" to targetType,
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
                .put("selected_text", request.selectedText.take(AiSelectedTextMaxChars))
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

private fun String.isMockAiBackend(): Boolean =
    isBlank() ||
        equals(MockAiBackendBaseUrl, ignoreCase = true) ||
        equals("local/mock", ignoreCase = true)

private fun String.limitAiAnswer(): String =
    if (length <= AiAnswerMaxChars) this else take(AiAnswerMaxChars)

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

private fun Story.vocabIdFor(index: Int): String = "$id:${index + 1}"

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

private enum class FeedbackSatisfaction(
    val prefValue: String,
    @StringRes val labelRes: Int,
) {
    Satisfied(prefValue = "satisfied", labelRes = R.string.feedback_satisfaction_satisfied),
    Okay(prefValue = "okay", labelRes = R.string.feedback_satisfaction_okay),
    NotSatisfied(prefValue = "not_satisfied", labelRes = R.string.feedback_satisfaction_not_satisfied),
}

private enum class FeedbackAgeBand(
    val prefValue: String,
    @StringRes val labelRes: Int,
) {
    AgeFiveSix(prefValue = "5_6", labelRes = R.string.feedback_age_5_6),
    AgeSevenEight(prefValue = "7_8", labelRes = R.string.feedback_age_7_8),
    Other(prefValue = "other", labelRes = R.string.feedback_age_other),
}

private enum class FeedbackIssueType(
    val prefValue: String,
    @StringRes val labelRes: Int,
) {
    Content(prefValue = "content", labelRes = R.string.feedback_issue_content),
    Audio(prefValue = "audio", labelRes = R.string.feedback_issue_audio),
    Ai(prefValue = "ai", labelRes = R.string.feedback_issue_ai),
    Progress(prefValue = "progress", labelRes = R.string.feedback_issue_progress),
    Other(prefValue = "other", labelRes = R.string.feedback_issue_other),
}

private fun FeedbackSatisfaction.toSharedFeedbackSatisfaction(): SharedFeedbackSatisfaction =
    when (this) {
        FeedbackSatisfaction.Satisfied -> SharedFeedbackSatisfaction.Satisfied
        FeedbackSatisfaction.Okay -> SharedFeedbackSatisfaction.Neutral
        FeedbackSatisfaction.NotSatisfied -> SharedFeedbackSatisfaction.Dissatisfied
    }

private fun FeedbackAgeBand.toSharedFeedbackChildAgeBand(): SharedFeedbackChildAgeBand =
    when (this) {
        FeedbackAgeBand.AgeFiveSix -> SharedFeedbackChildAgeBand.Age5To6
        FeedbackAgeBand.AgeSevenEight -> SharedFeedbackChildAgeBand.Age7To8
        FeedbackAgeBand.Other -> SharedFeedbackChildAgeBand.PreferNotToSay
    }

private fun FeedbackIssueType.toSharedFeedbackIssueType(): SharedFeedbackIssueType =
    when (this) {
        FeedbackIssueType.Content -> SharedFeedbackIssueType.ContentTooHard
        FeedbackIssueType.Audio -> SharedFeedbackIssueType.AudioIssue
        FeedbackIssueType.Ai -> SharedFeedbackIssueType.AiExplainIssue
        FeedbackIssueType.Progress -> SharedFeedbackIssueType.Other
        FeedbackIssueType.Other -> SharedFeedbackIssueType.Other
    }

private data class ReaderSettings(
    val language: ReaderLanguage,
    val showPinyinByDefault: Boolean,
    val readingTextSize: ReadingTextSize,
    val aiBackendBaseUrl: String,
)

private enum class ReaderLanguage(
    val tag: String,
    @StringRes val labelRes: Int,
) {
    English(tag = "en", labelRes = R.string.settings_language_en),
    ChineseSimplified(tag = "zh-Hans", labelRes = R.string.settings_language_zh_hans),
    ;

    companion object {
        fun fromTag(tag: String?): ReaderLanguage =
            entries.firstOrNull { it.tag == tag } ?: English
    }
}

private enum class ReadingTextSize(
    val prefValue: String,
    @StringRes val labelRes: Int,
) {
    Small(prefValue = "small", labelRes = R.string.reading_text_size_small),
    Medium(prefValue = "medium", labelRes = R.string.reading_text_size_medium),
    Large(prefValue = "large", labelRes = R.string.reading_text_size_large),
    ;

    companion object {
        fun fromPrefValue(value: String?): ReadingTextSize =
            entries.firstOrNull { it.prefValue == value } ?: Medium
    }
}

private class ReaderSettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)

    fun read(): ReaderSettings = ReaderSettings(
        language = ReaderLanguage.fromTag(preferences.getString(KeyLanguage, null)),
        showPinyinByDefault = preferences.getBoolean(KeyShowPinyinDefault, true),
        readingTextSize = ReadingTextSize.fromPrefValue(preferences.getString(KeyReadingTextSize, null)),
        aiBackendBaseUrl = preferences.getString(KeyAiBackendBaseUrl, MockAiBackendBaseUrl)
            ?: MockAiBackendBaseUrl,
    )

    fun setLanguage(language: ReaderLanguage) {
        preferences.edit()
            .putString(KeyLanguage, language.tag)
            .apply()
    }

    fun setShowPinyinByDefault(showPinyin: Boolean) {
        preferences.edit()
            .putBoolean(KeyShowPinyinDefault, showPinyin)
            .apply()
    }

    fun setReadingTextSize(textSize: ReadingTextSize) {
        preferences.edit()
            .putString(KeyReadingTextSize, textSize.prefValue)
            .apply()
    }

    fun setAiBackendBaseUrl(baseUrl: String) {
        preferences.edit()
            .putString(KeyAiBackendBaseUrl, baseUrl)
            .apply()
    }

    fun readReadingParagraphIndex(storyId: String): Int =
        preferences.getInt(readingProgressKey(storyId), -1)

    fun setReadingParagraphIndex(storyId: String, paragraphIndex: Int) {
        preferences.edit()
            .putInt(readingProgressKey(storyId), paragraphIndex)
            .apply()
    }

    private fun readingProgressKey(storyId: String): String = "$KeyReadingProgressPrefix$storyId"

    private companion object {
        const val PreferencesName = "little_mandarin_reader_settings"
        const val KeyLanguage = "language"
        const val KeyShowPinyinDefault = "show_pinyin_default"
        const val KeyReadingTextSize = "reading_text_size"
        const val KeyAiBackendBaseUrl = "ai_backend_base_url"
        const val KeyReadingProgressPrefix = "reading_progress_"
    }
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
    Settings,
    StopAudio,
    Today,
}

private object LmcColors {
    val Success = Color(0xFF3B7A3B)
    val SuccessContainer = Color(0xFFE1F3DC)
}

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
