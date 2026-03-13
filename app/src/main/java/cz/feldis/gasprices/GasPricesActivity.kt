package cz.feldis.gasprices

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import cz.feldis.gasprices.models.GasPricesResponse
import cz.feldis.gasprices.utils.RegressionCalculator
import java.util.Locale

class GasPricesActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var lineChart: LineChart
    private lateinit var tvPrice95: TextView
    private lateinit var tvPrice98: TextView
    private lateinit var tvPriceDiesel: TextView

    private lateinit var viewModel: GasPriceViewModel

    private var latestResponse: GasPricesResponse? = null
    private var showRegressionLines = true

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        setTheme(R.style.Theme_GasPrices)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gas_prices)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        statusTextView = findViewById(R.id.textViewStatus)
        progressBar = findViewById(R.id.progressBar)
        lineChart = findViewById(R.id.chart)
        tvPrice95 = findViewById(R.id.tvPrice95)
        tvPrice98 = findViewById(R.id.tvPrice98)
        tvPriceDiesel = findViewById(R.id.tvPriceDiesel)

        setupChart()

        val apiService = ServiceBuilder.apiService
        val repository = GasPriceRepository(apiService)
        viewModel = ViewModelProvider(this, GasPriceViewModelFactory(repository))[GasPriceViewModel::class.java]

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) ProgressBar.VISIBLE else ProgressBar.GONE
            if (isLoading) {
                statusTextView.text = getString(R.string.loading_status)
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            if (!error.isNullOrBlank()) {
                statusTextView.text = error
            }
        }

        viewModel.gasPrices.observe(this) { response ->
            if (response == null) {
                return@observe
            }
            latestResponse = response
            statusTextView.text = "${response.label}\n${getString(R.string.updated_label, response.update)}"
            updateHeaderPrices(response.value)
            displayChartData(response)
        }

        refreshPrices()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_gas_prices, menu)
        menu?.findItem(R.id.action_toggle_regression)?.isChecked = showRegressionLines
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshPrices()
                true
            }
            R.id.action_toggle_regression -> {
                showRegressionLines = !showRegressionLines
                item.isChecked = showRegressionLines
                latestResponse?.let { displayChartData(it) }
                true
            }
            R.id.action_share -> {
                shareLatestPrices()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshPrices() {
        statusTextView.text = getString(R.string.loading_status)
        viewModel.loadGasPrices()
    }

    private fun shareLatestPrices() {
        val response = latestResponse
        if (response == null) {
            statusTextView.text = getString(R.string.data_not_ready_message)
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, buildShareText(response))
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_prompt)))
    }

    private fun buildShareText(response: GasPricesResponse): String {
        val latestWeekLabel = response.dimension.sp0207ts_tyz.category.label.values.firstOrNull() ?: ""
        val latestPrices = latestAvailableFuelPrices(response.value)

        return buildString {
            appendLine(response.label)
            if (latestWeekLabel.isNotBlank()) {
                appendLine(latestWeekLabel)
            }
            appendLine("${getString(R.string.octane_95_label)}: ${formatPrice(latestPrices.gasoline95)} EUR/l")
            appendLine("${getString(R.string.octane_98_label)}: ${formatPrice(latestPrices.gasoline98)} EUR/l")
            appendLine("${getString(R.string.diesel_label)}: ${formatPrice(latestPrices.diesel)} EUR/l")
            appendLine(getString(R.string.updated_label, response.update))
            appendLine(getString(R.string.source_label, response.href))
        }.trim()
    }

    private fun displayChartData(gasPricesResponse: GasPricesResponse) {
        latestResponse = gasPricesResponse
        val allWeeksLabels = gasPricesResponse.dimension.sp0207ts_tyz.category.label.values.toList()
        val fullWeeksLabels = allWeeksLabels.reversed()
        val numWeeks = fullWeeksLabels.size

        if (numWeeks == 0) {
            statusTextView.text = getString(R.string.no_data_available)
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        val weeksLabels = fullWeeksLabels.map { label ->
            if (label.contains("(") && label.contains(")")) {
                label.substringAfter("(").substringBefore(")")
            } else {
                label
            }
        }

        val values = gasPricesResponse.value
        val entries95 = createEntries(values, 0, numWeeks)
        val entries98 = createEntries(values, 1, numWeeks)
        val entriesDiesel = createEntries(values, 2, numWeeks)

        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val chartTextColor = if (isNightMode) Color.WHITE else Color.BLACK

        val color95 = Color.parseColor("#FFC107")
        val color98 = Color.parseColor("#4CAF50")
        val colorDiesel = Color.parseColor("#2196F3")

        val dataSet95 = createLineDataSet(entries95, getString(R.string.octane_95_label), color95, chartTextColor)
        val dataSet98 = createLineDataSet(entries98, getString(R.string.octane_98_label), color98, chartTextColor)
        val dataSetDiesel = createLineDataSet(entriesDiesel, getString(R.string.diesel_label), colorDiesel, chartTextColor)

        val lineData = LineData().apply {
            addDataSet(dataSet95)
            if (showRegressionLines && entries95.isNotEmpty()) {
                addDataSet(calculateRegressionLine(entries95, dataSet95.color))
            }
            addDataSet(dataSet98)
            if (showRegressionLines && entries98.isNotEmpty()) {
                addDataSet(calculateRegressionLine(entries98, dataSet98.color))
            }
            addDataSet(dataSetDiesel)
            if (showRegressionLines && entriesDiesel.isNotEmpty()) {
                addDataSet(calculateRegressionLine(entriesDiesel, dataSetDiesel.color))
            }
            setValueTextColor(chartTextColor)
        }

        lineChart.data = lineData
        lineChart.xAxis.valueFormatter = WeekAxisValueFormatter(weeksLabels)
        lineChart.marker = GasPriceMarkerView(
            context = this,
            weeksLabels = fullWeeksLabels
        )
        lineChart.xAxis.setLabelCount(5, true)
        lineChart.xAxis.setAxisMinimum(-0.5f)
        lineChart.xAxis.setAxisMaximum((numWeeks - 0.5f).coerceAtLeast(0f))
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.labelRotationAngle = -45f
        lineChart.invalidate()
    }

    private fun createLineDataSet(entries: List<Entry>, label: String, color: Int, valueTextColor: Int): LineDataSet {
        return LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 5f
            setDrawValues(true)
            this.valueTextColor = valueTextColor
        }
    }

    private fun updateHeaderPrices(values: List<Float?>) {
        val latestPrices = latestAvailableFuelPrices(values)
        tvPrice95.text = formatPrice(latestPrices.gasoline95, addCurrency = true)
        tvPrice98.text = formatPrice(latestPrices.gasoline98, addCurrency = true)
        tvPriceDiesel.text = formatPrice(latestPrices.diesel, addCurrency = true)
    }

    private fun latestAvailableFuelPrices(values: List<Float?>): FuelPriceTriple {
        val groupCount = values.size / 3
        for (groupIndex in 0 until groupCount) {
            val baseIndex = groupIndex * 3
            val price95 = values.getOrNull(baseIndex)
            val price98 = values.getOrNull(baseIndex + 1)
            val priceDiesel = values.getOrNull(baseIndex + 2)
            if (price95 != null || price98 != null || priceDiesel != null) {
                return FuelPriceTriple(price95, price98, priceDiesel)
            }
        }
        return FuelPriceTriple(null, null, null)
    }

    private fun formatPrice(value: Float?, addCurrency: Boolean = false): String {
        return value?.let {
            val formatted = String.format(Locale.getDefault(), "%.3f", it)
            if (addCurrency) "$formatted €" else formatted
        } ?: getString(R.string.not_available_label)
    }

    private data class FuelPriceTriple(val gasoline95: Float?, val gasoline98: Float?, val diesel: Float?)

    private fun createEntries(prices: List<Float?>, typeIndex: Int, numWeeks: Int): List<Entry> {
        val entries = prices.mapIndexedNotNull { index, price ->
            if (index % 3 == typeIndex) {
                val groupIndex = index / 3
                val xValue = (numWeeks - 1) - groupIndex.toFloat()
                price?.let { Entry(xValue, it) }
            } else {
                null
            }
        }
        return entries.sortedBy { it.x }
    }

    private fun setupChart() {
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val chartTextColor = if (isNightMode) Color.WHITE else Color.BLACK

        lineChart.apply {
            description.isEnabled = false
            setScaleEnabled(true)
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setDragEnabled(true)
            xAxis.textColor = chartTextColor
            axisLeft.textColor = chartTextColor
            axisRight.textColor = chartTextColor
            legend.textColor = chartTextColor
        }
    }

    class WeekAxisValueFormatter(private val weeksLabels: List<String>) : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val index = value.toInt().coerceIn(0, weeksLabels.size - 1)
            return weeksLabels.getOrNull(index) ?: ""
        }
    }

    fun calculateRegressionLine(dataPoints: List<Entry>, color: Int): LineDataSet {
        val result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints)
        val regressionEntries = dataPoints.map { Entry(it.x, (result.slope * it.x + result.intercept)) }
        return LineDataSet(regressionEntries, null).apply {
            form = Legend.LegendForm.NONE
            setDrawCircles(false)
            this.color = color
            lineWidth = 3f
            setDrawValues(false)
            isHighlightEnabled = false
            enableDashedLine(10f, 5f, 0f)
        }
    }

    private inner class GasPriceMarkerView(
        context: android.content.Context,
        private val weeksLabels: List<String>
    ) : MarkerView(context, R.layout.marker_gas_price) {
        private val markerText: TextView = findViewById(R.id.tvMarkerText)

        override fun refreshContent(e: Entry?, highlight: Highlight?) {
            if (e == null || highlight == null) {
                markerText.text = ""
                super.refreshContent(e, highlight)
                return
            }

            val weekIndex = e.x.toInt().coerceIn(0, weeksLabels.lastIndex)
            val weekLabel = weeksLabels[weekIndex]
            val datasetLabel = lineChart.lineData?.getDataSetByIndex(highlight.dataSetIndex)?.label
                ?: getString(R.string.price_label)
            val valueLabel = formatPrice(e.y)

            markerText.text = getString(
                R.string.marker_week_value_template,
                datasetLabel,
                weekLabel,
                valueLabel
            )
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF {
            return MPPointF(-(width / 2f), -height.toFloat() - 16f)
        }
    }
}
