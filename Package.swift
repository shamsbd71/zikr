// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "ZikrReminder",
    platforms: [.macOS(.v14)],
    targets: [
        .executableTarget(
            name: "ZikrReminder",
            path: "Sources/ZikrReminder"
        ),
        .executableTarget(
            name: "IconGen",
            path: "Sources/IconGen"
        ),
        .executableTarget(
            name: "BannerGen",
            path: "Sources/BannerGen"
        )
    ]
)
