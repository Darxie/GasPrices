package cz.feldis.gasprices

import cz.feldis.gasprices.models.DatasetInfo
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {
    /** Downloads the whole dataset as a zip archive containing one CSV file with all rows. */
    @Streaming
    @GET("public/api/dc/opendata/cube/dataset/sp0207ts/download")
    suspend fun downloadGasPrices(
        @Query("type") type: String = "CSV",
        @Query("lang") lang: String = "EN"
    ): Response<ResponseBody>

    @GET("public/api/dc/public/sp0207ts/info")
    suspend fun getDatasetInfo(): Response<DatasetInfo>
}
