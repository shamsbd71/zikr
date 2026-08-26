using System;
using System.IO;
using System.Linq;
using System.Media;
using System.Reflection;
using System.Speech.Synthesis;

namespace Zikr
{
    /// <summary>
    /// Speaks a zikr aloud. If a real recording is bundled for this phrase
    /// (Resources/audio/&lt;id&gt;.wav), that plays instead; otherwise falls
    /// back to SAPI text-to-speech via System.Speech - built into every
    /// Windows install, no extra dependency. Most Windows machines don't
    /// have an Arabic SAPI voice installed by default (unlike macOS, which
    /// ships one), so this looks for one and falls back to reading the
    /// transliteration in English if none is found - same layered fallback
    /// as the macOS and Linux builds.
    /// </summary>
    public static class Speech
    {
        private static readonly SpeechSynthesizer _synth = new SpeechSynthesizer();

        public static void Speak(ZikrItem zikr)
        {
            _synth.SpeakAsyncCancelAll();

            string clip = FindBundledClip(zikr.Id);
            if (clip != null)
            {
                try
                {
                    var player = new SoundPlayer(clip);
                    player.Play();
                    return;
                }
                catch
                {
                    // fall through to TTS
                }
            }

            var arabicVoice = _synth.GetInstalledVoices()
                .FirstOrDefault(v => v.Enabled && v.VoiceInfo.Culture.TwoLetterISOLanguageName == "ar");

            if (arabicVoice != null)
            {
                _synth.SelectVoice(arabicVoice.VoiceInfo.Name);
                _synth.Rate = -2;
                _synth.SpeakAsync(zikr.Arabic);
            }
            else
            {
                _synth.SelectVoiceByHints(VoiceGender.NotSet, VoiceAge.NotSet);
                _synth.Rate = -1;
                _synth.SpeakAsync(zikr.Transliteration);
            }
        }

        private static string FindBundledClip(int zikrId)
        {
            string exeDir = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location) ?? ".";
            string path = Path.Combine(exeDir, "Resources", "audio", $"{zikrId}.wav");
            return File.Exists(path) ? path : null;
        }
    }
}
