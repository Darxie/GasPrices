package cz.feldis.gasprices.utils

import com.github.mikephil.charting.data.Entry

object RegressionCalculator {

    data class LinearRegressionResult(val slope: Float, val intercept: Float)

    fun calculateSlopeAndIntercept(dataPoints: List<Entry>): LinearRegressionResult {
        val n = dataPoints.size
        if (n == 0) return LinearRegressionResult(0f, 0f)

        val sumX = dataPoints.fold(0f) { sum, entry -> sum + entry.x }
        val sumY = dataPoints.fold(0f) { sum, entry -> sum + entry.y }
        val sumXX = dataPoints.fold(0f) { sum, entry -> sum + entry.x * entry.x }
        val sumXY = dataPoints.fold(0f) { sum, entry -> sum + entry.x * entry.y }

        val denominator = n * sumXX - sumX * sumX
        
        val slope = if (denominator != 0f) (n * sumXY - sumX * sumY) / denominator else 0f
        val intercept = (sumY - slope * sumX) / n

        return LinearRegressionResult(slope, intercept)
    }
}