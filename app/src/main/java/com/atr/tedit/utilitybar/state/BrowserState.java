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
package com.atr.tedit.utilitybar.state;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.atr.tedit.mainstate.Browser;
import com.atr.tedit.R;
import com.atr.tedit.TEditActivity;
import com.atr.tedit.dialog.HelpDialog;
import com.atr.tedit.utilitybar.UtilityBar;

public class BrowserState extends UtilityState {
    public BrowserState(UtilityBar bar) {
        super(bar, UtilityBar.STATE_BROWSE);

        Button dir_parent = new Button(BAR.ctx);
        dir_parent.setBackgroundResource(R.drawable.button_dir_parent);
        dir_parent.setId(R.id.zero);
        dir_parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((Browser)BAR.ctx.getFrag()).upDir();
            }
        });
        dir_parent.setEnabled(false);

        Button doc = new Button(BAR.ctx);
        doc.setBackgroundResource(R.drawable.button_doc);
        doc.setId(R.id.one);
        doc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.newDocument(TEditActivity.DEFAULTPATH, "");
            }
        });

        Button newdir = new Button(BAR.ctx);
        newdir.setBackgroundResource(R.drawable.button_dir_new);
        newdir.setId(R.id.two);
        newdir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(BAR.ctx.getFrag() instanceof Browser))
                    return;

                if (((Browser) BAR.ctx.getFrag()).isBrowsingPermittedDirs()) {
                    BAR.ctx.launchDirPermissionIntent();
                    return;
                }
                String bDir = ((Browser)BAR.ctx.getFrag()).getCurrentPath().toJson();
                Browser.NewDirectory newDir = Browser.NewDirectory.newInstance(bDir);
                newDir.show(BAR.ctx.getSupportFragmentManager(), "NewDirectory");
            }
        });

        Button tabs = new Button(BAR.ctx);
        tabs.setBackgroundResource(R.drawable.button_tabs);
        tabs.setId(R.id.three);
        tabs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.tabs();
            }
        });

        Button help = new Button(BAR.ctx);
        help.setBackgroundResource(R.drawable.button_help);
        help.setId(R.id.four);

        Button[] l;

        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HelpDialog hd = HelpDialog.newInstance(R.layout.help_browser, BAR.ctx.getString(R.string.browser));
                hd.show(BAR.ctx.getSupportFragmentManager(), "HelpDialog");
            }
        });

        l = new Button[]{dir_parent, doc, newdir, tabs, help};

        int count = 0;
        for (Button v : l) {
            if (count == l.length - 1) {
                v.setTranslationX(BAR.barWidth - BAR.bWidth - BAR.padding_w);
                v.setNextFocusRightId(l[0].getId());
                v.setNextFocusLeftId(l[count - 1].getId());
            } else {
                v.setTranslationX(BAR.padding_w + (count * (BAR.margin + bar.bWidth)));
                v.setNextFocusRightId(l[count + 1].getId());
                if (count == 0)
                    v.setNextFocusLeftId(l[l.length - 1].getId());
            }
            v.setTranslationY(BAR.padding_h);

            v.setFocusable(true);
            v.setWidth(BAR.bWidth);
            v.setHeight(BAR.bHeight);
            v.setScaleX(1);
            v.setScaleY(1);
            v.setAlpha(1);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(BAR.bar.getLayoutParams());
            lp.width = BAR.bWidth;
            lp.height = BAR.bHeight;
            v.setLayoutParams(lp);

            count++;
        }

        LAYERS = new View[1][];
        LAYERS[0] = l;
    }
}
