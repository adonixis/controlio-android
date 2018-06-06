package ru.adonixis.controlio.activity;

import android.animation.Animator;
import android.annotation.TargetApi;
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
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.HttpException;
import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.ActivityNewProjectBinding;
import ru.adonixis.controlio.model.NewProjectRequest;
import ru.adonixis.controlio.model.ProjectResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import ru.adonixis.controlio.util.S3Utils;
import ru.adonixis.controlio.util.Utils;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class NewProjectActivity extends BaseSubmitFormActivity {

    private boolean isClient = true;
    private static final String TAG = "NewProjectActivity";
    private static final int REQUEST_ADD_CLIENTS = 0;
    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_TAKE_PHOTO = 2;
    private static final String CAPTURE_IMAGE_FILE_PROVIDER = "ru.adonixis.controlio.fileprovider";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String PROJECT = "project";
    private static final String CLIENTS_EMAILS = "clientsEmails";
    private ActivityNewProjectBinding mActivityNewProjectBinding;
    private String token;
    private String userId;
    private String fileName;
    private File fileUpload;
    private ProgressDialog progressDialog;
    private ArrayList<String> clientsEmails = new ArrayList<>();
    private File filePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.do_not_move, R.anim.do_not_move);
        mActivityNewProjectBinding = DataBindingUtil.setContentView(this, R.layout.activity_new_project);

        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mActivityNewProjectBinding.coordinator.setVisibility(View.INVISIBLE);

            ViewTreeObserver viewTreeObserver = mActivityNewProjectBinding.coordinator.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        circularRevealActivity();
                        mActivityNewProjectBinding.coordinator.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        }

        setSupportActionBar(mActivityNewProjectBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        token = intent.getStringExtra(TOKEN);
        userId = intent.getStringExtra(USER_ID);

        mActivityNewProjectBinding.content.toggleBtnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivityNewProjectBinding.content.toggleBtnClient.setChecked(true);
                mActivityNewProjectBinding.content.toggleBtnBusiness.setChecked(false);
                mActivityNewProjectBinding.content.layoutInputManagersEmail.setVisibility(View.VISIBLE);
                mActivityNewProjectBinding.content.layoutInputClientsEmails.setVisibility(View.INVISIBLE);
                isClient = true;
            }
        });
        mActivityNewProjectBinding.content.toggleBtnBusiness.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivityNewProjectBinding.content.toggleBtnClient.setChecked(false);
                mActivityNewProjectBinding.content.toggleBtnBusiness.setChecked(true);
                mActivityNewProjectBinding.content.layoutInputClientsEmails.setVisibility(View.VISIBLE);
                mActivityNewProjectBinding.content.layoutInputManagersEmail.setVisibility(View.INVISIBLE);
                isClient = false;
            }
        });

        mActivityNewProjectBinding.content.imagePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });

        mActivityNewProjectBinding.content.btnAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopup(view);
            }
        });

        mActivityNewProjectBinding.content.inputClientsEmails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NewProjectActivity.this, AddClientsActivity.class);
                intent.putStringArrayListExtra(CLIENTS_EMAILS, clientsEmails);
                startActivityForResult(intent, REQUEST_ADD_CLIENTS);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        mActivityNewProjectBinding.content.btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
                if (!validate()) {
                    return;
                }
                mActivityNewProjectBinding.content.btnCreate.setEnabled(false);
                String title = mActivityNewProjectBinding.content.inputTitle.getText().toString();
                String description = mActivityNewProjectBinding.content.inputDescription.getText().toString().isEmpty() ? null : mActivityNewProjectBinding.content.inputDescription.getText().toString();
                String status = mActivityNewProjectBinding.content.inputStatus.getText().toString().isEmpty() ? null : mActivityNewProjectBinding.content.inputStatus.getText().toString();
                boolean progressEnabled = mActivityNewProjectBinding.content.switchProgressEnabled.isChecked();
                NewProjectRequest newProjectRequest;
                String image = null;
                if (!TextUtils.isEmpty(fileName) && fileUpload != null) {
                    image = userId + '/' + fileName;
                }
                if (isClient) {
                    String managerEmail = mActivityNewProjectBinding.content.inputManagersEmail.getText().toString();
                    newProjectRequest = new NewProjectRequest(title, NewProjectRequest.TYPE_CLIENT, image, status, description, managerEmail, null, progressEnabled);
                } else {
                    newProjectRequest = new NewProjectRequest(title, NewProjectRequest.TYPE_MANAGER, image, status, description, null, clientsEmails, progressEnabled);
                }
                createNewProject(newProjectRequest);
            }
        });

        mActivityNewProjectBinding.content.layoutInputTitle.setError(getString(R.string.error_message_new_project_title));
        mActivityNewProjectBinding.content.layoutInputDescription.setError(getString(R.string.error_message_new_project_description));
        mActivityNewProjectBinding.content.layoutInputStatus.setError(getString(R.string.error_message_initial_status));
        mActivityNewProjectBinding.content.layoutInputManagersEmail.setError(getString(R.string.error_message_managers_email));
        mActivityNewProjectBinding.content.layoutInputClientsEmails.setError(getString(R.string.error_message_clients_emails));
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            destroyCircularRevealActivity();
        else
            finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    destroyCircularRevealActivity();
                else
                    finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void destroyCircularRevealActivity() {
        int px = (int) Utils.convertDpToPixel(16 + 28, this);

        int cx = mActivityNewProjectBinding.coordinator.getWidth() - px;
        int cy = mActivityNewProjectBinding.coordinator.getHeight() - px;

        float finalRadius = (float) Math.sqrt(mActivityNewProjectBinding.coordinator.getWidth() * mActivityNewProjectBinding.coordinator.getWidth() +
                mActivityNewProjectBinding.coordinator.getHeight() * mActivityNewProjectBinding.coordinator.getHeight());

        // create the animator for this view (the start radius is zero)
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(mActivityNewProjectBinding.coordinator, cx, cy, finalRadius, 0);
        circularReveal.setDuration(400);

        circularReveal.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mActivityNewProjectBinding.coordinator.setVisibility(View.INVISIBLE);
                finishAfterTransition();
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        // make the view visible and start the animation
        mActivityNewProjectBinding.coordinator.setVisibility(View.VISIBLE);
        circularReveal.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void circularRevealActivity() {
        int px = (int) Utils.convertDpToPixel(16 + 28, this);

        int cx = mActivityNewProjectBinding.coordinator.getWidth() - px;
        int cy = mActivityNewProjectBinding.coordinator.getHeight() - px;

        float finalRadius = (float) Math.sqrt(mActivityNewProjectBinding.coordinator.getWidth() * mActivityNewProjectBinding.coordinator.getWidth()
                + mActivityNewProjectBinding.coordinator.getHeight() * mActivityNewProjectBinding.coordinator.getHeight());

        // create the animator for this view (the start radius is zero)
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(mActivityNewProjectBinding.coordinator, cx, cy, 0, finalRadius);
        circularReveal.setDuration(400);

        // make the view visible and start the animation
        mActivityNewProjectBinding.coordinator.setVisibility(View.VISIBLE);
        circularReveal.start();
    }

    private void showPopup(final View view) {
        PopupMenu popup = new PopupMenu(NewProjectActivity.this, view, GravityCompat.END);
        MenuInflater inflate = popup.getMenuInflater();
        Menu popupMenu = popup.getMenu();
        inflate.inflate(R.menu.menu_popup_edit_image, popupMenu);

        MenuItem actionRemovePhoto = popupMenu.findItem(R.id.action_remove_photo);
        actionRemovePhoto.setVisible(!TextUtils.isEmpty(fileName) && fileUpload != null);
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
                            Uri imageUri = FileProvider.getUriForFile(NewProjectActivity.this, CAPTURE_IMAGE_FILE_PROVIDER, filePhoto);
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
                        fileName = null;
                        fileUpload = null;
                        mActivityNewProjectBinding.content.imagePhoto.setImageResource(R.drawable.camera_mask);
                        mActivityNewProjectBinding.content.btnAddPhoto.setText(R.string.btn_add);
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
            case REQUEST_ADD_CLIENTS:
                if (resultCode == RESULT_OK && data != null) {
                    Bundle bundle = data.getExtras();
                    clientsEmails = bundle.getStringArrayList(CLIENTS_EMAILS);

                    String clientsEmailsStr = TextUtils.join(", ", clientsEmails);

                    mActivityNewProjectBinding.content.inputClientsEmails.setText(clientsEmailsStr);
                }
                break;
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                    Uri uri = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        Bitmap squareBitmap = ThumbnailUtils.extractThumbnail(bitmap, (int) Utils.convertDpToPixel(60, this), (int) Utils.convertDpToPixel(60, this));
                        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
                        float roundPx = (float) squareBitmap.getWidth() * 0.1f;
                        roundedBitmapDrawable.setCornerRadius(roundPx);
                        mActivityNewProjectBinding.content.imagePhoto.setImageDrawable(roundedBitmapDrawable);
                        fileName = S3Utils.getFileNameFromUri(this, uri);
                        fileUpload = S3Utils.createFileFromUri(this, uri, userId, fileName);
                        mActivityNewProjectBinding.content.btnAddPhoto.setText(R.string.btn_edit);
                    } catch (IOException e) {
                        Log.e(TAG, "onActivityResult: ", e);
                        showSnackbar(
                                mActivityNewProjectBinding.coordinator,
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
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri uri = FileProvider.getUriForFile(this, CAPTURE_IMAGE_FILE_PROVIDER, filePhoto);
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        Bitmap squareBitmap = ThumbnailUtils.extractThumbnail(bitmap, (int) Utils.convertDpToPixel(60, this), (int) Utils.convertDpToPixel(60, this));
                        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
                        float roundPx = (float) squareBitmap.getWidth() * 0.1f;
                        roundedBitmapDrawable.setCornerRadius(roundPx);
                        mActivityNewProjectBinding.content.imagePhoto.setImageDrawable(roundedBitmapDrawable);
                        fileName = S3Utils.getFileNameFromUri(this, uri);
                        fileUpload = S3Utils.createFileFromUri(this, uri, userId, fileName);
                        mActivityNewProjectBinding.content.btnAddPhoto.setText(R.string.btn_edit);
                    } catch (IOException e) {
                        Log.e(TAG, "onActivityResult: ", e);
                        showSnackbar(
                                mActivityNewProjectBinding.coordinator,
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

    private void createNewProject(final NewProjectRequest newProjectRequest) {
        Log.d(TAG, "createNewProject");

        progressDialog = new ProgressDialog(NewProjectActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_adding_new_project));
        progressDialog.show();

        if (TextUtils.isEmpty(fileName) || fileUpload == null) {
            createProject(newProjectRequest);
        } else {
            TransferListener mTransferListener = new TransferListener() {
                @Override
                public void onStateChanged(int id, TransferState state) {
                    Log.d(TAG, "onStateChanged: " + state);
                    if (TransferState.COMPLETED.equals(state)) {
                        createProject(newProjectRequest);
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {}

                @Override
                public void onError(int id, Exception ex) {
                    mActivityNewProjectBinding.content.btnCreate.setEnabled(true);
                    progressDialog.dismiss();
                    Log.e(TAG, "onError: ", ex);
                    String message = "Uploading image failed: " +  ex.getMessage();
                    showSnackbar(
                            mActivityNewProjectBinding.coordinator,
                            null,
                            ContextCompat.getColor(NewProjectActivity.this, R.color.red),
                            Color.WHITE,
                            message,
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                }
            };
            S3Utils.upload(NewProjectActivity.this, fileUpload, userId + '/' + fileName, mTransferListener);
        }
    }

    private void createProject(NewProjectRequest newProjectRequest) {
        ControlioService service = ServiceFactory.getControlioService(NewProjectActivity.this);
        service.createProject(token, userId, newProjectRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ProjectResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        mActivityNewProjectBinding.content.btnCreate.setEnabled(true);
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Creating a new project failed: " + e);
                                showSnackbar(
                                        mActivityNewProjectBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(NewProjectActivity.this, R.color.red),
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
                                Log.e(TAG, "Creating a new project failed: " + message);
                                showSnackbar(
                                        mActivityNewProjectBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(NewProjectActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Creating a new project failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Creating a new project failed: ", e);
                            showSnackbar(
                                    mActivityNewProjectBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(NewProjectActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Creating a new project failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(ProjectResponse projectResponse) {
                        mActivityNewProjectBinding.content.btnCreate.setEnabled(true);
                        progressDialog.dismiss();
                        Intent intent = new Intent();
                        intent.putExtra(PROJECT, projectResponse);
                        setResult(RESULT_OK, intent);
                        onBackPressed();
                    }
                });
    }

    @Override
    protected boolean validate() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String title = mActivityNewProjectBinding.content.inputTitle.getText().toString();
        String managersEmail = mActivityNewProjectBinding.content.inputManagersEmail.getText().toString();

        if (title.isEmpty()) {
            mActivityNewProjectBinding.content.layoutInputTitle.setError(getString(R.string.error_message_new_project_title));
            mActivityNewProjectBinding.content.layoutInputTitle.startAnimation(shake);
            valid = false;
        }
        if (isClient) {
            if (managersEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(managersEmail).matches()) {
                mActivityNewProjectBinding.content.layoutInputManagersEmail.startAnimation(shake);
                valid = false;
            }
        }

        return valid;
    }
}
