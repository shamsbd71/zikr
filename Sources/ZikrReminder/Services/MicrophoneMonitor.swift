import CoreAudio
import Foundation

/// Detects whether any process — this app or another, e.g. Zoom or a
/// browser tab in a Google Meet call — currently has an active audio
/// input stream open. This is the same public CoreAudio signal macOS's
/// own orange mic-in-use menu bar dot is driven by, so it reflects
/// system-wide mic use, not just this app's.
enum MicrophoneMonitor {
    static var isInUse: Bool {
        guard let deviceID = defaultInputDeviceID() else { return false }
        return isDeviceRunning(deviceID)
    }

    private static func defaultInputDeviceID() -> AudioDeviceID? {
        var deviceID = AudioDeviceID(0)
        var size = UInt32(MemoryLayout<AudioDeviceID>.size)
        var address = AudioObjectPropertyAddress(
            mSelector: kAudioHardwarePropertyDefaultInputDevice,
            mScope: kAudioObjectPropertyScopeGlobal,
            mElement: kAudioObjectPropertyElementMain
        )
        let status = AudioObjectGetPropertyData(
            AudioObjectID(kAudioObjectSystemObject), &address, 0, nil, &size, &deviceID
        )
        guard status == noErr, deviceID != kAudioObjectUnknown else { return nil }
        return deviceID
    }

    /// `kAudioDevicePropertyDeviceIsRunningSomewhere` is true while any
    /// client — including a different process — is actively reading
    /// from the device, not just while this process is.
    private static func isDeviceRunning(_ deviceID: AudioDeviceID) -> Bool {
        var isRunning: UInt32 = 0
        var size = UInt32(MemoryLayout<UInt32>.size)
        var address = AudioObjectPropertyAddress(
            mSelector: kAudioDevicePropertyDeviceIsRunningSomewhere,
            mScope: kAudioObjectPropertyScopeGlobal,
            mElement: kAudioObjectPropertyElementMain
        )
        let status = AudioObjectGetPropertyData(deviceID, &address, 0, nil, &size, &isRunning)
        return status == noErr && isRunning != 0
    }
}
