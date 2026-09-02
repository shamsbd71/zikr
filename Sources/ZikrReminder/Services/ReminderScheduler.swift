import Foundation
import Combine

/// Drives the whole app: a single Timer that fires at a random interval
/// (chosen fresh each cycle between minIntervalMinutes and maxIntervalMinutes),
/// shows one random zikr, then reschedules itself. No polling, no retained
/// history — just one timer and a settings observation.
final class ReminderScheduler {
    static let shared = ReminderScheduler()

    private var timer: Timer?
    private var cancellables = Set<AnyCancellable>()
    private let settings = AppSettings.shared

    private init() {
        settings.$isEnabled
            .sink { [weak self] enabled in
                enabled ? self?.scheduleNext() : self?.stop()
            }
            .store(in: &cancellables)

        // Re-roll the pending timer if the user changes the interval range
        // while a reminder is already enabled, so changes feel immediate.
        Publishers.CombineLatest(settings.$minIntervalMinutes, settings.$maxIntervalMinutes)
            .dropFirst()
            .sink { [weak self] _ in
                guard let self, self.settings.isEnabled else { return }
                self.scheduleNext()
            }
            .store(in: &cancellables)
    }

    func start() {
        NotificationManager.requestAuthorizationIfNeeded()
        if settings.isEnabled {
            scheduleNext()
        }
    }

    /// Manual "Test" trigger from the menu/settings. Always flashes on
    /// screen and plays the sound directly, bypassing Notification Center
    /// entirely — notification delivery depends on system permission state
    /// that can't be guaranteed for an unsigned local build, so the test
    /// button needs a path that's always audible/visible.
    func testNow() {
        let zikr = ZikrList.random()
        if settings.speakAloud { ZikrSpeaker.shared.speak(zikr) }
        FlashOverlayController.shared.present(zikr)
    }

    private func scheduleNext() {
        timer?.invalidate()
        let minSeconds = max(1, settings.minIntervalMinutes) * 60
        let maxSeconds = max(minSeconds, settings.maxIntervalMinutes * 60)
        let delay = Double.random(in: minSeconds...maxSeconds)

        let newTimer = Timer(timeInterval: delay, repeats: false) { [weak self] _ in
            self?.fire()
        }
        newTimer.tolerance = delay * 0.1
        RunLoop.main.add(newTimer, forMode: .common)
        timer = newTimer
    }

    private func stop() {
        timer?.invalidate()
        timer = nil
    }

    private func fire() {
        let inQuietHours = settings.quietHoursEnabled && Self.isWithinQuietHours(
            nowMinutes: Self.currentMinutesOfDay(),
            startMinutes: settings.quietHoursStartMinutes,
            endMinutes: settings.quietHoursEndMinutes
        )
        if !(settings.pauseDuringCalls && MicrophoneMonitor.isInUse) && !inQuietHours {
            show(ZikrList.random())
        }
        if settings.isEnabled {
            scheduleNext()
        }
    }

    /// Pure so the wraparound logic (a window like 22:00–06:00 that
    /// crosses midnight) is easy to reason about independent of the
    /// clock. Equal bounds means "no window" rather than "always on" —
    /// a user who hasn't set both ends yet shouldn't get silenced
    /// entirely by accident.
    static func isWithinQuietHours(nowMinutes: Int, startMinutes: Int, endMinutes: Int) -> Bool {
        if startMinutes == endMinutes { return false }
        if startMinutes < endMinutes {
            return nowMinutes >= startMinutes && nowMinutes < endMinutes
        }
        return nowMinutes >= startMinutes || nowMinutes < endMinutes
    }

    private static func currentMinutesOfDay() -> Int {
        let comps = Calendar.current.dateComponents([.hour, .minute], from: Date())
        return (comps.hour ?? 0) * 60 + (comps.minute ?? 0)
    }

    private func show(_ zikr: Zikr) {
        if settings.speakAloud {
            ZikrSpeaker.shared.speak(zikr)
        }
        switch settings.displayStyle {
        case .notification:
            NotificationManager.deliver(zikr)
        case .fullScreen:
            FlashOverlayController.shared.present(zikr)
        }
    }
}
