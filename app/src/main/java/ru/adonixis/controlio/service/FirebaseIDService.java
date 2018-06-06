package ru.adonixis.controlio.service;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.model.ChangePushTokenRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FirebaseIDService extends FirebaseInstanceIdService {
    private static final String TAG = "FirebaseIDService";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String PUSH_TOKEN = "pushToken";

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String oldPushToken = settings.getString(PUSH_TOKEN, null);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PUSH_TOKEN, refreshedToken);
        editor.apply();

        if (oldPushToken != null) {
            sendRegistrationToServer(refreshedToken, oldPushToken);
        }
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param refreshedToken The new token.
     */
    private void sendRegistrationToServer(String refreshedToken, String oldPushToken) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String token = settings.getString(TOKEN, "");
        String userId = settings.getString(USER_ID, "");

        ControlioService service = ServiceFactory.getControlioService(this);
        service.changePushToken(token, userId, new ChangePushTokenRequest(refreshedToken, oldPushToken))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<OkResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Change push token: " + e);
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
                                Log.e(TAG, "Change push token: " + message);
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Change push token: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Change push token: ", e);
                        } else {
                            Log.e(TAG, "Change push token: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse response) {
                        if (response.isSuccess()) {
                        }
                    }
                });
    }
}