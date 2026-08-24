import AppKit

/// Checks GitHub Releases for a newer tag than the running app's
/// CFBundleShortVersionString, and if found, downloads the release zip,
/// swaps it into place over the currently-running .app, and relaunches.
/// No third-party updater framework — just URLSession + /usr/bin/ditto.
enum UpdateChecker {
    static let repo = "shamsbd71/zikr"
    private static let apiURL = URL(string: "https://api.github.com/repos/\(repo)/releases/latest")!

    static func checkAndUpdate(completion: @escaping (String) -> Void) {
        var request = URLRequest(url: apiURL)
        request.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")

        URLSession.shared.dataTask(with: request) { data, _, error in
            guard
                let data, error == nil,
                let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                let tag = json["tag_name"] as? String,
                let assets = json["assets"] as? [[String: Any]],
                let zipAsset = assets.first(where: { ($0["name"] as? String)?.hasSuffix(".zip") == true }),
                let downloadURLString = zipAsset["browser_download_url"] as? String,
                let downloadURL = URL(string: downloadURLString)
            else {
                DispatchQueue.main.async { completion("Couldn't check for updates. Try again later.") }
                return
            }

            let latestVersion = tag.hasPrefix("v") ? String(tag.dropFirst()) : tag
            let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"

            guard latestVersion.compare(currentVersion, options: .numeric) == .orderedDescending else {
                DispatchQueue.main.async { completion("You're up to date (v\(currentVersion)).") }
                return
            }

            downloadAndInstall(from: downloadURL, version: latestVersion, completion: completion)
        }.resume()
    }

    private static func downloadAndInstall(from url: URL, version: String, completion: @escaping (String) -> Void) {
        URLSession.shared.downloadTask(with: url) { tempURL, _, error in
            guard let tempURL, error == nil else {
                DispatchQueue.main.async { completion("Download failed. Try again later.") }
                return
            }

            do {
                let fm = FileManager.default
                let workDir = fm.temporaryDirectory.appendingPathComponent("ZikrUpdate-\(UUID().uuidString)")
                try fm.createDirectory(at: workDir, withIntermediateDirectories: true)

                let zipPath = workDir.appendingPathComponent("update.zip")
                try fm.moveItem(at: tempURL, to: zipPath)

                try run("/usr/bin/ditto", ["-x", "-k", zipPath.path, workDir.path])

                guard let newAppPath = try fm.contentsOfDirectory(at: workDir, includingPropertiesForKeys: nil)
                    .first(where: { $0.pathExtension == "app" })
                else {
                    throw NSError(domain: "Update", code: 1, userInfo: [NSLocalizedDescriptionKey: "Release zip didn't contain an .app"])
                }

                // Downloaded via URLSession, so it carries a quarantine flag;
                // this is our own ad-hoc-signed build, not a third-party
                // download, so clearing it is appropriate here.
                try run("/usr/bin/xattr", ["-cr", newAppPath.path])

                let installedURL = URL(fileURLWithPath: Bundle.main.bundlePath)
                let backupURL = installedURL.deletingLastPathComponent().appendingPathComponent("Zikr.app.old")
                try? fm.removeItem(at: backupURL)
                try fm.moveItem(at: installedURL, to: backupURL)
                try fm.moveItem(at: newAppPath, to: installedURL)
                try? fm.removeItem(at: backupURL)
                try? fm.removeItem(at: workDir)

                DispatchQueue.main.async {
                    completion("Updated to v\(version). Relaunching…")
                    relaunch(at: installedURL)
                }
            } catch {
                DispatchQueue.main.async { completion("Update failed: \(error.localizedDescription)") }
            }
        }.resume()
    }

    private static func relaunch(at appURL: URL) {
        let task = Process()
        task.executableURL = URL(fileURLWithPath: "/usr/bin/open")
        task.arguments = [appURL.path]
        try? task.run()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            NSApp.terminate(nil)
        }
    }

    @discardableResult
    private static func run(_ path: String, _ args: [String]) throws -> String {
        let task = Process()
        task.executableURL = URL(fileURLWithPath: path)
        task.arguments = args
        let pipe = Pipe()
        task.standardOutput = pipe
        task.standardError = pipe
        try task.run()
        task.waitUntilExit()
        let output = String(data: pipe.fileHandleForReading.readDataToEndOfFile(), encoding: .utf8) ?? ""
        if task.terminationStatus != 0 {
            throw NSError(domain: "Update", code: Int(task.terminationStatus), userInfo: [NSLocalizedDescriptionKey: output])
        }
        return output
    }
}
