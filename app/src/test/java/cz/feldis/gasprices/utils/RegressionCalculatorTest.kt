package cz.feldis.gasprices.utils

import com.github.mikephil.charting.data.Entry
import org.junit.Assert.assertEquals
import org.junit.Test

class RegressionCalculatorTest {

    @Test
    fun calculateSlopeAndIntercept_perfectLine_returnsCorrectSlopeAndIntercept() {
        // Given points on line y = 1x + 0
        val dataPoints = listOf(
            Entry(1f, 1f),
            Entry(2f, 2f),
            Entry(3f, 3f)
        )

        // When
        val result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints)

        // Then
        assertEquals(1f, result.slope, 0.001f)
        assertEquals(0f, result.intercept, 0.001f)
    }

    @Test
    fun calculateSlopeAndIntercept_offsetLine_returnsCorrectSlopeAndIntercept() {
        // Given points on line y = 2x + 1
        val dataPoints = listOf(
            Entry(1f, 3f), // 2(1) + 1 = 3
            Entry(2f, 5f), // 2(2) + 1 = 5
            Entry(3f, 7f)  // 2(3) + 1 = 7
        )

        // When
        val result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints)

        // Then
        assertEquals(2f, result.slope, 0.001f)
        assertEquals(1f, result.intercept, 0.001f)
    }

    @Test
    fun calculateSlopeAndIntercept_emptyList_returnsZeroes() {
        // Given
        val dataPoints = emptyList<Entry>()

        // When
        val result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints)

        // Then
        assertEquals(0f, result.slope, 0.001f)
        assertEquals(0f, result.intercept, 0.001f)
    }

    @Test
    fun calculateSlopeAndIntercept_horizontalLine_returnsZeroSlope() {
        // Given points on line y = 5
        val dataPoints = listOf(
            Entry(1f, 5f),
            Entry(2f, 5f),
            Entry(3f, 5f)
        )

        // When
        val result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints)

        // Then
        assertEquals(0f, result.slope, 0.001f)
        assertEquals(5f, result.intercept, 0.001f)
    }
}