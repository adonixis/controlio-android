package ru.adonixis.controlio.listener;

import android.view.View;

import ru.adonixis.controlio.model.InviteDetailsResponse;

public interface OnInviteClickListener {
    void onAcceptClick(View view, InviteDetailsResponse invite);
    void onRejectClick(View view, InviteDetailsResponse invite);
}
