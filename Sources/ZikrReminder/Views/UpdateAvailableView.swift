import SwiftUI

/// Shared with UpdateAvailableController so the "Installing…" status can
/// be pushed into the view from outside as the download progresses.
final class UpdateFlowViewModel: ObservableObject {
    @Published var isInstalling = false
    @Published var statusText = ""
}

/// The "a new version is available" dialog — the familiar Sparkle-style
/// layout most Mac apps use (Skip / Remind Later / Install), plus an
/// inline changelog preview so you can see what's new before installing.
struct UpdateAvailableView: View {
    let currentVersion: String
    let info: UpdateChecker.UpdateInfo
    let changelogBody: String
    @ObservedObject var viewModel: UpdateFlowViewModel
    @ObservedObject private var settings = AppSettings.shared

    var onSkip: () -> Void
    var onRemindLater: () -> Void
    var onInstall: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 14) {
                Image(nsImage: NSApp.applicationIconImage)
                    .resizable()
                    .frame(width: 56, height: 56)
                VStack(alignment: .leading, spacing: 4) {
                    Text("A new version of Zikr is available!")
                        .font(.headline)
                    Text("Zikr \(info.version) is now available — you have \(currentVersion). Would you like to download it now?")
                        .font(.system(size: 12.5))
                        .foregroundStyle(.secondary)
                }
            }

            if !changelogBody.isEmpty {
                ScrollView {
                    Text(changelogBody)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .textSelection(.enabled)
                }
                .frame(height: 130)
                .background(Color(nsColor: .textBackgroundColor))
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.primary.opacity(0.1)))
            }

            Toggle("Automatically download and install updates in the future", isOn: $settings.autoInstallUpdates)
                .font(.system(size: 12))
                .disabled(viewModel.isInstalling)

            if viewModel.isInstalling {
                HStack(spacing: 8) {
                    ProgressView().controlSize(.small)
                    Text(viewModel.statusText)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .center)
            } else {
                HStack {
                    Button("Skip This Version", action: onSkip)
                    Spacer()
                    Button("Remind Me Later", action: onRemindLater)
                    Button("Install Update", action: onInstall)
                        .keyboardShortcut(.defaultAction)
                        .buttonStyle(.borderedProminent)
                        .tint(.green)
                }
            }
        }
        .padding(20)
        .frame(width: 440)
    }
}
