package com.sam_chordas.android.stockhawk.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.sam_chordas.android.stockhawk.utility.Utility;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyStockFragment extends Fragment implements  LoaderManager.LoaderCallbacks<Cursor> {

    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Cursor mCursor;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATUS_OK, STATUS_SERVER_DOWN, STATUS_SERVER_INVALID,  STATUS_UNKNOWN})
    public @interface LocationStatus {}

    public static final int STATUS_OK = 0;
    public static final int STATUS_SERVER_DOWN = 1;
    public static final int STATUS_SERVER_INVALID = 2;
    public static final int STATUS_UNKNOWN = 3;

    @BindView(R.id.recycler_view) RecyclerView rcView;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.view_stock_empty) TextView tv;

    public MyStockFragment() {}

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(String stockId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_stocks, container, false);

        ButterKnife.bind(this, view);

        mCallbacks = this;
        rcView.setLayoutManager(new LinearLayoutManager(getContext()));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(getContext(), null);
        rcView.addOnItemTouchListener(new RecyclerViewItemClickListener(getContext(),
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View v, int position) {
                        mCursor = mCursorAdapter.getCursor();
                        mCursor.moveToPosition(position);
                        if (mCursor != null) {
                            ((Callback) getActivity())
                                    .onItemSelected(mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL)));
                        }
                    }
                }));
        rcView.setAdapter(mCursorAdapter);

        fab.attachToRecyclerView(rcView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                    new MaterialDialog.Builder(getContext()).title(R.string.symbol_search)
                            .content(R.string.content_test)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override public void onInput(MaterialDialog dialog, CharSequence input) {
                                    // On FAB click, receive user input. Make sure the stock doesn't already exist
                                    // in the DB and proceed accordingly
                                    Cursor c = getContext().getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[] { QuoteColumns.SYMBOL }, QuoteColumns.SYMBOL + "= ?",
                                            new String[] { input.toString() }, null);
                                    if (c.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(getContext(), R.string.stock_duplicate_msg,
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                    } else {
                                        // Add the stock to DB
                                        Intent mServiceIntent = new Intent(getActivity(), StockIntentService.class);
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", input.toString());
                                        getActivity().startService(mServiceIntent);
                                    }
                                }
                            })
                            .show();
            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(rcView);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, mCallbacks);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        return new CursorLoader(getContext(), QuoteProvider.Quotes.CONTENT_URI,
                new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        mCursorAdapter.swapCursor(data);
        updateEmptyView();
        mCursor = data;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mCursorAdapter.swapCursor(null);
    }

    private void updateEmptyView() {
        if ( mCursorAdapter.getItemCount() == 0 ) {
            if ( null != tv ) {
                // if cursor is empty, why? do we have an invalid location
                int message = R.string.empty_stock_list;
                if (!Utility.isConnected(getActivity()) ) {
                    message = R.string.empty_stock_list_no_network;
                }
                tv.setText(message);
            }
        } else {
            tv.setVisibility(View.INVISIBLE);
        }
    }
}
