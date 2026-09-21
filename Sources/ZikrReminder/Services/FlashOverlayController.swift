import AppKit
import SwiftUI

/// A borderless, non-activating panel that briefly flashes a zikr in the
/// center of the screen, then fades out on its own. The panel is created
/// lazily and released after each dismissal — nothing stays resident.
final class FlashOverlayController {
    static let shared = FlashOverlayController()

    private var panel: NSPanel?
    private var dismissWorkItem: DispatchWorkItem?

    private init() {}

    func present(_ zikr: Zikr) {
        dismissWorkItem?.cancel()
        panel?.close()

        let root = FlashOverlayView(zikr: zikr)

        // fittingSize alone measures as if the text never wraps, so a long
        // dua came out the right width but far too short and got clipped
        // (768x290 instead of 768x555). Take the natural width from
        // fittingSize, cap it, then ask SwiftUI for the height that width
        // actually needs.
        let natural = NSHostingView(rootView: root).fittingSize
        let width = min(natural.width, FlashOverlayView.maxCardWidth)
        let size = NSHostingController(rootView: root)
            .sizeThatFits(in: CGSize(width: width, height: CGFloat.greatestFiniteMagnitude))

        let hosting = NSHostingView(rootView: root)
        hosting.frame = NSRect(origin: .zero, size: size)

        let panel = NSPanel(
            contentRect: hosting.frame,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )
        panel.contentView = hosting
        panel.isOpaque = false
        panel.backgroundColor = .clear
        panel.hasShadow = false
        panel.level = .floating
        panel.collectionBehavior = [.canJoinAllSpaces, .stationary, .ignoresCycle]
        panel.isMovableByWindowBackground = false
        panel.ignoresMouseEvents = true

        if let screen = NSScreen.main {
            let origin = NSPoint(
                x: screen.frame.midX - hosting.frame.width / 2,
                y: screen.frame.midY - hosting.frame.height / 2
            )
            panel.setFrameOrigin(origin)
        }

        panel.alphaValue = 0
        panel.orderFrontRegardless()
        self.panel = panel

        NSAnimationContext.runAnimationGroup { ctx in
            ctx.duration = 0.25
            panel.animator().alphaValue = 1
        }

        let workItem = DispatchWorkItem { [weak self] in
            self?.dismiss()
        }
        dismissWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + AppSettings.shared.flashDurationSeconds, execute: workItem)
    }

    private func dismiss() {
        guard let panel else { return }
        NSAnimationContext.runAnimationGroup({ ctx in
            ctx.duration = 0.4
            panel.animator().alphaValue = 0
        }, completionHandler: { [weak self] in
            panel.close()
            self?.panel = nil
        })
    }
}
