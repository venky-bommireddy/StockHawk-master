package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * Created by sam_chordas on 10/5/15.
 * Modified by dyadav1
 */
@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
    private QuoteDatabase(){}

    public static final int VERSION = 8;

    @Table(QuoteColumns.class) public static final String QUOTES = "quotes";
    @Table(QuoteHistoricalColumn.class) public static final String HISTORICAL_DATA = "historical_data";
}
