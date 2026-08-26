using System.Diagnostics;

namespace Zikr
{
    /// <summary>
    /// No in-place self-updater, same decision as the Linux build and for
    /// the same reason: installs vary (Inno Setup to Program Files, a
    /// portable extraction, a future winget/choco package), so silently
    /// overwriting files isn't a sound default. Opens the releases page
    /// instead.
    /// </summary>
    public static class UpdateChecker
    {
        private const string ReleasesUrl = "https://github.com/shamsbd71/zikr/releases/latest";

        public static void OpenReleasesPage()
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = ReleasesUrl,
                UseShellExecute = true,
            });
        }
    }
}
