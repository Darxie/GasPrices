package cz.feldis.gasprices

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.feldis.gasprices.models.GasPricesResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GasPriceViewModel(private val repository: GasPriceRepository, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel() {

    private val _gasPrices = MutableLiveData<GasPricesResponse?>()
    val gasPrices: LiveData<GasPricesResponse?> = _gasPrices

    fun loadGasPrices() {
        viewModelScope.launch(dispatcher) {
            try {
                val response = repository.fetchGasPrices()
                _gasPrices.postValue(response)
            } catch (e: Exception) {
                _gasPrices.postValue(null)
            }
        }
    }
}