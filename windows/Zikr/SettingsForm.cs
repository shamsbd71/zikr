using System;
using System.Drawing;
using System.Windows.Forms;

namespace Zikr
{
    /// <summary>
    /// The Settings window. Mirrors SettingsView.swift / settings_window.py
    /// - same handful of options, nothing more.
    /// </summary>
    public sealed class SettingsForm : Form
    {
        private static SettingsForm _current;

        private readonly Settings _settings;
        private readonly Action _onChanged;

        private CheckBox _enabledBox;
        private NumericUpDown _minSpin;
        private NumericUpDown _maxSpin;
        private ComboBox _styleCombo;
        private NumericUpDown _durationSpin;
        private Label _durationLabel;
        private Panel _durationRow;
        private CheckBox _speakBox;
        private CheckBox _loginBox;

        public static void Show(Settings settings, Action onChanged)
        {
            if (_current != null)
            {
                _current.Activate();
                return;
            }
            _current = new SettingsForm(settings, onChanged);
            _current.FormClosed += (s, e) => _current = null;
            _current.Show();
        }

        private SettingsForm(Settings settings, Action onChanged)
        {
            _settings = settings;
            _onChanged = onChanged;

            Text = "Zikr Settings";
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(380, 400);
            Padding = new Padding(18);

            var layout = new FlowLayoutPanel
            {
                FlowDirection = FlowDirection.TopDown,
                Dock = DockStyle.Fill,
                AutoScroll = true,
                WrapContents = false,
            };
            Controls.Add(layout);

            _enabledBox = new CheckBox { Text = "Enable reminders", Checked = _settings.Enabled, AutoSize = true };
            _enabledBox.CheckedChanged += (s, e) => { _settings.Enabled = _enabledBox.Checked; _settings.Save(); _onChanged?.Invoke(); };
            layout.Controls.Add(_enabledBox);

            layout.Controls.Add(Section("Timing"));

            layout.Controls.Add(Row("Min minutes", out _minSpin, 1, 180, _settings.MinIntervalMinutes));
            _minSpin.ValueChanged += (s, e) =>
            {
                _settings.SetMinInterval((int)_minSpin.Value);
                if (_maxSpin.Value != _settings.MaxIntervalMinutes) _maxSpin.Value = _settings.MaxIntervalMinutes;
                _onChanged?.Invoke();
            };

            layout.Controls.Add(Row("Max minutes", out _maxSpin, 1, 180, _settings.MaxIntervalMinutes));
            _maxSpin.ValueChanged += (s, e) =>
            {
                _settings.SetMaxInterval((int)_maxSpin.Value);
                if (_minSpin.Value != _settings.MinIntervalMinutes) _minSpin.Value = _settings.MinIntervalMinutes;
                _onChanged?.Invoke();
            };

            layout.Controls.Add(Section("Appearance"));

            _styleCombo = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Width = 200 };
            _styleCombo.Items.AddRange(new object[] { "Notification", "Full-Screen Flash" });
            _styleCombo.SelectedIndex = _settings.DisplayStyle == "flash" ? 1 : 0;
            _styleCombo.SelectedIndexChanged += (s, e) =>
            {
                _settings.DisplayStyle = _styleCombo.SelectedIndex == 1 ? "flash" : "notification";
                _settings.Save();
                _durationRow.Visible = _durationLabel.Visible = _durationSpin.Visible = _settings.DisplayStyle == "flash";
                _onChanged?.Invoke();
            };
            layout.Controls.Add(LabeledRow("Style", _styleCombo));

            _durationRow = Row("Visible for (s)", out _durationSpin, 1, 8, (decimal)_settings.FlashDurationSeconds);
            _durationSpin.DecimalPlaces = 1;
            _durationSpin.Increment = 0.5m;
            _durationLabel = (Label)_durationRow.Controls[0];
            _durationSpin.ValueChanged += (s, e) =>
            {
                _settings.FlashDurationSeconds = (double)_durationSpin.Value;
                _settings.Save();
                _onChanged?.Invoke();
            };
            _durationRow.Visible = _durationLabel.Visible = _durationSpin.Visible = _settings.DisplayStyle == "flash";
            layout.Controls.Add(_durationRow);

            _speakBox = new CheckBox { Text = "Speak zikr aloud", Checked = _settings.SpeakAloud, AutoSize = true };
            _speakBox.CheckedChanged += (s, e) => { _settings.SpeakAloud = _speakBox.Checked; _settings.Save(); _onChanged?.Invoke(); };
            layout.Controls.Add(_speakBox);

            layout.Controls.Add(Section("System"));

            _loginBox = new CheckBox { Text = "Launch at login", Checked = _settings.LaunchAtLogin, AutoSize = true };
            _loginBox.CheckedChanged += (s, e) => { _settings.LaunchAtLogin = _loginBox.Checked; };
            layout.Controls.Add(_loginBox);

            layout.Controls.Add(Section(""));

            var testButton = new Button { Text = "Test Zikr (Speak + Flash)", AutoSize = true };
            testButton.Click += (s, e) =>
            {
                var z = ZikrData.RandomZikr();
                if (_settings.SpeakAloud) Speech.Speak(z);
                FlashForm.Present(z, _settings.FlashDurationSeconds);
            };
            layout.Controls.Add(testButton);

            var hint = new Label
            {
                Text = "Test always speaks the zikr aloud and flashes it on\nscreen, regardless of the Style setting above.",
                AutoSize = true,
                ForeColor = Color.Gray,
                Margin = new Padding(0, 8, 0, 0),
            };
            layout.Controls.Add(hint);
        }

        private static Label Section(string title)
        {
            return new Label
            {
                Text = title,
                Font = new Font("Segoe UI", 9, FontStyle.Bold),
                AutoSize = true,
                Margin = new Padding(0, 14, 0, 4),
            };
        }

        private static Panel LabeledRow(string label, Control control)
        {
            var panel = new Panel { Height = 28, Width = 340 };
            var lbl = new Label { Text = label, AutoSize = true, Location = new Point(0, 6), Width = 110 };
            control.Location = new Point(120, 2);
            panel.Controls.Add(lbl);
            panel.Controls.Add(control);
            return panel;
        }

        private static Panel Row(string label, out NumericUpDown spin, int min, int max, decimal value)
        {
            spin = new NumericUpDown { Minimum = min, Maximum = max, Value = value, Width = 80 };
            return LabeledRow(label, spin);
        }
    }
}
