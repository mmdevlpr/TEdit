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
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.atr.tedit.R;

public class FontUtil {
    public static Typeface metropolis;
    public static Typeface montserrat_alt;
    public static Typeface bebedera;

    public static void init(Context ctx) {
        metropolis = ResourcesCompat.getFont(ctx, R.font.metropolis_regular);
        montserrat_alt = ResourcesCompat.getFont(ctx, R.font.montserratalternates_regular);
        bebedera = ResourcesCompat.getFont(ctx, R.font.bebedera);
    }

    public static void applyFont(Typeface font, View view) {
        if (view instanceof TextView) {
            ((TextView)view).setTypeface(font);
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup)view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyFont(font, vg.getChildAt(i));
            }
        }
    }

    public static Typeface getDefault() {
        return montserrat_alt;
    }

    public static Typeface getEditorTypeface() {
        return metropolis;
    }

    public static Typeface getTitleTypeface() { return bebedera; }
}
