package ru.adonixis.controlio.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.adapter.InvitesAdapter;
import ru.adonixis.controlio.adapter.UsersAdapter;
import ru.adonixis.controlio.databinding.ActivityProjectInfoBinding;
import ru.adonixis.controlio.databinding.DialogFullImageBinding;
import ru.adonixis.controlio.listener.OnUserClickListener;
import ru.adonixis.controlio.model.DeleteClientRequest;
import ru.adonixis.controlio.model.DeleteManagerRequest;
import ru.adonixis.controlio.model.InviteIdRequest;
import ru.adonixis.controlio.model.InviteResponse;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.model.ProjectDetailsResponse;
import ru.adonixis.controlio.model.ProjectIdRequest;
import ru.adonixis.controlio.model.ProjectResponse;
import ru.adonixis.controlio.model.ProjectResponseTemp;
import ru.adonixis.controlio.model.UserResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static ru.adonixis.controlio.model.InviteResponse.TYPE_CLIENT;
import static ru.adonixis.controlio.model.InviteResponse.TYPE_MANAGE;
import static ru.adonixis.controlio.model.InviteResponse.TYPE_OWNER;

public class ProjectInfoActivity extends BaseActivity {

    private static final String TAG = "ProjectInfoActivity";
    private static final int REQUEST_INVITE_MANAGERS = 0;
    private static final int REQUEST_INVITE_CLIENTS = 1;
    private static final int REQUEST_EDIT_PROJECT = 2;
    private static final String IS_CLIENTS = "isClients";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String PROJECT = "project";
    private static final String PROJECT_INFO = "projectInfo";
    private static final String PROJECT_ID = "projectId";
    private static final String USER = "user";
    private ProjectResponse project;
    private ProjectDetailsResponse projectInfo;
    private String token;
    private String userId;
    private ProgressDialog progressDialog;
    private ActivityProjectInfoBinding mActivityProjectInfoBinding;
    private UsersAdapter managersAdapter;
    private UsersAdapter clientsAdapter;
    private InvitesAdapter managersInvitedAdapter;
    private InvitesAdapter clientsInvitedAdapter;
    private List<UserResponse> managers = new ArrayList<>();
    private List<UserResponse> clients = new ArrayList<>();
    private List<InviteResponse> managersInvited = new ArrayList<>();
    private List<InviteResponse> clientsInvited = new ArrayList<>();
    private UserResponse ownerInvited;

    private OnUserClickListener onUserClickListener = new OnUserClickListener() {
        @Override
        public void onUserClick(View view, UserResponse user) {
            Intent intent = new Intent(ProjectInfoActivity.this, UserInfoActivity.class);
            intent.putExtra(USER, user);
            startActivity(intent);
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
    };

    private final ItemTouchHelper itemManagerTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
            deleteManager(viewHolder.getAdapterPosition());
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

    private final ItemTouchHelper itemClientTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
            deleteClient(viewHolder.getAdapterPosition());
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

    private final ItemTouchHelper itemManagerInvitedTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
            deleteManagerInvited(viewHolder.getAdapterPosition());
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

    private final ItemTouchHelper itemClientInvitedTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
            deleteClientInvited(viewHolder.getAdapterPosition());
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
        mActivityProjectInfoBinding = DataBindingUtil.setContentView(this, R.layout.activity_project_info);

        setSupportActionBar(mActivityProjectInfoBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            token = bundle.getString(TOKEN);
            userId = bundle.getString(USER_ID);
            project = (ProjectResponse) bundle.getSerializable(PROJECT);
            setTitle(project != null ? project.getTitle() : null);
        }

        mActivityProjectInfoBinding.content.imageProject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFullImageBinding dialogFullImageBinding = DataBindingUtil.inflate(LayoutInflater.from(ProjectInfoActivity.this), R.layout.dialog_full_image, null, false);
                File file = new File(getCacheDir(), '/' + project.getImage());
                if (file.exists()) {
                    Glide.with(ProjectInfoActivity.this)
                            .load(file)
                            .into(dialogFullImageBinding.imageFull);

                    final Dialog dialog = new Dialog(ProjectInfoActivity.this, R.style.AppTheme_Light_DialogTransparent);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(dialogFullImageBinding.getRoot());
                    Window window = dialog.getWindow();
                    if (window != null) {
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                    dialog.show();
                    dialogFullImageBinding.imageFull.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.cancel();
                        }
                    });
                }
            }
        });

        mActivityProjectInfoBinding.content.linkInviteManagers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProjectInfoActivity.this, InviteActivity.class);
                intent.putExtra(TOKEN, token);
                intent.putExtra(USER_ID, userId);
                intent.putExtra(PROJECT_ID, project.getId());
                intent.putExtra(IS_CLIENTS, false);
                startActivityForResult(intent, REQUEST_INVITE_MANAGERS);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });
        mActivityProjectInfoBinding.content.linkInviteClients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProjectInfoActivity.this, InviteActivity.class);
                intent.putExtra(TOKEN, token);
                intent.putExtra(USER_ID, userId);
                intent.putExtra(PROJECT_ID, project.getId());
                intent.putExtra(IS_CLIENTS, true);
                startActivityForResult(intent, REQUEST_INVITE_CLIENTS);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        mActivityProjectInfoBinding.content.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.green_blue));
        mActivityProjectInfoBinding.content.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showRefreshing();
                getProjectById(project.getId());
            }
        });

        mActivityProjectInfoBinding.content.layoutOwner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProjectInfoActivity.this, UserInfoActivity.class);
                intent.putExtra(USER, projectInfo.getOwner());
                startActivity(intent);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        mActivityProjectInfoBinding.content.recyclerManagers.setLayoutManager(new LinearLayoutManager(this));
        mActivityProjectInfoBinding.content.recyclerManagers.setHasFixedSize(false);
        mActivityProjectInfoBinding.content.recyclerManagers.setItemViewCacheSize(20);
        mActivityProjectInfoBinding.content.recyclerManagers.setDrawingCacheEnabled(true);
        mActivityProjectInfoBinding.content.recyclerManagers.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        managersAdapter = new UsersAdapter(managers, onUserClickListener);
        mActivityProjectInfoBinding.content.recyclerManagers.setAdapter(managersAdapter);

        mActivityProjectInfoBinding.content.recyclerClients.setLayoutManager(new LinearLayoutManager(this));
        mActivityProjectInfoBinding.content.recyclerClients.setHasFixedSize(false);
        mActivityProjectInfoBinding.content.recyclerClients.setItemViewCacheSize(20);
        mActivityProjectInfoBinding.content.recyclerClients.setDrawingCacheEnabled(true);
        mActivityProjectInfoBinding.content.recyclerClients.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        clientsAdapter = new UsersAdapter(clients, onUserClickListener);
        mActivityProjectInfoBinding.content.recyclerClients.setAdapter(clientsAdapter);

        mActivityProjectInfoBinding.content.recyclerManagersInvited.setLayoutManager(new LinearLayoutManager(this));
        mActivityProjectInfoBinding.content.recyclerManagersInvited.setHasFixedSize(false);
        mActivityProjectInfoBinding.content.recyclerManagersInvited.setItemViewCacheSize(20);
        mActivityProjectInfoBinding.content.recyclerManagersInvited.setDrawingCacheEnabled(true);
        mActivityProjectInfoBinding.content.recyclerManagersInvited.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        managersInvitedAdapter = new InvitesAdapter(managersInvited, onUserClickListener);
        mActivityProjectInfoBinding.content.recyclerManagersInvited.setAdapter(managersInvitedAdapter);

        mActivityProjectInfoBinding.content.recyclerClientsInvited.setLayoutManager(new LinearLayoutManager(this));
        mActivityProjectInfoBinding.content.recyclerClientsInvited.setHasFixedSize(false);
        mActivityProjectInfoBinding.content.recyclerClientsInvited.setItemViewCacheSize(20);
        mActivityProjectInfoBinding.content.recyclerClientsInvited.setDrawingCacheEnabled(true);
        mActivityProjectInfoBinding.content.recyclerClientsInvited.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        clientsInvitedAdapter = new InvitesAdapter(clientsInvited, onUserClickListener);
        mActivityProjectInfoBinding.content.recyclerClientsInvited.setAdapter(clientsInvitedAdapter);

        if (project.isCanEdit()) {
            itemManagerTouchHelper.attachToRecyclerView(mActivityProjectInfoBinding.content.recyclerManagers);
            itemClientTouchHelper.attachToRecyclerView(mActivityProjectInfoBinding.content.recyclerClients);
            itemManagerInvitedTouchHelper.attachToRecyclerView(mActivityProjectInfoBinding.content.recyclerManagersInvited);
            itemClientInvitedTouchHelper.attachToRecyclerView(mActivityProjectInfoBinding.content.recyclerClientsInvited);
        }

        progressDialog = new ProgressDialog(ProjectInfoActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_getting_project_info));
        progressDialog.show();
        getProjectById(project.getId());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_project, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem actionFinishProject = menu.findItem(R.id.action_finish_project);
        MenuItem actionReviveProject = menu.findItem(R.id.action_revive_project);
        MenuItem actionLeaveProject = menu.findItem(R.id.action_leave_project);
        MenuItem actionDeleteProject = menu.findItem(R.id.action_delete_project);
        MenuItem actionEditProject = menu.findItem(R.id.action_edit_project);

        SpannableString ssDelete = new SpannableString(actionDeleteProject.getTitle().toString());
        ssDelete.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.red)), 0, ssDelete.length(), 0);
        actionDeleteProject.setTitle(ssDelete);

        SpannableString ssLeave = new SpannableString(actionLeaveProject.getTitle().toString());
        ssLeave.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.red)), 0, ssLeave.length(), 0);
        actionLeaveProject.setTitle(ssLeave);

        if (project != null) {
            actionFinishProject.setVisible(project.isCanEdit() && !project.isFinished());
            actionReviveProject.setVisible(project.isCanEdit() && project.isFinished());
            actionLeaveProject.setVisible(!project.isCanEdit());
            actionDeleteProject.setVisible(project.isCanEdit());
            actionEditProject.setVisible(project.isCanEdit() && !project.isFinished());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_finish_project:
                finishProject(project.getId());
                return true;
            case R.id.action_revive_project:
                reviveProject(project.getId());
                return true;
            case R.id.action_leave_project:
                AlertDialog.Builder builderLeave = new AlertDialog.Builder(this);
                builderLeave.setTitle(getString(R.string.dialog_title_leave_project, project.getTitle()));
                builderLeave.setMessage(R.string.dialog_message_leave_project);
                builderLeave.setPositiveButton(R.string.dialog_btn_leave, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        leaveProject(project.getId());
                    }
                });
                builderLeave.setNegativeButton(R.string.dialog_btn_cancel, null);
                final AlertDialog alertDialogLeave = builderLeave.create();
                alertDialogLeave.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        alertDialogLeave.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(ProjectInfoActivity.this ,R.color.red));
                    }
                });
                alertDialogLeave.show();
                return true;
            case R.id.action_delete_project:
                AlertDialog.Builder builderDelete = new AlertDialog.Builder(this);
                builderDelete.setTitle(getString(R.string.dialog_title_delete_project, project.getTitle()));
                builderDelete.setMessage(R.string.dialog_message_delete_project);
                builderDelete.setPositiveButton(R.string.dialog_btn_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteProject(project.getId());
                    }
                });
                builderDelete.setNegativeButton(R.string.dialog_btn_cancel, null);
                final AlertDialog alertDialogDelete = builderDelete.create();
                alertDialogDelete.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface arg0) {
                        alertDialogDelete.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(ProjectInfoActivity.this ,R.color.red));
                    }
                });
                alertDialogDelete.show();
                return true;
            case R.id.action_edit_project:
                Intent intent = new Intent(ProjectInfoActivity.this, EditProjectActivity.class);
                intent.putExtra(TOKEN, token);
                intent.putExtra(USER_ID, userId);
                intent.putExtra(PROJECT_INFO, projectInfo);
                startActivityForResult(intent, REQUEST_EDIT_PROJECT);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                return true;
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_INVITE_CLIENTS:
                if (resultCode == RESULT_OK) {
                    showRefreshing();
                    getProjectById(project.getId());
                    showSnackbar(
                            mActivityProjectInfoBinding.coordinator,
                            null,
                            ContextCompat.getColor(ProjectInfoActivity.this, R.color.dark_green_blue),
                            Color.WHITE,
                            getString(R.string.snackbar_message_clients_added),
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                }
                break;
            case REQUEST_INVITE_MANAGERS:
                if (resultCode == RESULT_OK) {
                    showRefreshing();
                    getProjectById(project.getId());
                    showSnackbar(
                            mActivityProjectInfoBinding.coordinator,
                            null,
                            ContextCompat.getColor(ProjectInfoActivity.this, R.color.dark_green_blue),
                            Color.WHITE,
                            getString(R.string.snackbar_message_managers_added),
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                }
                break;
            case REQUEST_EDIT_PROJECT:
                if (resultCode == RESULT_OK) {
                    showRefreshing();
                    getProjectById(project.getId());
                    showSnackbar(
                            mActivityProjectInfoBinding.coordinator,
                            null,
                            ContextCompat.getColor(ProjectInfoActivity.this, R.color.dark_green_blue),
                            Color.WHITE,
                            getString(R.string.snackbar_message_project_changed),
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void getProjectById(final String projectId) {
        Log.d(TAG, "getProjectById");

        ControlioService service = ServiceFactory.getControlioService(this);
        service.getProjectById(token, userId, projectId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ProjectDetailsResponse>() {
                    @Override
                    public final void onCompleted() {
                        hideRefreshing();
                        progressDialog.dismiss();
                    }

                    @Override
                    public final void onError(Throwable e) {
                        hideRefreshing();
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Get project by id failed: " + e);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                Log.e(TAG, "Get project by id failed: " + message);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Get project by id failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Get project by id failed: ", e);
                            showSnackbar(
                                    mActivityProjectInfoBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Get project by id failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(ProjectDetailsResponse projectResponse) {
                        managers.clear();
                        managersInvited.clear();
                        clients.clear();
                        clientsInvited.clear();
                        projectInfo = projectResponse;
                        mActivityProjectInfoBinding.content.setProjectInfo(projectInfo);

                        managers.addAll(projectInfo.getManagers());
                        managersAdapter.notifyDataSetChanged();

                        clients.addAll(projectInfo.getClients());
                        clientsAdapter.notifyDataSetChanged();


                        for (InviteResponse invite : projectInfo.getInvites()) {

                            switch (invite.getType()) {
                                case TYPE_MANAGE:
                                    managersInvited.add(invite);
                                    break;
                                case TYPE_CLIENT:
                                    clientsInvited.add(invite);
                                    break;
                                case TYPE_OWNER:
                                    ownerInvited = invite.getInvitee();
                                    break;
                            }
                        }

                        mActivityProjectInfoBinding.content.setManagersInvitedIsExists(!managersInvited.isEmpty());
                        mActivityProjectInfoBinding.content.setClientsInvitedIsExists(!clientsInvited.isEmpty());
                        mActivityProjectInfoBinding.content.setOwnerInvited(ownerInvited);

                        managersInvitedAdapter.notifyDataSetChanged();
                        clientsInvitedAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void finishProject(final String projectId) {
        Log.d(TAG, "finishProject");

        progressDialog = new ProgressDialog(ProjectInfoActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_finishing_project));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.finishProject(token, userId, new ProjectIdRequest(projectId))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ProjectResponseTemp>() {
                    @Override
                    public final void onCompleted() {
                        progressDialog.dismiss();
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Finish project failed: " + e);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                Log.e(TAG, "Finish project failed: " + message);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Finish project failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Finish project failed: ", e);
                            showSnackbar(
                                    mActivityProjectInfoBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Finish project failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(ProjectResponseTemp response) {
                        progressDialog.dismiss();
                        setResult(RESULT_OK, null);
                        finish();
                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    }
                });
    }

    private void reviveProject(final String projectId) {
        Log.d(TAG, "reviveProject");

        progressDialog = new ProgressDialog(ProjectInfoActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_reviving_project));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.reviveProject(token, userId, new ProjectIdRequest(projectId))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ProjectResponseTemp>() {
                    @Override
                    public final void onCompleted() {
                        progressDialog.dismiss();
                    }

                    @Override
                    public final void onError(Throwable e) {
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Revive project failed: " + e);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                Log.e(TAG, "Revive project failed: " + message);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Revive project failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Revive project failed: ", e);
                            showSnackbar(
                                    mActivityProjectInfoBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Revive project failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(ProjectResponseTemp response) {
                        progressDialog.dismiss();
                        setResult(RESULT_OK, null);
                        finish();
                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    }
                });
    }

    private void leaveProject(final String projectId) {
        Log.d(TAG, "leaveProject");

        progressDialog = new ProgressDialog(ProjectInfoActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_leaving_project));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.leaveProject(token, userId, new ProjectIdRequest(projectId))
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
                                Log.e(TAG, "Leave project failed: " + e);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                Log.e(TAG, "Leave project failed: " + message);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Leave project failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Leave project failed: ", e);
                            showSnackbar(
                                    mActivityProjectInfoBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Leave project failed: ", e);
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

    private void deleteProject(final String projectId) {
        Log.d(TAG, "deleteProject");

        progressDialog = new ProgressDialog(ProjectInfoActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_deleting_project));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.deleteProject(token, userId, new ProjectIdRequest(projectId))
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
                                Log.e(TAG, "Delete project failed: " + e);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                Log.e(TAG, "Delete project failed: " + message);
                                showSnackbar(
                                        mActivityProjectInfoBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Delete project failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Delete project failed: ", e);
                            showSnackbar(
                                    mActivityProjectInfoBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Delete project failed: ", e);
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

    private void deleteManager(final int position) {
        Log.d(TAG, "deleteManager");

        final UserResponse manager = managers.get(position);
        final String userId = manager.getId();

        managers.remove(position);
        managersAdapter.notifyItemRemoved(position);

        Snackbar.Callback callback = new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                switch (event) {
                    case Snackbar.Callback.DISMISS_EVENT_ACTION:
                        break;
                    default:
                        ControlioService service = ServiceFactory.getControlioService(ProjectInfoActivity.this);
                        service.deleteManager(token, userId, new DeleteManagerRequest(project.getId(), userId))
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<OkResponse>() {
                                    @Override
                                    public final void onCompleted() {
                                    }

                                    @Override
                                    public final void onError(Throwable e) {
                                        managers.add(position, manager);
                                        managersAdapter.notifyItemInserted(position);
                                        if (e instanceof HttpException) {
                                            if ((((HttpException) e).code() == 504)) {
                                                Log.e(TAG, "Delete manager failed: " + e);
                                                showSnackbar(
                                                        mActivityProjectInfoBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                                Log.e(TAG, "Delete manager failed: " + message);
                                                showSnackbar(
                                                        mActivityProjectInfoBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                                        Color.WHITE,
                                                        message,
                                                        Color.WHITE,
                                                        getString(R.string.snackbar_action_hide),
                                                        null
                                                );
                                            } catch (JSONException | IOException ex) {
                                                Log.e(TAG, "Delete manager failed: ", ex);
                                            }
                                        } else if (e instanceof java.io.IOException) {
                                            Log.e(TAG, "Delete manager failed: ", e);
                                            showSnackbar(
                                                    mActivityProjectInfoBinding.coordinator,
                                                    null,
                                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                                    Color.WHITE,
                                                    getString(R.string.error_message_check_internet),
                                                    Color.WHITE,
                                                    getString(R.string.snackbar_action_hide),
                                                    null
                                            );
                                        } else {
                                            Log.e(TAG, "Delete manager failed: ", e);
                                        }
                                    }

                                    @Override
                                    public void onNext(OkResponse response) {
                                        Log.d(TAG, response.toString());
                                        getProjectById(project.getId());
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
                managers.add(position, manager);
                managersAdapter.notifyItemInserted(position);
            }
        };

        showSnackbar(
                mActivityProjectInfoBinding.coordinator,
                callback,
                ContextCompat.getColor(this, R.color.dark_green_blue),
                Color.WHITE,
                getString(R.string.snackbar_message_manager_deleted, !TextUtils.isEmpty(manager.getName()) ? manager.getName() : manager.getEmail()),
                Color.WHITE,
                getString(R.string.snackbar_action_undo),
                onClickListener
        );
    }

    private void deleteClient(final int position) {
        Log.d(TAG, "deleteClient");

        final UserResponse client = clients.get(position);
        final String userId = client.getId();

        clients.remove(position);
        clientsAdapter.notifyItemRemoved(position);

        Snackbar.Callback callback = new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                switch (event) {
                    case Snackbar.Callback.DISMISS_EVENT_ACTION:
                        break;
                    default:
                        ControlioService service = ServiceFactory.getControlioService(ProjectInfoActivity.this);
                        service.deleteClient(token, userId, new DeleteClientRequest(project.getId(), userId))
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<OkResponse>() {
                                    @Override
                                    public final void onCompleted() {
                                    }

                                    @Override
                                    public final void onError(Throwable e) {
                                        clients.add(position, client);
                                        clientsAdapter.notifyItemInserted(position);
                                        if (e instanceof HttpException) {
                                            if ((((HttpException) e).code() == 504)) {
                                                Log.e(TAG, "Delete client failed: " + e);
                                                showSnackbar(
                                                        mActivityProjectInfoBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                                Log.e(TAG, "Delete client failed: " + message);
                                                showSnackbar(
                                                        mActivityProjectInfoBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                                        Color.WHITE,
                                                        message,
                                                        Color.WHITE,
                                                        getString(R.string.snackbar_action_hide),
                                                        null
                                                );
                                            } catch (JSONException | IOException ex) {
                                                Log.e(TAG, "Delete client failed: ", ex);
                                            }
                                        } else if (e instanceof java.io.IOException) {
                                            Log.e(TAG, "Delete client failed: ", e);
                                            showSnackbar(
                                                    mActivityProjectInfoBinding.coordinator,
                                                    null,
                                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                                    Color.WHITE,
                                                    getString(R.string.error_message_check_internet),
                                                    Color.WHITE,
                                                    getString(R.string.snackbar_action_hide),
                                                    null
                                            );
                                        } else {
                                            Log.e(TAG, "Delete client failed: ", e);
                                        }
                                    }

                                    @Override
                                    public void onNext(OkResponse response) {
                                        Log.d(TAG, response.toString());
                                        getProjectById(project.getId());
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
                clients.add(position, client);
                clientsAdapter.notifyItemInserted(position);
            }
        };

        showSnackbar(
                mActivityProjectInfoBinding.coordinator,
                callback,
                ContextCompat.getColor(this, R.color.dark_green_blue),
                Color.WHITE,
                getString(R.string.snackbar_message_client_deleted, !TextUtils.isEmpty(client.getName()) ? client.getName() : client.getEmail()),
                Color.WHITE,
                getString(R.string.snackbar_action_undo),
                onClickListener
        );
    }

    private void deleteManagerInvited(final int position) {
        Log.d(TAG, "deleteManagerInvited");

        final InviteResponse managerInvited = managersInvited.get(position);
        final String inviteId = managerInvited.getId();

        managersInvited.remove(position);
        managersInvitedAdapter.notifyItemRemoved(position);

        Snackbar.Callback callback = new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                switch (event) {
                    case Snackbar.Callback.DISMISS_EVENT_ACTION:
                        break;
                    default:
                        ControlioService service = ServiceFactory.getControlioService(ProjectInfoActivity.this);
                        service.deleteInvite(token, userId, new InviteIdRequest(inviteId))
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<OkResponse>() {
                                    @Override
                                    public final void onCompleted() {
                                    }

                                    @Override
                                    public final void onError(Throwable e) {
                                        managersInvited.add(position, managerInvited);
                                        managersInvitedAdapter.notifyItemInserted(position);
                                        if (e instanceof HttpException) {
                                            if ((((HttpException) e).code() == 504)) {
                                                Log.e(TAG, "Delete invite failed: " + e);
                                                showSnackbar(
                                                        mActivityProjectInfoBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                                Log.e(TAG, "Delete invite failed: " + message);
                                                showSnackbar(
                                                        mActivityProjectInfoBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                                        Color.WHITE,
                                                        message,
                                                        Color.WHITE,
                                                        getString(R.string.snackbar_action_hide),
                                                        null
                                                );
                                            } catch (JSONException | IOException ex) {
                                                Log.e(TAG, "Delete invite failed: ", ex);
                                            }
                                        } else if (e instanceof java.io.IOException) {
                                            Log.e(TAG, "Delete invite failed: ", e);
                                            showSnackbar(
                                                    mActivityProjectInfoBinding.coordinator,
                                                    null,
                                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                                    Color.WHITE,
                                                    getString(R.string.error_message_check_internet),
                                                    Color.WHITE,
                                                    getString(R.string.snackbar_action_hide),
                                                    null
                                            );
                                        } else {
                                            Log.e(TAG, "Delete invite failed: ", e);
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
                managersInvited.add(position, managerInvited);
                managersInvitedAdapter.notifyItemInserted(position);
            }
        };

        showSnackbar(
                mActivityProjectInfoBinding.coordinator,
                callback,
                ContextCompat.getColor(this, R.color.dark_green_blue),
                Color.WHITE,
                getString(R.string.snackbar_message_invite_revoked, !TextUtils.isEmpty(managerInvited.getInvitee().getName()) ? managerInvited.getInvitee().getName() : managerInvited.getInvitee().getEmail()),
                Color.WHITE,
                getString(R.string.snackbar_action_undo),
                onClickListener
        );
    }

    private void deleteClientInvited(final int position) {
        Log.d(TAG, "deleteClientInvited");

        final InviteResponse clientInvited = clientsInvited.get(position);
        final String inviteId = clientInvited.getId();

        clientsInvited.remove(position);
        clientsInvitedAdapter.notifyItemRemoved(position);

        Snackbar.Callback callback = new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                switch (event) {
                    case Snackbar.Callback.DISMISS_EVENT_ACTION:
                        break;
                    default:
                        ControlioService service = ServiceFactory.getControlioService(ProjectInfoActivity.this);
                        service.deleteInvite(token, userId, new InviteIdRequest(inviteId))
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<OkResponse>() {
                                    @Override
                                    public final void onCompleted() {
                                    }

                                    @Override
                                    public final void onError(Throwable e) {
                                        clientsInvited.add(position, clientInvited);
                                        clientsInvitedAdapter.notifyItemInserted(position);
                                        if (e instanceof HttpException) {
                                            if ((((HttpException) e).code() == 504)) {
                                                Log.e(TAG, "Delete invite failed: " + e);
                                                showSnackbar(
                                                        mActivityProjectInfoBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
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
                                                Log.e(TAG, "Delete invite failed: " + message);
                                                showSnackbar(
                                                        mActivityProjectInfoBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                                        Color.WHITE,
                                                        message,
                                                        Color.WHITE,
                                                        getString(R.string.snackbar_action_hide),
                                                        null
                                                );
                                            } catch (JSONException | IOException ex) {
                                                Log.e(TAG, "Delete invite failed: ", ex);
                                            }
                                        } else if (e instanceof java.io.IOException) {
                                            Log.e(TAG, "Delete invite failed: ", e);
                                            showSnackbar(
                                                    mActivityProjectInfoBinding.coordinator,
                                                    null,
                                                    ContextCompat.getColor(ProjectInfoActivity.this, R.color.red),
                                                    Color.WHITE,
                                                    getString(R.string.error_message_check_internet),
                                                    Color.WHITE,
                                                    getString(R.string.snackbar_action_hide),
                                                    null
                                            );
                                        } else {
                                            Log.e(TAG, "Delete invite failed: ", e);
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
                clientsInvited.add(position, clientInvited);
                clientsInvitedAdapter.notifyItemInserted(position);
            }
        };

        showSnackbar(
                mActivityProjectInfoBinding.coordinator,
                callback,
                ContextCompat.getColor(this, R.color.dark_green_blue),
                Color.WHITE,
                getString(R.string.snackbar_message_invite_revoked, !TextUtils.isEmpty(clientInvited.getInvitee().getName()) ? clientInvited.getInvitee().getName() : clientInvited.getInvitee().getEmail()),
                Color.WHITE,
                getString(R.string.snackbar_action_undo),
                onClickListener
        );
    }

    private void showRefreshing() {
        mActivityProjectInfoBinding.content.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityProjectInfoBinding.content.swipeRefresh.setRefreshing(true);
            }
        });
    }

    private void hideRefreshing() {
        mActivityProjectInfoBinding.content.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityProjectInfoBinding.content.swipeRefresh.setRefreshing(false);
            }
        });
    }
}