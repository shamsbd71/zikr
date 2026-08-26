using System;
using System.Drawing;
using System.Windows.Forms;

namespace Zikr
{
    /// <summary>
    /// A borderless, always-on-top form that briefly shows a zikr centered
    /// on screen, then fades out on its own. Mirrors
    /// FlashOverlayController.swift / flash.py.
    /// </summary>
    public sealed class FlashForm : Form
    {
        private static FlashForm _current;

        private readonly Timer _fadeTimer = new Timer { Interval = 16 };
        private readonly Timer _dismissTimer = new Timer();
        private bool _fadingOut;

        public static void Present(ZikrItem zikr, double durationSeconds)
        {
            _current?.ForceClose();

            var form = new FlashForm(zikr, durationSeconds);
            _current = form;
            form.Show();
        }

        private FlashForm(ZikrItem zikr, double durationSeconds)
        {
            FormBorderStyle = FormBorderStyle.None;
            StartPosition = FormStartPosition.Manual;
            TopMost = true;
            ShowInTaskbar = false;
            BackColor = Color.FromArgb(14, 30, 27);
            Opacity = 0;

            var arabic = new Label
            {
                Text = zikr.Arabic,
                Font = new Font("Segoe UI", 22, FontStyle.Bold),
                ForeColor = Color.FromArgb(244, 213, 141),
                AutoSize = true,
                TextAlign = ContentAlignment.MiddleCenter,
            };
            var translit = new Label
            {
                Text = zikr.Transliteration,
                Font = new Font("Segoe UI", 13, FontStyle.Regular),
                ForeColor = Color.White,
                AutoSize = true,
                TextAlign = ContentAlignment.MiddleCenter,
            };
            var meaning = new Label
            {
                Text = zikr.Translation,
                Font = new Font("Segoe UI", 9.5f, FontStyle.Regular),
                ForeColor = Color.FromArgb(207, 207, 207),
                AutoSize = true,
                TextAlign = ContentAlignment.MiddleCenter,
            };

            var layout = new FlowLayoutPanel
            {
                FlowDirection = FlowDirection.TopDown,
                AutoSize = true,
                Padding = new Padding(40, 30, 40, 30),
                BackColor = Color.Transparent,
            };
            foreach (var lbl in new[] { arabic, translit, meaning })
            {
                lbl.Margin = new Padding(0, 6, 0, 6);
                lbl.Anchor = AnchorStyles.None;
                layout.Controls.Add(lbl);
            }
            Controls.Add(layout);

            AutoSize = true;
            AutoSizeMode = AutoSizeMode.GrowAndShrink;

            Load += (s, e) => CenterOnScreen();

            _fadeTimer.Tick += (s, e) => FadeStep();
            _fadeTimer.Start();

            _dismissTimer.Interval = Math.Max(200, (int)(durationSeconds * 1000));
            _dismissTimer.Tick += (s, e) =>
            {
                _dismissTimer.Stop();
                _fadingOut = true;
                _fadeTimer.Start();
            };
            _dismissTimer.Start();
        }

        private void CenterOnScreen()
        {
            var screen = Screen.PrimaryScreen.Bounds;
            Location = new Point(
                screen.Left + (screen.Width - Width) / 2,
                screen.Top + (screen.Height - Height) / 2);
        }

        private void FadeStep()
        {
            double step = 0.08;
            if (!_fadingOut)
            {
                Opacity = Math.Min(1.0, Opacity + step * 2);
                if (Opacity >= 1.0) _fadeTimer.Stop();
            }
            else
            {
                Opacity = Math.Max(0.0, Opacity - step);
                if (Opacity <= 0.0)
                {
                    _fadeTimer.Stop();
                    ForceClose();
                }
            }
        }

        private void ForceClose()
        {
            _fadeTimer.Stop();
            _dismissTimer.Stop();
            if (_current == this) _current = null;
            if (!IsDisposed) Close();
        }
    }
}
