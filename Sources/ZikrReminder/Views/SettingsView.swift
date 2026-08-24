import SwiftUI

struct SettingsView: View {
    @ObservedObject private var settings = AppSettings.shared

    var body: some View {
        Form {
            Section {
                Toggle("Enable reminders", isOn: $settings.isEnabled)
            }

            Section("Timing") {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text("Every")
                        Spacer()
                        Text("\(Int(settings.minIntervalMinutes))–\(Int(settings.maxIntervalMinutes)) min")
                            .foregroundStyle(.secondary)
                            .monospacedDigit()
                    }
                    HStack {
                        Text("Min").frame(width: 34, alignment: .leading)
                        Slider(value: $settings.minIntervalMinutes, in: 1...180, step: 1)
                    }
                    HStack {
                        Text("Max").frame(width: 34, alignment: .leading)
                        Slider(value: $settings.maxIntervalMinutes, in: 1...180, step: 1)
                    }
                }
            }

            Section("Appearance") {
                Picker("Style", selection: $settings.displayStyle) {
                    ForEach(DisplayStyle.allCases) { style in
                        Text(style.label).tag(style)
                    }
                }
                .pickerStyle(.segmented)

                if settings.displayStyle == .fullScreen {
                    HStack {
                        Text("Visible for")
                        Spacer()
                        Text("\(settings.flashDurationSeconds, specifier: "%.1f")s")
                            .foregroundStyle(.secondary)
                            .monospacedDigit()
                    }
                    Slider(value: $settings.flashDurationSeconds, in: 1...8, step: 0.5)
                }

                Toggle("Speak zikr aloud", isOn: $settings.speakAloud)
            }

            Section("System") {
                Toggle("Launch at login", isOn: $settings.launchAtLogin)
            }

            Section {
                Button("Test Zikr (Speak + Flash)") {
                    ReminderScheduler.shared.testNow()
                }
                Text("Test always speaks the zikr aloud and flashes it on screen, regardless of the Style setting above — useful to confirm the voice works.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .formStyle(.grouped)
        .frame(width: 380, height: 430)
    }
}
