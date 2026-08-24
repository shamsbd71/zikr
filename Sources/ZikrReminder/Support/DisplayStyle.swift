import Foundation

enum DisplayStyle: String, CaseIterable, Identifiable {
    case notification
    case fullScreen

    var id: String { rawValue }

    var label: String {
        switch self {
        case .notification: return "Notification"
        case .fullScreen: return "Full-Screen Flash"
        }
    }
}
