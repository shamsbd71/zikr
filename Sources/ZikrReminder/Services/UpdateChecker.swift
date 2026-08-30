import AppKit

/// Checks GitHub Releases for a newer tag than the running app's
/// CFBundleShortVersionString. Checking and installing are separate
/// steps — UpdateFlow decides whether to ask first (the normal case) or
/// install silently (only when the user opted into automatic installs).
/// No third-party updater framework — just URLSession + /usr/bin/ditto.
enum UpdateChecker {
    static let repo = "shamsbd71/zikr"
    private static let apiURL = URL(string: "https://api.github.com/repos/\(repo)/releases/latest")!

    struct UpdateInfo {
        let version: String
        let downloadURL: URL
    }

    enum CheckResult {
        case upToDate(String)
        case available(UpdateInfo)
        case error(String)
    }

    static func checkForUpdate(completion: @escaping (CheckResult) -> Void) {
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
                DispatchQueue.main.async { completion(.error("Couldn't check for updates. Try again later.")) }
                return
            }

            let latestVersion = tag.hasPrefix("v") ? String(tag.dropFirst()) : tag
            let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"

            DispatchQueue.main.async {
                if latestVersion.compare(currentVersion, options: .numeric) == .orderedDescending {
                    completion(.available(UpdateInfo(version: latestVersion, downloadURL: downloadURL)))
                } else {
                    completion(.upToDate(currentVersion))
                }
            }
        }.resume()
    }

    static func downloadAndInstall(_ info: UpdateInfo, completion: @escaping (String) -> Void) {
        URLSession.shared.downloadTask(with: info.downloadURL) { tempURL, _, error in
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
                    completion("Updated to v\(info.version). Relaunching…")
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
