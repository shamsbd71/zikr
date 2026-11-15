import Foundation

/// Picks which zikr to speak. Two pure functions so the behaviour can be
/// reasoned about (and, on the platforms that have test suites, tested)
/// without a timer, a bundle or a speaker.
enum ZikrSelector {
    /// A windowed zikr counts this much more than an anytime one while
    /// its window is open. Tuned so the time of day tilts the selection
    /// without taking it over — see the design spec.
    static let inWindowWeight = 3

    /// Anything longer than this needs the user to opt in. A recitation
    /// arriving unannounced is intrusive in a way a short phrase is not.
    static let longZikrThreshold = 5.0

    /// How often the unlock greeting is one of the greeting-only verses
    /// instead of Bismillah. Bismillah stays the usual greeting; the verse
    /// is meant to arrive now and then, not to replace it.
    static let greetingVerseChance = 1.0 / 3.0

    static func eligible(_ zikr: Zikr, allowLong: Bool) -> Bool {
        allowLong || zikr.seconds <= longZikrThreshold
    }

    /// 1 for anytime, `inWindowWeight` inside a matching window, 0 outside
    /// it. Zero rather than a small value because a sleep dua at 10:00 is
    /// simply wrong; suppressing it is the point.
    static func weight(for zikr: Zikr, minutesSinceMidnight m: Int) -> Int {
        if zikr.windows.isEmpty { return 1 }
        return zikr.windows.contains { $0.contains(minutesSinceMidnight: m) } ? inWindowWeight : 0
    }

    /// Weighted random pick, avoiding an immediate repeat. The anytime
    /// pool is never empty, so there is no empty-pool case to handle —
    /// but `excluding` could in principle empty a tiny pool, hence the
    /// retry without it.
    static func pick(from list: [Zikr], allowLong: Bool, at date: Date = Date(),
                     excluding lastID: Int? = nil) -> Zikr? {
        let m = minutesSinceMidnight(date)
        var pool = list.filter {
            !$0.greeting && eligible($0, allowLong: allowLong) && weight(for: $0, minutesSinceMidnight: m) > 0
        }
        if pool.count > 1, let lastID { pool.removeAll { $0.id == lastID } }
        guard !pool.isEmpty else { return nil }

        let total = pool.reduce(0) { $0 + weight(for: $1, minutesSinceMidnight: m) }
        var roll = Int.random(in: 0..<total)
        for zikr in pool {
            roll -= weight(for: zikr, minutesSinceMidnight: m)
            if roll < 0 { return zikr }
        }
        return pool.last
    }

    /// The login/unlock greeting: Bismillah, or with `greetingVerseChance`
    /// one of the greeting-only entries. `roll` is in [0, 1) and picks
    /// both whether and which, so the whole choice is one number.
    static func greeting(bismillah: Zikr, from list: [Zikr],
                         roll: Double = Double.random(in: 0..<1)) -> Zikr {
        let verses = list.filter(\.greeting)
        guard !verses.isEmpty, roll < greetingVerseChance else { return bismillah }
        let index = Int(roll / greetingVerseChance * Double(verses.count))
        return verses[min(index, verses.count - 1)]
    }

    static func minutesSinceMidnight(_ date: Date) -> Int {
        let c = Calendar.current.dateComponents([.hour, .minute], from: date)
        return (c.hour ?? 0) * 60 + (c.minute ?? 0)
    }
}
