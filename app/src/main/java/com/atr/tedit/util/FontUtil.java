/*
 * Free Public License 1.0.0
 * Permission to use, copy, modify, and/or distribute this software
 * for any purpose with or without fee is hereby granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 * WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package com.atr.tedit.util;

import android.content.Context;
import android.graphics.Typeface;
import android.provider.FontsContract;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.TypefaceCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.atr.tedit.R;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;

public class FontUtil {
    public static final String SYSTEM_FONT_DIR = "/system/fonts";

    public static final String METROPOLIS = "metropolis";
    public static final String MONTSERRAT_ALT = "montserrat_alt";
    public static final String BEBEDERA = "bebedera";

    private static HashMap<String, SoftReference<Typeface>> cache = new HashMap<String, SoftReference<Typeface>>();

    private static Typeface metropolis;
    private static Typeface montserrat_alt;
    private static Typeface bebedera;

    private static Typeface system;
    private static Typeface editor;
    private static String systemPath = "montserrat_alt";
    private static String editorPath = "metropolis";

    public static void init(Context ctx) {
        metropolis = ResourcesCompat.getFont(ctx, R.font.metropolis_regular);
        montserrat_alt = ResourcesCompat.getFont(ctx, R.font.montserratalternates_regular);
        bebedera = ResourcesCompat.getFont(ctx, R.font.bebedera);

        system = montserrat_alt;
        editor = metropolis;
    }

    public static void applyFont(Typeface font, View view, View... exclude) {
        for (View v : exclude) {
            if (v.equals(view))
                return;
        }

        if (view instanceof TextView) {
            ((TextView)view).setTypeface(font);
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup)view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyFont(font, vg.getChildAt(i), exclude);
            }
        }
    }

    public static Typeface getMetropolis() {
        return metropolis;
    }

    public static Typeface getMontserratAlt() {
        return montserrat_alt;
    }

    public static Typeface getBebedera() {
        return bebedera;
    }

    public static String[] getSystemFonts() {
        File fontDir = new File(SYSTEM_FONT_DIR);
        if (!fontDir.exists())
            return new String[0];

        return fontDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".ttf") || s.endsWith(".otf");
            }
        });
    }

    public static void setSystemTypeface(String path) {
        Typeface tf = getTypefaceFromPath(path, getSystemTypeface());
        if (tf.equals(getSystemTypeface()))
            return;

        system = tf;
        systemPath = path;
    }

    public static Typeface getDefault() {
        return system;
    }

    public static Typeface getSystemTypeface() {
        return system;
    }

    public static void setEditorTypeface(String path) {
        Typeface tf = getTypefaceFromPath(path, getEditorTypeface());
        if (tf.equals(getEditorTypeface()))
            return;

        editor = tf;
        editorPath = path;
    }

    public static Typeface getEditorTypeface() {
        return editor;
    }

    public static Typeface getTitleTypeface() { return bebedera; }

    public static String getEditorPath() {
        if (editor.equals(metropolis)) {
            return METROPOLIS;
        } else if (editor.equals(montserrat_alt)) {
            return MONTSERRAT_ALT;
        } else if (editor.equals(bebedera)) {
            return BEBEDERA;
        } else
            return editorPath;
    }

    public static String getSystemPath() {
        if (system.equals(metropolis)) {
            return METROPOLIS;
        } else if (system.equals(montserrat_alt)) {
            return MONTSERRAT_ALT;
        } else if (system.equals(bebedera)) {
            return BEBEDERA;
        } else
            return systemPath;
    }

    public static Typeface getTypefaceFromPath(String path, Typeface defaultTypeface) {
        switch(path) {
            case METROPOLIS:
                return metropolis;
            case MONTSERRAT_ALT:
                return montserrat_alt;
            case BEBEDERA:
                return bebedera;
            case "":
                return defaultTypeface;
            default:
                File file = new File(path);
                if (!file.exists() || !file.canRead()) {
                    Log.w("TEdit Font", "The specified font could not be read: " + path);
                    return defaultTypeface;
                }
                SoftReference<Typeface> sr = cache.get(path);
                if (sr == null) {
                    Typeface tf = Typeface.createFromFile(file);
                    if (tf == null) {
                        Log.w("TEdit Font", "The specified font could not be read: " + path);
                        return defaultTypeface;
                    }
                    cache.put(path, new SoftReference(tf));

                    return tf;
                }

                Typeface tf = sr.get();
                if (tf == null) {
                    tf = Typeface.createFromFile(file);
                    if (tf == null) {
                        Log.w("TEdit Font", "The specified font could not be read: " + path);
                        return defaultTypeface;
                    }
                    cache.put(path, new SoftReference<>(tf));

                    return tf;
                }

                return tf;
        }
    }

    public static String getTypefaceName(String path) {
        switch(path) {
            case METROPOLIS:
                return "Metropolis";
            case MONTSERRAT_ALT:
                return "Montserrat Alternatives";
            case BEBEDERA:
                return "Bebedera";
            default:
                int slsh = path.lastIndexOf("/");
                return (slsh >= 0 && slsh < path.length() - 1) ? path.substring(slsh + 1, path.length()) : path;
        }
    }
}
