import AVFoundation

/// Speaks the zikr aloud. If a real recording is bundled for this phrase
/// (drop one at Resources/Audio/<id>.mp3|m4a|caf|wav — see README), that
/// plays instead; otherwise it falls back to the built-in macOS Arabic
/// voice (ships as "Majed" on every Mac) so it's still pronounced
/// correctly, not just chimed. We don't bundle third-party reciter audio
/// ourselves — the well-known Hisnul Muslim recordings we found are full
/// multi-dua CD tracks with no clear per-phrase reuse license, so bundling
/// them without a verified license isn't something we do on your behalf.
final class ZikrSpeaker {
    static let shared = ZikrSpeaker()

    private let synthesizer = AVSpeechSynthesizer()
    private var audioPlayer: AVAudioPlayer?
    private let arabicVoice = AVSpeechSynthesisVoice.speechVoices().first { $0.language.hasPrefix("ar") }

    private init() {}

    func speak(_ zikr: Zikr) {
        synthesizer.stopSpeaking(at: .immediate)
        audioPlayer?.stop()

        if let clipURL = bundledAudioURL(for: zikr) {
            playClip(at: clipURL, fallback: zikr)
        } else {
            speakWithVoice(zikr)
        }
    }

    private func bundledAudioURL(for zikr: Zikr) -> URL? {
        let name = String(zikr.id)
        for ext in ["m4a", "mp3", "caf", "wav", "aiff"] {
            if let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: "Audio") {
                return url
            }
        }
        return nil
    }

    private func playClip(at url: URL, fallback zikr: Zikr) {
        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.prepareToPlay()
            player.play()
            audioPlayer = player
        } catch {
            speakWithVoice(zikr)
        }
    }

    private func speakWithVoice(_ zikr: Zikr) {
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
