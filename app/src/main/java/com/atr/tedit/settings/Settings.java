package com.atr.tedit.settings;

import android.content.SharedPreferences;

import com.atr.tedit.BuildConfig;
import com.atr.tedit.TEditActivity;
import com.atr.tedit.file.AndPath;
import com.atr.tedit.file.FilePath;
import com.atr.tedit.util.FontUtil;

public class Settings {
    public static final int TEXTDIR_LTR = 0;
    public static final int TEXTDIR_RTL = 1;

    private static AndPath startupPath;

    private static boolean wordWrap = true;
    private static int systemTextDirection = TEXTDIR_LTR;
    private static int editorTextDirection = TEXTDIR_LTR;

    public static AndPath getStartupPath() {
        return startupPath.clone();
    }

    protected static void setStartupPath(AndPath path) {
        startupPath = path;
    }

    public static boolean isWordWrap() {
        return wordWrap;
    }

    protected static void setWordWrap(boolean wrap) {
        wordWrap = wrap;
    }

    public static int getSystemTextDirection() {
        return systemTextDirection;
    }

    protected static void setSystemTextDirection(int direction) {
        systemTextDirection = (direction > TEXTDIR_RTL) ? TEXTDIR_RTL : (direction < TEXTDIR_LTR) ? TEXTDIR_LTR : direction;
    }

    public static int getEditorTextDirection() {
        return editorTextDirection;
    }

    protected static void setEditorTextDirection(int direction) {
        editorTextDirection = (direction > TEXTDIR_RTL) ? TEXTDIR_RTL : (direction < TEXTDIR_LTR) ? TEXTDIR_LTR : direction;
    }

    public static void loadSettings(final TEditActivity ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getPackageName(), ctx.MODE_PRIVATE);

        try {
            startupPath = AndPath.fromJson(ctx, prefs.getString("startupPath", ""));
        } catch (Exception e) {
            startupPath = new FilePath(ctx.getStorageRoot());
        }

        wordWrap = prefs.getBoolean("wordWrap", true);
        systemTextDirection = prefs.getInt("systemTextDirection", TEXTDIR_LTR);
        editorTextDirection = prefs.getInt("editorTextDirection", TEXTDIR_LTR);

        FontUtil.setSystemTypeface(prefs.getString("systemTypeface", FontUtil.MONTSERRAT_ALT));
        FontUtil.setEditorTypeface(prefs.getString("editorTypeface", FontUtil.METROPOLIS));
    }

    public static void saveSettings(final TEditActivity ctx) {
        SharedPreferences.Editor prefs = ctx.getSharedPreferences(ctx.getPackageName(), ctx.MODE_PRIVATE).edit();

        prefs.putString("startupPath", startupPath.toJson());
        prefs.putBoolean("wordWrap", wordWrap);
        prefs.putInt("systemTextDirection", systemTextDirection);
        prefs.putInt("editorTextDirection", editorTextDirection);
        prefs.putString("systemTypeface", FontUtil.getSystemPath());
        prefs.putString("editorTypeface", FontUtil.getEditorPath());

        prefs.commit();
    }

    public static boolean isFirstRun(final TEditActivity ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(ctx.getPackageName(), ctx.MODE_PRIVATE);
        int lastVer = prefs.getInt("version", 0);
        return lastVer < BuildConfig.VERSION_CODE;
    }

    public static void saveVer(final TEditActivity ctx) {
        SharedPreferences.Editor prefs = ctx.getSharedPreferences(ctx.getPackageName(), ctx.MODE_PRIVATE).edit();
        prefs.putInt("version", BuildConfig.VERSION_CODE);
        prefs.commit();
    }
}
