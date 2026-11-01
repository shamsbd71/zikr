import AppKit
import SwiftUI

/// Presents UpdateAvailableView in its own window, keeping a strong
/// reference to it so it isn't deallocated while open (mirrors
/// FlashOverlayController's pattern of owning its own panel).
final class UpdateAvailableController: NSObject, NSWindowDelegate {
    static let shared = UpdateAvailableController()

    private var window: NSWindow?
    private var viewModel: UpdateFlowViewModel?
    private var info: UpdateChecker.UpdateInfo?
    private override init() { super.init() }

    func present(currentVersion: String, info: UpdateChecker.UpdateInfo, changelogBody: String, startInstalling: Bool = false) {
        if let window {
            window.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
            if startInstalling { install() }
            return
        }

        let viewModel = UpdateFlowViewModel()
        self.viewModel = viewModel
        self.info = info
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
            onInstall: { [weak self] in self?.install() }
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
        if startInstalling { install() }
    }

    private func install() {
        guard let viewModel, let info, !viewModel.isInstalling else { return }
        viewModel.isInstalling = true
        viewModel.statusText = "Downloading…"
        UpdateChecker.downloadAndInstall(info) { message in
            viewModel.statusText = message
            if message.hasPrefix("Update failed") || message.hasPrefix("Download failed") {
                viewModel.isInstalling = false
            }
            // On success the app relaunches itself, so this window's
            // fate stops mattering.
        }
    }

    func windowWillClose(_ notification: Notification) {
        window = nil
        viewModel = nil
        info = nil
    }
}
