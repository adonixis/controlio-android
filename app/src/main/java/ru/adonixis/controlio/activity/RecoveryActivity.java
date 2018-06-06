package ru.adonixis.controlio.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.ActivityRecoveryBinding;
import ru.adonixis.controlio.model.EmailRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RecoveryActivity extends BaseSubmitFormActivity {

    private static final String TAG = "RecoveryActivity";
    private static final String EMAIL = "email";
    private ActivityRecoveryBinding mActivityRecoveryBinding;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityRecoveryBinding = DataBindingUtil.setContentView(this, R.layout.activity_recovery);

        mActivityRecoveryBinding.btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                if (!validate()) {
                    return;
                }
                email = mActivityRecoveryBinding.inputEmail.getText().toString();
                recoverPassword(email);
            }
        });
    }

    private void recoverPassword(String email) {
        Log.d(TAG, "recover");

        mActivityRecoveryBinding.btnResetPassword.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(RecoveryActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_recover_account));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.recoverPassword(new EmailRequest(email))
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
                                Log.e(TAG, "Recovery failed: " + e);
                                onRecoveryFailed(getString(R.string.error_message_check_internet));
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
                                Log.e(TAG, "Recovery failed: " + message);
                                onRecoveryFailed(message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Recovery failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Recovery failed: ", e);
                            onRecoveryFailed(getString(R.string.error_message_check_internet));
                        } else {
                            Log.e(TAG, "Recovery failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        progressDialog.dismiss();
                        if (response.isSuccess()) {
                            onRecoverySuccess();
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    private void onRecoverySuccess() {
        mActivityRecoveryBinding.btnResetPassword.setEnabled(true);
        Intent intent = new Intent();
        intent.putExtra(EMAIL, email);
        setResult(RESULT_OK, intent);
        onBackPressed();
    }

    private void onRecoveryFailed(String message) {
        showSnackbar(
                mActivityRecoveryBinding.root,
                null,
                ContextCompat.getColor(this, R.color.red),
                Color.WHITE,
                message,
                Color.WHITE,
                getString(R.string.snackbar_action_hide),
                null
        );

        mActivityRecoveryBinding.btnResetPassword.setEnabled(true);
    }

    @Override
    protected boolean validate() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String email = mActivityRecoveryBinding.inputEmail.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mActivityRecoveryBinding.layoutInputEmail.setError(getString(R.string.error_message_enter_valid_email));
            mActivityRecoveryBinding.layoutInputEmail.startAnimation(shake);
            valid = false;
        } else {
            mActivityRecoveryBinding.layoutInputEmail.setError(null);
        }

        return valid;
    }
}
