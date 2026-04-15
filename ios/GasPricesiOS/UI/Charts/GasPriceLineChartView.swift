import DGCharts
import SwiftUI
import UIKit

struct GasPriceLineChartView: UIViewRepresentable {
    let response: GasPricesResponse
    let showRegressionLines: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> LineChartView {
        let lineChart = LineChartView()
        setupChart(lineChart)
        lineChart.marker = context.coordinator.markerView
        return lineChart
    }

    func updateUIView(_ lineChart: LineChartView, context: Context) {
        let labels = GasPriceTransformer.weekLabels(from: response)
        let fullWeeksLabels = labels.full
        let trimmedWeeksLabels = labels.trimmed

        guard !fullWeeksLabels.isEmpty else {
            lineChart.clear()
            return
        }

        let values = response.value
        let entries95 = toChartEntries(GasPriceTransformer.createEntries(values: values, typeIndex: 0, numWeeks: fullWeeksLabels.count))
        let entries98 = toChartEntries(GasPriceTransformer.createEntries(values: values, typeIndex: 1, numWeeks: fullWeeksLabels.count))
        let entriesDiesel = toChartEntries(GasPriceTransformer.createEntries(values: values, typeIndex: 2, numWeeks: fullWeeksLabels.count))

        let chartTextColor = UIColor.white
        let color95 = UIColor(red: 1.0, green: 0.7569, blue: 0.0275, alpha: 1.0)
        let color98 = UIColor(red: 0.2980, green: 0.6863, blue: 0.3137, alpha: 1.0)
        let colorDiesel = UIColor(red: 0.1294, green: 0.5882, blue: 0.9529, alpha: 1.0)

        var dataSets: [LineChartDataSet] = []

        let dataSet95 = createLineDataSet(entries: entries95, label: AppStrings.octane95Label, color: color95, valueTextColor: chartTextColor)
        dataSets.append(dataSet95)
        if showRegressionLines && !entries95.isEmpty {
            dataSets.append(calculateRegressionLine(entries: entries95, color: color95))
        }

        let dataSet98 = createLineDataSet(entries: entries98, label: AppStrings.octane98Label, color: color98, valueTextColor: chartTextColor)
        dataSets.append(dataSet98)
        if showRegressionLines && !entries98.isEmpty {
            dataSets.append(calculateRegressionLine(entries: entries98, color: color98))
        }

        let dataSetDiesel = createLineDataSet(entries: entriesDiesel, label: AppStrings.dieselLabel, color: colorDiesel, valueTextColor: chartTextColor)
        dataSets.append(dataSetDiesel)
        if showRegressionLines && !entriesDiesel.isEmpty {
            dataSets.append(calculateRegressionLine(entries: entriesDiesel, color: colorDiesel))
        }

        let lineData = LineChartData(dataSets: dataSets)
        lineData.setValueTextColor(chartTextColor)
        lineChart.data = lineData

        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(values: trimmedWeeksLabels)
        lineChart.xAxis.setLabelCount(5, force: true)
        lineChart.xAxis.axisMinimum = -0.5
        lineChart.xAxis.axisMaximum = max(Double(fullWeeksLabels.count) - 0.5, 0)
        lineChart.xAxis.granularity = 1
        lineChart.xAxis.labelRotationAngle = -45

        context.coordinator.markerView.update(weekLabels: fullWeeksLabels)
        lineChart.marker = context.coordinator.markerView
        lineChart.notifyDataSetChanged()
        lineChart.setNeedsDisplay()
    }

    private func setupChart(_ lineChart: LineChartView) {
        let chartTextColor = UIColor.white

        lineChart.chartDescription.enabled = false
        lineChart.setScaleEnabled(true)
        lineChart.dragEnabled = true
        lineChart.pinchZoomEnabled = true
        lineChart.drawGridBackgroundEnabled = false

        lineChart.xAxis.labelTextColor = chartTextColor
        lineChart.leftAxis.labelTextColor = chartTextColor
        lineChart.rightAxis.labelTextColor = chartTextColor
        lineChart.legend.textColor = chartTextColor
    }

    private func toChartEntries(_ entries: [PriceEntry]) -> [ChartDataEntry] {
        entries.map { ChartDataEntry(x: $0.x, y: $0.y) }
    }

    private func createLineDataSet(
        entries: [ChartDataEntry],
        label: String,
        color: UIColor,
        valueTextColor: UIColor
    ) -> LineChartDataSet {
        let dataSet = LineChartDataSet(entries: entries, label: label)
        dataSet.setColor(color)
        dataSet.lineWidth = 5
        dataSet.drawCirclesEnabled = true
        dataSet.circleRadius = 2
        dataSet.drawCircleHoleEnabled = false
        dataSet.setCircleColor(color)
        dataSet.drawValuesEnabled = true
        dataSet.valueTextColor = valueTextColor
        return dataSet
    }

    private func calculateRegressionLine(entries: [ChartDataEntry], color: UIColor) -> LineChartDataSet {
        let points = entries.map { PriceEntry(x: $0.x, y: $0.y) }
        let result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints: points)
        let regressionEntries = entries.map { entry in
            ChartDataEntry(x: entry.x, y: result.slope * entry.x + result.intercept)
        }

        let regressionDataSet = LineChartDataSet(entries: regressionEntries, label: "")
        regressionDataSet.form = Legend.Form.none
        regressionDataSet.drawCirclesEnabled = false
        regressionDataSet.setColor(color)
        regressionDataSet.lineWidth = 3
        regressionDataSet.drawValuesEnabled = false
        regressionDataSet.highlightEnabled = false
        regressionDataSet.lineDashLengths = [10, 5]
        regressionDataSet.lineDashPhase = 0
        return regressionDataSet
    }

    final class Coordinator {
        let markerView = GasPriceMarkerView(weekLabels: [])
    }
}