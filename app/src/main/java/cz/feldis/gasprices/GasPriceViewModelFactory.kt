package cz.feldis.gasprices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GasPriceViewModelFactory(private val repository: GasPriceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GasPriceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GasPriceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}