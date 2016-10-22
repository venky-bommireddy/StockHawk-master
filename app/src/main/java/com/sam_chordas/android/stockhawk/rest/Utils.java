package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON){
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject;
        JSONArray resultsArray;
        try{
            jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0){
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1){
                    jsonObject = jsonObject.getJSONObject("results").getJSONObject("quote");
                    ContentProviderOperation obj =  buildBatchOperation(jsonObject);
                    if(obj!=null)
                        batchOperations.add(obj);
                } else{
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0){
                        for (int i = 0; i < resultsArray.length(); i++){
                            jsonObject = resultsArray.getJSONObject(i);
                            if(jsonObject!=null)
                                batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    }
                 }
            }
        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice){
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange){
        String weight = change.substring(0,1);
        String ampersand = "";
        if (isPercentChange){
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
        String change = null;

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
        try {
            change = jsonObject.getString("Change");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (change != null && change != "null") {
            try {
                builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
                builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
                builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(jsonObject.getString("ChangeinPercent"), true));
                builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
                builder.withValue(QuoteColumns.ISCURRENT, 1);
                if (change.charAt(0) == '-') {
                    builder.withValue(QuoteColumns.ISUP, 0);
                } else {
                    builder.withValue(QuoteColumns.ISUP, 1);
                }
                builder.withValue(QuoteColumns.DAYSHIGH, jsonObject.getString("DaysHigh"));
                builder.withValue(QuoteColumns.DAYSLOW, jsonObject.getString("DaysLow"));
                builder.withValue(QuoteColumns.YEARHIGH, jsonObject.getString("YearHigh"));
                builder.withValue(QuoteColumns.YEARLOW, jsonObject.getString("YearLow"));
                builder.withValue(QuoteColumns.YEARRANGE, jsonObject.getString("YearRange"));
                builder.withValue(QuoteColumns.VOLUME, jsonObject.getString("Volume"));
                builder.withValue(QuoteColumns.AVGVOLUME, jsonObject.getString("AverageDailyVolume"));
                builder.withValue(QuoteColumns.MKTCAP, jsonObject.getString("MarketCapitalization"));
                builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
                //         builder.withValue(QuoteColumns.YIELD, jsonObject.getString("DividendYield"));
                builder.withValue(QuoteColumns.PEGRATIO, jsonObject.getString("PEGRatio"));
                builder.withValue(QuoteColumns.OPEN, jsonObject.getString("Open"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return builder.build();
        }
        return null;
    }
}
