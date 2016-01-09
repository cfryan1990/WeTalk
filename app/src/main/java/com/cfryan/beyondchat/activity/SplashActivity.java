package com.cfryan.beyondchat.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.cfryan.beyondchat.R;
import com.cfryan.beyondchat.util.PreferenceConstants;
import com.cfryan.beyondchat.util.PreferenceUtils;


public class SplashActivity extends Activity {

    Runnable gotoLoginAct = new Runnable() {

        @Override
        public void run() {

            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        }
    };
    Runnable gotoMainAct = new Runnable() {

        @Override
        public void run() {
            startActivity(new Intent(SplashActivity.this, MainTabActivty.class));
            overridePendingTransition(android.support.v7.appcompat.R.anim.abc_grow_fade_in_from_bottom,
                    android.support.v7.appcompat.R.anim.abc_shrink_fade_out_from_bottom);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Handler mHandler = new Handler();
        String password = PreferenceUtils.getPrefString(this,
                PreferenceConstants.PASSWORD, "");
        if (!TextUtils.isEmpty(password)) {

            mHandler.postDelayed(gotoMainAct, 2000);

        } else {
            mHandler.postDelayed(gotoLoginAct, 1000);

        }


    }
}
