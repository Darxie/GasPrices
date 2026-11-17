package cz.feldis.gasprices

import cz.feldis.gasprices.models.DatasetInfo
import cz.feldis.gasprices.models.FuelPriceRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@ExperimentalCoroutinesApi
class GasPriceRepositoryTest {

    @Mock
    private lateinit var mockApiService: ApiService

    private lateinit var repository: GasPriceRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = GasPriceRepository(mockApiService, weeksToShow = 2)
    }

    @Test
    fun fetchGasPrices_success_parsesZipAndKeepsLatestWeeksInOrder() = runTest {
        val csv = listOf(
            HEADER,
            row("CP_07202102", "W44", "2026-10-26", "2026-11-01", "1.785"),
            row("CP_07202103", "W44", "2026-10-26", "2026-11-01", "1.988"),
            row("CP_07202105", "W44", "2026-10-26", "2026-11-01", "1.979"),
            row("CP_07202104", "W44", "2026-10-26", "2026-11-01", "0.799"),
            row("CP_07202102", "W42", "2026-10-12", "2026-10-18", "1.885"),
            row("CP_07202102", "W43", "2026-10-19", "2026-10-25", "1.820"),
            row("CP_07202105", "W43", "2026-10-19", "2026-10-25", "1.970")
        ).joinToString("\n")
        stubDownload(Response.success(zipOf("structure.csv" to "a,b", "codelists/e_week.csv" to "code,name", "sp0207ts_CSV_EN.csv" to csv)))
        Mockito.`when`(mockApiService.getDatasetInfo()).thenReturn(
            Response.success(DatasetInfo("Average fuel prices", "2026-11-06T09:00:00+01:00"))
        )

        val result = repository.fetchGasPrices()

        assertEquals("Average fuel prices", result.label)
        assertEquals("06.11.2026 09:00", result.updated)
        assertEquals(listOf("W43", "W44"), result.weeks.map { it.week })
        val w43 = result.weeks[0]
        assertEquals(1.820f, w43.gasoline95)
        assertNull(w43.gasoline98)
        assertEquals(1.970f, w43.diesel)
        val w44 = result.weeks[1]
        assertEquals("2026-10-26", w44.dateFrom)
        assertEquals("2026-11-01", w44.dateTo)
        assertEquals(1.785f, w44.gasoline95)
        assertEquals(1.988f, w44.gasoline98)
        assertEquals(1.979f, w44.diesel)
    }

    @Test
    fun fetchGasPrices_infoFails_stillReturnsPrices() = runTest {
        val csv = HEADER + "\n" + row("CP_07202102", "W44", "2026-10-26", "2026-11-01", "1.785")
        stubDownload(Response.success(zipOf("sp0207ts_CSV_EN.csv" to csv)))
        Mockito.`when`(mockApiService.getDatasetInfo()).thenThrow(RuntimeException("boom"))

        val result = repository.fetchGasPrices()

        assertEquals(1, result.weeks.size)
        assertEquals("2026-11-01", result.updated)
    }

    @Test
    fun fetchGasPrices_apiError_throwsExceptionWithBody() = runTest {
        stubDownload(Response.error(404, "{\"message\":\"Not Found\"}".toResponseBody()))

        val exception = runCatching { repository.fetchGasPrices() }.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Not Found") == true)
    }

    @Test
    fun fetchGasPrices_zipWithoutDataCsv_throwsException() = runTest {
        stubDownload(Response.success(zipOf("structure.csv" to "a,b")))

        val exception = runCatching { repository.fetchGasPrices() }.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(exception?.message?.contains("no data CSV") == true)
    }

    @Test
    fun toWeeklyPrices_ignoresUnparseableValues() {
        val weeks = GasPriceRepository.toWeeklyPrices(
            listOf(FuelPriceRow("CP_07202102", "2026", "W44", "2026-10-26", "2026-11-01", ":"))
        )
        assertEquals(1, weeks.size)
        assertNull(weeks[0].gasoline95)
    }

    private suspend fun stubDownload(response: Response<ResponseBody>) {
        Mockito.`when`(mockApiService.downloadGasPrices(Mockito.anyString(), Mockito.anyString()))
            .thenReturn(response)
    }

    @Test
    fun parseCsv_missingColumn_throwsException() {
        val exception = runCatching {
            GasPriceRepository.parseCsv(sequenceOf("e_coicop_code,value", "CP_07202102,1.5"))
        }.exceptionOrNull()
        assertTrue(exception?.message?.contains("e_year_code") == true)
    }

    private fun row(code: String, week: String, from: String, to: String, value: String) =
        "$code,Fuel name,UNIT_EUR_L,Eur / l,2026,2026,$week,week,x,$from,x,$to,U_SP_0001,Fuel,$value"

    private companion object {
        const val HEADER = "e_coicop_code,e_coicop,e_unit_code,e_unit,e_year_code,e_year,e_week_code,e_week," +
            "e_datefrom_code,e_datefrom,e_dateto_code,e_dateto,e_indic_code,e_indic,value"
    }

    private fun zipOf(vararg files: Pair<String, String>): ResponseBody {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            files.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray().toResponseBody()
    }
}
