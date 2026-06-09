import Foundation
import SwiftUI
import shared

struct ContentView: View {
    @AppStorage("lmc_locale_identifier") private var localeIdentifier = LMCAppLocale.english.rawValue
    @AppStorage("lmc_show_pinyin") private var showPinyin = true
    @AppStorage("lmc_reading_size") private var readingSize = LMCReadingSize.medium.rawValue
    @StateObject private var viewModel = ReaderViewModel()
    @State private var selectedTab: LMCTab = .today
    @State private var flowRoute: LMCFlowRoute?
    @State private var lockedQuizAlert = false

    var body: some View {
        ZStack {
            LMCColor.background.ignoresSafeArea()

            if let flowRoute {
                flowView(for: flowRoute)
            } else {
                VStack(spacing: 0) {
                    topLevelView
                    LMCBottomNavigation(selectedTab: $selectedTab)
                }
            }
        }
        .environment(\.locale, Locale(identifier: localeIdentifier))
        .preferredColorScheme(.light)
        .task {
            await viewModel.load()
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
                openReading: { flowRoute = .reading(storyId: $0.id) },
                openVocabulary: { flowRoute = .vocabulary(storyId: $0.id) },
                openQuiz: { story in
                    if viewModel.isCompleted(story) {
                        flowRoute = .quiz(storyId: story.id)
                    } else {
                        lockedQuizAlert = true
                    }
                },
                openParent: { selectedTab = .parent },
                openSettings: { selectedTab = .settings }
            )
        case .library:
            LibraryScreen(
                viewModel: viewModel,
                openReading: { flowRoute = .reading(storyId: $0.id) }
            )
        case .parent:
            ParentReportScreen(
                viewModel: viewModel,
                openSettings: { selectedTab = .settings }
            )
        case .settings:
            SettingsScreen(
                viewModel: viewModel,
                localeIdentifier: $localeIdentifier,
                showPinyin: $showPinyin,
                readingSize: $readingSize,
                openParent: { selectedTab = .parent }
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
                    close: { flowRoute = nil },
                    openVocabulary: { flowRoute = .vocabulary(storyId: story.id) }
                )
            } else {
                LMCMissingStoryView(close: { flowRoute = nil })
            }
        case .vocabulary(let storyId):
            if let story = viewModel.story(id: storyId) {
                VocabularyScreen(
                    viewModel: viewModel,
                    story: story,
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
                    close: { flowRoute = .vocabulary(storyId: story.id) },
                    readAgain: { flowRoute = .reading(storyId: story.id) },
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
}

@MainActor
final class ReaderViewModel: ObservableObject {
    @Published private(set) var stories: [Story] = []
    @Published private(set) var completedStoryIds: Set<String> = []
    @Published private(set) var stats: ProgressStats?
    @Published private(set) var parentReport: ParentProgressReport?
    @Published private(set) var loadingState: LMCLoadingState = .idle
    @Published private(set) var isSpeaking = false

    private let repository = DefaultStoryRepository(
        resourceReader: IosStoryResourceReaderKt.defaultStoryResourceReader(),
        catalog: StoryResourceCatalog.shared.entries
    )
    private let progressService = IosProgressServiceKt.createPlatformProgressService()
    private let ttsService = IosTtsServiceKt.createTtsService()
    private let scoreQuizUseCase = ScoreQuizUseCase()
    private var speechTask: Task<Void, Never>?

    private lazy var getStoryListUseCase = GetStoryListUseCase(repository: repository)
    private lazy var markStoryCompletedUseCase = MarkStoryCompletedUseCase(progressService: progressService)
    private lazy var getProgressStatsUseCase = GetProgressStatsUseCase(progressService: progressService)
    private lazy var buildParentReportUseCase = BuildParentReportUseCase(progressService: progressService)

    var todayStory: Story? {
        stories.first { !completedStoryIds.contains($0.id) } ?? stories.first
    }

    var upNextStory: Story? {
        guard let todayStory, let currentIndex = stories.firstIndex(where: { $0.id == todayStory.id }) else {
            return stories.dropFirst().first
        }
        return stories.dropFirst(currentIndex + 1).first
    }

    func load() async {
        guard loadingState != .loading else { return }
        loadingState = .loading
        do {
            stories = try await getStoryListUseCase.invoke()
            try await refreshProgress()
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

    func score(story: Story, answers: [String: String]) -> QuizScore {
        scoreQuizUseCase.invoke(story: story, answers: answers)
    }

    func completeStory(_ story: Story, answers: [String: String]) async -> QuizScore {
        let score = score(story: story, answers: answers)
        let completedAt = Int64(Date().timeIntervalSince1970 * 1_000)
        let record = CompletionRecord(
            storyId: story.id,
            completedAtEpochMillis: completedAt,
            vocabCount: Int32(story.vocab.count),
            correctCount: Int32(score.correctCount),
            questionCount: Int32(score.totalQuestions)
        )
        do {
            try await markStoryCompletedUseCase.invoke(record: record)
            try await refreshProgress()
        } catch {
            loadingState = .failed
        }
        return score
    }

    func progressValue(for story: Story) -> Double {
        isCompleted(story) ? 1 : 0
    }

    func progressLabelKey(for story: Story) -> String {
        isCompleted(story) ? "status_completed" : "status_not_started"
    }

    func speakCurrent(_ text: String) {
        speak(text)
    }

    func speakAll(_ story: Story) {
        speak(story.paragraphs.map(\.text).joined(separator: "\n"))
    }

    func stopSpeaking() {
        speechTask?.cancel()
        speechTask = Task { @MainActor in
            try? await ttsService.stop()
            isSpeaking = false
        }
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
        completedStoryIds = Set(records.map(\.storyId))
        stats = try await getProgressStatsUseCase.invoke()
        parentReport = try await buildParentReportUseCase.invoke(
            nowEpochMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            weekWindowMillis: LMCSevenDaysMillis
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
    case vocabulary(storyId: String)
    case quiz(storyId: String)
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

    static func value(from rawValue: String) -> LMCReadingSize {
        LMCReadingSize(rawValue: rawValue) ?? .medium
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
        guard let selectedLevel else { return viewModel.stories }
        return viewModel.stories.filter { Int($0.level) == selectedLevel }
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
    let close: () -> Void
    let openVocabulary: () -> Void
    @State private var paragraphIndex = 0

    private var currentParagraph: Paragraph {
        story.paragraphs[max(0, min(paragraphIndex, story.paragraphs.count - 1))]
    }

    private var size: LMCReadingSize {
        LMCReadingSize.value(from: readingSize)
    }

    var body: some View {
        VStack(spacing: 0) {
            ReadingTopBar(
                story: story,
                progress: progress,
                countText: "\(paragraphIndex + 1) / \(max(story.paragraphs.count, 1))",
                isSpeaking: viewModel.isSpeaking,
                close: {
                    viewModel.stopSpeaking()
                    close()
                },
                playCurrent: {
                    if viewModel.isSpeaking {
                        viewModel.stopSpeaking()
                    } else {
                        viewModel.speakCurrent(currentParagraph.text)
                    }
                }
            )

            ScrollView {
                VStack(alignment: .leading, spacing: LMCSpace.s5) {
                    readingControls

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
                canGoBack: paragraphIndex > 0,
                isLast: paragraphIndex >= story.paragraphs.count - 1,
                previous: { paragraphIndex = max(0, paragraphIndex - 1) },
                next: {
                    if paragraphIndex >= story.paragraphs.count - 1 {
                        viewModel.stopSpeaking()
                        openVocabulary()
                    } else {
                        paragraphIndex += 1
                    }
                }
            )
        }
        .background(LMCColor.background.ignoresSafeArea())
    }

    private var progress: Double {
        guard !story.paragraphs.isEmpty else { return 0 }
        return Double(paragraphIndex + 1) / Double(story.paragraphs.count)
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
}

private struct VocabularyScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let story: Story
    let close: () -> Void
    let openQuiz: () -> Void
    @State private var wordIndex = 0

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
                            viewModel.speakCurrent([currentWord.word, currentWord.example].compactMap { $0 }.joined(separator: "。"))
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
    }
}

private struct QuizScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    let story: Story
    let close: () -> Void
    let readAgain: () -> Void
    let done: () -> Void

    @State private var questionIndex = 0
    @State private var selectedAnswer: String?
    @State private var submittedAnswer: String?
    @State private var answers: [String: String] = [:]
    @State private var isComplete = false
    @State private var completionScore: QuizScore?
    @State private var didMarkComplete = false

    private var question: Question {
        story.questions[max(0, min(questionIndex, story.questions.count - 1))]
    }

    var body: some View {
        VStack(spacing: 0) {
            if isComplete {
                completionView
            } else {
                quizQuestionView
            }
        }
        .background(LMCColor.background.ignoresSafeArea())
        .task(id: isComplete) {
            guard isComplete, !didMarkComplete else { return }
            didMarkComplete = true
            completionScore = await viewModel.completeStory(story, answers: answers)
        }
    }

    private var quizQuestionView: some View {
        VStack(spacing: 0) {
            LMCFlowTopBar(
                titleKey: "quiz_title",
                trailingText: "\(questionIndex + 1) / \(max(story.questions.count, 1))",
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
                                isSelected: selectedAnswer == option,
                                isSubmitted: submittedAnswer != nil,
                                isCorrectAnswer: option == question.answer,
                                isSubmittedAnswer: submittedAnswer == option,
                                action: {
                                    guard submittedAnswer == nil else { return }
                                    selectedAnswer = option
                                }
                            )
                        }
                    }

                    if submittedAnswer != nil {
                        FeedbackMessage(isCorrect: submittedAnswer == question.answer, explanation: question.explanation)
                    }
                }
                .padding(LMCSpace.s4)
                .frame(maxWidth: LMCSpace.readingMaxWidth)
                .frame(maxWidth: .infinity)
            }

            LMCBottomActionBar {
                Spacer()
                Button {
                    if submittedAnswer == nil {
                        guard let selectedAnswer else { return }
                        submittedAnswer = selectedAnswer
                        answers[question.id] = selectedAnswer
                    } else if questionIndex >= story.questions.count - 1 {
                        isComplete = true
                    } else {
                        questionIndex += 1
                        selectedAnswer = nil
                        submittedAnswer = nil
                    }
                } label: {
                    Text(LocalizedStringKey(quizActionKey))
                }
                .buttonStyle(LMCPrimaryButtonStyle())
                .disabled(submittedAnswer == nil && selectedAnswer == nil)
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
        guard !story.questions.isEmpty else { return 0 }
        return Double(questionIndex + 1) / Double(story.questions.count)
    }

    private var quizActionKey: String {
        if submittedAnswer == nil { return "action_submit" }
        return questionIndex >= story.questions.count - 1 ? "quiz_show_result" : "action_next"
    }

    private var scoreText: String {
        let score = completionScore ?? viewModel.score(story: story, answers: answers)
        return "\(score.correctCount) / \(score.totalQuestions)"
    }
}

private struct ParentReportScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
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
                MetricTile(titleKey: "parent_stories_read", value: "\(viewModel.stats?.completedCount ?? 0)")
                MetricTile(titleKey: "parent_reading_days", value: "\(readingDays)")
                MetricTile(titleKey: "parent_quiz_correct", value: quizCorrectText)
                MetricTile(titleKey: "parent_words_reviewed", value: "\(viewModel.stats?.vocabLearnedCount ?? 0)")
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
        guard let stats = viewModel.stats else { return "0 / 0" }
        return "\(stats.correctCount) / \(stats.questionCount)"
    }

    private var readingDays: Int {
        guard let completions = viewModel.parentReport?.recentCompletions else { return 0 }
        let calendar = Calendar.current
        let days = completions.map {
            calendar.startOfDay(for: Date(timeIntervalSince1970: Double($0.completedAtEpochMillis) / 1_000))
        }
        return Set(days).count
    }
}

private struct SettingsScreen: View {
    @ObservedObject var viewModel: ReaderViewModel
    @Binding var localeIdentifier: String
    @Binding var showPinyin: Bool
    @Binding var readingSize: String
    let openParent: () -> Void
    @State private var showPrivacy = false

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

            SettingsSection(titleKey: "settings_parent") {
                SettingsNavigationRow(titleKey: "parent_title", systemName: "chart.bar.xaxis", action: openParent)
                Divider().background(LMCColor.outlineVariant)
                SettingsNavigationRow(titleKey: "settings_privacy", systemName: "shield.checkered", action: { showPrivacy = true })
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
        VStack(alignment: .leading, spacing: LMCSpace.s2) {
            if showPinyin {
                Text(paragraph.pinyin)
                    .font(size.pinyinFont)
                    .foregroundStyle(LMCColor.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Text(paragraph.text)
                .font(size.hanziFont)
                .foregroundStyle(LMCColor.textPrimary)
                .lineSpacing(size.hanziLineSpacing)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(LMCSpace.s3)
        .background(isCurrent ? LMCColor.surface.opacity(0.8) : Color.clear)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
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

    var body: some View {
        HStack(spacing: 0) {
            ForEach(LMCTab.allCases, id: \.rawValue) { tab in
                Button {
                    selectedTab = tab
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

    private static func localized(_ key: String) -> String {
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
