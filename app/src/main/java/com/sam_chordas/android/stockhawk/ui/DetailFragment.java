package com.sam_chordas.android.stockhawk.ui;


import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteHistoricalColumn;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int CURSOR_LOADER_QUOTE = 1;
    private static final int CURSOR_LOADER_HISTORICAL_DATA = 2;
    static final String STOCK_SYMBOL = "SYMBOL";
    String symbol;

    @BindView(R.id.open) TextView open;
    @BindView(R.id.mkt_cap) TextView mkt_cap;
    @BindView(R.id.day_high) TextView day_high;
    @BindView(R.id.year_high) TextView year_high;
    @BindView(R.id.day_low) TextView day_low;
    @BindView(R.id.year_low) TextView year_low;
    @BindView(R.id.volume) TextView volume;
    @BindView(R.id.avg_vol) TextView avg_volume;
    @BindView(R.id.yield) TextView yield;
    @BindView(R.id.pneratio) TextView pneratio;
    @BindView(R.id.graph) LineChartView graph;

    public DetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;

        Bundle arguments = getArguments();

        if (arguments == null) {
            return null;
        }

        view = inflater.inflate(R.layout.fragment_detail, container, false);
        ButterKnife.bind(this, view);
        symbol = arguments.getString(DetailFragment.STOCK_SYMBOL);

        getActivity().setTitle(symbol);

        getLoaderManager().initLoader(CURSOR_LOADER_QUOTE, null, this);
        getLoaderManager().initLoader(CURSOR_LOADER_HISTORICAL_DATA, null, this);
        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        Log.d("APP", symbol);
        switch (id) {
            case CURSOR_LOADER_QUOTE:
                return new CursorLoader(getContext(), QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns._ID,
                                QuoteColumns.SYMBOL,
                                QuoteColumns.OPEN,
                                QuoteColumns.MKTCAP,
                                QuoteColumns.DAYSHIGH,
                                QuoteColumns.YEARHIGH,
                                QuoteColumns.DAYSLOW,
                                QuoteColumns.YEARLOW,
                                QuoteColumns.VOLUME,
                                QuoteColumns.AVGVOLUME,
                                QuoteColumns.YIELD,
                                QuoteColumns.PEGRATIO,
                                QuoteColumns.NAME },
                                QuoteColumns.SYMBOL + " = \"" + symbol + "\"", null, null);
            case CURSOR_LOADER_HISTORICAL_DATA:
                return new CursorLoader(getContext(), QuoteProvider.HistoricalData.CONTENT_URI,
                        new String[]{
                                QuoteHistoricalColumn._ID,
                                QuoteHistoricalColumn.SYMBOL,
                                QuoteHistoricalColumn.DATE,
                                QuoteHistoricalColumn.PRICE},
                        QuoteHistoricalColumn.SYMBOL + " = \"" + symbol + "\"",
                        null, QuoteHistoricalColumn._ID + " ASC");
                default:
                    throw new IllegalStateException();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        AxisValue axisValueX;

        if (!cursor.moveToFirst()) {
            return;
        }

        switch(loader.getId()) {
            case CURSOR_LOADER_QUOTE: {
                open.setText(String.valueOf(cursor.getString(2)));
                mkt_cap.setText(String.valueOf(cursor.getString(3)));
                day_high.setText(String.valueOf(cursor.getString(4)));
                year_high.setText(String.valueOf(cursor.getString(5)));
                day_low.setText(String.valueOf(cursor.getString(6)));
                year_low.setText(String.valueOf(cursor.getString(7)));
                volume.setText(String.valueOf(cursor.getString(8)));
                avg_volume.setText(String.valueOf(cursor.getString(9)));
                yield.setText(String.valueOf(cursor.getString(10)));
                pneratio.setText(String.valueOf(cursor.getString(11)));
            }
                break;

            case CURSOR_LOADER_HISTORICAL_DATA: {
                int i = 0;
                List<PointValue> values = new ArrayList<PointValue>();
                List<AxisValue> axisValuesX = new ArrayList<>();

                do {
                    String date = cursor.getString(2);
                    String close = cursor.getString(3);

                    int x = cursor.getCount() - 1 - i;
                    PointValue pointValue = new PointValue(i, Float.valueOf(close));
                    pointValue.setLabel(date);
                    values.add(pointValue);

                    //Set Date Label
                    if (i != 0 && i % (cursor.getCount() / 3) == 0) {
                        axisValueX = new AxisValue(x);
                        axisValueX.setLabel(date);
                        axisValuesX.add(axisValueX);
                    }

                    i++;
                } while (cursor.moveToNext());

                Line line = new Line(values).setColor(Color.WHITE).setCubic(true);
                List<Line> lines = new ArrayList<Line>();
                lines.add(line);

                LineChartData data = new LineChartData();
                data.setLines(lines);

                // Init x-axis
                Axis axisX = new Axis(axisValuesX);
                axisX.setHasLines(true);
                axisX.setMaxLabelChars(4);
                data.setAxisXBottom(axisX);

                // Init y-axis
                Axis axisY = new Axis();
                axisY.setAutoGenerated(true);
                axisY.setHasLines(true);
                axisY.setMaxLabelChars(4);
                data.setAxisYLeft(axisY);

                graph.setLineChartData(data);
                graph.setInteractive(true);
            }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
