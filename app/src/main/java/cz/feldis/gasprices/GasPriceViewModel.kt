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

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun loadGasPrices() {
        viewModelScope.launch(dispatcher) {
            _isLoading.postValue(true)
            try {
                val response = repository.fetchGasPrices()
                _gasPrices.postValue(response)
                _errorMessage.postValue(null)
            } catch (e: Exception) {
                _gasPrices.postValue(null)
                _errorMessage.postValue(e.localizedMessage ?: "Failed to load gas prices.")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
