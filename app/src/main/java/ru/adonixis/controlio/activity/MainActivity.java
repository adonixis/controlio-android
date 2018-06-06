package ru.adonixis.controlio.activity;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.StringDef;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.adapter.ProjectsAdapter;
import ru.adonixis.controlio.databinding.ActivityMainBinding;
import ru.adonixis.controlio.listener.EndlessRecyclerViewScrollListener;
import ru.adonixis.controlio.listener.OnInviteClickListener;
import ru.adonixis.controlio.listener.OnProjectClickListener;
import ru.adonixis.controlio.model.AcceptInviteRequest;
import ru.adonixis.controlio.model.FeaturesResponse;
import ru.adonixis.controlio.model.InviteDetailsResponse;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.model.ProjectResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseActivity {

    private static final int REQUEST_NEW_PROJECT = 0;
    private static final int REQUEST_POSTS = 1;
    private static final String TAG = "MainActivity";
    private static final String IS_LOGIN = "isLogin";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String PROJECT = "project";
    private static final String IS_ANDROID_PAYMENTS_AVAILABLE = "isAndroidPaymentsAvailable";
    private static final int DEFAULT_ITEMS_COUNT = 20;
    private ActivityMainBinding mActivityMainBinding;
    private List<InviteDetailsResponse> invites = new ArrayList<>();
    private List<ProjectResponse> projects = new ArrayList<>();
    private ProjectsAdapter projectsAdapter;
    private LinearLayoutManager layoutManager;
    private String token;
    private String userId;
    private ProgressDialog progressDialog;
    @ProjectType private String type;
    private String query;

    @StringDef({TYPE_ALL, TYPE_LIVE, TYPE_FINISHED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProjectType {}
    public static final String TYPE_ALL = "all";
    public static final String TYPE_LIVE = "live";
    public static final String TYPE_FINISHED = "finished";

    private OnProjectClickListener onProjectTouchListener = new OnProjectClickListener() {
        @Override
        public void onProjectClick(View view, int position) {
            Intent intent = new Intent(MainActivity.this, PostsActivity.class);
            intent.putExtra(TOKEN, token);
            intent.putExtra(USER_ID, userId);
            intent.putExtra(PROJECT, projects.get(position));
            startActivityForResult(intent, REQUEST_POSTS);
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
    };

    private OnInviteClickListener onInviteClickListener = new OnInviteClickListener() {
        @Override
        public void onAcceptClick(View view, InviteDetailsResponse invite) {
            progressDialog = new ProgressDialog(MainActivity.this, R.style.AppTheme_Light_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(getString(R.string.progress_message_accepting_invite));
            progressDialog.show();
            acceptInvite(invite);
        }

        @Override
        public void onRejectClick(View view, InviteDetailsResponse invite) {
            progressDialog = new ProgressDialog(MainActivity.this, R.style.AppTheme_Light_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setMessage(getString(R.string.progress_message_rejecting_invite));
            progressDialog.show();
            rejectInvite(invite);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (needLogin()) {
            Intent intent = new Intent(this, MagicLinkActivity.class);
            startActivity(intent);
            super.onCreate(savedInstanceState);
            finish();
        } else {
            setTheme(R.style.AppTheme_NoActionBar);
            super.onCreate(savedInstanceState);

            if (Build.VERSION.SDK_INT >= 25) {
                createShorcuts();
            }

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            token = settings.getString(TOKEN, "");
            userId = settings.getString(USER_ID, "");
            Crashlytics.setUserIdentifier(userId);

            mActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
            setSupportActionBar(mActivityMainBinding.toolbar);
            setTitle(R.string.title_activity_all_projects);

            mActivityMainBinding.appBar.setExpanded(false);
            mActivityMainBinding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    int position = tab.getPosition();
                    switch (position) {
                        case 0:
                            type = TYPE_ALL;
                            break;
                        case 1:
                            type = TYPE_LIVE;
                            break;
                        case 2:
                            type = TYPE_FINISHED;
                            break;
                    }
                    showRefreshing();
                    getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });

            mActivityMainBinding.fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MainActivity.this, NewProjectActivity.class);
                    intent.putExtra(TOKEN, token);
                    intent.putExtra(USER_ID, userId);
                    startActivityForResult(intent, REQUEST_NEW_PROJECT);
                }
            });

            mActivityMainBinding.linkPlaceholderCreateProjects.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivityMainBinding.fab.performClick();
                }
            });

            layoutManager = new LinearLayoutManager(this);
            mActivityMainBinding.recyclerProjects.setLayoutManager(layoutManager);
            mActivityMainBinding.recyclerProjects.setHasFixedSize(true);
            mActivityMainBinding.recyclerProjects.setItemViewCacheSize(20);
            mActivityMainBinding.recyclerProjects.setDrawingCacheEnabled(true);
            mActivityMainBinding.recyclerProjects.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

            projectsAdapter = new ProjectsAdapter(projects, invites, onProjectTouchListener, onInviteClickListener);
            mActivityMainBinding.recyclerProjects.setAdapter(projectsAdapter);

            mActivityMainBinding.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.green_blue));
            mActivityMainBinding.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    showRefreshing();
                    getFeatures();
                    getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showRefreshing();
        getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
    }

    @TargetApi(25)
    private void createShorcuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        Intent main = new Intent(this, MainActivity.class);
        main.setAction(Intent.ACTION_VIEW);

        Intent newProject = new Intent(this, NewProjectActivity.class);
        newProject.setAction(Intent.ACTION_VIEW);
        Intent[] intentsNewProject = new Intent[] {main, newProject};
        ShortcutInfo shortcutNewProject = new ShortcutInfo.Builder(this, getString(R.string.shortcut_new_project_id))
                .setIntents(intentsNewProject)
                .setRank(1)
                .setShortLabel(getString(R.string.shortcut_new_project_short_label))
                .setLongLabel(getString(R.string.shortcut_new_project_long_label))
                .setDisabledMessage(getString(R.string.shortcuts_disabled_message))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_new_project_purple_48dp))
                .build();

        Intent settings = new Intent(this, SettingsActivity.class);
        settings.setAction(Intent.ACTION_VIEW);
        Intent[] intentsSettings = new Intent[] {main, settings};
        ShortcutInfo shortcutSettings = new ShortcutInfo.Builder(this, getString(R.string.shortcut_settings_id))
                .setIntents(intentsSettings)
                .setRank(0)
                .setShortLabel(getString(R.string.shortcut_settings_short_label))
                .setLongLabel(getString(R.string.shortcut_settings_long_label))
                .setDisabledMessage(getString(R.string.shortcuts_disabled_message))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_settings_purple_48dp))
                .build();

        shortcutManager.setDynamicShortcuts(Arrays.asList(shortcutNewProject, shortcutSettings));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_NEW_PROJECT:
                if (resultCode == RESULT_OK) {
                    showSnackbar(
                            mActivityMainBinding.coordinator,
                            null,
                            ContextCompat.getColor(this, R.color.dark_green_blue),
                            Color.WHITE,
                            getString(R.string.snackbar_message_project_created),
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                    mActivityMainBinding.placeholderNoProjects.setVisibility(View.GONE);
                    mActivityMainBinding.swipeRefresh.setVisibility(View.VISIBLE);
                    Bundle bundle = getIntent().getExtras();
                    if (bundle != null) {
                        ProjectResponse project = (ProjectResponse) bundle.getSerializable(PROJECT);
                        projects.add(project);
                        projectsAdapter.notifyItemInserted(0);
                    }
                    layoutManager.scrollToPosition(0);
                    showRefreshing();
                    getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
                }
                return;
            case REQUEST_POSTS:
                if (resultCode == RESULT_OK) {
                    showRefreshing();
                    getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setQueryHint(getResources().getString(R.string.hint_search));

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    query = newText;
                    if (TextUtils.isEmpty(newText)) {
                        showRefreshing();
                        getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
                    } else {
                        showRefreshing();
                        getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
                    }
                    return true;
                }
            });
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_search:

                return true;
            case R.id.action_support:
                Intent supportIntent = new Intent(MainActivity.this, SupportActivity.class);
                startActivity(supportIntent);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                return true;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean needLogin() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        return !settings.getBoolean(IS_LOGIN, false);
    }

    private void getProjects(final int skip, final int limit, @ProjectType final String type, final String query) {
        ControlioService service = ServiceFactory.getControlioService(this);
        service.getProjects(token, userId, skip, limit, type, query)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<ProjectResponse>>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        hideRefreshing();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Get projects failed: " + e);
                                showSnackbar(
                                        mActivityMainBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(MainActivity.this, R.color.red),
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
                                Log.e(TAG, "Get projects failed: " + message);
                                showSnackbar(
                                        mActivityMainBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(MainActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Get projects failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Get projects failed: ", e);
                            showSnackbar(
                                    mActivityMainBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(MainActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Get projects failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(List<ProjectResponse> response) {
                        Log.d(TAG, projects.toString());
                        if (!projects.equals(response)) {

                            if (skip == 0) {
                                mActivityMainBinding.recyclerProjects.clearOnScrollListeners();
                                mActivityMainBinding.recyclerProjects.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager, DEFAULT_ITEMS_COUNT) {
                                    @Override
                                    public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                                        int offset = page * DEFAULT_ITEMS_COUNT;
                                        getProjects(offset, DEFAULT_ITEMS_COUNT, type, query);
                                    }
                                });
                                projects.clear();
                            }
                            projects.addAll(response);
                            projectsAdapter.notifyDataSetChanged();
                        }
                        getInvites();
                    }
                });
    }

    private void getInvites() {
        ControlioService service = ServiceFactory.getControlioService(this);
        service.getInvites(token, userId)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<InviteDetailsResponse>>() {
                    @Override
                    public final void onCompleted() {
                        hideRefreshing();
                    }

                    @Override
                    public final void onError(Throwable e) {
                        hideRefreshing();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Get invites failed: " + e);
                                showSnackbar(
                                        mActivityMainBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(MainActivity.this, R.color.red),
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
                                Log.e(TAG, "Get invites failed: " + message);
                                showSnackbar(
                                        mActivityMainBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(MainActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Get invites failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Get invites failed: ", e);
                            showSnackbar(
                                    mActivityMainBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(MainActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Get invites failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(List<InviteDetailsResponse> response) {
                        if (response.isEmpty() && projects.isEmpty()) {
                            mActivityMainBinding.placeholderNoProjects.setVisibility(View.VISIBLE);
                            mActivityMainBinding.swipeRefresh.setVisibility(View.GONE);
                        } else {
                            mActivityMainBinding.placeholderNoProjects.setVisibility(View.GONE);
                            mActivityMainBinding.swipeRefresh.setVisibility(View.VISIBLE);
                        }
                        if (!invites.equals(response)) {
                            invites.clear();
                            invites.addAll(response);
                            projectsAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void getFeatures() {
        ControlioService service = ServiceFactory.getControlioService(this);
        service.getFeatures()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<FeaturesResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Get features failed: " + e);
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
                                Log.e(TAG, "Get features failed: " + message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Get features failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Get features failed: ", e);
                        } else {
                            Log.e(TAG, "Get features failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(FeaturesResponse featuresResponse) {
                        boolean isAndroidPaymentsAvailable = featuresResponse.isAndroidPaymentsAvailable();
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean(IS_ANDROID_PAYMENTS_AVAILABLE, isAndroidPaymentsAvailable);
                        editor.apply();
                    }
                });
    }

    private void acceptInvite(final InviteDetailsResponse invite) {
        ControlioService service = ServiceFactory.getControlioService(this);
        service.acceptOrRejectInvite(token, userId, new AcceptInviteRequest(invite.getId(), true))
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
                                Log.e(TAG, "Accept invite failed: " + e);
                                showSnackbar(
                                        mActivityMainBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(MainActivity.this, R.color.red),
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
                                Log.e(TAG, "Accept invite failed: " + message);
                                showSnackbar(
                                        mActivityMainBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(MainActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Accept invite failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Accept invite failed: ", e);
                            showSnackbar(
                                    mActivityMainBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(MainActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Accept invite failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        progressDialog.dismiss();
                        if (response.isSuccess()) {
                            showSnackbar(
                                    mActivityMainBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(MainActivity.this, R.color.dark_green_blue),
                                    Color.WHITE,
                                    getString(R.string.snackbar_message_invite_accepted, invite.getProject().getTitle()),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                            getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
                        }
                    }
                });
    }

    private void rejectInvite(final InviteDetailsResponse invite) {
        ControlioService service = ServiceFactory.getControlioService(this);
        service.acceptOrRejectInvite(token, userId, new AcceptInviteRequest(invite.getId(), false))
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
                                Log.e(TAG, "Accept invite failed: " + e);
                                showSnackbar(
                                        mActivityMainBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(MainActivity.this, R.color.red),
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
                                Log.e(TAG, "Accept invite failed: " + message);
                                showSnackbar(
                                        mActivityMainBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(MainActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Accept invite failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Accept invite failed: ", e);
                            showSnackbar(
                                    mActivityMainBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(MainActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Accept invite failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        progressDialog.dismiss();
                        if (response.isSuccess()) {
                            showSnackbar(
                                    mActivityMainBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(MainActivity.this, R.color.dark_green_blue),
                                    Color.WHITE,
                                    getString(R.string.snackbar_message_invite_rejected, invite.getProject().getTitle()),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                            getProjects(0, DEFAULT_ITEMS_COUNT, type, query);
                        }
                    }
                });
    }

    private void showRefreshing() {
        mActivityMainBinding.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityMainBinding.swipeRefresh.setRefreshing(true);
            }
        });
    }

    private void hideRefreshing() {
        mActivityMainBinding.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityMainBinding.swipeRefresh.setRefreshing(false);
            }
        });
    }
}
