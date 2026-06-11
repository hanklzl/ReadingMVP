import Foundation
import AVFoundation
import SwiftUI
import UIKit
import shared

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var localeIdentifier = Self.defaultAppLocaleIdentifier()
    @State private var showPinyin = true
    @State private var readingSize = LMCReadingSize.medium.rawValue
    @State private var aiBackendBaseURL = ""
    @State private var sfxEnabled = true
    @State private var sfxVolume = 0.5
    @StateObject private var viewModel = ReaderViewModel()
    @State private var selectedTab: LMCTab = .today
    @State private var flowRoute: LMCFlowRoute?
    @State private var parentReportEntryPoint: LMCParentReportEntryPoint = .bottomNavigation
    @State private var lockedQuizAlert = false
    @State private var didTrackColdOpen = false
    @State private var didObserveInitialActiveScene = false
    @State private var didLeaveActiveScene = false

    var body: some View {
        ZStack {
            LMCColor.background.ignoresSafeArea()

            if viewModel.launchGate == .loading {
                LMCLoadingView()
                    .padding(LMCSpace.s4)
            } else if viewModel.launchGate == .onboarding {
                OnboardingFlow(
                    viewModel: viewModel,
                    initialLocaleIdentifier: Self.defaultAppLocaleIdentifier(),
                    complete: { ageBand, languageTag, dailyGoal, assessedLevel in
                        localeIdentifier = languageTag
                        await viewModel.finishOnboarding(
                            ageBand: ageBand,
                            languageTag: languageTag,
                            dailyGoalStories: dailyGoal,
                            assessedReadingLevel: assessedLevel
                        )
                        selectedTab = .today
                        flowRoute = nil
                        await loadSettings()
                    },
                    skip: {
                        await viewModel.skipOnboardingWithDefaults()
                        selectedTab = .today
                        flowRoute = nil
                        await loadSettings()
                    }
                )
            } else {
                if let flowRoute {
                    flowView(for: flowRoute)
                } else {
                    VStack(spacing: 0) {
                        topLevelView
                        LMCBottomNavigation(selectedTab: $selectedTab) { tab in
                            selectTab(tab, parentEntryPoint: .bottomNavigation)
                        }
                    }
                }
            }
        }
        .environment(\.locale, Locale(identifier: localeIdentifier))
        .preferredColorScheme(.light)
        .task {
            if !didTrackColdOpen {
                didTrackColdOpen = true
                viewModel.trackAppOpen(openType: .coldStart)
                if scenePhase == .active {
                    didObserveInitialActiveScene = true
                }
            }
            await viewModel.load()
            await viewModel.loadLaunchState()
            await loadSettings()
        }
        .onChange(of: localeIdentifier) { newValue in
            Task { await viewModel.setLanguageTag(newValue) }
        }
        .onChange(of: showPinyin) { newValue in
            Task { await viewModel.setShowPinyinByDefault(newValue) }
        }
        .onChange(of: readingSize) { newValue in
            Task { await viewModel.setReadingSizeValue(newValue) }
        }
        .onChange(of: aiBackendBaseURL) { newValue in
            Task { await viewModel.setAiBackendBaseURL(newValue) }
        }
        .onChange(of: sfxEnabled) { newValue in
            Task {
                await viewModel.setSfxEnabled(newValue)
                if newValue {
                    await viewModel.playSfxPreview()
                } else {
                    await viewModel.stopAllSfx()
                }
            }
        }
        .onChange(of: sfxVolume) { newValue in
            Task { await viewModel.setSfxVolume(newValue) }
        }
        .onChange(of: scenePhase) { newPhase in
            switch newPhase {
            case .active:
                if !didObserveInitialActiveScene {
                    didObserveInitialActiveScene = true
                    didLeaveActiveScene = false
                    return
                }
                guard didTrackColdOpen, didLeaveActiveScene else { return }
                didLeaveActiveScene = false
                viewModel.trackAppOpen(openType: .foreground)
            case .inactive, .background:
                didLeaveActiveScene = true
                viewModel.stopAllSfx()
            @unknown default:
                break
            }
        }
        .alert("today_quiz_locked_title", isPresented: $lockedQuizAlert) {
            Button("action_ok", role: .cancel) { }
        } message: {
            Text("today_quiz_locked_message")
        }
    }

    @ViewBuilder
    private var topLevelView: some View {
        switch selectedTab {
        case .today:
            TodayScreen(
                viewModel: viewModel,
                openReading: { story in
                    let source: LMCStoryOpenSource = story.id == viewModel.todayStory?.id ? .todayHero : .todayUpNext
                    openStory(story, source: source)
                },
                openVocabulary: { flowRoute = .vocabulary(storyId: $0.id, openSource: .todaySummary) },
                openQuiz: { story in
                    if viewModel.canOpenQuiz(story) {
                        flowRoute = .quiz(storyId: story.id)
                    } else {
                        lockedQuizAlert = true
                    }
                },
                openParent: { selectTab(.parent, parentEntryPoint: .todayHeader) },
                openReview: {
                    viewModel.openPendingReview()
                    flowRoute = .reviewPack
                },
                openAbilityMap: { flowRoute = .abilityMap },
                openSettings: { flowRoute = .settings }
            )
        case .library:
            LibraryScreen(
                viewModel: viewModel,
                openReading: { openStory($0, source: .library) },
                openSettings: { flowRoute = .settings }
            )
        case .wordBook:
            WordBookScreen(
                viewModel: viewModel,
                openReview: { flowRoute = .wordReview },
                openToday: { selectedTab = .today },
                openSettings: { flowRoute = .settings }
            )
        case .parent:
            ParentReportScreen(
                viewModel: viewModel,
                entryPoint: parentReportEntryPoint,
                openSettings: { flowRoute = .settings },
                openStory: { openStory($0, source: .parentReport) },
                openWords: {
                    selectedTab = .wordBook
                    flowRoute = nil
                },
                openReview: {
                    viewModel.openPendingReview()
                    flowRoute = .reviewPack
                },
                recordings: viewModel.recordings,
                playRecording: { recording in
                    Task { await viewModel.playRecording(recording) }
                },
                deleteRecording: { recordingId in
                    Task { await viewModel.deleteRecording(recordingId) }
                },
                clearAllRecordings: {
                    Task { await viewModel.clearRecordings() }
                }
            )
        }
    }

    @ViewBuilder
    private func flowView(for route: LMCFlowRoute) -> some View {
        switch route {
        case .reading(let storyId):
            if let story = viewModel.story(id: storyId) {
                ReadingScreen(
                    viewModel: viewModel,
                    story: story,
                    showPinyin: $showPinyin,
                    readingSize: $readingSize,
                    aiBackendBaseURL: $aiBackendBaseURL,
                    close: { flowRoute = nil },
                    openVocabulary: { flowRoute = .vocabulary(storyId: story.id, openSource: .readingFlow) }
                )
            } else {
                LMCMissingStoryView(close: { flowRoute = nil })
            }
        case .vocabulary(let storyId, let openSource):
            if let story = viewModel.story(id: storyId) {
                VocabularyScreen(
                    viewModel: viewModel,
                    story: story,
                    openSource: openSource,
                    close: {
                        switch openSource {
                        case .todaySummary:
                            selectedTab = .today
                            flowRoute = nil
                        case .readingFlow, .quizBack:
                            flowRoute = .reading(storyId: story.id)
                        }
                    },
                    openQuiz: { flowRoute = .quiz(storyId: story.id) }
                )
            } else {
                LMCMissingStoryView(close: { flowRoute = nil })
            }
        case .quiz(let storyId):
            if let story = viewModel.story(id: storyId) {
                QuizScreen(
                    viewModel: viewModel,
                    story: story,
                    close: { flowRoute = .vocabulary(storyId: story.id, openSource: .quizBack) },
                    readAgain: { restartStoryFromBeginning(story, source: .quizCompletion) },
                    openReview: {
                        viewModel.openCompletionReview()
                        flowRoute = .reviewPack
                    },
                    done: {
                        selectedTab = .today
                        flowRoute = nil
                    }
                )
            } else {
                LMCMissingStoryView(close: { flowRoute = nil })
            }
        case .wordReview:
            WordReviewScreen(
                viewModel: viewModel,
                close: {
                    selectedTab = .wordBook
                    flowRoute = nil
                }
            )
        case .reviewPack:
            ReviewPackScreen(
                pack: viewModel.activeReviewPack ?? viewModel.pendingReview?.pack,
                done: {
                    Task { await viewModel.markReviewCompleted() }
                    selectedTab = .today
                    flowRoute = nil
                },
                close: { flowRoute = nil }
            )
        case .abilityMap:
            VStack(spacing: 0) {
                LMCFlowTopBar(
                    titleKey: "ability_title",
                    trailingText: nil,
                    close: { flowRoute = nil }
                )
                AbilityMapScreen(abilityMap: viewModel.abilityMap)
            }
        case .settings:
            VStack(spacing: 0) {
                LMCFlowTopBar(
                    titleKey: "settings_title",
                    trailingText: nil,
                    close: { flowRoute = nil }
                )
                SettingsScreen(
                    viewModel: viewModel,
                    localeIdentifier: $localeIdentifier,
                    showPinyin: $showPinyin,
                    readingSize: $readingSize,
                    aiBackendBaseURL: $aiBackendBaseURL,
                    sfxEnabled: $sfxEnabled,
                    sfxVolume: $sfxVolume,
                    openParent: {
                        flowRoute = nil
                        selectTab(.parent, parentEntryPoint: .settings)
                    },
                    previewSfx: { Task { await viewModel.playSfxPreview() } },
                    includeTitle: false
                )
            }
        }
    }

    private func openStory(_ story: Story, source: LMCStoryOpenSource) {
        guard !viewModel.isCompleted(story) else {
            restartStoryFromBeginning(story, source: source)
            return
        }
        viewModel.trackStoryOpen(story, openSource: source)
        flowRoute = .reading(storyId: story.id)
    }

    private func restartStoryFromBeginning(_ story: Story, source: LMCStoryOpenSource) {
        viewModel.trackStoryOpen(story, openSource: source)
        Task { @MainActor in
            await viewModel.resetReadingParagraphIndex(for: story)
            flowRoute = .reading(storyId: story.id)
        }
    }

    private func selectTab(_ tab: LMCTab, parentEntryPoint: LMCParentReportEntryPoint) {
        if tab == .parent {
            parentReportEntryPoint = parentEntryPoint
        }
        selectedTab = tab
        flowRoute = nil
    }

    private static func isChineseLanguageTag(_ tag: String) -> Bool {
        tag.hasPrefix("zh")
    }

    private static func defaultAppLocaleIdentifier() -> String {
        let preferredLanguageTag = Locale.preferredLanguages.first?.lowercased()
            ?? Locale.current.identifier.lowercased()

        return isChineseLanguageTag(preferredLanguageTag)
            ? LMCAppLocale.simplifiedChinese.rawValue
            : LMCAppLocale.english.rawValue
    }

    private func loadSettings() async {
        guard let settings = await viewModel.readSettings() else { return }
        if viewModel.launchGate != .onboarding {
            localeIdentifier = settings.language.tag
        }
        showPinyin = settings.showPinyinByDefault
        readingSize = settings.readingTextSize.prefValue
        aiBackendBaseURL = settings.aiBackendBaseUrl
        sfxEnabled = settings.sfxSettings.enabled
        sfxVolume = Double(settings.sfxSettings.volume)
    }
}

@MainActor
final class ReaderViewModel: ObservableObject {
    @Published private(set) var stories: [Story] = []
    @Published private(set) var completedStoryIds: Set<String> = []
    @Published private(set) var readingPositions: [String: Int] = [:]
    @Published private(set) var stats: ProgressStats?
    @Published private(set) var parentReport: ParentProgressReport?
    @Published private(set) var loadingState: LMCLoadingState = .idle
    @Published private(set) var isSpeaking = false
    @Published private(set) var readingAudioStatus: LMCReadingAudioStatus = .idle
    @Published private(set) var activeCharIndex: Int? = nil
    @Published private(set) var readingMode: LMCReadingPlaybackMode = .readAlong
    @Published private(set) var autoContinueEnabled = true
    @Published private(set) var playbackSpeed: LMCReadingPlaybackSpeed = .defaultSlow
    @Published private(set) var launchGate: LMCLaunchGate = .loading
    @Published private(set) var wordBookSummary: WordBookSummary?
    @Published private(set) var vocabRecords: [VocabSrsRecord] = []
    @Published private(set) var streakSummary: StreakSummary?
    @Published private(set) var lastCompletionStreakSummary: StreakSummary?
    @Published private(set) var onboardingPreferences: OnboardingPreferences?
    @Published private(set) var pendingReview: PendingReview?
    @Published private(set) var aiInteractionRecords: [AiInteractionRecord] = []
    @Published var activeReviewPack: ReviewPack?
    @Published private(set) var lastCompletionReviewPack: ReviewPack?
    @Published private(set) var recordings: [VoiceRecording] = []

    private let repository = DefaultStoryRepository(
        resourceReader: IosStoryResourceReaderKt.defaultStoryResourceReader(),
        catalog: StoryResourceCatalog.shared.entries
    )
    private let progressService = IosProgressServiceKt.createPlatformProgressService()
    private let settingsService = IosReaderSettingsServiceKt.createPlatformReaderSettingsService()
    private let onboardingService = IosEngagementServicesKt.createPlatformOnboardingService()
    private let streakService = IosEngagementServicesKt.createPlatformStreakService()
    private let vocabReviewService = IosVocabReviewServiceKt.createPlatformVocabReviewService()
    private let reviewPackService = IosEngagementServicesKt.createPlatformReviewPackService()
    private let aiInteractionLogService = IosEngagementServicesKt.createPlatformAiInteractionLogService()
    private let aiSafetyConsoleUseCases = AiSafetyConsoleUseCases()
    private let ttsService = IosTtsServiceKt.createTtsService()
    private let storyPresentationUseCases = StoryPresentationUseCases()
    private let readingLevelAssessmentUseCases = ReadingLevelAssessmentUseCases()
    private let readingPathRecommender = AdaptiveReadingPathRecommender()
    private let buildParentReportSummaryUseCase = BuildParentReportSummaryUseCase()
    private let abilityMapUseCases = AbilityMapUseCases()
    private let readingSessionReducer = ReadingSessionReducer()
    private let karaokeReducer = ReadAlongKaraokeReducer()
    private let quizSessionReducer = QuizSessionReducer(scoreQuizUseCase: ScoreQuizUseCase())
    private let sfxEventReducer = SfxEventReducer()
    private let buildSpeechTextUseCase = BuildSpeechTextUseCase()
    private let analytics = LMCUserDefaultsAnalyticsAdapter()
    private let feedbackRepository = LMCSharedFeedbackRepository()
    private let aiService = LMCAiExplanationAdapter()
    private let wordLookupUseCase = WordLookupUseCase()
    private var speechTask: Task<Void, Never>?
    private var readingAudioTask: Task<Void, Never>?
    private var activeSentenceShouldAutoContinue = true
    private var generatedAudioPlayer: AVAudioPlayer?
    private var generatedAudioDelegate: LMCGeneratedAudioDelegate?
    @Published private(set) var activeSentenceRecordingLocation: LMCSentenceLocation?
    private var sentenceRecorder: AVAudioRecorder?
    private var activeSentenceRecording: LMCActiveSentenceRecording?
    // Optional retell recording reuses the same recorder + local-only service. Tagged with
    // paragraphIndex = -1 so the parent list labels it as a retell.
    @Published private(set) var activeRetellRecordingStoryId: String?
    private var retellRecorder: AVAudioRecorder?
    private var activeRetellRecording: LMCActiveRetellRecording?
    private var recordingPlaybackPlayer: AVAudioPlayer?
    private var recordingPlaybackDelegate: LMCGeneratedAudioDelegate?
    private var sfxPlayers: [String: AVAudioPlayer] = [:]

    private let maxRecordingsPerStory = 12
    private let maxRecordingsOverall = 60
    private let readingRecordingsDirectoryName = "recording"
    private let voiceRecordingService = IosRecordingServiceKt.createPlatformVoiceRecordingService(
        retentionPolicy: RecordingRetentionPolicy(
            maxPerStory: 12,
            maxOverall: 60
        )
    )

    // Per-character ("karaoke") highlight state, driven by the shared reducer.
    private let loadAudioManifestUseCase = LoadStoryAudioManifestUseCase(
        resourceReader: IosStoryResourceReaderKt.defaultStoryResourceReader()
    )
    private var audioManifests: [String: StoryAudioManifest] = [:]
    private var activeKaraokeTimeline = KaraokeTimeline.companion.Empty
    private var karaokeTimer: Timer?

    private lazy var getStoryListUseCase = GetStoryListUseCase(repository: repository)
    private lazy var markStoryCompletedUseCase = MarkStoryCompletedUseCase(progressService: progressService)
    private lazy var getProgressStatsUseCase = GetProgressStatsUseCase(progressService: progressService)
    private lazy var buildParentReportUseCase = BuildParentReportUseCase(progressService: progressService)
    private lazy var streakUseCase = StreakUseCase(streakService: streakService)
    private let mistakeRemediationUseCases = MistakeRemediationUseCases()
    private lazy var reviewPackUseCase = ReviewPackUseCase(service: reviewPackService)
    private lazy var vocabReviewUseCase = VocabReviewUseCase(
        storyRepository: repository,
        progressService: progressService,
        reviewService: vocabReviewService,
        scheduler: SrsScheduler()
    )

    var todayStory: Story? {
        todayShelf.todayStory
    }

    var upNextStory: Story? {
        todayShelf.upNextStory
    }

    var readingAudioLocation: LMCSentenceLocation? {
        readingAudioStatus.location
    }

    var isReadingAudioActive: Bool {
        readingAudioStatus.isActive
    }

    var isReadingAudioPaused: Bool {
        readingAudioStatus.isPaused
    }

    private var todayShelf: TodayStories {
        storyPresentationUseCases.selectTodayStories(
            stories: stories,
            completedStoryIds: completedStoryIds,
            policy: .firstincomplete,
            recommendedStoryId: readingPathRecommendation.nextStory?.id
        )
    }

    // Adaptive recommendation driven by the local reading level, history, in-progress
    // positions and due-vocab count. Pure shared logic; never child PII (AGENTS.md §7).
    private var readingPathRecommendation: ReadingPathRecommendation {
        let level = onboardingPreferences?.recommendedLevel ?? 1
        return readingPathRecommender.recommend(
            stories: stories,
            readingLevel: level,
            completionRecords: completionRecords,
            readingPositions: readingPositionsForRecommender,
            dueVocabWordCount: Int32(wordBookSummary?.dueCount ?? 0)
        )
    }

    var reviewWordCount: Int {
        Int(readingPathRecommendation.reviewWordCount)
    }

    private var completionRecords: [CompletionRecord] {
        parentReport?.recentCompletions ?? []
    }

    // Story-driven Chinese-ability map (pure shared logic). The most recently completed
    // story drives the parent "today you practised …" line. No child PII (AGENTS.md §7).
    var abilityMap: AbilityMap {
        let recentSessionStoryId = completionRecords
            .max(by: { $0.completedAtEpochMillis < $1.completedAtEpochMillis })?
            .storyId
        return abilityMapUseCases.buildAbilityMap(
            stories: stories,
            completionRecords: completionRecords,
            recentSessionStoryId: recentSessionStoryId
        )
    }

    private var readingPositionsForRecommender: [String: KotlinInt] {
        readingPositions.mapValues { KotlinInt(int: Int32($0)) }
    }

    /// Build the optional placement-check items. Deterministic seed (catalog size) for MVP.
    func assessmentItems() -> [AssessmentItem] {
        readingLevelAssessmentUseCases.selectAssessmentItems(
            stories: stories,
            seed: Int32(stories.count),
            itemCount: 5
        )
    }

    /// Score the placement check, persist the resulting reading level locally, return the level.
    @discardableResult
    func applyAssessment(items: [AssessmentItem], answersByItemId: [String: String]) async -> Int {
        let assessed = readingLevelAssessmentUseCases.scoreAssessment(
            items: items,
            answersByItemId: answersByItemId
        )
        await setAssessedReadingLevel(KotlinInt(int: assessed.level))
        return Int(assessed.level)
    }

    func setAssessedReadingLevel(_ level: KotlinInt?) async {
        do {
            let current = try await onboardingService.read()
            let updated = current.doCopy(
                completed: true,
                skipped: current.skipped,
                childAgeBand: current.childAgeBand,
                language: current.language,
                dailyGoalStories: current.dailyGoalStories,
                assessedReadingLevel: level
            )
            try await onboardingService.complete(preferences: updated)
            onboardingPreferences = try await onboardingService.read()
        } catch {
            loadingState = .failed
        }
    }

    func load() async {
        guard loadingState != .loading else { return }
        loadingState = .loading
        registerKaraokeRangeListener()
        do {
            stories = try await getStoryListUseCase.invoke()
            try await refreshProgress()
            try await refreshReadingPositions()
            try await refreshWordBook()
            try await refreshStreak()
            try await refreshPendingReview()
            await syncRecordingsFromService()
            loadingState = .loaded
        } catch {
            loadingState = .failed
        }
    }

    func loadLaunchState() async {
        let preferences = try? await onboardingService.read()
        onboardingPreferences = preferences
        if let preferences, preferences.completed {
            try? await streakUseCase.setDailyGoal(dailyGoalStories: preferences.dailyGoalStories)
            launchGate = .ready
        } else {
            launchGate = .onboarding
        }
        try? await refreshStreak()
    }

    func finishOnboarding(
        ageBand: LMCOnboardingAgeBand,
        languageTag: String,
        dailyGoalStories: Int,
        assessedReadingLevel: KotlinInt? = nil
    ) async {
        do {
            let language = ReaderLanguage.companion.fromTag(tag: languageTag)
            try await settingsService.setLanguage(language: language)
            try await onboardingService.complete(
                preferences: OnboardingPreferences(
                    completed: true,
                    skipped: false,
                    childAgeBand: ageBand.sharedValue,
                    language: language,
                    dailyGoalStories: Int32(dailyGoalStories),
                    assessedReadingLevel: assessedReadingLevel
                )
            )
            onboardingPreferences = try await onboardingService.read()
            try await streakUseCase.setDailyGoal(dailyGoalStories: Int32(dailyGoalStories))
            try await refreshStreak()
            launchGate = .ready
        } catch {
            loadingState = .failed
        }
    }

    func skipOnboarding() async {
        await skipOnboardingWithDefaults()
    }

    func skipOnboardingWithDefaults() async {
        let systemLocaleTag = ReaderViewModel.defaultAppLocaleIdentifier()
        let language = ReaderLanguage.companion.fromTag(tag: systemLocaleTag)

        do {
            try await settingsService.setLanguage(language: language)
            try await onboardingService.skip(
                preferences: OnboardingPreferences(
                    completed: true,
                    skipped: true,
                    childAgeBand: .age5to8,
                    language: language,
                    dailyGoalStories: 1,
                    assessedReadingLevel: nil
                )
            )
            onboardingPreferences = try await onboardingService.read()
            try await streakUseCase.setDailyGoal(dailyGoalStories: 1)
            try await refreshStreak()
            launchGate = .ready
        } catch {
            loadingState = .failed
        }
    }

    private static func defaultAppLocaleIdentifier() -> String {
        let preferredLanguageTag = Locale.preferredLanguages.first?.lowercased()
            ?? Locale.current.identifier.lowercased()

        return preferredLanguageTag.hasPrefix("zh")
            ? LMCAppLocale.simplifiedChinese.rawValue
            : LMCAppLocale.english.rawValue
    }

    func story(id: String) -> Story? {
        stories.first { $0.id == id }
    }

    func isCompleted(_ story: Story) -> Bool {
        completedStoryIds.contains(story.id)
    }

    func canOpenQuiz(_ story: Story) -> Bool {
        storyPresentationUseCases.canOpenQuiz(
            story: story,
            completedStoryIds: completedStoryIds
        )
    }

    func completeStory(_ story: Story, quizState: QuizSessionState) async -> (
        score: QuizScore,
        wasNewCompletion: Bool,
        newMilestoneDays: KotlinInt?
    ) {
        let score = quizScore(story, state: quizState)
        var wasNewCompletion = false
        var newMilestoneDays: KotlinInt?
        do {
            let now = Int64(Date().timeIntervalSince1970 * 1_000)
            let wasAlreadyCompleted = try await progressService.getRecords().contains { $0.storyId == story.id }
            try await markStoryCompletedUseCase.invoke(record: completionRecord(story, state: quizState))
            try await vocabReviewUseCase.syncLearnedWords(todayEpochMillis: now)
            let dueWords = Set((wordBookSummary?.items ?? []).filter { $0.due }.map { $0.word })
            let reviewPack = mistakeRemediationUseCases.buildReviewPack(
                story: story,
                quizScore: score,
                dueWords: dueWords,
                maxReviewWords: 3
            )
            lastCompletionReviewPack = reviewPack
            try await reviewPackUseCase.savePack(pack: reviewPack, todayEpochMillis: now)
            try await refreshPendingReview()
            if wasAlreadyCompleted {
                lastCompletionStreakSummary = try await streakUseCase.summary(todayEpochMillis: now)
            } else {
                lastCompletionStreakSummary = try await streakUseCase.recordStoryCompleted(nowEpochMillis: now)
            }
            wasNewCompletion = !wasAlreadyCompleted
            newMilestoneDays = lastCompletionStreakSummary?.newMilestoneDays
            try await refreshProgress()
            try await refreshWordBook()
            try await refreshStreak()
            trackStoryComplete(story, quizCompleted: true)
        } catch {
            loadingState = .failed
        }
        return (score, wasNewCompletion, newMilestoneDays)
    }

    func refreshPendingReview() async throws {
        let now = Int64(Date().timeIntervalSince1970 * 1_000)
        pendingReview = try await reviewPackUseCase.pendingReview(todayEpochMillis: now)
    }

    func markReviewCompleted() async {
        let now = Int64(Date().timeIntervalSince1970 * 1_000)
        try? await reviewPackUseCase.markCompleted(todayEpochMillis: now)
        try? await refreshPendingReview()
        activeReviewPack = nil
    }

    func openCompletionReview() {
        activeReviewPack = lastCompletionReviewPack
    }

    func openPendingReview() {
        activeReviewPack = pendingReview?.pack
    }

    func progressValue(for story: Story) -> Double {
        storyProgress(for: story).fraction
    }

    func progressLabelKey(for story: Story) -> String {
        switch storyProgress(for: story).status {
        case .completed:
            return "status_completed"
        case .inprogress:
            return "status_continue"
        default:
            return "status_new"
        }
    }

    func setReadingMode(_ mode: LMCReadingPlaybackMode) {
        readingMode = mode
    }

    func setAutoContinue(_ enabled: Bool) {
        autoContinueEnabled = enabled
    }

    func setPlaybackSpeed(_ speed: LMCReadingPlaybackSpeed) {
        playbackSpeed = speed
        generatedAudioPlayer?.rate = Float(speed.multiplier)
    }

    func speakCurrent(_ text: String) {
        speak(text)
    }

    func speakAll(_ story: Story) {
        speak(buildSpeechTextUseCase.story(story: story))
    }

    func startSentencePlayback(story: Story, paragraphIndex: Int, sentenceIndex: Int = 0) {
        guard let location = story.lmcNormalizedSentenceLocation(
            paragraphIndex: paragraphIndex,
            sentenceIndex: sentenceIndex
        ) else {
            stopSpeaking()
            return
        }
        activeSentenceShouldAutoContinue = readingMode == .readAlong && autoContinueEnabled
        playReadingSentence(story: story, location: location, shouldTrack: true)
    }

    func playTappedSentence(story: Story, paragraphIndex: Int, sentenceIndex: Int) {
        guard let location = story.lmcNormalizedSentenceLocation(
            paragraphIndex: paragraphIndex,
            sentenceIndex: sentenceIndex
        ) else {
            stopSpeaking()
            return
        }
        activeSentenceShouldAutoContinue = readingMode == .readAlong && autoContinueEnabled
        playReadingSentence(story: story, location: location, shouldTrack: true)
    }

    func playSentenceOnly(story: Story, paragraphIndex: Int, sentenceIndex: Int) {
        guard let location = story.lmcNormalizedSentenceLocation(
            paragraphIndex: paragraphIndex,
            sentenceIndex: sentenceIndex
        ) else {
            stopSpeaking()
            return
        }
        activeSentenceShouldAutoContinue = false
        playReadingSentence(story: story, location: location, shouldTrack: true)
    }

    func repeatCurrentSentence(story: Story) {
        guard let location = readingAudioLocation else { return }
        activeSentenceShouldAutoContinue = false
        playReadingSentence(story: story, location: location, shouldTrack: true)
    }

    func pauseSentencePlayback() {
        guard case .playing(let location, let source) = readingAudioStatus else { return }
        switch source {
        case .recorded:
            generatedAudioPlayer?.pause()
        case .tts:
            readingAudioTask?.cancel()
            readingAudioTask = nil
            speechTask = Task { @MainActor in
                try? await ttsService.stop()
            }
        }
        readingAudioStatus = .paused(location, source)
        isSpeaking = false
    }

    func resumeSentencePlayback(story: Story) {
        guard case .paused(let location, let source) = readingAudioStatus else { return }
        switch source {
        case .recorded where generatedAudioPlayer != nil:
            guard generatedAudioPlayer?.play() == true else {
                playReadingSentence(story: story, location: location, shouldTrack: false)
                return
            }
            readingAudioStatus = .playing(location, source)
            isSpeaking = true
        default:
            playReadingSentence(story: story, location: location, shouldTrack: false)
        }
    }

    func advanceSentencePlayback(story: Story) {
        guard let location = readingAudioLocation else { return }
        guard let nextLocation = story.lmcNextSentenceLocation(after: location) else {
            stopSpeaking()
            return
        }
        if isReadingAudioActive && !isReadingAudioPaused {
            playReadingSentence(story: story, location: nextLocation, shouldTrack: true)
        } else {
            readingAudioStatus = .paused(nextLocation, .tts)
        }
    }

    func previousSentencePlayback(story: Story) {
        guard let location = readingAudioLocation,
              let previousLocation = story.lmcPreviousSentenceLocation(before: location) else {
            return
        }
        if isReadingAudioActive && !isReadingAudioPaused {
            playReadingSentence(story: story, location: previousLocation, shouldTrack: true)
        } else {
            readingAudioStatus = .paused(previousLocation, .tts)
        }
    }

    func stopSentencePlayback() {
        let location = readingAudioLocation
        stopSpeaking()
        stopSentenceRecording()
        if let location {
            readingAudioStatus = .stopped(location)
        }
    }

    enum LMCRecordingStartResult {
        case started
        case permissionDenied
        case failed
    }

    func isRecordingSentence(storyId: String, paragraphIndex: Int, sentenceIndex: Int) -> Bool {
        guard let location = activeSentenceRecordingLocation else { return false }
        return location.storyId == storyId && location.paragraphIndex == paragraphIndex && location.sentenceIndex == sentenceIndex
    }

    func recordingRetentionLimits() -> (perStory: Int, overall: Int) {
        (maxRecordingsPerStory, maxRecordingsOverall)
    }

    func startSentenceRecording(story: Story, paragraphIndex: Int, sentenceIndex: Int) async -> LMCRecordingStartResult {
        guard let location = story.lmcNormalizedSentenceLocation(
            paragraphIndex: paragraphIndex,
            sentenceIndex: sentenceIndex
        ) else {
            return .failed
        }

        let isPermissionGranted = await ensureMicrophonePermissionGranted()
        guard isPermissionGranted else {
            return .permissionDenied
        }

        guard let fileURL = makeRecordingFileURL(for: story.id, location: location) else {
            return .failed
        }

        stopSentenceRecording()

        stopSentencePlayback()
        stopRecordingPlayback()

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)

            let recorder = try AVAudioRecorder(url: fileURL, settings: lmcAudioRecordingSettings)
            recorder.prepareToRecord()
            guard recorder.record() else {
                return .failed
            }

            if let previous = activeSentenceRecording {
                try? FileManager.default.removeItem(at: previous.fileURL)
            }

            let active = LMCActiveSentenceRecording(
                location: location,
                fileURL: fileURL,
                startedAt: Date()
            )
            activeSentenceRecording = active
            sentenceRecorder = recorder
            activeSentenceRecordingLocation = location
            return .started
        } catch {
            return .failed
        }
    }

    func stopSentenceRecording() {
        guard let active = activeSentenceRecording, let recorder = sentenceRecorder else { return }
        defer {
            sentenceRecorder = nil
            activeSentenceRecording = nil
            activeSentenceRecordingLocation = nil
        }

        recorder.stop()

        let endAt = Date()
        let durationSeconds = max(0, endAt.timeIntervalSince(active.startedAt))
        let durationMillis = Int64(durationSeconds * 1_000)

        guard durationMillis > 250, FileManager.default.fileExists(atPath: active.fileURL.path) else {
            try? FileManager.default.removeItem(at: active.fileURL)
            return
        }

        let newRecording = VoiceRecording(
            id: UUID().uuidString,
            storyId: active.location.storyId,
            paragraphIndex: Int32(active.location.paragraphIndex),
            filePath: active.fileURL.path,
            durationMs: durationMillis,
            createdAtEpochMillis: Int64(active.startedAt.timeIntervalSince1970 * 1_000)
        )

        Task {
            _ = try? await self.voiceRecordingService.add(recording: newRecording)
            await self.syncRecordingsFromService()
        }
    }

    func playRecording(_ recording: VoiceRecording) async {
        stopSpeaking()
        stopSentenceRecording()
        stopRecordingPlayback()

        guard let url = localRecordingFileURL(for: recording) else { return }
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
            try AVAudioSession.sharedInstance().setActive(true)

            let player = try AVAudioPlayer(contentsOf: url)
            let delegate = LMCGeneratedAudioDelegate { [weak self] in
                Task { @MainActor [weak self] in
                    self?.recordingPlaybackPlayer = nil
                    self?.recordingPlaybackDelegate = nil
                }
            }
            player.prepareToPlay()
            player.delegate = delegate
            if player.play() {
                recordingPlaybackPlayer = player
                recordingPlaybackDelegate = delegate
            } else {
                stopRecordingPlayback()
            }
        } catch {
            stopRecordingPlayback()
        }
    }

    // MARK: - Optional retell recording (reuses the same local-only voice recording service)

    func isRecordingRetell(storyId: String) -> Bool {
        activeRetellRecordingStoryId == storyId
    }

    func latestRetellRecording(forStoryId storyId: String) -> VoiceRecording? {
        recordings
            .filter { $0.storyId == storyId && $0.paragraphIndex < 0 }
            .sorted { $0.createdAtEpochMillis > $1.createdAtEpochMillis }
            .first
    }

    func startRetellRecording(story: Story) async -> LMCRecordingStartResult {
        let isPermissionGranted = await ensureMicrophonePermissionGranted()
        guard isPermissionGranted else { return .permissionDenied }

        guard let directory = recordingsDirectory else { return .failed }
        let fileName = "\(story.id)_retell_\(UUID().uuidString).m4a"
            .replacingOccurrences(of: "/", with: "_")
        let fileURL = directory.appendingPathComponent(fileName)

        stopRetellRecording()
        stopSentenceRecording()
        stopSentencePlayback()
        stopRecordingPlayback()

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)

            let recorder = try AVAudioRecorder(url: fileURL, settings: lmcAudioRecordingSettings)
            recorder.prepareToRecord()
            guard recorder.record() else { return .failed }

            activeRetellRecording = LMCActiveRetellRecording(
                storyId: story.id,
                fileURL: fileURL,
                startedAt: Date()
            )
            retellRecorder = recorder
            activeRetellRecordingStoryId = story.id
            return .started
        } catch {
            return .failed
        }
    }

    func stopRetellRecording() {
        guard let active = activeRetellRecording, let recorder = retellRecorder else { return }
        defer {
            retellRecorder = nil
            activeRetellRecording = nil
            activeRetellRecordingStoryId = nil
        }

        recorder.stop()

        let durationMillis = Int64(max(0, Date().timeIntervalSince(active.startedAt)) * 1_000)
        guard durationMillis > 250, FileManager.default.fileExists(atPath: active.fileURL.path) else {
            try? FileManager.default.removeItem(at: active.fileURL)
            return
        }

        let newRecording = VoiceRecording(
            id: UUID().uuidString,
            storyId: active.storyId,
            paragraphIndex: -1,
            filePath: active.fileURL.path,
            durationMs: durationMillis,
            createdAtEpochMillis: Int64(active.startedAt.timeIntervalSince1970 * 1_000)
        )

        Task {
            _ = try? await self.voiceRecordingService.add(recording: newRecording)
            await self.syncRecordingsFromService()
        }
    }

    func deleteRecording(_ recordingId: String) async {
        guard let index = recordings.firstIndex(where: { $0.id == recordingId }) else { return }
        let recording = recordings.remove(at: index)
        if let fileURL = localRecordingFileURL(for: recording) {
            try? FileManager.default.removeItem(at: fileURL)
        }
        _ = try? await voiceRecordingService.delete(recordingId: recordingId)
        await syncRecordingsFromService()
    }

    func clearRecordings() async {
        let toDelete = recordings
        do {
            try await voiceRecordingService.clearAll()
        } catch {
            remove(recordings: toDelete)
            await syncRecordingsFromService()
            return
        }
        remove(recordings: toDelete)
        await syncRecordingsFromService()
    }

    func vocabSpeechText(_ word: Vocab) -> String {
        buildSpeechTextUseCase.vocab(word: word)
    }

    func stopSpeaking() {
        speechTask?.cancel()
        readingAudioTask?.cancel()
        stopGeneratedAudio()
        stopKaraoke()
        isSpeaking = false
        readingAudioStatus = .idle
        speechTask = Task { @MainActor in
            try? await ttsService.stop()
        }
    }

    func trackAppOpen(openType: LMCAppOpenType) {
        analytics.track(analytics.appOpenPayload(openType: openType))
    }

    func trackStoryOpen(_ story: Story, openSource: LMCStoryOpenSource) {
        analytics.track(
            ReaderAnalyticsEvents.shared.storyOpen(
                story: story,
                storyOrder: Int32(storyOrder(for: story)),
                openSource: openSource.rawValue,
                previousStoryStatus: storyProgress(for: story).status
            )
        )
    }

    func trackParagraphAudioPlay(
        _ story: Story,
        paragraphIndex: Int,
        sentenceIndex: Int? = nil,
        audioSource: LMCAudioSource = .tts,
        playbackSpeedBucket: String? = nil
    ) {
        analytics.track(
            ReaderAnalyticsEvents.shared.paragraphAudioPlay(
                storyId: story.id,
                paragraphIndex: Int32(paragraphIndex + 1),
                audioSource: audioSource.rawValue,
                sentenceIndex: sentenceIndex.map { KotlinInt(int: Int32($0)) },
                playbackSpeedBucket: playbackSpeedBucket
            )
        )
    }

    func trackPinyinToggle(_ story: Story, paragraphIndex: Int, enabled: Bool) {
        analytics.track(
            ReaderAnalyticsEvents.shared.pinyinToggle(
                storyId: story.id,
                enabled: enabled,
                surface: "reading",
                paragraphIndex: KotlinInt(int: Int32(paragraphIndex + 1))
            )
        )
    }

    func trackVocabOpen(_ story: Story, wordIndex: Int, openSource: LMCVocabOpenSource) {
        guard story.vocab.indices.contains(wordIndex) else { return }
        analytics.track(
            ReaderAnalyticsEvents.shared.vocabOpen(
                story: story,
                vocabIndex: Int32(wordIndex),
                openSource: openSource.rawValue
            )
        )
    }

    func trackQuizStart(_ story: Story) {
        analytics.track(
            ReaderAnalyticsEvents.shared.quizStart(story: story)
        )
    }

    func trackQuizComplete(_ story: Story, score: QuizScore) {
        analytics.track(
            ReaderAnalyticsEvents.shared.quizComplete(story: story, score: score)
        )
    }

    func trackParentReportOpen(entryPoint: LMCParentReportEntryPoint) {
        analytics.track(
            ReaderAnalyticsEvents.shared.parentReportOpen(
                entryPoint: entryPoint.rawValue,
                reportPeriod: "week"
            )
        )
    }

    func trackWordBookOpen() {
        let summary = wordBookSummary
        analytics.track(
            ReaderAnalyticsEvents.shared.wordBookOpen(
                entryPoint: "bottom_navigation",
                learnedCount: Int32(summary?.totalWords ?? 0),
                dueCount: Int32(summary?.dueCount ?? 0)
            )
        )
    }

    func trackWordReviewAnswer(item: WordBookItem, rating: String, reviewIndex: Int) {
        let storyId = item.sourceStoryIds.first as? String ?? ""
        analytics.track(
            ReaderAnalyticsEvents.shared.wordReviewAnswer(
                storyId: storyId,
                vocabId: "\(storyId):word_book",
                rating: rating,
                reviewIndex: Int32(reviewIndex),
                nextIntervalDays: Int32(item.intervalDays)
            )
        )
    }

    func trackWordReviewComplete(sessionSize: Int, reviewedCount: Int, knownCount: Int, needsPracticeCount: Int) {
        analytics.track(
            ReaderAnalyticsEvents.shared.wordReviewComplete(
                sessionSize: Int32(sessionSize),
                reviewedCount: Int32(reviewedCount),
                knownCount: Int32(knownCount),
                needsPracticeCount: Int32(needsPracticeCount)
            )
        )
    }

    func wordReviewItemsDueToday() -> [WordBookItem] {
        (wordBookSummary?.items ?? []).filter { $0.due }
    }

    func recordWordReview(item: WordBookItem, assessment: VocabReviewAssessment) async {
        _ = try? await vocabReviewUseCase.review(
            word: item.word,
            assessment: assessment,
            todayEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        try? await refreshWordBook()
    }

    func askAboutParagraph(story: Story, paragraphIndex: Int, selectedText: String, baseURL: String) async -> LMCAiAskState {
        let questionType = AiQuestionTypes.shared.ExplainSentence
        let result = await aiService.explain(
            storyId: story.id,
            selectedText: selectedText,
            questionType: questionType,
            baseURL: baseURL
        )
        analytics.track(
            ReaderAnalyticsEvents.shared.aiExplainRequest(
                storyId: story.id,
                requestType: questionType,
                safetyOutcome: result.analyticsOutcome.rawValue,
                targetType: "paragraph"
            )
        )
        switch result {
        case .answer(let answer, let outcome):
            await logAiInteraction(
                storyId: story.id,
                questionType: questionType,
                query: selectedText,
                answerPreview: answer,
                outcome: outcome
            )
            return .answered(answer)
        case .failure:
            return .failed
        }
    }

    // Resolve a tapped token against the story's curated vocab (shared WordLookupUseCase).
    func lookupWord(story: Story, token: String) -> WordLookupResult {
        wordLookupUseCase.lookup(story: story, token: token)
    }

    // Controlled AI fallback for a single word (question_type = explain_word). No open chat.
    func explainWord(story: Story, token: String, baseURL: String) async -> LMCAiAskState {
        let questionType = AiQuestionTypes.shared.ExplainWord
        let result = await aiService.explain(
            storyId: story.id,
            selectedText: token,
            questionType: questionType,
            baseURL: baseURL
        )
        analytics.track(
            ReaderAnalyticsEvents.shared.aiExplainRequest(
                storyId: story.id,
                requestType: questionType,
                safetyOutcome: result.analyticsOutcome.rawValue,
                targetType: "word"
            )
        )
        switch result {
        case .answer(let answer, let outcome):
            await logAiInteraction(
                storyId: story.id,
                questionType: questionType,
                query: token,
                answerPreview: answer,
                outcome: outcome
            )
            return .answered(answer)
        case .failure:
            return .failed
        }
    }

    // Append a controlled-AI answer to the local, parent-reviewable safety log.
    // Story content (query + short answer preview) + metadata only — never child PII.
    private func logAiInteraction(
        storyId: String,
        questionType: String,
        query: String,
        answerPreview: String,
        outcome: LMCAiSafetyOutcome
    ) async {
        let now = Int64(Date().timeIntervalSince1970 * 1_000)
        let record = AiInteractionRecord(
            id: "\(storyId)|\(questionType)|\(now)",
            storyId: storyId,
            questionType: questionType,
            query: query,
            answerPreview: answerPreview,
            outcome: outcome == .outOfScope ? .outofscope : .allowed,
            epochMillis: now
        )
        try? await aiInteractionLogService.append(record: record)
        await refreshAiInteractionLog()
    }

    func refreshAiInteractionLog() async {
        let records = (try? await aiInteractionLogService.read()) ?? []
        aiInteractionRecords = records
    }

    func deleteAiInteraction(id: String) async {
        try? await aiInteractionLogService.deleteById(id: id)
        await refreshAiInteractionLog()
    }

    func clearAiInteractionLog() async {
        try? await aiInteractionLogService.clear()
        await refreshAiInteractionLog()
    }

    var aiInteractionSummary: AiSafetyConsoleSummary {
        aiSafetyConsoleUseCases.summary(records: aiInteractionRecords)
    }

    func saveFeedback(_ draft: LMCFeedbackDraft) async throws {
        try await feedbackRepository.save(draft)
    }

    func readSettings() async -> ReaderSettings? {
        try? await settingsService.read()
    }

    func setLanguageTag(_ tag: String) async {
        try? await settingsService.setLanguage(
            language: ReaderLanguage.companion.fromTag(tag: tag)
        )
    }

    func setShowPinyinByDefault(_ enabled: Bool) async {
        try? await settingsService.setShowPinyinByDefault(showPinyin: enabled)
    }

    func setReadingSizeValue(_ value: String) async {
        try? await settingsService.setReadingTextSize(
            textSize: ReadingTextSize.companion.fromPrefValue(value: value)
        )
    }

    func setAiBackendBaseURL(_ value: String) async {
        try? await settingsService.setAiBackendBaseUrl(baseUrl: value)
    }

    func setSfxEnabled(_ enabled: Bool) async {
        try? await settingsService.setSfxEnabled(enabled: enabled)
        if !enabled {
            stopAllSfx()
        }
    }

    func setSfxVolume(_ value: Double) async {
        try? await settingsService.setSfxVolume(volume: Float(value))
    }

    func playSfxForQuizAnswerSubmitted(isCorrect: Bool) async {
        guard let settings = try? await settingsService.read() else { return }
        guard let cue = sfxEventReducer.quizAnswerSubmitted(
            isCorrect: isCorrect,
            settings: settings.sfxSettings
        ) else { return }
        await playSfx(cue)
    }

    func playSfxForStoryCompletion(milestoneDays: KotlinInt?) async {
        guard let settings = try? await settingsService.read() else { return }
        guard let cue = sfxEventReducer.storyCompleted(
            newMilestoneDays: milestoneDays,
            settings: settings.sfxSettings
        ) else { return }
        await playSfx(cue)
    }

    func playSfxPreview() async {
        guard let settings = try? await settingsService.read(),
              settings.sfxSettings.enabled else { return }
        await playSfxResource("sound_toggle_preview", volume: settings.sfxSettings.volume)
    }

    func stopAllSfx() {
        for player in sfxPlayers.values {
            player.stop()
            player.currentTime = 0
        }
    }

    func resetAiBackendBaseURL() async -> String {
        try? await settingsService.setAiBackendBaseUrl(baseUrl: "")
        return (try? await settingsService.read().aiBackendBaseUrl) ?? ""
    }

    func savedReadingParagraphIndex(for story: Story) async -> Int {
        let saved = (try? await settingsService.readReadingParagraphIndex(storyId: story.id)) ?? -1
        return Int(
            readingSessionReducer.initialState(
                story: story,
                savedParagraphIndex: Int32(saved),
                audioManifest: emptyAudioManifest(for: story),
                savedSentenceIndex: 0,
                playbackMode: sharedReadingMode,
                autoContinue: autoContinueEnabled,
                playbackSpeed: sharedPlaybackSpeed
            ).paragraphIndex
        )
    }

    func saveReadingParagraphIndex(_ index: Int, for story: Story) {
        readingPositions[story.id] = index
        Task { try? await settingsService.setReadingParagraphIndex(storyId: story.id, paragraphIndex: Int32(index)) }
    }

    func resetReadingParagraphIndex(for story: Story) async {
        readingPositions[story.id] = 0
        try? await settingsService.setReadingParagraphIndex(storyId: story.id, paragraphIndex: Int32(0))
    }

    func readingState(for story: Story, paragraphIndex: Int) -> ReadingSessionState {
        readingSessionReducer.stateFor(
            story: story,
            paragraphIndex: Int32(paragraphIndex),
            sentenceIndex: 0,
            audioManifest: emptyAudioManifest(for: story),
            playbackStatus: .stopped,
            playbackMode: sharedReadingMode,
            autoContinue: autoContinueEnabled,
            playbackSpeed: sharedPlaybackSpeed
        )
    }

    func previousReadingState(_ story: Story, state: ReadingSessionState) -> ReadingSessionState {
        readingSessionReducer.previous(
            story: story,
            state: state,
            audioManifest: emptyAudioManifest(for: story)
        )
    }

    func nextReadingTransition(_ story: Story, state: ReadingSessionState) -> ReadingSessionTransition {
        readingSessionReducer.next(
            story: story,
            state: state,
            audioManifest: emptyAudioManifest(for: story)
        )
    }

    private func emptyAudioManifest(for story: Story) -> StoryAudioManifest {
        StoryAudioManifest.companion.empty(storyId: story.id)
    }

    private var sharedReadingMode: ReadingSessionMode {
        switch readingMode {
        case .readAlong:
            return .readalong
        case .tapToListen:
            return .taptolisten
        }
    }

    private var sharedPlaybackSpeed: ReadingPlaybackSpeed {
        switch playbackSpeed {
        case .slow:
            return .slow
        case .defaultSlow:
            return .defaultslow
        case .normal:
            return .normal
        }
    }

    func filteredStories(level selectedLevel: Int?) -> [Story] {
        storyPresentationUseCases.filterStoriesByLevel(
            stories: stories,
            selectedLevel: selectedLevel.map { KotlinInt(int: Int32($0)) }
        )
    }

    func availableLibraryLevels() -> [Int] {
        Array(Set(stories.map { Int($0.level) })).sorted()
    }

    func initialQuizState(_ story: Story) -> QuizSessionState {
        quizSessionReducer.initialState(story: story)
    }

    func quizQuestionState(_ story: Story, state: QuizSessionState) -> QuizQuestionState {
        quizSessionReducer.questionState(story: story, state: state)
    }

    func selectQuizAnswer(_ story: Story, state: QuizSessionState, answer: String) -> QuizSessionState {
        quizSessionReducer.selectAnswer(story: story, state: state, answer: answer)
    }

    func submitOrAdvanceQuiz(_ story: Story, state: QuizSessionState) -> QuizSessionState {
        quizSessionReducer.submitOrAdvance(story: story, state: state)
    }

    func quizScore(_ story: Story, state: QuizSessionState) -> QuizScore {
        quizSessionReducer.score(story: story, state: state)
    }

    func completionRecord(_ story: Story, state: QuizSessionState) -> CompletionRecord {
        quizSessionReducer.completionRecord(
            story: story,
            state: state,
            nowEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000)
        )
    }

    func parentReportSummary() -> ParentReportSummary? {
        guard let parentReport, let stats else { return nil }
        let nextStoryId = stories.first { !completedStoryIds.contains($0.id) }?.id
        return buildParentReportSummaryUseCase.invoke(
            report: parentReport,
            stats: stats,
            nowEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            weekWindowMillis: LMCSevenDaysMillis,
            dueWordCount: Int32(wordBookSummary?.dueCount ?? 0),
            nextStoryId: nextStoryId
        )
    }

    private func speak(_ text: String) {
        speechTask?.cancel()
        readingAudioTask?.cancel()
        stopGeneratedAudio()
        readingAudioStatus = .idle
        speechTask = Task { @MainActor in
            isSpeaking = true
            try? await ttsService.speak(text: text, speedMultiplier: 1.0)
        }
    }

    // MARK: - Karaoke (per-character highlight)

    private func registerKaraokeRangeListener() {
        // System-TTS fallback path: native willSpeakRangeOfSpeechString reports the
        // spoken UTF-16 range; map it to a code-point index via the shared reducer.
        // TtsRangeListener is a Kotlin function type; its Int params bridge to
        // KotlinInt, so the closure receives boxed integers.
        ttsService.setRangeListener(listener: { [weak self] (utf16Start: KotlinInt, _: KotlinInt) in
            let startOffset = utf16Start.int32Value
            Task { @MainActor [weak self] in
                guard let self else { return }
                guard case .playing(_, .tts) = self.readingAudioStatus else { return }
                let timeline = self.activeKaraokeTimeline
                guard !timeline.isEmpty else { return }
                let text = timeline.chars.map { $0.character }.joined()
                if let index = self.karaokeReducer.charIndexForUtf16Range(
                    text: text,
                    utf16Start: startOffset
                )?.intValue {
                    self.activeCharIndex = index
                }
            }
        })
    }

    @discardableResult
    private func audioManifest(for story: Story) async -> StoryAudioManifest {
        if let cached = audioManifests[story.id] {
            return cached
        }
        let manifest = (try? await loadAudioManifestUseCase.invoke(storyId: story.id))
            ?? StoryAudioManifest.companion.empty(storyId: story.id)
        audioManifests[story.id] = manifest
        return manifest
    }

    /// Warm the per-character timing manifest cache before playback so the
    /// recorded-audio karaoke highlight is accurate from the first sentence.
    func prepareReadingAudio(for story: Story) async {
        await audioManifest(for: story)
    }

    private func beginKaraoke(story: Story, location: LMCSentenceLocation, source: LMCAudioSource) {
        guard let sentence = story.lmcSentence(at: location) else {
            stopKaraoke()
            return
        }
        let segment = audioManifests[story.id]?.segmentFor(
            paragraphIndex: Int32(location.paragraphIndex),
            sentenceIndex: Int32(location.sentenceIndex)
        )
        let timeline: KaraokeTimeline
        if source == .recorded, let segment {
            timeline = karaokeReducer.timelineForSegment(segment: segment)
        } else {
            // TTS fallback (or missing manifest timings): only the per-character
            // text is needed; native range callbacks drive the cursor.
            timeline = karaokeReducer.timelineForText(text: sentence.text, durationMillis: 0)
        }
        activeKaraokeTimeline = timeline
        activeCharIndex = timeline.isEmpty ? nil : 0
        karaokeTimer?.invalidate()
        karaokeTimer = nil

        // Recorded path: poll the player position and resolve the active char.
        if source == .recorded {
            let timer = Timer.scheduledTimer(withTimeInterval: 0.06, repeats: true) { [weak self] _ in
                Task { @MainActor [weak self] in
                    guard let self, let player = self.generatedAudioPlayer, player.isPlaying else { return }
                    let positionMillis = Int64(player.currentTime * 1000)
                    if let index = self.karaokeReducer.charIndexAt(
                        timeline: self.activeKaraokeTimeline,
                        positionMillis: positionMillis
                    )?.intValue {
                        self.activeCharIndex = index
                    }
                }
            }
            RunLoop.main.add(timer, forMode: .common)
            karaokeTimer = timer
        }
    }

    private func stopKaraoke() {
        karaokeTimer?.invalidate()
        karaokeTimer = nil
        activeKaraokeTimeline = KaraokeTimeline.companion.Empty
        activeCharIndex = nil
    }

    private var lmcAudioRecordingSettings: [String: Any] {
        [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 44_100,
            AVNumberOfChannelsKey: 1,
            AVEncoderAudioQualityKey: AVAudioQuality.high.rawValue,
            AVEncoderBitRateKey: 96_000
        ]
    }

    private var recordingsDirectory: URL? {
        guard let appSupport = try? FileManager.default.url(
            for: .applicationSupportDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        ) else { return nil }
        let target = appSupport.appendingPathComponent(readingRecordingsDirectoryName, isDirectory: true)
        do {
            try FileManager.default.createDirectory(at: target, withIntermediateDirectories: true)
            return target
        } catch {
            return nil
        }
    }

    private func localRecordingFileURL(for recording: VoiceRecording) -> URL? {
        if recording.filePath.hasPrefix("/") {
            return URL(fileURLWithPath: recording.filePath)
        }
        return recordingsDirectory?.appendingPathComponent(recording.filePath)
    }

    private func localRecordingFileURL(for storyId: String, location: LMCSentenceLocation) -> URL? {
        latestRecording(forStoryId: storyId, paragraphIndex: location.paragraphIndex, sentenceIndex: location.sentenceIndex)
            .flatMap({ localRecordingFileURL(for: $0) })
    }

    private func latestRecording(
        forStoryId storyId: String,
        paragraphIndex: Int,
        sentenceIndex: Int
    ) -> VoiceRecording? {
        recordings
            .filter {
                $0.storyId == storyId &&
                paragraphAndSentenceIndex(for: $0)?.0 == paragraphIndex &&
                paragraphAndSentenceIndex(for: $0)?.1 == sentenceIndex
            }
            .sorted { $0.createdAtEpochMillis > $1.createdAtEpochMillis }
            .first
    }

    private func makeRecordingFileURL(for storyId: String, location: LMCSentenceLocation) -> URL? {
        guard let directory = recordingsDirectory else { return nil }
        let fileName = "\(storyId)_p\(location.paragraphIndex + 1)_s\(location.sentenceIndex + 1)_\(UUID().uuidString).m4a"
            .replacingOccurrences(of: "/", with: "_")
        return directory.appendingPathComponent(fileName)
    }

    private func syncRecordingsFromService() async {
        let previous = recordings
        let loaded = try? await voiceRecordingService.getRecordings(storyId: nil)
        recordings = loaded ?? []

        let removed = previous.filter { old in
            !recordings.contains { $0.id == old.id }
        }
        remove(recordings: removed)
        pruneRecordingOrphans(against: recordings)
    }

    private func pruneRecordingOrphans(against knownRecordings: [VoiceRecording]) {
        guard let directory = recordingsDirectory else { return }
        guard let entries = try? FileManager.default.contentsOfDirectory(atPath: directory.path) else { return }

        let referencedPaths = Set(knownRecordings.compactMap { recording in
            localRecordingFileURL(for: recording)?.path
        })

        for entry in entries where entry.hasSuffix(".m4a") {
            let entryPath = directory.appendingPathComponent(entry).path
            if !referencedPaths.contains(entryPath) {
                try? FileManager.default.removeItem(atPath: entryPath)
            }
        }
    }

    private func reloadRecordingsFromService() async {
        await syncRecordingsFromService()
    }

    private func remove(recordings removeList: [VoiceRecording]) {
        for recording in removeList {
            guard let fileURL = localRecordingFileURL(for: recording), FileManager.default.fileExists(atPath: fileURL.path) else {
                continue
            }
            try? FileManager.default.removeItem(at: fileURL)
        }
    }

    private func paragraphAndSentenceIndex(for recording: VoiceRecording) -> (Int, Int)? {
        guard let fileName = localRecordingFileURL(for: recording)?.lastPathComponent else { return nil }
        let stem = fileName
            .split(separator: ".")
            .first
            .map { String($0) }
            ?? fileName
        let parts = stem.split(separator: "_")

        var paragraphIndex = -1
        var sentenceIndex = -1

        for part in parts {
            if part.hasPrefix("p"),
               let parsed = Int(part.dropFirst()) {
                paragraphIndex = parsed - 1
            }
            if part.hasPrefix("s"),
               let parsed = Int(part.dropFirst()) {
                sentenceIndex = parsed - 1
            }
        }

        guard paragraphIndex >= 0, sentenceIndex >= 0 else { return nil }
        return (paragraphIndex, sentenceIndex)
    }

    private func ensureMicrophonePermissionGranted() async -> Bool {
        switch AVAudioSession.sharedInstance().recordPermission {
        case .granted:
            return true
        case .undetermined:
            return await withCheckedContinuation { continuation in
                AVAudioSession.sharedInstance().requestRecordPermission { granted in
                    continuation.resume(returning: granted)
                }
            }
        case .denied:
            return false
        @unknown default:
            return false
        }
    }

    private func stopRecordingPlayback() {
        recordingPlaybackPlayer?.stop()
        recordingPlaybackPlayer = nil
        recordingPlaybackDelegate = nil
    }

    private func playReadingSentence(story: Story, location: LMCSentenceLocation, shouldTrack: Bool) {
        guard let sentence = story.lmcSentence(at: location) else {
            stopSpeaking()
            return
        }

        speechTask?.cancel()
        readingAudioTask?.cancel()
        stopGeneratedAudio()
        karaokeTimer?.invalidate()
        karaokeTimer = nil

        if let localURL = localRecordingFileURL(for: story.id, location: location),
           playGeneratedSentenceAudio(
            url: localURL,
            story: story,
            location: location,
            shouldTrack: false,
            completion: { [weak self] in
                Task { @MainActor [weak self] in
                    self?.finishReadingSentence(story: story, location: location)
                }
            }
           ) {
            return
        }

        if let url = LMCSentenceAudioResolver.generatedAudioURL(storyId: story.id, location: location),
           playGeneratedSentenceAudio(
            url: url,
            story: story,
            location: location,
            shouldTrack: shouldTrack,
            completion: { [weak self] in
                Task { @MainActor [weak self] in
                    self?.finishReadingSentence(story: story, location: location)
                }
            }
           ) {
            return
        }

        playTtsSentenceAudio(text: sentence.text, story: story, location: location, shouldTrack: shouldTrack)
    }

    private func playGeneratedSentenceAudio(
        url: URL,
        story: Story,
        location: LMCSentenceLocation,
        shouldTrack: Bool,
        completion: @escaping () -> Void
    ) -> Bool {
        do {
            try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
            try? AVAudioSession.sharedInstance().setActive(true)

            let player = try AVAudioPlayer(contentsOf: url)
            player.enableRate = true
            player.rate = Float(playbackSpeed.multiplier)
            let delegate = LMCGeneratedAudioDelegate { [weak self] in
                Task { @MainActor [weak self] in
                    completion()
                }
            }
            player.delegate = delegate
            player.prepareToPlay()
            guard player.play() else { return false }

            generatedAudioPlayer = player
            generatedAudioDelegate = delegate
            readingAudioStatus = .playing(location, .recorded)
            isSpeaking = true
            beginKaraoke(story: story, location: location, source: .recorded)
            if shouldTrack {
                trackParagraphAudioPlay(
                    story,
                    paragraphIndex: location.paragraphIndex,
                    sentenceIndex: location.sentenceIndex,
                    audioSource: .recorded,
                    playbackSpeedBucket: playbackSpeed.analyticsBucket
                )
            }
            return true
        } catch {
            stopGeneratedAudio()
            return false
        }
    }

    private func playTtsSentenceAudio(
        text: String,
        story: Story,
        location: LMCSentenceLocation,
        shouldTrack: Bool
    ) {
        readingAudioStatus = .playing(location, .tts)
        isSpeaking = true
        beginKaraoke(story: story, location: location, source: .tts)
        if shouldTrack {
            trackParagraphAudioPlay(
                story,
                paragraphIndex: location.paragraphIndex,
                sentenceIndex: location.sentenceIndex,
                audioSource: .tts,
                playbackSpeedBucket: playbackSpeed.analyticsBucket
            )
        }

        readingAudioTask = Task { @MainActor in
            try? await ttsService.speak(text: text, speedMultiplier: Float(playbackSpeed.multiplier))
            do {
                try await Task.sleep(
                    nanoseconds: LMCTtsDurationEstimator.nanoseconds(for: text, speedMultiplier: playbackSpeed.multiplier)
                )
            } catch {
                return
            }
            guard !Task.isCancelled else { return }
            try? await ttsService.stop()
            readingAudioTask = nil
            finishReadingSentence(story: story, location: location)
        }
    }

    private func finishReadingSentence(story: Story, location: LMCSentenceLocation) {
        guard readingAudioStatus.location == location else { return }
        guard activeSentenceShouldAutoContinue else {
            readingAudioStatus = .stopped(location)
            isSpeaking = false
            stopGeneratedAudio()
            stopKaraoke()
            return
        }
        guard let nextLocation = story.lmcNextSentenceLocation(after: location) else {
            readingAudioStatus = .idle
            isSpeaking = false
            stopGeneratedAudio()
            stopKaraoke()
            return
        }
        playReadingSentence(story: story, location: nextLocation, shouldTrack: true)
    }

    private func stopGeneratedAudio() {
        generatedAudioPlayer?.stop()
        generatedAudioPlayer = nil
        generatedAudioDelegate = nil
    }

    private func playSfx(_ cue: SfxCue) async {
        guard let player = await sfxPlayer(for: cue.semanticKey) else { return }
        await configureSfxAudioSession()
        player.volume = cue.volume
        if player.isPlaying {
            player.stop()
            player.currentTime = 0
        }
        player.prepareToPlay()
        player.play()
    }

    private func playSfxResource(_ resourceName: String, volume: Float) async {
        guard let player = await sfxPlayer(for: resourceName) else { return }
        await configureSfxAudioSession()
        player.volume = volume
        if player.isPlaying {
            player.stop()
            player.currentTime = 0
        }
        player.prepareToPlay()
        player.play()
    }

    private func sfxPlayer(for resourceName: String) async -> AVAudioPlayer? {
        if let player = sfxPlayers[resourceName] { return player }

        guard let url = Bundle.main.url(forResource: resourceName, withExtension: "wav", subdirectory: "sfx") else {
            return nil
        }
        guard let player = try? AVAudioPlayer(contentsOf: url) else { return nil }
        player.prepareToPlay()
        sfxPlayers[resourceName] = player
        return player
    }

    private func configureSfxAudioSession() async {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.ambient, mode: .default, options: [.mixWithOthers])
        try? session.setActive(true)
    }

    private func refreshProgress() async throws {
        let records = try await progressService.getRecords()
        completedStoryIds = storyPresentationUseCases.completedStoryIds(records: records)
        stats = try await getProgressStatsUseCase.invoke()
        parentReport = try await buildParentReportUseCase.invoke(
            nowEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            weekWindowMillis: LMCSevenDaysMillis
        )
    }

    private func refreshWordBook() async throws {
        try await vocabReviewUseCase.syncLearnedWords(
            todayEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        wordBookSummary = try await vocabReviewUseCase.wordBook(
            todayEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        vocabRecords = try await vocabReviewService.getRecords()
    }

    private let parentWeeklyPlanUseCases = ParentWeeklyPlanUseCases()

    // Pure shared weekly action plan. Clock is read here (display-time only); the
    // shared core stays pure. No child PII anywhere (AGENTS.md §7).
    func parentWeeklyPlan() -> ParentWeeklyPlan {
        let nowMillis = Int64(Date().timeIntervalSince1970 * 1_000)
        let wordStates = vocabRecords.map { record in
            WeeklyPlanWordState(
                word: record.word,
                pinyin: record.pinyin,
                meaning: record.meaning,
                intervalDays: record.intervalDays,
                dueEpochDay: record.dueEpochDay,
                lapses: record.lapses
            )
        }
        return parentWeeklyPlanUseCases.buildWeeklyPlan(
            stories: stories,
            completionRecords: completionRecords,
            wordStates: wordStates,
            todayEpochDay: Int32(nowMillis / 86_400_000),
            nowEpochMillis: nowMillis,
            streakDays: streakSummary?.currentStreakDays ?? 0
        )
    }

    private func refreshStreak() async throws {
        streakSummary = try await streakUseCase.summary(
            todayEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000)
        )
    }

    private func refreshReadingPositions() async throws {
        var positions: [String: Int] = [:]
        for story in stories {
            positions[story.id] = Int(try await settingsService.readReadingParagraphIndex(storyId: story.id))
        }
        readingPositions = positions
    }

    private func trackStoryComplete(_ story: Story, quizCompleted: Bool) {
        analytics.track(
            ReaderAnalyticsEvents.shared.storyComplete(
                story: story,
                storyOrder: Int32(storyOrder(for: story)),
                quizCompleted: quizCompleted
            )
        )
    }

    private func storyOrder(for story: Story) -> Int {
        (stories.firstIndex { $0.id == story.id } ?? 0) + 1
    }

    private func storyProgress(for story: Story) -> StoryProgress {
        storyPresentationUseCases.storyProgress(
            story: story,
            completedStoryIds: completedStoryIds,
            savedParagraphIndex: Int32(readingPositions[story.id] ?? -1)
        )
    }
}

private let LMCSevenDaysMillis: Int64 = 7 * 24 * 60 * 60 * 1_000

enum LMCLoadingState {
    case idle
    case loading
    case loaded
    case failed
}

enum LMCLaunchGate {
    case loading
    case onboarding
    case ready
}

enum LMCTab: String, CaseIterable {
    case today
    case library
    case wordBook
    case parent

    var titleKey: LocalizedStringKey {
        switch self {
        case .today: return "nav_today"
        case .library: return "nav_library"
        case .wordBook: return "nav_word_book"
        case .parent: return "nav_parent"
        }
    }

    var icon: String {
        switch self {
        case .today: return "sun.max.fill"
        case .library: return "books.vertical.fill"
        case .wordBook: return "textformat.characters"
        case .parent: return "chart.bar.xaxis"
        }
    }
}

enum LMCFlowRoute {
    case reading(storyId: String)
    case vocabulary(storyId: String, openSource: LMCVocabOpenSource)
    case quiz(storyId: String)
    case wordReview
    case settings
    case reviewPack
    case abilityMap
}

enum LMCAppOpenType: String {
    case coldStart = "cold_start"
    case foreground
}

enum LMCStoryOpenSource: String {
    case todayHero = "today_hero"
    case todayUpNext = "today_up_next"
    case library
    case parentReport = "parent_report"
    case quizCompletion = "quiz_completion"
}

enum LMCVocabOpenSource: String {
    case todaySummary = "today_summary"
    case readingFlow = "reading_flow"
    case quizBack = "quiz_back"
}

enum LMCAudioSource: String {
    case recorded
    case tts
}

enum LMCReadingPlaybackMode: Hashable {
    case readAlong
    case tapToListen
}

enum LMCReadingPlaybackSpeed: CaseIterable, Hashable {
    case slow
    case defaultSlow
    case normal

    var multiplier: Double {
        switch self {
        case .slow:
            return 0.80
        case .defaultSlow:
            return 0.90
        case .normal:
            return 1.00
        }
    }

    var analyticsBucket: String {
        switch self {
        case .slow:
            return "slow"
        case .defaultSlow:
            return "default_slow"
        case .normal:
            return "normal"
        }
    }

    var labelKey: String {
        switch self {
        case .slow:
            return "reading_speed_slow"
        case .defaultSlow:
            return "reading_speed_default"
        case .normal:
            return "reading_speed_normal"
        }
    }
}

struct LMCSentenceLocation: Equatable {
    let storyId: String
    let paragraphIndex: Int
    let sentenceIndex: Int

    var scrollId: String {
        "sentence-\(storyId)-\(paragraphIndex)-\(sentenceIndex)"
    }
}

private struct LMCActiveSentenceRecording {
    let location: LMCSentenceLocation
    let fileURL: URL
    let startedAt: Date
}

private struct LMCActiveRetellRecording {
    let storyId: String
    let fileURL: URL
    let startedAt: Date
}

enum LMCReadingAudioStatus {
    case idle
    case playing(LMCSentenceLocation, LMCAudioSource)
    case paused(LMCSentenceLocation, LMCAudioSource)
    case stopped(LMCSentenceLocation)

    var location: LMCSentenceLocation? {
        switch self {
        case .idle:
            return nil
        case .playing(let location, _), .paused(let location, _):
            return location
        case .stopped(let location):
            return location
        }
    }

    var isActive: Bool {
        switch self {
        case .playing, .paused:
            return true
        case .idle, .stopped:
            return false
        }
    }

    var isPaused: Bool {
        if case .paused = self { return true }
        return false
    }
}

enum LMCParentReportEntryPoint: String {
    case bottomNavigation = "bottom_navigation"
    case todayHeader = "today_header"
    case settings
}

enum LMCWordBookFilter: String, CaseIterable {
    case all
    case due
    case learning
    case known

    var titleKey: LocalizedStringKey {
        switch self {
        case .all: return "word_book_filter_all"
        case .due: return "word_book_filter_due"
        case .learning: return "word_book_filter_learning"
        case .known: return "word_book_filter_known"
        }
    }
}

enum LMCAppLocale: String, CaseIterable {
    case english = "en"
    case simplifiedChinese = "zh-Hans"

    var labelKey: LocalizedStringKey {
        switch self {
        case .english: return "settings_language_english"
        case .simplifiedChinese: return "settings_language_chinese"
        }
    }
}

enum LMCOnboardingAgeBand: String, CaseIterable {
    case age5To6
    case age7To8

    var titleKey: LocalizedStringKey {
        switch self {
        case .age5To6: return "onboarding_age_5_6"
        case .age7To8: return "onboarding_age_7_8"
        }
    }

    var sharedValue: ChildAgeBand {
        switch self {
        case .age5To6: return .age5to6
        case .age7To8: return .age7to8
        }
    }
}

enum LMCReadingSize: String, CaseIterable {
    case small
    case medium
    case large

    var labelKey: LocalizedStringKey {
        switch self {
        case .small: return "reading_size_small"
        case .medium: return "reading_size_medium"
        case .large: return "reading_size_large"
        }
    }

    var hanziFont: Font {
        switch self {
        case .small: return .system(size: 22, weight: .medium, design: .serif)
        case .medium: return .system(size: 26, weight: .medium, design: .serif)
        case .large: return .system(size: 30, weight: .medium, design: .serif)
        }
    }

    var pinyinFont: Font {
        switch self {
        case .small: return .system(size: 13, weight: .regular, design: .rounded)
        case .medium: return .system(size: 15, weight: .regular, design: .rounded)
        case .large: return .system(size: 17, weight: .regular, design: .rounded)
        }
    }

    var hanziLineSpacing: CGFloat {
        switch self {
        case .small: return 8
        case .medium: return 10
        case .large: return 12
        }
    }

    var hanziLineHeight: CGFloat {
        switch self {
        case .small: return 36
        case .medium: return 42
        case .large: return 48
        }
    }

    var pinyinLineHeight: CGFloat {
        switch self {
        case .small: return 22
        case .medium: return 24
        case .large: return 28
        }
    }

    static func value(from rawValue: String) -> LMCReadingSize {
        LMCReadingSize(rawValue: rawValue) ?? .medium
    }
}

struct LMCUserDefaultsAnalyticsAdapter {
    private static let firstOpenEpochKey = "lmc_first_open_epoch_seconds"

    private let defaults: UserDefaults
    private let service: Analytics

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.service = IosAnalyticsServiceKt.createPlatformAnalytics(
            appVersion: LMCBundleInfo.appVersion,
            uiLocale: defaults.string(forKey: "lmc_locale_identifier") ?? LMCAppLocale.english.rawValue
        )
    }

    func appOpenPayload(openType: LMCAppOpenType) -> AnalyticsEventPayload {
        let now = Date()
        let firstOpenEpoch = defaults.double(forKey: Self.firstOpenEpochKey)
        let isFirstOpen = firstOpenEpoch == 0
        if isFirstOpen {
            defaults.set(now.timeIntervalSince1970, forKey: Self.firstOpenEpochKey)
        }
        let firstOpenDate = Date(timeIntervalSince1970: isFirstOpen ? now.timeIntervalSince1970 : firstOpenEpoch)
        let daysSinceFirstOpen = Calendar.current.dateComponents([.day], from: firstOpenDate, to: now).day ?? 0
        return ReaderAnalyticsEvents.shared.appOpen(
            openType: openType.rawValue,
            isFirstOpen: KotlinBoolean(bool: isFirstOpen),
            daysSinceFirstOpen: KotlinInt(int: Int32(max(0, daysSinceFirstOpen)))
        )
    }

    func track(_ payload: AnalyticsEventPayload) {
        service.track(
            eventName: payload.eventName,
            properties: payload.properties
        ) { _, _ in }
    }
}

enum LMCAiSafetyOutcome: String {
    case allowed
    case outOfScope = "out_of_scope"
    case error
}

enum LMCAiAskState: Equatable {
    case idle
    case loading
    case answered(String)
    case failed
}

// Identifiable wrapper so the tap-word lookup can drive a SwiftUI .sheet(item:).
struct LMCWordLookup: Identifiable {
    let id = UUID()
    let result: WordLookupResult

    var curated: WordLookupResultCurated? { result as? WordLookupResultCurated }
    var needsAi: WordLookupResultNeedsAi? { result as? WordLookupResultNeedsAi }
}

enum LMCAiExplanationResult {
    case answer(String, LMCAiSafetyOutcome)
    case failure

    var analyticsOutcome: LMCAiSafetyOutcome {
        switch self {
        case .answer(_, let outcome): return outcome
        case .failure: return .error
        }
    }
}

struct LMCAiExplanationAdapter {
    private let backendClient = LMCSharedAiBackendClient()
    private let buildRequest = BuildAiExplanationRequestUseCase()

    func explain(storyId: String, selectedText: String, questionType: String, baseURL: String) async -> LMCAiExplanationResult {
        let trimmedBaseURL = baseURL.trimmingCharacters(in: .whitespacesAndNewlines)
        guard let request = buildRequest.forSelectedText(
            storyId: storyId,
            selectedText: selectedText,
            questionType: questionType,
            childAge: 6
        ) else {
            let outOfScope = LMCStrings.localized("ai_out_of_scope")
            let answer = AiExplanationResponse(
                answer: outOfScope,
                messageKey: AiMessageKeys.shared.OutOfScope
            ).toLimitedDisplayText(
                stubText: LMCStrings.localized("ai_mock_answer"),
                outOfScopeText: outOfScope
            )
            return .answer(answer, .outOfScope)
        }

        let useMock = ReaderSettingsKt.isMockAiBackend(trimmedBaseURL)
        let config = AiServiceConfig(
            baseUrl: useMock ? nil : trimmedBaseURL,
            apiKey: nil,
            maxSelectedTextLength: AiPresentationKt.AiSelectedTextMaxChars,
            maxAnswerLength: AiPresentationKt.AiAnswerMaxChars
        )
        let service = AiServiceKt.createAiService(
            config: config,
            backendClient: useMock ? nil : backendClient
        )
        do {
            let response = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<AiExplanationResponse, Error>) in
                service.explain(request: request) { response, error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else if let response {
                        continuation.resume(returning: response)
                    } else {
                        continuation.resume(throwing: LMCSharedAiBackendError.emptyResponse)
                    }
                }
            }
            let outOfScope = LMCStrings.localized("ai_out_of_scope")
            let answer = response.toLimitedDisplayText(
                stubText: LMCStrings.localized("ai_mock_answer"),
                outOfScopeText: outOfScope
            )
            let outcome = response.safetyOutcome(displayedAnswer: answer, outOfScopeText: outOfScope) == "out_of_scope"
                ? LMCAiSafetyOutcome.outOfScope
                : .allowed
            return .answer(answer, outcome)
        } catch {
            return .failure
        }
    }
}

final class LMCSharedAiBackendClient: NSObject, AiExplainBackendClient {
    func postExplain(
        baseUrl: String,
        apiKey: String?,
        request: AiExplanationRequest,
        completionHandler: @escaping (AiExplanationResponse?, Error?) -> Void
    ) {
        Task {
            do {
                completionHandler(
                    try await performPostExplain(baseUrl: baseUrl, apiKey: apiKey, request: request),
                    nil
                )
            } catch {
                completionHandler(nil, error)
            }
        }
    }

    private func performPostExplain(baseUrl: String, apiKey: String?, request: AiExplanationRequest) async throws -> AiExplanationResponse {
        guard let base = URL(string: baseUrl), ["http", "https"].contains(base.scheme?.lowercased()) else {
            throw LMCSharedAiBackendError.invalidBaseURL
        }

        let endpoint = baseUrl.hasSuffix("/ai/explain") ? base : base.appendingPathComponent("ai/explain")
        var urlRequest = URLRequest(url: endpoint)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let apiKey, !apiKey.isEmpty {
            urlRequest.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        }
        urlRequest.httpBody = try JSONEncoder().encode(
            LMCBackendAiExplainRequest(
                storyId: request.storyId,
                selectedText: request.selectedText,
                questionType: request.questionType,
                childAge: Int(request.childAge)
            )
        )

        let (data, response) = try await URLSession.shared.data(for: urlRequest)
        guard let httpResponse = response as? HTTPURLResponse, 200..<300 ~= httpResponse.statusCode else {
            throw LMCSharedAiBackendError.badStatus
        }

        let decoded = try JSONDecoder().decode(LMCBackendAiExplainResponse.self, from: data)
        if decoded.messageKey == "ai_out_of_scope" {
            return AiExplanationResponse(
                answer: LMCStrings.localized("ai_out_of_scope"),
                messageKey: "ai_out_of_scope"
            )
        }
        if let messageKey = decoded.messageKey, !messageKey.isEmpty {
            return AiExplanationResponse(
                answer: LMCStrings.localized(messageKey),
                messageKey: messageKey
            )
        }
        if let answer = decoded.answer?.trimmingCharacters(in: .whitespacesAndNewlines), !answer.isEmpty {
            return AiExplanationResponse(answer: answer, messageKey: nil)
        }

        throw LMCSharedAiBackendError.emptyResponse
    }
}

private enum LMCSharedAiBackendError: Error {
    case badStatus
    case emptyResponse
    case invalidBaseURL
}

private struct LMCBackendAiExplainRequest: Encodable {
    let storyId: String
    let selectedText: String
    let questionType: String
    let childAge: Int

    enum CodingKeys: String, CodingKey {
        case storyId = "story_id"
        case selectedText = "selected_text"
        case questionType = "question_type"
        case childAge = "child_age"
    }
}

private struct LMCBackendAiExplainResponse: Decodable {
    let answer: String?
    let messageKey: String?

    enum CodingKeys: String, CodingKey {
        case answer
        case messageKey
        case messageKeySnake = "message_key"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        answer = try container.decodeIfPresent(String.self, forKey: .answer)
        messageKey = try container.decodeIfPresent(String.self, forKey: .messageKey)
            ?? container.decodeIfPresent(String.self, forKey: .messageKeySnake)
    }
}

struct LMCFeedbackDraft {
    var satisfaction: LMCFeedbackSatisfaction = .satisfied
    var childAgeBand: LMCFeedbackAgeBand = .fiveToSix
    var issueType: LMCFeedbackIssueType = .content
    var suggestion = ""
    var parentContact = ""

    var canSubmit: Bool {
        BuildFeedbackSubmissionUseCase().canSubmit(suggestion: suggestion)
    }
}

enum LMCFeedbackSatisfaction: String, CaseIterable, Codable {
    case verySatisfied = "very_satisfied"
    case satisfied
    case needsWork = "needs_work"

    var labelKey: LocalizedStringKey {
        switch self {
        case .verySatisfied: return "feedback_satisfaction_very_satisfied"
        case .satisfied: return "feedback_satisfaction_satisfied"
        case .needsWork: return "feedback_satisfaction_needs_work"
        }
    }
}

enum LMCFeedbackAgeBand: String, CaseIterable, Codable {
    case fiveToSix = "5_6"
    case sevenToEight = "7_8"
    case preferNotToSay = "prefer_not_to_say"

    var labelKey: LocalizedStringKey {
        switch self {
        case .fiveToSix: return "feedback_age_5_6"
        case .sevenToEight: return "feedback_age_7_8"
        case .preferNotToSay: return "feedback_age_prefer_not_to_say"
        }
    }
}

enum LMCFeedbackIssueType: String, CaseIterable, Codable {
    case content
    case audio
    case aiExplanation = "ai_explanation"
    case bug
    case other

    var labelKey: LocalizedStringKey {
        switch self {
        case .content: return "feedback_issue_content"
        case .audio: return "feedback_issue_audio"
        case .aiExplanation: return "feedback_issue_ai"
        case .bug: return "feedback_issue_bug"
        case .other: return "feedback_issue_other"
        }
    }
}

private extension LMCFeedbackSatisfaction {
    var sharedValue: FeedbackSatisfaction {
        switch self {
        case .verySatisfied: return FeedbackSatisfaction.verysatisfied
        case .satisfied: return FeedbackSatisfaction.satisfied
        case .needsWork: return FeedbackSatisfaction.dissatisfied
        }
    }
}

private extension LMCFeedbackAgeBand {
    var sharedValue: FeedbackChildAgeBand {
        switch self {
        case .fiveToSix: return FeedbackChildAgeBand.age5to6
        case .sevenToEight: return FeedbackChildAgeBand.age7to8
        case .preferNotToSay: return FeedbackChildAgeBand.prefernottosay
        }
    }
}

private extension LMCFeedbackIssueType {
    var sharedValue: FeedbackIssueType {
        switch self {
        case .content: return FeedbackIssueType.contenttoohard
        case .audio: return FeedbackIssueType.audioissue
        case .aiExplanation: return FeedbackIssueType.aiexplainissue
        case .bug: return FeedbackIssueType.bug
        case .other: return FeedbackIssueType.other
        }
    }
}

enum LMCFeedbackSaveState {
    case idle
    case saving
    case saved
    case failed
}

struct LMCSharedFeedbackRepository {
    private let service: FeedbackService
    private let buildSubmission = BuildFeedbackSubmissionUseCase()

    init(service: FeedbackService = IosFeedbackServiceKt.createPlatformFeedbackService()) {
        self.service = service
    }

    func save(_ draft: LMCFeedbackDraft) async throws {
        let submission = buildSubmission.invoke(
            satisfaction: draft.satisfaction.sharedValue,
            childAgeBand: draft.childAgeBand.sharedValue,
            issueType: draft.issueType.sharedValue,
            suggestion: draft.suggestion,
            parentContact: draft.parentContact
        )

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            service.submit(submission: submission) { _, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }
}

enum LMCBundleInfo {
    static var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }
}

enum LMCISO8601 {
    static func utcString(from date: Date) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.string(from: date)
    }
}

enum LMCColor {
    static let primary = Color(hex: 0xB84535)
    static let onPrimary = Color.white
    static let primaryContainer = Color(hex: 0xFFE0D6)
    static let onPrimaryContainer = Color(hex: 0x3D0E08)
    static let secondary = Color(hex: 0x126B68)
    static let onSecondary = Color.white
    static let secondaryContainer = Color(hex: 0xD9F1EE)
    static let onSecondaryContainer = Color(hex: 0x063432)
    static let tertiary = Color(hex: 0x8A6100)
    static let tertiaryContainer = Color(hex: 0xFFF4D8)
    static let background = Color(hex: 0xFFF8EC)
    static let surface = Color.white
    static let surfaceVariant = Color(hex: 0xF3F7F1)
    static let outline = Color(hex: 0x9AA7A0)
    static let outlineVariant = Color(hex: 0xD7DED8)
    static let textPrimary = Color(hex: 0x202523)
    static let textSecondary = Color(hex: 0x4F5E58)
    static let success = Color(hex: 0x3B7A3B)
    static let successContainer = Color(hex: 0xE1F3DC)
    static let error = Color(hex: 0xB3261E)
    static let errorContainer = Color(hex: 0xFADAD6)
    static let onErrorContainer = Color(hex: 0x410E0B)
    static let info = Color(hex: 0x2B6CA3)
    static let infoContainer = Color(hex: 0xDCEEFF)
}

enum LMCSpace {
    static let s1: CGFloat = 4
    static let s2: CGFloat = 8
    static let s3: CGFloat = 12
    static let s4: CGFloat = 16
    static let s5: CGFloat = 20
    static let s6: CGFloat = 24
    static let s8: CGFloat = 32
    static let minTouch: CGFloat = 48
    static let readingMaxWidth: CGFloat = 720
    static let gridMaxWidth: CGFloat = 960
}

private extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0
        )
    }
}

private struct TodayScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let openReading: (Story) -> Void
    let openVocabulary: (Story) -> Void
    let openQuiz: (Story) -> Void
    let openParent: () -> Void
    let openReview: () -> Void
    let openAbilityMap: () -> Void
    let openSettings: () -> Void

    var body: some View {
        LMCScreen {
            header

            switch viewModel.loadingState {
            case .idle, .loading:
                LMCLoadingView()
            case .failed:
                LMCErrorView()
            case .loaded:
                if let story = viewModel.todayStory {
                    VStack(alignment: .leading, spacing: LMCSpace.s6) {
                        StreakCard(summary: viewModel.streakSummary)
                        if let pending = viewModel.pendingReview {
                            Button(action: openReview) {
                                VStack(alignment: .leading, spacing: LMCSpace.s2) {
                                    Text("today_review_pending_title")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundStyle(LMCColor.textPrimary)
                                    Text(pending.isFromPreviousDay ? "today_review_pending_body" : "today_review_fresh_body")
                                        .font(.system(size: 15))
                                        .foregroundStyle(LMCColor.textSecondary)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(LMCSpace.s4)
                                .background(LMCColor.secondaryContainer)
                                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                        SectionTitle("today_title")
                        StoryHeroCard(
                            story: story,
                            progress: viewModel.progressValue(for: story),
                            progressLabelKey: viewModel.progressLabelKey(for: story),
                            isCompleted: viewModel.isCompleted(story),
                            openReading: { openReading(story) }
                        )

                        HStack(spacing: LMCSpace.s3) {
                            SummaryTile(
                                icon: "textformat.characters",
                                titleKey: "today_words",
                                value: "\(story.vocab.count)",
                                action: { openVocabulary(story) }
                            )
                            SummaryTile(
                                icon: "checklist",
                                titleKey: "today_quiz",
                                value: "\(story.questions.count)",
                                action: { openQuiz(story) }
                            )
                        }

                        if viewModel.reviewWordCount > 0 {
                            Text(LMCStrings.format("today_review_words", viewModel.reviewWordCount))
                                .font(.system(size: 15, weight: .bold))
                                .foregroundStyle(LMCColor.tertiary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(LMCSpace.s4)
                                .background(LMCColor.tertiaryContainer)
                                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                        }

                        AbilityMapTodayCard(
                            abilityMap: viewModel.abilityMap,
                            action: openAbilityMap
                        )

                        if let upNext = viewModel.upNextStory {
                            VStack(alignment: .leading, spacing: LMCSpace.s3) {
                                SectionTitle("today_up_next")
                                CompactStoryRow(
                                    story: upNext,
                                    progress: viewModel.progressValue(for: upNext),
                                    progressLabelKey: viewModel.progressLabelKey(for: upNext),
                                    action: { openReading(upNext) }
                                )
                            }
                        }
                    }
                } else {
                    LMCEmptyState(titleKey: "empty_stories_title", messageKey: "empty_stories_message")
                }
            }
        }
        .safeAreaInset(edge: .top) {
            Color.clear.frame(height: 0)
        }
    }

    private var header: some View {
        HStack(alignment: .top, spacing: LMCSpace.s3) {
            VStack(alignment: .leading, spacing: LMCSpace.s1) {
                Text("app_name")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Text("app_subtitle")
                    .font(.system(size: 16, weight: .regular))
                    .foregroundStyle(LMCColor.textSecondary)
            }
            Spacer()
            IconButton(systemName: "person.2.fill", labelKey: "nav_parent", action: openParent)
            IconButton(systemName: "gearshape.fill", labelKey: "nav_settings", action: openSettings)
        }
    }
}

private struct OnboardingFlow: View {
    @ObservedObject var viewModel: ReaderViewModel
    let initialLocaleIdentifier: String
    let complete: (LMCOnboardingAgeBand, String, Int, KotlinInt?) async -> Void
    let skip: () async -> Void

    @State private var selectedAgeBand: LMCOnboardingAgeBand = .age5To6
    @State private var selectedLanguage: LMCAppLocale = .english
    @State private var dailyGoal = 1
    @State private var showPlacementCheck = false

    var body: some View {
        if showPlacementCheck {
            PlacementCheckView(
                viewModel: viewModel,
                onComplete: { level in
                    await complete(selectedAgeBand, selectedLanguage.rawValue, dailyGoal, level)
                },
                onSkip: {
                    await complete(selectedAgeBand, selectedLanguage.rawValue, dailyGoal, nil)
                }
            )
        } else {
            onboardingForm
        }
    }

    private var onboardingForm: some View {
        LMCScreen(maxWidth: LMCSpace.readingMaxWidth) {
            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                Text("onboarding_title")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Text("onboarding_subtitle")
                    .font(.system(size: 16))
                    .foregroundStyle(LMCColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            SettingsSection(titleKey: "onboarding_age_title") {
                ForEach(LMCOnboardingAgeBand.allCases, id: \.rawValue) { ageBand in
                    SettingsChoiceRow(
                        titleKey: ageBand.titleKey,
                        isSelected: selectedAgeBand == ageBand,
                        action: { selectedAgeBand = ageBand }
                    )
                }
            }

            SettingsSection(titleKey: "onboarding_language_title") {
                ForEach(LMCAppLocale.allCases, id: \.rawValue) { locale in
                    SettingsChoiceRow(
                        titleKey: locale.labelKey,
                        isSelected: selectedLanguage == locale,
                        action: { selectedLanguage = locale }
                    )
                }
            }

            SettingsSection(titleKey: "onboarding_goal_title") {
                SettingsChoiceRow(
                    titleKey: "onboarding_goal_one_story",
                    isSelected: dailyGoal == 1,
                    action: { dailyGoal = 1 }
                )
                SettingsChoiceRow(
                    titleKey: "onboarding_goal_two_stories",
                    isSelected: dailyGoal == 2,
                    action: { dailyGoal = 2 }
                )
            }

            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                Label("onboarding_day_one_title", systemImage: "seal.fill")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(LMCColor.tertiary)
                Text("onboarding_day_one_body")
                    .font(.system(size: 16))
                    .foregroundStyle(LMCColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(LMCSpace.s4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(LMCColor.tertiaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            SettingsSection(titleKey: "placement_section_title") {
                VStack(alignment: .leading, spacing: LMCSpace.s2) {
                    Text("placement_intro_body")
                        .font(.system(size: 15))
                        .foregroundStyle(LMCColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                    Button("placement_start") {
                        showPlacementCheck = true
                    }
                    .buttonStyle(LMCSecondaryButtonStyle())
                    .disabled(viewModel.stories.isEmpty)
                }
                .padding(LMCSpace.s4)
            }

            Text("onboarding_privacy_note")
                .font(.system(size: 15))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: LMCSpace.s3) {
                Button("action_skip") {
                    Task { await skip() }
                }
                .buttonStyle(LMCSecondaryButtonStyle())

                Button("onboarding_get_started") {
                    Task {
                        await complete(selectedAgeBand, selectedLanguage.rawValue, dailyGoal, nil)
                    }
                }
                .buttonStyle(LMCPrimaryButtonStyle())
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
        .onAppear {
            selectedLanguage = LMCAppLocale(rawValue: initialLocaleIdentifier) ?? .english
        }
    }
}

/// Optional, child-friendly placement check shown during onboarding and re-check. Big tappable
/// meaning cards, encouraging non-punitive tone. Result is a local reading-level preference only.
private struct PlacementCheckView: View {
    @ObservedObject var viewModel: ReaderViewModel
    let onComplete: (KotlinInt?) async -> Void
    let onSkip: () async -> Void

    @State private var items: [AssessmentItem] = []
    @State private var currentIndex = 0
    @State private var answers: [String: String] = [:]
    @State private var resolvedLevel: Int?

    var body: some View {
        Group {
            if items.isEmpty {
                emptyState
            } else if let level = resolvedLevel {
                resultState(level: level)
            } else {
                questionState
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
        .onAppear {
            if items.isEmpty {
                items = viewModel.assessmentItems()
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: LMCSpace.s4) {
            Text("placement_no_items")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
                .multilineTextAlignment(.center)
            Button("placement_result_continue") {
                Task { await onSkip() }
            }
            .buttonStyle(LMCPrimaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func resultState(level: Int) -> some View {
        VStack(spacing: LMCSpace.s4) {
            Spacer()
            Text("placement_result_title")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
            Text(LMCStrings.format("placement_result_level", level))
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(LMCColor.tertiary)
            Text("placement_result_privacy")
                .font(.system(size: 15))
                .foregroundStyle(LMCColor.textSecondary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
            Spacer()
            Button("placement_result_continue") {
                Task { await onComplete(KotlinInt(int: Int32(level))) }
            }
            .buttonStyle(LMCPrimaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var questionState: some View {
        let item = items[currentIndex]
        return LMCScreen(maxWidth: LMCSpace.readingMaxWidth) {
            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                Text("placement_intro_title")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Text(LMCStrings.format("placement_progress", currentIndex + 1, items.count))
                    .font(.system(size: 16))
                    .foregroundStyle(LMCColor.textSecondary)
            }

            VStack(spacing: LMCSpace.s2) {
                Text(item.pinyin)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Text(item.word)
                    .font(.system(size: 40, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
            }
            .frame(maxWidth: .infinity)
            .padding(LMCSpace.s4)
            .background(LMCColor.tertiaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

            Text("placement_question")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)

            ForEach(item.options, id: \.self) { option in
                Button {
                    answers[item.id] = option
                    if currentIndex < items.count - 1 {
                        currentIndex += 1
                    } else {
                        Task {
                            resolvedLevel = await viewModel.applyAssessment(
                                items: items,
                                answersByItemId: answers
                            )
                        }
                    }
                } label: {
                    HStack {
                        Text(option)
                            .font(.system(size: 17))
                            .foregroundStyle(LMCColor.textPrimary)
                        Spacer()
                    }
                    .padding(LMCSpace.s4)
                    .frame(maxWidth: .infinity)
                    .background(LMCColor.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
            }

            Button("placement_skip") {
                Task { await onSkip() }
            }
            .buttonStyle(LMCSecondaryButtonStyle())
        }
    }
}

private struct StreakCard: View {
    let summary: StreakSummary?

    var body: some View {
        let dayCount = Int(summary?.currentStreakDays ?? 0)
        let todayCount = Int(summary?.todayCompletedStories ?? 0)
        let goal = max(1, Int(summary?.dailyGoalStories ?? 1))
        let isComplete = summary?.todayGoalMet ?? false

        HStack(spacing: LMCSpace.s3) {
            Image(systemName: isComplete ? "checkmark.seal.fill" : "calendar.badge.checkmark")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(isComplete ? LMCColor.success : LMCColor.tertiary)
                .frame(width: 48, height: 48)
                .background(LMCColor.surface)
                .clipShape(Circle())
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: LMCSpace.s1) {
                Text(LMCStrings.format("streak_day_count", max(1, dayCount)))
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Text(LMCStrings.format("streak_progress_format", todayCount, goal))
                    .font(.system(size: 15))
                    .foregroundStyle(LMCColor.textSecondary)
            }

            Spacer()

            Text(LocalizedStringKey(isComplete ? "streak_goal_complete" : "streak_goal_continue"))
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(isComplete ? LMCColor.success : LMCColor.tertiary)
        }
        .padding(LMCSpace.s4)
        .background(isComplete ? LMCColor.successContainer : LMCColor.tertiaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct WordBookScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let openReview: () -> Void
    let openToday: () -> Void
    let openSettings: () -> Void
    @State private var selectedFilter: LMCWordBookFilter = .all
    @State private var didTrackOpen = false

    private var items: [WordBookItem] {
        let allItems = viewModel.wordBookSummary?.items ?? []
        switch selectedFilter {
        case .all:
            return allItems
        case .due:
            return allItems.filter { $0.due }
        case .learning:
            return allItems.filter { !$0.due && $0.reps < 2 }
        case .known:
            return allItems.filter { $0.reps >= 2 }
        }
    }

    var body: some View {
        LMCScreen(maxWidth: LMCSpace.gridMaxWidth) {
            HStack(spacing: LMCSpace.s3) {
                SectionTitle("word_book_title")
                Spacer()
                IconButton(systemName: "gearshape.fill", labelKey: "nav_settings", action: openSettings)
            }

            HStack(spacing: LMCSpace.s3) {
                SummaryTile(
                    icon: "textformat.characters",
                    titleKey: "word_book_learned_count",
                    value: "\(viewModel.wordBookSummary?.totalWords ?? 0)",
                    action: { selectedFilter = .all }
                )
                SummaryTile(
                    icon: "calendar.badge.checkmark",
                    titleKey: "word_book_due_count",
                    value: "\(viewModel.wordBookSummary?.dueCount ?? 0)",
                    action: { selectedFilter = .due }
                )
            }

            if (viewModel.wordBookSummary?.dueCount ?? 0) > 0 {
                Button("action_start_review", action: openReview)
                    .buttonStyle(LMCPrimaryButtonStyle())
            } else if (viewModel.wordBookSummary?.totalWords ?? 0) > 0 {
                LMCEmptyState(titleKey: "word_book_no_due_title", messageKey: "word_book_no_due_body")
                Button("action_start_reading", action: openToday)
                    .buttonStyle(LMCSecondaryButtonStyle())
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: LMCSpace.s2) {
                    ForEach(LMCWordBookFilter.allCases, id: \.rawValue) { filter in
                        FilterChip(
                            titleKey: filter.titleKey,
                            isSelected: selectedFilter == filter,
                            action: { selectedFilter = filter }
                        )
                    }
                }
            }

            if (viewModel.wordBookSummary?.totalWords ?? 0) == 0 {
                LMCEmptyState(titleKey: "word_book_empty_title", messageKey: "word_book_empty_body")
                Button("action_start_reading", action: openToday)
                    .buttonStyle(LMCPrimaryButtonStyle())
            } else if items.isEmpty {
                LMCEmptyState(titleKey: "word_book_no_due_title", messageKey: "word_book_no_due_body")
            } else {
                LazyVStack(spacing: LMCSpace.s3) {
                    ForEach(items, id: \.word) { item in
                        WordBookRow(item: item, sourceTitle: sourceTitle(for: item)) {
                            viewModel.speakCurrent([item.word, item.example ?? ""].filter { !$0.isEmpty }.joined(separator: " "))
                        }
                    }
                }
            }
        }
        .onAppear {
            guard !didTrackOpen else { return }
            didTrackOpen = true
            viewModel.trackWordBookOpen()
        }
    }

    private func sourceTitle(for item: WordBookItem) -> String? {
        guard let storyId = item.sourceStoryIds.first as? String else { return nil }
        return viewModel.story(id: storyId)?.titleZh
    }
}

private struct WordReviewScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let close: () -> Void

    @State private var items: [WordBookItem] = []
    @State private var wordIndex = 0
    @State private var answerRevealed = false
    @State private var knownCount = 0
    @State private var needsPracticeCount = 0
    @State private var didComplete = false

    private var currentItem: WordBookItem? {
        guard items.indices.contains(wordIndex) else { return nil }
        return items[wordIndex]
    }

    var body: some View {
        VStack(spacing: 0) {
            LMCFlowTopBar(
                titleKey: "word_review_title",
                trailingText: items.isEmpty ? nil : "\(min(wordIndex + 1, items.count)) / \(items.count)",
                close: close
            )

            LMCProgressBar(value: didComplete ? 1 : progress)
                .padding(.horizontal, LMCSpace.s4)

            if didComplete {
                reviewComplete
            } else if let currentItem {
                reviewCard(currentItem)
            } else {
                VStack(spacing: LMCSpace.s4) {
                    LMCEmptyState(titleKey: "word_review_no_due_title", messageKey: "word_review_no_due_body")
                    Button("action_done", action: close)
                        .buttonStyle(LMCPrimaryButtonStyle())
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
        .onAppear {
            if items.isEmpty {
                items = viewModel.wordReviewItemsDueToday()
            }
        }
    }

    private func reviewCard(_ item: WordBookItem) -> some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: LMCSpace.s5) {
                    VocabularyCard(word: item.asVocab) {
                        viewModel.speakCurrent([item.word, item.example ?? ""].filter { !$0.isEmpty }.joined(separator: " "))
                    }
                    .blur(radius: answerRevealed ? 0 : 0)

                    if !answerRevealed {
                        Text("word_review_prompt")
                            .font(.system(size: 16))
                            .foregroundStyle(LMCColor.textSecondary)
                    }
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth)
                .frame(maxWidth: .infinity)
            }

            LMCBottomActionBar {
                if answerRevealed {
                    Button("action_still_learning") {
                        Task { await submit(item, assessment: .needspractice, rating: "needs_practice") }
                    }
                    .buttonStyle(LMCSecondaryButtonStyle())

                    Button("action_got_it") {
                        Task { await submit(item, assessment: .known, rating: "known") }
                    }
                    .buttonStyle(LMCPrimaryButtonStyle())
                } else {
                    Spacer()
                    Button("action_show_answer") {
                        answerRevealed = true
                    }
                    .buttonStyle(LMCPrimaryButtonStyle())
                }
            }
        }
    }

    private var reviewComplete: some View {
        VStack(spacing: LMCSpace.s5) {
            Spacer()
            Image(systemName: "checkmark.seal.fill")
                .font(.system(size: 64, weight: .bold))
                .foregroundStyle(LMCColor.success)
                .accessibilityHidden(true)
            Text("word_review_complete_title")
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
            Text(LMCStrings.format("word_review_complete_body", knownCount + needsPracticeCount))
                .font(.system(size: 18))
                .foregroundStyle(LMCColor.textSecondary)
            Button("action_done", action: close)
                .buttonStyle(LMCPrimaryButtonStyle())
            Spacer()
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var progress: Double {
        guard !items.isEmpty else { return 1 }
        return Double(wordIndex) / Double(items.count)
    }

    private func submit(_ item: WordBookItem, assessment: VocabReviewAssessment, rating: String) async {
        await viewModel.recordWordReview(item: item, assessment: assessment)
        viewModel.trackWordReviewAnswer(item: item, rating: rating, reviewIndex: wordIndex + 1)
        if assessment == .known {
            knownCount += 1
        } else {
            needsPracticeCount += 1
        }
        if wordIndex >= items.count - 1 {
            didComplete = true
            viewModel.trackWordReviewComplete(
                sessionSize: items.count,
                reviewedCount: knownCount + needsPracticeCount,
                knownCount: knownCount,
                needsPracticeCount: needsPracticeCount
            )
        } else {
            wordIndex += 1
            answerRevealed = false
        }
    }
}

private struct CompletionMilestoneBanner: View {
    let summary: StreakSummary

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            Label(titleText, systemImage: "seal.fill")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(LMCColor.success)
            Text(LMCStrings.format("completion_milestone_goal_body", Int(summary.todayCompletedStories), Int(summary.dailyGoalStories)))
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textPrimary)
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.successContainer)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var titleText: LocalizedStringKey {
        if let milestone = summary.newMilestoneDays {
            return LocalizedStringKey(LMCStrings.format("completion_milestone_streak_title", Int(milestone)))
        }
        return "completion_milestone_goal_title"
    }
}

private struct CompletionCelebrationLayer: View {
    let animationToken: Int
    let isMilestone: Bool
    let enabled: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var progress = 0.0

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                ForEach(CompletionParticle.particles) { particle in
                    CompletionParticleView(particle: particle, isMilestone: isMilestone)
                        .position(position(for: particle, in: proxy.size))
                        .opacity(opacity)
                        .scaleEffect(reduceMotion ? 1 : 0.85 + progress * 0.25)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .onAppear(perform: runAnimation)
        .onChange(of: animationToken) { _ in runAnimation() }
    }

    private var opacity: Double {
        if reduceMotion { return enabled ? 0.18 : 0 }
        return enabled ? max(0.22, 1.0 - progress * 0.58) : 0
    }

    private func position(for particle: CompletionParticle, in size: CGSize) -> CGPoint {
        let target = CGPoint(x: size.width * particle.x, y: size.height * particle.y)
        if reduceMotion {
            return target
        }
        let origin = CGPoint(x: size.width * 0.5, y: min(120, size.height * 0.18))
        let lift = sin(progress * .pi) * particle.lift
        return CGPoint(
            x: origin.x + (target.x - origin.x) * progress,
            y: origin.y + (target.y - origin.y) * progress - lift
        )
    }

    private func runAnimation() {
        guard enabled else {
            progress = 0
            return
        }
        if reduceMotion {
            progress = 1
            return
        }
        progress = 0
        withAnimation(.easeOut(duration: 0.72)) {
            progress = 1
        }
    }
}

private struct CompletionStampBadge: View {
    let summary: StreakSummary?
    let completionJustRecorded: Bool
    let animationToken: Int
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isVisible = false

    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            Circle()
                .fill(LMCColor.successContainer)
                .frame(width: 96, height: 96)
                .overlay {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 54, weight: .bold))
                        .foregroundStyle(LMCColor.success)
                        .accessibilityHidden(true)
                }
                .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 1)

            Label(stampText, systemImage: "seal.fill")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(LMCColor.primary)
                .lineLimit(1)
                .padding(.horizontal, LMCSpace.s3)
                .frame(minHeight: 36)
                .background(LMCColor.primaryContainer)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .rotationEffect(.degrees(reduceMotion ? 0 : -6))
                .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 1)
        }
        .frame(width: 152, height: 118)
        .scaleEffect(isVisible ? 1 : 0.9)
        .opacity(isVisible ? 1 : 0.2)
        .onAppear(perform: runAnimation)
        .onChange(of: animationToken) { _ in runAnimation() }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(stampText))
    }

    private var stampText: String {
        if let milestone = summary?.newMilestoneDays {
            return LMCStrings.format("completion_stamp_milestone", Int(milestone))
        }
        if didAdvanceStreakOnCompletion {
            return LMCStrings.localized("completion_stamp_streak_plus_one")
        }
        if completionJustRecorded {
            return LMCStrings.localized("completion_stamp_story_plus_one")
        }
        return LMCStrings.localized("completion_stamp_story_done")
    }

    private var didAdvanceStreakOnCompletion: Bool {
        guard completionJustRecorded, let summary = summary else { return false }
        return summary.todayGoalMet && summary.todayCompletedStories == summary.dailyGoalStories
    }

    private func runAnimation() {
        if reduceMotion {
            isVisible = true
            return
        }
        isVisible = false
        withAnimation(.spring(response: 0.42, dampingFraction: 0.72)) {
            isVisible = true
        }
    }
}

private struct CompletionParticleView: View {
    let particle: CompletionParticle
    let isMilestone: Bool

    var body: some View {
        Group {
            if particle.kind == .star {
                Image(systemName: isMilestone ? "sparkles" : "star.fill")
                    .font(.system(size: particle.size, weight: .bold))
                    .foregroundStyle(particle.color)
            } else {
                RoundedRectangle(cornerRadius: 3, style: .continuous)
                    .fill(particle.color)
                    .frame(width: particle.size * 1.5, height: particle.size * 0.72)
                    .rotationEffect(.degrees(particle.rotation))
            }
        }
    }
}

private struct CompletionParticle: Identifiable {
    enum Kind {
        case star
        case confetti
    }

    let id: Int
    let x: CGFloat
    let y: CGFloat
    let size: CGFloat
    let lift: CGFloat
    let rotation: Double
    let color: Color
    let kind: Kind

    static let particles: [CompletionParticle] = [
        CompletionParticle(id: 0, x: 0.14, y: 0.16, size: 16, lift: 34, rotation: -12, color: LMCColor.primary, kind: .star),
        CompletionParticle(id: 1, x: 0.30, y: 0.10, size: 14, lift: 28, rotation: 18, color: LMCColor.secondary, kind: .confetti),
        CompletionParticle(id: 2, x: 0.44, y: 0.18, size: 12, lift: 30, rotation: -24, color: LMCColor.tertiary, kind: .star),
        CompletionParticle(id: 3, x: 0.62, y: 0.12, size: 15, lift: 34, rotation: 10, color: LMCColor.success, kind: .confetti),
        CompletionParticle(id: 4, x: 0.78, y: 0.22, size: 15, lift: 28, rotation: 28, color: LMCColor.primary, kind: .star),
        CompletionParticle(id: 5, x: 0.88, y: 0.34, size: 13, lift: 24, rotation: -18, color: LMCColor.secondary, kind: .confetti),
        CompletionParticle(id: 6, x: 0.12, y: 0.42, size: 12, lift: 20, rotation: 32, color: LMCColor.tertiary, kind: .confetti),
        CompletionParticle(id: 7, x: 0.28, y: 0.52, size: 15, lift: 24, rotation: -8, color: LMCColor.success, kind: .star),
        CompletionParticle(id: 8, x: 0.74, y: 0.50, size: 15, lift: 22, rotation: 20, color: LMCColor.primary, kind: .confetti),
        CompletionParticle(id: 9, x: 0.90, y: 0.62, size: 12, lift: 18, rotation: -30, color: LMCColor.secondary, kind: .star),
    ]
}

private struct LibraryScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let openReading: (Story) -> Void
    let openSettings: () -> Void
    @State private var selectedLevel: Int?

    private var filteredStories: [Story] {
        viewModel.filteredStories(level: selectedLevel)
    }

    private var availableLevels: [Int] {
        viewModel.availableLibraryLevels()
    }

    var body: some View {
        GeometryReader { proxy in
            LMCScreen(maxWidth: LMCSpace.gridMaxWidth) {
                VStack(alignment: .leading, spacing: LMCSpace.s4) {
                    HStack(spacing: LMCSpace.s3) {
                        SectionTitle("library_title")
                        Spacer()
                        IconButton(systemName: "gearshape.fill", labelKey: "nav_settings", action: openSettings)
                    }

                    filterChips

                    if viewModel.loadingState == .failed {
                        LMCLibraryStateCard(
                            titleKey: "library_error_title",
                            messageKey: "library_error_body",
                            actionKey: "common_retry",
                            action: { Task { await viewModel.load() } }
                        )
                    } else if viewModel.loadingState == .loading || viewModel.loadingState == .idle {
                        LMCLibraryStateCard(
                            titleKey: "library_loading_title",
                            messageKey: "library_loading_body",
                            actionKey: "common_retry",
                            showsProgress: true,
                            action: { Task { await viewModel.load() } }
                        )
                    } else if viewModel.stories.isEmpty {
                        LMCLibraryStateCard(
                            titleKey: "empty_library_title",
                            messageKey: "empty_library_body",
                            actionKey: "common_retry",
                            action: { Task { await viewModel.load() } }
                        )
                    } else if filteredStories.isEmpty {
                        LMCLibraryStateCard(
                            titleKey: "library_no_results_title",
                            messageKey: "library_no_results_body",
                            actionKey: "action_clear_filter",
                            action: { selectedLevel = nil }
                        )
                    } else {
                        LazyVGrid(columns: columns(for: proxy.size.width), spacing: LMCSpace.s4) {
                            ForEach(Array(filteredStories.enumerated()), id: \.element.id) { _, story in
                                StoryListCard(
                                    story: story,
                                    sequenceNumber: sequenceNumber(for: story),
                                    progress: viewModel.progressValue(for: story),
                                    progressLabelKey: viewModel.progressLabelKey(for: story),
                                    isCompleted: viewModel.isCompleted(story),
                                    action: { openReading(story) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private var filterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: LMCSpace.s2) {
                FilterChip(titleKey: "filter_all", isSelected: selectedLevel == nil) {
                    selectedLevel = nil
                }
                ForEach(availableLevels, id: \.self) { level in
                    FilterChip(
                        title: LMCStrings.format("filter_level", level),
                        isSelected: selectedLevel == level,
                        action: { selectedLevel = level }
                    )
                }
            }
            .padding(.vertical, LMCSpace.s1)
        }
    }

    private func sequenceNumber(for story: Story) -> Int {
        (viewModel.stories.firstIndex { $0.id == story.id } ?? 0) + 1
    }

    private func columns(for width: CGFloat) -> [GridItem] {
        if width >= 760 {
            return [
                GridItem(.flexible(), spacing: LMCSpace.s4),
                GridItem(.flexible(), spacing: LMCSpace.s4),
            ]
        }
        return [GridItem(.flexible())]
    }
}

private struct ReadingScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let story: Story
    @Binding var showPinyin: Bool
    @Binding var readingSize: String
    @Binding var aiBackendBaseURL: String
    let close: () -> Void
    let openVocabulary: () -> Void
    @State private var paragraphIndex = 0
    @State private var askState: LMCAiAskState = .idle
    @State private var autoFollowReading = true
    @State private var showSettingsSheet = false
    @State private var wordLookup: LMCWordLookup?
    @State private var wordLookupAskState: LMCAiAskState = .idle
    @AppStorage("reading_coachmark_tts_row_dismissed") private var didDismissReadAlongCoachmark = false
    @State private var showMicrophonePermissionAlert = false

    private var activeSentenceLocation: LMCSentenceLocation? {
        guard let location = viewModel.readingAudioLocation, location.storyId == story.id else { return nil }
        return location
    }

    private var canGoPreviousSentence: Bool {
        guard let location = activeSentenceLocation else { return false }
        return story.lmcPreviousSentenceLocation(before: location) != nil
    }

    private var canGoNextSentence: Bool {
        guard let location = activeSentenceLocation else { return false }
        return story.lmcNextSentenceLocation(after: location) != nil
    }

    private var activeSentenceProgress: (index: Int, total: Int, source: LMCAudioSource, isPaused: Bool)? {
        guard let location = activeSentenceLocation else { return nil }
        let paragraphSentenceCount: Int
        if story.paragraphs.indices.contains(location.paragraphIndex) {
            paragraphSentenceCount = story.paragraphs[location.paragraphIndex].lmcSentenceSegments.count
        } else {
            paragraphSentenceCount = 0
        }
        let source: LMCAudioSource? = {
            switch viewModel.readingAudioStatus {
            case .playing(_, let source), .paused(_, let source):
                return source
            default:
                return nil
            }
        }()
        guard let source else { return nil }
        return (
            index: min(location.sentenceIndex + 1, max(paragraphSentenceCount, 1)),
            total: max(paragraphSentenceCount, 1),
            source: source,
            isPaused: viewModel.isReadingAudioPaused
        )
    }

    private var readingPlayerStatusText: String {
        guard let activeSentenceProgress else { return "" }
        let sourceLabel = activeSentenceProgress.source == .recorded
            ? LMCStrings.localized("reading_audio_source_recorded")
            : LMCStrings.localized("reading_audio_source_tts")
        let stateLabel = activeSentenceProgress.isPaused
            ? LMCStrings.localized("reading_audio_paused")
            : LMCStrings.localized("reading_audio_playing")
        return LMCStrings.format(
            "reading_sentence_progress",
            activeSentenceProgress.index,
            activeSentenceProgress.total,
            sourceLabel,
            stateLabel
        )
    }

    private var readingState: ReadingSessionState {
        viewModel.readingState(for: story, paragraphIndex: paragraphIndex)
    }

    private var currentParagraph: Paragraph {
        readingState.currentParagraph ?? story.paragraphs[max(0, min(paragraphIndex, story.paragraphs.count - 1))]
    }

    private var size: LMCReadingSize {
        LMCReadingSize.value(from: readingSize)
    }

    var body: some View {
        VStack(spacing: 0) {
            ReadingTopBar(
                story: story,
                countText: "\(Int(readingState.paragraphIndex) + 1) / \(Int(readingState.paragraphCount))",
                openSettings: {
                    showSettingsSheet = true
                },
                close: {
                    viewModel.stopSentencePlayback()
                    close()
                }
            )

            ReadingProgressHairline(
                value: progress,
                accessibilityValue: LMCStrings.format(
                    "reading_progress_accessibility",
                    Int(readingState.paragraphIndex) + 1,
                    Int(readingState.paragraphCount)
                )
            )

            ScrollViewReader { proxy in
                ScrollView {
                    VStack(alignment: .leading, spacing: LMCSpace.s5) {
                        VStack(alignment: .leading, spacing: LMCSpace.s5) {
                            ForEach(Array(story.paragraphs.enumerated()), id: \.offset) { index, paragraph in
                                ReadingParagraphView(
                                    storyId: story.id,
                                    paragraphIndex: index,
                                    paragraph: paragraph,
                                    size: size,
                                    showPinyin: showPinyin,
                                    isCurrentParagraph: index == paragraphIndex,
                                    activeSentenceIndex: activeSentenceLocation?.paragraphIndex == index ? activeSentenceLocation?.sentenceIndex : nil,
                                    activeCharIndex: activeSentenceLocation?.paragraphIndex == index ? viewModel.activeCharIndex : nil,
                                    playSentence: { sentenceIndex in
                                        autoFollowReading = true
                                        viewModel.playTappedSentence(
                                            story: story,
                                            paragraphIndex: index,
                                            sentenceIndex: sentenceIndex
                                        )
                                    },
                                    playSentenceOnly: { sentenceIndex in
                                        autoFollowReading = true
                                        viewModel.playSentenceOnly(
                                            story: story,
                                            paragraphIndex: index,
                                            sentenceIndex: sentenceIndex
                                        )
                                    },
                                isRecordingSentence: { sentenceIndex in
                                    viewModel.isRecordingSentence(
                                        storyId: story.id,
                                        paragraphIndex: index,
                                        sentenceIndex: sentenceIndex
                                    )
                                },
                                    startSentenceRecording: { sentenceIndex in
                                        Task {
                                            let result = await viewModel.startSentenceRecording(
                                                story: story,
                                                paragraphIndex: index,
                                                sentenceIndex: sentenceIndex
                                            )
                                            if result == .permissionDenied {
                                                showMicrophonePermissionAlert = true
                                            }
                                        }
                                    },
                                    stopSentenceRecording: { _ in
                                        viewModel.stopSentenceRecording()
                                    },
                                    onWordTap: { token in
                                        wordLookupAskState = .idle
                                        wordLookup = LMCWordLookup(result: viewModel.lookupWord(story: story, token: token))
                                    }
                                )
                                .id(paragraphScrollId(index))

                                if index == paragraphIndex {
                                    askPanel
                                }
                            }
                        }
                        .padding(LMCSpace.s4)
                        .background(LMCColor.surfaceVariant)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .frame(maxWidth: LMCSpace.readingMaxWidth, alignment: .leading)
                    }
                    .padding(LMCSpace.s4)
                    .frame(maxWidth: LMCSpace.readingMaxWidth + LMCSpace.s8)
                    .frame(maxWidth: .infinity)
                }
                .simultaneousGesture(
                    DragGesture(minimumDistance: 8).onChanged { _ in
                        if viewModel.isReadingAudioActive {
                            autoFollowReading = false
                        }
                    }
                )
                .onChange(of: viewModel.readingAudioLocation) { location in
                    guard let location, location.storyId == story.id else { return }
                    paragraphIndex = location.paragraphIndex
                    guard autoFollowReading else { return }
                    scrollToLocation(location, in: proxy)
                }
                .onChange(of: showPinyin) { _ in
                    recenterActiveSentenceIfNeeded(in: proxy)
                }
                .onChange(of: readingSize) { _ in
                    recenterActiveSentenceIfNeeded(in: proxy)
                }
                .safeAreaInset(edge: .bottom) {
                    VStack(spacing: LMCSpace.s2) {
                        if !didDismissReadAlongCoachmark {
                            ReadingAudioCoachmark(dismiss: {
                                didDismissReadAlongCoachmark = true
                            })
                            .padding(.horizontal, LMCSpace.s4)
                        }

                        ReadingBottomDock(
                            isAudioActive: viewModel.isReadingAudioActive,
                            autoFollowReading: autoFollowReading,
                            location: activeSentenceLocation,
                            canGoPreviousSentence: canGoPreviousSentence,
                            canGoNextSentence: canGoNextSentence,
                            playerStatusText: readingPlayerStatusText,
                            isReadingAudioPaused: viewModel.isReadingAudioPaused,
                            readingState: readingState,
                            canGoPreviousParagraph: readingState.canGoPrevious,
                            backToReadingPlace: {
                                guard let location = activeSentenceLocation else { return }
                                autoFollowReading = true
                                paragraphIndex = location.paragraphIndex
                                scrollToLocation(location, in: proxy)
                            },
                            previousSentence: { viewModel.previousSentencePlayback(story: story) },
                            togglePlayback: {
                                if viewModel.isReadingAudioPaused {
                                    viewModel.resumeSentencePlayback(story: story)
                                } else {
                                    viewModel.pauseSentencePlayback()
                                }
                            },
                            repeatSentence: { viewModel.repeatCurrentSentence(story: story) },
                            nextSentence: { viewModel.advanceSentencePlayback(story: story) },
                            stopAudio: { viewModel.stopSentencePlayback() },
                            previousParagraph: {
                                let targetIndex = Int(viewModel.previousReadingState(story, state: readingState).paragraphIndex)
                                paragraphIndex = targetIndex
                                scrollToParagraph(targetIndex, in: proxy)
                            },
                            nextParagraph: {
                                let transition = viewModel.nextReadingTransition(story, state: readingState)
                                let targetIndex = Int(transition.state.paragraphIndex)
                                paragraphIndex = targetIndex
                                scrollToParagraph(targetIndex, in: proxy)
                                if transition.shouldOpenVocabulary {
                                    viewModel.stopSentencePlayback()
                                    openVocabulary()
                                }
                            },
                            readParagraph: { startPlaybackAtVisiblePosition() }
                        )
                    }
                    .padding(.horizontal, LMCSpace.s4)
                    .padding(.bottom, LMCSpace.s2)
                }
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
        .alert("reading_recording_permission_required_title", isPresented: $showMicrophonePermissionAlert) {
            Button("action_open_settings") {
                if let settingsURL = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(settingsURL)
                }
            }
            Button("action_ok", role: .cancel) {
                showMicrophonePermissionAlert = false
            }
        } message: {
            Text("reading_recording_permission_required_message")
        }
        .sheet(isPresented: $showSettingsSheet) {
            ReadingSettingsSheet(
                showPinyin: $showPinyin,
                readingSize: $readingSize,
                readingModeBinding: readingModeBinding,
                autoContinueBinding: autoContinueBinding,
                playbackSpeed: viewModel.playbackSpeed,
                setPlaybackSpeed: viewModel.setPlaybackSpeed
            )
            .presentationDetents([.medium])
        }
        .sheet(item: $wordLookup) { lookup in
            WordLookupSheet(
                lookup: lookup,
                askState: wordLookupAskState,
                explainWithAi: {
                    guard let token = lookup.needsAi?.token else { return }
                    wordLookupAskState = .loading
                    Task {
                        wordLookupAskState = await viewModel.explainWord(
                            story: story,
                            token: token,
                            baseURL: aiBackendBaseURL
                        )
                    }
                },
                dismiss: {
                    wordLookup = nil
                    wordLookupAskState = .idle
                }
            )
            .presentationDetents([.medium])
        }
        .task(id: story.id) {
            await viewModel.prepareReadingAudio(for: story)
            paragraphIndex = await viewModel.savedReadingParagraphIndex(for: story)
        }
        .onChange(of: showPinyin) { enabled in
            viewModel.trackPinyinToggle(story, paragraphIndex: paragraphIndex, enabled: enabled)
        }
        .onChange(of: paragraphIndex) { _ in
            askState = .idle
            viewModel.saveReadingParagraphIndex(paragraphIndex, for: story)
        }
        .onDisappear {
            viewModel.stopSentencePlayback()
        }
    }

    private var progress: Double {
        readingState.progressFraction
    }

    private var readingModeBinding: Binding<LMCReadingPlaybackMode> {
        Binding(
            get: { viewModel.readingMode },
            set: { mode in viewModel.setReadingMode(mode) }
        )
    }

    private var autoContinueBinding: Binding<Bool> {
        Binding(
            get: { viewModel.autoContinueEnabled },
            set: { enabled in viewModel.setAutoContinue(enabled) }
        )
    }

    private func startPlaybackAtVisiblePosition() {
        autoFollowReading = true
        let retainedLocation = activeSentenceLocation?.paragraphIndex == paragraphIndex ? activeSentenceLocation : nil
        viewModel.startSentencePlayback(
            story: story,
            paragraphIndex: retainedLocation?.paragraphIndex ?? paragraphIndex,
            sentenceIndex: retainedLocation?.sentenceIndex ?? 0
        )
    }

    private func scrollToLocation(_ location: LMCSentenceLocation, in proxy: ScrollViewProxy) {
        withAnimation(.easeInOut(duration: 0.25)) {
            proxy.scrollTo(location.scrollId, anchor: .center)
        }
    }

    private func paragraphScrollId(_ index: Int) -> String {
        "paragraph-\(index)"
    }

    private func scrollToParagraph(_ index: Int, in proxy: ScrollViewProxy) {
        withAnimation(.easeInOut(duration: 0.25)) {
            proxy.scrollTo(paragraphScrollId(index), anchor: .top)
        }
    }

    private func recenterActiveSentenceIfNeeded(in proxy: ScrollViewProxy) {
        guard autoFollowReading, viewModel.isReadingAudioActive, let location = activeSentenceLocation else { return }
        scrollToLocation(location, in: proxy)
    }

    private var askPanel: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            Button {
                askCurrentParagraph()
            } label: {
                Label("reading_ask", systemImage: "sparkles")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(LMCSecondaryButtonStyle())
            .disabled(askState == .loading || currentParagraph.text.isEmpty)

            Text("reading_ai_boundary")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            switch askState {
            case .idle:
                EmptyView()
            case .loading:
                HStack(spacing: LMCSpace.s2) {
                    ProgressView()
                        .tint(LMCColor.primary)
                    Text("reading_ai_loading")
                        .font(.system(size: 16))
                        .foregroundStyle(LMCColor.textSecondary)
                }
                .frame(minHeight: LMCSpace.minTouch)
            case .answered(let answer):
                VStack(alignment: .leading, spacing: LMCSpace.s2) {
                    Label("reading_ai_answer", systemImage: "lightbulb.fill")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(LMCColor.info)
                    Text(answer)
                        .font(.system(size: 18))
                        .foregroundStyle(LMCColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(LMCColor.infoContainer)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            case .failed:
                Text("reading_ai_error")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(LMCColor.error)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(LMCSpace.s3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(LMCColor.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
        .frame(maxWidth: LMCSpace.readingMaxWidth, alignment: .leading)
    }

    private func askCurrentParagraph() {
        askState = .loading
        let selectedParagraph = currentParagraph.text
        let selectedParagraphIndex = paragraphIndex
        Task {
            askState = await viewModel.askAboutParagraph(
                story: story,
                paragraphIndex: selectedParagraphIndex,
                selectedText: selectedParagraph,
                baseURL: aiBackendBaseURL
            )
        }
    }
}

// Tap-word lookup sheet: trusted curated entry, or a single controlled "Explain with AI"
// action whose answer is clearly badged as AI. No free-text input, no open chat (§7).
private struct WordLookupSheet: View {
    let lookup: LMCWordLookup
    let askState: LMCAiAskState
    let explainWithAi: () -> Void
    let dismiss: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: LMCSpace.s3) {
                if let curated = lookup.curated {
                    Text(curated.word)
                        .font(.system(size: 34, weight: .bold))
                        .foregroundStyle(LMCColor.textPrimary)
                    if !curated.pinyin.isEmpty {
                        Text(curated.pinyin)
                            .font(.system(size: 20, weight: .semibold))
                            .foregroundStyle(LMCColor.secondary)
                    }
                    LMCWordLookupBadge(textKey: "word_lookup_curated_source")
                    Text(curated.meaning)
                        .font(.system(size: 18))
                        .foregroundStyle(LMCColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    if let example = curated.example, !example.isEmpty {
                        Divider()
                        Text("word_lookup_example_label")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(LMCColor.textSecondary)
                        Text(example)
                            .font(.system(size: 18))
                            .foregroundStyle(LMCColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                } else if let needsAi = lookup.needsAi {
                    Text(needsAi.token)
                        .font(.system(size: 34, weight: .bold))
                        .foregroundStyle(LMCColor.textPrimary)
                    Text("word_lookup_no_entry")
                        .font(.system(size: 16))
                        .foregroundStyle(LMCColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                    Button(action: explainWithAi) {
                        Label("word_lookup_explain_button", systemImage: "sparkles")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(LMCPrimaryButtonStyle())
                    .disabled(askState == .loading)

                    aiSection
                }

                Button(action: dismiss) {
                    Text("word_lookup_close")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(LMCSecondaryButtonStyle())
                .padding(.top, LMCSpace.s2)
            }
            .padding(LMCSpace.s4)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(LMCColor.background.ignoresSafeArea())
    }

    @ViewBuilder
    private var aiSection: some View {
        switch askState {
        case .idle:
            EmptyView()
        case .loading:
            HStack(spacing: LMCSpace.s2) {
                ProgressView().tint(LMCColor.primary)
                Text("word_lookup_ai_loading")
                    .font(.system(size: 16))
                    .foregroundStyle(LMCColor.textSecondary)
            }
            .frame(minHeight: LMCSpace.minTouch)
        case .answered(let answer):
            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                LMCWordLookupBadge(textKey: "word_lookup_ai_badge")
                Text(answer)
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text("word_lookup_ai_boundary")
                    .font(.system(size: 14))
                    .foregroundStyle(LMCColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(LMCSpace.s4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(LMCColor.infoContainer)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        case .failed:
            Text("word_lookup_ai_error")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(LMCColor.error)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct LMCWordLookupBadge: View {
    let textKey: LocalizedStringKey

    var body: some View {
        Text(textKey)
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(LMCColor.onSecondaryContainer)
            .padding(.horizontal, LMCSpace.s2)
            .padding(.vertical, LMCSpace.s1)
            .background(LMCColor.secondaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}

private struct VocabularyScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let story: Story
    let openSource: LMCVocabOpenSource
    let close: () -> Void
    let openQuiz: () -> Void
    @State private var wordIndex = 0
    @State private var didTrackInitialWord = false

    private var currentWord: Vocab {
        story.vocab[max(0, min(wordIndex, story.vocab.count - 1))]
    }

    var body: some View {
        VStack(spacing: 0) {
            LMCFlowTopBar(
                titleKey: "vocab_title",
                trailingText: "\(wordIndex + 1) / \(max(story.vocab.count, 1))",
                close: close
            )

            ScrollView {
                VStack(alignment: .leading, spacing: LMCSpace.s5) {
                    Text(story.titleZh)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(LMCColor.textPrimary)

                    if story.vocab.isEmpty {
                        LMCEmptyState(titleKey: "vocab_empty_title", messageKey: "vocab_empty_message")
                    } else {
                        VocabularyCard(word: currentWord) {
                            viewModel.speakCurrent(viewModel.vocabSpeechText(currentWord))
                        }

                        StepDots(count: story.vocab.count, index: wordIndex)
                    }
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth)
                .frame(maxWidth: .infinity)
            }

            LMCBottomActionBar {
                if story.vocab.isEmpty {
                    Button(backActionKey, action: close)
                        .buttonStyle(LMCSecondaryButtonStyle())
                    Button("action_continue_to_quiz", action: openQuiz)
                        .buttonStyle(LMCPrimaryButtonStyle())
                } else {
                    Button("action_previous") {
                        wordIndex = max(0, wordIndex - 1)
                    }
                        .buttonStyle(LMCSecondaryButtonStyle())
                        .disabled(wordIndex == 0)

                    Button {
                        if wordIndex >= story.vocab.count - 1 {
                            openQuiz()
                        } else {
                            wordIndex += 1
                        }
                    } label: {
                        Text(LocalizedStringKey(wordIndex >= story.vocab.count - 1 ? "action_start_quiz" : "action_next"))
                    }
                    .buttonStyle(LMCPrimaryButtonStyle())
                }
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
        .onAppear {
            guard !didTrackInitialWord else { return }
            didTrackInitialWord = true
            trackCurrentWord()
        }
        .onChange(of: wordIndex) { _ in
            trackCurrentWord()
        }
    }

    private func trackCurrentWord() {
        viewModel.trackVocabOpen(story, wordIndex: wordIndex, openSource: openSource)
    }

    private var backActionKey: LocalizedStringKey {
        openSource == .todaySummary ? "action_back_to_today" : "action_back_to_reading"
    }
}

private struct QuizScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let story: Story
    let close: () -> Void
    let readAgain: () -> Void
    let openReview: () -> Void
    let done: () -> Void

    @State private var quizState: QuizSessionState?
    @State private var completionScore: QuizScore?
    @State private var didMarkComplete = false
    @State private var didTrackQuizStart = false
    @State private var completionAnimationToken = 0
    @State private var completionJustRecorded = false
    @State private var showPlayMore = false
    @State private var retellExpanded = false
    @State private var retellCheckedItems: Set<String> = []
    @State private var retellRevealedHints: Set<String> = []
    @State private var retellMessageKey: String?

    private static let retellUseCases = RetellGuideUseCases()

    private var retellGuide: RetellGuide {
        Self.retellUseCases.buildGuide(story: story, maxItems: 5)
    }

    private var activeQuizState: QuizSessionState {
        quizState ?? viewModel.initialQuizState(story)
    }

    private var questionState: QuizQuestionState {
        viewModel.quizQuestionState(story, state: activeQuizState)
    }

    private var question: Question {
        questionState.question ?? story.questions[max(0, min(Int(questionState.questionIndex), story.questions.count - 1))]
    }

    private static let practiceUseCases = InteractivePracticeUseCases()

    private var practiceItems: [any InteractivePracticeItem] {
        Self.practiceUseCases.generate(story: story, seed: Int32(truncatingIfNeeded: story.id.hashValue), maxItems: 3)
    }

    private var hasInteractivePractice: Bool {
        !practiceItems.isEmpty
    }

    var body: some View {
        VStack(spacing: 0) {
            if activeQuizState.isComplete {
                completionView
            } else {
                quizQuestionView
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
        .fullScreenCover(isPresented: $showPlayMore) {
            PlayMorePracticeView(
                items: practiceItems,
                useCases: Self.practiceUseCases,
                close: { showPlayMore = false }
            )
        }
        .onAppear {
            guard !didTrackQuizStart else { return }
            didTrackQuizStart = true
            viewModel.trackQuizStart(story)
        }
        .task(id: activeQuizState.isComplete) {
            guard activeQuizState.isComplete, !didMarkComplete else { return }
            didMarkComplete = true
            let score = viewModel.quizScore(story, state: activeQuizState)
            completionScore = score
            viewModel.trackQuizComplete(story, score: score)
            let completionResult = await viewModel.completeStory(story, quizState: activeQuizState)
            completionJustRecorded = completionResult.wasNewCompletion
            if completionResult.wasNewCompletion || completionResult.newMilestoneDays != nil {
                await viewModel.playSfxForStoryCompletion(milestoneDays: completionResult.newMilestoneDays)
            }
            completionAnimationToken += 1
        }
    }

    private var quizQuestionView: some View {
        VStack(spacing: 0) {
            LMCFlowTopBar(
                titleKey: "quiz_title",
                trailingText: "\(Int(questionState.questionIndex) + 1) / \(Int(questionState.questionCount))",
                close: close
            )
            LMCProgressBar(value: progress)
                .padding(.horizontal, LMCSpace.s4)
                .padding(.bottom, LMCSpace.s3)

            ScrollView {
                VStack(alignment: .leading, spacing: LMCSpace.s5) {
                    Text(question.prompt)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(LMCColor.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)

                    VStack(spacing: LMCSpace.s3) {
                        ForEach(question.options, id: \.self) { option in
                            QuizOptionRow(
                                option: option,
                                isSelected: questionState.selectedAnswer == option,
                                isSubmitted: questionState.submitted,
                                isCorrectAnswer: option == questionState.result?.correctAnswer,
                                isSubmittedAnswer: questionState.selectedAnswer == option,
                                action: {
                                    quizState = viewModel.selectQuizAnswer(story, state: activeQuizState, answer: option)
                                }
                            )
                        }
                    }

                    if !questionState.submitted && questionState.selectedAnswer == nil {
                        Text("quiz_select_answer_prompt")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(LMCColor.info)
                            .fixedSize(horizontal: false, vertical: true)
                            .padding(LMCSpace.s3)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(LMCColor.infoContainer)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }

                    if questionState.submitted, let result = questionState.result {
                        FeedbackMessage(isCorrect: result.isCorrect, explanation: question.explanation)
                    }
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth)
                .frame(maxWidth: .infinity)
            }

            LMCBottomActionBar {
                Spacer()
                Button {
                    let nextQuestionState = viewModel.submitOrAdvanceQuiz(story, state: activeQuizState)
                    let wasSubmitted = questionState.submitted
                    quizState = nextQuestionState
                    if !wasSubmitted,
                       let result = viewModel.quizQuestionState(story, state: nextQuestionState).result {
                        Task {
                            await viewModel.playSfxForQuizAnswerSubmitted(isCorrect: result.isCorrect)
                        }
                    }
                } label: {
                    Text(LocalizedStringKey(quizActionKey))
                }
                .buttonStyle(LMCPrimaryButtonStyle())
                .disabled(!questionState.canSubmitOrAdvance)
            }
        }
    }

    private var retellEncouragementKey: String {
        let guide = retellGuide
        switch Self.retellUseCases.encouragementFor(
            checkedCount: Int32(retellCheckedItems.count),
            totalItems: Int32(guide.checkItems.count)
        ) {
        case .greatretell: return "retell_encouragement_great"
        case .goodprogress: return "retell_encouragement_good_progress"
        default: return "retell_encouragement_just_started"
        }
    }

    @ViewBuilder
    private var retellCard: some View {
        let guide = retellGuide
        let isRecording = viewModel.isRecordingRetell(storyId: story.id)
        let latest = viewModel.latestRetellRecording(forStoryId: story.id)

        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            SectionTitle("retell_step_title")
            Text(guide.prompt)
                .font(.system(size: 20, weight: .medium))
                .foregroundStyle(LMCColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)

            if !retellExpanded {
                Text("retell_step_optional")
                    .font(.system(size: 15))
                    .foregroundStyle(LMCColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                Button("retell_step_start") { retellExpanded = true }
                    .buttonStyle(LMCPrimaryButtonStyle())
            } else {
                HStack(spacing: LMCSpace.s2) {
                    Button(isRecording ? "retell_stop_recording" : "retell_start_recording") {
                        Task {
                            if isRecording {
                                viewModel.stopRetellRecording()
                                retellMessageKey = "retell_recording_saved"
                            } else {
                                let result = await viewModel.startRetellRecording(story: story)
                                if result == .permissionDenied {
                                    retellMessageKey = "retell_mic_denied"
                                } else if result == .failed {
                                    retellMessageKey = "retell_recording_failed"
                                } else {
                                    retellMessageKey = nil
                                }
                            }
                        }
                    }
                    .buttonStyle(LMCPrimaryButtonStyle())

                    if let latest, !isRecording {
                        Button("reading_recording_play") {
                            Task { await viewModel.playRecording(latest) }
                        }
                        .buttonStyle(LMCSecondaryButtonStyle())
                    }
                }

                Text("retell_privacy_note")
                    .font(.system(size: 13))
                    .foregroundStyle(LMCColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)

                if let retellMessageKey {
                    Text(LocalizedStringKey(retellMessageKey))
                        .font(.system(size: 13))
                        .foregroundStyle(LMCColor.primary)
                }

                if !guide.checkItems.isEmpty {
                    Divider()
                    SectionTitle("retell_self_check_title")
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: LMCSpace.s2) {
                            ForEach(guide.checkItems, id: \.text) { item in
                                FilterChip(
                                    title: item.text,
                                    isSelected: retellCheckedItems.contains(item.text)
                                ) {
                                    if retellCheckedItems.contains(item.text) {
                                        retellCheckedItems.remove(item.text)
                                    } else {
                                        retellCheckedItems.insert(item.text)
                                    }
                                }
                            }
                        }
                    }
                    Text(LocalizedStringKey(retellEncouragementKey))
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(LMCColor.primary)
                        .fixedSize(horizontal: false, vertical: true)

                    ForEach(guide.checkItems.filter { !($0.meaning ?? "").isEmpty }, id: \.text) { item in
                        let revealed = retellRevealedHints.contains(item.text)
                        Button {
                            if revealed {
                                retellRevealedHints.remove(item.text)
                            } else {
                                retellRevealedHints.insert(item.text)
                            }
                        } label: {
                            Text(revealed
                                ? "\(item.text) — \(item.meaning ?? "")"
                                : LMCStrings.format("retell_hint_show_format", item.text))
                                .font(.system(size: 14))
                                .foregroundStyle(LMCColor.textSecondary)
                                .frame(minHeight: 44, alignment: .leading)
                        }
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(LMCSpace.s4)
        .background(LMCColor.tertiaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var completionView: some View {
        VStack(spacing: 0) {
            LMCFlowTopBar(titleKey: "quiz_complete_title", trailingText: nil, close: done)
            ZStack(alignment: .top) {
                CompletionCelebrationLayer(
                    animationToken: completionAnimationToken,
                    isMilestone: viewModel.lastCompletionStreakSummary?.newMilestoneDays != nil,
                    enabled: completionJustRecorded || viewModel.lastCompletionStreakSummary?.newMilestoneDays != nil
                )

                ScrollView {
                    VStack(alignment: .center, spacing: LMCSpace.s6) {
                        CompletionStampBadge(
                            summary: viewModel.lastCompletionStreakSummary,
                            completionJustRecorded: completionJustRecorded,
                            animationToken: completionAnimationToken
                        )

                        VStack(spacing: LMCSpace.s2) {
                            Text("completion_encouragement_title")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundStyle(LMCColor.primary)
                            Text("completion_encouragement_body")
                                .font(.system(size: 18))
                                .foregroundStyle(LMCColor.textSecondary)
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                            Text(scoreText)
                                .font(.system(size: 32, weight: .bold))
                                .foregroundStyle(LMCColor.textPrimary)
                            Text("quiz_score_label")
                                .font(.system(size: 16))
                                .foregroundStyle(LMCColor.textSecondary)
                        }

                        if let streak = viewModel.lastCompletionStreakSummary {
                            CompletionMilestoneBanner(summary: streak)
                        }

                        retellCard

                        if let reviewPack = viewModel.lastCompletionReviewPack {
                            VStack(alignment: .leading, spacing: LMCSpace.s3) {
                                SectionTitle("review_pack_title")
                                Text(reviewPack.missedQuestions.isEmpty ? "review_pack_all_correct" : "review_pack_intro")
                                    .font(.system(size: 16))
                                    .foregroundStyle(LMCColor.textSecondary)
                                    .fixedSize(horizontal: false, vertical: true)
                                Button("review_pack_view", action: openReview)
                                    .buttonStyle(LMCPrimaryButtonStyle())
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(LMCSpace.s4)
                            .background(LMCColor.secondaryContainer)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }

                        if hasInteractivePractice {
                            VStack(alignment: .leading, spacing: LMCSpace.s3) {
                                SectionTitle("play_more_title")
                                Text("play_more_subtitle")
                                    .font(.system(size: 16))
                                    .foregroundStyle(LMCColor.textSecondary)
                                    .fixedSize(horizontal: false, vertical: true)
                                Button("play_more_start") { showPlayMore = true }
                                    .buttonStyle(LMCPrimaryButtonStyle())
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(LMCSpace.s4)
                            .background(LMCColor.tertiaryContainer)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        }
                    }
                    .padding(LMCSpace.s4)
                    .frame(maxWidth: LMCSpace.readingMaxWidth)
                    .frame(maxWidth: .infinity)
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityAddTraits(.updatesFrequently)

            LMCBottomActionBar {
                Button("action_read_again_from_start", action: readAgain)
                    .buttonStyle(LMCSecondaryButtonStyle())
                Button("action_done", action: done)
                    .buttonStyle(LMCPrimaryButtonStyle())
            }
        }
    }

    private var progress: Double {
        questionState.progressFraction
    }

    private var quizActionKey: String {
        if !questionState.submitted { return "action_submit" }
        return questionState.isLastQuestion ? "quiz_show_result" : "action_next"
    }

    private var scoreText: String {
        let score = completionScore ?? viewModel.quizScore(story, state: activeQuizState)
        return "\(score.correctCount) / \(score.totalQuestions)"
    }
}

// MARK: - Optional "Play more" interactive practice round

private enum PracticeFeedback {
    case none, correct, tryAgain
}

private struct PlayMorePracticeView: View {
    let items: [any InteractivePracticeItem]
    let useCases: InteractivePracticeUseCases
    let close: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            LMCFlowTopBar(titleKey: "play_more_title", trailingText: nil, close: close)
            ScrollView {
                VStack(alignment: .leading, spacing: LMCSpace.s5) {
                    Text("play_more_intro")
                        .font(.system(size: 18))
                        .foregroundStyle(LMCColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)

                    if items.isEmpty {
                        Text("play_more_empty")
                            .font(.system(size: 18, weight: .medium))
                            .foregroundStyle(LMCColor.textPrimary)
                    }

                    ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                        if let ordering = item as? InteractivePracticeItemSentenceOrdering {
                            PracticeOrderingCard(item: ordering, useCases: useCases)
                        } else if let matching = item as? InteractivePracticeItemWordMatching {
                            PracticeMatchingCard(item: matching, useCases: useCases)
                        } else if let cloze = item as? InteractivePracticeItemCloze {
                            PracticeClozeCard(item: cloze, useCases: useCases)
                        }
                    }
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth)
                .frame(maxWidth: .infinity)
            }
            LMCBottomActionBar {
                Spacer()
                Button("play_more_done", action: close)
                    .buttonStyle(LMCPrimaryButtonStyle())
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
    }
}

private struct PracticeCardShell<Content: View>: View {
    let titleKey: LocalizedStringKey
    let feedback: PracticeFeedback
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            SectionTitle(titleKey)
            content
            PracticeFeedbackRow(feedback: feedback)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(LMCSpace.s4)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(LMCColor.outlineVariant, lineWidth: 1)
        )
    }
}

private struct PracticeFeedbackRow: View {
    let feedback: PracticeFeedback

    var body: some View {
        switch feedback {
        case .none:
            EmptyView()
        case .correct:
            Text("play_more_correct")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(LMCColor.primary)
                .accessibilityAddTraits(.updatesFrequently)
        case .tryAgain:
            Text("play_more_try_again")
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(LMCColor.secondary)
                .accessibilityAddTraits(.updatesFrequently)
        }
    }
}

private struct PracticeOrderingCard: View {
    let item: InteractivePracticeItemSentenceOrdering
    let useCases: InteractivePracticeUseCases

    @State private var order: [String]
    @State private var feedback: PracticeFeedback = .none

    init(item: InteractivePracticeItemSentenceOrdering, useCases: InteractivePracticeUseCases) {
        self.item = item
        self.useCases = useCases
        _order = State(initialValue: item.shuffled)
    }

    var body: some View {
        PracticeCardShell(titleKey: "play_more_ordering_title", feedback: feedback) {
            VStack(spacing: LMCSpace.s2) {
                ForEach(Array(order.enumerated()), id: \.offset) { index, sentence in
                    HStack(spacing: LMCSpace.s2) {
                        Text(sentence)
                            .font(.system(size: 18))
                            .foregroundStyle(LMCColor.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Button {
                            move(index, by: -1)
                        } label: {
                            Image(systemName: "chevron.up")
                                .font(.system(size: 18, weight: .bold))
                                .frame(width: LMCSpace.minTouch, height: LMCSpace.minTouch)
                        }
                        .disabled(index == 0)
                        .accessibilityLabel(Text("play_more_move_up"))
                        Button {
                            move(index, by: 1)
                        } label: {
                            Image(systemName: "chevron.down")
                                .font(.system(size: 18, weight: .bold))
                                .frame(width: LMCSpace.minTouch, height: LMCSpace.minTouch)
                        }
                        .disabled(index == order.count - 1)
                        .accessibilityLabel(Text("play_more_move_down"))
                    }
                }
            }
            Button("play_more_check") {
                feedback = useCases.scoreOrdering(item: item, submitted: order) ? .correct : .tryAgain
            }
            .buttonStyle(LMCPrimaryButtonStyle())
        }
    }

    private func move(_ index: Int, by offset: Int) {
        let target = index + offset
        guard target >= 0, target < order.count else { return }
        order.swapAt(index, target)
        feedback = .none
    }
}

private struct PracticeMatchingCard: View {
    let item: InteractivePracticeItemWordMatching
    let useCases: InteractivePracticeUseCases

    @State private var meanings: [String]
    @State private var selectedWord: String?
    @State private var matches: [String: String] = [:]
    @State private var feedback: PracticeFeedback = .none

    init(item: InteractivePracticeItemWordMatching, useCases: InteractivePracticeUseCases) {
        self.item = item
        self.useCases = useCases
        _meanings = State(initialValue: item.pairs.map { $0.meaning }.shuffled())
    }

    var body: some View {
        PracticeCardShell(titleKey: "play_more_matching_title", feedback: feedback) {
            VStack(spacing: LMCSpace.s2) {
                ForEach(item.pairs, id: \.word) { pair in
                    Button {
                        selectedWord = (selectedWord == pair.word) ? nil : pair.word
                        feedback = .none
                    } label: {
                        Text(matches[pair.word].map { "\(pair.word) → \($0)" } ?? pair.word)
                            .frame(maxWidth: .infinity, minHeight: LMCSpace.minTouch)
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal, LMCSpace.s3)
                    .background(selectedWord == pair.word ? LMCColor.primaryContainer : LMCColor.surfaceVariant)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
            }
            Rectangle().fill(LMCColor.outlineVariant).frame(height: 1)
            FlowLayout(spacing: LMCSpace.s2) {
                ForEach(meanings, id: \.self) { meaning in
                    let used = matches.values.contains(meaning)
                    Button(meaning) {
                        guard let word = selectedWord else { return }
                        for (key, value) in matches where value == meaning { matches[key] = nil }
                        matches[word] = meaning
                        selectedWord = nil
                        feedback = .none
                    }
                    .buttonStyle(LMCSecondaryButtonStyle())
                    .disabled(selectedWord == nil || used)
                }
            }
            Button("play_more_check") {
                feedback = useCases.scoreMatching(item: item, submitted: matches) ? .correct : .tryAgain
            }
            .buttonStyle(LMCPrimaryButtonStyle())
        }
    }
}

private struct PracticeClozeCard: View {
    let item: InteractivePracticeItemCloze
    let useCases: InteractivePracticeUseCases

    @State private var selected: String?
    @State private var feedback: PracticeFeedback = .none

    var body: some View {
        PracticeCardShell(titleKey: "play_more_cloze_title", feedback: feedback) {
            Text(item.sentenceBefore + (selected ?? "＿＿＿") + item.sentenceAfter)
                .font(.system(size: 18))
                .foregroundStyle(LMCColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            FlowLayout(spacing: LMCSpace.s2) {
                ForEach(item.options, id: \.self) { option in
                    Button(option) {
                        selected = option
                        feedback = useCases.scoreCloze(item: item, submitted: option) ? .correct : .tryAgain
                    }
                    .buttonStyle(selected == option ? AnyButtonStyle(LMCPrimaryButtonStyle()) : AnyButtonStyle(LMCSecondaryButtonStyle()))
                }
            }
        }
    }
}

/// Type-erased button style so we can pick between primary/secondary at runtime.
private struct AnyButtonStyle: ButtonStyle {
    private let makeBodyClosure: (Configuration) -> AnyView

    init<S: ButtonStyle>(_ style: S) {
        makeBodyClosure = { configuration in AnyView(style.makeBody(configuration: configuration)) }
    }

    func makeBody(configuration: Configuration) -> some View {
        makeBodyClosure(configuration)
    }
}

/// Minimal wrapping flow layout for option chips.
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var rowWidth: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0
        var totalWidth: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if rowWidth + size.width > maxWidth, rowWidth > 0 {
                totalHeight += rowHeight + spacing
                totalWidth = max(totalWidth, rowWidth - spacing)
                rowWidth = 0
                rowHeight = 0
            }
            rowWidth += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
        totalHeight += rowHeight
        totalWidth = max(totalWidth, rowWidth - spacing)
        return CGSize(width: min(totalWidth, maxWidth), height: totalHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout Void) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}

private func reviewParentTipKey(_ tip: ReviewParentTip) -> LocalizedStringKey {
    if tip == ReviewParentTip.readtogether {
        return "review_pack_tip_read_together"
    } else if tip == ReviewParentTip.reviewduewords {
        return "review_pack_tip_review_due_words"
    } else {
        return "review_pack_tip_praise"
    }
}

private struct ReviewPackScreen: View {
    let pack: ReviewPack?
    let done: () -> Void
    let close: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            LMCFlowTopBar(titleKey: "review_pack_title", trailingText: nil, close: close)
            if let pack {
                ScrollView {
                    VStack(alignment: .leading, spacing: LMCSpace.s5) {
                        Text("review_pack_intro")
                            .font(.system(size: 16))
                            .foregroundStyle(LMCColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)

                        reviewCard(titleKey: "review_pack_missed_title") {
                            if pack.missedQuestions.isEmpty {
                                Text("review_pack_all_correct")
                                    .font(.system(size: 17))
                                    .foregroundStyle(LMCColor.textPrimary)
                                    .fixedSize(horizontal: false, vertical: true)
                            } else {
                                ForEach(pack.missedQuestions, id: \.questionId) { missed in
                                    VStack(alignment: .leading, spacing: LMCSpace.s1) {
                                        Text(missed.prompt)
                                            .font(.system(size: 17, weight: .semibold))
                                            .foregroundStyle(LMCColor.textPrimary)
                                        Text("\(LMCStrings.localized("review_pack_correct_answer_label")): \(missed.correctAnswer)")
                                            .font(.system(size: 15))
                                            .foregroundStyle(LMCColor.primary)
                                        Text("\(LMCStrings.localized("review_pack_why_label")): \(missed.explanation)")
                                            .font(.system(size: 15))
                                            .foregroundStyle(LMCColor.textSecondary)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                }
                            }
                        }

                        if !pack.reviewWords.isEmpty {
                            reviewCard(titleKey: "review_pack_words_title") {
                                ForEach(pack.reviewWords, id: \.word) { word in
                                    VStack(alignment: .leading, spacing: LMCSpace.s1) {
                                        Text("\(word.word) · \(word.pinyin)")
                                            .font(.system(size: 17, weight: .semibold))
                                            .foregroundStyle(LMCColor.textPrimary)
                                        Text(word.meaning)
                                            .font(.system(size: 15))
                                            .foregroundStyle(LMCColor.textSecondary)
                                    }
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                }
                            }
                        }

                        if let sentence = pack.rereadSentence {
                            reviewCard(titleKey: "review_pack_reread_title") {
                                Text(sentence)
                                    .font(.system(size: 18, weight: .medium))
                                    .foregroundStyle(LMCColor.textPrimary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }

                        reviewCard(titleKey: "review_pack_parent_tip_title", background: LMCColor.tertiaryContainer) {
                            Text(reviewParentTipKey(pack.parentTip))
                                .font(.system(size: 15))
                                .foregroundStyle(LMCColor.textSecondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(LMCSpace.s4)
                    .frame(maxWidth: LMCSpace.readingMaxWidth)
                    .frame(maxWidth: .infinity)
                }
            } else {
                Spacer()
                Text("review_pack_all_correct")
                    .foregroundStyle(LMCColor.textSecondary)
                Spacer()
            }
            LMCBottomActionBar {
                Button("review_pack_done", action: done)
                    .buttonStyle(LMCPrimaryButtonStyle())
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
    }

    @ViewBuilder
    private func reviewCard<Content: View>(
        titleKey: LocalizedStringKey,
        background: Color = LMCColor.surface,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            SectionTitle(titleKey)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(LMCSpace.s4)
        .background(background)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct ParentReportScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let entryPoint: LMCParentReportEntryPoint
    let openSettings: () -> Void
    let openStory: (Story) -> Void
    let openWords: () -> Void
    let openReview: () -> Void
    let recordings: [VoiceRecording]
    let playRecording: (VoiceRecording) -> Void
    let deleteRecording: (String) -> Void
    let clearAllRecordings: () -> Void
    @State private var gatePassed = false
    @State private var showPrivacy = false
    @State private var showClearRecordingsConfirm = false

    var body: some View {
        LMCScreen(maxWidth: LMCSpace.gridMaxWidth) {
            HStack {
                SectionTitle("parent_title")
                Spacer()
                IconButton(systemName: "gearshape.fill", labelKey: "nav_settings", action: openSettings)
            }

            if gatePassed {
                reportContent
            } else {
                parentGate
            }
        }
        .sheet(isPresented: $showPrivacy) {
            PrivacyScreen(close: { showPrivacy = false })
                .preferredColorScheme(.light)
        }
        .alert("parent_recordings_clear_all_confirm_title", isPresented: $showClearRecordingsConfirm) {
            Button("action_cancel", role: .cancel) {
                showClearRecordingsConfirm = false
            }
            Button("action_delete", role: .destructive) {
                clearAllRecordings()
            }
        } message: {
            Text("parent_recordings_clear_all_confirm_message")
        }
    }

    private var parentGate: some View {
        LMCAdultGateCard(messageKey: "parent_gate_message") {
            gatePassed = true
            viewModel.trackParentReportOpen(entryPoint: entryPoint)
        }
    }

    private var reportContent: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s6) {
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: LMCSpace.s3) {
                MetricTile(titleKey: "parent_stories_read", value: "\(summary?.storiesCompletedThisWeek ?? 0)")
                MetricTile(titleKey: "parent_reading_days", value: "\(readingDays)")
                MetricTile(titleKey: "parent_quiz_correct", value: quizCorrectText)
                MetricTile(titleKey: "parent_words_reviewed", value: "\(summary?.vocabLearnedThisWeek ?? 0)")
            }

            ParentPractisedSection(abilityMap: viewModel.abilityMap)

            ParentAdviceCard(
                summary: summary,
                story: adviceStory,
                reviewWords: openWords,
                openStory: openStory
            )

            ParentWeeklyPlanSection(
                plan: viewModel.parentWeeklyPlan(),
                story: { id in viewModel.story(id: id) }
            )

            if let pending = viewModel.pendingReview {
                Button(action: openReview) {
                    VStack(alignment: .leading, spacing: LMCSpace.s2) {
                        SectionTitle("parent_review_pack_title")
                        Text(reviewParentTipKey(pending.pack.parentTip))
                            .font(.system(size: 15))
                            .foregroundStyle(LMCColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(LMCSpace.s4)
                    .background(LMCColor.tertiaryContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .buttonStyle(.plain)
            }

            ParentRecordingSection(
                recordings: recordings,
                maxPerStory: viewModel.recordingRetentionLimits().perStory,
                maxOverall: viewModel.recordingRetentionLimits().overall,
                lookupStoryTitle: { id in
                    viewModel.story(id: id)?.titleZh ?? id
                },
                playRecording: playRecording,
                deleteRecording: deleteRecording,
                clearAllRecordings: { showClearRecordingsConfirm = true }
            )

            VStack(alignment: .leading, spacing: LMCSpace.s3) {
                SectionTitle("parent_story_progress")
                ForEach(viewModel.stories, id: \.id) { story in
                    StoryProgressRow(
                        story: story,
                        progress: viewModel.progressValue(for: story),
                        statusKey: viewModel.progressLabelKey(for: story)
                    )
                }
            }

            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                SectionTitle("parent_privacy")
                Text("parent_privacy_note")
                    .font(.system(size: 16))
                    .foregroundStyle(LMCColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                Button("action_open_privacy") {
                    showPrivacy = true
                }
                .buttonStyle(LMCSecondaryButtonStyle())
            }
            .padding(LMCSpace.s4)
            .background(LMCColor.infoContainer)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    private var quizCorrectText: String {
        guard let summary else { return "0 / 0" }
        return "\(summary.correctCount) / \(summary.questionCount)"
    }

    private var readingDays: Int {
        Int(summary?.readingDaysThisWeek ?? 0)
    }

    private var summary: ParentReportSummary? {
        viewModel.parentReportSummary()
    }

    private var adviceStory: Story? {
        guard let storyId = summary?.advice.storyId else { return nil }
        return viewModel.story(id: storyId)
    }
}

private struct ParentRecordingSection: View {
    let recordings: [VoiceRecording]
    let maxPerStory: Int
    let maxOverall: Int
    let lookupStoryTitle: (String) -> String
    let playRecording: (VoiceRecording) -> Void
    let deleteRecording: (String) -> Void
    let clearAllRecordings: () -> Void

    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        return formatter
    }

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            HStack(alignment: .center) {
                SectionTitle("parent_recordings_title")
                Spacer()
                if !recordings.isEmpty {
                    Button("parent_recordings_clear_all") {
                        clearAllRecordings()
                    }
                    .buttonStyle(LMCSecondaryButtonStyle())
                }
            }

            if recordings.isEmpty {
                Text("parent_recordings_empty")
                    .font(.system(size: 15))
                    .foregroundStyle(LMCColor.textSecondary)
            } else {
                ForEach(recordingsByStory) { row in
                    HStack(spacing: LMCSpace.s2) {
                        VStack(alignment: .leading, spacing: LMCSpace.s1) {
                            Text(row.title)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(LMCColor.textPrimary)
                            Text(row.subtitle)
                                .font(.system(size: 13))
                                .foregroundStyle(LMCColor.textSecondary)
                        }

                        Spacer()

                        Button(action: { playRecording(row.recording) }) {
                            Image(systemName: "play.circle")
                                .font(.system(size: 20, weight: .bold))
                                .frame(width: LMCSpace.minTouch, height: LMCSpace.minTouch)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("reading_recording_play"))

                        Button(action: { deleteRecording(row.recording.id) }) {
                            Image(systemName: "trash")
                                .font(.system(size: 20, weight: .bold))
                                .frame(width: LMCSpace.minTouch, height: LMCSpace.minTouch)
                                .foregroundStyle(LMCColor.textSecondary)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("action_delete"))
                    }
                    .padding(LMCSpace.s3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(LMCColor.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }

                Text(
                    LMCStrings.format(
                        "parent_recordings_retention_statement",
                        maxPerStory,
                        maxOverall
                    )
                )
                .font(.system(size: 13))
                .foregroundStyle(LMCColor.textSecondary)
            }
        }
        .padding(LMCSpace.s3)
        .background(LMCColor.surfaceVariant)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var recordingsByStory: [RecordingRow] {
        recordings
            .sorted { $0.createdAtEpochMillis > $1.createdAtEpochMillis }
            .compactMap { recording in
                let title = lookupStoryTitle(recording.storyId)
                if recording.paragraphIndex < 0 {
                    let subtitle = LMCStrings.format(
                        "parent_recording_retell_row_format",
                        title,
                        dateString(recording.createdAtEpochMillis),
                        durationText(recording.durationMs)
                    )
                    return RecordingRow(recording: recording, title: title, subtitle: subtitle)
                }
                guard let indexes = paragraphAndSentenceIndex(for: recording) else { return nil }
                let subtitle = LMCStrings.format(
                    "parent_recording_row_format",
                    title,
                    indexes.0 + 1,
                    indexes.1 + 1,
                    dateString(recording.createdAtEpochMillis),
                    durationText(recording.durationMs)
                )
                return RecordingRow(recording: recording, title: title, subtitle: subtitle)
            }
    }

    private func durationText(_ durationMs: Int64) -> String {
        let totalSeconds = Int((durationMs + 500) / 1_000)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        return LMCStrings.format("reading_recording_duration", String(format: "%02d:%02d", minutes, seconds))
    }

    private func dateString(_ epochMillis: Int64) -> String {
        dateFormatter.string(from: Date(timeIntervalSince1970: Double(epochMillis) / 1_000))
    }

    private func paragraphAndSentenceIndex(for recording: VoiceRecording) -> (Int, Int)? {
        let fileName = URL(fileURLWithPath: recording.filePath).lastPathComponent
        let stem = fileName
            .split(separator: ".")
            .first
            .map(String.init) ?? fileName
        let parts = stem.split(separator: "_")

        var paragraphIndex = -1
        var sentenceIndex = -1
        for part in parts {
            if part.hasPrefix("p"), let value = Int(part.dropFirst()) {
                paragraphIndex = value - 1
            }
            if part.hasPrefix("s"), let value = Int(part.dropFirst()) {
                sentenceIndex = value - 1
            }
        }

        guard paragraphIndex >= 0, sentenceIndex >= 0 else { return nil }
        return (paragraphIndex, sentenceIndex)
    }

    private struct RecordingRow: Identifiable {
        let recording: VoiceRecording
        let title: String
        let subtitle: String

        var id: String {
            recording.id
        }
    }
}

private struct ParentAdviceCard: View {
    let summary: ParentReportSummary?
    let story: Story?
    let reviewWords: () -> Void
    let openStory: (Story) -> Void

    var body: some View {
        if let summary {
            VStack(alignment: .leading, spacing: LMCSpace.s3) {
                Label("parent_advice_title", systemImage: "lightbulb.fill")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)

                Text(adviceText(for: summary))
                    .font(.system(size: 16))
                    .foregroundStyle(LMCColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)

                if shouldShowRetellPrompt(for: summary), let story {
                    Text(LMCStrings.format("parent_advice_retell_prompt_format", story.retellPrompt))
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(LMCColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }

                if summary.advice.type == .reviewduewords {
                    Button("parent_advice_action_review_words", action: reviewWords)
                        .buttonStyle(LMCSecondaryButtonStyle())
                } else if shouldOpenStory(for: summary), let story {
                    Button("parent_advice_action_open_story") {
                        openStory(story)
                    }
                    .buttonStyle(LMCSecondaryButtonStyle())
                }
            }
            .padding(LMCSpace.s4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(LMCColor.primaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }

    private func adviceText(for summary: ParentReportSummary) -> String {
        switch summary.advice.type {
        case .reviewduewords:
            return LMCStrings.format("parent_advice_review_due_words_format", Int(summary.advice.dueWordCount))
        case .revisitrecentstory:
            return LMCStrings.format("parent_advice_revisit_story_format", story?.titleZh ?? "")
        case .trynextstory:
            return LMCStrings.format("parent_advice_try_next_story_format", story?.titleZh ?? "")
        case .celebratestreak:
            return LMCStrings.localized("parent_advice_keep_streak")
        default:
            return LMCStrings.localized("parent_advice_read_together")
        }
    }

    private func shouldShowRetellPrompt(for summary: ParentReportSummary) -> Bool {
        summary.advice.type == .revisitrecentstory || summary.advice.type == .trynextstory
    }

    private func shouldOpenStory(for summary: ParentReportSummary) -> Bool {
        summary.advice.type == .revisitrecentstory || summary.advice.type == .trynextstory
    }
}

// Parent weekly action plan: stories read, mastered vs needs-practice words,
// one sentence to re-read, a weekend retell question, a top suggestion, and a
// PII-free shareable card. Mirrors the Android Compose section (AGENTS.md §7).
private struct ParentWeeklyPlanSection: View {
    let plan: ParentWeeklyPlan
    let story: (String) -> Story?

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s4) {
            Label("weekly_plan_title", systemImage: "calendar.badge.clock")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)

            // 1. Stories read this week
            block(titleKey: "weekly_plan_stories_read") {
                if plan.storiesReadThisWeek.isEmpty {
                    bodyText(LMCStrings.localized("weekly_plan_stories_empty"))
                } else {
                    ForEach(plan.storiesReadThisWeek, id: \.storyId) { s in
                        bodyText(LMCStrings.format("weekly_plan_story_titles_format", s.titleZh, s.titleEn))
                    }
                }
            }

            // 2. Mastered vs needs-practice words
            block(titleKey: "weekly_plan_mastered_words") {
                wordList(plan.masteredWords, emptyKey: "weekly_plan_mastered_empty")
            }
            block(titleKey: "weekly_plan_weak_words") {
                wordList(plan.weakWords, emptyKey: "weekly_plan_weak_empty")
            }

            // 3. One sentence to re-read (only when present)
            if let sentence = plan.rereadSentence, !sentence.isEmpty {
                block(titleKey: "weekly_plan_reread_title") {
                    bodyText(sentence)
                    if let storyId = plan.rereadStoryId, let s = story(storyId) {
                        Text(LMCStrings.format("weekly_plan_reread_from_format", s.titleZh))
                            .font(.system(size: 14))
                            .foregroundStyle(LMCColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }

            // 4. Weekend retell question (only when present)
            if let prompt = plan.weekendRetellPrompt, !prompt.isEmpty {
                block(titleKey: "weekly_plan_retell_title") {
                    bodyText(prompt)
                }
            }

            // 5. Top suggestion
            block(titleKey: "weekly_plan_advice_title") {
                bodyText(adviceText(plan.topAdvice))
            }

            // 6. Shareable, PII-free card
            shareCard
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.surfaceVariant)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var shareCard: some View {
        let summary = LMCStrings.format(
            "weekly_plan_share_summary_format",
            Int(plan.shareCard.storiesThisWeek),
            Int(plan.shareCard.wordsInNotebook),
            Int(plan.shareCard.masteredWords),
            Int(plan.shareCard.streakDays)
        )
        return VStack(alignment: .leading, spacing: LMCSpace.s2) {
            Text("weekly_plan_share_card_title")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(LMCColor.textPrimary)
            Text(summary)
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            ShareLink(item: summary) {
                Text("weekly_plan_share_button")
            }
            .buttonStyle(LMCSecondaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.primaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    @ViewBuilder
    private func block<Content: View>(
        titleKey: LocalizedStringKey,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: LMCSpace.s1) {
            Text(titleKey)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(LMCColor.textPrimary)
            content()
        }
    }

    @ViewBuilder
    private func wordList(_ words: [WeeklyPlanWord], emptyKey: LocalizedStringKey) -> some View {
        if words.isEmpty {
            Text(emptyKey)
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        } else {
            ForEach(words, id: \.word) { w in
                bodyText(LMCStrings.format("weekly_plan_word_line_format", w.word, w.pinyin, w.meaning))
            }
        }
    }

    private func bodyText(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 16))
            .foregroundStyle(LMCColor.textSecondary)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func adviceText(_ advice: ParentAdviceType) -> String {
        switch advice {
        case .reviewduewords:
            return LMCStrings.localized("weekly_plan_advice_review_due_words")
        case .revisitrecentstory:
            return LMCStrings.localized("weekly_plan_advice_revisit_story")
        case .trynextstory:
            return LMCStrings.localized("weekly_plan_advice_try_next_story")
        case .celebratestreak:
            return LMCStrings.localized("weekly_plan_advice_celebrate_streak")
        default:
            return LMCStrings.localized("weekly_plan_advice_read_together")
        }
    }
}

private struct LMCAdultGateCard: View {
    let messageKey: LocalizedStringKey
    let unlock: () -> Void
    @State private var answer = ""
    @State private var showError = false

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s4) {
            Label("parent_gate_title", systemImage: "lock.shield.fill")
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)

            Text(messageKey)
                .font(.system(size: 17))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            Text("parent_gate_question")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            TextField("parent_gate_answer_label", text: $answer)
                .keyboardType(.numberPad)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled(true)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(LMCColor.textPrimary)
                .padding(LMCSpace.s3)
                .frame(minHeight: LMCSpace.minTouch)
                .background(LMCColor.surfaceVariant)
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(showError ? LMCColor.error : LMCColor.outlineVariant, lineWidth: 1)
                )
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                .onChange(of: answer) { _ in showError = false }

            if showError {
                Text("parent_gate_error")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(LMCColor.error)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Button("parent_gate_button") {
                if answer.trimmingCharacters(in: .whitespacesAndNewlines) == LMCParentGateExpectedAnswer {
                    showError = false
                    unlock()
                } else {
                    showError = true
                }
            }
            .buttonStyle(LMCPrimaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private let LMCParentGateExpectedAnswer = "23"

private struct SettingsScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    @Binding var localeIdentifier: String
    @Binding var showPinyin: Bool
    @Binding var readingSize: String
    @Binding var aiBackendBaseURL: String
    @Binding var sfxEnabled: Bool
    @Binding var sfxVolume: Double
    let openParent: () -> Void
    let previewSfx: () -> Void
    let includeTitle: Bool
    @State private var showPrivacy = false
    @State private var showFeedback = false
    @State private var showRecheckLevel = false
    @State private var showAiConsole = false
    @State private var adultSettingsUnlocked = false

    var body: some View {
        LMCScreen(maxWidth: LMCSpace.readingMaxWidth) {
            if includeTitle {
                SectionTitle("settings_title")
            }

            if adultSettingsUnlocked {
                settingsContent
            } else {
                LMCAdultGateCard(messageKey: "settings_grownups_gate_message") {
                    adultSettingsUnlocked = true
                }
            }
        }
        .sheet(isPresented: $showPrivacy) {
            PrivacyScreen(close: { showPrivacy = false })
                .preferredColorScheme(.light)
        }
        .sheet(isPresented: $showAiConsole) {
            AiSafetyConsoleScreen(viewModel: viewModel, close: { showAiConsole = false })
                .environment(\.locale, Locale(identifier: localeIdentifier))
                .preferredColorScheme(.light)
        }
        .sheet(isPresented: $showFeedback) {
            FeedbackSheet(viewModel: viewModel)
                .environment(\.locale, Locale(identifier: localeIdentifier))
                .preferredColorScheme(.light)
        }
        .sheet(isPresented: $showRecheckLevel) {
            PlacementCheckView(
                viewModel: viewModel,
                onComplete: { level in
                    await viewModel.setAssessedReadingLevel(level)
                    showRecheckLevel = false
                },
                onSkip: { showRecheckLevel = false }
            )
            .environment(\.locale, Locale(identifier: localeIdentifier))
            .preferredColorScheme(.light)
        }
    }

    @ViewBuilder
    private var settingsContent: some View {
        SettingsSection(titleKey: "settings_language") {
            VStack(spacing: LMCSpace.s2) {
                ForEach(LMCAppLocale.allCases, id: \.rawValue) { locale in
                    SettingsChoiceRow(
                        titleKey: locale.labelKey,
                        isSelected: localeIdentifier == locale.rawValue,
                        action: { localeIdentifier = locale.rawValue }
                    )
                }
            }
        }

        SettingsSection(titleKey: "settings_reading") {
            Toggle(isOn: $showPinyin) {
                Text("settings_pinyin_default")
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
            }
            .tint(LMCColor.secondary)
            .frame(minHeight: LMCSpace.minTouch)

            Divider().background(LMCColor.outlineVariant)

            HStack(spacing: LMCSpace.s3) {
                Text("reading_font_size")
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
                Spacer()
                LMCSegmentedReadingSize(readingSize: $readingSize)
            }
            .frame(minHeight: LMCSpace.minTouch)

            Divider().background(LMCColor.outlineVariant)

            SettingsInfoRow(titleKey: "settings_audio_voice", valueKey: "settings_audio_system")

            Divider().background(LMCColor.outlineVariant)

            SettingsInfoRow(
                titleKey: "settings_sfx",
                valueKey: sfxEnabled ? "settings_sfx_enabled" : "settings_sfx_disabled"
            )

            Toggle(isOn: $sfxEnabled) {
                Text("settings_sfx_enable")
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
            }
            .tint(LMCColor.secondary)
            .frame(minHeight: LMCSpace.minTouch)

            Divider().background(LMCColor.outlineVariant)

            HStack(spacing: LMCSpace.s2) {
                Text("settings_sfx_volume")
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
                Spacer()
                Text(LMCStrings.format("settings_sfx_volume_percent", Int(sfxVolume * 100)))
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(LMCColor.textSecondary)
            }
            .frame(minHeight: LMCSpace.minTouch)

            Slider(
                value: $sfxVolume,
                in: 0...1,
                onEditingChanged: { isEditing in
                    if !isEditing && sfxEnabled {
                        previewSfx()
                    }
                }
            )
                .disabled(!sfxEnabled)

            Divider().background(LMCColor.outlineVariant)

            HStack(spacing: LMCSpace.s2) {
                Spacer()
                Button("settings_sfx_preview") {
                    previewSfx()
                }
                .buttonStyle(LMCSecondaryButtonStyle())
                .disabled(!sfxEnabled)
                Spacer()
            }
            .frame(minHeight: LMCSpace.minTouch)
        }

        SettingsSection(titleKey: "settings_privacy") {
            Text("parent_privacy_note")
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Divider().background(LMCColor.outlineVariant)
            SettingsNavigationRow(titleKey: "settings_privacy", systemName: "shield.checkered", action: { showPrivacy = true })
        }

        SettingsSection(titleKey: "settings_grownups") {
            adultSettingsContent
        }

        Text("settings_about")
            .font(.system(size: 14))
            .foregroundStyle(LMCColor.textSecondary)
    }

    private var adultSettingsContent: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            SettingsNavigationRow(titleKey: "parent_title", systemName: "chart.bar.xaxis", action: openParent)
            Divider().background(LMCColor.outlineVariant)
            SettingsNavigationRow(titleKey: "settings_ai_console", systemName: "shield.lefthalf.filled", action: { showAiConsole = true })
            Divider().background(LMCColor.outlineVariant)
            SettingsNavigationRow(titleKey: "settings_recheck_level", systemName: "checkmark.seal", action: { showRecheckLevel = true })
            Divider().background(LMCColor.outlineVariant)
            SettingsNavigationRow(titleKey: "settings_give_feedback", systemName: "bubble.left.and.bubble.right.fill", action: { showFeedback = true })
            Divider().background(LMCColor.outlineVariant)
            VStack(alignment: .leading, spacing: LMCSpace.s3) {
                Text("settings_developer")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)

                Text("settings_ai_boundary")
                    .font(.system(size: 15))
                    .foregroundStyle(LMCColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)

                Text("settings_ai_backend_base_url")
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)

                TextField("settings_ai_backend_placeholder", text: $aiBackendBaseURL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled(true)
                    .keyboardType(.URL)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(LMCColor.textPrimary)
                    .padding(LMCSpace.s3)
                    .frame(minHeight: LMCSpace.minTouch)
                    .background(LMCColor.surfaceVariant)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))

                Button("settings_ai_use_mock") {
                    Task {
                        aiBackendBaseURL = await viewModel.resetAiBackendBaseURL()
                    }
                }
                .buttonStyle(LMCSecondaryButtonStyle())
            }
        }
    }
}

private struct PrivacyScreen: View {
    let close: () -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: LMCSpace.s5) {
                    VStack(alignment: .leading, spacing: LMCSpace.s2) {
                        SectionTitle("privacy_title")
                        Text("privacy_summary")
                            .font(.system(size: 16))
                            .foregroundStyle(LMCColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    VStack(alignment: .leading, spacing: LMCSpace.s3) {
                        PrivacyPoint(titleKey: "privacy_coppa_gdprk_title", bodyKey: "privacy_coppa_gdprk")
                        PrivacyPoint(titleKey: "privacy_parent_account_title", bodyKey: "privacy_parent_account_only")
                        PrivacyPoint(titleKey: "privacy_no_child_details_title", bodyKey: "privacy_no_child_details")
                        PrivacyPoint(titleKey: "privacy_ai_scope_title", bodyKey: "privacy_ai_scope")
                        PrivacyPoint(titleKey: "privacy_analytics_title", bodyKey: "privacy_analytics_anonymous")
                        PrivacyPoint(titleKey: "privacy_local_progress_title", bodyKey: "privacy_local_progress")
                        PrivacyPoint(titleKey: "privacy_content_title", bodyKey: "privacy_public_domain_content")
                        PrivacyPoint(titleKey: "privacy_network_title", bodyKey: "privacy_in_app_no_external_page")
                    }
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth, alignment: .leading)
                .frame(maxWidth: .infinity)
            }
            .background(LMCColor.background)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("action_close", action: close)
                }
            }
        }
    }
}

private struct PrivacyPoint: View {
    let titleKey: LocalizedStringKey
    let bodyKey: LocalizedStringKey

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            Text(titleKey)
                .font(.system(size: 17, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
            Text(bodyKey)
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct AiSafetyConsoleScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let close: () -> Void

    private static let relativeFormatter: RelativeDateTimeFormatter = {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter
    }()

    private func relativeTime(_ epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMillis) / 1_000)
        return Self.relativeFormatter.localizedString(for: date, relativeTo: Date())
    }

    var body: some View {
        let records = viewModel.aiInteractionRecords
        let summary = viewModel.aiInteractionSummary
        return NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: LMCSpace.s4) {
                    SectionTitle("ai_console_title")

                    VStack(alignment: .leading, spacing: LMCSpace.s2) {
                        Text(LMCStrings.format("ai_console_summary_total", Int(summary.totalCount)))
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(LMCColor.textPrimary)
                        Text(LMCStrings.format("ai_console_summary_declined", Int(summary.outOfScopeCount)))
                            .font(.system(size: 16))
                            .foregroundStyle(LMCColor.textSecondary)
                        let lastText = summary.lastEpochMillis.map { relativeTime($0.int64Value) }
                            ?? LMCStrings.localized("ai_console_last_none")
                        Text(LMCStrings.format("ai_console_summary_last", lastText))
                            .font(.system(size: 16))
                            .foregroundStyle(LMCColor.textSecondary)
                    }
                    .padding(LMCSpace.s4)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(LMCColor.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                    Text("ai_console_retention")
                        .font(.system(size: 15))
                        .foregroundStyle(LMCColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)

                    if !records.isEmpty {
                        Button("ai_console_clear_all") {
                            Task { await viewModel.clearAiInteractionLog() }
                        }
                        .buttonStyle(LMCSecondaryButtonStyle())
                    }

                    if records.isEmpty {
                        Text("ai_console_empty")
                            .font(.system(size: 16))
                            .foregroundStyle(LMCColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    } else {
                        ForEach(records, id: \.id) { record in
                            AiInteractionRow(
                                record: record,
                                relativeTime: relativeTime(record.epochMillis),
                                onDelete: {
                                    Task { await viewModel.deleteAiInteraction(id: record.id) }
                                }
                            )
                        }
                    }
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth, alignment: .leading)
                .frame(maxWidth: .infinity)
            }
            .background(LMCColor.background)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("action_close", action: close)
                }
            }
        }
        .task { await viewModel.refreshAiInteractionLog() }
    }
}

private struct AiInteractionRow: View {
    let record: AiInteractionRecord
    let relativeTime: String
    let onDelete: () -> Void

    private var isDeclined: Bool { record.outcome == .outofscope }

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            HStack(spacing: LMCSpace.s2) {
                Text(isDeclined ? "ai_console_outcome_declined" : "ai_console_outcome_allowed")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(isDeclined ? LMCColor.onErrorContainer : LMCColor.onSecondaryContainer)
                    .padding(.horizontal, LMCSpace.s2)
                    .padding(.vertical, LMCSpace.s1)
                    .background(isDeclined ? LMCColor.errorContainer : LMCColor.secondaryContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                Spacer()
                Text(relativeTime)
                    .font(.system(size: 13))
                    .foregroundStyle(LMCColor.textSecondary)
            }
            Text(LMCStrings.format("ai_console_query_label", record.query))
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(LMCColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            Text(LMCStrings.format("ai_console_answer_label", record.answerPreview))
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Button("ai_console_delete", role: .destructive, action: onDelete)
                .buttonStyle(LMCSecondaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct FeedbackSheet: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var viewModel: ReaderViewModel
    @State private var draft = LMCFeedbackDraft()
    @State private var saveState: LMCFeedbackSaveState = .idle

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: LMCSpace.s6) {
                    SectionTitle("feedback_title")

                    Text("feedback_privacy_warning")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(LMCColor.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(LMCSpace.s4)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(LMCColor.infoContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                    SettingsSection(titleKey: "feedback_satisfaction") {
                        Picker("feedback_satisfaction", selection: $draft.satisfaction) {
                            ForEach(LMCFeedbackSatisfaction.allCases, id: \.rawValue) { option in
                                Text(option.labelKey).tag(option)
                            }
                        }
                        .pickerStyle(.segmented)
                    }

                    SettingsSection(titleKey: "feedback_child_age_band") {
                        Picker("feedback_child_age_band", selection: $draft.childAgeBand) {
                            ForEach(LMCFeedbackAgeBand.allCases, id: \.rawValue) { option in
                                Text(option.labelKey).tag(option)
                            }
                        }
                        .pickerStyle(.menu)
                    }

                    SettingsSection(titleKey: "feedback_issue_type") {
                        Picker("feedback_issue_type", selection: $draft.issueType) {
                            ForEach(LMCFeedbackIssueType.allCases, id: \.rawValue) { option in
                                Text(option.labelKey).tag(option)
                            }
                        }
                        .pickerStyle(.menu)
                    }

                    SettingsSection(titleKey: "feedback_suggestion") {
                        ZStack(alignment: .topLeading) {
                            if draft.suggestion.isEmpty {
                                Text("feedback_suggestion_placeholder")
                                    .font(.system(size: 16))
                                    .foregroundStyle(LMCColor.textSecondary)
                                    .padding(.horizontal, LMCSpace.s2)
                                    .padding(.vertical, LMCSpace.s3)
                            }
                            TextEditor(text: $draft.suggestion)
                                .font(.system(size: 16))
                                .foregroundStyle(LMCColor.textPrimary)
                                .frame(minHeight: 132)
                                .scrollContentBackground(.hidden)
                                .background(Color.clear)
                        }
                        .padding(LMCSpace.s2)
                        .background(LMCColor.surfaceVariant)
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }

                    SettingsSection(titleKey: "feedback_parent_contact") {
                        Text("feedback_parent_contact_note")
                            .font(.system(size: 15))
                            .foregroundStyle(LMCColor.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)

                        TextField("feedback_parent_contact_placeholder", text: $draft.parentContact)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled(true)
                            .keyboardType(.emailAddress)
                            .font(.system(size: 16))
                            .foregroundStyle(LMCColor.textPrimary)
                            .padding(LMCSpace.s3)
                            .frame(minHeight: LMCSpace.minTouch)
                            .background(LMCColor.surfaceVariant)
                            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }

                    feedbackStatus
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth, alignment: .leading)
                .frame(maxWidth: .infinity)
            }
            .background(LMCColor.background)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("action_close") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("feedback_submit") { saveFeedback() }
                        .disabled(!draft.canSubmit || saveState == .saving)
                }
            }
        }
    }

    @ViewBuilder
    private var feedbackStatus: some View {
        switch saveState {
        case .idle:
            EmptyView()
        case .saving:
            ProgressView()
                .tint(LMCColor.primary)
        case .saved:
            Label("feedback_saved", systemImage: "checkmark.circle.fill")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(LMCColor.success)
        case .failed:
            Label("feedback_error", systemImage: "exclamationmark.triangle.fill")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(LMCColor.error)
        }
    }

    private func saveFeedback() {
        saveState = .saving
        Task {
            do {
                try await viewModel.saveFeedback(draft)
                saveState = .saved
                draft = LMCFeedbackDraft()
            } catch {
                saveState = .failed
            }
        }
    }
}

private struct LMCScreen<Content: View>: View {
    let maxWidth: CGFloat
    let content: Content

    init(maxWidth: CGFloat = LMCSpace.readingMaxWidth, @ViewBuilder content: () -> Content) {
        self.maxWidth = maxWidth
        self.content = content()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: LMCSpace.s6) {
                content
            }
            .padding(LMCSpace.s4)
            .frame(maxWidth: maxWidth, alignment: .leading)
            .frame(maxWidth: .infinity)
        }
        .background(LMCColor.background)
    }
}

private struct SectionTitle: View {
    let key: LocalizedStringKey

    init(_ key: LocalizedStringKey) {
        self.key = key
    }

    var body: some View {
        Text(key)
            .font(.system(size: 22, weight: .bold))
            .foregroundStyle(LMCColor.textPrimary)
            .fixedSize(horizontal: false, vertical: true)
    }
}

private struct StoryHeroCard: View {
    let story: Story
    let progress: Double
    let progressLabelKey: String
    let isCompleted: Bool
    let openReading: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s4) {
            HStack(alignment: .top, spacing: LMCSpace.s4) {
                StoryCover(story: story, size: 148)

                VStack(alignment: .leading, spacing: LMCSpace.s2) {
                    StoryTitleBlock(story: story)
                    HStack(spacing: LMCSpace.s2) {
                        LevelChip(level: Int(story.level))
                        Text(story.ageRange)
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(LMCColor.tertiary)
                    }
                }
            }

            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                LMCProgressBar(value: progress)
                Text(LocalizedStringKey(progressLabelKey))
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(LMCColor.textSecondary)
            }

            Button {
                openReading()
            } label: {
                Label(
                    LocalizedStringKey(isCompleted ? "action_read_again" : "action_start_reading"),
                    systemImage: "book.pages.fill"
                )
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(LMCPrimaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 1)
        .contentShape(Rectangle())
        .onTapGesture(perform: openReading)
    }
}

private struct StoryListCard: View {
    let story: Story
    let sequenceNumber: Int
    let progress: Double
    let progressLabelKey: String
    let isCompleted: Bool
    let action: () -> Void

    private var hasProgress: Bool {
        progress > 0 && !isCompleted
    }

    private var statusKey: LocalizedStringKey {
        if isCompleted { return "library_status_done" }
        if hasProgress { return "library_status_continue" }
        return "library_status_new"
    }

    private var actionKey: LocalizedStringKey {
        if isCompleted { return "action_read_again_short" }
        if hasProgress { return "action_continue" }
        return "action_start_reading"
    }

    var body: some View {
        HStack(alignment: .top, spacing: LMCSpace.s3) {
            StoryCover(story: story, size: 96)

            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                Text(LMCStrings.format("library_story_order_series", sequenceNumber, LMCStrings.localized("series_three_kingdoms")))
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(LMCColor.tertiary)
                StoryTitleBlock(story: story)
                HStack(spacing: LMCSpace.s2) {
                    Text(statusKey)
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(isCompleted ? LMCColor.success : LMCColor.tertiary)
                        .padding(.horizontal, LMCSpace.s2)
                        .padding(.vertical, LMCSpace.s1)
                        .background(isCompleted ? LMCColor.successContainer : LMCColor.tertiaryContainer)
                        .clipShape(Capsule())
                    LevelChip(level: Int(story.level))
                }
                LMCProgressBar(value: progress)
                Text(LocalizedStringKey(progressLabelKey))
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(LMCColor.textSecondary)
                if isCompleted {
                    Button(action: action) {
                        Text(actionKey)
                    }
                    .buttonStyle(LMCSecondaryButtonStyle())
                } else {
                    Button(action: action) {
                        Text(actionKey)
                    }
                    .buttonStyle(LMCPrimaryButtonStyle())
                }
            }
        }
        .padding(LMCSpace.s4)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 1)
        .contentShape(Rectangle())
        .onTapGesture(perform: action)
    }
}

private struct CompactStoryRow: View {
    let story: Story
    let progress: Double
    let progressLabelKey: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: LMCSpace.s3) {
                StoryCover(story: story, size: 64)
                VStack(alignment: .leading, spacing: LMCSpace.s1) {
                    Text(story.titleZh)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(LMCColor.textPrimary)
                    Text(story.titleEn)
                        .font(.system(size: 15))
                        .foregroundStyle(LMCColor.textSecondary)
                        .lineLimit(2)
                    LMCProgressBar(value: progress)
                    Text(LocalizedStringKey(progressLabelKey))
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(LMCColor.textSecondary)
                }
                Image(systemName: "chevron.right")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(LMCColor.secondary)
                    .accessibilityHidden(true)
            }
            .padding(LMCSpace.s3)
            .background(LMCColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct StoryTitleBlock: View {
    let story: Story

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s1) {
            Text(story.titleZh)
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
                .lineLimit(2)
            Text(story.titleEn)
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textSecondary)
                .lineLimit(2)
        }
    }
}

/// A warm, kid-friendly storybook palette for a programmatic cover.
/// `gradient` is the background; `ink` is the prominent Chinese title; `subtitle` the English line;
/// `badge`/`onBadge` the small level pill. Pairings keep legible contrast on the gradient's lighter
/// (top-leading) tones. To use a real cover image later, replace only the gradient layer.
private struct StoryCoverPalette {
    let gradient: [Color]
    let ink: Color
    let subtitle: Color
    let badge: Color
    let onBadge: Color
}

private enum StoryCoverPalettes {
    // 6 fixed palettes drawn from the LMC design tokens. paletteIndex (0..5) maps 1:1, so the
    // same story always gets the same palette — matching the Android mapping.
    static let all: [StoryCoverPalette] = [
        // 0 — warm vermilion
        StoryCoverPalette(
            gradient: [Color(hex: 0xFFE0D6), Color(hex: 0xFFC4B2)],
            ink: Color(hex: 0x3D0E08), subtitle: Color(hex: 0x6E2417),
            badge: Color(hex: 0xB84535), onBadge: .white
        ),
        // 1 — calm teal
        StoryCoverPalette(
            gradient: [Color(hex: 0xD9F1EE), Color(hex: 0xB6E3DE)],
            ink: Color(hex: 0x063432), subtitle: Color(hex: 0x14534F),
            badge: Color(hex: 0x126B68), onBadge: .white
        ),
        // 2 — warm gold
        StoryCoverPalette(
            gradient: [Color(hex: 0xFFF4D8), Color(hex: 0xFFE3A8)],
            ink: Color(hex: 0x5A4000), subtitle: Color(hex: 0x7A5800),
            badge: Color(hex: 0x8A6100), onBadge: .white
        ),
        // 3 — leaf green
        StoryCoverPalette(
            gradient: [Color(hex: 0xE1F3DC), Color(hex: 0xC2E6BC)],
            ink: Color(hex: 0x1E3D1E), subtitle: Color(hex: 0x2E5A2E),
            badge: Color(hex: 0x3B7A3B), onBadge: .white
        ),
        // 4 — soft sky
        StoryCoverPalette(
            gradient: [Color(hex: 0xDCEEFF), Color(hex: 0xB9DCF7)],
            ink: Color(hex: 0x0F2E48), subtitle: Color(hex: 0x1E4E78),
            badge: Color(hex: 0x2B6CA3), onBadge: .white
        ),
        // 5 — rice paper / ink
        StoryCoverPalette(
            gradient: [Color(hex: 0xFFF8EC), Color(hex: 0xF1E4C9)],
            ink: Color(hex: 0x202523), subtitle: Color(hex: 0x4F5E58),
            badge: Color(hex: 0x8A6100), onBadge: .white
        ),
    ]
}

private struct StoryCover: View {
    let story: Story
    let size: CGFloat

    private static let useCases = StoryCoverUseCases()

    private var theme: StoryCoverTheme {
        Self.useCases.coverThemeFor(story: story, paletteCount: Int32(StoryCoverPalettes.all.count))
    }

    private var isHero: Bool { size >= 148 }

    var body: some View {
        let t = theme
        let idx = max(0, min(Int(t.paletteIndex), StoryCoverPalettes.all.count - 1))
        let palette = StoryCoverPalettes.all[idx]
        let pad: CGFloat = isHero ? LMCSpace.s3 : LMCSpace.s2
        let motifSize: CGFloat = isHero ? 40 : 26
        let titleSize: CGFloat = isHero ? 22 : 15
        let subtitleSize: CGFloat = isHero ? 12 : 10
        let badgeSize: CGFloat = isHero ? 12 : 10

        RoundedRectangle(cornerRadius: 8, style: .continuous)
            .fill(
                LinearGradient(
                    colors: palette.gradient,
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay(alignment: .topLeading) {
                Text(LMCStrings.format("level_value", Int(t.level)))
                    .font(.system(size: badgeSize, weight: .bold))
                    .foregroundStyle(palette.onBadge)
                    .padding(.horizontal, LMCSpace.s2)
                    .padding(.vertical, 2)
                    .background(palette.badge)
                    .clipShape(Capsule())
                    .padding(pad)
            }
            .overlay(alignment: .topTrailing) {
                Text(t.motif)
                    .font(.system(size: motifSize))
                    .padding(pad)
                    .accessibilityHidden(true)
            }
            .overlay(alignment: .bottomLeading) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(story.titleZh)
                        .font(.system(size: titleSize, weight: .bold))
                        .foregroundStyle(palette.ink)
                        .lineLimit(isHero ? 3 : 2)
                        .multilineTextAlignment(.leading)
                    if isHero {
                        Text(story.titleEn)
                            .font(.system(size: subtitleSize, weight: .semibold))
                            .foregroundStyle(palette.subtitle)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(pad)
            }
            .frame(width: size, height: size)
            .overlay(
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(LMCColor.outlineVariant, lineWidth: 1)
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(story.titleZh), \(story.titleEn)")
    }
}

private struct SummaryTile: View {
    let icon: String
    let titleKey: LocalizedStringKey
    let value: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                Image(systemName: icon)
                    .font(.system(size: 24, weight: .bold))
                    .foregroundStyle(LMCColor.secondary)
                    .accessibilityHidden(true)
                Text(titleKey)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(value)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(LMCColor.tertiary)
            }
            .frame(maxWidth: .infinity, minHeight: 112, alignment: .leading)
            .padding(LMCSpace.s4)
            .background(LMCColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct ReadingTopBar: View {
    let story: Story
    let countText: String
    let openSettings: () -> Void
    let close: () -> Void

    var body: some View {
        HStack(spacing: LMCSpace.s2) {
            IconButton(systemName: "xmark", labelKey: "action_close", action: close)
            Text(story.titleZh)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
                .lineLimit(1)
            Spacer()
            Text(countText)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(LMCColor.textSecondary)
            IconButton(systemName: "gearshape", labelKey: "reading_settings_title", action: openSettings)
        }
        .padding(.horizontal, LMCSpace.s4)
        .padding(.top, LMCSpace.s4)
        .padding(.bottom, LMCSpace.s3)
        .background(LMCColor.surface)
    }
}

private struct ReadingProgressHairline: View {
    let value: Double
    let accessibilityValue: String

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(LMCColor.outlineVariant)
                Capsule()
                    .fill(progressTint)
                    .frame(width: max(0, min(proxy.size.width, proxy.size.width * clampedProgress)))
            }
        }
        .frame(height: 2)
        .accessibilityValue(Text(accessibilityValue))
        .accessibilityAddTraits(.updatesFrequently)
    }

    private var progressTint: Color {
        value >= 1 ? LMCColor.success : LMCColor.primary
    }

    private var clampedProgress: Double {
        min(max(value, 0), 1)
    }
}

private struct ReadingSettingsSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var showPinyin: Bool
    @Binding var readingSize: String
    let readingModeBinding: Binding<LMCReadingPlaybackMode>
    let autoContinueBinding: Binding<Bool>
    let playbackSpeed: LMCReadingPlaybackSpeed
    let setPlaybackSpeed: (LMCReadingPlaybackSpeed) -> Void

    private var isTapToListen: Bool {
        readingModeBinding.wrappedValue == .tapToListen
    }

    var body: some View {
        VStack(spacing: LMCSpace.s5) {
            HStack {
                Text("reading_settings_title")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Spacer()
                Button("action_done") {
                    dismiss()
                }
                .buttonStyle(LMCSecondaryButtonStyle())
            }
            .padding(.horizontal, LMCSpace.s4)

            VStack(spacing: LMCSpace.s3) {
                Toggle(isOn: $showPinyin) {
                    Text("reading_pinyin")
                        .font(.system(size: 18))
                        .foregroundStyle(LMCColor.textPrimary)
                }
                .tint(LMCColor.secondary)

                Divider().background(LMCColor.outlineVariant)

                HStack(alignment: .center, spacing: LMCSpace.s3) {
                    Text("reading_font_size")
                        .font(.system(size: 18))
                        .foregroundStyle(LMCColor.textPrimary)
                    Spacer(minLength: LMCSpace.s2)
                    LMCSegmentedReadingSize(readingSize: $readingSize)
                }

                Divider().background(LMCColor.outlineVariant)

                LMCPlaybackModeControl(mode: readingModeBinding)

                Divider().background(LMCColor.outlineVariant)

                Toggle(isOn: autoContinueBinding) {
                    Text("reading_auto_continue")
                        .font(.system(size: 18))
                        .foregroundStyle(LMCColor.textPrimary)
                }
                .tint(LMCColor.secondary)
                .disabled(isTapToListen)
                .opacity(isTapToListen ? 0.45 : 1)

                HStack(alignment: .center, spacing: LMCSpace.s3) {
                    Text("reading_playback_speed")
                        .font(.system(size: 18))
                        .foregroundStyle(LMCColor.textPrimary)
                    Spacer(minLength: LMCSpace.s2)

                    Menu {
                        ForEach(LMCReadingPlaybackSpeed.allCases, id: \.self) { speed in
                            Button {
                                setPlaybackSpeed(speed)
                            } label: {
                                HStack {
                                    Text(LocalizedStringKey(speed.labelKey))
                                    if speed == playbackSpeed {
                                        Spacer()
                                        Image(systemName: "checkmark")
                                    }
                                }
                            }
                        }
                    } label: {
                        HStack(spacing: LMCSpace.s2) {
                            Text(LocalizedStringKey(playbackSpeed.labelKey))
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(LMCColor.textPrimary)
                            Image(systemName: "chevron.up.chevron.down")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundStyle(LMCColor.textSecondary)
                                .accessibilityHidden(true)
                        }
                        .padding(.horizontal, LMCSpace.s3)
                        .frame(minHeight: LMCSpace.minTouch)
                        .background(LMCColor.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, LMCSpace.s4)

            Spacer(minLength: 0)
        }
        .padding(.vertical, LMCSpace.s4)
        .presentationDragIndicator(.visible)
    }
}

private struct ReadingBottomDock: View {
    let isAudioActive: Bool
    let autoFollowReading: Bool
    let location: LMCSentenceLocation?
    let canGoPreviousSentence: Bool
    let canGoNextSentence: Bool
    let playerStatusText: String
    let isReadingAudioPaused: Bool
    let readingState: ReadingSessionState
    let canGoPreviousParagraph: Bool
    let backToReadingPlace: () -> Void
    let previousSentence: () -> Void
    let togglePlayback: () -> Void
    let repeatSentence: () -> Void
    let nextSentence: () -> Void
    let stopAudio: () -> Void
    let previousParagraph: () -> Void
    let nextParagraph: () -> Void
    let readParagraph: () -> Void

    var body: some View {
        VStack(spacing: LMCSpace.s2) {
            if isAudioActive {
                if !autoFollowReading, location != nil {
                    Button("reading_back_to_reading_place") {
                        backToReadingPlace()
                    }
                    .buttonStyle(LMCSecondaryButtonStyle())
                    .frame(maxWidth: .infinity)
                }

                ReadingPlayerBar(
                    canGoPreviousSentence: canGoPreviousSentence,
                    canGoNextSentence: canGoNextSentence,
                    isReadingAudioPaused: isReadingAudioPaused,
                    playerStatusText: playerStatusText,
                    previousSentence: previousSentence,
                    togglePlayback: togglePlayback,
                    repeatSentence: repeatSentence,
                    nextSentence: nextSentence,
                    stopAudio: stopAudio
                )
            } else {
                HStack(spacing: LMCSpace.s2) {
                    IconButton(systemName: "chevron.left", labelKey: "action_previous", action: previousParagraph)
                        .disabled(!canGoPreviousParagraph)

                    Button {
                        readParagraph()
                    } label: {
                        HStack(spacing: LMCSpace.s2) {
                            Image(systemName: "play.fill")
                                .font(.system(size: 18, weight: .bold))
                            Text("reading_read_all")
                                .font(.system(size: 16, weight: .bold))
                        }
                    }
                    .buttonStyle(LMCPrimaryButtonStyle())
                    .frame(maxWidth: .infinity)

                    IconButton(
                        systemName: "chevron.right",
                        labelKey: readingState.isLastParagraph ? "action_new_words" : "action_next",
                        action: nextParagraph
                    )
                }
            }
        }
        .padding(LMCSpace.s3)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(LMCColor.outlineVariant)
                .frame(height: 1)
        }
    }
}

private struct ReadingPlayerBar: View {
    let canGoPreviousSentence: Bool
    let canGoNextSentence: Bool
    let isReadingAudioPaused: Bool
    let playerStatusText: String
    let previousSentence: () -> Void
    let togglePlayback: () -> Void
    let repeatSentence: () -> Void
    let nextSentence: () -> Void
    let stopAudio: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            if !playerStatusText.isEmpty {
                Text(playerStatusText)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(LMCColor.textSecondary)
                    .lineLimit(1)
            }

            HStack(spacing: LMCSpace.s2) {
                ReadingTransportButton(
                    icon: "backward.end.fill",
                    labelKey: "reading_previous_sentence",
                    isEnabled: canGoPreviousSentence,
                    action: previousSentence
                )

                ReadingTransportButton(
                    icon: isReadingAudioPaused ? "play.fill" : "pause.fill",
                    labelKey: isReadingAudioPaused ? "reading_resume_audio" : "reading_pause_audio",
                    isEnabled: true,
                    isPrimary: true,
                    action: togglePlayback
                )

                ReadingTransportButton(
                    icon: "repeat",
                    labelKey: "reading_repeat_sentence",
                    isEnabled: true,
                    action: repeatSentence
                )

                ReadingTransportButton(
                    icon: "forward.end.fill",
                    labelKey: "reading_next_sentence",
                    isEnabled: canGoNextSentence,
                    action: nextSentence
                )

                ReadingTransportButton(
                    icon: "stop.circle.fill",
                    labelKey: "reading_stop_audio",
                    isEnabled: true,
                    action: stopAudio
                )
            }
        }
        .padding(LMCSpace.s3)
        .padding(.top, LMCSpace.s1)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(alignment: .top) {
            Rectangle()
                .fill(LMCColor.outlineVariant)
                .frame(height: 1)
        }
    }
}

private struct ReadingTransportButton: View {
        let icon: String
        let labelKey: LocalizedStringKey
        let isEnabled: Bool
        let isPrimary: Bool
        let action: () -> Void

    init(icon: String, labelKey: LocalizedStringKey, isEnabled: Bool, isPrimary: Bool = false, action: @escaping () -> Void) {
        self.icon = icon
        self.labelKey = labelKey
        self.isEnabled = isEnabled
        self.isPrimary = isPrimary
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: isPrimary ? 22 : 20, weight: .bold))
                .frame(width: isPrimary ? 56 : LMCSpace.minTouch, height: isPrimary ? 56 : LMCSpace.minTouch)
                .background(isPrimary ? LMCColor.primary : LMCColor.secondaryContainer)
                .foregroundStyle(isPrimary ? LMCColor.onPrimary : LMCColor.secondary)
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .opacity(isEnabled ? 1 : 0.35)
        .accessibilityLabel(Text(labelKey))
    }
}

private struct LMCPlaybackModeControl: View {
    @Binding var mode: LMCReadingPlaybackMode

    var body: some View {
        HStack(spacing: LMCSpace.s1) {
            playbackModeButton(.readAlong, labelKey: "reading_mode_read_along", systemName: "speaker.wave.2.fill")
            playbackModeButton(.tapToListen, labelKey: "reading_mode_tap_to_listen", systemName: "hand.tap.fill")
        }
        .padding(LMCSpace.s1)
        .background(LMCColor.outlineVariant)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private func playbackModeButton(
        _ candidate: LMCReadingPlaybackMode,
        labelKey: LocalizedStringKey,
        systemName: String
    ) -> some View {
        let selected = mode == candidate
        return Button {
            mode = candidate
        } label: {
            Label(labelKey, systemImage: systemName)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(selected ? LMCColor.onPrimary : LMCColor.textPrimary)
                .frame(maxWidth: .infinity, minHeight: 44)
                .background(selected ? LMCColor.primary : LMCColor.surface)
                .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct ReadingAudioCoachmark: View {
    let dismiss: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            Label("reading_coachmark_play", systemImage: "speaker.wave.2.fill")
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
            Label("reading_coachmark_sentence", systemImage: "hand.tap.fill")
                .font(.system(size: 15))
                .foregroundStyle(LMCColor.textSecondary)
            Button("action_done", action: dismiss)
                .buttonStyle(LMCSecondaryButtonStyle())
        }
        .padding(LMCSpace.s3)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.secondaryContainer)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct ReadingParagraphView: View {
    let storyId: String
    let paragraphIndex: Int
    let paragraph: Paragraph
    let size: LMCReadingSize
    let showPinyin: Bool
    let isCurrentParagraph: Bool
    let activeSentenceIndex: Int?
    let activeCharIndex: Int?
    let playSentence: (Int) -> Void
    let playSentenceOnly: (Int) -> Void
    let isRecordingSentence: (Int) -> Bool
    let startSentenceRecording: (Int) -> Void
    let stopSentenceRecording: (Int) -> Void
    var onWordTap: ((String) -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            ForEach(paragraph.lmcSentenceSegments) { sentence in
                    let isActiveSentence = activeSentenceIndex == sentence.index
                    ReadingSentenceView(
                    sentence: sentence,
                    size: size,
                    showPinyin: showPinyin,
                    isActive: isActiveSentence,
                    activeCharIndex: isActiveSentence ? activeCharIndex : nil,
                    playSentence: { playSentence(sentence.index) },
                    playSentenceOnly: { playSentenceOnly(sentence.index) },
                    isRecording: isRecordingSentence(sentence.index),
                    startRecording: { startSentenceRecording(sentence.index) },
                    stopRecording: { stopSentenceRecording(sentence.index) },
                    onWordTap: onWordTap
                )
                .id(LMCSentenceLocation(
                    storyId: storyId,
                    paragraphIndex: paragraphIndex,
                    sentenceIndex: sentence.index
                ).scrollId)
            }
        }
            .padding(LMCSpace.s3)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(isCurrentParagraph ? LMCColor.surface.opacity(0.8) : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(paragraph.text))
    }
}

private struct ReadingSentenceView: View {
    let sentence: LMCSentenceSegment
    let size: LMCReadingSize
    let showPinyin: Bool
    let isActive: Bool
    var activeCharIndex: Int? = nil
    let playSentence: () -> Void
    let playSentenceOnly: () -> Void
    let isRecording: Bool
    let startRecording: () -> Void
    let stopRecording: () -> Void
    var onWordTap: ((String) -> Void)? = nil

    @ViewBuilder
    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: LMCSpace.s2) {
            Button(action: playSentenceOnly) {
                Image(systemName: "speaker.wave.2.fill")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(isActive ? LMCColor.onSecondaryContainer : LMCColor.secondary)
                    .frame(width: LMCSpace.minTouch, height: LMCSpace.minTouch)
                    .background(isActive ? LMCColor.secondary : LMCColor.secondaryContainer)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("reading_sentence_play"))

            Button(action: isRecording ? stopRecording : startRecording) {
                Image(systemName: isRecording ? "stop.circle.fill" : "mic.fill")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(isRecording ? LMCColor.onErrorContainer : LMCColor.textSecondary)
                    .frame(width: LMCSpace.minTouch, height: LMCSpace.minTouch)
                    .background(isRecording ? LMCColor.error : LMCColor.surfaceVariant)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(isRecording ? "reading_sentence_stop_recording" : "reading_sentence_record"))

            sentenceContent
                // Sentence tap plays audio; per-character word taps run as a higher
                // priority gesture (see LMCRubyCellView / character cells) so they win.
                .onTapGesture(perform: playSentence)
                .accessibilityAddTraits(.isButton)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private var sentenceContent: some View {
        if showPinyin {
            LMCRubyTextFlow(cells: sentence.cells, size: size, activeCharIndex: activeCharIndex, onWordTap: onWordTap)
                .padding(.horizontal, LMCSpace.s2)
                .padding(.vertical, LMCSpace.s1)
                .background(sentenceBackground)
                .overlay(sentenceBorder)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .frame(maxWidth: .infinity, alignment: .leading)
        } else {
            tappableHanziText
                .padding(.horizontal, LMCSpace.s3)
                .padding(.vertical, LMCSpace.s2)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(sentenceBackground)
                .overlay(sentenceBorder)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }

    // Sentence hanzi without ruby: each character is its own tap target (word lookup),
    // while preserving the karaoke active-character highlight.
    @ViewBuilder
    private var tappableHanziText: some View {
        let codePoints = Array(sentence.text.unicodeScalars).map { String($0) }
        LMCRubyFlowLayout(horizontalSpacing: 0, verticalSpacing: LMCSpace.s2) {
            ForEach(Array(codePoints.enumerated()), id: \.offset) { index, scalar in
                let isActiveChar = activeCharIndex == index
                LMCTappableHanziCharacter(
                    character: scalar,
                    font: size.hanziFont,
                    color: isActiveChar ? LMCColor.primary : LMCColor.textPrimary,
                    isBold: isActiveChar,
                    onTap: onWordTap
                )
            }
        }
    }

    private var sentenceBackground: Color {
        isActive ? LMCColor.primaryContainer : Color.clear
    }

    private var sentenceBorder: some View {
        RoundedRectangle(cornerRadius: 10, style: .continuous)
            .stroke(isActive ? LMCColor.primary.opacity(0.35) : Color.clear, lineWidth: 1)
    }
}

private struct LMCRubyCellData: Equatable {
    let character: String
    let pinyin: String
}

private struct LMCSentenceSegment: Identifiable, Equatable {
    let index: Int
    let text: String
    let cells: [LMCRubyCellData]

    var id: Int {
        index
    }
}

private struct LMCSentenceTextSegment {
    let index: Int
    let text: String
    let characterRange: Range<Int>
}

private extension Paragraph {
    var lmcRubyCells: [LMCRubyCellData] {
        if cells.isEmpty {
            return text.map { character in
                LMCRubyCellData(character: String(character), pinyin: "")
            }
        }

        return cells.map { cell in
            LMCRubyCellData(character: cell.c, pinyin: cell.p)
        }
    }

    var lmcSentenceSegments: [LMCSentenceSegment] {
        let textSegments = LMCSentenceSplitter.split(text)
        let rubyCells = lmcRubyCells
        let textCharacterCount = text.count

        return textSegments.map { segment in
            let sentenceCells: [LMCRubyCellData]
            if rubyCells.count == textCharacterCount {
                sentenceCells = Array(rubyCells[segment.characterRange])
            } else {
                sentenceCells = segment.text.map { character in
                    LMCRubyCellData(character: String(character), pinyin: "")
                }
            }
            return LMCSentenceSegment(index: segment.index, text: segment.text, cells: sentenceCells)
        }
    }
}

private extension Story {
    func lmcSentence(at location: LMCSentenceLocation) -> LMCSentenceSegment? {
        guard location.storyId == id, paragraphs.indices.contains(location.paragraphIndex) else { return nil }
        return paragraphs[location.paragraphIndex].lmcSentenceSegments.first { $0.index == location.sentenceIndex }
    }

    func lmcNormalizedSentenceLocation(paragraphIndex: Int, sentenceIndex: Int) -> LMCSentenceLocation? {
        guard !paragraphs.isEmpty else { return nil }
        let clampedParagraphIndex = max(0, min(paragraphIndex, paragraphs.count - 1))

        for candidateParagraphIndex in clampedParagraphIndex..<paragraphs.count {
            let segments = paragraphs[candidateParagraphIndex].lmcSentenceSegments
            guard !segments.isEmpty else { continue }
            let candidateSentenceIndex = candidateParagraphIndex == clampedParagraphIndex
                ? max(0, min(sentenceIndex, segments.count - 1))
                : 0
            return LMCSentenceLocation(
                storyId: id,
                paragraphIndex: candidateParagraphIndex,
                sentenceIndex: candidateSentenceIndex
            )
        }

        return nil
    }

    func lmcNextSentenceLocation(after location: LMCSentenceLocation) -> LMCSentenceLocation? {
        guard location.storyId == id, paragraphs.indices.contains(location.paragraphIndex) else { return nil }

        for candidateParagraphIndex in location.paragraphIndex..<paragraphs.count {
            let segments = paragraphs[candidateParagraphIndex].lmcSentenceSegments
            guard !segments.isEmpty else { continue }
            let firstCandidateSentenceIndex = candidateParagraphIndex == location.paragraphIndex
                ? location.sentenceIndex + 1
                : 0
            if segments.indices.contains(firstCandidateSentenceIndex) {
                return LMCSentenceLocation(
                    storyId: id,
                    paragraphIndex: candidateParagraphIndex,
                    sentenceIndex: firstCandidateSentenceIndex
                )
            }
        }

        return nil
    }

    func lmcPreviousSentenceLocation(before location: LMCSentenceLocation) -> LMCSentenceLocation? {
        guard location.storyId == id, paragraphs.indices.contains(location.paragraphIndex) else { return nil }

        for candidateParagraphIndex in stride(from: location.paragraphIndex, through: 0, by: -1) {
            let segments = paragraphs[candidateParagraphIndex].lmcSentenceSegments
            guard !segments.isEmpty else { continue }
            let candidateSentenceIndex = candidateParagraphIndex == location.paragraphIndex
                ? location.sentenceIndex - 1
                : segments.count - 1
            if segments.indices.contains(candidateSentenceIndex) {
                return LMCSentenceLocation(
                    storyId: id,
                    paragraphIndex: candidateParagraphIndex,
                    sentenceIndex: candidateSentenceIndex
                )
            }
        }

        return nil
    }
}

private enum LMCSentenceSplitter {
    // TODO(shared API): replace this local splitter with the shared sentence plan when it lands.
    private static let sentenceEndPunctuation: Set<Character> = ["。", "！", "？", "；", "…"]
    private static let sentenceTrailingCharacters: Set<Character> = ["”", "’", "」", "』", "》", "〉", "）", ")"]

    static func split(_ text: String) -> [LMCSentenceTextSegment] {
        let characters = Array(text)
        guard !characters.isEmpty else { return [] }

        var segments: [LMCSentenceTextSegment] = []
        var start = 0
        var index = 0

        while index < characters.count {
            if sentenceEndPunctuation.contains(characters[index]) {
                var end = index + 1
                while end < characters.count,
                      sentenceEndPunctuation.contains(characters[end]) || sentenceTrailingCharacters.contains(characters[end]) {
                    end += 1
                }
                appendSegment(characters: characters, range: start..<end, to: &segments)
                start = end
                while start < characters.count, characters[start].lmcIsWhitespace {
                    start += 1
                }
                index = start
            } else {
                index += 1
            }
        }

        appendSegment(characters: characters, range: start..<characters.count, to: &segments)
        return segments
    }

    private static func appendSegment(
        characters: [Character],
        range: Range<Int>,
        to segments: inout [LMCSentenceTextSegment]
    ) {
        var lowerBound = range.lowerBound
        var upperBound = range.upperBound

        while lowerBound < upperBound, characters[lowerBound].lmcIsWhitespace {
            lowerBound += 1
        }
        while upperBound > lowerBound, characters[upperBound - 1].lmcIsWhitespace {
            upperBound -= 1
        }
        guard lowerBound < upperBound else { return }

        let text = characters[lowerBound..<upperBound].map(String.init).joined()
        segments.append(
            LMCSentenceTextSegment(
                index: segments.count,
                text: text,
                characterRange: lowerBound..<upperBound
            )
        )
    }
}

private enum LMCSentenceAudioResolver {
    // TODO(shared API): replace Bundle manifest lookup with a shared audio resource resolver when it lands.
    static func generatedAudioURL(storyId: String, location: LMCSentenceLocation) -> URL? {
        if let manifestURL = Bundle.main.url(forResource: "audio", withExtension: "json", subdirectory: "stories/\(storyId)"),
           let data = try? Data(contentsOf: manifestURL),
           let manifest = try? JSONDecoder().decode(LMCAudioManifest.self, from: data),
           let sentence = manifest.sentences.first(where: {
               $0.paraIndex == location.paragraphIndex && $0.sentIndex == location.sentenceIndex && $0.unavailable != true
           }),
           let manifestAudioURL = url(storyId: storyId, audioPath: sentence.audioPath) {
            return manifestAudioURL
        }

        return Bundle.main.url(
            forResource: "p\(location.paragraphIndex + 1)_s\(location.sentenceIndex + 1)",
            withExtension: "wav",
            subdirectory: "stories/\(storyId)/audio"
        )
    }

    private static func url(storyId: String, audioPath: String) -> URL? {
        let path = NSString(string: audioPath)
        let directory = path.deletingLastPathComponent
        let resource = NSString(string: path.lastPathComponent).deletingPathExtension
        let ext = path.pathExtension
        guard !resource.isEmpty, !ext.isEmpty else { return nil }
        let subdirectory = directory.isEmpty || directory == "."
            ? "stories/\(storyId)"
            : "stories/\(storyId)/\(directory)"
        return Bundle.main.url(
            forResource: resource,
            withExtension: ext,
            subdirectory: subdirectory
        )
    }
}

private struct LMCAudioManifest: Decodable {
    let sentences: [LMCAudioManifestSentence]
}

private struct LMCAudioManifestSentence: Decodable {
    let paraIndex: Int
    let sentIndex: Int
    let audioPath: String
    let unavailable: Bool?
}

private enum LMCTtsDurationEstimator {
    static func nanoseconds(for text: String, speedMultiplier: Double = 1.0) -> UInt64 {
        let characterCount = max(4, text.count)
        let seconds = min(14.0, max(1.4, Double(characterCount) * 0.28 + 0.6))
        return UInt64((seconds / max(0.5, speedMultiplier)) * 1_000_000_000)
    }
}

private final class LMCGeneratedAudioDelegate: NSObject, AVAudioPlayerDelegate {
    private let finish: () -> Void

    init(finish: @escaping () -> Void) {
        self.finish = finish
    }

    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        finish()
    }

    func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) {
        finish()
    }
}

private extension Character {
    var lmcIsWhitespace: Bool {
        unicodeScalars.allSatisfy { CharacterSet.whitespacesAndNewlines.contains($0) }
    }
}

private struct LMCRubyTextFlow: View {
    let cells: [LMCRubyCellData]
    let size: LMCReadingSize
    var activeCharIndex: Int? = nil
    var onWordTap: ((String) -> Void)? = nil

    var body: some View {
        LMCRubyFlowLayout(horizontalSpacing: LMCSpace.s1, verticalSpacing: LMCSpace.s2) {
            ForEach(Array(cells.enumerated()), id: \.offset) { index, cell in
                // Ruby cells are 1:1 with sentence code points, so the cell index
                // matches the karaoke character index from shared.
                LMCRubyCellView(cell: cell, size: size, isActiveChar: activeCharIndex == index, onTap: onWordTap)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct LMCRubyCellView: View {
    let cell: LMCRubyCellData
    let size: LMCReadingSize
    var isActiveChar: Bool = false
    var onTap: ((String) -> Void)? = nil

    @ViewBuilder
    var body: some View {
        if let onTap, !cell.character.isEmpty {
            cellContent
                .contentShape(Rectangle())
                .frame(minWidth: LMCSpace.minTouch, minHeight: LMCSpace.minTouch)
                // Per-character tap opens the word lookup. It is a higher-priority gesture
                // than the sentence tap, so tapping a word never starts sentence audio.
                .highPriorityGesture(TapGesture().onEnded { onTap(cell.character) })
        } else {
            cellContent
        }
    }

    private var cellContent: some View {
        VStack(spacing: 0) {
            Text(cell.pinyin.isEmpty ? " " : cell.pinyin)
                .font(size.pinyinFont)
                .fontWeight(isActiveChar ? .bold : nil)
                .foregroundStyle(isActiveChar ? LMCColor.onPrimaryContainer : LMCColor.textSecondary)
                .lineLimit(1)
                .multilineTextAlignment(.center)
                .opacity(cell.pinyin.isEmpty ? 0 : 1)
                .frame(height: size.pinyinLineHeight, alignment: .bottom)

            Text(cell.character)
                .font(size.hanziFont)
                .fontWeight(isActiveChar ? .bold : nil)
                .foregroundStyle(isActiveChar ? LMCColor.onPrimaryContainer : LMCColor.textPrimary)
                .lineLimit(1)
                .multilineTextAlignment(.center)
                .frame(height: size.hanziLineHeight, alignment: .top)
        }
        .fixedSize(horizontal: true, vertical: true)
        .padding(.horizontal, isActiveChar ? 2 : 0)
        .background(
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(isActiveChar ? LMCColor.primaryContainer : Color.clear)
        )
    }
}

// A single hanzi character (no ruby) that is its own >=48dp tap target for word lookup.
private struct LMCTappableHanziCharacter: View {
    let character: String
    let font: Font
    let color: Color
    let isBold: Bool
    var onTap: ((String) -> Void)? = nil

    @ViewBuilder
    var body: some View {
        let text = Text(character)
            .font(font)
            .fontWeight(isBold ? .bold : nil)
            .foregroundColor(color)
            .fixedSize(horizontal: true, vertical: true)
        if let onTap, !character.isEmpty {
            text
                .frame(minWidth: LMCSpace.minTouch, minHeight: LMCSpace.minTouch)
                .contentShape(Rectangle())
                .highPriorityGesture(TapGesture().onEnded { onTap(character) })
        } else {
            text
        }
    }
}

private struct LMCRubyFlowLayout: Layout {
    let horizontalSpacing: CGFloat
    let verticalSpacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = rows(for: subviews, proposalWidth: proposal.width)
        let width = proposal.width ?? rows.map(\.width).max() ?? 0
        let height = rows.enumerated().reduce(CGFloat.zero) { result, row in
            result + row.element.height + (row.offset == rows.count - 1 ? 0 : verticalSpacing)
        }
        return CGSize(width: width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let rows = rows(for: subviews, proposalWidth: bounds.width)
        var y = bounds.minY

        for row in rows {
            var x = bounds.minX
            for item in row.items {
                subviews[item.index].place(
                    at: CGPoint(x: x, y: y),
                    anchor: .topLeading,
                    proposal: ProposedViewSize(item.size)
                )
                x += item.size.width + horizontalSpacing
            }
            y += row.height + verticalSpacing
        }
    }

    private func rows(for subviews: Subviews, proposalWidth: CGFloat?) -> [LMCRubyFlowRow] {
        let availableWidth = proposalWidth ?? .greatestFiniteMagnitude
        let itemProposal = ProposedViewSize(width: nil, height: nil)
        var rows: [LMCRubyFlowRow] = []
        var currentItems: [LMCRubyFlowRow.Item] = []
        var currentWidth = CGFloat.zero
        var currentHeight = CGFloat.zero

        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(itemProposal)
            let nextWidth = currentItems.isEmpty ? size.width : currentWidth + horizontalSpacing + size.width

            if !currentItems.isEmpty, nextWidth > availableWidth {
                rows.append(LMCRubyFlowRow(items: currentItems, width: currentWidth, height: currentHeight))
                currentItems = [LMCRubyFlowRow.Item(index: index, size: size)]
                currentWidth = size.width
                currentHeight = size.height
            } else {
                currentItems.append(LMCRubyFlowRow.Item(index: index, size: size))
                currentWidth = nextWidth
                currentHeight = max(currentHeight, size.height)
            }
        }

        if !currentItems.isEmpty {
            rows.append(LMCRubyFlowRow(items: currentItems, width: currentWidth, height: currentHeight))
        }

        return rows
    }
}

private struct LMCRubyFlowRow {
    struct Item {
        let index: Int
        let size: CGSize
    }

    let items: [Item]
    let width: CGFloat
    let height: CGFloat
}

private struct VocabularyCard: View {
    let word: Vocab
    let playAudio: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s4) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: LMCSpace.s2) {
                    Text(word.word)
                        .font(.system(size: 32, weight: .bold))
                        .foregroundStyle(LMCColor.textPrimary)
                    Text(word.pinyin)
                        .font(.system(size: 18, weight: .semibold, design: .rounded))
                        .foregroundStyle(LMCColor.secondary)
                }
                Spacer()
                IconButton(systemName: "speaker.wave.2.fill", labelKey: "reading_audio", action: playAudio)
            }

            Text(word.meaning)
                .font(.system(size: 20))
                .foregroundStyle(LMCColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)

            if let example = word.example, !example.isEmpty {
                Text(example)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundStyle(LMCColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(LMCSpace.s3)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(LMCColor.surfaceVariant)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 1)
    }
}

private struct WordBookRow: View {
    let item: WordBookItem
    let sourceTitle: String?
    let playAudio: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: LMCSpace.s3) {
            VStack(alignment: .leading, spacing: LMCSpace.s1) {
                HStack(spacing: LMCSpace.s2) {
                    Text(item.word)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(LMCColor.textPrimary)
                    WordStatusChip(item: item)
                }
                Text(item.pinyin)
                    .font(.system(size: 16, weight: .semibold, design: .rounded))
                    .foregroundStyle(LMCColor.secondary)
                Text(item.meaning)
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                if let sourceTitle {
                    Text(LMCStrings.format("word_book_source_story", sourceTitle))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(LMCColor.textSecondary)
                }
            }
            Spacer()
            IconButton(systemName: "speaker.wave.2.fill", labelKey: "reading_audio", action: playAudio)
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 1)
    }
}

private struct WordStatusChip: View {
    let item: WordBookItem

    var body: some View {
        Text(titleKey)
            .font(.system(size: 12, weight: .bold))
            .foregroundStyle(foreground)
            .padding(.horizontal, LMCSpace.s2)
            .padding(.vertical, LMCSpace.s1)
            .background(background)
            .clipShape(Capsule())
    }

    private var titleKey: LocalizedStringKey {
        if item.due { return "word_book_status_due" }
        if item.reps >= 2 { return "word_book_status_known" }
        return "word_book_status_learning"
    }

    private var foreground: Color {
        if item.due { return LMCColor.primary }
        if item.reps >= 2 { return LMCColor.success }
        return LMCColor.tertiary
    }

    private var background: Color {
        if item.due { return LMCColor.primaryContainer }
        if item.reps >= 2 { return LMCColor.successContainer }
        return LMCColor.tertiaryContainer
    }
}

private struct QuizOptionRow: View {
    let option: String
    let isSelected: Bool
    let isSubmitted: Bool
    let isCorrectAnswer: Bool
    let isSubmittedAnswer: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: LMCSpace.s3) {
                Image(systemName: iconName)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(iconColor)
                    .accessibilityHidden(true)
                Text(option)
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, LMCSpace.s4)
            .padding(.vertical, LMCSpace.s3)
            .frame(maxWidth: .infinity, minHeight: 56, alignment: .leading)
            .background(background)
            .overlay(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .stroke(borderColor, lineWidth: isSelected || isSubmitted ? 2 : 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
    }

    private var iconName: String {
        if isSubmitted && isCorrectAnswer { return "checkmark.circle.fill" }
        if isSubmitted && isSubmittedAnswer && !isCorrectAnswer { return "book.circle.fill" }
        return isSelected ? "largecircle.fill.circle" : "circle"
    }

    private var iconColor: Color {
        if isSubmitted && isCorrectAnswer { return LMCColor.success }
        if isSubmitted && isSubmittedAnswer && !isCorrectAnswer { return LMCColor.info }
        return isSelected ? LMCColor.secondary : LMCColor.outline
    }

    private var background: Color {
        if isSubmitted && isCorrectAnswer { return LMCColor.successContainer }
        if isSubmitted && isSubmittedAnswer && !isCorrectAnswer { return LMCColor.infoContainer }
        if isSelected { return LMCColor.secondaryContainer }
        return LMCColor.surface
    }

    private var borderColor: Color {
        if isSubmitted && isCorrectAnswer { return LMCColor.success }
        if isSubmitted && isSubmittedAnswer && !isCorrectAnswer { return LMCColor.info }
        if isSelected { return LMCColor.secondary }
        return LMCColor.outline
    }
}

private struct FeedbackMessage: View {
    let isCorrect: Bool
    let explanation: String

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            Label(
                LocalizedStringKey(isCorrect ? "quiz_correct" : "quiz_review_story"),
                systemImage: isCorrect ? "checkmark.circle.fill" : "book.circle.fill"
            )
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(isCorrect ? LMCColor.success : LMCColor.info)
            Text(explanation)
                .font(.system(size: 18))
                .foregroundStyle(LMCColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(isCorrect ? LMCColor.successContainer : LMCColor.infoContainer)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct MetricTile: View {
    let titleKey: LocalizedStringKey
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            Text(titleKey)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Text(value)
                .font(.system(size: 28, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
        }
        .frame(maxWidth: .infinity, minHeight: 96, alignment: .leading)
        .padding(LMCSpace.s4)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct StoryProgressRow: View {
    let story: Story
    let progress: Double
    let statusKey: String

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            HStack(alignment: .firstTextBaseline) {
                Text(story.titleZh)
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Spacer()
                Text(LocalizedStringKey(statusKey))
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(LMCColor.textSecondary)
            }
            LMCProgressBar(value: progress)
        }
        .padding(.vertical, LMCSpace.s2)
    }
}

private struct SettingsSection<Content: View>: View {
    let titleKey: LocalizedStringKey
    let content: Content

    init(titleKey: LocalizedStringKey, @ViewBuilder content: () -> Content) {
        self.titleKey = titleKey
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            Text(titleKey)
                .font(.system(size: 18, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                content
            }
            .padding(LMCSpace.s4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(LMCColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }
}

private struct SettingsChoiceRow: View {
    let titleKey: LocalizedStringKey
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(titleKey)
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(LMCColor.secondary)
                        .accessibilityHidden(true)
                }
            }
            .frame(minHeight: LMCSpace.minTouch)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? AccessibilityTraits.isSelected : AccessibilityTraits())
    }
}

private struct SettingsInfoRow: View {
    let titleKey: LocalizedStringKey
    let valueKey: LocalizedStringKey

    var body: some View {
        HStack {
            Text(titleKey)
                .font(.system(size: 18))
                .foregroundStyle(LMCColor.textPrimary)
            Spacer()
            Text(valueKey)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(LMCColor.textSecondary)
        }
        .frame(minHeight: LMCSpace.minTouch)
    }
}

private struct SettingsNavigationRow: View {
    let titleKey: LocalizedStringKey
    let systemName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: LMCSpace.s3) {
                Image(systemName: systemName)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(LMCColor.secondary)
                    .accessibilityHidden(true)
                Text(titleKey)
                    .font(.system(size: 18))
                    .foregroundStyle(LMCColor.textPrimary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(LMCColor.textSecondary)
                    .accessibilityHidden(true)
            }
            .frame(minHeight: LMCSpace.minTouch)
        }
        .buttonStyle(.plain)
    }
}

private struct LMCBottomNavigation: View {
    @Binding var selectedTab: LMCTab
    let selectTab: (LMCTab) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(LMCTab.allCases, id: \.rawValue) { tab in
                Button {
                    selectTab(tab)
                } label: {
                    VStack(spacing: LMCSpace.s1) {
                        Image(systemName: tab.icon)
                            .font(.system(size: 22, weight: .bold))
                            .accessibilityHidden(true)
                        Text(tab.titleKey)
                            .font(.system(size: 12, weight: .bold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                    .foregroundStyle(selectedTab == tab ? LMCColor.primary : LMCColor.textSecondary)
                    .frame(maxWidth: .infinity, minHeight: 64)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, LMCSpace.s2)
        .padding(.top, LMCSpace.s2)
        .padding(.bottom, LMCSpace.s2)
        .background(LMCColor.surface)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(LMCColor.outlineVariant)
                .frame(height: 1)
        }
    }
}

private struct LMCFlowTopBar: View {
    let titleKey: LocalizedStringKey
    let trailingText: String?
    let close: () -> Void

    var body: some View {
        HStack(spacing: LMCSpace.s2) {
            IconButton(systemName: "chevron.left", labelKey: "action_back", action: close)
            Text(titleKey)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
            Spacer()
            if let trailingText {
                Text(trailingText)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(LMCColor.textSecondary)
            }
        }
        .padding(.horizontal, LMCSpace.s4)
        .padding(.top, LMCSpace.s4)
        .padding(.bottom, LMCSpace.s3)
        .background(LMCColor.surface)
    }
}

private struct LMCBottomActionBar<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        HStack(spacing: LMCSpace.s3) {
            content
        }
        .padding(LMCSpace.s4)
        .background(LMCColor.surface)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(LMCColor.outlineVariant)
                .frame(height: 1)
        }
    }
}

private struct IconButton: View {
    let systemName: String
    let labelKey: LocalizedStringKey
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(LMCColor.secondary)
                .frame(width: LMCSpace.minTouch, height: LMCSpace.minTouch)
                .background(LMCColor.secondaryContainer)
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(labelKey))
    }
}

private struct LMCProgressBar: View {
    let value: Double

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule()
                    .fill(LMCColor.tertiaryContainer)
                Capsule()
                    .fill(value >= 1 ? LMCColor.success : LMCColor.primary)
                    .frame(width: max(0, min(proxy.size.width, proxy.size.width * value)))
            }
        }
        .frame(height: 8)
        .accessibilityValue(Text("\(Int(value * 100))%"))
    }
}

private struct LevelChip: View {
    let level: Int

    var body: some View {
        Text(LMCStrings.format("level_value", level))
            .font(.system(size: 14, weight: .bold))
            .foregroundStyle(LMCColor.onSecondaryContainer)
            .padding(.horizontal, LMCSpace.s3)
            .frame(minHeight: 32)
            .background(LMCColor.secondaryContainer)
            .clipShape(Capsule())
    }
}

private struct FilterChip: View {
    let title: String?
    let titleKey: LocalizedStringKey?
    let isSelected: Bool
    let action: () -> Void

    init(titleKey: LocalizedStringKey, isSelected: Bool, action: @escaping () -> Void) {
        self.title = nil
        self.titleKey = titleKey
        self.isSelected = isSelected
        self.action = action
    }

    init(title: String, isSelected: Bool, action: @escaping () -> Void) {
        self.title = title
        self.titleKey = nil
        self.isSelected = isSelected
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Group {
                if let titleKey {
                    Text(titleKey)
                } else {
                    Text(title ?? "")
                }
            }
            .font(.system(size: 15, weight: .bold))
            .foregroundStyle(isSelected ? LMCColor.onSecondaryContainer : LMCColor.textPrimary)
            .padding(.horizontal, LMCSpace.s4)
            .frame(minHeight: 40)
            .background(isSelected ? LMCColor.secondaryContainer : LMCColor.surface)
            .overlay(
                Capsule()
                    .stroke(isSelected ? LMCColor.secondary : LMCColor.outline, lineWidth: 1)
            )
            .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

private struct LMCSegmentedReadingSize: View {
    @Binding var readingSize: String

    var body: some View {
        HStack(spacing: LMCSpace.s1) {
            ForEach(LMCReadingSize.allCases, id: \.rawValue) { size in
                Button {
                    readingSize = size.rawValue
                } label: {
                    Text(size.labelKey)
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(readingSize == size.rawValue ? LMCColor.onPrimary : LMCColor.textPrimary)
                        .frame(width: 44, height: 44)
                        .background(readingSize == size.rawValue ? LMCColor.primary : LMCColor.surface)
                        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(LMCSpace.s1)
        .background(LMCColor.outlineVariant)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct StepDots: View {
    let count: Int
    let index: Int

    var body: some View {
        HStack(spacing: LMCSpace.s2) {
            ForEach(0..<max(count, 0), id: \.self) { dot in
                Circle()
                    .fill(dot == index ? LMCColor.primary : LMCColor.outlineVariant)
                    .frame(width: 10, height: 10)
            }
        }
        .frame(maxWidth: .infinity)
        .accessibilityLabel(Text(LMCStrings.format("step_count", index + 1, max(count, 1))))
    }
}

private struct LMCLoadingView: View {
    var body: some View {
        VStack(spacing: LMCSpace.s4) {
            Image("LaunchMark")
                .resizable()
                .scaledToFit()
                .frame(width: 112, height: 112)
                .accessibilityHidden(true)
            Text("app_name")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
                .multilineTextAlignment(.center)
            ProgressView()
                .tint(LMCColor.primary)
            Text("state_loading")
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textSecondary)
        }
        .frame(maxWidth: .infinity, minHeight: 200)
    }
}

private struct LMCErrorView: View {
    var body: some View {
        LMCEmptyState(titleKey: "state_error_title", messageKey: "state_error_message")
    }
}

private struct LMCEmptyState: View {
    let titleKey: LocalizedStringKey
    let messageKey: LocalizedStringKey

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            Text(titleKey)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
            Text(messageKey)
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
}

private struct LMCLibraryStateCard: View {
    let titleKey: LocalizedStringKey
    let messageKey: LocalizedStringKey
    let actionKey: LocalizedStringKey
    var showsProgress = false
    let action: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            if showsProgress {
                ProgressView()
                    .tint(LMCColor.primary)
            }
            Text(titleKey)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(LMCColor.textPrimary)
            Text(messageKey)
                .font(.system(size: 16))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
            Button(actionKey, action: action)
                .buttonStyle(LMCSecondaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .shadow(color: .black.opacity(0.08), radius: 4, x: 0, y: 1)
    }
}

private struct LMCMissingStoryView: View {
    let close: () -> Void

    var body: some View {
        VStack(spacing: LMCSpace.s5) {
            LMCEmptyState(titleKey: "state_error_title", messageKey: "state_error_message")
            Button("action_done", action: close)
                .buttonStyle(LMCPrimaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(LMCColor.background)
    }
}

private struct LMCPrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .bold))
            .foregroundStyle(isEnabled ? LMCColor.onPrimary : LMCColor.textSecondary)
            .padding(.horizontal, 20)
            .frame(minWidth: 128, minHeight: 56)
            .background(isEnabled ? (configuration.isPressed ? LMCColor.primary.opacity(0.86) : LMCColor.primary) : LMCColor.surfaceVariant)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .opacity(configuration.isPressed ? 0.9 : 1)
    }
}

private struct LMCSecondaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 16, weight: .bold))
            .foregroundStyle(isEnabled ? LMCColor.secondary : LMCColor.textSecondary)
            .padding(.horizontal, 20)
            .frame(minWidth: 104, minHeight: 48)
            .background(isEnabled ? LMCColor.surface : LMCColor.surfaceVariant)
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(LMCColor.outline, lineWidth: 1)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
            .opacity(configuration.isPressed ? 0.78 : 1)
    }
}

// MARK: - Story-driven Chinese-ability map (child + parent surfaces)

extension AbilityDimension {
    /// Localized display-name key. Mapping lives in the platform layer (no hardcoded UI text).
    var labelKey: String {
        switch self {
        case .characterrecognition: return "ability_dim_character_recognition"
        case .wordmeaning: return "ability_dim_word_meaning"
        case .sentencereading: return "ability_dim_sentence_reading"
        case .listening: return "ability_dim_listening"
        case .comprehension: return "ability_dim_comprehension"
        case .retelling: return "ability_dim_retelling"
        case .culture: return "ability_dim_culture"
        default: return "ability_dim_character_recognition"
        }
    }

    var localizedLabel: String { LMCStrings.localized(labelKey) }
}

private struct AbilityMapTodayCard: View {
    let abilityMap: AbilityMap
    let action: () -> Void

    private var practicedDimensions: Int {
        abilityMap.dimensions.filter { $0.practicedStories > 0 }.count
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: LMCSpace.s3) {
                Image(systemName: "map.fill")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(LMCColor.secondary)
                VStack(alignment: .leading, spacing: LMCSpace.s1) {
                    Text("ability_today_card_title")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(LMCColor.onSecondaryContainer)
                    Text("ability_today_card_body")
                        .font(.system(size: 15))
                        .foregroundStyle(LMCColor.onSecondaryContainer)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(LMCStrings.format("ability_progress_fraction", practicedDimensions, abilityMap.dimensions.count))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(LMCColor.onSecondaryContainer)
                }
                Spacer()
                Text("ability_today_card_action")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(LMCColor.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(LMCSpace.s4)
            .background(LMCColor.secondaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}

private struct AbilityMapScreen: View {
    let abilityMap: AbilityMap

    var body: some View {
        LMCScreen(maxWidth: LMCSpace.readingMaxWidth) {
            Text("ability_subtitle")
                .font(.system(size: 18))
                .foregroundStyle(LMCColor.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            ForEach(abilityMap.dimensions, id: \.dimension) { progress in
                AbilityDimensionCard(
                    progress: progress,
                    practicedNow: abilityMap.recentlyPracticed.contains(progress.dimension)
                )
            }

            Text("ability_encourage")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(LMCColor.tertiary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(LMCSpace.s4)
                .background(LMCColor.tertiaryContainer)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }
}

private struct AbilityDimensionCard: View {
    let progress: AbilityProgress
    let practicedNow: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            HStack {
                Text(progress.dimension.localizedLabel)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Spacer()
                Text(LMCStrings.format("ability_progress_fraction", Int(progress.practicedStories), Int(progress.totalStories)))
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(LMCColor.textSecondary)
            }
            LMCProgressBar(value: progress.masteryFraction)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(LMCSpace.s4)
        .background(practicedNow ? LMCColor.successContainer : LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(LMCColor.outlineVariant, lineWidth: 1)
        )
    }
}

private struct ParentPractisedSection: View {
    let abilityMap: AbilityMap

    private var practisedLabels: [String] {
        abilityMap.dimensions
            .map { $0.dimension }
            .filter { abilityMap.recentlyPracticed.contains($0) }
            .map { $0.localizedLabel }
    }

    private var accuracyText: String? {
        guard let accuracy = abilityMap.comprehensionAccuracy else { return nil }
        return "\(Int((accuracy.doubleValue * 100).rounded()))%"
    }

    var body: some View {
        if practisedLabels.isEmpty && accuracyText == nil {
            EmptyView()
        } else {
            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                SectionTitle("parent_practised_title")
                if !practisedLabels.isEmpty {
                    let separator = LMCStrings.localized("parent_practised_separator")
                    Text(LMCStrings.format("parent_practised_format", practisedLabels.joined(separator: separator)))
                        .font(.system(size: 18))
                        .foregroundStyle(LMCColor.onSecondaryContainer)
                        .fixedSize(horizontal: false, vertical: true)
                }
                if let accuracyText {
                    Text(LMCStrings.format("parent_comprehension_accuracy_format", accuracyText))
                        .font(.system(size: 15))
                        .foregroundStyle(LMCColor.onSecondaryContainer)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(LMCSpace.s4)
            .background(LMCColor.secondaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
    }
}

enum LMCStrings {
    static func format(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: localized(key), arguments: arguments)
    }

    static func localized(_ key: String) -> String {
        let selected = UserDefaults.standard.string(forKey: "lmc_locale_identifier") ?? LMCAppLocale.english.rawValue
        let candidates = [selected, selected.components(separatedBy: "-").first].compactMap { $0 }
        for candidate in candidates {
            if let path = Bundle.main.path(forResource: candidate, ofType: "lproj"),
               let bundle = Bundle(path: path) {
                return bundle.localizedString(forKey: key, value: nil, table: nil)
            }
        }
        return Bundle.main.localizedString(forKey: key, value: nil, table: nil)
    }
}

private extension WordBookItem {
    var asVocab: Vocab {
        Vocab(word: word, pinyin: pinyin, meaning: meaning, example: example)
    }
}

#Preview {
    ContentView()
}
