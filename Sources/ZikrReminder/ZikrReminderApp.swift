import SwiftUI

@main
struct ZikrReminderApp: App {
    init() {
        ReminderScheduler.shared.start()
        UnlockGreeter.shared.start()
    }

    var body: some Scene {
        MenuBarExtra {
            MenuContentView()
        } label: {
            Image(systemName: "moon.stars.fill")
        }
        .menuBarExtraStyle(.menu)

        Settings {
            SettingsView()
        }
    }
}
