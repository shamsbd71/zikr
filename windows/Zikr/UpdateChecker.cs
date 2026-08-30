using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Net;
using System.Reflection;
using System.Web.Script.Serialization;

namespace Zikr
{
    /// <summary>
    /// Checks GitHub Releases for a newer tag than the running app's
    /// version. No in-place self-updater, same decision as the Linux
    /// build and for the same reason: installs vary (Inno Setup to
    /// Program Files, a portable extraction, a future winget/choco
    /// package), so silently overwriting files isn't a sound default.
    /// "Check for Updates" surfaces what changed and opens the releases
    /// page for you to download and reinstall.
    /// </summary>
    public static class UpdateChecker
    {
        private const string ReleasesUrl = "https://github.com/shamsbd71/zikr/releases/latest";
        private const string ApiUrl = "https://api.github.com/repos/shamsbd71/zikr/releases/latest";

        public enum Status { Available, UpToDate, Error }

        public sealed class CheckResult
        {
            public Status Status;
            public string Version;
            public string Message;
        }

        /// <summary>Reads the MSBuild `Version` property verbatim, as
        /// stamped into AssemblyInformationalVersionAttribute at build
        /// time via `-p:Version=X.Y.Z` (see release.yml/test.yml) -
        /// more direct than coercing through AssemblyVersion, which
        /// can't carry a value like "0.0.0-test".</summary>
        public static string CurrentVersion
        {
            get
            {
                var attrs = Assembly.GetExecutingAssembly()
                    .GetCustomAttributes(typeof(AssemblyInformationalVersionAttribute), false)
                    as AssemblyInformationalVersionAttribute[];
                return attrs != null && attrs.Length > 0 ? attrs[0].InformationalVersion : "0.0.0";
            }
        }

        public static CheckResult CheckForUpdate()
        {
            try
            {
                using (var client = new WebClient())
                {
                    client.Headers.Add("User-Agent", "Zikr-Updater");
                    client.Headers.Add("Accept", "application/vnd.github+json");
                    string json = client.DownloadString(ApiUrl);
                    var serializer = new JavaScriptSerializer();
                    var data = serializer.Deserialize<Dictionary<string, object>>(json);
                    string tag = data != null && data.TryGetValue("tag_name", out var t) ? Convert.ToString(t) : null;
                    if (string.IsNullOrEmpty(tag))
                        return new CheckResult { Status = Status.Error, Message = "Couldn't check for updates. Try again later." };

                    string latest = tag.StartsWith("v") ? tag.Substring(1) : tag;
                    string current = CurrentVersion;
                    if (CompareVersions(latest, current) > 0)
                        return new CheckResult { Status = Status.Available, Version = latest };
                    return new CheckResult { Status = Status.UpToDate, Version = current };
                }
            }
            catch (Exception)
            {
                return new CheckResult { Status = Status.Error, Message = "Couldn't check for updates. Try again later." };
            }
        }

        /// <summary>-1 if a&lt;b, 0 if equal, 1 if a&gt;b, comparing
        /// dot-separated numeric components.</summary>
        public static int CompareVersions(string a, string b)
        {
            int[] Parts(string v)
            {
                var segments = v.Split('.');
                var result = new int[segments.Length];
                for (int i = 0; i < segments.Length; i++)
                    int.TryParse(segments[i], out result[i]);
                return result;
            }

            var pa = Parts(a);
            var pb = Parts(b);
            int length = Math.Max(pa.Length, pb.Length);
            for (int i = 0; i < length; i++)
            {
                int va = i < pa.Length ? pa[i] : 0;
                int vb = i < pb.Length ? pb[i] : 0;
                if (va != vb) return va < vb ? -1 : 1;
            }
            return 0;
        }

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
