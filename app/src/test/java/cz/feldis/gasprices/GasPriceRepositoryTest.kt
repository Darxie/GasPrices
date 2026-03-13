package cz.feldis.gasprices

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import cz.feldis.gasprices.models.Category
import cz.feldis.gasprices.models.CategoryDetail
import cz.feldis.gasprices.models.Dimension
import cz.feldis.gasprices.models.GasPricesResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@ExperimentalCoroutinesApi
class GasPriceRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockApiService: ApiService

    private lateinit var repository: GasPriceRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = GasPriceRepository(mockApiService)
    }

    @Test
    fun fetchGasPrices_success_returnsResponse_andRequests30WeeksIncludingCurrentWeek() = runTest {
        val response = dummyResponse()
        var requestedWeeksCsv = ""
        Mockito.`when`(mockApiService.getGasPrices(Mockito.anyString())).thenAnswer { invocation ->
            requestedWeeksCsv = invocation.getArgument(0)
            Response.success(response)
        }

        val result = repository.fetchGasPrices()
        val requestedWeeks = requestedWeeksCsv.split(",")
        val expectedCurrentWeek = LocalDate.now()
            .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1L)
            .format(DateTimeFormatter.ofPattern("yyyyww"))

        assertEquals(response, result)
        Mockito.verify(mockApiService).getGasPrices(Mockito.anyString())
        assertEquals(30, requestedWeeks.size)
        assertEquals(expectedCurrentWeek, requestedWeeks.last())
        assertTrue(requestedWeeks.all { it.matches(Regex("\\d{6}")) })
    }

    @Test
    fun fetchGasPrices_apiError_throwsExceptionWithBody() = runTest {
        val errorResponse = Response.error<GasPricesResponse>(
            404,
            "{\"message\":\"Not Found\"}".toResponseBody()
        )
        Mockito.`when`(mockApiService.getGasPrices(Mockito.anyString())).thenReturn(errorResponse)

        val exception = runCatching { repository.fetchGasPrices() }.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Not Found") == true)
    }

    @Test
    fun fetchGasPrices_nullBody_throwsException() = runTest {
        Mockito.`when`(mockApiService.getGasPrices(Mockito.anyString())).thenReturn(Response.success(null))

        val exception = runCatching { repository.fetchGasPrices() }.exceptionOrNull()
        assertNotNull(exception)
        assertNotNull(exception?.message)
    }

    @Test
    fun fetchGasPrices_unsuccessfulResponse_throwsException() = runTest {
        val unsuccessfulResponse = Response.error<GasPricesResponse>(
            500,
            "{\"message\":\"Server Error\"}".toResponseBody()
        )
        Mockito.`when`(mockApiService.getGasPrices(Mockito.anyString())).thenReturn(unsuccessfulResponse)

        val exception = runCatching { repository.fetchGasPrices() }.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Server Error") == true)
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
        value = listOf(1.5f)
    )
}
