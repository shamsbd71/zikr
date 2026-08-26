using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Web.Script.Serialization;

namespace Zikr
{
    /// <summary>
    /// Loads the bundled zikr list. Same 21 general adhkar as the macOS
    /// build's ZikrList.swift and the Linux build's data/zikr.json (kept
    /// in sync by hand).
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
            string json = File.ReadAllText(path, System.Text.Encoding.UTF8);
            var serializer = new JavaScriptSerializer();
            var raw = serializer.Deserialize<List<Dictionary<string, object>>>(json);

            return raw.Select(d => new ZikrItem
            {
                Id = Convert.ToInt32(d["id"]),
                Arabic = (string)d["arabic"],
                Transliteration = (string)d["transliteration"],
                Translation = (string)d["translation"],
            }).ToList();
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
