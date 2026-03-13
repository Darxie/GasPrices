package cz.feldis.gasprices

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import cz.feldis.gasprices.models.Category
import cz.feldis.gasprices.models.CategoryDetail
import cz.feldis.gasprices.models.Dimension
import cz.feldis.gasprices.models.GasPricesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
class GasPriceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: GasPriceRepository

    private lateinit var viewModel: GasPriceViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = GasPriceViewModel(mockRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadGasPrices_success_updatesDataAndClearsError() = runTest {
        val loadingEvents = mutableListOf<Boolean?>()
        val errorEvents = mutableListOf<String?>()
        val response = dummyResponse()

        viewModel.isLoading.observeForever { loadingEvents.add(it) }
        viewModel.errorMessage.observeForever { errorEvents.add(it) }

        Mockito.`when`(mockRepository.fetchGasPrices()).thenReturn(response)
        viewModel.loadGasPrices()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(response, viewModel.gasPrices.value)
        assertNull(viewModel.errorMessage.value)
        assertTrue(loadingEvents.contains(true))
        assertFalse(loadingEvents.last() == true)
        assertTrue(errorEvents.last() == null)
    }

    @Test
    fun loadGasPrices_failure_setsErrorAndNullData() = runTest {
        val loadingEvents = mutableListOf<Boolean?>()
        viewModel.isLoading.observeForever { loadingEvents.add(it) }

        Mockito.`when`(mockRepository.fetchGasPrices()).thenThrow(RuntimeException("Network error"))
        viewModel.loadGasPrices()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.gasPrices.value)
        assertEquals("Network error", viewModel.errorMessage.value)
        assertTrue(loadingEvents.contains(true))
        assertFalse(loadingEvents.last() == true)
    }

    @Test
    fun loadGasPrices_failureWithoutMessage_usesFallbackError() = runTest {
        Mockito.`when`(mockRepository.fetchGasPrices()).thenThrow(RuntimeException())
        viewModel.loadGasPrices()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.gasPrices.value)
        assertEquals("Failed to load gas prices.", viewModel.errorMessage.value)
    }

    private fun dummyResponse() = GasPricesResponse(
        version = "1.0",
        categoryClass = "test_class",
        label = "Test Label",
        update = "Test Update",
        href = "http://test.com",
        dimension = Dimension(
            sp0207ts_tyz = Category(
                label = "Dummy Label for sp0207ts_tyz",
                note = "",
                category = CategoryDetail(
                    index = emptyMap(),
                    label = mapOf("week_1" to "1. week (01.01.2023-07.01.2023)")
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
}
