import XCTest
@testable import GasPricesiOS

final class WeekRangeGeneratorTests: XCTestCase {
    func testGetLast30WeeksReturnsExpectedShapeAndCurrentWeekLast() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = Locale(identifier: "sk_SK")
        calendar.firstWeekday = 2

        let fixedDate = Date(timeIntervalSince1970: 1_744_720_000)
        let generator = WeekRangeGenerator(calendar: calendar, nowProvider: { fixedDate })

        let weeks = generator.getLast30Weeks().split(separator: ",").map(String.init)

        XCTAssertEqual(weeks.count, 30)
        XCTAssertTrue(weeks.allSatisfy { $0.range(of: "^\\d{6}$", options: .regularExpression) != nil })

        let expectedCurrentWeek = expectedCurrentWeekCode(date: fixedDate, calendar: calendar)
        XCTAssertEqual(weeks.last, expectedCurrentWeek)
    }

    private func expectedCurrentWeekCode(date: Date, calendar: Calendar) -> String {
        let startOfDay = calendar.startOfDay(for: date)
        let weekday = calendar.component(.weekday, from: startOfDay)
        let delta = (weekday - calendar.firstWeekday + 7) % 7
        let currentWeekStart = calendar.date(byAdding: .day, value: -delta, to: startOfDay) ?? startOfDay

        let year = calendar.component(.year, from: currentWeekStart)
        let week = calendar.component(.weekOfYear, from: currentWeekStart)
        return String(format: "%04d%02d", year, week)
    }
}