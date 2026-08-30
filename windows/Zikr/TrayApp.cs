using System;
using System.Drawing;
using System.Windows.Forms;

namespace Zikr
{
    /// <summary>
    /// Wires up the tray icon, its menu, and the scheduler. Mirrors
    /// ZikrReminderApp.swift + MenuContentView.swift / app.py.
    /// Tray-only app: no main window, driven by ApplicationContext.
    /// </summary>
    public sealed class TrayApp : ApplicationContext
    {
        private readonly Settings _settings = new Settings();
        private readonly Scheduler _scheduler;
        private readonly NotifyIcon _icon;
        // A handle-forced, never-shown Control purely so background
        // threads have something reliable to BeginInvoke back onto -
        // there's no main Form in a tray-only ApplicationContext app.
        // (SynchronizationContext.Current isn't a safe alternative here:
        // it's only installed once some Control's handle exists, which
        // hasn't happened yet this early - capturing it in a field
        // initializer, which runs before Application.Run even starts,
        // would just capture null.)
        private readonly Control _uiMarshal = new Control();
        private readonly Timer _updateTimer = new Timer { Interval = 24 * 60 * 60 * 1000 };
        private ToolStripMenuItem _activeItem;

        public TrayApp()
        {
            _uiMarshal.CreateControl();

            _scheduler = new Scheduler(_settings);

            _icon = new NotifyIcon
            {
                Icon = LoadIcon(),
                Text = "Zikr",
                Visible = true,
                ContextMenuStrip = BuildMenu(),
            };
            _icon.DoubleClick += (s, e) => _scheduler.FireNow();

            Notifications.Attach(_icon);

            _scheduler.Start();

            var startupTimer = new Timer { Interval = 5000 };
            startupTimer.Tick += (s, e) => { startupTimer.Stop(); CheckForUpdatesAutomatically(); };
            startupTimer.Start();

            _updateTimer.Tick += (s, e) => CheckForUpdatesAutomatically();
            _updateTimer.Start();
        }

        private static Icon LoadIcon()
        {
            string exeDir = System.IO.Path.GetDirectoryName(
                System.Reflection.Assembly.GetExecutingAssembly().Location) ?? ".";
            string path = System.IO.Path.Combine(exeDir, "Resources", "icon.ico");
            return System.IO.File.Exists(path) ? new Icon(path) : SystemIcons.Application;
        }

        private ContextMenuStrip BuildMenu()
        {
            var menu = new ContextMenuStrip();

            _activeItem = new ToolStripMenuItem("Active") { CheckOnClick = true, Checked = _settings.Enabled };
            _activeItem.CheckedChanged += (s, e) =>
            {
                _settings.Enabled = _activeItem.Checked;
                _settings.Save();
                _scheduler.OnSettingsChanged();
            };
            menu.Items.Add(_activeItem);

            var test = new ToolStripMenuItem("Test Zikr (Speak + Flash)");
            test.Click += (s, e) => _scheduler.FireNow();
            menu.Items.Add(test);

            menu.Items.Add(new ToolStripSeparator());

            var settings = new ToolStripMenuItem("Settings…");
            settings.Click += (s, e) => SettingsForm.Show(_settings, _scheduler.OnSettingsChanged);
            menu.Items.Add(settings);

            var whatsNew = new ToolStripMenuItem("What's New…");
            whatsNew.Click += (s, e) => ShowWhatsNew();
            menu.Items.Add(whatsNew);

            var update = new ToolStripMenuItem("Check for Updates…");
            update.Click += (s, e) => CheckForUpdatesManually();
            menu.Items.Add(update);

            menu.Items.Add(new ToolStripSeparator());

            var quit = new ToolStripMenuItem("Quit Zikr");
            quit.Click += (s, e) => ExitApp();
            menu.Items.Add(quit);

            return menu;
        }

        private void ExitApp()
        {
            _icon.Visible = false;
            _scheduler.Stop();
            _updateTimer.Stop();
            _uiMarshal.Dispose();
            Application.Exit();
        }

        // ---- updates ----

        private void ShowWhatsNew()
        {
            System.Threading.Tasks.Task.Run(() =>
            {
                var entries = Changelog.Fetch();
                _uiMarshal.BeginInvoke(new Action(() => new WhatsNewForm(entries).Show()));
            });
        }

        private void CheckForUpdatesManually()
        {
            System.Threading.Tasks.Task.Run(() =>
            {
                var result = UpdateChecker.CheckForUpdate();
                _uiMarshal.BeginInvoke(new Action(() => HandleUpdateResult(result, force: true)));
            });
        }

        private void CheckForUpdatesAutomatically()
        {
            System.Threading.Tasks.Task.Run(() =>
            {
                var result = UpdateChecker.CheckForUpdate();
                _uiMarshal.BeginInvoke(new Action(() => HandleUpdateResult(result, force: false)));
            });
        }

        private void HandleUpdateResult(UpdateChecker.CheckResult result, bool force)
        {
            if (result.Status == UpdateChecker.Status.Error)
            {
                if (force) MessageBox.Show(result.Message, "Zikr Update");
                return;
            }

            if (result.Status == UpdateChecker.Status.UpToDate)
            {
                if (force) MessageBox.Show($"You're up to date (v{result.Version}).", "Zikr Update");
                return;
            }

            // Available
            if (!force && result.Version == _settings.SkippedUpdateVersion) return;

            System.Threading.Tasks.Task.Run(() =>
            {
                var entries = Changelog.Fetch();
                string body = entries.Find(en => en.Version == result.Version)?.Body ?? "";
                _uiMarshal.BeginInvoke(new Action(() =>
                {
                    new UpdateAvailableForm(UpdateChecker.CurrentVersion, result.Version, body, onSkip: () =>
                    {
                        _settings.SkippedUpdateVersion = result.Version;
                        _settings.Save();
                    }).Show();
                }));
            });
        }
    }
}
