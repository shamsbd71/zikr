import AppKit
import Combine

/// The newest release found by any check, so the menu can show
/// "Update to vX…" inside the app — even for a version whose popup the
/// user skipped. Nil when up to date.
final class UpdateStatus: ObservableObject {
    static let shared = UpdateStatus()
    @Published var available: UpdateChecker.UpdateInfo?
    private init() {}
}

/// Decides *when* to bother the user about an update, on top of
/// UpdateChecker's plain check/install primitives:
///   - a manual "Check for Updates…" always shows the dialog if one
///     exists, even for a version the user previously skipped, since
///     that's an explicit request;
///   - the automatic background check (on launch, then periodically)
///     stays silent for a version the user skipped, and installs without
///     asking if the user opted into "Automatically download and
///     install updates".
enum UpdateFlow {
    static func checkManually() {
        UpdateChecker.checkForUpdate { result in
            record(result)
            switch result {
            case .upToDate(let version):
                presentAlert("You're up to date (v\(version)).")
            case .error(let message):
                presentAlert(message)
            case .available(let info):
                presentDialog(for: info)
            }
        }
    }

    static func checkAutomatically() {
        UpdateChecker.checkForUpdate { result in
            record(result)
            guard case .available(let info) = result else { return }
            if info.version == AppSettings.shared.skippedUpdateVersion { return }

            if AppSettings.shared.autoInstallUpdates {
                UpdateChecker.downloadAndInstall(info) { _ in }
            } else {
                presentDialog(for: info)
            }
        }
    }

    /// The menu's "Update to vX…": opens the update window already
    /// downloading, so one click updates and the progress is visible.
    static func installNow(_ info: UpdateChecker.UpdateInfo) {
        presentDialog(for: info, startInstalling: true)
    }

    private static func record(_ result: UpdateChecker.CheckResult) {
        switch result {
        case .available(let info): UpdateStatus.shared.available = info
        case .upToDate: UpdateStatus.shared.available = nil
        case .error: break
        }
    }

    private static func presentDialog(for info: UpdateChecker.UpdateInfo, startInstalling: Bool = false) {
        let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"
        ChangelogFetcher.fetch { entries in
            let body = entries.first(where: { $0.version == info.version })?.body ?? ""
            UpdateAvailableController.shared.present(currentVersion: currentVersion, info: info, changelogBody: body, startInstalling: startInstalling)
        }
    }

    private static func presentAlert(_ message: String) {
        let alert = NSAlert()
        alert.messageText = "Zikr Update"
        alert.informativeText = message
        alert.runModal()
    }
}
