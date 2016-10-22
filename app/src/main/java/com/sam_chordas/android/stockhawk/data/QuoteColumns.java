package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by sam_chordas on 10/5/15.
 */
public class QuoteColumns {
    @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
    public static final String _ID = "_id";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String SYMBOL = "symbol";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String PERCENT_CHANGE = "percent_change";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String CHANGE = "change";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String BIDPRICE = "bid_price";
    @DataType(DataType.Type.TEXT)
    public static final String CREATED = "created";
    @DataType(DataType.Type.INTEGER) @NotNull
    public static final String ISUP = "is_up";
    @DataType(DataType.Type.INTEGER) @NotNull
    public static final String ISCURRENT = "is_current";

    //Add new columns for Detail Activity
    @DataType(DataType.Type.TEXT)
    public static final String DAYSLOW = "days_low";
    @DataType(DataType.Type.TEXT)
    public static final String DAYSHIGH = "days_high";
    @DataType(DataType.Type.TEXT)
    public static final String YEARLOW = "year_low";
    @DataType(DataType.Type.TEXT)
    public static final String YEARHIGH = "year_high";
    @DataType(DataType.Type.TEXT)
    public static final String NAME = "name";
    @DataType(DataType.Type.TEXT)
    public static final String OPEN = "open";
    @DataType(DataType.Type.TEXT)
    public static final String PEGRATIO = "peg_ratio";
    @DataType(DataType.Type.TEXT)
    public static final String VOLUME = "volume";
    @DataType(DataType.Type.TEXT)
    public static final String YIELD = "yield";
    @DataType(DataType.Type.TEXT)
    public static final String MKTCAP = "market_cap";
    @DataType(DataType.Type.TEXT)
    public static final String AVGVOLUME = "average_volume";
    @DataType(DataType.Type.TEXT)
    public static final String YEARRANGE = "year_range";
}
