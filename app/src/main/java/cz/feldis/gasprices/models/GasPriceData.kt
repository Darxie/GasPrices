package cz.feldis.gasprices.models

/**
 * Average fuel prices for one week, as published in dataset sp0207ts.
 * A price is null when the statistics office did not publish it for that week.
 */
data class WeeklyFuelPrices(
    val year: String,
    val week: String,
    val dateFrom: String,
    val dateTo: String,
    val gasoline95: Float?,
    val gasoline98: Float?,
    val diesel: Float?
)

/** Weekly prices in chronological order (oldest first) plus dataset metadata. */
data class GasPriceData(
    val label: String,
    val updated: String,
    val sourceUrl: String,
    val weeks: List<WeeklyFuelPrices>
)
