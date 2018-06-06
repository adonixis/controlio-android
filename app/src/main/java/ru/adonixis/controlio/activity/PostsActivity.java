package ru.adonixis.controlio.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
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
import ru.adonixis.controlio.adapter.NewAttachmentsAdapter;
import ru.adonixis.controlio.adapter.PostsAdapter;
import ru.adonixis.controlio.databinding.ActivityPostsBinding;
import ru.adonixis.controlio.databinding.DialogFullImageBinding;
import ru.adonixis.controlio.listener.EndlessRecyclerViewScrollListener;
import ru.adonixis.controlio.listener.OnNewAttachmentClickListener;
import ru.adonixis.controlio.listener.OnPostClickListener;
import ru.adonixis.controlio.model.DeletePostRequest;
import ru.adonixis.controlio.model.EditPostRequest;
import ru.adonixis.controlio.model.EditProgressRequest;
import ru.adonixis.controlio.model.NewPostRequest;
import ru.adonixis.controlio.model.OkResponse;
import ru.adonixis.controlio.model.PostDetailsResponse;
import ru.adonixis.controlio.model.PostResponse;
import ru.adonixis.controlio.model.ProjectResponse;
import ru.adonixis.controlio.model.UserResponse;
import ru.adonixis.controlio.network.ControlioService;
import ru.adonixis.controlio.network.ServiceFactory;
import ru.adonixis.controlio.util.S3Utils;
import ru.adonixis.controlio.util.Utils;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static ru.adonixis.controlio.model.NewPostRequest.TYPE_POST;
import static ru.adonixis.controlio.util.S3Utils.createFileFromUri;

public class PostsActivity extends BaseSubmitFormActivity {

    private static final String TAG = "PostsActivity";
    private static final int REQUEST_PICK_IMAGE = 0;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_PROJECT_INFO = 2;
    private static final String CAPTURE_IMAGE_FILE_PROVIDER = "ru.adonixis.controlio.fileprovider";
    private static final String TOKEN = "token";
    private static final String USER_ID = "userId";
    private static final String PROJECT = "project";
    private static final String USER = "user";
    private static final int DEFAULT_ITEMS_COUNT = 20;
    private ActivityPostsBinding mActivityPostsBinding;
    private DialogFullImageBinding dialogFullImageBinding;
    private ProjectResponse project;
    private List<PostDetailsResponse> posts = new ArrayList<>();
    private int postPosition;
    private List<Uri> newAttachments = new ArrayList<>();
    private List<String> downloadedAttachments = new ArrayList<>();
    private List<String> fileNames = new ArrayList<>();
    private PostsAdapter postsAdapter;
    private NewAttachmentsAdapter newAttachmentsAdapter;
    private LinearLayoutManager layoutManager;
    private String token;
    private String userId;
    private Dialog dialog;
    private boolean isMessage = true;
    private boolean isCreateRequest = true;
    private ProgressDialog progressDialog;
    private File filePhoto;
    private int progress;

    private OnPostClickListener onPostClickListener = new OnPostClickListener() {
        @Override
        public void onPostLongClick(View view, int position) {
            postPosition = position;
            showPopupPost(view, position);
        }

        @Override
        public void onManagerPhotoClick(View view, UserResponse manager) {
            Intent intent = new Intent(PostsActivity.this, UserInfoActivity.class);
            intent.putExtra(USER, manager);
            startActivity(intent);
            overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
    };

    private OnNewAttachmentClickListener onNewAttachmentClickListener = new OnNewAttachmentClickListener() {
        @Override
        public void onNewAttachmentClick(View view, int position) {
            dialogFullImageBinding = DataBindingUtil.inflate(LayoutInflater.from(PostsActivity.this), R.layout.dialog_full_image, null, false);
            Uri newAttachment = newAttachments.get(position);
            Glide.with(PostsActivity.this)
                    .load(newAttachment)
                    .into(dialogFullImageBinding.imageFull);

            dialog = new Dialog(PostsActivity.this, R.style.AppTheme_Light_DialogTransparent);
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

        @Override
        public void onRemoveNewAttachmentClick(View view, int position) {
            newAttachments.remove(position);
            newAttachmentsAdapter.notifyItemRemoved(position + downloadedAttachments.size());
            if (downloadedAttachments.isEmpty() && newAttachments.isEmpty()) {
                mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.GONE);
            }
        }

        @Override
        public void onDownloadedAttachmentClick(View view, int position) {
            dialogFullImageBinding = DataBindingUtil.inflate(LayoutInflater.from(PostsActivity.this), R.layout.dialog_full_image, null, false);
            File file = new File(getCacheDir(), '/' + downloadedAttachments.get(position));
            if (file.exists()) {
                Glide.with(PostsActivity.this)
                        .load(file)
                        .into(dialogFullImageBinding.imageFull);

                dialog = new Dialog(PostsActivity.this, R.style.AppTheme_Light_DialogTransparent);
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

        @Override
        public void onRemoveDownloadedAttachmentClick(View view, int position) {
            downloadedAttachments.remove(position);
            newAttachmentsAdapter.notifyItemRemoved(position);
            if (downloadedAttachments.isEmpty() && newAttachments.isEmpty()) {
                mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.GONE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityPostsBinding = DataBindingUtil.setContentView(this, R.layout.activity_posts);

        setSupportActionBar(mActivityPostsBinding.toolbar);
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

        mActivityPostsBinding.setFinished(project.isFinished());
        mActivityPostsBinding.setCanEdit(project.isCanEdit());
        mActivityPostsBinding.setProgressEnabled(project.isProgressEnabled());
        progress = project.getProgress();
        mActivityPostsBinding.setProgress(progress);

        layoutManager = new LinearLayoutManager(this);
        mActivityPostsBinding.recyclerPosts.setLayoutManager(layoutManager);
        mActivityPostsBinding.recyclerPosts.setHasFixedSize(true);
        mActivityPostsBinding.recyclerPosts.setItemViewCacheSize(20);
        mActivityPostsBinding.recyclerPosts.setDrawingCacheEnabled(true);
        mActivityPostsBinding.recyclerPosts.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        postsAdapter = new PostsAdapter(posts, project, onPostClickListener);
        mActivityPostsBinding.recyclerPosts.setAdapter(postsAdapter);

        mActivityPostsBinding.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.green_blue));
        mActivityPostsBinding.swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showRefreshing();
                getPosts(0, DEFAULT_ITEMS_COUNT);
            }
        });
        if (project.isCanEdit() && !project.isFinished()) {
            mActivityPostsBinding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int newProgress, boolean fromUser) {
                    if (fromUser) {
                        progress = newProgress;
                        mActivityPostsBinding.setProgress(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    editProgress(new EditProgressRequest(project.getId(), progress));
                }
            });

            mActivityPostsBinding.btnProgressDec.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    progress--;
                    mActivityPostsBinding.setProgress(progress);
                    editProgress(new EditProgressRequest(project.getId(), progress));
                }
            });

            mActivityPostsBinding.btnProgressInc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    progress++;
                    mActivityPostsBinding.setProgress(progress);
                    editProgress(new EditProgressRequest(project.getId(), progress));
                }
            });

            mActivityPostsBinding.iconAttachment.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupAttachment(view);
                }
            });

            RecyclerView recyclerNewAttachments = mActivityPostsBinding.recyclerNewAttachments;
            LinearLayoutManager horizontalLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            recyclerNewAttachments.setLayoutManager(horizontalLayoutManager);
            recyclerNewAttachments.setHasFixedSize(true);
            newAttachmentsAdapter = new NewAttachmentsAdapter(downloadedAttachments, newAttachments, onNewAttachmentClickListener);
            recyclerNewAttachments.setAdapter(newAttachmentsAdapter);

            mActivityPostsBinding.btnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!validate()) {
                        return;
                    }
                    hideKeyboard();
                    mActivityPostsBinding.btnSend.setEnabled(false);
                    String message = mActivityPostsBinding.inputNewMessage.getText().toString();
                    if (isCreateRequest) {
                        uploadImages(message);
                    } else {
                        editImages(message);
                    }

                }
            });
            mActivityPostsBinding.btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!validate()) {
                        return;
                    }
                    hideKeyboard();
                    mActivityPostsBinding.btnSave.setEnabled(false);
                    String status = mActivityPostsBinding.inputStatus.getText().toString();
                    if (isCreateRequest) {
                        NewPostRequest changeStatusRequest = new NewPostRequest(project.getId(), status, null, NewPostRequest.TYPE_STATUS);
                        changeStatus(changeStatusRequest);
                    } else {
                        EditPostRequest editStatusRequest = new EditPostRequest(project.getId(), posts.get(postPosition).getId(), status, null);
                        editStatus(editStatusRequest);
                    }
                }
            });
            mActivityPostsBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hideKeyboard();
                    mActivityPostsBinding.btnCancel.setVisibility(View.GONE);
                    mActivityPostsBinding.toggleButtons.setVisibility(View.VISIBLE);
                    downloadedAttachments.clear();
                    newAttachments.clear();
                    fileNames.clear();
                    newAttachmentsAdapter.notifyDataSetChanged();
                    mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.GONE);
                    mActivityPostsBinding.inputNewMessage.setText("");
                    mActivityPostsBinding.inputStatus.setText("");
                    isCreateRequest = true;
                }
            });
            mActivityPostsBinding.btnNewMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mActivityPostsBinding.btnNewMessage.setChecked(true);
                    mActivityPostsBinding.btnChangeStatus.setChecked(false);
                    mActivityPostsBinding.viewFlipper.setDisplayedChild(0);
                    isMessage = true;
                }
            });
            mActivityPostsBinding.btnChangeStatus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mActivityPostsBinding.btnNewMessage.setChecked(false);
                    mActivityPostsBinding.btnChangeStatus.setChecked(true);
                    mActivityPostsBinding.viewFlipper.setDisplayedChild(1);
                    isMessage = false;
                }
            });
            mActivityPostsBinding.layoutEditPost.setVisibility(View.VISIBLE);
        } else {
            mActivityPostsBinding.layoutEditPost.setVisibility(View.GONE);
        }

        showRefreshing();
        getPosts(0, DEFAULT_ITEMS_COUNT);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        final MenuItem infoItem = menu.findItem(R.id.action_info);
        if (project.getImage() != null) {
            final File file = new File(getCacheDir(), project.getImage());
            if (file.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                if (bitmap != null) {
                    Bitmap squareBitmap = ThumbnailUtils.extractThumbnail(bitmap, (int) Utils.convertDpToPixel(60, this), (int) Utils.convertDpToPixel(60, this));
                    RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
                    float roundPx = (float) squareBitmap.getWidth() * 0.1f;
                    roundedBitmapDrawable.setCornerRadius(roundPx);
                    infoItem.setIcon(roundedBitmapDrawable);
                } else {
                    infoItem.setIcon(R.drawable.ic_info_outline_white_24dp);
                }
            } else {
                infoItem.setIcon(R.drawable.ic_info_outline_white_24dp);
            }
        } else {
            infoItem.setIcon(R.drawable.ic_info_outline_white_24dp);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_info:
                Intent intent = new Intent(PostsActivity.this, ProjectInfoActivity.class);
                intent.putExtra(TOKEN, token);
                intent.putExtra(USER_ID, userId);
                intent.putExtra(PROJECT, project);
                startActivityForResult(intent, REQUEST_PROJECT_INFO);
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                return true;
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showPopupAttachment(final View view) {
        PopupMenu popup = new PopupMenu(PostsActivity.this, view, GravityCompat.END);
        MenuInflater inflate = popup.getMenuInflater();
        Menu popupMenu = popup.getMenu();
        inflate.inflate(R.menu.menu_popup_new_attachment, popupMenu);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_choose_image:
                        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        pickImageIntent.setType("image/*");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            pickImageIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        }
                        startActivityForResult(pickImageIntent, REQUEST_PICK_IMAGE);
                        break;
                    case R.id.action_take_photo:
                        Intent takePictureIntent  = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            try {
                                filePhoto = S3Utils.createTempImageFile(PostsActivity.this);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Uri imageUri = FileProvider.getUriForFile(PostsActivity.this, CAPTURE_IMAGE_FILE_PROVIDER, filePhoto);
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
        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                if (resultCode == RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            Uri uri = item.getUri();
                            mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.VISIBLE);
                            newAttachments.add(uri);
                            newAttachmentsAdapter.notifyItemInserted(downloadedAttachments.size() + newAttachments.size() - 1);
                        }
                    } else {
                        if (data.getData() != null){
                            Uri uri = data.getData();
                            mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.VISIBLE);
                            newAttachments.add(uri);
                            newAttachmentsAdapter.notifyItemInserted(downloadedAttachments.size() + newAttachments.size() - 1);
                        }
                    }
                }
                break;
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri uri = FileProvider.getUriForFile(this, CAPTURE_IMAGE_FILE_PROVIDER, filePhoto);
                    mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.VISIBLE);
                    newAttachments.add(uri);
                    newAttachmentsAdapter.notifyItemInserted(downloadedAttachments.size() + newAttachments.size() - 1);
                }
                break;
            case REQUEST_PROJECT_INFO:
                if (resultCode == RESULT_OK) {
                    setResult(RESULT_OK, null);
                    finish();
                    overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void getPosts(final int skip, int limit) {
        ControlioService service = ServiceFactory.getControlioService(this);
        service.getPosts(token, userId, project.getId(), skip, limit)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<PostDetailsResponse>>() {
                    @Override
                    public final void onCompleted() {
                        hideRefreshing();
                    }

                    @Override
                    public final void onError(Throwable e) {
                        hideRefreshing();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Get posts failed: " + e);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
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
                                Log.e(TAG, "Get posts failed: " + message);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Get posts failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Get posts failed: ", e);
                            showSnackbar(
                                    mActivityPostsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PostsActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Get posts failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(List<PostDetailsResponse> postsResponse) {
                        Log.d(TAG, posts.toString());
                        if (postsResponse.isEmpty() && posts.isEmpty()) {
                            if (project.isCanEdit()) {
                                mActivityPostsBinding.labelPlaceholderNoPosts.setText(R.string.label_placeholder_no_posts_for_manager);
                            } else {
                                mActivityPostsBinding.labelPlaceholderNoPosts.setText(R.string.label_placeholder_no_posts_for_client);
                            }
                            mActivityPostsBinding.placeholderNoPosts.setVisibility(View.VISIBLE);
                            mActivityPostsBinding.swipeRefresh.setVisibility(View.GONE);
                        } else {
                            mActivityPostsBinding.placeholderNoPosts.setVisibility(View.GONE);
                            mActivityPostsBinding.swipeRefresh.setVisibility(View.VISIBLE);
                        }
                        if (!posts.equals(postsResponse)) {
                            if (skip == 0) {
                                mActivityPostsBinding.recyclerPosts.clearOnScrollListeners();
                                mActivityPostsBinding.recyclerPosts.addOnScrollListener(new EndlessRecyclerViewScrollListener(layoutManager, DEFAULT_ITEMS_COUNT) {
                                    @Override
                                    public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                                        int offset = page * DEFAULT_ITEMS_COUNT;
                                        getPosts(offset, DEFAULT_ITEMS_COUNT);
                                    }
                                });
                                posts.clear();
                            }
                            posts.addAll(postsResponse);
                            postsAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void showRefreshing() {
        mActivityPostsBinding.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityPostsBinding.swipeRefresh.setRefreshing(true);
            }
        });
    }

    private void hideRefreshing() {
        mActivityPostsBinding.swipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mActivityPostsBinding.swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void uploadImages(final String message) {
        Log.d(TAG, "uploadImages");

        progressDialog = new ProgressDialog(PostsActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_creating_new_post));
        progressDialog.show();

        if (newAttachments.isEmpty()) {
            NewPostRequest newPostRequest = new NewPostRequest(project.getId(), message, null, TYPE_POST);
            createNewPost(newPostRequest);
        } else {
            final TransferListener mTransferListener = new TransferListener() {
                int i = 0;

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED.equals(state)) {
                        if (i == newAttachments.size() - 1) {
                            List<String> s3ObjectKeys = new ArrayList<>();
                            for (String fileName : fileNames) {
                                String s3ObjectKey = userId + '/' + fileName;
                                s3ObjectKeys.add(s3ObjectKey);
                            }
                            NewPostRequest newPostRequest;
                            if (message.isEmpty()) {
                                newPostRequest = new NewPostRequest(project.getId(), null, s3ObjectKeys, TYPE_POST);
                            } else {
                                newPostRequest = new NewPostRequest(project.getId(), message, s3ObjectKeys, TYPE_POST);
                            }
                            createNewPost(newPostRequest);
                        } else {
                            i++;
                            String fileName = S3Utils.getFileNameFromUri(PostsActivity.this, newAttachments.get(i));
                            fileNames.add(fileName);
                            try {
                                File uploadFile = S3Utils.createFileFromUri(PostsActivity.this, newAttachments.get(i), userId, fileName);
                                S3Utils.upload(PostsActivity.this, uploadFile, userId + '/' + fileName, this);
                            } catch (IOException e) {
                                Log.e(TAG, "onActivityResult: ", e);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                        Color.WHITE,
                                        "Picked image failed: " + e.getMessage(),
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            }
                        }
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                }

                @Override
                public void onError(int id, Exception ex) {
                    mActivityPostsBinding.btnSend.setEnabled(true);
                    Log.e(TAG, "onError: ", ex);
                    showSnackbar(
                            mActivityPostsBinding.coordinator,
                            null,
                            ContextCompat.getColor(PostsActivity.this, R.color.red),
                            Color.WHITE,
                            "Uploading image failed: " + ex.getMessage(),
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                }
            };
            String fileName = S3Utils.getFileNameFromUri(PostsActivity.this, newAttachments.get(0));
            fileNames.add(fileName);
            try {
                File uploadFile = createFileFromUri(PostsActivity.this, newAttachments.get(0), userId, fileName);
                S3Utils.upload(PostsActivity.this, uploadFile, userId + '/' + fileName, mTransferListener);
            } catch (IOException e) {
                Log.e(TAG, "onActivityResult: ", e);
                showSnackbar(
                        mActivityPostsBinding.coordinator,
                        null,
                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                        Color.WHITE,
                        "Picked image failed: " + e.getMessage(),
                        Color.WHITE,
                        getString(R.string.snackbar_action_hide),
                        null
                );
            }
        }
    }

    private void createNewPost(NewPostRequest newPostRequest) {
        ControlioService service = ServiceFactory.getControlioService(PostsActivity.this);
        service.createPost(token, userId, newPostRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<PostResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        mActivityPostsBinding.btnSend.setEnabled(true);
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Creating a new post failed: " + e);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
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
                                Log.e(TAG, "Creating a new post failed: " + message);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Creating a new post failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Creating a new post failed: ", e);
                            showSnackbar(
                                    mActivityPostsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PostsActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Creating a new post failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(PostResponse postResponse) {
                        mActivityPostsBinding.btnSend.setEnabled(true);
                        progressDialog.dismiss();
                        showRefreshing();
                        getPosts(0, DEFAULT_ITEMS_COUNT);

                        newAttachments.clear();
                        fileNames.clear();
                        newAttachmentsAdapter.notifyDataSetChanged();
                        mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.GONE);
                        mActivityPostsBinding.inputNewMessage.setText("");

                        showSnackbar(
                                mActivityPostsBinding.coordinator,
                                null,
                                ContextCompat.getColor(PostsActivity.this, R.color.dark_green_blue),
                                Color.WHITE,
                                getString(R.string.snackbar_message_message_sent),
                                Color.WHITE,
                                getString(R.string.snackbar_action_hide),
                                null
                        );
                    }
                });
    }

    private void editImages(final String message) {
        Log.d(TAG, "editImages");

        progressDialog = new ProgressDialog(PostsActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_editing_post));
        progressDialog.show();

        if (newAttachments.isEmpty()) {
            EditPostRequest editPostRequest = new EditPostRequest(project.getId(), posts.get(postPosition).getId(), message, downloadedAttachments);
            editPost(editPostRequest);
        } else {
            final TransferListener mTransferListener = new TransferListener() {
                int i = 0;

                @Override
                public void onStateChanged(int id, TransferState state) {
                    if (TransferState.COMPLETED.equals(state)) {
                        if (i == newAttachments.size() - 1) {
                            for (String fileName : fileNames) {
                                String s3ObjectKey = userId + '/' + fileName;
                                downloadedAttachments.add(s3ObjectKey);
                            }
                            EditPostRequest editPostRequest;
                            if (message.isEmpty()) {
                                editPostRequest = new EditPostRequest(project.getId(), posts.get(postPosition).getId(), null, downloadedAttachments);
                            } else {
                                editPostRequest = new EditPostRequest(project.getId(), posts.get(postPosition).getId(), message, downloadedAttachments);
                            }
                            editPost(editPostRequest);
                        } else {
                            i++;
                            String fileName = S3Utils.getFileNameFromUri(PostsActivity.this, newAttachments.get(i));
                            fileNames.add(fileName);
                            try {
                                File uploadFile = S3Utils.createFileFromUri(PostsActivity.this, newAttachments.get(i), userId, fileName);
                                S3Utils.upload(PostsActivity.this, uploadFile, userId + '/' + fileName, this);
                            } catch (IOException e) {
                                Log.e(TAG, "onActivityResult: ", e);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                        Color.WHITE,
                                        "Picked image failed: " + e.getMessage(),
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            }
                        }
                    }
                }

                @Override
                public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                }

                @Override
                public void onError(int id, Exception ex) {
                    mActivityPostsBinding.btnSend.setEnabled(true);
                    Log.e(TAG, "onError: ", ex);
                    showSnackbar(
                            mActivityPostsBinding.coordinator,
                            null,
                            ContextCompat.getColor(PostsActivity.this, R.color.red),
                            Color.WHITE,
                            "Uploading image failed: " + ex.getMessage(),
                            Color.WHITE,
                            getString(R.string.snackbar_action_hide),
                            null
                    );
                }
            };
            String fileName = S3Utils.getFileNameFromUri(PostsActivity.this, newAttachments.get(0));
            fileNames.add(fileName);
            try {
                File uploadFile = createFileFromUri(PostsActivity.this, newAttachments.get(0), userId, fileName);
                S3Utils.upload(PostsActivity.this, uploadFile, userId + '/' + fileName, mTransferListener);
            } catch (IOException e) {
                Log.e(TAG, "onActivityResult: ", e);
                showSnackbar(
                        mActivityPostsBinding.coordinator,
                        null,
                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                        Color.WHITE,
                        "Picked image failed: " + e.getMessage(),
                        Color.WHITE,
                        getString(R.string.snackbar_action_hide),
                        null
                );
            }
        }
    }

    private void editPost(EditPostRequest editPostRequest) {
        ControlioService service = ServiceFactory.getControlioService(PostsActivity.this);
        service.editPost(token, userId, editPostRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<PostResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        mActivityPostsBinding.btnSend.setEnabled(true);
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Creating a new post failed: " + e);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
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
                                Log.e(TAG, "Creating a new post failed: " + message);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Creating a new post failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Creating a new post failed: ", e);
                            showSnackbar(
                                    mActivityPostsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PostsActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Creating a new post failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(PostResponse postResponse) {
                        mActivityPostsBinding.btnSend.setEnabled(true);
                        progressDialog.dismiss();
                        showRefreshing();
                        getPosts(0, DEFAULT_ITEMS_COUNT);

                        downloadedAttachments.clear();
                        newAttachments.clear();
                        fileNames.clear();
                        newAttachmentsAdapter.notifyDataSetChanged();
                        mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.GONE);
                        mActivityPostsBinding.inputNewMessage.setText("");
                        mActivityPostsBinding.btnCancel.performClick();

                        showSnackbar(
                                mActivityPostsBinding.coordinator,
                                null,
                                ContextCompat.getColor(PostsActivity.this, R.color.dark_green_blue),
                                Color.WHITE,
                                getString(R.string.snackbar_message_message_changed),
                                Color.WHITE,
                                getString(R.string.snackbar_action_hide),
                                null
                        );
                    }
                });
    }

    private void changeStatus(final NewPostRequest changeStatusRequest) {
        Log.d(TAG, "changeStatus");

        final ProgressDialog progressDialog = new ProgressDialog(PostsActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_changing_status));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.createPost(token, userId, changeStatusRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<PostResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        mActivityPostsBinding.btnSave.setEnabled(true);
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Changing status of project failed: " + e);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
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
                                Log.e(TAG, "Changing status of project failed: " + message);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Changing status of project failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Changing status of project failed: ", e);
                            showSnackbar(
                                    mActivityPostsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PostsActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Changing status of project failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(PostResponse postResponse) {
                        mActivityPostsBinding.btnSave.setEnabled(true);
                        progressDialog.dismiss();
                        showRefreshing();
                        getPosts(0, DEFAULT_ITEMS_COUNT);

                        mActivityPostsBinding.inputStatus.setText("");

                        showSnackbar(
                                mActivityPostsBinding.coordinator,
                                null,
                                ContextCompat.getColor(PostsActivity.this, R.color.dark_green_blue),
                                Color.WHITE,
                                getString(R.string.snackbar_message_status_has_been_changed),
                                Color.WHITE,
                                getString(R.string.snackbar_action_hide),
                                null
                        );
                    }
                });
    }

    private void editStatus(final EditPostRequest editStatusRequest) {
        Log.d(TAG, "editStatus");

        final ProgressDialog progressDialog = new ProgressDialog(PostsActivity.this, R.style.AppTheme_Light_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(getString(R.string.progress_message_editing_status));
        progressDialog.show();

        ControlioService service = ServiceFactory.getControlioService(this);
        service.editPost(token, userId, editStatusRequest)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<PostResponse>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        mActivityPostsBinding.btnSave.setEnabled(true);
                        progressDialog.dismiss();
                        if (e instanceof HttpException) {
                            if ((((HttpException) e).code() == 504)) {
                                Log.e(TAG, "Changing status of project failed: " + e);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
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
                                Log.e(TAG, "Changing status of project failed: " + message);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Changing status of project failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Changing status of project failed: ", e);
                            showSnackbar(
                                    mActivityPostsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PostsActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Changing status of project failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(PostResponse postResponse) {
                        mActivityPostsBinding.btnSave.setEnabled(true);
                        progressDialog.dismiss();
                        showRefreshing();
                        getPosts(0, DEFAULT_ITEMS_COUNT);

                        mActivityPostsBinding.inputStatus.setText("");
                        mActivityPostsBinding.btnCancel.performClick();

                        showSnackbar(
                                mActivityPostsBinding.coordinator,
                                null,
                                ContextCompat.getColor(PostsActivity.this, R.color.dark_green_blue),
                                Color.WHITE,
                                getString(R.string.snackbar_message_status_changed),
                                Color.WHITE,
                                getString(R.string.snackbar_action_hide),
                                null
                        );
                    }
                });
    }

    private void deletePost(final int position) {
        Log.d(TAG, "deletePost");

        final PostDetailsResponse post = posts.get(position);
        final String postId = post.getId();

        posts.remove(position);
        postsAdapter.notifyItemRemoved(position);

        Snackbar.Callback callback = new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                switch (event) {
                    case Snackbar.Callback.DISMISS_EVENT_ACTION:
                        break;
                    default:
                        ControlioService service = ServiceFactory.getControlioService(PostsActivity.this);
                        service.deletePost(token, userId, new DeletePostRequest(project.getId(), postId))
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<OkResponse>() {
                                    @Override
                                    public final void onCompleted() {
                                    }

                                    @Override
                                    public final void onError(Throwable e) {
                                        posts.add(position, post);
                                        postsAdapter.notifyItemInserted(position);
                                        layoutManager.scrollToPosition(position);
                                        if (e instanceof HttpException) {
                                            if ((((HttpException) e).code() == 504)) {
                                                Log.e(TAG, "Delete post failed: " + e);
                                                showSnackbar(
                                                        mActivityPostsBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
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
                                                Log.e(TAG, "Delete post failed: " + message);
                                                showSnackbar(
                                                        mActivityPostsBinding.coordinator,
                                                        null,
                                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                                        Color.WHITE,
                                                        message,
                                                        Color.WHITE,
                                                        getString(R.string.snackbar_action_hide),
                                                        null
                                                );
                                            } catch (JSONException | IOException ex) {
                                                Log.e(TAG, "Delete post failed: ", ex);
                                            }
                                        } else if (e instanceof java.io.IOException) {
                                            Log.e(TAG, "Delete post failed: ", e);
                                            showSnackbar(
                                                    mActivityPostsBinding.coordinator,
                                                    null,
                                                    ContextCompat.getColor(PostsActivity.this, R.color.red),
                                                    Color.WHITE,
                                                    getString(R.string.error_message_check_internet),
                                                    Color.WHITE,
                                                    getString(R.string.snackbar_action_hide),
                                                    null
                                            );
                                        } else {
                                            Log.e(TAG, "Delete post failed: ", e);
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
                posts.add(position, post);
                postsAdapter.notifyItemInserted(position);
                layoutManager.scrollToPosition(position);
            }
        };

        showSnackbar(
                mActivityPostsBinding.coordinator,
                callback,
                ContextCompat.getColor(this, R.color.dark_green_blue),
                Color.WHITE,
                getString(R.string.snackbar_message_post_deleted),
                Color.WHITE,
                getString(R.string.snackbar_action_undo),
                onClickListener
        );
    }

    private void editProgress(final EditProgressRequest editProgressRequest) {
        Log.d(TAG, "editProgress");

        ControlioService service = ServiceFactory.getControlioService(this);
        service.editProgress(token, userId, editProgressRequest)
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
                                Log.e(TAG, "Editing progress of project failed: " + e);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
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
                                Log.e(TAG, "Editing progress of project failed: " + message);
                                showSnackbar(
                                        mActivityPostsBinding.coordinator,
                                        null,
                                        ContextCompat.getColor(PostsActivity.this, R.color.red),
                                        Color.WHITE,
                                        message,
                                        Color.WHITE,
                                        getString(R.string.snackbar_action_hide),
                                        null
                                );
                            } catch (JSONException | IOException ex) {
                                Log.e(TAG, "Editing progress of project failed: ", ex);
                            }
                        } else if (e instanceof java.io.IOException) {
                            Log.e(TAG, "Editing progress of project failed: ", e);
                            showSnackbar(
                                    mActivityPostsBinding.coordinator,
                                    null,
                                    ContextCompat.getColor(PostsActivity.this, R.color.red),
                                    Color.WHITE,
                                    getString(R.string.error_message_check_internet),
                                    Color.WHITE,
                                    getString(R.string.snackbar_action_hide),
                                    null
                            );
                        } else {
                            Log.e(TAG, "Editing progress of project failed: ", e);
                        }
                    }

                    @Override
                    public void onNext(OkResponse okResponse) {

                    }
                });
    }

    private void showPopupPost(final View view, final int position) {
        PopupMenu popup = new PopupMenu(PostsActivity.this, view, GravityCompat.END);
        MenuInflater inflate = popup.getMenuInflater();
        inflate.inflate(R.menu.menu_popup_post, popup.getMenu());

        MenuItem actionEdit = popup.getMenu().findItem(R.id.action_edit);
        final PostDetailsResponse post = posts.get(position);
        actionEdit.setVisible(userId.equals(post.getAuthor().getId()));

        MenuItem actionDelete = popup.getMenu().findItem(R.id.action_delete);
        SpannableString ss = new SpannableString(actionDelete.getTitle().toString());
        ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.red)), 0, ss.length(), 0);
        actionDelete.setTitle(ss);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_edit:
                        if (userId.equals(post.getAuthor().getId())) {
                            mActivityPostsBinding.btnCancel.setVisibility(View.VISIBLE);
                            mActivityPostsBinding.toggleButtons.setVisibility(View.GONE);
                            PostDetailsResponse post = posts.get(position);
                            if (post.getType().equals(TYPE_POST)) {
                                mActivityPostsBinding.btnNewMessage.setChecked(true);
                                mActivityPostsBinding.btnChangeStatus.setChecked(false);
                                mActivityPostsBinding.viewFlipper.setDisplayedChild(0);
                                isMessage = true;
                                newAttachments.clear();
                                downloadedAttachments.clear();
                                downloadedAttachments.addAll(post.getAttachments());
                                if (!downloadedAttachments.isEmpty()) {
                                    mActivityPostsBinding.recyclerNewAttachments.setVisibility(View.VISIBLE);
                                }
                                newAttachmentsAdapter.notifyDataSetChanged();
                                mActivityPostsBinding.inputNewMessage.setText(post.getText());
                            } else {
                                mActivityPostsBinding.btnNewMessage.setChecked(false);
                                mActivityPostsBinding.btnChangeStatus.setChecked(true);
                                mActivityPostsBinding.viewFlipper.setDisplayedChild(1);
                                isMessage = false;
                                mActivityPostsBinding.inputStatus.setText(posts.get(position).getText());
                            }
                            isCreateRequest = false;
                        }
                        break;
                    case R.id.action_delete:
                        deletePost(position);
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
    protected boolean validate() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String message = mActivityPostsBinding.inputNewMessage.getText().toString();
        String status = mActivityPostsBinding.inputStatus.getText().toString();

        if (isMessage) {
            if (downloadedAttachments.isEmpty() && newAttachments.isEmpty() && message.isEmpty()) {
                mActivityPostsBinding.inputNewMessage.startAnimation(shake);
                valid = false;
            }
        } else {
            if (status.isEmpty()) {
                mActivityPostsBinding.inputStatus.startAnimation(shake);
                valid = false;
            }
        }

        return valid;
    }
}
