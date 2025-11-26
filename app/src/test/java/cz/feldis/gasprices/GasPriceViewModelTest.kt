package cz.feldis.gasprices

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import cz.feldis.gasprices.models.GasPricesResponse
import cz.feldis.gasprices.models.Dimension
import cz.feldis.gasprices.models.Category
import cz.feldis.gasprices.models.CategoryDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.lang.RuntimeException

@ExperimentalCoroutinesApi
class GasPriceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: GasPriceRepository

    @Mock
    private lateinit var mockObserver: Observer<GasPricesResponse?>

    private lateinit var viewModel: GasPriceViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = GasPriceViewModel(mockRepository, testDispatcher)
        viewModel.gasPrices.observeForever(mockObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.gasPrices.removeObserver(mockObserver)
    }

    @Test
    fun loadGasPrices_success_updatesLiveData() = runTest {
        // Given
        val dummyResponse = GasPricesResponse(
            version = "1.0", // Added missing fields
            categoryClass = "test_class", // Added missing fields
            label = "Test Label",
            update = "Test Update",
            href = "http://test.com", // Added missing fields
            dimension = Dimension(
                sp0207ts_tyz = Category(
                    label = "Dummy Label for sp0207ts_tyz", // Corrected label type
                    note = "",
                    category = CategoryDetail(
                        index = emptyMap(),
                        label = mapOf(
                            "week_1" to "1. week (01.01.2023-07.01.2023)",
                            "week_2" to "2. week (08.01.2023-14.01.2023)"
                        )
                    )
                ),
                sp0207ts_ukaz = Category(
                    label = "Dummy Label for sp0207ts_ukaz",
                    note = "",
                    category = CategoryDetail(emptyMap(), emptyMap())
                ),
                sp0207ts_data = Category(
                    label = "Dummy Label for sp0207ts_data",
                    note = "",
                    category = CategoryDetail(emptyMap(), emptyMap())
                )
            ),
            value = listOf(1.5f, 1.6f, 1.7f)
        )
        `when`(mockRepository.fetchGasPrices()).thenReturn(dummyResponse)

        // When
        viewModel.loadGasPrices()
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutines complete

        // Then
        verify(mockObserver).onChanged(dummyResponse)
    }

    @Test
    fun loadGasPrices_failure_updatesLiveDataWithNull() = runTest {
        // Given
        `when`(mockRepository.fetchGasPrices()).thenThrow(RuntimeException("Network error"))

        // When
        viewModel.loadGasPrices()
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutines complete

        // Then
        verify(mockObserver).onChanged(null)
    }

    @Test
    fun loadGasPrices_repositoryReturnsNull_updatesLiveDataWithNull() = runTest {
        // Given
        `when`(mockRepository.fetchGasPrices()).thenReturn(null)

        // When
        viewModel.loadGasPrices()
        testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutines complete

        // Then
        verify(mockObserver).onChanged(null)
    }
}