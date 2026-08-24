import UserNotifications

enum NotificationManager {
    static func requestAuthorizationIfNeeded() {
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            guard settings.authorizationStatus == .notDetermined else { return }
            center.requestAuthorization(options: [.alert, .sound]) { _, _ in }
        }
    }

    static func deliver(_ zikr: Zikr) {
        let content = UNMutableNotificationContent()
        content.title = zikr.transliteration
        content.subtitle = zikr.arabic
        content.body = zikr.translation
        // No banner sound — ZikrSpeaker already speaks the zikr aloud.
        content.sound = nil

        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request)
    }
}
