package ru.adonixis.controlio.activity;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.ActivityEditProfileBinding;
import ru.adonixis.controlio.model.EditProfileRequest;
import ru.adonixis.controlio.model.UserResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import ru.adonixis.controlio.util.S3Utils;
import ru.adonixis.controlio.util.Utils;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EditProfileActivity extends BaseActivity {

    private static final String TAG = "EditProfileActivity";
    private static final int REQUEST_PICK_IMAGE = 0;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String CAPTURE_IMAGE_FILE_PROVIDER = "ru.adonixis.controlio.fileprovider";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private ActivityEditProfileBinding mActivityEditProfileBinding;
    private String token;
    private String userId;
    private String fileName;
    private File fileUpload;
    private String imagePath;
    private ProgressDialog progressDialog;
    private UserResponse profile;
    private File filePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityEditProfileBinding = DataBindingUtil.setContentView(this, R.layout.activity_edit_profile);

        setSupportActionBar(mActivityEditProfileBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        token = intent.getStringExtra(TOKEN);
        userId = intent.getStringExtra(USER_ID);

        mActivityEditProfileBinding.content.imageProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });

        mActivityEditProfileBinding.content.btnEditPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });

        mActivityEditProfileBinding.content.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.green_blue));
        mActivityEditProfileBinding.content.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showRefreshing();
                getProfile();
            }
        });

        mActivityEditProfileBinding.content.inputLayoutEmail.setError(getString(R.string.error_message_cannot_change_email));
        mActivityEditProfileBinding.content.inputLayoutPhone.setError(getString(R.string.error_message_profile_phone));

        progressDialog = new ProgressDialog(EditProfileActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_get_profile_info));
        progressDialog.show();
        getProfile();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_save, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_save:
                String name = mActivityEditProfileBinding.content.inputName.getText().toString().isEmpty() ? null : mActivityEditProfileBinding.content.inputName.getText().toString();
                String phone = mActivityEditProfileBinding.content.inputPhone.getText().toString().isEmpty() ? null : mActivityEditProfileBinding.content.inputPhone.getText().toString();
                progressDialog = new ProgressDialog(EditProfileActivity.this, R.style.AppTheme_Light_Dialog);
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setMessage(getString(R.string.progress_message_editing_profile));
                progressDialog.show();
                if (!TextUtils.isEmpty(fileName) && fileUpload != null) {
                    imagePath = userId + '/' + fileName;
                    EditProfileRequest editProfileRequest = new EditProfileRequest(name, phone, userId + '/' + fileName);
                    editProfileWithPhoto(editProfileRequest);
                } else {
                    EditProfileRequest editProfileRequest = new EditProfileRequest(name, phone, imagePath);
                    editProfile(editProfileRequest);
                }
                return true;
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPopup(final View view) {
        PopupMenu popup = new PopupMenu(EditProfileActivity.this, view, GravityCompat.END);
        MenuInflater inflate = popup.getMenuInflater();
        Menu popupMenu = popup.getMenu();
        inflate.inflate(R.menu.menu_popup_edit_image, popupMenu);

        MenuItem actionRemovePhoto = popupMenu.findItem(R.id.action_remove_photo);
        actionRemovePhoto.setVisible(!TextUtils.isEmpty(imagePath));
        SpannableString ssRemovePhoto = new SpannableString(actionRemovePhoto.getTitle().toString());
        ssRemovePhoto.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.red)), 0, ssRemovePhoto.length(), 0);
        actionRemovePhoto.setTitle(ssRemovePhoto);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_choose_image:
                        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        pickImageIntent.setType("image/*");
                        startActivityForResult(pickImageIntent, REQUEST_PICK_IMAGE);
                        break;
                    case R.id.action_take_photo:
                        Intent takePictureIntent  = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            filePhoto = new File(new File(getFilesDir(), "photos"), "image.jpg");
                            if (filePhoto.exists()) {
                                filePhoto.delete();
                            } else {
                                filePhoto.getParentFile().mkdirs();
                            }
                            Uri imageUri = FileProvider.getUriForFile(EditProfileActivity.this, CAPTURE_IMAGE_FILE_PROVIDER, filePhoto);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            } else {
                                ClipData clip = ClipData.newUri(getContentResolver(), "A photo", imageUri);
                                takePictureIntent.setClipData(clip);
                                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            }
                            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                        }
                        break;
                    case R.id.action_remove_photo:
                        imagePath = null;
                        fileName = null;
                        fileUpload = null;
                        mActivityEditProfileBinding.content.imageProfilePhoto.setImageResource(R.drawable.camera_mask);
                        mActivityEditProfileBinding.content.btnEditPhoto.setText(R.string.btn_add_photo);
                        break;
                    default:
                        return false;
                }
                return false;
            }
        });
        popup.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    Uri uri = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        Bitmap squareBitmap = ThumbnailUtils.extractThumbnail(bitmap, (int) Utils.convertDpToPixel(60, this), (int) Utils.convertDpToPixel(60, this));
                        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
                        float roundPx = (float) squareBitmap.getWidth() * 0.1f;
                        roundedBitmapDrawable.setCornerRadius(roundPx);
                        mActivityEditProfileBinding.content.imageProfilePhoto.setImageDrawable(roundedBitmapDrawable);
                        fileName = S3Utils.getFileNameFromUri(this, uri);
                        fileUpload = S3Utils.createFileFromUri(this, uri, userId, fileName);
                        imagePath = userId + '/' + fileName;
                        mActivityEditProfileBinding.content.btnEditPhoto.setText(R.string.btn_edit_photo);
                    } catch (IOException e) {
                        Log.e(TAG, "onActivityResult: ", e);
                        showSnackbar(
                                mActivityEditProfileBinding.coordinator,
                                null,
                                ContextCompat.getColor(this, R.color.red),
                                Color.WHITE,
                                e.getMessage(),
                                Color.WHITE,
                                getString(R.string.snackbar_action_hide),
                                null
                        );
                    }
                }
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri uri = FileProvider.getUriForFile(this, CAPTURE_IMAGE_FILE_PROVIDER, filePhoto);
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        Bitmap squareBitmap = ThumbnailUtils.extractThumbnail(bitmap, (int) Utils.convertDpToPixel(60, this), (int) Utils.convertDpToPixel(60, this));
                        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
                        float roundPx = (float) squareBitmap.getWidth() * 0.1f;
                        roundedBitmapDrawable.setCornerRadius(roundPx);
                        mActivityEditProfileBinding.content.imageProfilePhoto.setImageDrawable(roundedBitmapDrawable);
                        fileName = S3Utils.getFileNameFromUri(this, uri);
                        fileUpload = S3Utils.createFileFromUri(this, uri, userId, fileName);
                        imagePath = userId + '/' + fileName;
                        mActivityEditProfileBinding.content.btnEditPhoto.setText(R.string.btn_edit_photo);
                    } catch (IOException e) {
                        Log.e(TAG, "onActivityResult: ", e);
                        showSnackbar(
                                mActivityEditProfileBinding.coordinator,
                                null,
                                ContextCompat.getColor(this, R.color.red),
                                Color.WHITE,
                                e.getMessage(),
                                Color.WHITE,
                                getString(R.string.snackbar_action_hide),
                                null
                        );
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void getProfile() {
        ControlioService service = ServiceFactory.getControlioService(this);
        service.getProfile(token, userId, null)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UserResponse>() {
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
                                Log.e(TAG, "Get profile info failed: " + e);
                                showSnackbar(
                                        mActivityEditProfileBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(EditProfileActivity.this, R.color.red),
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
                                Log.e(TAG, "Get profile info failed: " + message);
                                showSnackbar(
                                        mActivityEditProfileBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(EditProfileActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Get profile info failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Get profile info failed: ", e);
                            showSnackbar(
                                    mActivityEditProfileBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(EditProfileActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Get profile info failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(UserResponse userResponse) {
                        profile = userResponse;
                        imagePath = profile.getPhoto();
                        mActivityEditProfileBinding.content.setProfile(profile);
                    }
                });
    }

    private void editProfile(final EditProfileRequest editProfileRequest) {
        ControlioService service = ServiceFactory.getControlioService(this);
        service.editProfile(token, userId, editProfileRequest)
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
                                Log.e(TAG, "Editing profile failed: " + e);
                                showSnackbar(
                                        mActivityEditProfileBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(EditProfileActivity.this, R.color.red),
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
                                Log.e(TAG, "Editing profile failed: " + message);
                                showSnackbar(
                                        mActivityEditProfileBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(EditProfileActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Editing profile failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Editing profile failed: ", e);
                            showSnackbar(
                                    mActivityEditProfileBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(EditProfileActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Editing profile failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(UserResponse userResponse) {
                        progressDialog.dismiss();
                        showSnackbar(
                                mActivityEditProfileBinding.coordinator,
                                null,
                                ContextCompat.getColor(EditProfileActivity.this, R.color.dark_green_blue),
                                Color.WHITE,
                                getString(R.string.snackbar_message_profile_updated),
                                Color.WHITE,
                                getString(R.string.snackbar_action_hide),
                                null
                        );
                    }
                });
    }

    private void editProfileWithPhoto(final EditProfileRequest editProfileRequest) {
        Log.d(TAG, "editProfileWithPhoto");

        TransferListener mTransferListener = new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "onStateChanged: " + state);
                if (TransferState.COMPLETED.equals(state)) {
                    editProfile(editProfileRequest);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {}

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "onError: ", ex);
                showSnackbar(
                        mActivityEditProfileBinding.coordinator,
                        null,
                        ContextCompat.getColor(EditProfileActivity.this, R.color.red),
                        Color.WHITE,
                        ex.getMessage(),
                        Color.WHITE,
                        getString(R.string.snackbar_action_hide),
                        null
                );
            }
        };
        S3Utils.upload(EditProfileActivity.this, fileUpload, userId + '/' + fileName, mTransferListener);
    }

    private void showRefreshing() {
        mActivityEditProfileBinding.content.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityEditProfileBinding.content.swipeRefresh.setRefreshing(true);
            }
        });
    }

    private void hideRefreshing() {
        mActivityEditProfileBinding.content.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityEditProfileBinding.content.swipeRefresh.setRefreshing(false);
            }
        });
    }
}
