import Foundation

struct ChangelogEntry: Identifiable {
    var id: String { version }
    let version: String
    let date: String
    let body: String
}

/// Fetches and parses CHANGELOG.md straight from the repo's main branch,
/// so "What's New" and the update dialog always show the same text as
/// the file in the repo — nothing duplicated or hand-copied into the app.
enum ChangelogFetcher {
    private static let rawURL = URL(
        string: "https://raw.githubusercontent.com/shamsbd71/zikr/main/CHANGELOG.md"
    )!

    static func fetch(completion: @escaping ([ChangelogEntry]) -> Void) {
        URLSession.shared.dataTask(with: rawURL) { data, _, error in
            guard let data, error == nil, let text = String(data: data, encoding: .utf8) else {
                DispatchQueue.main.async { completion([]) }
                return
            }
            let entries = parse(text)
            DispatchQueue.main.async { completion(entries) }
        }.resume()
    }

    /// Splits on "## [version] — date" headers. Pure function, so it's
    /// testable without a network call.
    static func parse(_ markdown: String) -> [ChangelogEntry] {
        var entries: [ChangelogEntry] = []
        var currentVersion: String?
        var currentDate = ""
        var currentBody: [String] = []

        func flush() {
            guard let version = currentVersion, version.lowercased() != "unreleased" else { return }
            let body = currentBody
                .joined(separator: "\n")
                .trimmingCharacters(in: .whitespacesAndNewlines)
            entries.append(ChangelogEntry(version: version, date: currentDate, body: body))
        }

        for line in markdown.components(separatedBy: "\n") {
            if line.hasPrefix("## [") {
                flush()
                currentBody = []
                let afterBracket = line.dropFirst(4) // past "## ["
                if let closeBracket = afterBracket.firstIndex(of: "]") {
                    currentVersion = String(afterBracket[afterBracket.startIndex..<closeBracket])
                    let rest = afterBracket[afterBracket.index(after: closeBracket)...]
                    currentDate = rest
                        .replacingOccurrences(of: "\u{2014}", with: "") // em dash
                        .trimmingCharacters(in: .whitespaces)
                } else {
                    currentVersion = nil
                }
            } else if currentVersion != nil {
                currentBody.append(line)
            }
        }
        flush()
        return entries
    }
}
