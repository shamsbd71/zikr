import AVFoundation

/// Speaks the zikr aloud. Every phrase in the list ships with a real
/// recitation (data/audio/<id>.mp3, bundled by build.sh), so that plays;
/// the built-in macOS Arabic voice ("Majed", on every Mac) remains the
/// fallback for anything without a clip, so a phrase is still pronounced
/// rather than chimed. Dropping a file at Resources/Audio/<id>.mp3 (or
/// .m4a/.caf/.wav) overrides the bundled clip — see README.
///
/// The clips are cut from the Hisn al-Muslim recitation published by
/// dua.gtaf.org; tools/fetch_audio.py documents how and from where.
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
