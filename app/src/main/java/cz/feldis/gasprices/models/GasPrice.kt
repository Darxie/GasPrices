package cz.feldis.gasprices.models

data class GasPrice(
    val weekNumber: String,
    val gasoline95OctanePrice: Double,
    val gasoline98OctanePrice: Double,
    val dieselOilPrice: Double
)
