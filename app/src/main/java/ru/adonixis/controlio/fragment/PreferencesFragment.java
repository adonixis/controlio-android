package ru.adonixis.controlio.fragment;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.BuildConfig;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.activity.EditProfileActivity;
import ru.adonixis.controlio.activity.MagicLinkActivity;
import ru.adonixis.controlio.activity.PaymentMethodsActivity;
import ru.adonixis.controlio.activity.PlansActivity;
import ru.adonixis.controlio.model.LogoutRequest;
import ru.adonixis.controlio.model.UserResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_SELECT_PLAN = 0;
    private static final String IS_ANDROID_PAYMENTS_AVAILABLE = "isAndroidPaymentsAvailable";
    private static final String IS_LOGIN = "isLogin";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String STRIPE_ID = "stripeId";
    private static final String PLAN_ID = "planId";
    private static final String PUSH_TOKEN = "pushToken";
    private boolean isLogin;
    private String token;
    private String userId;
    private String stripeId;
    private int planId;
    private SharedPreferences settings;
    private PreferenceCategory preferenceCategoryAccount;
    private Preference plansPref;
    private Preference paymentMethodsPref;
    private PreferenceCategory preferenceCategoryDebug;
    private EditTextPreference apiUrlPref;
    private String pushToken;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        isLogin = settings.getBoolean(IS_LOGIN, false);
        token = settings.getString(TOKEN, "");
        userId = settings.getString(USER_ID, "");
        stripeId = settings.getString(STRIPE_ID, "");
        planId = settings.getInt(PLAN_ID, 0);
        pushToken = settings.getString(PUSH_TOKEN, null);

        preferenceCategoryAccount = (PreferenceCategory) findPreference("preference_category_account");

        Preference editProfilePref = findPreference("preference_edit_profile");
        editProfilePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getContext(), EditProfileActivity.class);
                intent.putExtra(TOKEN, token);
                intent.putExtra(USER_ID, userId);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                return true;
            }
        });

        preferenceCategoryDebug = (PreferenceCategory) findPreference("preference_category_debug");
        apiUrlPref = (EditTextPreference) findPreference("preference_api_url");
        if (!BuildConfig.DEBUG) {
            preferenceCategoryDebug.removePreference(apiUrlPref);
            getPreferenceScreen().removePreference(preferenceCategoryDebug);
        } else {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            String apiUrl = settings.getString("preference_api_url", BuildConfig.BASE_URL);

            apiUrlPref.setText(apiUrl);
            apiUrlPref.setSummary(apiUrl);
            apiUrlPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (!newValue.toString().isEmpty()) {
                        preference.setSummary((String) newValue);
                        logOut();
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }

        plansPref = findPreference("preference_plans");
        preferenceCategoryAccount.removePreference(plansPref);

        paymentMethodsPref = findPreference("preference_payment_methods");
        preferenceCategoryAccount.removePreference(paymentMethodsPref);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean isAndroidPaymentsAvailable = settings.getBoolean(IS_ANDROID_PAYMENTS_AVAILABLE, false);

        if (isAndroidPaymentsAvailable) {
            plansPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getContext(), PlansActivity.class);
                    intent.putExtra(TOKEN, token);
                    intent.putExtra(USER_ID, userId);
                    intent.putExtra(STRIPE_ID, stripeId);
                    intent.putExtra(PLAN_ID, planId);
                    startActivityForResult(intent, REQUEST_SELECT_PLAN);
                    getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    return true;
                }
            });
            switch (planId) {
                case 0:
                    plansPref.setSummary(R.string.summary_prefs_plan_free);
                    break;
                case 1:
                    plansPref.setSummary(R.string.summary_prefs_plan_five);
                    break;
                case 2:
                    plansPref.setSummary(R.string.summary_prefs_plan_twenty);
                    break;
                case 3:
                    plansPref.setSummary(R.string.summary_prefs_plan_fifty);
                    break;
            }
            preferenceCategoryAccount.addPreference(plansPref);
            paymentMethodsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getContext(), PaymentMethodsActivity.class);
                    intent.putExtra(TOKEN, token);
                    intent.putExtra(USER_ID, userId);
                    intent.putExtra(STRIPE_ID, stripeId);
                    startActivity(intent);
                    getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    return true;
                }
            });
            preferenceCategoryAccount.addPreference(paymentMethodsPref);
        } else {
            preferenceCategoryAccount.removePreference(plansPref);
            preferenceCategoryAccount.removePreference(paymentMethodsPref);
        }

        Preference logOutPref = findPreference("preference_log_out");
        logOutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(R.string.dialog_title_log_out);
                builder.setPositiveButton(R.string.dialog_btn_log_out, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logOut();
                    }
                });
                builder.setNegativeButton(R.string.dialog_btn_cancel, null);
                builder.show();
                return true;
            }
        });
    }

    private void logOut() {
        if (isLogin) {
            ControlioService service = ServiceFactory.getControlioService(getContext());
            service.logout(token, userId, new LogoutRequest(pushToken))
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<UserResponse>() {
                        @Override
                        public final void onCompleted() {
                        }

                        @Override
                        public final void onError(Throwable e) {
                            if (e instanceof HttpException) {
                                if ((((HttpException) e).code() == 504)) {
                                    Log.e(TAG, "Logout failed: " + e);
                                    return;
                                }
                                ResponseBody body = ((HttpException) e).response().errorBody();
                                try {
                                    JSONObject jObjError = new JSONObject(body.string());
                                    String message = "";
                                    if (jObjError.has("message")) {
                                        message = (String) jObjError.get("message");
                                    } else if (jObjError.has("errors")) {
                                        JSONArray errors = (JSONArray) jObjError.get("errors");
                                        JSONObject error = (JSONObject) errors.get(0);
                                        JSONArray messages = (JSONArray) error.get("messages");
                                        message = (String) messages.get(0);
                                    }
                                    Log.e(TAG, "Logout failed: " + message);
                                } catch (JSONException | IOException ex) {
                                    Log.e(TAG, "Logout failed: ", ex);
                                }
                            } else {
                                Log.e(TAG, "Logout failed: ", e);
                            }
                        }

                        @Override
                        public void onNext(UserResponse userResponse) {
                            Log.d(TAG, "onNext: " + userResponse);
                        }
                    });
        }

        if (Build.VERSION.SDK_INT >= 25) {
            removeShorcuts();
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(IS_LOGIN, false);
        editor.remove(TOKEN);
        editor.remove(USER_ID);
        editor.remove(STRIPE_ID);
        editor.remove(PLAN_ID);
        editor.apply();
        Intent intent = new Intent(getContext(), MagicLinkActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    @TargetApi(25)
    private void removeShorcuts() {
        ShortcutManager shortcutManager = getContext().getSystemService(ShortcutManager.class);
        shortcutManager.disableShortcuts(Arrays.asList(getString(R.string.shortcut_new_project_id), getString(R.string.shortcut_settings_id)));
        shortcutManager.removeAllDynamicShortcuts();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELECT_PLAN) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                planId = bundle.getInt(PLAN_ID, 0);
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(PLAN_ID, planId);
                editor.apply();
                switch (planId) {
                    case 0:
                        plansPref.setSummary(R.string.summary_prefs_plan_free);
                        return;
                    case 1:
                        plansPref.setSummary(R.string.summary_prefs_plan_five);
                        return;
                    case 2:
                        plansPref.setSummary(R.string.summary_prefs_plan_twenty);
                        return;
                    case 3:
                        plansPref.setSummary(R.string.summary_prefs_plan_fifty);
                }
            }
        }
    }
}