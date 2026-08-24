import ServiceManagement
import os.log

/// Thin wrapper around the modern SMAppService login-item API
/// (the Apple-recommended replacement for SMLoginItemSetEnabled / LaunchAgents).
enum LaunchAtLogin {
    private static let logger = Logger(subsystem: "com.abu.ZikrReminder", category: "LaunchAtLogin")

    static var isEnabled: Bool {
        SMAppService.mainApp.status == .enabled
    }

    static func set(_ enabled: Bool) {
        do {
            if enabled {
                if SMAppService.mainApp.status == .enabled { return }
                try SMAppService.mainApp.register()
            } else {
                if SMAppService.mainApp.status != .enabled { return }
                try SMAppService.mainApp.unregister()
            }
        } catch {
            logger.error("Failed to update login item: \(error.localizedDescription, privacy: .public)")
        }
    }
}
