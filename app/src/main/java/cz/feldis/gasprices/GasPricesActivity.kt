package cz.feldis.gasprices

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import cz.feldis.gasprices.models.GasPricesResponse

class GasPricesActivity : AppCompatActivity() {
    private lateinit var viewModel: GasPriceViewModel
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var lineChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GasPrices)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gas_prices)

        // Initialize UI components
        statusTextView = findViewById(R.id.textViewStatus)
        progressBar = findViewById(R.id.progressBar)
        lineChart = findViewById(R.id.chart)
        setupChart()

        val apiService = ServiceBuilder.apiService // Make sure ServiceBuilder is properly initialized
        val repository = GasPriceRepository(apiService)

        // Initialize ViewModel - often done via ViewModelProviders or an injection framework
        viewModel = ViewModelProvider(this, GasPriceViewModelFactory(repository))[GasPriceViewModel::class.java]

        viewModel.gasPrices.observe(this, Observer { response ->
            progressBar.visibility = View.GONE // Hide the progress bar

            if (response != null) {
                // Update your adapter and refresh the RecyclerView
                statusTextView.text = "${response.label}\nUpdated:  ${response.update}"
                setLineChartData(response)

            } else {
                statusTextView.text = "Failed to load data."
            }
        })

        progressBar.visibility = View.VISIBLE
        viewModel.loadGasPrices()
    }

    private fun setLineChartData(gasPricesResponse: GasPricesResponse) {
        val weeksLabels = gasPricesResponse.dimension.sp0207ts_tyz.category.label.values.toList()
        val values = gasPricesResponse.value

        val prices95 = values.filterIndexed { index, _ -> index % 3 == 0 }
        val prices98 = values.filterIndexed { index, _ -> index % 3 == 1 }
        val dieselPrices = values.filterIndexed { index, _ -> index % 3 == 2 }

        val dataSet95 = LineDataSet(createEntries(prices95), "95 Octane").apply {
            color = ColorTemplate.VORDIPLOM_COLORS[0]
            lineWidth = 5f
        }
        val dataSet98 = LineDataSet(createEntries(prices98), "98 Octane").apply {
            color = ColorTemplate.VORDIPLOM_COLORS[1]
            lineWidth = 5f
        }
        val dataSetDiesel = LineDataSet(createEntries(dieselPrices), "Diesel").apply {
            color = ColorTemplate.VORDIPLOM_COLORS[2]
            lineWidth = 5f
        }

        val entries95 = getEntriesFromDataSet(dataSet95)
        val entries98 = getEntriesFromDataSet(dataSet98)
        val entriesDiesel = getEntriesFromDataSet(dataSetDiesel)

        val regressionDataSet95 = calculateRegressionLine(entries95)
        val regressionDataSet98 = calculateRegressionLine(entries98)
        val regressionDataSetDiesel = calculateRegressionLine(entriesDiesel)

        val lineData = LineData()
        lineData.addDataSet(dataSet95)
//        lineData.addDataSet(regressionDataSet95)


        lineData.addDataSet(dataSet98)
//        lineData.addDataSet(regressionDataSet98)

        lineData.addDataSet(dataSetDiesel)
//        lineData.addDataSet(regressionDataSetDiesel)

        lineChart.data = lineData
        lineChart.xAxis.valueFormatter = WeekAxisValueFormatter(weeksLabels)
        lineChart.xAxis.setLabelCount(5, false)
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.labelRotationAngle = -45f
        lineChart.invalidate()
    }

    private fun createEntries(prices: List<Float?>): List<Entry> {
        return prices.mapIndexedNotNull { index, price ->
            price?.let { Entry(index.toFloat(), it) }
        }
    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false
            setScaleEnabled(true)
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(true)
            // ... Add more customization as needed
        }
    }

    class WeekAxisValueFormatter(private val weeksLabels: List<String>) : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return weeksLabels.getOrNull(value.toInt()) ?: ""
        }
    }

    fun calculateRegressionLine(dataPoints: List<Entry>): LineDataSet {
        val n = dataPoints.size
        val sumX = dataPoints.fold(0f) { sum, entry -> sum + entry.x }
        val sumY = dataPoints.fold(0f) { sum, entry -> sum + entry.y }
        val sumXX = dataPoints.fold(0f) { sum, entry -> sum + entry.x * entry.x }
        val sumXY = dataPoints.fold(0f) { sum, entry -> sum + entry.x * entry.y }

        val slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n

        val regressionEntries = dataPoints.map { Entry(it.x, (slope * it.x + intercept)) }
        val regressionDataSet = LineDataSet(regressionEntries, "Regression Line")
        regressionDataSet.setDrawCircles(false)
        regressionDataSet.color = ColorTemplate.VORDIPLOM_COLORS[4]
        regressionDataSet.setDrawValues(false)
        regressionDataSet.enableDashedLine(10f, 5f, 0f)

        return regressionDataSet
    }

    private fun getEntriesFromDataSet(dataSet: LineDataSet): MutableList<Entry> {
        val entries: MutableList<Entry> = mutableListOf()
        for (index in 0 until dataSet.entryCount) {
            dataSet.getEntryForIndex(index)?.let { entry ->
                entries.add(entry)
            }
        }
        return entries
    }
}
