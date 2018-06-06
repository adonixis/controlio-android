package ru.adonixis.controlio.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import java.util.ArrayList;

import ru.adonixis.controlio.R;
import ru.adonixis.controlio.adapter.EmailsAdapter;
import ru.adonixis.controlio.databinding.ActivityAddClientsBinding;

public class AddClientsActivity extends BaseSubmitFormActivity {

    private static final String TAG = "AddClientsActivity";
    private static final String CLIENTS_EMAILS = "clientsEmails";
    private ActivityAddClientsBinding mActivityAddClientsBinding;
    private ArrayList<String> clientsEmails = new ArrayList<>();
    private EmailsAdapter mAdapter;
    private LinearLayoutManager layoutManager;

    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            clientsEmails.remove(viewHolder.getAdapterPosition());
            mAdapter.notifyItemRemoved(viewHolder.getAdapterPosition());
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                Paint p = new Paint();
                Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                View itemView = viewHolder.itemView;
                float height = (float) itemView.getBottom() - (float) itemView.getTop();
                float width = (float) itemView.getRight() - (float) itemView.getLeft();

                if (dX < 0) {
                    p.setColor(Color.parseColor("#FE4128"));
                    RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(),(float) itemView.getRight(), (float) itemView.getBottom());
                    c.drawRect(background, p);
                    textPaint.setTextSize(50);
                    textPaint.setColor(Color.WHITE);
                    textPaint.setStyle(Paint.Style.STROKE);
                    c.drawText(getString(R.string.action_delete), (float) itemView.getRight() - 200, (float) itemView.getBottom() - (height / 2) + 15, textPaint);
                }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivityAddClientsBinding = DataBindingUtil.setContentView(this, R.layout.activity_add_clients);

        setSupportActionBar(mActivityAddClientsBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            clientsEmails = extras.getStringArrayList(CLIENTS_EMAILS);
        }

        layoutManager = new LinearLayoutManager(this);
        mActivityAddClientsBinding.recyclerClients.setLayoutManager(layoutManager);
        mActivityAddClientsBinding.recyclerClients.setHasFixedSize(false);
        mAdapter = new EmailsAdapter(clientsEmails);
        mActivityAddClientsBinding.recyclerClients.setAdapter(mAdapter);
        itemTouchHelper.attachToRecyclerView(mActivityAddClientsBinding.recyclerClients);

        mActivityAddClientsBinding.btnAddClientsEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validate()) {
                    return;
                }
                addClientsEmail();
            }
        });

        mActivityAddClientsBinding.inputClientsEmail.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    if (!validate()) {
                        return false;
                    }
                    addClientsEmail();
                    return true;
                }
                return false;
            }
        });

        mActivityAddClientsBinding.layoutInputClientsEmail.setError(getString(R.string.error_message_email_of_client));
    }

    private void addClientsEmail() {
        clientsEmails.add(0, mActivityAddClientsBinding.inputClientsEmail.getText().toString());
        mAdapter.notifyItemInserted(0);
        mActivityAddClientsBinding.recyclerClients.scrollToPosition(0);
        mActivityAddClientsBinding.inputClientsEmail.setText("");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                return true;
            case R.id.action_done:
                Intent intent = new Intent();
                intent.putStringArrayListExtra(CLIENTS_EMAILS, clientsEmails);
                setResult(RESULT_OK, intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean validate() {
        boolean valid = true;

        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        String clientsEmail = mActivityAddClientsBinding.inputClientsEmail.getText().toString();

        if (clientsEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(clientsEmail).matches()) {
            mActivityAddClientsBinding.layoutInputClientsEmail.startAnimation(shake);
            valid = false;
        }

        return valid;
    }
}
