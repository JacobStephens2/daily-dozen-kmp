import SwiftUI
import ComposeApp

// Thin iOS launcher. Hosts the shared Compose UI via a UIViewController
// bridge; contains no business or UI logic of its own.
@main
struct iOSApp: App {
    // Start Koin once at launch, before any shared ViewModel is resolved —
    // the iOS counterpart to Android's DailyDozenApp and Wasm's main().
    init() {
        KoinInitializerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
