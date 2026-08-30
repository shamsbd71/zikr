import SwiftUI

/// The in-app changelog — pulls the same CHANGELOG.md the repo and site
/// use, so there's always somewhere to see what changed without leaving
/// the app.
struct WhatsNewView: View {
    @State private var entries: [ChangelogEntry] = []
    @State private var isLoading = true

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("What's New in Zikr")
                .font(.headline)
                .padding([.horizontal, .top], 20)
                .padding(.bottom, 12)

            Divider()

            if isLoading {
                VStack {
                    Spacer()
                    ProgressView()
                    Spacer()
                }
                .frame(maxWidth: .infinity)
            } else if entries.isEmpty {
                VStack {
                    Spacer()
                    Text("Couldn't load the changelog. Check your internet connection.")
                        .font(.system(size: 13))
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(20)
                    Spacer()
                }
            } else {
                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        ForEach(entries) { entry in
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(spacing: 8) {
                                    Text("v\(entry.version)").font(.system(size: 13, weight: .bold))
                                    Text(entry.date).font(.system(size: 11)).foregroundStyle(.secondary)
                                }
                                Text(entry.body)
                                    .font(.system(size: 12.5))
                                    .foregroundStyle(.secondary)
                                    .textSelection(.enabled)
                            }
                        }
                    }
                    .padding(20)
                }
            }
        }
        .frame(width: 440, height: 420)
        .onAppear {
            ChangelogFetcher.fetch { fetched in
                entries = fetched
                isLoading = false
            }
        }
    }
}
