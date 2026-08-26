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
        private ToolStripMenuItem _activeItem;

        public TrayApp()
        {
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

            var update = new ToolStripMenuItem("Check for Updates…");
            update.Click += (s, e) => UpdateChecker.OpenReleasesPage();
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
            Application.Exit();
        }
    }
}
