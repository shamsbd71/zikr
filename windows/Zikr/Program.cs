using System;
using System.Threading;
using System.Windows.Forms;

namespace Zikr
{
    internal static class Program
    {
        [STAThread]
        private static void Main()
        {
            // Prevent a second instance - matches the single-tray-icon
            // expectation of MenuBarExtra / a single AppIndicator.
            using (var singleInstance = new Mutex(true, "Zikr-SingleInstance", out bool createdNew))
            {
                if (!createdNew) return;

                Application.EnableVisualStyles();
                Application.SetCompatibleTextRenderingDefault(false);
                Application.Run(new TrayApp());
            }
        }
    }
}
