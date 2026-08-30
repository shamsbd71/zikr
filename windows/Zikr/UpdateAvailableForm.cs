using System;
using System.Diagnostics;
using System.Drawing;
using System.Windows.Forms;

namespace Zikr
{
    /// <summary>
    /// The "a new version is available" dialog - mirrors
    /// UpdateAvailableView.swift / update_dialog.py's layout: Skip This
    /// Version / Remind Me Later / Download Update, with an inline
    /// changelog preview. No in-place install here either (see
    /// UpdateChecker) - Download Update opens the releases page.
    /// </summary>
    public sealed class UpdateAvailableForm : Form
    {
        public UpdateAvailableForm(string currentVersion, string newVersion, string changelogBody, Action onSkip)
        {
            Text = "Zikr Update";
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(440, 360);
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
                Text = $"Zikr {newVersion} is now available — you have {currentVersion}. Would you like to download it now?",
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
                    Size = new Size(400, 140),
                    Margin = new Padding(0, 0, 0, 14),
                });
            }

            var buttonRow = new FlowLayoutPanel
            {
                FlowDirection = FlowDirection.LeftToRight,
                AutoSize = true,
                WrapContents = false,
                Width = 400,
            };

            var skipBtn = new Button { Text = "Skip This Version", AutoSize = true };
            skipBtn.Click += (s, e) => { onSkip(); Close(); };
            buttonRow.Controls.Add(skipBtn);

            buttonRow.Controls.Add(new Panel { Width = 60, Height = 1 });

            var remindBtn = new Button { Text = "Remind Me Later", AutoSize = true };
            remindBtn.Click += (s, e) => Close();
            buttonRow.Controls.Add(remindBtn);

            var installBtn = new Button { Text = "Download Update", AutoSize = true };
            installBtn.Click += (s, e) =>
            {
                Process.Start(new ProcessStartInfo
                {
                    FileName = "https://github.com/shamsbd71/zikr/releases/latest",
                    UseShellExecute = true,
                });
                Close();
            };
            buttonRow.Controls.Add(installBtn);

            layout.Controls.Add(buttonRow);
        }
    }
}
