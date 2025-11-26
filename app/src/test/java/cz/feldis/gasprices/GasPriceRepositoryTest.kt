package cz.feldis.gasprices

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import cz.feldis.gasprices.models.GasPricesResponse
import cz.feldis.gasprices.models.Dimension
import cz.feldis.gasprices.models.Category
import cz.feldis.gasprices.models.CategoryDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.* // Import all static methods from Mockito
import org.mockito.MockitoAnnotations
import retrofit2.Response

@ExperimentalCoroutinesApi
class GasPriceRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockApiService: ApiService

    // Observer is no longer needed in Repository tests, as ViewModel now handles LiveData updates.
    // private lateinit var mockObserver: Observer<GasPricesResponse?>

    private lateinit var repository: GasPriceRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = GasPriceRepository(mockApiService)
        // LiveData observation is now handled by ViewModel, not directly in Repository
        // repository.gasPrices.observeForever(mockObserver)
    }

    @Test
    fun fetchGasPrices_success_returnsResponse() = runTest {
        // Given
        val dummyResponse = GasPricesResponse(
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
                        label = mapOf(
                            "week_1" to "1. week (01.01.2023-07.01.2023)"
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
            value = listOf(1.5f)
        )
        `when`(mockApiService.getGasPrices(anyString()))
            .thenReturn(Response.success(dummyResponse))

        // When
        val result = repository.fetchGasPrices()

        // Then
        verify(mockApiService).getGasPrices(anyString())
        assertEquals(dummyResponse, result)
    }

    @Test(expected = Exception::class)
    fun fetchGasPrices_apiError_throwsException() = runTest {
        // Given
        val errorResponse = Response.error<GasPricesResponse>(
            404,
            "{\"message\":\"Not Found\"}".toResponseBody()
        )
        `when`(mockApiService.getGasPrices(anyString()))
            .thenReturn(errorResponse)

        // When
        repository.fetchGasPrices()

        // Then - exception is expected
        verify(mockApiService).getGasPrices(anyString())
    }

    @Test(expected = Exception::class)
    fun fetchGasPrices_nullBody_throwsException() = runTest {
        // Given
        `when`(mockApiService.getGasPrices(anyString()))
            .thenReturn(Response.success(null))

        // When
        repository.fetchGasPrices()

        // Then - exception is expected
        verify(mockApiService).getGasPrices(anyString())
    }

    @Test(expected = Exception::class)
    fun fetchGasPrices_unsuccessfulResponse_throwsException() = runTest {
        // Given
        val unsuccessfulResponse = Response.error<GasPricesResponse>(
            500, // Internal Server Error
            "{\"message\":\"Server Error\"}".toResponseBody()
        )
        `when`(mockApiService.getGasPrices(anyString()))
            .thenReturn(unsuccessfulResponse)

        // When
        repository.fetchGasPrices()

        // Then - exception is expected
        verify(mockApiService).getGasPrices(anyString())
    }
}