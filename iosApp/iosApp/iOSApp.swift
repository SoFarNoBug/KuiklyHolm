import SwiftUI
import holmDemo

@main
struct iOSApp: App {
    init() {
        let caches = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first
            ?? FileManager.default.temporaryDirectory
        HolmIosBridge.shared.configure(cacheDir: caches.path)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
