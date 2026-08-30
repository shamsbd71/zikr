import AppKit
import SwiftUI

/// Presents WhatsNewView in its own window. Same ownership pattern as
/// UpdateAvailableController.
final class WhatsNewController: NSObject, NSWindowDelegate {
    static let shared = WhatsNewController()

    private var window: NSWindow?
    private override init() { super.init() }

    func present() {
        if let window {
            window.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
            return
        }

        let hosting = NSHostingController(rootView: WhatsNewView())
        let newWindow = NSWindow(contentViewController: hosting)
        newWindow.title = "What's New"
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
