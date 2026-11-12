package cz.feldis.gasprices

import cz.feldis.gasprices.models.FuelPriceRow
import cz.feldis.gasprices.models.GasPriceData
import cz.feldis.gasprices.models.WeeklyFuelPrices
import java.io.InputStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

class GasPriceRepository(
    private val apiService: ApiService,
    private val weeksToShow: Int = 30
) {

    suspend fun fetchGasPrices(): GasPriceData {
        val response = apiService.downloadGasPrices()
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            val error = response.errorBody()?.string()
            throw Exception("Error fetching gas prices: ${error ?: "HTTP ${response.code()}"}")
        }
        val rows = body.byteStream().use { parseZippedRows(it) }
        val weeks = toWeeklyPrices(rows).takeLast(weeksToShow)

        // Metadata is optional: prices are still shown if the info endpoint fails.
        val info = runCatching { apiService.getDatasetInfo().body() }.getOrNull()

        return GasPriceData(
            label = info?.nameEn ?: DEFAULT_LABEL,
            updated = formatUpdated(info?.lastDataChange) ?: weeks.lastOrNull()?.dateTo.orEmpty(),
            sourceUrl = SOURCE_URL,
            weeks = weeks
        )
    }

    companion object {
        const val CODE_GASOLINE_95 = "CP_07202102"
        const val CODE_GASOLINE_98 = "CP_07202103"
        const val CODE_DIESEL = "CP_07202105"
        const val SOURCE_URL = "https://statdata.statistics.sk/public/ui/dc/"
        private const val DEFAULT_LABEL = "Average fuel prices in the Slovak Republic (weekly)"

        private val TRACKED_CODES = setOf(CODE_GASOLINE_95, CODE_GASOLINE_98, CODE_DIESEL)

        /**
         * Reads the data CSV (sp0207ts_CSV_EN.csv) from the zip archive returned by the download
         * endpoint, skipping the structure.csv and codelists/ entries.
         */
        fun parseZippedRows(input: InputStream): List<FuelPriceRow> {
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (!entry.isDirectory && name.endsWith(".csv", ignoreCase = true) &&
                        !name.contains('/') && !name.equals("structure.csv", ignoreCase = true)
                    ) {
                        return parseCsv(zip.bufferedReader(Charsets.UTF_8).lineSequence())
                    }
                }
            }
            throw Exception("Error fetching gas prices: no data CSV in downloaded archive")
        }

        /**
         * Parses the open data CSV export. It holds the full history of every fuel type, so only
         * rows of tracked fuels are kept. Values never contain commas or quotes, so a plain split
         * is enough.
         */
        fun parseCsv(lines: Sequence<String>): List<FuelPriceRow> {
            val iterator = lines.iterator()
            if (!iterator.hasNext()) return emptyList()
            val header = iterator.next().removePrefix("﻿").split(',')
            fun column(name: String) = header.indexOf(name).also {
                if (it < 0) throw Exception("Error fetching gas prices: missing column '$name'")
            }
            val code = column("e_coicop_code")
            val year = column("e_year_code")
            val week = column("e_week_code")
            val dateFrom = column("e_datefrom")
            val dateTo = column("e_dateto")
            val value = column("value")

            val rows = mutableListOf<FuelPriceRow>()
            for (line in iterator) {
                val cells = line.split(',')
                if (cells.size != header.size || cells[code] !in TRACKED_CODES) continue
                rows += FuelPriceRow(
                    fuelCode = cells[code],
                    year = cells[year],
                    week = cells[week],
                    dateFrom = cells[dateFrom],
                    dateTo = cells[dateTo],
                    value = cells[value]
                )
            }
            return rows
        }

        /** Groups rows by week and returns the weeks in chronological order. */
        fun toWeeklyPrices(rows: List<FuelPriceRow>): List<WeeklyFuelPrices> {
            return rows
                .filter { it.fuelCode in TRACKED_CODES && !it.dateFrom.isNullOrEmpty() }
                .groupBy { it.dateFrom!! }
                .toSortedMap() // ISO yyyy-MM-dd dates sort chronologically as strings
                .map { (dateFrom, weekRows) ->
                    fun price(code: String) =
                        weekRows.firstOrNull { it.fuelCode == code }?.value?.toFloatOrNull()
                    val first = weekRows.first()
                    WeeklyFuelPrices(
                        year = first.year.orEmpty(),
                        week = first.week.orEmpty(),
                        dateFrom = dateFrom,
                        dateTo = first.dateTo.orEmpty(),
                        gasoline95 = price(CODE_GASOLINE_95),
                        gasoline98 = price(CODE_GASOLINE_98),
                        diesel = price(CODE_DIESEL)
                    )
                }
        }

        private fun formatUpdated(isoDateTime: String?): String? = isoDateTime?.let {
            runCatching {
                OffsetDateTime.parse(it).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
            }.getOrDefault(it)
        }
    }
}
