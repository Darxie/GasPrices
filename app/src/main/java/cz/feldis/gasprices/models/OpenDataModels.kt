package cz.feldis.gasprices.models

import com.google.gson.annotations.SerializedName

/** One row of the open data CSV export (sp0207ts_CSV_EN.csv inside the downloaded zip). */
data class FuelPriceRow(
    @SerializedName("e_coicop_code") val fuelCode: String?,
    @SerializedName("e_year_code") val year: String?,
    @SerializedName("e_week_code") val week: String?,
    @SerializedName("e_datefrom") val dateFrom: String?,
    @SerializedName("e_dateto") val dateTo: String?,
    @SerializedName("value") val value: String?
)

/** Response of /public/api/dc/public/{dataset}/info. */
data class DatasetInfo(
    val nameEn: String?,
    val lastDataChange: String?
)
