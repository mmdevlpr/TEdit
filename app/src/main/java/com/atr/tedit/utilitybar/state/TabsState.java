package com.atr.tedit.utilitybar.state;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.atr.tedit.R;
import com.atr.tedit.util.HelpDialog;
import com.atr.tedit.utilitybar.UtilityBar;

public class TabsState extends UtilityState {
    public TabsState(UtilityBar bar) {
        super(bar, UtilityBar.STATE_TAB);

        Button help = new Button(BAR.ctx);
        help.setBackgroundResource(R.drawable.button_help);
        help.setWidth(BAR.bWidth);
        help.setHeight(BAR.bHeight);
        help.setTranslationX(BAR.barWidth - BAR.bWidth - BAR.padding_w);
        help.setTranslationY(BAR.padding_h);
        help.setScaleX(1);
        help.setScaleY(1);
        help.setAlpha(1);
        help.setFocusable(true);
        help.setId(R.id.zero);
        help.setNextFocusRightId(R.id.one);
        help.setNextFocusLeftId(R.id.one);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(BAR.bar.getLayoutParams());
        lp.width = BAR.bWidth;
        lp.height = BAR.bHeight;
        help.setLayoutParams(lp);
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HelpDialog hd = HelpDialog.newInstance(R.layout.help_tabs, BAR.ctx.getString(R.string.tabs));
                hd.show(BAR.ctx.getSupportFragmentManager(), "HelpDialog");
            }
        });

        Button[] l = {help};
        LAYERS = new View[1][];
        LAYERS[0] = l;
    }
}
