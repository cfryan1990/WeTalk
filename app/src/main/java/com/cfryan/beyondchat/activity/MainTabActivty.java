package com.cfryan.beyondchat.activity;

import com.cfryan.beyondchat.R;
//import com.cfryan.wanglai4android.application.XXBroadcastReceiver;
import com.cfryan.beyondchat.fragment.ContactFragment;
//import com.cfryan.wanglai4android.fragment.RecentChatFragment;

import com.cfryan.beyondchat.service.CoreService;
import com.cfryan.beyondchat.service.IConnectionStatusCallback;
//import com.cfryan.wanglai4android.util.ActivityManager;
import com.cfryan.beyondchat.util.L;
import com.cfryan.beyondchat.util.PreferenceConstants;
import com.cfryan.beyondchat.util.PreferenceUtils;
import com.cfryan.beyondchat.util.T;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainTabActivty extends Activity implements IConnectionStatusCallback {

    private String linkStatus = "";
    private int mCurrentIndex;
    private CoreService mService;

    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private RadioGroup radioGroup;
    private TextView mTitle;
    private ImageView mLeftBtn;
    private ProgressBar mTitleProgressBar;

    private static final String STATUS_BAR_HEIGHT_RES_NAME = "status_bar_height";

    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((CoreService.XXBinder) service).getService();
            mService.registerConnectionStatusCallback(MainTabActivty.this);
            // 开始连接xmpp服务器
            if (!mService.isAuthenticated()) {
                String usr = PreferenceUtils.getPrefString(MainTabActivty.this,
                        PreferenceConstants.ACCOUNT, "");
                String password = PreferenceUtils.getPrefString(
                        MainTabActivty.this, PreferenceConstants.PASSWORD, "");
                mService.Login(usr, password);
                //       mService.setStatusFromConfig();

            } else {
                /*
                 * mTitleNameView.setText(XMPPHelper
				 * .splitJidAndServer(PreferenceUtils.getPrefString(
				 * MainActivity.this, PreferenceConstants.ACCOUNT, "")));
				 * setStatusImage(true);
				 */
//                mService.setStatusFromConfig();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService.unRegisterConnectionStatusCallback();
            mService = null;
        }

    };


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor()
    {
        getWindow().setStatusBarColor(getResources().getColor(R.color.ui_green));
    }

    private int getInternalDimensionSize(Resources res, String key) {
        int result = 0;
        int resourceId = res.getIdentifier(key, "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(MainTabActivty.this, CoreService.class));
        setContentView(R.layout.activity_main_tab);

        int mStatusBarHeight = getInternalDimensionSize(getResources(), STATUS_BAR_HEIGHT_RES_NAME);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            setStatusBarColor();
        }
        else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            View StatusBar = (View) findViewById(R.id.ui_status_bar);
            LinearLayout.LayoutParams barLP
                    = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, mStatusBarHeight);
            StatusBar.setLayoutParams(barLP);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mTitle = (TextView) findViewById(R.id.ui_title_bar_txt);
        mLeftBtn = (ImageView) findViewById(R.id.ui_title_bar_back_btn);
        mLeftBtn.setVisibility(View.GONE);

        mTitleProgressBar = (ProgressBar) findViewById(R.id.ivTitleProgress);

        fragmentManager = getFragmentManager();
        radioGroup = (RadioGroup) findViewById(R.id.rg_tab);
        ((RadioButton) radioGroup.findViewById(R.id.RB_0)).setChecked(true);

        transaction = fragmentManager.beginTransaction();

        Fragment fragment = new ContactFragment();
        transaction.replace(R.id.content, fragment);
        transaction.commit();

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // 此处chenckID为radiogroup中radioButton的R.id
                changeFragmentByIndex(checkedId);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindXMPPService();
//        getContentResolver().registerContentObserver(
//                RosterProvider.CONTENT_URI, true, mRosterObserver);
//        setStatusImage(isConnected());
//        // if (!isConnected())
//        // mTitleNameView.setText(R.string.login_prompt_no);
//        mRosterAdapter.requery();
//        XXBroadcastReceiver.mListeners.add(this);
//        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE)
//            mNetErrorView.setVisibility(View.VISIBLE);
//        else
//            mNetErrorView.setVisibility(View.GONE);
//        ChangeLog cl = new ChangeLog(this);
//        if (cl != null && cl.firstRun()) {
//            cl.getFullLogDialog().show();
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //    getContentResolver().unregisterContentObserver(mRosterObserver);
        unbindXMPPService();
//        XXBroadcastReceiver.mListeners.remove(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void unbindXMPPService() {
        try {
            unbindService(mServiceConnection);
            L.i(LoginActivity.class, "[SERVICE] Unbind");
        } catch (IllegalArgumentException e) {
            L.e(LoginActivity.class, "Service wasn't bound!");
        }
    }

    private void bindXMPPService() {
        L.i(LoginActivity.class, "[SERVICE] bind");
        bindService(new Intent(MainTabActivty.this, CoreService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE
                        + Context.BIND_DEBUG_UNBIND);
    }

    private void changeFragmentByIndex(int index) {

        switch (index) {
            case R.id.RB_0:
                transaction = fragmentManager.beginTransaction();
                Fragment fragment1 = new ContactFragment();
                transaction.replace(R.id.content, fragment1);
                transaction.commit();
                mTitle.setText(linkStatus.endsWith("") ? getString(R.string.main_tab_message) : linkStatus);
                mCurrentIndex = 0;
                break;

            case R.id.RB_1:
                transaction = fragmentManager.beginTransaction();
                Fragment fragment2 = new ContactFragment();
                transaction.replace(R.id.content, fragment2);
                transaction.commit();
                mTitle.setText(getString(R.string.main_tab_contact));
                mCurrentIndex = 1;
                break;
            case R.id.RB_2:
                transaction = fragmentManager.beginTransaction();
                Fragment fragment3 = new ContactFragment();
                transaction.replace(R.id.content, fragment3);
                transaction.commit();
                mTitle.setText(getString(R.string.main_tab_activity));
                mCurrentIndex = 2;
                break;
            case R.id.RB_3:
                transaction = fragmentManager.beginTransaction();
                Fragment fragment4 = new ContactFragment();
                transaction.replace(R.id.content, fragment4);
                transaction.commit();
                mTitle.setText(getString(R.string.main_tab_setting));
                mCurrentIndex = 3;
                break;
        }
    }


    @Override
    public void connectionStatusChanged(int connectedState, String reason) {
        switch (connectedState) {
            case CoreService.CONNECTED:
                linkStatus = "";
                // mTitleNameView.setText(XMPPHelper.splitJidAndServer(PreferenceUtils
                // .getPrefString(MainActivity.this,
                // PreferenceConstants.ACCOUNT, "")));
                // setStatusImage(true);
                if (mCurrentIndex == 0)
                {
                    mTitle.setText(getString(R.string.main_tab_message));
                }
                mTitleProgressBar.setVisibility(View.GONE);

                break;
            case CoreService.CONNECTING:
                linkStatus = getString(R.string.connect_prompt_connecting);
                if (mCurrentIndex == 0) {
                    mTitle.setText(linkStatus);
                    mTitleProgressBar.setVisibility(View.VISIBLE);
                }

                break;
            case CoreService.DISCONNECTED:
                linkStatus = getString(R.string.connect_prompt_no);
                if (mCurrentIndex == 0 ) {
                    mTitle.setText(linkStatus);
                    mTitleProgressBar.setVisibility(View.GONE);
                }

                T.showLong(this, reason);
                break;

            default:
                break;
        }

    }


//	@Override
//	public XXService getService() {
//		// TODO Auto-generated method stub
//		return this.mService;
//	}


	public MainTabActivty getMainActivity() {
		// TODO Auto-generated method stub
		return null;
	}

}