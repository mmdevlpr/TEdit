package com.atr.tedit.utilitybar.state;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.atr.tedit.R;
import com.atr.tedit.TEditActivity;
import com.atr.tedit.util.HelpDialog;
import com.atr.tedit.utilitybar.UtilityBar;

public class TextState extends UtilityState {
    public TextState(UtilityBar bar) {
        super(bar, UtilityBar.STATE_TEXT);

        Button newdoc = new Button(BAR.ctx);
        newdoc.setBackgroundResource(R.drawable.button_doc);
        newdoc.setId(R.id.zero);
        newdoc.setNextFocusRightId(R.id.one);
        newdoc.setNextFocusLeftId(R.id.six);
        newdoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.newDocument(TEditActivity.DEFAULTPATH, "");
            }
        });

        Button opendoc = new Button(BAR.ctx);
        opendoc.setBackgroundResource(R.drawable.button_dir);
        opendoc.setId(R.id.one);
        opendoc.setNextFocusRightId(R.id.two);
        opendoc.setNextFocusLeftId(R.id.zero);
        opendoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.requestOpenBrowser();
            }
        });

        Button savedoc = new Button(BAR.ctx);
        savedoc.setBackgroundResource(R.drawable.button_save);
        savedoc.setId(R.id.two);
        savedoc.setNextFocusRightId(R.id.three);
        savedoc.setNextFocusLeftId(R.id.one);
        savedoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.saveDocument(false);
            }
        });

        Button savedocas = new Button(BAR.ctx);
        savedocas.setBackgroundResource(R.drawable.button_save_as);
        savedocas.setId(R.id.three);
        savedocas.setNextFocusRightId(R.id.four);
        savedocas.setNextFocusLeftId(R.id.two);
        savedocas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.saveAsDocument(false);
            }
        });

        Button tabs = new Button(BAR.ctx);
        tabs.setBackgroundResource(R.drawable.button_tabs);
        tabs.setId(R.id.four);
        tabs.setNextFocusRightId(R.id.five);
        tabs.setNextFocusLeftId(R.id.three);
        tabs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BAR.ctx.tabs();
            }
        });

        Button help = new Button(BAR.ctx);
        help.setBackgroundResource(R.drawable.button_help);
        help.setId(R.id.five);
        help.setNextFocusRightId(R.id.six);
        help.setNextFocusLeftId(R.id.four);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HelpDialog hd = HelpDialog.newInstance(R.layout.help_editor, BAR.ctx.getString(R.string.editor));
                hd.show(BAR.ctx.getSupportFragmentManager(), "HelpDialog");
            }
        });

        Button[] l = {newdoc, opendoc, savedoc, savedocas, tabs, help};
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
