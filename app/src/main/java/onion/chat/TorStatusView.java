/*
 * Chat.onion - P2P Instant Messenger
 *
 * http://play.google.com/store/apps/details?id=onion.chat
 * http://onionapps.github.io/Chat.onion/
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.chat;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TorStatusView extends LinearLayout {

    public TorStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void update() {
        Tor tor = Tor.getInstance(getContext());

        setVisibility(!tor.isReady() ? View.VISIBLE : View.GONE);

        String status = tor.getStatus();
        int i = status.indexOf(']');
        if (i >= 0) status = status.substring(i + 1);
        status = status.trim();

        TextView view = (TextView) findViewById(R.id.status);

        String prefix = "Bootstrapped";
        if (status.contains("%") && status.length() > prefix.length() && status.startsWith(prefix)) {
            status = status.substring(prefix.length());
            status = status.trim();
            view.setText(status);
        } else if (view.length() == 0) {
            view.setText("Starting...");
        }
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();

        if (!isInEditMode()) {
            Tor tor = Tor.getInstance(getContext());
            tor.setLogListener(new Tor.LogListener() {
                @Override
                public void onLog() {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            update();
                        }
                    });
                }
            });
            update();
        }

    }

    @Override
    protected void onDetachedFromWindow() {

        Tor tor = Tor.getInstance(getContext());
        tor.setLogListener(null);

        if (!isInEditMode()) {
            super.onDetachedFromWindow();
        }

    }
}
