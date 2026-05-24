package com.boilerplate.network


import com.boilerplate.network.auth.data.repository.AuthRepositoryImpl
import com.boilerplate.network.model.DataResponse
import com.boilerplate.network.model.NetworkResult
import com.boilerplate.network.model.NetworkResultStatus
import com.boilerplate.network.utils.NetworkConstants
import com.boilerplate.network.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import java.net.UnknownHostException

class NetworkResource<out Output>(
    private val remoteFetch: suspend () -> Response<DataResponse<Output>>?,
    private val refreshControl: RefreshControl = RefreshControl()
) : RefreshControl.Listener, ITimeLimitedResource by refreshControl {

    private var localFetch: (suspend () -> List<Output>?)? = null
    private var localStore: (suspend (Output) -> Unit)? = null
    private var localDelete: (suspend () -> Unit)? = null

    private var localData: Output? = null

    private var isCallingGenerateAccessToken = false

    internal constructor(
        remoteFetch: suspend () -> Response<DataResponse<Output>>?,
        refreshControl: RefreshControl = RefreshControl(),
        isCallingGenerateAccessToken : Boolean = false
    ) : this(remoteFetch, refreshControl){
        this.isCallingGenerateAccessToken = isCallingGenerateAccessToken
    }

    constructor(
        remoteFetch: suspend () -> Response<DataResponse<Output>>?,
        localFetch: suspend () -> List<Output>?,
        localStore: suspend (Output) -> Unit,
        localDelete: suspend () -> Unit,
        refreshControl: RefreshControl = RefreshControl()
    ) : this(remoteFetch, refreshControl) {
        this.localFetch = localFetch
        this.localStore = localStore
        this.localDelete = localDelete
    }

    init {
        refreshControl.addListener(this)
    }

    private var isAccessTokenAPICalled = false


    suspend fun query(force: Boolean = false): Flow<NetworkResult<Output?>> = flow {
        emit(NetworkResult.loading(null))
        /**
         * if force is false local data is retrieved first
         */
        if (!force) {
            //calling local db
            fetchFromLocal()?.run {
                localData = this
                emit(NetworkResult.success(this))
            }
        }
        /**
         * if refresh time is expired or force is true, network call is made
         * NOTE : for first time, refreshControl.isExpired() always returns true
         */
        if (refreshControl.isExpired() || force) {
            //calling network call
            fetchFromRemote().run {
                /**
                 * if force is true, result is emitted
                 * if force is false and data from network is same as stored in local, then emitting the result and storing in local steps are skipped
                 */
                if (force || !isSameAsCached(this.data) || this.status == NetworkResultStatus.ERROR) {
                    emit(this)
                    kotlin.runCatching {
                        //storing in local is skipped if force is true
                        if (!force && data != null && this.status != NetworkResultStatus.ERROR) {
                            this.let { it1 ->
                                withContext(Dispatchers.IO) {
                                    cleanup()
                                    it1.data?.let { localStore?.let { it2 -> it2(it) } }
                                }
                            }
                            refreshControl.refresh()
                        }
                    }.onFailure {
                        //log error
                    }

                }
            }
        }
    }

    /**
     * checks if output is same as the data fetched from db
     *
     * @param output is compared with local data
     * @return true if data are same or local data is null, else false
     */
    private fun isSameAsCached(output: Output?): Boolean {
        if (output == null) return false
        return localData?.let { NetworkUtils.deepEquals(output, it) } == true

    }

    override suspend fun cleanup() {
//        Timber.tag("Resource").d("cleaning local")
        deleteLocal()
    }


    private suspend fun deleteLocal() = kotlin.runCatching {
        withContext(Dispatchers.IO) {
//            Timber.tag("Resource").d("deleting local")
            localDelete?.let { it() }
        }
    }.getOrNull()

    /**
     * local db fetch call
     *
     */
    private suspend fun fetchFromLocal() = kotlin.runCatching {
//        Timber.tag("Resource").d("fetching local")
        withContext(Dispatchers.IO) {
            localFetch?.let { it() }?.get(0)
        }
    }.getOrNull()

    /**
     * network call
     *
     * @return output with Result wrapper class
     */
    private suspend fun fetchFromRemote(): NetworkResult<Output?> {
//        Timber.tag("Resource").d("fetching remote")
        var res: Response<DataResponse<Output>>? = null
        var networkResult: NetworkResult<Output?>? = null

        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                res = remoteFetch()
                networkResult = getDataFromResponse(res)
            }.onFailure {
                networkResult = if (it is UnknownHostException) {
                    NetworkResult.error(
                        localData,
                        "Please check your internet connection and try again later",
                        res?.code()
                    )
                } else {
                    NetworkResult.error(
                        localData,
                        it.message,
                        res?.code()
                    )
                }
            }
        }

        return networkResult ?: NetworkResult.error(localData, "", 2)
    }

    /**
     * converts Retrofit Response to Result<Output>
     *
     * It also check for auth error. If error code is 401, access token is updated from API.
     * if both access and refresh token are expired then hardLogoutPostRefreshTokenFails is called
     *
     * @param response from network call
     * @return output with Result wrapper class
     */
    private suspend fun getDataFromResponse(response: Response<DataResponse<Output>>?): NetworkResult<Output?> {
        if (response?.isSuccessful == true) {
            return NetworkResult.success(response.body()?.data, response.code(), true)
        }
        else if(response?.code() == 401 && !isAccessTokenAPICalled){
            if(generateAccessToken()){
                return fetchFromRemote()
            }
        }
        var errorMessage = response?.errorBody()?.string()
        if (response?.errorBody() != null) {
            kotlin.runCatching {
                val errorResponse = errorMessage?.let { JSONObject(it) }
                errorMessage = errorResponse?.getJSONObject("error")?.getString("message")
            }
        }
        return NetworkResult.error(localData, errorMessage, response?.code())
    }

    suspend fun queryWithoutFlow() : NetworkResult<Output?>{
        return fetchFromRemote()
    }

    private suspend fun generateAccessToken() : Boolean{
        val networkHandler = NetworkHandler.getInstance()

        val baseUrl = if(networkHandler.isDebug()) NetworkConstants.BASE_URL_DEBUG else NetworkConstants.BASE_URL
        val defaultAuthenticationCallback = networkHandler.getDefaultAuthCallback()
        val refreshToken = networkHandler.getRefreshToken() ?: ""

        val res = AuthRepositoryImpl().generateAccessToken(baseUrl, hashMapOf(NetworkConstants.REFRESH_TOKEN to refreshToken))
        if(res.status == NetworkResultStatus.SUCCESS){
            networkHandler.setAccessToken(res.data?.token?.accessToken ?: "")
            kotlin.runCatching {  defaultAuthenticationCallback?.onNewAccessTokenGenerated(res.data?.token?.accessToken, res.data?.token?.refreshToken, res.data?.token?.refreshExpiresIn) }
            return true
        }else{
            kotlin.runCatching {  defaultAuthenticationCallback?.onRefreshTokenFailed() }
            return false
        }
    }
}