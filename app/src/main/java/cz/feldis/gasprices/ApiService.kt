package cz.feldis.gasprices

import cz.feldis.gasprices.models.GasPricesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @GET("api/v2/dataset/sp0207ts/{weeks}/UKAZ01,UKAZ02,UKAZ04?lang=en")
    suspend fun getGasPrices(@Path("weeks") weeks: String): Response<GasPricesResponse>
}