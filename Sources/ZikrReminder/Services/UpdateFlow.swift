import AppKit

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
            guard case .available(let info) = result else { return }
            if info.version == AppSettings.shared.skippedUpdateVersion { return }

            if AppSettings.shared.autoInstallUpdates {
                UpdateChecker.downloadAndInstall(info) { _ in }
            } else {
                presentDialog(for: info)
            }
        }
    }

    private static func presentDialog(for info: UpdateChecker.UpdateInfo) {
        let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"
        ChangelogFetcher.fetch { entries in
            let body = entries.first(where: { $0.version == info.version })?.body ?? ""
            UpdateAvailableController.shared.present(currentVersion: currentVersion, info: info, changelogBody: body)
        }
    }

    private static func presentAlert(_ message: String) {
        let alert = NSAlert()
        alert.messageText = "Zikr Update"
        alert.informativeText = message
        alert.runModal()
    }
}
