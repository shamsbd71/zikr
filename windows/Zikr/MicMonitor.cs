using System;
using Microsoft.Win32;

namespace Zikr
{
    /// <summary>
    /// Detects whether the microphone is currently in use by any app -
    /// this one or another, e.g. Zoom or a browser tab in a Google Meet
    /// call - via the same privacy ledger Settings -> Privacy -> Microphone
    /// reads from. Windows writes one subkey per app that has ever
    /// captured audio (packaged apps directly, legacy desktop apps under
    /// "NonPackaged"); LastUsedTimeStop stays 0 for as long as that app
    /// is still capturing.
    /// </summary>
    public static class MicMonitor
    {
        private const string ConsentStoreKeyPath =
            @"Software\Microsoft\Windows\CurrentVersion\CapabilityAccessManager\ConsentStore\microphone";

        /// <summary>consentStoreKeyPath is injectable so tests can point
        /// at a throwaway registry subtree instead of the real consent
        /// store - same pattern as Settings's runKeyPath.</summary>
        public static bool IsInUse(string consentStoreKeyPath = null)
        {
            string path = consentStoreKeyPath ?? ConsentStoreKeyPath;
            using (var micKey = Registry.CurrentUser.OpenSubKey(path))
            {
                if (micKey == null) return false;
                if (AnyAppSubkeyCapturing(micKey)) return true;

                using (var nonPackaged = micKey.OpenSubKey("NonPackaged"))
                {
                    return nonPackaged != null && AnyAppSubkeyCapturing(nonPackaged);
                }
            }
        }

        private static bool AnyAppSubkeyCapturing(RegistryKey parent)
        {
            foreach (var name in parent.GetSubKeyNames())
            {
                using (var appKey = parent.OpenSubKey(name))
                {
                    if (appKey != null && SubkeyIsCurrentlyCapturing(appKey)) return true;
                }
            }
            return false;
        }

        internal static bool SubkeyIsCurrentlyCapturing(RegistryKey appKey)
        {
            object start = appKey.GetValue("LastUsedTimeStart");
            object stop = appKey.GetValue("LastUsedTimeStop");
            if (start == null) return false;

            long startValue = Convert.ToInt64(start);
            long stopValue = stop == null ? 0 : Convert.ToInt64(stop);
            return startValue > 0 && stopValue == 0;
        }
    }
}
