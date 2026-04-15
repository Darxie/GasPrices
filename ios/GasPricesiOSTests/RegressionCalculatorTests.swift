import XCTest
@testable import GasPricesiOS

final class RegressionCalculatorTests: XCTestCase {
    func testCalculateSlopeAndInterceptPerfectLine() {
        let dataPoints = [
            PriceEntry(x: 1, y: 1),
            PriceEntry(x: 2, y: 2),
            PriceEntry(x: 3, y: 3)
        ]

        let result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints: dataPoints)

        XCTAssertEqual(result.slope, 1, accuracy: 0.001)
        XCTAssertEqual(result.intercept, 0, accuracy: 0.001)
    }

    func testCalculateSlopeAndInterceptOffsetLine() {
        let dataPoints = [
            PriceEntry(x: 1, y: 3),
            PriceEntry(x: 2, y: 5),
            PriceEntry(x: 3, y: 7)
        ]

        let result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints: dataPoints)

        XCTAssertEqual(result.slope, 2, accuracy: 0.001)
        XCTAssertEqual(result.intercept, 1, accuracy: 0.001)
    }

    func testCalculateSlopeAndInterceptEmptyList() {
        let result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints: [])

        XCTAssertEqual(result.slope, 0, accuracy: 0.001)
        XCTAssertEqual(result.intercept, 0, accuracy: 0.001)
    }

    func testCalculateSlopeAndInterceptHorizontalLine() {
        let dataPoints = [
            PriceEntry(x: 1, y: 5),
            PriceEntry(x: 2, y: 5),
            PriceEntry(x: 3, y: 5)
        ]

        let result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints: dataPoints)

        XCTAssertEqual(result.slope, 0, accuracy: 0.001)
        XCTAssertEqual(result.intercept, 5, accuracy: 0.001)
    }
}