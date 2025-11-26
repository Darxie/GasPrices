package cz.feldis.gasprices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers

class GasPriceViewModelFactory(private val repository: GasPriceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GasPriceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GasPriceViewModel(repository, Dispatchers.IO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}