import Foundation
import SwiftUI
import shared

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var localeIdentifier = LMCAppLocale.english.rawValue
    @State private var showPinyin = true
    @State private var readingSize = LMCReadingSize.medium.rawValue
    @State private var aiBackendBaseURL = ""
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
                openSettings: { selectedTab = .settings }
            )
        case .library:
            LibraryScreen(
                viewModel: viewModel,
                openReading: { openStory($0, source: .library) }
            )
        case .parent:
            ParentReportScreen(
                viewModel: viewModel,
                entryPoint: parentReportEntryPoint,
                openSettings: { selectedTab = .settings }
            )
        case .settings:
            SettingsScreen(
                viewModel: viewModel,
                localeIdentifier: $localeIdentifier,
                showPinyin: $showPinyin,
                readingSize: $readingSize,
                aiBackendBaseURL: $aiBackendBaseURL,
                openParent: { selectTab(.parent, parentEntryPoint: .settings) }
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
                    close: { flowRoute = .reading(storyId: story.id) },
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
                    readAgain: { openStory(story, source: .quizCompletion) },
                    done: {
                        selectedTab = .today
                        flowRoute = nil
                    }
                )
            } else {
                LMCMissingStoryView(close: { flowRoute = nil })
            }
        }
    }

    private func openStory(_ story: Story, source: LMCStoryOpenSource) {
        viewModel.trackStoryOpen(story, openSource: source)
        flowRoute = .reading(storyId: story.id)
    }

    private func selectTab(_ tab: LMCTab, parentEntryPoint: LMCParentReportEntryPoint) {
        if tab == .parent {
            parentReportEntryPoint = parentEntryPoint
        }
        selectedTab = tab
    }

    private func loadSettings() async {
        guard let settings = await viewModel.readSettings() else { return }
        localeIdentifier = settings.language.tag
        showPinyin = settings.showPinyinByDefault
        readingSize = settings.readingTextSize.prefValue
        aiBackendBaseURL = settings.aiBackendBaseUrl
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

    private let repository = DefaultStoryRepository(
        resourceReader: IosStoryResourceReaderKt.defaultStoryResourceReader(),
        catalog: StoryResourceCatalog.shared.entries
    )
    private let progressService = IosProgressServiceKt.createPlatformProgressService()
    private let settingsService = IosReaderSettingsServiceKt.createPlatformReaderSettingsService()
    private let ttsService = IosTtsServiceKt.createTtsService()
    private let storyPresentationUseCases = StoryPresentationUseCases()
    private let buildParentReportSummaryUseCase = BuildParentReportSummaryUseCase()
    private let readingSessionReducer = ReadingSessionReducer()
    private let quizSessionReducer = QuizSessionReducer(scoreQuizUseCase: ScoreQuizUseCase())
    private let buildSpeechTextUseCase = BuildSpeechTextUseCase()
    private let analytics = LMCUserDefaultsAnalyticsAdapter()
    private let feedbackRepository = LMCSharedFeedbackRepository()
    private let aiService = LMCAiExplanationAdapter()
    private var speechTask: Task<Void, Never>?

    private lazy var getStoryListUseCase = GetStoryListUseCase(repository: repository)
    private lazy var markStoryCompletedUseCase = MarkStoryCompletedUseCase(progressService: progressService)
    private lazy var getProgressStatsUseCase = GetProgressStatsUseCase(progressService: progressService)
    private lazy var buildParentReportUseCase = BuildParentReportUseCase(progressService: progressService)

    var todayStory: Story? {
        todayShelf.todayStory
    }

    var upNextStory: Story? {
        todayShelf.upNextStory
    }

    private var todayShelf: TodayStories {
        storyPresentationUseCases.selectTodayStories(
            stories: stories,
            completedStoryIds: completedStoryIds,
            policy: .firstincomplete
        )
    }

    func load() async {
        guard loadingState != .loading else { return }
        loadingState = .loading
        do {
            stories = try await getStoryListUseCase.invoke()
            try await refreshProgress()
            try await refreshReadingPositions()
            loadingState = .loaded
        } catch {
            loadingState = .failed
        }
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

    func completeStory(_ story: Story, quizState: QuizSessionState) async -> QuizScore {
        let score = quizScore(story, state: quizState)
        do {
            try await markStoryCompletedUseCase.invoke(record: completionRecord(story, state: quizState))
            try await refreshProgress()
            trackStoryComplete(story, quizCompleted: true)
        } catch {
            loadingState = .failed
        }
        return score
    }

    func progressValue(for story: Story) -> Double {
        storyProgress(for: story).fraction
    }

    func progressLabelKey(for story: Story) -> String {
        storyProgress(for: story).status == .completed ? "status_completed" : "status_not_started"
    }

    func speakCurrent(_ text: String) {
        speak(text)
    }

    func speakAll(_ story: Story) {
        speak(buildSpeechTextUseCase.story(story: story))
    }

    func vocabSpeechText(_ word: Vocab) -> String {
        buildSpeechTextUseCase.vocab(word: word)
    }

    func stopSpeaking() {
        speechTask?.cancel()
        speechTask = Task { @MainActor in
            try? await ttsService.stop()
            isSpeaking = false
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

    func trackParagraphAudioPlay(_ story: Story, paragraphIndex: Int) {
        analytics.track(
            ReaderAnalyticsEvents.shared.paragraphAudioPlay(
                storyId: story.id,
                paragraphIndex: Int32(paragraphIndex + 1),
                audioSource: "tts"
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
        case .answer(let answer, _):
            return .answered(answer)
        case .failure:
            return .failed
        }
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

    func resetAiBackendBaseURL() async -> String {
        try? await settingsService.setAiBackendBaseUrl(baseUrl: "")
        return (try? await settingsService.read().aiBackendBaseUrl) ?? ""
    }

    func savedReadingParagraphIndex(for story: Story) async -> Int {
        let saved = (try? await settingsService.readReadingParagraphIndex(storyId: story.id)) ?? -1
        return Int(readingSessionReducer.initialState(story: story, savedParagraphIndex: Int32(saved)).paragraphIndex)
    }

    func saveReadingParagraphIndex(_ index: Int, for story: Story) {
        readingPositions[story.id] = index
        Task { try? await settingsService.setReadingParagraphIndex(storyId: story.id, paragraphIndex: Int32(index)) }
    }

    func readingState(for story: Story, paragraphIndex: Int) -> ReadingSessionState {
        readingSessionReducer.stateFor(story: story, paragraphIndex: Int32(paragraphIndex))
    }

    func previousReadingState(_ story: Story, state: ReadingSessionState) -> ReadingSessionState {
        readingSessionReducer.previous(story: story, state: state)
    }

    func nextReadingTransition(_ story: Story, state: ReadingSessionState) -> ReadingSessionTransition {
        readingSessionReducer.next(story: story, state: state)
    }

    func filteredStories(level selectedLevel: Int?) -> [Story] {
        storyPresentationUseCases.filterStoriesByLevel(
            stories: stories,
            selectedLevel: selectedLevel.map { KotlinInt(int: Int32($0)) }
        )
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
        return buildParentReportSummaryUseCase.invoke(
            report: parentReport,
            stats: stats,
            nowEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            weekWindowMillis: LMCSevenDaysMillis
        )
    }

    private func speak(_ text: String) {
        speechTask?.cancel()
        speechTask = Task { @MainActor in
            isSpeaking = true
            try? await ttsService.speak(text: text)
        }
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

enum LMCTab: String, CaseIterable {
    case today
    case library
    case parent
    case settings

    var titleKey: LocalizedStringKey {
        switch self {
        case .today: return "nav_today"
        case .library: return "nav_library"
        case .parent: return "nav_parent"
        case .settings: return "nav_settings"
        }
    }

    var icon: String {
        switch self {
        case .today: return "sun.max.fill"
        case .library: return "books.vertical.fill"
        case .parent: return "chart.bar.xaxis"
        case .settings: return "gearshape.fill"
        }
    }
}

enum LMCFlowRoute {
    case reading(storyId: String)
    case vocabulary(storyId: String, openSource: LMCVocabOpenSource)
    case quiz(storyId: String)
}

enum LMCAppOpenType: String {
    case coldStart = "cold_start"
    case foreground
}

enum LMCStoryOpenSource: String {
    case todayHero = "today_hero"
    case todayUpNext = "today_up_next"
    case library
    case quizCompletion = "quiz_completion"
}

enum LMCVocabOpenSource: String {
    case todaySummary = "today_summary"
    case readingFlow = "reading_flow"
    case quizBack = "quiz_back"
}

enum LMCParentReportEntryPoint: String {
    case bottomNavigation = "bottom_navigation"
    case todayHeader = "today_header"
    case settings
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

private struct LibraryScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let openReading: (Story) -> Void
    @State private var selectedLevel: Int?

    private var filteredStories: [Story] {
        viewModel.filteredStories(level: selectedLevel)
    }

    var body: some View {
        GeometryReader { proxy in
            LMCScreen(maxWidth: LMCSpace.gridMaxWidth) {
                VStack(alignment: .leading, spacing: LMCSpace.s5) {
                    SectionTitle("library_title")
                    filterChips

                    if viewModel.loadingState == .failed {
                        LMCErrorView()
                    } else if filteredStories.isEmpty && viewModel.loadingState == .loaded {
                        LMCEmptyState(titleKey: "library_empty_title", messageKey: "library_empty_message")
                    } else if viewModel.loadingState == .loading || viewModel.loadingState == .idle {
                        LMCLoadingView()
                    } else {
                        LazyVGrid(columns: columns(for: proxy.size.width), spacing: LMCSpace.s4) {
                            ForEach(filteredStories, id: \.id) { story in
                                StoryListCard(
                                    story: story,
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
                ForEach([1, 2, 3], id: \.self) { level in
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
                    progress: progress,
                    countText: "\(Int(readingState.paragraphIndex) + 1) / \(Int(readingState.paragraphCount))",
                    isSpeaking: viewModel.isSpeaking,
                close: {
                    viewModel.stopSpeaking()
                    close()
                },
                playCurrent: {
                    if viewModel.isSpeaking {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.trackParagraphAudioPlay(story, paragraphIndex: paragraphIndex)
                        viewModel.speakCurrent(currentParagraph.text)
                    }
                }
            )

            ScrollView {
                VStack(alignment: .leading, spacing: LMCSpace.s5) {
                    readingControls
                    askPanel

                    VStack(alignment: .leading, spacing: LMCSpace.s5) {
                        ForEach(Array(story.paragraphs.enumerated()), id: \.offset) { index, paragraph in
                            ReadingParagraphView(
                                paragraph: paragraph,
                                size: size,
                                showPinyin: showPinyin,
                                isCurrent: index == paragraphIndex
                            )
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

            ReadingBottomBar(
                canGoBack: readingState.canGoPrevious,
                isLast: readingState.isLastParagraph,
                previous: {
                    paragraphIndex = Int(viewModel.previousReadingState(story, state: readingState).paragraphIndex)
                },
                next: {
                    let transition = viewModel.nextReadingTransition(story, state: readingState)
                    paragraphIndex = Int(transition.state.paragraphIndex)
                    if transition.shouldOpenVocabulary {
                        viewModel.stopSpeaking()
                        openVocabulary()
                    }
                }
            )
        }
        .background(LMCColor.background.ignoresSafeArea())
        .task(id: story.id) {
            paragraphIndex = await viewModel.savedReadingParagraphIndex(for: story)
        }
        .onChange(of: showPinyin) { enabled in
            viewModel.trackPinyinToggle(story, paragraphIndex: paragraphIndex, enabled: enabled)
        }
        .onChange(of: paragraphIndex) { _ in
            askState = .idle
            viewModel.saveReadingParagraphIndex(paragraphIndex, for: story)
        }
    }

    private var progress: Double {
        readingState.progressFraction
    }

    private var readingControls: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s3) {
            Toggle(isOn: $showPinyin) {
                Text("reading_pinyin")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
            }
            .tint(LMCColor.secondary)

            HStack(alignment: .center, spacing: LMCSpace.s3) {
                Text("reading_font_size")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundStyle(LMCColor.textPrimary)
                Spacer(minLength: LMCSpace.s2)
                LMCSegmentedReadingSize(readingSize: $readingSize)
            }

            Button {
                if viewModel.isSpeaking {
                    viewModel.stopSpeaking()
                } else {
                    viewModel.speakAll(story)
                }
            } label: {
                Label(
                    LocalizedStringKey(viewModel.isSpeaking ? "reading_stop_audio" : "reading_read_all"),
                    systemImage: viewModel.isSpeaking ? "stop.circle.fill" : "speaker.wave.2.fill"
                )
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(LMCSecondaryButtonStyle())
        }
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
}

private struct QuizScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let story: Story
    let close: () -> Void
    let readAgain: () -> Void
    let done: () -> Void

    @State private var quizState: QuizSessionState?
    @State private var completionScore: QuizScore?
    @State private var didMarkComplete = false
    @State private var didTrackQuizStart = false

    private var activeQuizState: QuizSessionState {
        quizState ?? viewModel.initialQuizState(story)
    }

    private var questionState: QuizQuestionState {
        viewModel.quizQuestionState(story, state: activeQuizState)
    }

    private var question: Question {
        questionState.question ?? story.questions[max(0, min(Int(questionState.questionIndex), story.questions.count - 1))]
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
            _ = await viewModel.completeStory(story, quizState: activeQuizState)
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
                    quizState = viewModel.submitOrAdvanceQuiz(story, state: activeQuizState)
                } label: {
                    Text(LocalizedStringKey(quizActionKey))
                }
                .buttonStyle(LMCPrimaryButtonStyle())
                .disabled(!questionState.canSubmitOrAdvance)
            }
        }
    }

    private var completionView: some View {
        VStack(spacing: 0) {
            LMCFlowTopBar(titleKey: "quiz_complete_title", trailingText: nil, close: done)
            ScrollView {
                VStack(alignment: .center, spacing: LMCSpace.s6) {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 64, weight: .bold))
                        .foregroundStyle(LMCColor.success)
                        .accessibilityHidden(true)

                    VStack(spacing: LMCSpace.s2) {
                        Text(scoreText)
                            .font(.system(size: 32, weight: .bold))
                            .foregroundStyle(LMCColor.textPrimary)
                        Text("quiz_score_label")
                            .font(.system(size: 16))
                            .foregroundStyle(LMCColor.textSecondary)
                    }

                    VStack(alignment: .leading, spacing: LMCSpace.s3) {
                        SectionTitle("quiz_retell")
                        Text(story.retellPrompt)
                            .font(.system(size: 20, weight: .medium))
                            .foregroundStyle(LMCColor.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(LMCSpace.s4)
                    .background(LMCColor.tertiaryContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth)
                .frame(maxWidth: .infinity)
            }

            LMCBottomActionBar {
                Button("action_read_again", action: readAgain)
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

private struct ParentReportScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let entryPoint: LMCParentReportEntryPoint
    let openSettings: () -> Void
    @State private var gatePassed = false

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
    }

    private var parentGate: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s5) {
            Text("parent_gate_message")
                .font(.system(size: 18))
                .foregroundStyle(LMCColor.textPrimary)
                .fixedSize(horizontal: false, vertical: true)

            Button("action_continue") {
                gatePassed = true
                viewModel.trackParentReportOpen(entryPoint: entryPoint)
            }
            .buttonStyle(LMCPrimaryButtonStyle())
        }
        .padding(LMCSpace.s4)
        .background(LMCColor.surface)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var reportContent: some View {
        VStack(alignment: .leading, spacing: LMCSpace.s6) {
	            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: LMCSpace.s3) {
	                MetricTile(titleKey: "parent_stories_read", value: "\(summary?.storiesCompletedThisWeek ?? 0)")
	                MetricTile(titleKey: "parent_reading_days", value: "\(readingDays)")
	                MetricTile(titleKey: "parent_quiz_correct", value: quizCorrectText)
	                MetricTile(titleKey: "parent_words_reviewed", value: "\(summary?.vocabLearnedThisWeek ?? 0)")
	            }

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
}

private struct SettingsScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    @Binding var localeIdentifier: String
    @Binding var showPinyin: Bool
    @Binding var readingSize: String
    @Binding var aiBackendBaseURL: String
    let openParent: () -> Void
    @State private var showPrivacy = false
    @State private var showFeedback = false

    var body: some View {
        LMCScreen(maxWidth: LMCSpace.readingMaxWidth) {
            SectionTitle("settings_title")

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
            }

            SettingsSection(titleKey: "settings_ai") {
                VStack(alignment: .leading, spacing: LMCSpace.s3) {
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

            SettingsSection(titleKey: "settings_parent") {
                SettingsNavigationRow(titleKey: "parent_title", systemName: "chart.bar.xaxis", action: openParent)
                Divider().background(LMCColor.outlineVariant)
                SettingsNavigationRow(titleKey: "settings_privacy", systemName: "shield.checkered", action: { showPrivacy = true })
                Divider().background(LMCColor.outlineVariant)
                SettingsNavigationRow(titleKey: "settings_give_feedback", systemName: "bubble.left.and.bubble.right.fill", action: { showFeedback = true })
            }

            Text("settings_about")
                .font(.system(size: 14))
                .foregroundStyle(LMCColor.textSecondary)
        }
        .alert("settings_privacy", isPresented: $showPrivacy) {
            Button("action_ok", role: .cancel) { }
        } message: {
            Text("parent_privacy_note")
        }
        .sheet(isPresented: $showFeedback) {
            FeedbackSheet(viewModel: viewModel)
                .environment(\.locale, Locale(identifier: localeIdentifier))
                .preferredColorScheme(.light)
        }
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
                        TextField("feedback_parent_contact_placeholder", text: $draft.parentContact)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled(true)
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
    let progress: Double
    let progressLabelKey: String
    let isCompleted: Bool
    let action: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: LMCSpace.s3) {
            StoryCover(story: story, size: 96)

            VStack(alignment: .leading, spacing: LMCSpace.s2) {
                StoryTitleBlock(story: story)
                HStack(spacing: LMCSpace.s2) {
                    LevelChip(level: Int(story.level))
                    Text(LocalizedStringKey(progressLabelKey))
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(LMCColor.textSecondary)
                }
                LMCProgressBar(value: progress)
                if isCompleted {
                    Button(action: action) {
                        Text("action_read_again")
                    }
                    .buttonStyle(LMCSecondaryButtonStyle())
                } else {
                    Button(action: action) {
                        Text("action_start_reading")
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

private struct StoryCover: View {
    let story: Story
    let size: CGFloat

    var body: some View {
        RoundedRectangle(cornerRadius: 8, style: .continuous)
            .fill(
                LinearGradient(
                    colors: [LMCColor.tertiaryContainer, LMCColor.primaryContainer, LMCColor.secondaryContainer],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay {
                Image(systemName: "book.pages.fill")
                    .font(.system(size: max(28, size * 0.32), weight: .bold))
                    .foregroundStyle(LMCColor.primary)
                    .accessibilityHidden(true)
            }
            .frame(width: size, height: size)
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
    let progress: Double
    let countText: String
    let isSpeaking: Bool
    let close: () -> Void
    let playCurrent: () -> Void

    var body: some View {
        VStack(spacing: LMCSpace.s3) {
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
                IconButton(
                    systemName: isSpeaking ? "stop.circle.fill" : "speaker.wave.2.fill",
                    labelKey: isSpeaking ? "reading_stop_audio" : "reading_audio",
                    action: playCurrent
                )
            }
            LMCProgressBar(value: progress)
        }
        .padding(.horizontal, LMCSpace.s4)
        .padding(.top, LMCSpace.s4)
        .padding(.bottom, LMCSpace.s3)
        .background(LMCColor.surface)
    }
}

private struct ReadingParagraphView: View {
    let paragraph: Paragraph
    let size: LMCReadingSize
    let showPinyin: Bool
    let isCurrent: Bool

    var body: some View {
        content
            .padding(LMCSpace.s3)
            .background(isCurrent ? LMCColor.surface.opacity(0.8) : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(paragraph.text))
    }

    @ViewBuilder
    private var content: some View {
        if showPinyin {
            LMCRubyTextFlow(cells: paragraph.lmcRubyCells, size: size)
        } else {
            Text(paragraph.text)
                .font(size.hanziFont)
                .foregroundStyle(LMCColor.textPrimary)
                .lineSpacing(size.hanziLineSpacing)
                .fixedSize(horizontal: false, vertical: true)
        }
    }
}

private struct LMCRubyCellData: Equatable {
    let character: String
    let pinyin: String
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
}

private struct LMCRubyTextFlow: View {
    let cells: [LMCRubyCellData]
    let size: LMCReadingSize

    var body: some View {
        LMCRubyFlowLayout(horizontalSpacing: LMCSpace.s1, verticalSpacing: LMCSpace.s2) {
            ForEach(Array(cells.enumerated()), id: \.offset) { _, cell in
                LMCRubyCellView(cell: cell, size: size)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct LMCRubyCellView: View {
    let cell: LMCRubyCellData
    let size: LMCReadingSize

    var body: some View {
        VStack(spacing: 0) {
            Text(cell.pinyin.isEmpty ? " " : cell.pinyin)
                .font(size.pinyinFont)
                .foregroundStyle(LMCColor.textSecondary)
                .lineLimit(1)
                .multilineTextAlignment(.center)
                .opacity(cell.pinyin.isEmpty ? 0 : 1)
                .frame(height: size.pinyinLineHeight, alignment: .bottom)

            Text(cell.character)
                .font(size.hanziFont)
                .foregroundStyle(LMCColor.textPrimary)
                .lineLimit(1)
                .multilineTextAlignment(.center)
                .frame(height: size.hanziLineHeight, alignment: .top)
        }
        .fixedSize(horizontal: true, vertical: true)
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

private struct ReadingBottomBar: View {
    let canGoBack: Bool
    let isLast: Bool
    let previous: () -> Void
    let next: () -> Void

    var body: some View {
        LMCBottomActionBar {
            Button("action_previous", action: previous)
                .buttonStyle(LMCSecondaryButtonStyle())
                .disabled(!canGoBack)
            Button(action: next) {
                Text(LocalizedStringKey(isLast ? "action_new_words" : "action_next"))
            }
            .buttonStyle(LMCPrimaryButtonStyle())
        }
    }
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
        if isSubmitted && isSubmittedAnswer && !isCorrectAnswer { return "xmark.circle.fill" }
        return isSelected ? "largecircle.fill.circle" : "circle"
    }

    private var iconColor: Color {
        if isSubmitted && isCorrectAnswer { return LMCColor.success }
        if isSubmitted && isSubmittedAnswer && !isCorrectAnswer { return LMCColor.error }
        return isSelected ? LMCColor.secondary : LMCColor.outline
    }

    private var background: Color {
        if isSubmitted && isCorrectAnswer { return LMCColor.successContainer }
        if isSelected { return LMCColor.secondaryContainer }
        return LMCColor.surface
    }

    private var borderColor: Color {
        if isSubmitted && isCorrectAnswer { return LMCColor.success }
        if isSubmitted && isSubmittedAnswer && !isCorrectAnswer { return LMCColor.error }
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
        VStack(spacing: LMCSpace.s3) {
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

#Preview {
    ContentView()
}
