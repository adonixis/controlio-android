package ru.adonixis.controlio.activity;

import android.content.Context;
import android.support.annotation.ColorInt;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public abstract class BaseActivity extends AppCompatActivity {
    protected void showSnackbar(View view,
                                Snackbar.Callback callback,
                                @ColorInt int backgroundColor,
                                @ColorInt int textColor,
                                String text,
                                @ColorInt int actionTextColor,
                                String actionText,
                                View.OnClickListener onClickListener) {
        if (onClickListener == null) {
            onClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {}
            };
        }
        Snackbar snackbar = Snackbar
                .make(view, text, Snackbar.LENGTH_LONG)
                .addCallback(callback)
                .setActionTextColor(actionTextColor)
                .setAction(actionText, onClickListener);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(backgroundColor);
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(textColor);
        snackbar.show();
    }

    protected void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }
}
