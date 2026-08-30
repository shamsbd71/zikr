using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;

namespace Zikr
{
    /// <summary>Lists the app's changelog inline - same content as
    /// CHANGELOG.md in the repo, fetched fresh each time.</summary>
    public sealed class WhatsNewForm : Form
    {
        public WhatsNewForm(List<ChangelogEntry> entries)
        {
            Text = "What's New";
            FormBorderStyle = FormBorderStyle.FixedDialog;
            MaximizeBox = false;
            MinimizeBox = false;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(440, 420);
            Padding = new Padding(18);

            var layout = new FlowLayoutPanel
            {
                FlowDirection = FlowDirection.TopDown,
                Dock = DockStyle.Fill,
                AutoScroll = true,
                WrapContents = false,
            };
            Controls.Add(layout);

            if (entries.Count == 0)
            {
                layout.Controls.Add(new Label
                {
                    Text = "Couldn't load the changelog. Check your internet connection.",
                    AutoSize = true,
                    MaximumSize = new Size(390, 0),
                });
                return;
            }

            foreach (var entry in entries)
            {
                layout.Controls.Add(new Label
                {
                    Text = $"v{entry.Version}   {entry.Date}",
                    Font = new Font("Segoe UI", 9, FontStyle.Bold),
                    AutoSize = true,
                    Margin = new Padding(0, 14, 0, 4),
                });
                layout.Controls.Add(new Label
                {
                    Text = entry.Body,
                    AutoSize = true,
                    MaximumSize = new Size(390, 0),
                    ForeColor = Color.DimGray,
                });
            }
        }
    }
}
