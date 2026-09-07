import Foundation

struct Zikr: Identifiable, Hashable, Decodable {
    let id: Int
    let arabic: String
    let transliteration: String
    let translation: String

    /// Times of day this zikr belongs to. Empty means anytime, which is
    /// the default and the majority of the list.
    let windows: [ZikrWindow]

    /// Measured length of the bundled recitation, in seconds. Drives the
    /// "longer adhkar" opt-in; also a fair estimate for the TTS fallback.
    let seconds: Double

    private enum CodingKeys: String, CodingKey {
        case id, arabic, transliteration, translation, windows, seconds
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        arabic = try c.decode(String.self, forKey: .arabic)
        transliteration = try c.decode(String.self, forKey: .transliteration)
        translation = try c.decode(String.self, forKey: .translation)
        seconds = try c.decodeIfPresent(Double.self, forKey: .seconds) ?? 0
        // Unknown window names are dropped rather than failing the whole
        // load: a future data file should degrade to "anytime", not stop
        // the app from starting.
        windows = (try c.decodeIfPresent([String].self, forKey: .windows) ?? [])
            .compactMap(ZikrWindow.init(rawValue:))
    }

    init(id: Int, arabic: String, transliteration: String, translation: String,
         windows: [ZikrWindow] = [], seconds: Double = 0) {
        self.id = id
        self.arabic = arabic
        self.transliteration = transliteration
        self.translation = translation
        self.windows = windows
        self.seconds = seconds
    }
}

/// Wall-clock time bands. Deliberately not user-configurable and
/// deliberately not tied to prayer times — no location, no network.
enum ZikrWindow: String, Hashable {
    case morning, evening, night

    /// Half-open [start, end) in minutes since local midnight.
    var range: (start: Int, end: Int) {
        switch self {
        case .morning: return (4 * 60 + 30, 10 * 60)
        case .evening: return (15 * 60, 19 * 60)
        case .night: return (21 * 60, 2 * 60)      // wraps midnight
        }
    }

    func contains(minutesSinceMidnight m: Int) -> Bool {
        let (start, end) = range
        return start <= end ? (m >= start && m < end) : (m >= start || m < end)
    }
}
