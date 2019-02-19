package com.atr.tedit.utilitybar.state;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.atr.tedit.Browser;
import com.atr.tedit.R;
import com.atr.tedit.TEditActivity;
import com.atr.tedit.util.HelpDialog;
import com.atr.tedit.utilitybar.UtilityBar;

public class BrowserState extends UtilityState {
    public BrowserState(UtilityBar bar) {
        super(bar, UtilityBar.STATE_BROWSE);

        Button dir_parent = new Button(BAR.ctx);
        dir_parent.setBackgroundResource(R.drawable.button_dir_parent);
        dir_parent.setId(R.id.zero);
        dir_parent.setNextFocusRightId(R.id.one);
        dir_parent.setNextFocusLeftId(R.id.five);
        dir_parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.upDir();
            }
        });
        dir_parent.setEnabled(false);

        Button doc = new Button(BAR.ctx);
        doc.setBackgroundResource(R.drawable.button_doc);
        doc.setId(R.id.one);
        doc.setNextFocusRightId(R.id.two);
        doc.setNextFocusLeftId(R.id.zero);
        doc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.newDocument(TEditActivity.DEFAULTPATH, "");
            }
        });

        Button newdir = new Button(BAR.ctx);
        newdir.setBackgroundResource(R.drawable.button_dir);
        newdir.setId(R.id.two);
        newdir.setNextFocusRightId(R.id.three);
        newdir.setNextFocusLeftId(R.id.one);
        newdir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!(BAR.ctx.getFrag() instanceof Browser))
                    return;
                String bDir = ((Browser)BAR.ctx.getFrag()).getCurrentDir().getPath();
                Browser.NewDirectory newDir = Browser.NewDirectory.newInstance(bDir);
                newDir.show(BAR.ctx.getSupportFragmentManager(), "NewDirectory");
            }
        });

        Button tabs = new Button(BAR.ctx);
        tabs.setBackgroundResource(R.drawable.button_tabs);
        tabs.setId(R.id.three);
        tabs.setNextFocusRightId(R.id.four);
        tabs.setNextFocusLeftId(R.id.two);
        tabs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.tabs();
            }
        });

        Button help = new Button(BAR.ctx);
        help.setBackgroundResource(R.drawable.button_help);
        help.setId(R.id.four);
        help.setNextFocusRightId(R.id.five);
        help.setNextFocusLeftId(R.id.three);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HelpDialog hd = HelpDialog.newInstance(R.layout.help_browser, BAR.ctx.getString(R.string.browser));
                hd.show(BAR.ctx.getSupportFragmentManager(), "HelpDialog");
            }
        });

        Button[] l = {dir_parent, doc, newdir, tabs, help};
        int count = 0;
        for (Button v : l) {
            if (count == l.length - 1) {
                v.setTranslationX(BAR.barWidth - BAR.bWidth - BAR.padding_w);
            } else
                v.setTranslationX(BAR.padding_w + (count * (BAR.margin + bar.bWidth)));
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
