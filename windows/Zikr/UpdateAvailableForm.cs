using System;
using System.Drawing;
using System.Windows.Forms;

namespace Zikr
{
    /// <summary>
    /// The "a new version is available" dialog - mirrors
    /// UpdateAvailableView.swift / update_dialog.py's layout: Skip This
    /// Version / Remind Me Later / Install Update, with an inline
    /// changelog preview. Install Update downloads the release's installer
    /// and runs it (see UpdateInstaller); if that fails, it offers the
    /// release page instead.
    /// </summary>
    public sealed class UpdateAvailableForm : Form
    {
        private readonly string _newVersion;
        private readonly string _installerUrl;
        private readonly Action _exitApp;
        private readonly Label _status;
        private readonly ProgressBar _progress;
        private readonly FlowLayoutPanel _buttonRow;
        private bool _installing;

        public UpdateAvailableForm(string currentVersion, string newVersion, string changelogBody,
            string installerUrl, Action onSkip, Action exitApp)
        {
            _newVersion = newVersion;
            _installerUrl = installerUrl;
            _exitApp = exitApp;

            Text = "Zikr Update";
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(440, 390);
            Padding = new Padding(18);

            var layout = new FlowLayoutPanel
            {
                FlowDirection = FlowDirection.TopDown,
                Dock = DockStyle.Fill,
                WrapContents = false,
            };
            Controls.Add(layout);

            layout.Controls.Add(new Label
            {
                Text = "A new version of Zikr is available!",
                Font = new Font("Segoe UI", 11, FontStyle.Bold),
                AutoSize = true,
                Margin = new Padding(0, 0, 0, 8),
            });

            layout.Controls.Add(new Label
            {
                Text = $"Zikr {newVersion} is now available — you have {currentVersion}. Would you like to install it now?",
                AutoSize = true,
                MaximumSize = new Size(400, 0),
                Margin = new Padding(0, 0, 0, 12),
            });

            if (!string.IsNullOrWhiteSpace(changelogBody))
            {
                layout.Controls.Add(new TextBox
                {
                    Multiline = true,
                    ReadOnly = true,
                    ScrollBars = ScrollBars.Vertical,
                    Text = changelogBody,
                    Size = new Size(400, 130),
                    Margin = new Padding(0, 0, 0, 10),
                });
            }

            _progress = new ProgressBar { Width = 400, Height = 14, Visible = false, Margin = new Padding(0, 0, 0, 6) };
            layout.Controls.Add(_progress);

            _status = new Label { AutoSize = true, MaximumSize = new Size(400, 0), Margin = new Padding(0, 0, 0, 8) };
            layout.Controls.Add(_status);

            _buttonRow = new FlowLayoutPanel
            {
                FlowDirection = FlowDirection.LeftToRight,
                AutoSize = true,
                WrapContents = false,
                Width = 400,
            };

            var skipBtn = new Button { Text = "Skip This Version", AutoSize = true };
            skipBtn.Click += (s, e) => { onSkip(); Close(); };
            _buttonRow.Controls.Add(skipBtn);

            _buttonRow.Controls.Add(new Panel { Width = 60, Height = 1 });

            var remindBtn = new Button { Text = "Remind Me Later", AutoSize = true };
            remindBtn.Click += (s, e) => Close();
            _buttonRow.Controls.Add(remindBtn);

            var installBtn = new Button { Text = "Install Update", AutoSize = true };
            installBtn.Click += (s, e) => StartInstall();
            _buttonRow.Controls.Add(installBtn);
            AcceptButton = installBtn;

            layout.Controls.Add(_buttonRow);
        }

        /// <summary>Downloads and runs the installer. Also called straight
        /// from the tray's "Update to vX…" item, so one click updates.</summary>
        public async void StartInstall()
        {
            if (_installing) return;
            if (string.IsNullOrEmpty(_installerUrl))
            {
                UpdateChecker.OpenReleasesPage();
                Close();
                return;
            }

            _installing = true;
            _buttonRow.Enabled = false;
            _progress.Visible = true;
            _progress.Style = ProgressBarStyle.Marquee;
            _status.ForeColor = SystemColors.ControlText;
            _status.Text = "Downloading…";

            try
            {
                string path = await UpdateInstaller.DownloadAsync(_installerUrl, _newVersion, pct =>
                {
                    if (IsDisposed) return;
                    BeginInvoke(new Action(() =>
                    {
                        if (pct < 0) { _progress.Style = ProgressBarStyle.Marquee; return; }
                        _progress.Style = ProgressBarStyle.Continuous;
                        _progress.Value = pct;
                        _status.Text = $"Downloading… {pct}%";
                    }));
                });

                _status.Text = "Installing… Zikr will restart by itself.";
                UpdateInstaller.RunInstaller(path);
                _exitApp();
            }
            catch (Exception)
            {
                _installing = false;
                _buttonRow.Enabled = true;
                _progress.Visible = false;
                _status.ForeColor = Color.Firebrick;
                _status.Text = "The update couldn't be downloaded. Check your connection and try again, " +
                               "or get it from the releases page.";
                var open = MessageBox.Show(this, "Open the releases page to download it yourself?", "Zikr Update",
                    MessageBoxButtons.YesNo, MessageBoxIcon.Warning);
                if (open == DialogResult.Yes) UpdateChecker.OpenReleasesPage();
            }
        }
    }
}
