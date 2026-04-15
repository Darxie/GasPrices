import Foundation

struct WeekRangeGenerator {
    var calendar: Calendar
    var nowProvider: () -> Date

    init(
        calendar: Calendar = WeekRangeGenerator.makeDefaultCalendar(),
        nowProvider: @escaping () -> Date = Date.init
    ) {
        self.calendar = calendar
        self.nowProvider = nowProvider
    }

    static func makeDefaultCalendar() -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = .current
        return calendar
    }

    func getLast30Weeks() -> String {
        let now = nowProvider()
        let currentWeekStart = startOfCurrentWeek(from: now)

        let weeks = (0..<30).map { offset -> String in
            let date = calendar.date(byAdding: .weekOfYear, value: -offset, to: currentWeekStart) ?? currentWeekStart
            let year = calendar.component(.year, from: date)
            let week = calendar.component(.weekOfYear, from: date)
            return String(format: "%04d%02d", year, week)
        }

        return weeks.reversed().joined(separator: ",")
    }

    private func startOfCurrentWeek(from date: Date) -> Date {
        let startOfDay = calendar.startOfDay(for: date)
        let weekday = calendar.component(.weekday, from: startOfDay)
        let delta = (weekday - calendar.firstWeekday + 7) % 7
        return calendar.date(byAdding: .day, value: -delta, to: startOfDay) ?? startOfDay
    }
}