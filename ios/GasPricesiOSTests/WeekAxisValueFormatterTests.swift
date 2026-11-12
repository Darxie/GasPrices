import XCTest
@testable import GasPricesiOS

final class WeekAxisValueFormatterTests: XCTestCase {
    func testFractionalAndOutOfRangePositionsMapToNearestWeek() {
        let labels = (0..<30).map { "w\($0)" }
        let formatter = GasPriceLineChartView.WeekAxisValueFormatter(labels: labels)

        // Positions the chart requests for 5 forced labels over -0.5...29.5.
        let result = [-0.5, 7, 14.5, 22, 29.5].map { formatter.stringForValue($0, axis: nil) }

        XCTAssertEqual(result, ["w0", "w7", "w14", "w22", "w29"])
    }

    func testEmptyLabelsReturnEmptyString() {
        let formatter = GasPriceLineChartView.WeekAxisValueFormatter(labels: [])
        XCTAssertEqual(formatter.stringForValue(3, axis: nil), "")
    }
}
