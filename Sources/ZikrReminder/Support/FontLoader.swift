import AppKit
import CoreText
import os

/// Registers the bundled Amiri faces with Core Text at launch.
///
/// Info.plist's ATSApplicationFontsPath is the documented way to do this
/// and needs no code, but it did not actually take here — the flash card
/// still drew in the system font — so registration is explicit instead.
/// Verified working: CTFontManagerRegisterFontsForURL returns true and
/// NSFont(name: "Amiri") resolves afterwards.
enum FontLoader {
    private static let logger = Logger(subsystem: "com.abu.ZikrReminder", category: "fonts")

    static func registerBundledFonts() {
        for name in ["Amiri-Regular", "Amiri-Bold"] {
            guard let url = Bundle.main.url(forResource: name, withExtension: "ttf", subdirectory: "Fonts") else {
                logger.error("font missing from bundle: \(name, privacy: .public)")
                continue
            }
            var error: Unmanaged<CFError>?
            if CTFontManagerRegisterFontsForURL(url as CFURL, .process, &error) {
                logger.info("registered \(name, privacy: .public)")
            } else {
                // Already-registered is not a failure worth shouting about;
                // anything else means the flash card falls back to the
                // system font, which is the bug this exists to fix.
                logger.error("failed to register \(name, privacy: .public)")
            }
        }
        logger.info("Amiri resolves: \(NSFont(name: "Amiri", size: 24) != nil, privacy: .public)")
    }
}
