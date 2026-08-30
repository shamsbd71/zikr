using System;
using System.Collections.Generic;
using System.Net;

namespace Zikr
{
    public sealed class ChangelogEntry
    {
        public string Version;
        public string Date;
        public string Body;
    }

    /// <summary>
    /// Fetches and parses CHANGELOG.md from the repo's main branch,
    /// mirroring ChangelogFetcher.swift / changelog.py, so "What's New"
    /// and the update dialog show the same text as the file in the repo.
    /// </summary>
    public static class Changelog
    {
        private const string RawUrl = "https://raw.githubusercontent.com/shamsbd71/zikr/main/CHANGELOG.md";

        public static List<ChangelogEntry> Fetch()
        {
            try
            {
                using (var client = new WebClient())
                {
                    client.Headers.Add("User-Agent", "Zikr-Updater");
                    string text = client.DownloadString(RawUrl);
                    return Parse(text);
                }
            }
            catch (Exception)
            {
                return new List<ChangelogEntry>();
            }
        }

        /// <summary>Pure function: splits on "## [version] — date"
        /// headers.</summary>
        public static List<ChangelogEntry> Parse(string markdown)
        {
            var entries = new List<ChangelogEntry>();
            string currentVersion = null;
            string currentDate = "";
            var currentBody = new List<string>();

            void Flush()
            {
                if (currentVersion != null && !currentVersion.Equals("Unreleased", StringComparison.OrdinalIgnoreCase))
                {
                    entries.Add(new ChangelogEntry
                    {
                        Version = currentVersion,
                        Date = currentDate,
                        Body = string.Join("\n", currentBody).Trim(),
                    });
                }
            }

            foreach (var rawLine in markdown.Replace("\r\n", "\n").Split('\n'))
            {
                if (rawLine.StartsWith("## ["))
                {
                    Flush();
                    currentBody = new List<string>();
                    string afterBracket = rawLine.Substring(4);
                    int closeBracket = afterBracket.IndexOf(']');
                    if (closeBracket >= 0)
                    {
                        currentVersion = afterBracket.Substring(0, closeBracket);
                        currentDate = afterBracket.Substring(closeBracket + 1).Replace("—", "").Trim();
                    }
                    else
                    {
                        currentVersion = null;
                    }
                }
                else if (currentVersion != null)
                {
                    currentBody.Add(rawLine);
                }
            }
            Flush();
            return entries;
        }
    }
}
