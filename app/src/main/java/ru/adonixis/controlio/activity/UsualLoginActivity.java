package ru.adonixis.controlio.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.ActivityUsualLoginBinding;
import ru.adonixis.controlio.databinding.DialogResetPasswordBinding;
import ru.adonixis.controlio.model.LoginRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.model.ResetPasswordRequest;
import ru.adonixis.controlio.model.UserResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class UsualLoginActivity extends BaseSubmitFormActivity {

    private boolean isSignUpScreen = true;
    private static final String TAG = "UsualLoginActivity";
    private static final String EMAIL = "email";
    private static final String IS_LOGIN = "isLogin";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String STRIPE_ID = "stripeId";
    private static final String PLAN_ID = "planId";
    private static final String PUSH_TOKEN = "pushToken";
    private static final int REQUEST_RECOVERY = 0;
    private ActivityUsualLoginBinding mActivityUsualLoginBinding;
    private DialogResetPasswordBinding dialogResetPasswordBinding;
    private String pushToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityUsualLoginBinding = DataBindingUtil.setContentView(this, R.layout.activity_usual_login);
        onNewIntent(getIntent());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        pushToken = settings.getString(PUSH_TOKEN, null);

        mActivityUsualLoginBinding.toggleBtnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivityUsualLoginBinding.toggleBtnSignup.setChecked(true);
                mActivityUsualLoginBinding.toggleBtnSignin.setChecked(false);
                mActivityUsualLoginBinding.layoutInputRepeatPassword.setVisibility(View.VISIBLE);
                mActivityUsualLoginBinding.btnSignupSignin.setText(R.string.btn_signup);
                isSignUpScreen = true;
            }
        });
        mActivityUsualLoginBinding.toggleBtnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivityUsualLoginBinding.toggleBtnSignup.setChecked(false);
                mActivityUsualLoginBinding.toggleBtnSignin.setChecked(true);
                mActivityUsualLoginBinding.layoutInputRepeatPassword.setVisibility(View.GONE);
                mActivityUsualLoginBinding.btnSignupSignin.setText(R.string.btn_signin);
                isSignUpScreen = false;
            }
        });

        mActivityUsualLoginBinding.btnSignupSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                if (!validate()) {
                    return;
                }
                mActivityUsualLoginBinding.btnSignupSignin.setEnabled(false);
                String email = mActivityUsualLoginBinding.inputEmail.getText().toString();
                String password = mActivityUsualLoginBinding.inputPassword.getText().toString();
                if (isSignUpScreen){
                    signup(email, password);
                } else {
                    signin(email, password);
                }
            }
        });

        mActivityUsualLoginBinding.linkForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UsualLoginActivity.this, RecoveryActivity.class);
                startActivityForResult(intent, REQUEST_RECOVERY);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        mActivityUsualLoginBinding.layoutInputPassword.setError(getString(R.string.error_message_password_length));
    }

    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        if (Intent.ACTION_VIEW.equals(action) && !Uri.EMPTY.equals(data)) {
            final String token = data.getQueryParameter("token");
            dialogResetPasswordBinding = DataBindingUtil.inflate(LayoutInflater.from(UsualLoginActivity.this), R.layout.dialog_reset_password, null, false);
            if ("/public/setPassword".equals(data.getPath())) {
                final AlertDialog alertDialog = new AlertDialog.Builder(UsualLoginActivity.this, R.style.AppTheme_Green_Dialog)
                        .setView(dialogResetPasswordBinding.getRoot())
                        .setTitle(R.string.dialog_title_set_password)
                        .setPositiveButton(R.string.dialog_btn_set, null)
                        .setNegativeButton(R.string.dialog_btn_cancel, null)
                        .create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!validateReset()) {
                                    return;
                                }
                                alertDialog.dismiss();
                                String password = dialogResetPasswordBinding.inputPassword.getText().toString();
                                ResetPasswordRequest setPasswordRequest = new ResetPasswordRequest(token, password);
                                setPassword(setPasswordRequest);
                            }
                        });
                    }
                });
                alertDialog.show();
            } else if ("/public/resetPassword".equals(data.getPath())) {
                final AlertDialog alertDialog = new AlertDialog.Builder(UsualLoginActivity.this, R.style.AppTheme_Green_Dialog)
                        .setView(dialogResetPasswordBinding.getRoot())
                        .setTitle(R.string.dialog_title_reset_password)
                        .setPositiveButton(R.string.dialog_btn_reset, null)
                        .setNegativeButton(R.string.dialog_btn_cancel, null)
                        .create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!validateReset()) {
                                    return;
                                }
                                alertDialog.dismiss();
                                String password = dialogResetPasswordBinding.inputPassword.getText().toString();
                                ResetPasswordRequest resetPasswordRequest = new ResetPasswordRequest(token, password);
                                resetPassword(resetPasswordRequest);
                            }
                        });
                    }
                });
                alertDialog.show();
            }
            dialogResetPasswordBinding.layoutInputPassword.setError(getString(R.string.error_message_password_length));
        }
    }

    private void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        Log.d(TAG, "resetPassword");

        final ProgressDialog progressDialog = new ProgressDialog(UsualLoginActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_reset_password));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.resetPassword(resetPasswordRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<OkResponse>() {
                    @Override
                    public final void onCompleted() {
                        progressDialog.dismiss();
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Reset password: " + e);
                                onSignupSigninFailed(getString(R.string.error_message_check_internet));
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
                                Log.e(TAG, "Reset password: " + message);
                                onSignupSigninFailed(message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Reset password: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Reset password: ", e);
                            onSignupSigninFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Reset password: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        progressDialog.dismiss();
                        if (response.isSuccess()) {
                            showSnackbar(
                                    mActivityUsualLoginBinding.root,
                                    null,
                                    ContextCompat.getColor(UsualLoginActivity.this, R.color.dark_green_blue),
                                    Color.WHITE,
                                    getString(R.string.snackbar_message_password_reset),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        }
                    }
                });
    }

    private void setPassword(ResetPasswordRequest setPasswordRequest) {
        Log.d(TAG, "setPassword");

        final ProgressDialog progressDialog = new ProgressDialog(UsualLoginActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_setting_password));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.setPassword(setPasswordRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<OkResponse>() {
                    @Override
                    public final void onCompleted() {
                        progressDialog.dismiss();
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Set password failed: " + e);
                                onSignupSigninFailed(getString(R.string.error_message_check_internet));
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
                                Log.e(TAG, "Set password failed: " + message);
                                onSignupSigninFailed(message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Set password failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Set password failed: ", e);
                            onSignupSigninFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Set password failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        progressDialog.dismiss();
                        if (response.isSuccess()) {
                            showSnackbar(
                                    mActivityUsualLoginBinding.root,
                                    null,
                                    ContextCompat.getColor(UsualLoginActivity.this, R.color.dark_green_blue),
                                    Color.WHITE,
                                    getString(R.string.snackbar_message_password_set),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        }
                    }
                });
    }

    private void signup(String email, String password) {
        Log.d(TAG, "signup");

        final ProgressDialog progressDialog = new ProgressDialog(UsualLoginActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_creating_account));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.signUp(new LoginRequest(email, password, pushToken))
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
                                Log.e(TAG, "Signup failed: " + e);
                                onSignupSigninFailed(getString(R.string.error_message_check_internet));
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
                                Log.e(TAG, "Signup failed: " + message);
                                onSignupSigninFailed( message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Signup failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Signup failed: ", e);
                            onSignupSigninFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Signup failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(UserResponse userResponse) {
                        progressDialog.dismiss();
                        onSignupSigninSuccess(userResponse.getToken(), userResponse.getId(), userResponse.getStripeId(), userResponse.getPlan());
                    }
                });
    }

    private void signin(String email, String password) {
        Log.d(TAG, "signin");

        final ProgressDialog progressDialog = new ProgressDialog(UsualLoginActivity.this, R.style.AppTheme_Light_Dialog);
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
                                onSignupSigninFailed(getString(R.string.error_message_check_internet));
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
                                onSignupSigninFailed(message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Signin failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Signin failed: ", e);
                            onSignupSigninFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Signin failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(UserResponse userResponse) {
                        progressDialog.dismiss();
                        onSignupSigninSuccess(userResponse.getToken(), userResponse.getId(), userResponse.getStripeId(), userResponse.getPlan());
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_RECOVERY:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    String email = bundle.getString(EMAIL);
                    mActivityUsualLoginBinding.inputEmail.setText(email);
                    mActivityUsualLoginBinding.toggleBtnSignin.performClick();
                    showSnackbar(
                            mActivityUsualLoginBinding.root,
                            null,
                            ContextCompat.getColor(this, R.color.dark_green_blue),
                            Color.WHITE,
                            getString(R.string.snackbar_message_check_inbox_reset_password),
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                }
        }
    }

    private void onSignupSigninSuccess(String token, String userId, String stripeId, int planId) {
        mActivityUsualLoginBinding.btnSignupSignin.setEnabled(true);
        setResult(RESULT_OK, null);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(IS_LOGIN, true);
        editor.putString(TOKEN, token);
        editor.putString(USER_ID, userId);
        editor.putString(STRIPE_ID, stripeId);
        editor.putInt(PLAN_ID, planId);
        editor.apply();
        finish();
    }

    private void onSignupSigninFailed(String message) {
        showSnackbar(
                mActivityUsualLoginBinding.root,
                null,
                ContextCompat.getColor(this, R.color.red),
                Color.WHITE,
                message,
                Color.WHITE,
                getString(R.string.snackbar_action_hide),
                null
        );

        mActivityUsualLoginBinding.btnSignupSignin.setEnabled(true);
    }

    @Override
    protected boolean validate() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String email = mActivityUsualLoginBinding.inputEmail.getText().toString();
        String password = mActivityUsualLoginBinding.inputPassword.getText().toString();
        String repeatPassword = mActivityUsualLoginBinding.inputRepeatPassword.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mActivityUsualLoginBinding.layoutInputEmail.setError(getString(R.string.error_message_enter_valid_email));
            mActivityUsualLoginBinding.layoutInputEmail.startAnimation(shake);
            valid = false;
        } else {
            mActivityUsualLoginBinding.layoutInputEmail.setError(null);
        }

        if (password.length() < 8 || password.length() > 30) {
            mActivityUsualLoginBinding.layoutInputPassword.startAnimation(shake);
            valid = false;
        }

        if (isSignUpScreen) {
            if (!repeatPassword.equals(password)) {
                mActivityUsualLoginBinding.layoutInputRepeatPassword.setError(getString(R.string.error_message_password_dont_match));
                mActivityUsualLoginBinding.layoutInputPassword.startAnimation(shake);
                mActivityUsualLoginBinding.layoutInputRepeatPassword.startAnimation(shake);
                valid = false;
            } else {
                mActivityUsualLoginBinding.layoutInputRepeatPassword.setError(null);
            }
        }

        return valid;
    }

    private boolean validateReset() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String password = dialogResetPasswordBinding.inputPassword.getText().toString();

        if (password.length() < 8 || password.length() > 30) {
            dialogResetPasswordBinding.layoutInputPassword.startAnimation(shake);
            valid = false;
        }

        return valid;
    }
}
