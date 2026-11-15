using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Web.Script.Serialization;

namespace Zikr
{
    /// <summary>
    /// Loads the bundled zikr list: the repo-root data/zikr.json every
    /// platform reads. Entries marked "greeting" are only the unlock
    /// greeting on macOS/Android, which this build doesn't have, so they
    /// are left out here.
    /// </summary>
    public static class ZikrData
    {
        private static readonly Lazy<List<ZikrItem>> _all = new Lazy<List<ZikrItem>>(Load);

        public static List<ZikrItem> All => _all.Value;

        private static readonly Random _rng = new Random();

        public static ZikrItem RandomZikr()
        {
            var all = All;
            return all[_rng.Next(all.Count)];
        }

        private static List<ZikrItem> Load()
        {
            string path = ResolveDataPath();
            return Parse(File.ReadAllText(path, System.Text.Encoding.UTF8));
        }

        /// <summary>Pure so it can be tested without the file.</summary>
        public static List<ZikrItem> Parse(string json)
        {
            var serializer = new JavaScriptSerializer();
            var raw = serializer.Deserialize<List<Dictionary<string, object>>>(json);

            return raw.Where(d => !IsGreeting(d)).Select(d => new ZikrItem
            {
                Id = Convert.ToInt32(d["id"]),
                Arabic = (string)d["arabic"],
                Transliteration = (string)d["transliteration"],
                Translation = (string)d["translation"],
            }).ToList();
        }

        private static bool IsGreeting(Dictionary<string, object> d)
        {
            object value;
            return d.TryGetValue("greeting", out value) && value is bool && (bool)value;
        }

        private static string ResolveDataPath()
        {
            string exeDir = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
            string candidate = Path.Combine(exeDir ?? ".", "Resources", "zikr.json");
            if (File.Exists(candidate)) return candidate;

            candidate = Path.Combine(exeDir ?? ".", "zikr.json");
            if (File.Exists(candidate)) return candidate;

            throw new FileNotFoundException("zikr.json not found next to the executable");
        }
    }
}
