import Foundation
import Combine

/// Single source of truth for user preferences, backed by UserDefaults.
/// Kept as one small class (no Core Data, no external storage) so the
/// app's memory and disk footprint stay negligible.
final class AppSettings: ObservableObject {
    static let shared = AppSettings()

    private let defaults = UserDefaults.standard

    private enum Key {
        static let isEnabled = "isEnabled"
        static let minInterval = "minIntervalMinutes"
        static let maxInterval = "maxIntervalMinutes"
        static let displayStyle = "displayStyle"
        static let speakAloud = "speakAloud"
        static let launchAtLogin = "launchAtLogin"
        static let flashDurationSeconds = "flashDurationSeconds"
    }

    @Published var isEnabled: Bool {
        didSet { defaults.set(isEnabled, forKey: Key.isEnabled) }
    }

    @Published var minIntervalMinutes: Double {
        didSet {
            if minIntervalMinutes > maxIntervalMinutes {
                maxIntervalMinutes = minIntervalMinutes
            }
            defaults.set(minIntervalMinutes, forKey: Key.minInterval)
        }
    }

    @Published var maxIntervalMinutes: Double {
        didSet {
            if maxIntervalMinutes < minIntervalMinutes {
                minIntervalMinutes = maxIntervalMinutes
            }
            defaults.set(maxIntervalMinutes, forKey: Key.maxInterval)
        }
    }

    @Published var displayStyle: DisplayStyle {
        didSet { defaults.set(displayStyle.rawValue, forKey: Key.displayStyle) }
    }

    @Published var speakAloud: Bool {
        didSet { defaults.set(speakAloud, forKey: Key.speakAloud) }
    }

    @Published var launchAtLogin: Bool {
        didSet {
            defaults.set(launchAtLogin, forKey: Key.launchAtLogin)
            LaunchAtLogin.set(launchAtLogin)
        }
    }

    /// How long the full-screen flash stays visible before fading out.
    @Published var flashDurationSeconds: Double {
        didSet { defaults.set(flashDurationSeconds, forKey: Key.flashDurationSeconds) }
    }

    private init() {
        defaults.register(defaults: [
            Key.isEnabled: true,
            Key.minInterval: 20.0,
            Key.maxInterval: 45.0,
            Key.displayStyle: DisplayStyle.notification.rawValue,
            Key.speakAloud: true,
            Key.launchAtLogin: false,
            Key.flashDurationSeconds: 2.0,
        ])

        isEnabled = defaults.bool(forKey: Key.isEnabled)
        minIntervalMinutes = defaults.double(forKey: Key.minInterval)
        maxIntervalMinutes = defaults.double(forKey: Key.maxInterval)
        displayStyle = DisplayStyle(rawValue: defaults.string(forKey: Key.displayStyle) ?? "") ?? .notification
        speakAloud = defaults.bool(forKey: Key.speakAloud)
        launchAtLogin = LaunchAtLogin.isEnabled
        flashDurationSeconds = defaults.double(forKey: Key.flashDurationSeconds)
    }
}
