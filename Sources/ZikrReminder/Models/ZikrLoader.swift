import Foundation

/// Loads the bundled zikr list. `zikr.json` is copied into the app bundle
/// by build.sh from the canonical data/zikr.json at the repo root, which
/// the Linux, Windows and Android builds read too — one file to edit
/// instead of four hand-synced copies.
enum ZikrLoader {
    static let all: [Zikr] = load()

    private static func load() -> [Zikr] {
        guard let url = Bundle.main.url(forResource: "zikr", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let list = try? JSONDecoder().decode([Zikr].self, from: data),
              !list.isEmpty
        else {
            // A missing or unparseable list is a build error, not a runtime
            // condition to limp along with — the app has nothing to say
            // without it. Same stance as the Linux build, which raises
            // FileNotFoundError rather than starting empty.
            fatalError("zikr.json missing or unreadable from the app bundle")
        }
        return list
    }
}
