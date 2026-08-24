import SwiftUI

struct FlashOverlayView: View {
    let zikr: Zikr

    var body: some View {
        VStack(spacing: 14) {
            Text(zikr.arabic)
                .font(.system(size: 46, weight: .semibold))
                .multilineTextAlignment(.center)

            Text(zikr.transliteration)
                .font(.system(size: 22, weight: .medium, design: .rounded))
                .foregroundStyle(.secondary)

            Text(zikr.translation)
                .font(.system(size: 15))
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, 56)
        .padding(.vertical, 44)
        .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 28, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .strokeBorder(Color.white.opacity(0.12), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.25), radius: 40, y: 10)
    }
}
