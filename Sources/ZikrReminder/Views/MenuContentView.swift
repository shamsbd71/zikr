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

        Button("Check for Updates…") {
            NSApp.activate(ignoringOtherApps: true)
            UpdateChecker.checkAndUpdate { message in
                let alert = NSAlert()
                alert.messageText = "Zikr Update"
                alert.informativeText = message
                alert.runModal()
            }
        }

        Divider()

        Button("Quit Zikr") {
            NSApp.terminate(nil)
        }
        .keyboardShortcut("q")
    }
}
