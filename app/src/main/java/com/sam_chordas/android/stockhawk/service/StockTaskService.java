package com.sam_chordas.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteHistoricalColumn;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService(){}

    public StockTaskService(Context context){
        mContext = context;
    }

    String fetchData(String url) throws IOException{
        Request request = new Request.Builder()
            .url(url)
            .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params){
        Cursor initQueryCursor;
        ArrayList list = new ArrayList();

        if (mContext == null){
            mContext = this;
        }

        StringBuilder urlStringBuilder = new StringBuilder();
        try{
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
            + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (params.getTag().equals("init") || params.getTag().equals("periodic")){
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
            new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null){
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                    URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                    list.add( "YHOO" );
                    list.add( "AAPL" );
                    list.add( "GOOG" );
                    list.add( "MSFT" );
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null){
                String str = null;
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    str = initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"));
                    mStoredSymbols.append("\""+ initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) +"\",");
                    list.add(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")));
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")){
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
            } catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null){
            urlString = urlStringBuilder.toString();
            try{
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate){
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                        null, null);
                    }

                    ArrayList<ContentProviderOperation> cpo = Utils.quoteJsonToContentVals(getResponse);
                    if(cpo!=null && cpo.size() > 0)
                        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,cpo);
                    else
                        result = GcmNetworkManager.RESULT_FAILURE;

                    //mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                    //Utils.quoteJsonToContentVals(getResponse));

                    //Load Stocks History
                    String[] strArray = new String[ list.size() ];

                    for( int j = 0; j < strArray.length; j++ )
                        loadStockHistory(list.get( j ).toString());

                } catch (RemoteException | OperationApplicationException e){
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            } catch (IOException e){
                    e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Format to get Historical data for a symobol
     * http://query.yahooapis.com/v1/public/yql?q=
     * select * from   yahoo.finance.historicaldata
     * where  symbol    = "SYMBOL"
     * and    startDate = "yyyy-mm-dd"
     * and    endDate   = "yyy-mm-dd"
     * &format=json
     * &diagnostics=true
     * &env=store://datatables.org/alltableswithkeys
     * &callback=
     */
    private void loadStockHistory(String symbol) throws IOException {
        StringBuilder urlStringBuilder = new StringBuilder();
        String urlString = null;
        String getResponse = null;
        JSONObject jsonObject = null;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDate = new Date();

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(currentDate);
        calEnd.add(Calendar.DATE, 0);

        Calendar calStart = Calendar.getInstance();
        calStart.setTime(currentDate);
        calStart.add(Calendar.MONTH, -1);

        String startDate = dateFormat.format(calStart.getTime());
        String endDate = dateFormat.format(calEnd.getTime());

        urlStringBuilder.append("http://query.yahooapis.com/v1/public/yql?q= ");
        urlStringBuilder.append("select * from yahoo.finance.historicaldata where symbol=\"" +
                symbol +
                "\" and startDate=\"" + startDate + "\" and endDate=\"" + endDate + "\"");

        urlStringBuilder.append(" &format=json%20&diagnostics=true%20&env=store://datatables.org/alltableswithkeys%20&callback=");

        urlString = urlStringBuilder.toString();
        getResponse = fetchData(urlString);

        //parse and update db
        if (getResponse == null)
            return;

        //Empty Historic values
        mContext.getContentResolver().delete(QuoteProvider.HistoricalData.CONTENT_URI,
                QuoteHistoricalColumn.SYMBOL + " = \"" + symbol + "\"", null);

        try {
            jsonObject = new JSONObject(getResponse);
            jsonObject = jsonObject.getJSONObject("query");
            JSONArray history = jsonObject.getJSONObject("results").getJSONArray("quote");

            for (int i = 0; i < history.length(); i++ ) {
                ContentValues values = new ContentValues();

                values.put(QuoteHistoricalColumn.SYMBOL, history.getJSONObject(i).getString("Symbol"));
                values.put(QuoteHistoricalColumn.DATE, history.getJSONObject(i).getString("Date"));
                values.put(QuoteHistoricalColumn.PRICE, history.getJSONObject(i).getString("Close"));

                mContext.getContentResolver().insert(QuoteProvider.HistoricalData.CONTENT_URI, values);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
