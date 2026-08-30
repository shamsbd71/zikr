import Foundation

/// Runs UpdateFlow.checkAutomatically() once shortly after launch and
/// then every 24 hours, mirroring how most auto-updating Mac apps behave
/// — check periodically in the background, only surface something when
/// there's actually a new version.
final class UpdateScheduler {
    static let shared = UpdateScheduler()

    private var timer: Timer?
    private init() {}

    func start() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
            UpdateFlow.checkAutomatically()
        }

        timer?.invalidate()
        let newTimer = Timer(timeInterval: 24 * 60 * 60, repeats: true) { _ in
            UpdateFlow.checkAutomatically()
        }
        RunLoop.main.add(newTimer, forMode: .common)
        timer = newTimer
    }
}
