package ru.adonixis.controlio.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.BuildConfig;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.adapter.CardsAdapter;
import ru.adonixis.controlio.databinding.ActivityPaymentMethodsBinding;
import ru.adonixis.controlio.databinding.DialogNewCardBinding;
import ru.adonixis.controlio.listener.OnCardClickListener;
import ru.adonixis.controlio.model.DeleteCardRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.model.StripeCustomerResponse;
import ru.adonixis.controlio.model.StripeSourceRequest;
import ru.adonixis.controlio.model.StripeSourceResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class PaymentMethodsActivity extends BaseActivity {

    private static final String TAG = "PaymentMethodsActivity";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String STRIPE_ID = "stripeId";
    private String token;
    private String userId;
    private String stripeId;
    private ProgressDialog progressDialog;
    private ActivityPaymentMethodsBinding mActivityPaymentMethodsBinding;
    private DialogNewCardBinding dialogNewCardBinding;
    private CardsAdapter cardsAdapter;
    private List<StripeSourceResponse> cards = new ArrayList<>();

    private OnCardClickListener onCardTouchListener = new OnCardClickListener() {
        @Override
        public void onCardClick(View view, int position) {
            progressDialog = new ProgressDialog(PaymentMethodsActivity.this, R.style.AppTheme_Light_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(getString(R.string.progress_message_setting_default_source));
            progressDialog.show();
            setDefaultStripeSource(new StripeSourceRequest(stripeId, cards.get(position).getId()));
        }
    };

    private final ItemTouchHelper itemCardTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
            deleteCard(viewHolder.getAdapterPosition());
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                Paint p = new Paint();
                Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                View itemView = viewHolder.itemView;
                float height = (float) itemView.getBottom() - (float) itemView.getTop();
                float width = (float) itemView.getRight() - (float) itemView.getLeft();

                if (dX < 0) {
                    p.setColor(Color.parseColor("#FE4128"));
                    RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(),(float) itemView.getRight(), (float) itemView.getBottom());
                    c.drawRect(background, p);
                    textPaint.setTextSize(50);
                    textPaint.setColor(Color.WHITE);
                    textPaint.setStyle(Paint.Style.STROKE);
                    c.drawText(getString(R.string.action_delete), (float) itemView.getRight() - 200, (float) itemView.getBottom() - (height / 2) + 15, textPaint);
                }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityPaymentMethodsBinding = DataBindingUtil.setContentView(this, R.layout.activity_payment_methods);

        setSupportActionBar(mActivityPaymentMethodsBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            token = bundle.getString(TOKEN);
            userId = bundle.getString(USER_ID);
            stripeId = bundle.getString(STRIPE_ID);
        }

        mActivityPaymentMethodsBinding.content.recyclerCards.setLayoutManager(new LinearLayoutManager(this));
        mActivityPaymentMethodsBinding.content.recyclerCards.setHasFixedSize(false);
        cardsAdapter = new CardsAdapter(cards, onCardTouchListener);
        mActivityPaymentMethodsBinding.content.recyclerCards.setAdapter(cardsAdapter);
        itemCardTouchHelper.attachToRecyclerView(mActivityPaymentMethodsBinding.content.recyclerCards);

        mActivityPaymentMethodsBinding.content.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.green_blue));
        mActivityPaymentMethodsBinding.content.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showRefreshing();
                getStripeCustomer(stripeId);
            }
        });

        mActivityPaymentMethodsBinding.content.layoutAddCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogNewCardBinding = DataBindingUtil.inflate(LayoutInflater.from(PaymentMethodsActivity.this), R.layout.dialog_new_card, null, false);
                final AlertDialog alertDialog = new AlertDialog.Builder(PaymentMethodsActivity.this)
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

        progressDialog = new ProgressDialog(PaymentMethodsActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_getting_payment_methods));
        progressDialog.show();
        getStripeCustomer(stripeId);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getStripeCustomer(final String stripeId) {
        Log.d(TAG, "getStripeCustomer");

        ControlioService service = ServiceFactory.getControlioService(this);
        service.getStripeCustomer(token, userId, stripeId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<StripeCustomerResponse>() {
                    @Override
                    public final void onCompleted() {
                        progressDialog.dismiss();
                        hideRefreshing();
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        hideRefreshing();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Get stripe customer: " + e);
                                showSnackbar(
                                        mActivityPaymentMethodsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                                        mActivityPaymentMethodsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                                    mActivityPaymentMethodsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                        if (stripeCustomerResponse.getSources().getData().isEmpty()) {
                            dialogNewCardBinding = DataBindingUtil.inflate(LayoutInflater.from(PaymentMethodsActivity.this), R.layout.dialog_new_card, null, false);
                            final AlertDialog alertDialog = new AlertDialog.Builder(PaymentMethodsActivity.this)
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
                            cards.clear();
                            List<StripeSourceResponse> sources = stripeCustomerResponse.getSources().getData();
                            for (StripeSourceResponse source : sources) {
                                source.setDefaultSource(source.getId().equals(stripeCustomerResponse.getDefaultSource()));
                            }
                            cards.addAll(sources);
                            cardsAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void submitCard() {
        Log.d(TAG, "submitCard");

        progressDialog = new ProgressDialog(PaymentMethodsActivity.this, R.style.AppTheme_Light_Dialog);
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
                    mActivityPaymentMethodsBinding.coordinator,
                    null,
                    ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                        mActivityPaymentMethodsBinding.coordinator,
                        null,
                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
                        Color.WHITE,
                        error.getLocalizedMessage(),
                        Color.WHITE,
                        getString(R.string.snackbar_action_hide),
                        null
                );

                dialogNewCardBinding = DataBindingUtil.inflate(LayoutInflater.from(PaymentMethodsActivity.this), R.layout.dialog_new_card, null, false);
                final AlertDialog alertDialog = new AlertDialog.Builder(PaymentMethodsActivity.this)
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
                                        mActivityPaymentMethodsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                                        mActivityPaymentMethodsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                                    mActivityPaymentMethodsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                        getStripeCustomer(stripeId);
                    }
                });
    }

    private void setDefaultStripeSource(StripeSourceRequest stripeSourceRequest) {
        Log.d(TAG, "setDefaultStripeSource");

        ControlioService service = ServiceFactory.getControlioService(this);
        service.setDefaultStripeSource(token, userId, stripeSourceRequest)
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
                                Log.e(TAG, "Setting default Stripe source failed: " + e);
                                showSnackbar(
                                        mActivityPaymentMethodsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                                Log.e(TAG, "Setting default Stripe source failed: " + message);
                                showSnackbar(
                                        mActivityPaymentMethodsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Setting default Stripe source failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Setting default Stripe source failed: ", e);
                            showSnackbar(
                                    mActivityPaymentMethodsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Setting default Stripe source failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(StripeCustomerResponse stripeSourceResponse) {
                        getStripeCustomer(stripeId);
                    }
                });
    }

    private void deleteCard(final int position) {
        Log.d(TAG, "deleteCard");

        final StripeSourceResponse card = cards.get(position);
        final String cardId = card.getId();

        cards.remove(position);
        cardsAdapter.notifyItemRemoved(position);

        Snackbar.Callback callback = new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                switch (event) {
                    case Snackbar.Callback.DISMISS_EVENT_ACTION:
                        break;
                    default:
                        ControlioService service = ServiceFactory.getControlioService(PaymentMethodsActivity.this);
                        service.deleteCard(token, userId, new DeleteCardRequest(stripeId, cardId))
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<OkResponse>() {
                                    @Override
                                    public final void onCompleted() {
                                    }

                                    @Override
                                    public final void onError(Throwable e) {
                                        cards.add(position, card);
                                        cardsAdapter.notifyItemInserted(position);
                                        if (e instanceof HttpException) {
                                            if ((((HttpException) e).code() == 504)) {
                                                Log.e(TAG, "Delete card failed: " + e);
                                                showSnackbar(
                                                        mActivityPaymentMethodsBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
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
                                                Log.e(TAG, "Delete card failed: " + message);
                                                showSnackbar(
                                                        mActivityPaymentMethodsBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
                                                        Color.WHITE,
                                                        message,
                                                        Color.WHITE,
                                                        getString(R.string.snackbar_action_hide),
                                                        null
                                                );
                                            } catch (JSONException | IOException ex) {
                                                Log.e(TAG, "Delete card failed: ", ex);
                                            }
                                        } else if (e instanceof IOException) {
                                            Log.e(TAG, "Delete card failed: ", e);
                                            showSnackbar(
                                                    mActivityPaymentMethodsBinding.coordinator,
                                                    null,
                                                    ContextCompat.getColor(PaymentMethodsActivity.this, R.color.red),
                                                    Color.WHITE,
                                                    getString(R.string.error_message_check_internet),
                                                    Color.WHITE,
                                                    getString(R.string.snackbar_action_hide),
                                                    null
                                            );
                                        } else {
                                            Log.e(TAG, "Delete card failed: ", e);
                                        }
                                    }

                                    @Override
                                    public void onNext(OkResponse response) {
                                        Log.d(TAG, response.toString());
                                    }
                                });
                        break;
                }
            }

            @Override
            public void onShown(Snackbar snackbar) {
            }
        };

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cards.add(position, card);
                cardsAdapter.notifyItemInserted(position);
            }
        };

        showSnackbar(
                mActivityPaymentMethodsBinding.coordinator,
                callback,
                ContextCompat.getColor(this, R.color.dark_green_blue),
                Color.WHITE,
                getString(R.string.snackbar_message_card_deleted),
                Color.WHITE,
                getString(R.string.snackbar_action_undo),
                onClickListener
        );
    }

    private void showRefreshing() {
        mActivityPaymentMethodsBinding.content.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityPaymentMethodsBinding.content.swipeRefresh.setRefreshing(true);
            }
        });
    }

    private void hideRefreshing() {
        mActivityPaymentMethodsBinding.content.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityPaymentMethodsBinding.content.swipeRefresh.setRefreshing(false);
            }
        });
    }
}