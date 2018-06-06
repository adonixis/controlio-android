package ru.adonixis.controlio.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.BuildConfig;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.ActivityMagicLinkBinding;
import ru.adonixis.controlio.model.EmailRequest;
import ru.adonixis.controlio.model.FacebookLoginRequest;
import ru.adonixis.controlio.model.LoginMagicLinkRequest;
import ru.adonixis.controlio.model.LoginRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.model.UserResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MagicLinkActivity extends BaseSubmitFormActivity {

    private static final String TAG = "MagicLinkActivity";
    private static final String IS_LOGIN = "isLogin";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String STRIPE_ID = "stripeId";
    private static final String PLAN_ID = "planId";
    private static final String PUSH_TOKEN = "pushToken";
    private static final int REQUEST_USUAL_LOGIN = 0;
    private ActivityMagicLinkBinding mActivityMagicLinkBinding;
    private CallbackManager callbackManager;
    private String pushToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityMagicLinkBinding = DataBindingUtil.setContentView(this, R.layout.activity_magic_link);
        onNewIntent(getIntent());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        pushToken = settings.getString(PUSH_TOKEN, null);

        mActivityMagicLinkBinding.btnGetMagicLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                if (!validate()) {
                    return;
                }
                mActivityMagicLinkBinding.btnGetMagicLink.setEnabled(false);
                String email = mActivityMagicLinkBinding.inputEmail.getText().toString();
                requestMagicLink(email);
            }
        });

        callbackManager = CallbackManager.Factory.create();
        mActivityMagicLinkBinding.btnFacebookLogin.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                LoginManager.getInstance().logOut();
                signinFacebook(loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
                onSigninFailed(error.getMessage());
            }
        });


        mActivityMagicLinkBinding.linkDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String demoEmail;
                demoEmail = BuildConfig.DEMO_EMAIL;
                signin(demoEmail, BuildConfig.DEMO_PASSWORD);
            }
        });

        mActivityMagicLinkBinding.linkUsualLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MagicLinkActivity.this, UsualLoginActivity.class);
                startActivityForResult(intent, REQUEST_USUAL_LOGIN);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && data != null && !Uri.EMPTY.equals(data)) {
            String token = data.getQueryParameter("token");
            LoginMagicLinkRequest loginMagicLinkRequest = new LoginMagicLinkRequest(token, pushToken);
            signinWithMagicLink(loginMagicLinkRequest);
        }
    }

    private void requestMagicLink(String email) {
        Log.d(TAG, "requestMagicLink");

        final ProgressDialog progressDialog = new ProgressDialog(MagicLinkActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_request_magic_link));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.requestMagicLink(new EmailRequest(email))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<OkResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Signin failed: " + e);
                                onRequestMagicLinkFailed(getString(R.string.error_message_check_internet));
                                return;
                            }
                            ResponseBody body = ((HttpException) e).response().errorBody();
                            try {
                                JSONObject jObjError = new JSONObject (body.string());
                                String message = "";
                                if (jObjError.has("message")) {
                                    message = (String) jObjError.get("message");
                                } else if (jObjError.has("errors")) {
                                    JSONArray errors = (JSONArray) jObjError.get("errors");
                                    JSONObject error = (JSONObject) errors.get(0);
                                    JSONArray messages = (JSONArray) error.get("messages");
                                    message = (String) messages.get(0);
                                }
                                Log.e(TAG, "Signin failed: " + message);
                                onRequestMagicLinkFailed(message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Signin failed: ", ex);
                            }
                        } else if (e instanceof IOException) {
                            Log.e(TAG, "Signin failed: ", e);
                            onRequestMagicLinkFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Signin failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        progressDialog.dismiss();
                        if (response.isSuccess()) {
                            onRequestMagicLinkSuccess();
                        }
                    }
                });
    }

    private void signinWithMagicLink(LoginMagicLinkRequest loginMagicLinkRequest) {
        Log.d(TAG, "signinWithMagicLink");

        final ProgressDialog progressDialog = new ProgressDialog(MagicLinkActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_authenticating));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.loginMagicLink(loginMagicLinkRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UserResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Signin with magic failed: " + e);
                                onRequestMagicLinkFailed(getString(R.string.error_message_check_internet));
                                return;
                            }
                            ResponseBody body = ((HttpException) e).response().errorBody();
                            try {
                                JSONObject jObjError = new JSONObject (body.string());
                                String message = "";
                                if (jObjError.has("message")) {
                                    message = (String) jObjError.get("message");
                                } else if (jObjError.has("errors")) {
                                    JSONArray errors = (JSONArray) jObjError.get("errors");
                                    JSONObject error = (JSONObject) errors.get(0);
                                    JSONArray messages = (JSONArray) error.get("messages");
                                    message = (String) messages.get(0);
                                }
                                Log.e(TAG, "Signin with magic link failed: " + message);
                                onSigninFailed(message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Signin with magic link failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Signin with magic link failed:", e);
                            onSigninFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Signin with magic link failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(UserResponse userResponse) {
                        progressDialog.dismiss();
                        onSigninSuccess(userResponse.getToken(), userResponse.getId(), userResponse.getStripeId(), userResponse.getPlan());
                    }
                });
    }

    private void signin(String email, String password) {
        Log.d(TAG, "signin");

        final ProgressDialog progressDialog = new ProgressDialog(MagicLinkActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_authenticating));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.login(new LoginRequest(email, password, pushToken))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UserResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Signin failed: " + e);
                                onSigninFailed(getString(R.string.error_message_check_internet));
                                return;
                            }
                            ResponseBody body = ((HttpException) e).response().errorBody();
                            try {
                                JSONObject jObjError = new JSONObject (body.string());
                                String message = "";
                                if (jObjError.has("message")) {
                                    message = (String) jObjError.get("message");
                                } else if (jObjError.has("errors")) {
                                    JSONArray errors = (JSONArray) jObjError.get("errors");
                                    JSONObject error = (JSONObject) errors.get(0);
                                    JSONArray messages = (JSONArray) error.get("messages");
                                    message = (String) messages.get(0);
                                }
                                Log.e(TAG, "Signin failed: " + message);
                                onSigninFailed(message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Signin failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Signin failed: ", e);
                            onSigninFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Signin failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(UserResponse userResponse) {
                        progressDialog.dismiss();
                        onSigninSuccess(userResponse.getToken(), userResponse.getId(), userResponse.getStripeId(), userResponse.getPlan());
                    }
                });
    }

    private void signinFacebook(String accessToken) {
        Log.d(TAG, "signinFacebook");

        final ProgressDialog progressDialog = new ProgressDialog(MagicLinkActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_authenticating));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.loginFacebook(new FacebookLoginRequest(accessToken, pushToken))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UserResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Signin with Facebook failed: " + e);
                                onSigninFailed(getString(R.string.error_message_check_internet));
                                return;
                            }
                            ResponseBody body = ((HttpException) e).response().errorBody();
                            try {
                                JSONObject jObjError = new JSONObject (body.string());
                                String message = "";
                                if (jObjError.has("message")) {
                                    message = (String) jObjError.get("message");
                                } else if (jObjError.has("errors")) {
                                    JSONArray errors = (JSONArray) jObjError.get("errors");
                                    JSONObject error = (JSONObject) errors.get(0);
                                    JSONArray messages = (JSONArray) error.get("messages");
                                    message = (String) messages.get(0);
                                }
                                Log.e(TAG, "Signin with Facebook failed: " + message);
                                onSigninFailed(message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Signin with Facebook failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Signin with Facebook failed: ", e);
                            onSigninFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Signin with Facebook failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(UserResponse userResponse) {
                        progressDialog.dismiss();
                        onSigninSuccess(userResponse.getToken(), userResponse.getId(), userResponse.getStripeId(), userResponse.getPlan());
                    }
                });
    }

    private void onSigninSuccess(String token, String userId, String stripeId, int planId) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(IS_LOGIN, true);
        editor.putString(TOKEN, token);
        editor.putString(USER_ID, userId);
        editor.putString(STRIPE_ID, stripeId);
        editor.putInt(PLAN_ID, planId);
        editor.apply();
        startActivity(new Intent(MagicLinkActivity.this, MainActivity.class));
        finish();
    }

    private void onSigninFailed(String message) {
        showSnackbar(
                mActivityMagicLinkBinding.root,
                null,
                ContextCompat.getColor(this, R.color.red),
                Color.WHITE,
                message,
                Color.WHITE,
                getString(R.string.snackbar_action_hide),
                null
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_USUAL_LOGIN:
                if (resultCode == RESULT_OK) {
                    startActivity(new Intent(MagicLinkActivity.this, MainActivity.class));
                    finish();
                }
                return;
        }
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void onRequestMagicLinkSuccess() {
        showSnackbar(
                mActivityMagicLinkBinding.root,
                null,
                ContextCompat.getColor(this, R.color.dark_green_blue),
                Color.WHITE,
                getString(R.string.snackbar_message_check_inbox_magic_link),
                Color.WHITE,
                getString(R.string.snackbar_action_hide),
                null
        );

        mActivityMagicLinkBinding.btnGetMagicLink.setEnabled(true);
    }

    private void onRequestMagicLinkFailed(String message) {
        showSnackbar(
                mActivityMagicLinkBinding.root,
                null,
                ContextCompat.getColor(this, R.color.red),
                Color.WHITE,
                message,
                Color.WHITE,
                getString(R.string.snackbar_action_hide),
                null
        );

        mActivityMagicLinkBinding.btnGetMagicLink.setEnabled(true);
    }

    @Override
    protected boolean validate() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String email = mActivityMagicLinkBinding.inputEmail.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mActivityMagicLinkBinding.layoutInputEmail.setError(getString(R.string.error_message_enter_valid_email));
            mActivityMagicLinkBinding.layoutInputEmail.startAnimation(shake);
            valid = false;
        } else {
            mActivityMagicLinkBinding.layoutInputEmail.setError(null);
        }

        return valid;
    }
}