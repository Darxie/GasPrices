package cz.feldis.gasprices.models

import com.google.gson.annotations.SerializedName

data class GasPricesResponse(
    val version: String,
    @SerializedName("class") val categoryClass: String,
    val label: String,
    val update: String,
    val href: String,
    val dimension: Dimension,
    val value: List<Float>
)

data class Dimension(
    val sp0207ts_tyz: Category,
    val sp0207ts_ukaz: Category,
    val sp0207ts_data: Category
)

data class Category(
    val label: String,
    val note: String,
    val category: CategoryDetail
)

data class CategoryDetail(
    val index: Map<String, Int>,
    val label: Map<String, String>
)
