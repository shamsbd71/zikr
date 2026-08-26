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
            Show(ZikrData.RandomZikr());
            if (_settings.Enabled) ScheduleNext();
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
