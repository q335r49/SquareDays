package com.q335.r49.squaredays;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.res.ResourcesCompat;
import android.util.DisplayMetrics;
import android.util.Log;

class Glob {
    static PaletteRing palette;
    private static int PALLETTE_LENGTH = 24;
    static int COLOR_SCALE_TEXT;
    static int COLOR_GRID_BACKGROUND;
    static int COLOR_NOW_LINE;
    static int COLOR_STATUS_BAR;
    static int COLOR_SELECTION;
    static int COLOR_END_BOX;
    static int COLOR_ERROR;
    static int COLOR_OVERFLOW;
    static int COLOR_PRIMARY_DARK;
    static int COLOR_EXP_GRID;
    static Typeface CommandFont;
    static float rPxDp;

    static void init(Context context) {
        palette = new PaletteRing(PALLETTE_LENGTH);
        palette.add(new String[]
               {"#1abc9c", "#2ecc71", "#3498db", "#9b59b6", "#34495e", "#16a085"," #27ae60", "#2980b9",
                "#8e44ad", "#2c3e50", "#f1c40f", "#e67e22", "#e74c3c", "#ecf0f1", "#95a5a6", "#f39c12",
                "#d35400", "#c0392b", "#bdc3c7", "#7f8c8d", "#3b5999", "#21759b", "#dd4b39", "#bd081c"});
        Resources res = context.getResources();
        COLOR_SCALE_TEXT      = ResourcesCompat.getColor(res, R.color.scale_text, null);
        COLOR_GRID_BACKGROUND = ResourcesCompat.getColor(res, R.color.grid_background, null);
        COLOR_NOW_LINE        = ResourcesCompat.getColor(res, R.color.now_line, null);
        COLOR_STATUS_BAR      = ResourcesCompat.getColor(res, R.color.status_bar, null);
        COLOR_SELECTION       = ResourcesCompat.getColor(res, R.color.selection, null);
        COLOR_END_BOX         = ResourcesCompat.getColor(res, R.color.end_box, null);
        COLOR_ERROR           = ResourcesCompat.getColor(res, R.color.error, null);
        COLOR_OVERFLOW        = ResourcesCompat.getColor(res, R.color.overflow, null);
        COLOR_PRIMARY_DARK    = ResourcesCompat.getColor(res, R.color.colorPrimaryDark, null);
        COLOR_EXP_GRID        = ResourcesCompat.getColor(res, R.color.exp_grid_background, null);
        CommandFont           = Typeface.createFromAsset(context.getAssets(),  "fonts/22203___.TTF");
        rPxDp                 = 0.75f * context.getResources().getDisplayMetrics().density;
    }
    static int darkenColor(int color, float factor) {
        return Color.argb(Color.alpha(color),
                Math.min(Math.round(Color.red(color) * factor),255),
                Math.min(Math.round(Color.green(color) * factor),255),
                Math.min(Math.round(Color.blue(color) * factor),255));
    }
    static int parseColor(String s) {
        try { return Color.parseColor(s);
        } catch (Exception e) {
            Log.d("SquareDays","Bad color: " + s);
            return Glob.COLOR_ERROR;
        }
    }
}