using System;
using System.Windows.Forms;

namespace Zikr
{
    /// <summary>
    /// Drives the app: one WinForms Timer that fires at a random interval,
    /// shows a random zikr, then reschedules itself. Mirrors
    /// ReminderScheduler.swift / scheduler.py - no polling, no retained
    /// history.
    /// </summary>
    public sealed class Scheduler
    {
        private readonly Settings _settings;
        private readonly Timer _timer = new Timer();
        private static readonly Random _rng = new Random();

        public Scheduler(Settings settings)
        {
            _settings = settings;
            _timer.Tick += (s, e) => Fire();
        }

        /// <summary>Pure function so the random-interval math is
        /// unit-testable without a running message loop / timer.</summary>
        public static double PickDelaySeconds(int minIntervalMinutes, int maxIntervalMinutes)
        {
            int lo = Math.Max(1, minIntervalMinutes) * 60;
            int hi = Math.Max(lo, maxIntervalMinutes * 60);
            return lo + _rng.NextDouble() * (hi - lo);
        }

        public void Start()
        {
            if (_settings.Enabled) ScheduleNext();
        }

        public void Stop()
        {
            _timer.Stop();
        }

        public void OnSettingsChanged()
        {
            Stop();
            if (_settings.Enabled) ScheduleNext();
        }

        public void FireNow()
        {
            Show(ZikrData.RandomZikr());
        }

        private void ScheduleNext()
        {
            Stop();
            double delaySeconds = PickDelaySeconds(_settings.MinIntervalMinutes, _settings.MaxIntervalMinutes);
            int ms = (int)Math.Min(int.MaxValue, delaySeconds * 1000);
            _timer.Interval = Math.Max(1, ms);
            _timer.Start();
        }

        private void Fire()
        {
            bool inQuietHours = _settings.QuietHoursEnabled && IsWithinQuietHours(
                CurrentMinutesOfDay(), _settings.QuietHoursStartMinutes, _settings.QuietHoursEndMinutes);
            if (!(_settings.PauseDuringCalls && MicMonitor.IsInUse()) && !inQuietHours)
            {
                Show(ZikrData.RandomZikr());
            }
            if (_settings.Enabled) ScheduleNext();
        }

        /// <summary>Pure so the wraparound logic (a window like
        /// 22:00-06:00 that crosses midnight) is easy to test independent
        /// of the clock. Equal bounds means "no window" rather than
        /// "always on" - a user who hasn't set both ends yet shouldn't get
        /// silenced entirely by accident.</summary>
        public static bool IsWithinQuietHours(int nowMinutes, int startMinutes, int endMinutes)
        {
            if (startMinutes == endMinutes) return false;
            if (startMinutes < endMinutes) return nowMinutes >= startMinutes && nowMinutes < endMinutes;
            return nowMinutes >= startMinutes || nowMinutes < endMinutes;
        }

        private static int CurrentMinutesOfDay()
        {
            DateTime now = DateTime.Now;
            return now.Hour * 60 + now.Minute;
        }

        private void Show(ZikrItem zikr)
        {
            if (_settings.SpeakAloud) Speech.Speak(zikr);

            if (_settings.DisplayStyle == "flash")
                FlashForm.Present(zikr, _settings.FlashDurationSeconds);
            else
                Notifications.Deliver(zikr);
        }
    }
}
