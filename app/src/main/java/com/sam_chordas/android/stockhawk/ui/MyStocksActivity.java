package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.utility.Utility;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyStocksActivity extends AppCompatActivity implements MyStockFragment.Callback {
    private boolean mTwoPane;
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private CharSequence mTitle;
    boolean isConnected;

    @Nullable
    @BindView(R.id.stock_detail_container) FrameLayout layout;

    private void startStockService() {
        Intent mServiceIntent = new Intent(this, StockIntentService.class);

        // Run the initialize task service so that some stocks appear upon an empty database
        mServiceIntent.putExtra("tag", "init");
        if (isConnected){
            startService(mServiceIntent);
        } else {
            networkToast();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isConnected = Utility.isConnected(this);
        setContentView(R.layout.activity_my_stocks);

        ButterKnife.bind(this);

        if (savedInstanceState == null){
            startStockService();
        }

        if (layout != null) {
            mTwoPane = true;

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.stock_detail_container, new DetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }

        mTitle = getTitle();
        if (isConnected){
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = new PeriodicTask.Builder()
                .setService(StockTaskService.class)
                .setPeriod(period)
                .setFlex(flex)
                .setTag(periodicTag)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .build();
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }
    }

    public void networkToast(){
        Toast.makeText(this, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_units:
                Utils.showPercent = !Utils.showPercent;

                if (Utils.showPercent)
                    item.setIcon(R.drawable.ic_attach_money_white_24dp);
                else
                    item.setIcon(R.drawable.percentage);
                this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(String symbol) {
        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putString(DetailFragment.STOCK_SYMBOL, symbol);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.stock_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(DetailFragment.STOCK_SYMBOL, symbol);
            startActivity(intent);
        }
    }
}
