import UserNotifications

enum NotificationManager {
    /// One fixed identifier for every reminder, so a new zikr *replaces*
    /// the previous one in Notification Center instead of stacking. With
    /// a fresh UUID per delivery they accumulated indefinitely — a day of
    /// reminders left a wall of unread badges to dismiss by hand, which
    /// is exactly the kind of chore this app is supposed to avoid.
    private static let identifier = "com.abu.ZikrReminder.reminder"

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

        let center = UNUserNotificationCenter.current()
        center.removeDeliveredNotifications(withIdentifiers: [identifier])
        center.add(UNNotificationRequest(identifier: identifier, content: content, trigger: nil))
    }

    /// Clears any reminder still sitting in Notification Center. Called at
    /// launch so upgrading users don't inherit the backlog the old
    /// unique-identifier behaviour left behind.
    static func clearDelivered() {
        UNUserNotificationCenter.current()
            .removeDeliveredNotifications(withIdentifiers: [identifier])
    }
}
