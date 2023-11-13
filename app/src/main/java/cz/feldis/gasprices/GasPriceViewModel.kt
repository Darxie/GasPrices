package cz.feldis.gasprices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.feldis.gasprices.models.GasPricesResponse
import kotlinx.coroutines.launch

class GasPriceViewModel(private val repository: GasPriceRepository) : ViewModel() {

    val gasPrices: LiveData<GasPricesResponse> = repository.gasPrices

    fun loadGasPrices() {
        viewModelScope.launch {
            try {
                repository.fetchGasPrices()
            } catch (e: Exception) {
                // Handle any exceptions by updating the UI accordingly
            }
        }
    }
}
