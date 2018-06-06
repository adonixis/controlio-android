package ru.adonixis.controlio.activity;

import android.app.ProgressDialog;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.adapter.EmailsAdapter;
import ru.adonixis.controlio.databinding.ActivityInviteBinding;
import ru.adonixis.controlio.model.AddClientsRequest;
import ru.adonixis.controlio.model.AddManagersRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class InviteActivity extends BaseSubmitFormActivity {

    private static final String TAG = "InviteActivity";
    private static final String IS_CLIENTS = "isClients";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String PROJECT_ID = "projectId";
    private ActivityInviteBinding mActivityInviteBinding;
    private String token;
    private String userId;
    private String projectId;
    private ArrayList<String> emails = new ArrayList<>();
    private EmailsAdapter mAdapter;
    private LinearLayoutManager layoutManager;
    private ProgressDialog progressDialog;
    private boolean isClients;

    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            emails.remove(viewHolder.getAdapterPosition());
            mAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
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
        mActivityInviteBinding = DataBindingUtil.setContentView(this, R.layout.activity_invite);

        setSupportActionBar(mActivityInviteBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            isClients = bundle.getBoolean(IS_CLIENTS);
            token = bundle.getString(TOKEN);
            userId = bundle.getString(USER_ID);
            projectId = bundle.getString(PROJECT_ID);
        }

        setTitle(isClients ? R.string.title_activity_add_clients : R.string.title_activity_add_managers);

        layoutManager = new LinearLayoutManager(this);
        mActivityInviteBinding.recyclerEmails.setLayoutManager(layoutManager);
        mActivityInviteBinding.recyclerEmails.setHasFixedSize(false);
        mAdapter = new EmailsAdapter(emails);
        mActivityInviteBinding.recyclerEmails.setAdapter(mAdapter);
        itemTouchHelper.attachToRecyclerView(mActivityInviteBinding.recyclerEmails);

        mActivityInviteBinding.btnAddEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validate()) {
                    return;
                }
                addEmailToList();
            }
        });

        mActivityInviteBinding.inputEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    if (!validate()) {
                        return false;
                    }
                    addEmailToList();
                    return true;
                }
                return false;
            }
        });

        if (isClients) {
            mActivityInviteBinding.layoutInputEmail.setError(getString(R.string.error_message_email_of_client));
        } else {
            mActivityInviteBinding.layoutInputEmail.setError(getString(R.string.error_message_email_of_manager));
        }
    }

    private void addEmailToList() {
        emails.add(0, mActivityInviteBinding.inputEmail.getText().toString());
        mAdapter.notifyItemInserted(0);
        mActivityInviteBinding.recyclerEmails.scrollToPosition(0);
        mActivityInviteBinding.inputEmail.setText("");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                return true;
            case R.id.action_save:
                if (emails.isEmpty()) {
                    finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                } else {
                    if (isClients) {
                        addClients(new AddClientsRequest(projectId, emails));
                    } else {
                        addManagers(new AddManagersRequest(projectId, emails));
                    }
                }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean validate() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String email = mActivityInviteBinding.inputEmail.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mActivityInviteBinding.layoutInputEmail.startAnimation(shake);
            valid = false;
        }

        return valid;
    }

    private void addManagers(final AddManagersRequest addManagersRequest) {
        Log.d(TAG, "addManagers");

        progressDialog = new ProgressDialog(InviteActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_adding_managers));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.addManagers(token, userId, addManagersRequest)
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
                                Log.e(TAG, "Add managers failed: " + e);
                                showSnackbar(
                                        mActivityInviteBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(InviteActivity.this, R.color.red),
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
                                Log.e(TAG, "Add managers failed: " + message);
                                showSnackbar(
                                        mActivityInviteBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(InviteActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Add managers failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Add managers failed: ", e);
                            showSnackbar(
                                    mActivityInviteBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(InviteActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Add managers failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        progressDialog.dismiss();
                        if (response.isSuccess()) {
                            setResult(RESULT_OK, null);
                            finish();
                            overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                        }
                    }
                });
    }

    private void addClients(final AddClientsRequest addClientsRequest) {
        Log.d(TAG, "addClients");

        progressDialog = new ProgressDialog(InviteActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_adding_clients));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.addClients(token, userId, addClientsRequest)
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
                                Log.e(TAG, "Add clients failed: " + e);
                                showSnackbar(
                                        mActivityInviteBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(InviteActivity.this, R.color.red),
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
                                Log.e(TAG, "Add clients failed: " + message);
                                showSnackbar(
                                        mActivityInviteBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(InviteActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Add clients failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Add clients failed: ", e);
                            showSnackbar(
                                    mActivityInviteBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(InviteActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Add clients failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        progressDialog.dismiss();
                        if (response.isSuccess()) {
                            setResult(RESULT_OK, null);
                            finish();
                            overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                        }
                    }
                });
    }
}