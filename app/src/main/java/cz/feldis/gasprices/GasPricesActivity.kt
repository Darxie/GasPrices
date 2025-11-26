package cz.feldis.gasprices

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import cz.feldis.gasprices.models.GasPricesResponse
import cz.feldis.gasprices.utils.RegressionCalculator

class GasPricesActivity : AppCompatActivity() {
    private lateinit var viewModel: GasPriceViewModel
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var lineChart: LineChart
    private lateinit var switchDarkMode: Switch
    
    // Header Price TextViews
    private lateinit var tvPrice95: TextView
    private lateinit var tvPrice98: TextView
    private lateinit var tvPriceDiesel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GasPrices)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gas_prices)

        // Initialize UI components
        statusTextView = findViewById(R.id.textViewStatus)
        progressBar = findViewById(R.id.progressBar)
        lineChart = findViewById(R.id.chart)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        
        tvPrice95 = findViewById(R.id.tvPrice95)
        tvPrice98 = findViewById(R.id.tvPrice98)
        tvPriceDiesel = findViewById(R.id.tvPriceDiesel)
        
        setupDarkModeSwitch()
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
                
                // Update header prices and chart
                updateHeaderPrices(response.value)
                setLineChartData(response)

            } else {
                statusTextView.text = "Failed to load data."
            }
        })

        progressBar.visibility = View.VISIBLE
        viewModel.loadGasPrices()
    }
    
    private fun updateHeaderPrices(values: List<Float?>) {
        // The values list is ordered newest to oldest.
        // Index 0 = 95 Octane (Newest)
        // Index 1 = 98 Octane (Newest)
        // Index 2 = Diesel (Newest)
        
        val price95 = values.getOrNull(0)
        val price98 = values.getOrNull(1)
        val priceDiesel = values.getOrNull(2)

        tvPrice95.text = price95?.let { String.format("%.3f €", it) } ?: "-.--- €"
        tvPrice98.text = price98?.let { String.format("%.3f €", it) } ?: "-.--- €"
        tvPriceDiesel.text = priceDiesel?.let { String.format("%.3f €", it) } ?: "-.--- €"
    }
    
    private fun setupDarkModeSwitch() {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        switchDarkMode.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun setLineChartData(gasPricesResponse: GasPricesResponse) {
        val allWeeksLabels = gasPricesResponse.dimension.sp0207ts_tyz.category.label.values.toList()
        val numWeeks = allWeeksLabels.size

        if (numWeeks == 0) {
            statusTextView.text = "No data available to display."
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        // Reverse allWeeksLabels to have oldest week at index 0 and newest at numWeeks-1
        // Also extract only the date range from the label: "39. week (22.9.2025-28.9.2025)" -> "22.9.2025-28.9.2025"
        val weeksLabels = allWeeksLabels.reversed().map { label ->
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

        // Determine text color based on night mode
        val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val chartTextColor = if (isNightMode) Color.WHITE else Color.BLACK

        // Colors matching the dashboard header
        val color95 = Color.parseColor("#FFC107") // Amber
        val color98 = Color.parseColor("#4CAF50") // Green
        val colorDiesel = Color.parseColor("#2196F3") // Blue

        val dataSet95 = LineDataSet(entries95, "95 Octane").apply {
            color = color95
            lineWidth = 5f
            setDrawValues(true)
            valueTextColor = chartTextColor
        }
        val dataSet98 = LineDataSet(entries98, "98 Octane").apply {
            color = color98
            lineWidth = 5f
            setDrawValues(true)
            valueTextColor = chartTextColor
        }
        val dataSetDiesel = LineDataSet(entriesDiesel, "Diesel").apply {
            color = colorDiesel
            lineWidth = 5f
            setDrawValues(true)
            valueTextColor = chartTextColor
        }

        // Use entries directly for regression calculation
        val regressionDataSet95 = calculateRegressionLine(entries95, dataSet95.color)
        val regressionDataSet98 = calculateRegressionLine(entries98, dataSet98.color)
        val regressionDataSetDiesel = calculateRegressionLine(entriesDiesel, dataSetDiesel.color)

        val lineData = LineData()
        lineData.addDataSet(dataSet95)
        lineData.addDataSet(regressionDataSet95)

        lineData.addDataSet(dataSet98)
        lineData.addDataSet(regressionDataSet98)

        lineData.addDataSet(dataSetDiesel)
        lineData.addDataSet(regressionDataSetDiesel)
        
        // Ensure values use the correct text color
        lineData.setValueTextColor(chartTextColor)

        lineChart.data = lineData
        lineChart.xAxis.valueFormatter = WeekAxisValueFormatter(weeksLabels)
        
        // Force 5 labels
        lineChart.xAxis.setLabelCount(5, true)
        
        // Disable clipping avoidance to prevent overlapping labels
        lineChart.xAxis.setAvoidFirstLastClipping(false)
        
        lineChart.xAxis.granularity = 1f
        lineChart.xAxis.labelRotationAngle = -45f
        
        // Add padding to axis range so first/last labels are not cut off by screen edge
        lineChart.xAxis.setAxisMinimum(-0.5f)
        lineChart.xAxis.setAxisMaximum(numWeeks - 0.5f)
        
        lineChart.invalidate()
    }

    private fun createEntries(prices: List<Float?>, typeIndex: Int, numWeeks: Int): List<Entry> {
        val entries = prices.mapIndexedNotNull { index, price ->
            if (index % 3 == typeIndex) {
                // The API provides data from newest to oldest. We want oldest on x=0 and newest on x=numWeeks-1.
                // groupIndex 0 is the newest week, groupIndex numWeeks-1 is the oldest week.
                val groupIndex = (index / 3)
                val xValue = (numWeeks - 1) - groupIndex.toFloat()
                price?.let { Entry(xValue, it) }
            } else {
                null
            }
        }
        // MPAndroidChart requires entries to be sorted by x-value ascending
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
            setDrawGridBackground(false) // Cleaner look, usually better for dark mode too
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
        // Calculate slope and intercept using the helper class
        val result = RegressionCalculator.calculateSlopeAndIntercept(dataPoints)
        val slope = result.slope
        val intercept = result.intercept

        val regressionEntries = dataPoints.map { Entry(it.x, (slope * it.x + intercept)) }
        val regressionDataSet = LineDataSet(regressionEntries, null)
        regressionDataSet.form = Legend.LegendForm.NONE // Hide from legend
        regressionDataSet.setDrawCircles(false)
        regressionDataSet.color = color // Use the provided color for the regression line
        regressionDataSet.lineWidth = 3f // Increased width for visibility
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
