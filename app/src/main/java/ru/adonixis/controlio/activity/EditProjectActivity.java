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
import android.support.v7.widget.PopupMenu;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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
import ru.adonixis.controlio.databinding.ActivityEditProjectBinding;
import ru.adonixis.controlio.model.EditProjectRequest;
import ru.adonixis.controlio.model.ProjectDetailsResponse;
import ru.adonixis.controlio.model.ProjectResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import ru.adonixis.controlio.util.S3Utils;
import ru.adonixis.controlio.util.Utils;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EditProjectActivity extends BaseSubmitFormActivity {

    private static final String TAG = "EditProjectActivity";
    private static final int REQUEST_PICK_IMAGE = 0;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final String CAPTURE_IMAGE_FILE_PROVIDER = "ru.adonixis.controlio.fileprovider";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String PROJECT = "project";
    private static final String PROJECT_INFO = "projectInfo";
    private ActivityEditProjectBinding mActivityEditProjectBinding;
    private String token;
    private String userId;
    private String fileName;
    private File fileUpload;
    private String imagePath;
    private ProjectDetailsResponse projectInfo;
    private ProgressDialog progressDialog;
    private File filePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityEditProjectBinding = DataBindingUtil.setContentView(this, R.layout.activity_edit_project);

        setSupportActionBar(mActivityEditProjectBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            token = bundle.getString(TOKEN);
            userId = bundle.getString(USER_ID);
            projectInfo = (ProjectDetailsResponse) bundle.getSerializable(PROJECT_INFO);
            imagePath = projectInfo != null ? projectInfo.getImage() : null;
        }
        setTitle("");
        mActivityEditProjectBinding.content.setProjectInfo(projectInfo);

        mActivityEditProjectBinding.content.imagePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });

        mActivityEditProjectBinding.content.btnEditPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });

        mActivityEditProjectBinding.content.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                if (!validate()) {
                    return;
                }
                mActivityEditProjectBinding.content.btnSave.setEnabled(false);
                String title = mActivityEditProjectBinding.content.inputTitle.getText().toString();
                String description = mActivityEditProjectBinding.content.inputDescription.getText().toString().isEmpty() ? null : mActivityEditProjectBinding.content.inputDescription.getText().toString();
                boolean progressEnabled = mActivityEditProjectBinding.content.switchProgressEnabled.isChecked();
                EditProjectRequest editProjectRequest;
                if (!TextUtils.isEmpty(fileName) && fileUpload != null) {
                    imagePath = userId + '/' + fileName;
                }
                editProjectRequest = new EditProjectRequest(projectInfo.getId(), title, description, imagePath, progressEnabled);
                editProjectImage(editProjectRequest);
            }
        });

        mActivityEditProjectBinding.content.layoutInputTitle.setError(getString(R.string.error_message_edit_project_title));
        mActivityEditProjectBinding.content.layoutInputDescription.setError(getString(R.string.error_message_edit_project_description));
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

    private void showPopup(final View view) {
        PopupMenu popup = new PopupMenu(EditProjectActivity.this, view, GravityCompat.END);
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
                            Uri imageUri = FileProvider.getUriForFile(EditProjectActivity.this, CAPTURE_IMAGE_FILE_PROVIDER, filePhoto);
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
                        mActivityEditProjectBinding.content.imagePhoto.setImageResource(R.drawable.camera_mask);
                        mActivityEditProjectBinding.content.btnEditPhoto.setText(R.string.btn_add);
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
                        mActivityEditProjectBinding.content.imagePhoto.setImageDrawable(roundedBitmapDrawable);
                        fileName = S3Utils.getFileNameFromUri(this, uri);
                        fileUpload = S3Utils.createFileFromUri(this, uri, userId, fileName);
                        imagePath = userId + '/' + fileName;
                        mActivityEditProjectBinding.content.btnEditPhoto.setText(R.string.btn_edit);
                    } catch (IOException e) {
                        Log.e(TAG, "onActivityResult: ", e);
                        showSnackbar(
                                mActivityEditProjectBinding.coordinator,
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
                        mActivityEditProjectBinding.content.imagePhoto.setImageDrawable(roundedBitmapDrawable);
                        fileName = S3Utils.getFileNameFromUri(this, uri);
                        fileUpload = S3Utils.createFileFromUri(this, uri, userId, fileName);
                        imagePath = userId + '/' + fileName;
                        mActivityEditProjectBinding.content.btnEditPhoto.setText(R.string.btn_edit);
                    } catch (IOException e) {
                        Log.e(TAG, "onActivityResult: ", e);
                        showSnackbar(
                                mActivityEditProjectBinding.coordinator,
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

    private void editProjectImage(final EditProjectRequest editProjectRequest) {
        Log.d(TAG, "editProjectImage");

        progressDialog = new ProgressDialog(EditProjectActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_editing_project));
        progressDialog.show();

        if (TextUtils.isEmpty(fileName) || fileUpload == null) {
            editProject(editProjectRequest);
        } else {
            TransferListener mTransferListener = new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    Log.d(TAG, "onStateChanged: " + state);
                    if (TransferState.COMPLETED.equals(state)) {
                        editProject(editProjectRequest);
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {}

                @Override
                public void onError(int id, Exception ex) {
                    mActivityEditProjectBinding.content.btnSave.setEnabled(true);
                    progressDialog.dismiss();
                    Log.e(TAG, "onError: ", ex);
                    String message = "Uploading image failed: " +  ex.getMessage();
                    showSnackbar(
                            mActivityEditProjectBinding.coordinator,
                            null,
                            ContextCompat.getColor(EditProjectActivity.this, R.color.red),
                            Color.WHITE,
                            message,
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                }
            };
            S3Utils.upload(EditProjectActivity.this, fileUpload, userId + '/' + fileName, mTransferListener);
        }
    }

    private void editProject(EditProjectRequest editProjectRequest) {
        Log.d(TAG, "editProject");

        ControlioService service = ServiceFactory.getControlioService(EditProjectActivity.this);
        service.editProject(token, userId, editProjectRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ProjectResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        mActivityEditProjectBinding.content.btnSave.setEnabled(true);
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Editing the new project failed: " + e);
                                showSnackbar(
                                        mActivityEditProjectBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(EditProjectActivity.this, R.color.red),
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
                                Log.e(TAG, "Editing the project failed: " + message);
                                showSnackbar(
                                        mActivityEditProjectBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(EditProjectActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Editing the project failed: ", ex);
                            }
                        } else if (e instanceof IOException) {
                            Log.e(TAG, "Editing the project failed: ", e);
                            showSnackbar(
                                    mActivityEditProjectBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(EditProjectActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Editing the project failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(ProjectResponse projectResponse) {
                        mActivityEditProjectBinding.content.btnSave.setEnabled(true);
                        progressDialog.dismiss();
                        Intent intent = new Intent();
                        intent.putExtra(PROJECT, projectResponse);
                        setResult(RESULT_OK, intent);
                        finish();
                        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                    }
                });
    }

    @Override
    protected boolean validate() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String title = mActivityEditProjectBinding.content.inputTitle.getText().toString();

        if (title.isEmpty()) {
            mActivityEditProjectBinding.content.layoutInputTitle.startAnimation(shake);
            valid = false;
        }

        return valid;
    }
}
