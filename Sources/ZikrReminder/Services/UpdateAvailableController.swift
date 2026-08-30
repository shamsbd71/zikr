import AppKit
import SwiftUI

/// Presents UpdateAvailableView in its own window, keeping a strong
/// reference to it so it isn't deallocated while open (mirrors
/// FlashOverlayController's pattern of owning its own panel).
final class UpdateAvailableController: NSObject, NSWindowDelegate {
    static let shared = UpdateAvailableController()

    private var window: NSWindow?
    private override init() { super.init() }

    func present(currentVersion: String, info: UpdateChecker.UpdateInfo, changelogBody: String) {
        if let window {
            window.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
            return
        }

        let viewModel = UpdateFlowViewModel()
        let view = UpdateAvailableView(
            currentVersion: currentVersion,
            info: info,
            changelogBody: changelogBody,
            viewModel: viewModel,
            onSkip: { [weak self] in
                AppSettings.shared.skippedUpdateVersion = info.version
                self?.window?.close()
            },
            onRemindLater: { [weak self] in
                self?.window?.close()
            },
            onInstall: {
                viewModel.isInstalling = true
                viewModel.statusText = "Downloading…"
                UpdateChecker.downloadAndInstall(info) { message in
                    viewModel.statusText = message
                    if message.hasPrefix("Update failed") || message.hasPrefix("Download failed") {
                        viewModel.isInstalling = false
                    }
                    // On success the app relaunches itself, so this
                    // window's fate stops mattering.
                }
            }
        )

        let hosting = NSHostingController(rootView: view)
        let newWindow = NSWindow(contentViewController: hosting)
        newWindow.title = "Zikr Update"
        newWindow.styleMask = [.titled, .closable]
        newWindow.isReleasedWhenClosed = false
        newWindow.delegate = self
        newWindow.center()

        NSApp.activate(ignoringOtherApps: true)
        newWindow.makeKeyAndOrderFront(nil)
        window = newWindow
    }

    func windowWillClose(_ notification: Notification) {
        window = nil
    }
}
