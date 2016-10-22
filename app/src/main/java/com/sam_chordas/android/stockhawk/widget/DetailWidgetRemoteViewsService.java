package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * Created by dyadav1 on 9/13/2016.
 */
public class DetailWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                                QuoteColumns.ISCURRENT + " = ?",
                                new String[]{"1"},
                                null);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);

                if (data.moveToPosition(position)) {
                    views.setTextViewText(R.id.stock_symbol, data.getString(1));
                    views.setTextViewText(R.id.bid_price, data.getString(2));
                    views.setTextViewText(R.id.change, data.getString(4));

                    final Intent fillInIntent = new Intent();
                    fillInIntent.putExtra("SYMBOL", data.getString(1));
                    views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                }

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return null;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return  position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
