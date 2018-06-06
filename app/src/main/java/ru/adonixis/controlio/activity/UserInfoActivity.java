package ru.adonixis.controlio.activity;

import android.app.Dialog;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.bumptech.glide.Glide;

import java.io.File;

import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.ActivityUserInfoBinding;
import ru.adonixis.controlio.databinding.DialogFullImageBinding;
import ru.adonixis.controlio.model.UserResponse;

public class UserInfoActivity extends BaseActivity {

    private static final String TAG = "UserInfoActivity";
    private static final String USER = "user";
    private UserResponse user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUserInfoBinding mActivityUserInfoBinding = DataBindingUtil.setContentView(this, R.layout.activity_user_info);

        setSupportActionBar(mActivityUserInfoBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("");

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            user = (UserResponse) bundle.getSerializable(USER);
        }
        mActivityUserInfoBinding.setUser(user);

        mActivityUserInfoBinding.imageUserPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFullImageBinding dialogFullImageBinding = DataBindingUtil.inflate(LayoutInflater.from(UserInfoActivity.this), R.layout.dialog_full_image, null, false);
                File file = new File(getCacheDir(), '/' + user.getPhoto());
                if (file.exists()) {
                    Glide.with(UserInfoActivity.this)
                            .load(file)
                            .into(dialogFullImageBinding.imageFull);

                    final Dialog dialog = new Dialog(UserInfoActivity.this, R.style.AppTheme_Light_DialogTransparent);
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

}