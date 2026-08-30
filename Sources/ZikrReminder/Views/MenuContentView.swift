import SwiftUI
import AppKit

struct MenuContentView: View {
    @ObservedObject private var settings = AppSettings.shared
    @Environment(\.openSettings) private var openSettings

    var body: some View {
        Toggle("Active", isOn: $settings.isEnabled)

        Button("Test Zikr (Speak + Flash)") {
            ReminderScheduler.shared.testNow()
        }

        Divider()

        Button("Settings…") {
            NSApp.activate(ignoringOtherApps: true)
            openSettings()
        }
        .keyboardShortcut(",")

        Button("What's New…") {
            NSApp.activate(ignoringOtherApps: true)
            WhatsNewController.shared.present()
        }

        Button("Check for Updates…") {
            NSApp.activate(ignoringOtherApps: true)
            UpdateFlow.checkManually()
        }

        Divider()

        Button("Quit Zikr") {
            NSApp.terminate(nil)
        }
        .keyboardShortcut("q")
    }
}
