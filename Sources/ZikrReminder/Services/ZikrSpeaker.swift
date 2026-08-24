import AVFoundation

/// Speaks the zikr aloud using the built-in macOS Arabic voice (ships as
/// "Majed" on every Mac) so the phrase is pronounced correctly, not just
/// chimed. Falls back to reading the transliteration in the default
/// English voice on the rare system without an Arabic voice installed.
final class ZikrSpeaker {
    static let shared = ZikrSpeaker()

    private let synthesizer = AVSpeechSynthesizer()
    private let arabicVoice = AVSpeechSynthesisVoice.speechVoices().first { $0.language.hasPrefix("ar") }

    private init() {}

    func speak(_ zikr: Zikr) {
        synthesizer.stopSpeaking(at: .immediate)

        let utterance: AVSpeechUtterance
        if let arabicVoice {
            utterance = AVSpeechUtterance(string: zikr.arabic)
            utterance.voice = arabicVoice
            utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.85
        } else {
            utterance = AVSpeechUtterance(string: zikr.transliteration)
            utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
            utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9
        }
        synthesizer.speak(utterance)
    }
}
