import AppKit

/// Speaks "Bismillah" once at app launch (covers login, since the app can
/// launch at login) and again every time the screen is unlocked / the
/// session becomes active. Respects the same "Speak zikr aloud" toggle as
/// regular reminders — no separate setting needed.
final class UnlockGreeter {
    static let shared = UnlockGreeter()

    private let bismillah = Zikr(
        id: 0,
        arabic: "بِسْمِ اللَّهِ",
        transliteration: "Bismillah",
        translation: "In the name of Allah"
    )

    private init() {
        // sessionDidBecomeActiveNotification only reliably fires for fast
        // user switching, not a plain lock/unlock. loginwindow posts this
        // distributed notification on actual screen unlock instead — the
        // technique most menu bar utilities rely on for this.
        DistributedNotificationCenter.default().addObserver(
            self,
            selector: #selector(speak),
            name: Notification.Name("com.apple.screenIsUnlocked"),
            object: nil
        )

        NSWorkspace.shared.notificationCenter.addObserver(
            self,
            selector: #selector(speak),
            name: NSWorkspace.sessionDidBecomeActiveNotification,
            object: nil
        )
    }

    func start() {
        speak()
    }

    @objc private func speak() {
        guard AppSettings.shared.speakAloud else { return }
        ZikrSpeaker.shared.speak(bismillah)
    }
}
