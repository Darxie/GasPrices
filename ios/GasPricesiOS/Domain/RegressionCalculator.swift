import Foundation

struct PriceEntry: Equatable {
    let x: Double
    let y: Double
}

struct LinearRegressionResult: Equatable {
    let slope: Double
    let intercept: Double
}

enum RegressionCalculator {
    static func calculateSlopeAndIntercept(dataPoints: [PriceEntry]) -> LinearRegressionResult {
        let n = Double(dataPoints.count)
        guard n > 0 else {
            return LinearRegressionResult(slope: 0, intercept: 0)
        }

        let sumX = dataPoints.reduce(0) { $0 + $1.x }
        let sumY = dataPoints.reduce(0) { $0 + $1.y }
        let sumXX = dataPoints.reduce(0) { $0 + ($1.x * $1.x) }
        let sumXY = dataPoints.reduce(0) { $0 + ($1.x * $1.y) }

        let denominator = n * sumXX - sumX * sumX
        let slope = denominator != 0 ? (n * sumXY - sumX * sumY) / denominator : 0
        let intercept = (sumY - slope * sumX) / n

        return LinearRegressionResult(slope: slope, intercept: intercept)
    }
}