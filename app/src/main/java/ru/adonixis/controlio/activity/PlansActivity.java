package ru.adonixis.controlio.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.BuildConfig;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.ActivityPlansBinding;
import ru.adonixis.controlio.databinding.DialogNewCardBinding;
import ru.adonixis.controlio.databinding.DialogRedeemCouponBinding;
import ru.adonixis.controlio.model.CouponRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.model.StripeCustomerResponse;
import ru.adonixis.controlio.model.StripeSourceRequest;
import ru.adonixis.controlio.model.StripeSourceResponse;
import ru.adonixis.controlio.model.SubscriptionRequest;
import ru.adonixis.controlio.model.UserResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PlansActivity extends BaseActivity {

    private static final String TAG = "PlansActivity";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String STRIPE_ID = "stripeId";
    private static final String PLAN_ID = "planId";
    private static final String[] plans = new String[] {"Free", "$4.99", "$19.99", "$49.99"};
    private ActivityPlansBinding mActivityPlansBinding;
    private DialogNewCardBinding dialogNewCardBinding;
    private DialogRedeemCouponBinding dialogRedeemCouponBinding;
    private String token;
    private String userId;
    private String stripeId;
    private int newPlanId;
    private int currentPlanId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityPlansBinding = DataBindingUtil.setContentView(this, R.layout.activity_plans);

        setSupportActionBar(mActivityPlansBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        token = intent.getStringExtra(TOKEN);
        userId = intent.getStringExtra(USER_ID);
        stripeId = intent.getStringExtra(STRIPE_ID);
        currentPlanId = intent.getIntExtra(PLAN_ID, 0);

        mActivityPlansBinding.content.setPlanId(currentPlanId);

        mActivityPlansBinding.content.btnFree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newPlanId = 0;
                if (newPlanId == currentPlanId) {
                    mActivityPlansBinding.content.btnFree.setChecked(true);
                } else {
                    mActivityPlansBinding.content.btnFree.setChecked(false);
                    AlertDialog alertDialog = new AlertDialog.Builder(PlansActivity.this)
                            .setTitle(R.string.dialog_title_confirmation)
                            .setMessage(getString(R.string.dialog_message_switch_plan, plans[newPlanId]))
                            .setPositiveButton(R.string.dialog_btn_switch, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    subscribe(newPlanId);
                                }
                            })
                            .setNegativeButton(R.string.dialog_btn_cancel, null)
                            .create();
                    alertDialog.show();
                }
            }
        });
        mActivityPlansBinding.content.btnFive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newPlanId = 1;
                if (newPlanId == currentPlanId) {
                    mActivityPlansBinding.content.btnFive.setChecked(true);
                } else {
                    mActivityPlansBinding.content.btnFive.setChecked(false);
                    getStripeCustomer(stripeId);
                }
            }
        });
        mActivityPlansBinding.content.btnTwenty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivityPlansBinding.content.btnTwenty.setChecked(false);
                newPlanId = 2;
                if (newPlanId == currentPlanId) {
                    mActivityPlansBinding.content.btnTwenty.setChecked(true);
                } else {
                    mActivityPlansBinding.content.btnTwenty.setChecked(false);
                    getStripeCustomer(stripeId);
                }
            }
        });
        mActivityPlansBinding.content.btnFifty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivityPlansBinding.content.btnFifty.setChecked(false);
                newPlanId = 3;
                if (newPlanId == currentPlanId) {
                    mActivityPlansBinding.content.btnFifty.setChecked(true);
                } else {
                    mActivityPlansBinding.content.btnFifty.setChecked(false);
                    getStripeCustomer(stripeId);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_redeem, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_redeem:
                dialogRedeemCouponBinding = DataBindingUtil.inflate(LayoutInflater.from(PlansActivity.this), R.layout.dialog_redeem_coupon, null, false);
                final AlertDialog alertDialog = new AlertDialog.Builder(PlansActivity.this, R.style.AppTheme_Light_Dialog)
                        .setView(dialogRedeemCouponBinding.getRoot())
                        .setTitle(R.string.dialog_title_redeem_coupon)
                        .setPositiveButton(R.string.dialog_btn_redeem, null)
                        .setNegativeButton(R.string.dialog_btn_cancel, null)
                        .create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (!validateCoupon()) {
                                    return;
                                }
                                alertDialog.dismiss();
                                String coupon = dialogRedeemCouponBinding.inputCoupon.getText().toString();
                                CouponRequest couponRequest = new CouponRequest(userId, coupon);
                                applyCoupon(couponRequest);
                            }
                        });
                    }
                });
                if (alertDialog.getWindow() != null) {
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                alertDialog.show();
                return true;
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getStripeCustomer(final String stripeId) {
        Log.d(TAG, "getStripeCustomer");

        progressDialog = new ProgressDialog(PlansActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_getting_payment_methods));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.getStripeCustomer(token, userId, stripeId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<StripeCustomerResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Get stripe customer: " + e);
                                showSnackbar(
                                        mActivityPlansBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                                        Color.WHITE,
                                        getString(R.string.error_message_check_internet),
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
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
                                Log.e(TAG, "Get stripe customer: " + message);
                                showSnackbar(
                                        mActivityPlansBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Get stripe customer: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Get stripe customer: ", e);
                            showSnackbar(
                                    mActivityPlansBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PlansActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Get stripe customer: ", e);
                        }
                    }

                    @Override
                    public void onNext(StripeCustomerResponse stripeCustomerResponse) {
                        progressDialog.dismiss();
                        if (stripeCustomerResponse.getSources().getData().isEmpty()) {
                            dialogNewCardBinding = DataBindingUtil.inflate(LayoutInflater.from(PlansActivity.this), R.layout.dialog_new_card, null, false);
                            final AlertDialog alertDialog = new AlertDialog.Builder(PlansActivity.this)
                                    .setView(dialogNewCardBinding.getRoot())
                                    .setTitle(R.string.dialog_title_add_payment)
                                    .setPositiveButton(R.string.dialog_btn_submit, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            submitCard();
                                        }
                                    })
                                    .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                        }
                                    })
                                    .create();
                            if (alertDialog.getWindow() != null) {
                                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                            }
                            alertDialog.show();
                        } else {
                            AlertDialog alertDialog = new AlertDialog.Builder(PlansActivity.this)
                                    .setTitle(R.string.dialog_title_confirmation)
                                    .setMessage(getString(R.string.dialog_message_switch_plan, plans[newPlanId]))
                                    .setPositiveButton(R.string.dialog_btn_switch, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            subscribe(newPlanId);
                                        }
                                    })
                                    .setNegativeButton(R.string.dialog_btn_cancel, null)
                                    .create();
                            alertDialog.show();
                        }
                    }
                });
    }

    private void subscribe(final int planId) {
        Log.d(TAG, "subscribe");

        progressDialog = new ProgressDialog(PlansActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_subscription));
        progressDialog.show();

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(userId, planId);
        ControlioService service = ServiceFactory.getControlioService(this);
        service.subscribe(token, userId, subscriptionRequest)
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
                                Log.e(TAG, "Subscription failed: " + e);
                                showSnackbar(
                                        mActivityPlansBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                                        Color.WHITE,
                                        getString(R.string.error_message_check_internet),
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
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
                                Log.e(TAG, "Subscription failed: " + message);
                                showSnackbar(
                                        mActivityPlansBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Subscription failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Subscription failed: ", e);
                            showSnackbar(
                                    mActivityPlansBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PlansActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Subscription failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(UserResponse userResponse) {
                        progressDialog.dismiss();
                        currentPlanId = userResponse.getPlan();
                        mActivityPlansBinding.content.setPlanId(currentPlanId);
                        Intent intent = new Intent();
                        intent.putExtra(PLAN_ID, currentPlanId);
                        setResult(RESULT_OK, intent);
                    }
                });
    }

    private void submitCard() {
        Log.d(TAG, "submitCard");

        progressDialog = new ProgressDialog(PlansActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_adding_payment));
        progressDialog.show();

        final String publishableApiKey;
        if (BuildConfig.DEBUG) {
            publishableApiKey = BuildConfig.STRIPE_PUBLISHABLE_KEY_TEST;
        } else {
            publishableApiKey = BuildConfig.STRIPE_PUBLISHABLE_KEY_LIVE;
        }

        Card card = dialogNewCardBinding.cardInputWidget.getCard();
        if (card == null) {
            progressDialog.dismiss();
            showSnackbar(
                    mActivityPlansBinding.coordinator,
                    null,
                    ContextCompat.getColor(PlansActivity.this, R.color.red),
                    Color.WHITE,
                    "Invalid card data",
                    Color.WHITE,
                    getString(R.string.snackbar_action_hide),
                    null
            );
            return;
        }

        Stripe stripe = new Stripe(this);
        stripe.createToken(card, publishableApiKey, new TokenCallback() {
            public void onSuccess(Token token) {
                String stripeSourceToken = token.getId();
                addStripeSource(stripeId, stripeSourceToken);
            }

            public void onError(Exception error) {
                progressDialog.cancel();
                Log.e(TAG, "onError: ", error);
                showSnackbar(
                        mActivityPlansBinding.coordinator,
                        null,
                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                        Color.WHITE,
                        error.getLocalizedMessage(),
                        Color.WHITE,
                        getString(R.string.snackbar_action_hide),
                        null
                );

                dialogNewCardBinding = DataBindingUtil.inflate(LayoutInflater.from(PlansActivity.this), R.layout.dialog_new_card, null, false);
                AlertDialog alertDialog = new AlertDialog.Builder(PlansActivity.this)
                        .setView(dialogNewCardBinding.getRoot())
                        .setTitle(R.string.dialog_title_add_payment)
                        .setPositiveButton(R.string.dialog_btn_submit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                submitCard();
                            }
                        })
                        .setNegativeButton(R.string.dialog_btn_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .create();
                if (alertDialog.getWindow() != null) {
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                alertDialog.show();
            }
        });
    }

    private void addStripeSource(final String customerid, final String source) {
        Log.d(TAG, "addStripeSource");

        StripeSourceRequest stripeSourceRequest = new StripeSourceRequest(customerid, source);
        ControlioService service = ServiceFactory.getControlioService(this);
        service.addStripeSource(token, userId, stripeSourceRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<StripeSourceResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Adding Stripe source failed: " + e);
                                showSnackbar(
                                        mActivityPlansBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                                        Color.WHITE,
                                        getString(R.string.error_message_check_internet),
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
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
                                Log.e(TAG, "Adding Stripe source failed: " + message);
                                showSnackbar(
                                        mActivityPlansBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Adding Stripe source failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Adding Stripe source failed: ", e);
                            showSnackbar(
                                    mActivityPlansBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PlansActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Adding Stripe source failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(StripeSourceResponse stripeSourceResponse) {
                        progressDialog.dismiss();
                        AlertDialog alertDialog = new AlertDialog.Builder(PlansActivity.this)
                                .setTitle(R.string.dialog_title_confirmation)
                                .setMessage(getString(R.string.dialog_message_switch_plan, plans[newPlanId]))
                                .setPositiveButton(R.string.dialog_btn_switch, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        subscribe(newPlanId);
                                    }
                                })
                                .setNegativeButton(R.string.dialog_btn_cancel, null)
                                .create();
                        alertDialog.show();
                    }
                });
    }

    private void applyCoupon(CouponRequest couponRequest) {
        Log.d(TAG, "applyCoupon");

        progressDialog = new ProgressDialog(PlansActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_applying_coupon));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.applyCoupon(token, userId, couponRequest)
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
                                Log.e(TAG, "Applying coupon failed: " + e);
                                showSnackbar(
                                        mActivityPlansBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                                        Color.WHITE,
                                        getString(R.string.error_message_check_internet),
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
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
                                Log.e(TAG, "Applying coupon failed: " + message);
                                showSnackbar(
                                        mActivityPlansBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PlansActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Applying coupon failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Applying coupon failed: ", e);
                            showSnackbar(
                                    mActivityPlansBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PlansActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Applying coupon failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse okResponse) {
                        progressDialog.dismiss();
                        if (okResponse.isSuccess()) {
                            showSnackbar(
                                    mActivityPlansBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PlansActivity.this, R.color.dark_green_blue),
                                    Color.WHITE,
                                    getString(R.string.snackbar_message_coupon_applied),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        }
                    }
                });
    }

    private boolean validateCoupon() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String coupon = dialogRedeemCouponBinding.inputCoupon.getText().toString();

        if (TextUtils.isEmpty(coupon)) {
            dialogRedeemCouponBinding.layoutInputCoupon.startAnimation(shake);
            valid = false;
        }

        return valid;
    }
}