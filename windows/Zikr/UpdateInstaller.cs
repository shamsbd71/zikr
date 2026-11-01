using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Threading.Tasks;

namespace Zikr
{
    /// <summary>
    /// Downloads the release's Inno Setup installer and runs it silently.
    /// The installer is per-user (no admin prompt), closes the running
    /// Zikr, replaces it, and starts the new version again (see the
    /// WizardSilent [Run] entry in ZikrSetup.iss). Zikr never writes over
    /// its own files itself.
    /// </summary>
    public static class UpdateInstaller
    {
        /// <summary>Silent-install switches passed to Inno Setup. Pure, so
        /// the tests pin them down.</summary>
        public const string SilentArgs = "/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /CLOSEAPPLICATIONS /SP-";

        /// <summary>Downloads on a background thread. Reports 0-100 (or -1
        /// when the size is unknown) through <paramref name="progress"/>,
        /// and returns the downloaded installer's path.</summary>
        public static Task<string> DownloadAsync(string url, string version, Action<int> progress)
        {
            return Task.Run(() =>
            {
                // GitHub requires TLS 1.2; older Windows builds don't offer
                // it to .NET Framework apps by default.
                ServicePointManager.SecurityProtocol |= SecurityProtocolType.Tls12;

                string dir = Path.Combine(Path.GetTempPath(), "ZikrUpdate");
                Directory.CreateDirectory(dir);
                string dest = Path.Combine(dir, $"ZikrSetup-{version}.exe");
                string partial = dest + ".part";
                if (File.Exists(partial)) File.Delete(partial);

                var request = (HttpWebRequest)WebRequest.Create(url);
                request.UserAgent = "Zikr-Updater";
                request.AllowAutoRedirect = true;
                request.Timeout = 30000;
                using (var response = (HttpWebResponse)request.GetResponse())
                using (var input = response.GetResponseStream())
                using (var output = File.Create(partial))
                {
                    long total = response.ContentLength;
                    long read = 0;
                    int last = -2;
                    var buffer = new byte[64 * 1024];
                    int n;
                    while ((n = input.Read(buffer, 0, buffer.Length)) > 0)
                    {
                        output.Write(buffer, 0, n);
                        read += n;
                        int pct = ProgressPercent(read, total);
                        if (pct != last) { last = pct; progress?.Invoke(pct); }
                    }
                    if (total > 0 && read != total) throw new IOException("Download incomplete");
                }

                if (File.Exists(dest)) File.Delete(dest);
                File.Move(partial, dest);
                return dest;
            });
        }

        /// <summary>0-100 when the total is known, -1 otherwise.</summary>
        public static int ProgressPercent(long read, long total)
        {
            if (total <= 0) return -1;
            long pct = read * 100 / total;
            return (int)Math.Max(0, Math.Min(100, pct));
        }

        /// <summary>Starts the installer. The caller exits the app right
        /// after, so the installer can replace Zikr.exe.</summary>
        public static void RunInstaller(string installerPath)
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = installerPath,
                Arguments = SilentArgs,
                UseShellExecute = true,
            });
        }
    }
}
