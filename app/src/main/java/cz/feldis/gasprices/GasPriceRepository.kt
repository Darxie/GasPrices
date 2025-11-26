package cz.feldis.gasprices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cz.feldis.gasprices.models.GasPricesResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

class GasPriceRepository(private val apiService: ApiService) {

    private val _gasPrices = MutableLiveData<GasPricesResponse>()
    val gasPrices: LiveData<GasPricesResponse> get() = _gasPrices

    /**
     * Generates a comma-separated string of the last 30 weeks in the `yyyyww` format.
     * This format is required by the data.statistics.sk API.
     */
    private fun getLast30Weeks(): String {
        val today = LocalDate.now().minusWeeks(1)
        val weekFields = WeekFields.of(Locale.getDefault())
        val weekOfYearFormatter = DateTimeFormatter.ofPattern("yyyyww")

        return (1..30).map { i ->
            today.minusWeeks(i.toLong())
                .with(weekFields.dayOfWeek(), 1L) // Ensure we are at the start of the week
                .format(weekOfYearFormatter)
        }.reversed().joinToString(",")
    }

    suspend fun fetchGasPrices(){
        val weeks = getLast30Weeks()
        val response = apiService.getGasPrices(weeks)
        if (response.isSuccessful && response.body() != null) {
            _gasPrices.postValue(response.body()!!)
        } else {
            val error = response.errorBody()?.string()
            throw Exception("Error fetching gas prices: $error")
        }
    }
}
