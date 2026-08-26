using System.Windows.Forms;

namespace Zikr
{
    /// <summary>
    /// Desktop notifications via the tray icon's balloon tip. Windows 10+
    /// automatically renders these as modern Action Center toasts - no
    /// MSIX packaging or AppUserModelID registration needed, which is
    /// otherwise required for an unpackaged Win32 app to use the modern
    /// toast API directly. Mirrors NotificationManager.swift /
    /// notifications.py.
    /// </summary>
    public static class Notifications
    {
        private static NotifyIcon _icon;

        public static void Attach(NotifyIcon icon)
        {
            _icon = icon;
        }

        public static void Deliver(ZikrItem zikr)
        {
            if (_icon == null) return;
            string body = $"{zikr.Arabic}\n{zikr.Translation}";
            _icon.BalloonTipTitle = zikr.Transliteration;
            _icon.BalloonTipText = body;
            _icon.ShowBalloonTip(6000);
        }
    }
}
