package com.example.blesstify.data.remote

import com.example.blesstify.data.remote.dto.ApiSongsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

class BlesstifyRemoteDataSource @Inject constructor(
    private val api: BlesstifyApi
) {
    suspend fun searchSongs(query: String, limit: Int = 20): NetworkResult<ApiSongsResponse> =
        safeCall { api.searchSongs(query = query, limit = limit) }

    suspend fun getTrendingSongs(limit: Int = 20): NetworkResult<ApiSongsResponse> =
        safeCall { api.getTrendingSongs(limit = limit) }

    private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): NetworkResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                NetworkResult.Success(block())
            } catch (e: HttpException) {
                val body = try {
                    e.response()?.errorBody()?.string()
                } catch (_: Throwable) {
                    null
                }
                NetworkResult.HttpError(
                    code = e.code(),
                    message = e.message(),
                    body = body
                )
            } catch (t: Throwable) {
                NetworkResult.Error(message = t.message ?: "Network error", throwable = t)
            }
        }
    }
}
