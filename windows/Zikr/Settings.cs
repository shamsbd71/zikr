using System;
using System.Collections.Generic;
using System.IO;
using System.Web.Script.Serialization;
using Microsoft.Win32;

namespace Zikr
{
    /// <summary>
    /// Persisted preferences, backed by a JSON file under %APPDATA%\Zikr.
    /// Mirrors the macOS build's AppSettings and the Linux build's
    /// settings.py: one small file, no database. Launch-at-login is
    /// handled via the standard per-user Registry Run key, and its
    /// source of truth is whether that key actually exists - not the
    /// cached JSON - same pattern as SMAppService.isEnabled on macOS.
    /// </summary>
    public sealed class Settings
    {
        private const string RunKeyPath = @"Software\Microsoft\Windows\CurrentVersion\Run";
        private const string RunValueName = "Zikr";

        private readonly string _configDir;
        private readonly string _configFile;

        public string ConfigFile => _configFile;

        public bool Enabled { get; set; } = true;
        public int MinIntervalMinutes { get; set; } = 20;
        public int MaxIntervalMinutes { get; set; } = 45;
        public string DisplayStyle { get; set; } = "notification"; // "notification" | "flash"
        public bool SpeakAloud { get; set; } = true;
        public double FlashDurationSeconds { get; set; } = 2.0;

        /// <summary>Skip a scheduled reminder while the microphone is in
        /// use - by this app or any other, e.g. a call in Zoom or a
        /// browser tab - so Zikr never talks over a meeting.</summary>
        public bool PauseDuringCalls { get; set; } = true;

        /// <summary>Version dismissed via "Skip This Version" in the
        /// update dialog - the automatic background check won't
        /// re-prompt for it, though a manual "Check for Updates…" still
        /// will. Empty means none.</summary>
        public string SkippedUpdateVersion { get; set; } = "";

        /// <summary>Turn reminders completely off during a daily time
        /// window (e.g. overnight) - unlike PauseDuringCalls, this is a
        /// fixed schedule rather than a live signal.</summary>
        public bool QuietHoursEnabled { get; set; } = false;

        /// <summary>Minutes since midnight (0-1439), local time. When
        /// start > end the window wraps past midnight (e.g.
        /// 22:00-06:00).</summary>
        public int QuietHoursStartMinutes { get; set; } = 22 * 60;
        public int QuietHoursEndMinutes { get; set; } = 6 * 60;

        private bool _launchAtLogin;
        public bool LaunchAtLogin
        {
            get => _launchAtLogin;
            set
            {
                _launchAtLogin = value;
                SetAutostart(value);
            }
        }

        private readonly string _runKeyPath;

        /// <summary>
        /// configDir/runKeyPath are injectable so tests don't touch the
        /// real %APPDATA%\Zikr folder or the real HKCU Run key - the
        /// production TrayApp just uses the parameterless constructor.
        /// </summary>
        public Settings(string configDir = null, string runKeyPath = null)
        {
            _configDir = configDir ?? Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Zikr");
            _configFile = Path.Combine(_configDir, "settings.json");
            _runKeyPath = runKeyPath ?? RunKeyPath;

            Load();
            _launchAtLogin = IsAutostartEnabled();
        }

        public void SetMinInterval(int minutes)
        {
            MinIntervalMinutes = minutes;
            if (MinIntervalMinutes > MaxIntervalMinutes) MaxIntervalMinutes = MinIntervalMinutes;
            Save();
        }

        public void SetMaxInterval(int minutes)
        {
            MaxIntervalMinutes = minutes;
            if (MaxIntervalMinutes < MinIntervalMinutes) MinIntervalMinutes = MaxIntervalMinutes;
            Save();
        }

        private void Load()
        {
            if (!File.Exists(_configFile)) return;
            try
            {
                string json = File.ReadAllText(_configFile, System.Text.Encoding.UTF8);
                var serializer = new JavaScriptSerializer();
                var data = serializer.Deserialize<Dictionary<string, object>>(json);
                if (data == null) return;

                if (data.TryGetValue("enabled", out var v1)) Enabled = Convert.ToBoolean(v1);
                if (data.TryGetValue("min_interval_minutes", out var v2)) MinIntervalMinutes = Convert.ToInt32(v2);
                if (data.TryGetValue("max_interval_minutes", out var v3)) MaxIntervalMinutes = Convert.ToInt32(v3);
                if (data.TryGetValue("display_style", out var v4)) DisplayStyle = Convert.ToString(v4);
                if (data.TryGetValue("speak_aloud", out var v5)) SpeakAloud = Convert.ToBoolean(v5);
                if (data.TryGetValue("flash_duration_seconds", out var v6)) FlashDurationSeconds = Convert.ToDouble(v6);
                if (data.TryGetValue("pause_during_calls", out var v7)) PauseDuringCalls = Convert.ToBoolean(v7);
                if (data.TryGetValue("skipped_update_version", out var v8)) SkippedUpdateVersion = Convert.ToString(v8);
                if (data.TryGetValue("quiet_hours_enabled", out var v9)) QuietHoursEnabled = Convert.ToBoolean(v9);
                if (data.TryGetValue("quiet_hours_start_minutes", out var v10)) QuietHoursStartMinutes = Convert.ToInt32(v10);
                if (data.TryGetValue("quiet_hours_end_minutes", out var v11)) QuietHoursEndMinutes = Convert.ToInt32(v11);
            }
            catch (Exception)
            {
                // Corrupt or unreadable config - fall back to defaults rather than crash.
            }
        }

        public void Save()
        {
            Directory.CreateDirectory(_configDir);
            var data = new Dictionary<string, object>
            {
                ["enabled"] = Enabled,
                ["min_interval_minutes"] = MinIntervalMinutes,
                ["max_interval_minutes"] = MaxIntervalMinutes,
                ["display_style"] = DisplayStyle,
                ["speak_aloud"] = SpeakAloud,
                ["flash_duration_seconds"] = FlashDurationSeconds,
                ["pause_during_calls"] = PauseDuringCalls,
                ["skipped_update_version"] = SkippedUpdateVersion,
                ["quiet_hours_enabled"] = QuietHoursEnabled,
                ["quiet_hours_start_minutes"] = QuietHoursStartMinutes,
                ["quiet_hours_end_minutes"] = QuietHoursEndMinutes,
            };
            var serializer = new JavaScriptSerializer();
            File.WriteAllText(_configFile, serializer.Serialize(data), System.Text.Encoding.UTF8);
        }

        private bool IsAutostartEnabled()
        {
            using (var key = Registry.CurrentUser.OpenSubKey(_runKeyPath, writable: false))
            {
                return key?.GetValue(RunValueName) != null;
            }
        }

        private void SetAutostart(bool enabled)
        {
            using (var key = Registry.CurrentUser.CreateSubKey(_runKeyPath))
            {
                if (key == null) return;
                if (enabled)
                {
                    string exePath = System.Reflection.Assembly.GetExecutingAssembly().Location;
                    key.SetValue(RunValueName, $"\"{exePath}\"");
                }
                else
                {
                    key.DeleteValue(RunValueName, throwOnMissingValue: false);
                }
            }
        }
    }
}
