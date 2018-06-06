package ru.adonixis.controlio.activity;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import ru.adonixis.controlio.R;
import ru.adonixis.controlio.databinding.ActivitySupportBinding;

import static ru.adonixis.controlio.util.Utils.goToMarket;
import static ru.adonixis.controlio.util.Utils.isAppInstalled;

public class SupportActivity extends AppCompatActivity {

    private static final String TAG = "SupportActivity";
    private ActivitySupportBinding mActivitySupportBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivitySupportBinding = DataBindingUtil.setContentView(this, R.layout.activity_support);

        setSupportActionBar(mActivitySupportBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mActivitySupportBinding.content.linearTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAppInstalled(SupportActivity.this, "org.telegram.messenger")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SupportActivity.this);
                    builder.setTitle(R.string.dialog_title_telegram_not_installed);
                    builder.setMessage(R.string.dialog_message_install_telegram);
                    builder.setPositiveButton(R.string.dialog_btn_install, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            goToMarket(SupportActivity.this, "org.telegram.messenger");
                        }
                    });
                    builder.setNegativeButton(R.string.dialog_btn_cancel, null);
                    builder.show();
                } else {
                    Uri telegramUri = Uri.parse("tg://resolve?domain=borodutch");
                    Intent telegramIntent = new Intent(Intent.ACTION_VIEW, telegramUri);
                    telegramIntent.setComponent(new ComponentName("org.telegram.messenger", "org.telegram.ui.LaunchActivity"));
                    telegramIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(telegramIntent);
                }
            }
        });

        mActivitySupportBinding.content.linearFacebookMessenger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAppInstalled(SupportActivity.this, "com.facebook.orca")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(SupportActivity.this);
                    builder.setTitle(R.string.dialog_title_messenger_not_installed);
                    builder.setMessage(R.string.dialog_message_install_messenger);
                    builder.setPositiveButton(R.string.dialog_btn_install, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            goToMarket(SupportActivity.this, "com.facebook.orca");
                        }
                    });
                    builder.setNegativeButton(R.string.dialog_btn_cancel, null);
                    builder.show();
                } else {
                    Uri facebookUri = Uri.parse("fb-messenger://user/100000419744452");
                    Intent facebookIntent = new Intent(Intent.ACTION_VIEW, facebookUri);
                    facebookIntent.setPackage("com.facebook.orca");
                    facebookIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(facebookIntent);
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
