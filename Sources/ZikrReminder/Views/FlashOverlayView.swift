import SwiftUI

struct FlashOverlayView: View {
    let zikr: Zikr

    /// Amiri (SIL OFL 1.1), bundled at Resources/Fonts and registered via
    /// ATSApplicationFontsPath. The system font shapes Arabic poorly —
    /// especially fully vocalised text like this, where the harakat
    /// collide — so the Arabic line gets a real Naskh face. If the font
    /// somehow fails to register, .custom falls back to the system font
    /// rather than failing to draw.
    private static let arabicFont = "Amiri"

    /// Caps how wide the card can grow. Without it a long dua lays out as
    /// one enormous line — the morning/evening adhkar are 15 words and
    /// would run the full width of the display.
    static let contentWidth: CGFloat = 640

    /// Widest the panel can be: content plus the horizontal padding below.
    static let maxCardWidth: CGFloat = contentWidth + 64 * 2

    var body: some View {
        VStack(spacing: 18) {
            Text(zikr.arabic)
                .font(.custom(Self.arabicFont, size: 54))
                .lineSpacing(14)
                .multilineTextAlignment(.center)
                .frame(maxWidth: Self.contentWidth)
                .fixedSize(horizontal: false, vertical: true)
                .foregroundStyle(
                    LinearGradient(
                        colors: [Color(red: 0.99, green: 0.98, blue: 0.94),
                                 Color(red: 0.90, green: 0.85, blue: 0.72)],
                        startPoint: .top, endPoint: .bottom
                    )
                )
                .shadow(color: .black.opacity(0.45), radius: 12, y: 3)
                .environment(\.layoutDirection, .rightToLeft)

            Rectangle()
                .fill(
                    LinearGradient(
                        colors: [.clear, Color(red: 0.83, green: 0.71, blue: 0.44).opacity(0.55), .clear],
                        startPoint: .leading, endPoint: .trailing
                    )
                )
                .frame(width: 190, height: 1)

            Text(zikr.transliteration)
                .font(.system(size: 21, weight: .medium, design: .rounded))
                .foregroundStyle(Color(red: 0.83, green: 0.71, blue: 0.44))
                .multilineTextAlignment(.center)
                .frame(maxWidth: Self.contentWidth)
                .fixedSize(horizontal: false, vertical: true)

            // fixedSize lets this wrap to as many lines as it needs; without
            // it the longer translations get truncated with an ellipsis.
            Text(zikr.translation)
                .font(.system(size: 15))
                .foregroundStyle(.white.opacity(0.68))
                .multilineTextAlignment(.center)
                .lineSpacing(3)
                .frame(maxWidth: Self.contentWidth - 60)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 64)
        .padding(.vertical, 48)
        .background {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color(red: 0.05, green: 0.15, blue: 0.16),
                                 Color(red: 0.02, green: 0.08, blue: 0.10)],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    )
                )
                .overlay {
                    // Faint warm bloom behind the Arabic, so the card reads
                    // as lit rather than flat.
                    RadialGradient(
                        colors: [Color(red: 0.83, green: 0.71, blue: 0.44).opacity(0.16), .clear],
                        center: .top, startRadius: 8, endRadius: 320
                    )
                }
        }
        .overlay {
            RoundedRectangle(cornerRadius: 32, style: .continuous)
                .strokeBorder(
                    LinearGradient(
                        colors: [Color(red: 0.83, green: 0.71, blue: 0.44).opacity(0.55),
                                 Color(red: 0.83, green: 0.71, blue: 0.44).opacity(0.12)],
                        startPoint: .topLeading, endPoint: .bottomTrailing
                    ),
                    lineWidth: 1
                )
        }
        .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
        .shadow(color: .black.opacity(0.55), radius: 48, y: 18)
    }
}
