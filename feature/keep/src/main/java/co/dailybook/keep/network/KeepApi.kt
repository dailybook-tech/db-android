package co.dailybook.keep.network

import co.dailybook.boilerplate.network.model.DataResponse
import co.dailybook.keep.model.AddAdvanceRequestBody
import co.dailybook.keep.model.AdvanceEntryRequestBody
import co.dailybook.keep.model.AdvanceEntryResponse
import co.dailybook.keep.model.AssignWorkerToTeamRequestBody
import co.dailybook.keep.model.CreateTeamRequestBody
import co.dailybook.keep.model.TeamResponse
import co.dailybook.keep.model.UpdateTeamRequestBody
import co.dailybook.keep.model.AddStaffUserRequestBody
import co.dailybook.keep.model.AddStaffUserResponse
import co.dailybook.keep.model.AddStaffUsersRequestBody
import co.dailybook.keep.model.GetUserResponse
import co.dailybook.keep.model.MarkBulkAttendanceRequestBody
import co.dailybook.keep.model.MarkSingleAttendanceRequestBody
import co.dailybook.keep.model.OvertimeRequestBody
import co.dailybook.keep.model.SalaryData
import co.dailybook.keep.model.StaffAttendanceResponse
import co.dailybook.keep.model.StaffUserResponseModel
import co.dailybook.keep.model.UpdateUserNameRequestBody
import co.dailybook.keep.model.CurrentSalaryResponse
import co.dailybook.keep.model.subscription.CancelSubscriptionResponse
import co.dailybook.keep.model.subscription.CreateSubscriptionRequest
import co.dailybook.keep.model.subscription.CreateSubscriptionResponse
import co.dailybook.keep.model.subscription.SubscriptionPlansResponse
import co.dailybook.keep.model.subscription.UserSubscription
import co.dailybook.keep.model.subscription.VerifySubscriptionRequest
import co.dailybook.keep.model.subscription.VerifySubscriptionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface KeepApi {
    companion object {
        const val API_VERSION = "api/v1"
        const val STAFFS = "/users"
        const val STAFF = "/user"
        const val USER = "/user/{id}"
        const val USER_ATTENDANCE = "/user/{id}/attendances"
        const val MARK_BULK_ATTENDANCE = "/user/{id}/attendances"
        const val MARK_SINGLE_ATTENDANCE = "/user/{id}/attendance"
        const val ADD_ADVANCE = "/user/{id}/advance"
        const val SAVE_OVERTIME = "/user/{id}/overtime"
        const val ADD_OT = "/user/{user_id}/ot"
        const val ADVANCE_ENTRIES = "/user/{id}/advances"
        const val ADVANCE_ENTRY = "/advances/{advance_id}"
        const val TEAMS = "/teams"
        const val TEAM = "/teams/{team_id}"
        const val USER_TEAM = "/user/{id}/team"
    }

    @GET(API_VERSION + STAFFS)
    suspend fun getStaffUsers(@Query("manager_id") id : String): Response<DataResponse<StaffUserResponseModel>>

    @POST(API_VERSION + STAFFS)
    suspend fun addStaffUsers(@Body staffUsers: AddStaffUsersRequestBody): Response<DataResponse<String>>

    @POST(API_VERSION + STAFF)
    suspend fun addStaffUser(@Body staffUser: AddStaffUserRequestBody): Response<DataResponse<AddStaffUserResponse>>

    @DELETE(API_VERSION + USER)
    suspend fun deleteStaffUser(@Path("id") id: String): Response<DataResponse<String>>

    @GET(API_VERSION + USER_ATTENDANCE)
    suspend fun getUserAttendance(@Path("id") id: String, @Query("month") month: String, @Query("year") year: String): Response<DataResponse<StaffAttendanceResponse>>

    @POST(API_VERSION + SAVE_OVERTIME)
    suspend fun saveOvertime(@Path("id") id: String, @Body overtime: OvertimeRequestBody): Response<DataResponse<String>>

    @POST(API_VERSION + MARK_BULK_ATTENDANCE)
    suspend fun markBulkAttendance(@Path("id") id: String, @Body markAttendanceBody: MarkBulkAttendanceRequestBody): Response<DataResponse<String>>

    @PATCH(API_VERSION + MARK_SINGLE_ATTENDANCE)
    suspend fun markSingleAttendance(
        @Path("id") id: String,
        @Query("month") month: String,
        @Query("year") year: String,
        @Body markAttendanceBody: MarkSingleAttendanceRequestBody
    ): Response<DataResponse<String>>

    @PUT(API_VERSION + USER)
    suspend fun updateUserName(@Path("id") id: String, @Body updateUserNameRequestBody: UpdateUserNameRequestBody): Response<DataResponse<GetUserResponse>>

    @GET(API_VERSION + USER)
    suspend fun getUser(@Path("id") id: String): Response<DataResponse<GetUserResponse>>

    @PATCH(API_VERSION + ADD_ADVANCE)
    suspend fun addAdvance(@Path("id") id: String, @Body addAdvanceRequestBody: AddAdvanceRequestBody): Response<DataResponse<String>>

    @PUT(API_VERSION + ADD_OT)
    suspend fun addOvertime(
        @Path("user_id") userId: String,
        @Body body: OvertimeRequestBody
    ): Response<DataResponse<String>>

    @POST("api/v1/users/{user_id}/salaries")
    suspend fun addOrUpdateSalary(
        @Path("user_id") userId: String,
        @Body body: co.dailybook.keep.model.AddOrUpdateSalaryRequestBody
    ): Response<DataResponse<String>>

    @GET("api/v1/users/{user_id}/salaries")
    suspend fun getUserSalary(
        @Path("user_id") userId: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<DataResponse<SalaryData>>

    @GET("api/v1/users/{user_id}/salaries/current")
    suspend fun getCurrentSalary(
        @Path("user_id") userId: String
    ): Response<DataResponse<CurrentSalaryResponse>>
    
    // Subscription APIs
    @GET("api/v1/subscription-plans")
    suspend fun getSubscriptionPlans(
        @Query("user_id") userId: String
    ): Response<DataResponse<SubscriptionPlansResponse>>
    
    @GET("api/v1/users/{user_id}/subscription")
    suspend fun getUserSubscription(
        @Path("user_id") userId: String
    ): Response<DataResponse<UserSubscription>>
    
    @POST(API_VERSION + ADVANCE_ENTRIES)
    suspend fun addAdvanceEntry(
        @Path("id") userId: String,
        @Body body: AdvanceEntryRequestBody
    ): Response<DataResponse<AdvanceEntryResponse>>

    @GET(API_VERSION + ADVANCE_ENTRIES)
    suspend fun listAdvanceEntries(
        @Path("id") userId: String,
        @Query("month") month: Int,
        @Query("year") year: Int
    ): Response<DataResponse<List<AdvanceEntryResponse>>>

    @DELETE(API_VERSION + ADVANCE_ENTRY)
    suspend fun deleteAdvanceEntry(
        @Path("advance_id") advanceId: String
    ): Response<DataResponse<String>>

    @POST(API_VERSION + TEAMS)
    suspend fun createTeam(@Body body: CreateTeamRequestBody): Response<DataResponse<TeamResponse>>

    @GET(API_VERSION + TEAMS)
    suspend fun listTeams(): Response<DataResponse<List<TeamResponse>>>

    @PUT(API_VERSION + TEAM)
    suspend fun updateTeam(
        @Path("team_id") teamId: String,
        @Body body: UpdateTeamRequestBody
    ): Response<DataResponse<TeamResponse>>

    @DELETE(API_VERSION + TEAM)
    suspend fun deleteTeam(@Path("team_id") teamId: String): Response<DataResponse<String>>

    @POST(API_VERSION + USER_TEAM)
    suspend fun assignWorkerToTeam(
        @Path("id") userId: String,
        @Body body: AssignWorkerToTeamRequestBody
    ): Response<DataResponse<String>>

    @POST("api/v1/subscriptions")
    suspend fun createSubscription(
        @Query("user_id") userId: String,
        @Body request: CreateSubscriptionRequest
    ): Response<DataResponse<CreateSubscriptionResponse>>
    
    @POST("api/v1/subscriptions/{subscription_id}/verify")
    suspend fun verifySubscription(
        @Path("subscription_id") subscriptionId: String,
        @Body request: VerifySubscriptionRequest
    ): Response<DataResponse<VerifySubscriptionResponse>>
    
    @POST("api/v1/subscriptions/{subscription_id}/cancel")
    suspend fun cancelSubscription(
        @Path("subscription_id") subscriptionId: String
    ): Response<DataResponse<CancelSubscriptionResponse>>
}