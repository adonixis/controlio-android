package ru.adonixis.controlio.network;

import java.util.List;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import ru.adonixis.controlio.BuildConfig;
import ru.adonixis.controlio.model.AcceptInviteRequest;
import ru.adonixis.controlio.model.AddClientsRequest;
import ru.adonixis.controlio.model.AddManagersRequest;
import ru.adonixis.controlio.model.ChangePushTokenRequest;
import ru.adonixis.controlio.model.CouponRequest;
import ru.adonixis.controlio.model.DeleteCardRequest;
import ru.adonixis.controlio.model.DeleteClientRequest;
import ru.adonixis.controlio.model.DeleteManagerRequest;
import ru.adonixis.controlio.model.DeletePostRequest;
import ru.adonixis.controlio.model.EditPostRequest;
import ru.adonixis.controlio.model.EditProfileRequest;
import ru.adonixis.controlio.model.EditProgressRequest;
import ru.adonixis.controlio.model.EditProjectRequest;
import ru.adonixis.controlio.model.EmailRequest;
import ru.adonixis.controlio.model.FacebookLoginRequest;
import ru.adonixis.controlio.model.FeaturesResponse;
import ru.adonixis.controlio.model.InviteDetailsResponse;
import ru.adonixis.controlio.model.InviteIdRequest;
import ru.adonixis.controlio.model.LoginMagicLinkRequest;
import ru.adonixis.controlio.model.LoginRequest;
import ru.adonixis.controlio.model.LogoutRequest;
import ru.adonixis.controlio.model.NewPostRequest;
import ru.adonixis.controlio.model.NewProjectRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.model.PostDetailsResponse;
import ru.adonixis.controlio.model.PostResponse;
import ru.adonixis.controlio.model.ProjectDetailsResponse;
import ru.adonixis.controlio.model.ProjectIdRequest;
import ru.adonixis.controlio.model.ProjectResponse;
import ru.adonixis.controlio.model.ProjectResponseTemp;
import ru.adonixis.controlio.model.ResetPasswordRequest;
import ru.adonixis.controlio.model.StripeCustomerResponse;
import ru.adonixis.controlio.model.StripeSourceRequest;
import ru.adonixis.controlio.model.StripeSourceResponse;
import ru.adonixis.controlio.model.SubscriptionRequest;
import ru.adonixis.controlio.model.UserResponse;
import rx.Observable;

public interface ControlioService {

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @GET("feature_list")
    Observable<FeaturesResponse> getFeatures();


    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/changeAndroidToken")
    Observable<OkResponse> changePushToken(@Header("token") String token, @Header("userId") String userId, @Body ChangePushTokenRequest changePushTokenRequest);


    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/login")
    Observable<UserResponse> login(@Body LoginRequest loginRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/loginFacebook")
    Observable<UserResponse> loginFacebook(@Body FacebookLoginRequest facebookLoginRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/signUp")
    Observable<UserResponse> signUp(@Body LoginRequest loginRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/recoverPassword")
    Observable<OkResponse> recoverPassword(@Body EmailRequest emailRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/resetPassword")
    Observable<OkResponse> resetPassword(@Body ResetPasswordRequest resetPasswordRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/setPassword")
    Observable<OkResponse> setPassword(@Body ResetPasswordRequest setPasswordRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/requestMagicLink")
    Observable<OkResponse> requestMagicLink(@Body EmailRequest emailRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/loginMagicLink")
    Observable<UserResponse> loginMagicLink(@Body LoginMagicLinkRequest loginMagicLinkRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/logout")
    Observable<UserResponse> logout(@Header("token") String token, @Header("userId") String userId, @Body LogoutRequest logoutRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @GET("users/profile")
    Observable<UserResponse> getProfile(@Header("token") String token, @Header("userId") String userId, @Query("id") String id);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("users/profile")
    Observable<UserResponse> editProfile(@Header("token") String token, @Header("userId") String userId, @Body EditProfileRequest editProfileRequest);


    @Headers("apiKey: " + BuildConfig.API_KEY)
    @GET("projects")
    Observable<List<ProjectResponse>> getProjects(@Header("token") String token, @Header("userId") String userId, @Query("skip") Integer skip, @Query("limit") Integer limit, @Query("type") String type, @Query("query") String query);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @GET("projects/project")
    Observable<ProjectDetailsResponse> getProjectById(@Header("token") String token, @Header("userId") String userId, @Query("projectid") String projectId);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("projects")
    Observable<ProjectResponse> createProject(@Header("token") String token, @Header("userId") String userId, @Body NewProjectRequest newProjectRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @PUT("projects")
    Observable<ProjectResponse> editProject(@Header("token") String token, @Header("userId") String userId, @Body EditProjectRequest editProjectRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @PUT("projects/progress")
    Observable<OkResponse> editProgress(@Header("token") String token, @Header("userId") String userId, @Body EditProgressRequest editProgressRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("projects/clients")
    Observable<OkResponse> addClients(@Header("token") String token, @Header("userId") String userId, @Body AddClientsRequest addClientsRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @HTTP(method = "DELETE", path = "projects/client", hasBody = true)
    Observable<OkResponse> deleteClient(@Header("token") String token, @Header("userId") String userId, @Body DeleteClientRequest deleteClientRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("projects/managers")
    Observable<OkResponse> addManagers(@Header("token") String token, @Header("userId") String userId, @Body AddManagersRequest addManagersRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @HTTP(method = "DELETE", path = "projects/manager", hasBody = true)
    Observable<OkResponse> deleteManager(@Header("token") String token, @Header("userId") String userId, @Body DeleteManagerRequest deleteManagerRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("projects/finish")
    Observable<ProjectResponseTemp> finishProject(@Header("token") String token, @Header("userId") String userId, @Body ProjectIdRequest projectIdRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("projects/revive")
    Observable<ProjectResponseTemp> reviveProject(@Header("token") String token, @Header("userId") String userId, @Body ProjectIdRequest projectIdRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("projects/leave")
    Observable<OkResponse> leaveProject(@Header("token") String token, @Header("userId") String userId, @Body ProjectIdRequest projectIdRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @HTTP(method = "DELETE", path = "projects", hasBody = true)
    Observable<OkResponse> deleteProject(@Header("token") String token, @Header("userId") String userId, @Body ProjectIdRequest projectIdRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @GET("projects/invites")
    Observable<List<InviteDetailsResponse>> getInvites(@Header("token") String token, @Header("userId") String userId);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("projects/invite")
    Observable<OkResponse> acceptOrRejectInvite(@Header("token") String token, @Header("userId") String userId, @Body AcceptInviteRequest acceptInviteRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @HTTP(method = "DELETE", path = "projects/invite", hasBody = true)
    Observable<OkResponse> deleteInvite(@Header("token") String token, @Header("userId") String userId, @Body InviteIdRequest inviteIdRequest);


    @Headers("apiKey: " + BuildConfig.API_KEY)
    @GET("posts")
    Observable<List<PostDetailsResponse>> getPosts(@Header("token") String token, @Header("userId") String userId, @Query("projectid") String projectId, @Query("skip") Integer skip, @Query("limit") Integer limit);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("posts")
    Observable<PostResponse> createPost(@Header("token") String token, @Header("userId") String userId, @Body NewPostRequest newPostRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @PUT("posts")
    Observable<PostResponse> editPost(@Header("token") String token, @Header("userId") String userId, @Body EditPostRequest editPostRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @HTTP(method = "DELETE", path = "posts", hasBody = true)
    Observable<OkResponse> deletePost(@Header("token") String token, @Header("userId") String userId, @Body DeletePostRequest deletePostRequest);


    @Headers("apiKey: " + BuildConfig.API_KEY)
    @GET("payments/customer")
    Observable<StripeCustomerResponse> getStripeCustomer(@Header("token") String token, @Header("userId") String userId, @Query("customerid") String customerId);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("payments/customer/sources")
    Observable<StripeSourceResponse> addStripeSource(@Header("token") String token, @Header("userId") String userId, @Body StripeSourceRequest stripeSourceRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("payments/customer/default_source")
    Observable<StripeCustomerResponse> setDefaultStripeSource(@Header("token") String token, @Header("userId") String userId, @Body StripeSourceRequest stripeSourceRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("payments/customer/subscription")
    Observable<UserResponse> subscribe(@Header("token") String token, @Header("userId") String userId, @Body SubscriptionRequest subscriptionRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @POST("payments/customer/coupon")
    Observable<OkResponse> applyCoupon(@Header("token") String token, @Header("userId") String userId, @Body CouponRequest couponRequest);

    @Headers("apiKey: " + BuildConfig.API_KEY)
    @HTTP(method = "DELETE", path = "payments/customer/card", hasBody = true)
    Observable<OkResponse> deleteCard(@Header("token") String token, @Header("userId") String userId, @Body DeleteCardRequest deleteCardRequest);

}