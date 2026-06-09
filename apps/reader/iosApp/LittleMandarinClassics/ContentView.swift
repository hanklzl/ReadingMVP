import SwiftUI
import shared

struct ContentView: View {
    private let appInfo = GetAppInfoUseCase().invoke()

    var body: some View {
        Text(LocalizedStringKey(appInfo.nameResourceKey))
            .font(.title)
            .multilineTextAlignment(.center)
            .padding()
    }
}

#Preview {
    ContentView()
}
