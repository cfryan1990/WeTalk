package cfryan.wetalk.service;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.Toast;

//import com.cfryan.wanglai4android.application.XXBroadcastReceiver;
//import com.cfryan.wanglai4android.application.XXBroadcastReceiver.EventHandler;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import cfryan.wetalk.R;
import cfryan.wetalk.activity.LoginActivity;
import cfryan.wetalk.smack.SmackImpl;
import cfryan.wetalk.util.L;
import cfryan.wetalk.util.NetUtil;
import cfryan.wetalk.util.PreferenceConstants;
import cfryan.wetalk.util.PreferenceUtils;

//import com.cfryan.wanglai4android.exception.XXMPException;

public class CoreService extends BaseService
//        implements EventHandler//, BackPressHandler
{
    public static final int CONNECTED = 0;
    public static final int DISCONNECTED = -1;
    public static final int CONNECTING = 1;
    public static final String PONG_TIMEOUT = "pong timeout";// 连接超时
    public static final String NETWORK_ERROR = "network error";// 网络错误
    public static final String LOGOUT = "logout";// 手动退出
    public static final String LOGIN_FAILED = "login failed";// 登录失败
    public static final String DISCONNECTED_WITHOUT_WARNING = "disconnected without warning";// 没有警告的断开连接
    // 自动重连 start
    private static final int RECONNECT_AFTER = 5;
    private static final int RECONNECT_MAXIMUM = 10 * 60;// 最大重连时间间隔
    private static final String RECONNECT_ALARM = "com.cfryan.RECONNECT_ALARM";
    private IBinder mBinder = new XXBinder();
    private IConnectionStatusCallback mConnectionStatusCallback;
    private SmackImpl mSmackable;
    private Thread mConnectingThread;
    private Handler mMainHandler = new Handler();
    private boolean mIsFirstLoginAction;
    // private boolean mIsNeedReConnection = false; // 是否需要重连
    private int mConnectedState = DISCONNECTED; // 是否已经连接
    private int mReconnectTimeout = RECONNECT_AFTER;
    private Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
    private PendingIntent mPAlarmIntent;
    private BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();
    // 自动重连 end
    private ActivityManager mActivityManager;
    // 判断程序是否在后台运行的任务
    Runnable monitorStatus = new Runnable() {
        @Override
        public void run() {
            try {
                L.i("monitorStatus is running... " + getPackageName());
                mMainHandler.removeCallbacks(monitorStatus);
                // 如果在后台运行并且连接上了
                if (!isAppOnForeground()) {
                    L.i("app run in background...");
                    // if (isAuthenticated())
//                    updateServiceNotification(getString(R.string.run_bg_ticker));
                    return;
                } else {
                    stopForeground(true);
                }
                // mMainHandler.postDelayed(monitorStatus, 1000L);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
    private HashSet<String> mIsBoundTo = new HashSet<>();// 用来保存当前正在聊天对象的数组
    //头像上传状态的handle
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {

            if (msg.what == 0) {
                Toast.makeText(getApplicationContext(), "头像上传失败！", Toast.LENGTH_SHORT).show();
            } else if (msg.what == 1) {
                Toast.makeText(getApplicationContext(), "头像上传成功！", Toast.LENGTH_SHORT).show();
            }

        }

        ;
    };

    private static byte[] getFileBytes(File file) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            int bytes = (int) file.length();
            byte[] buffer = new byte[bytes];
            int readBytes = bis.read(buffer);
            if (readBytes != buffer.length) {
                throw new IOException("Entire file not read");
            }
            return buffer;
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }

    /**
     * 注册注解面和聊天界面时连接状态变化回调
     *
     * @param cb
     */
    public void registerConnectionStatusCallback(IConnectionStatusCallback cb) {
        mConnectionStatusCallback = cb;
    }

    public void unRegisterConnectionStatusCallback() {
        mConnectionStatusCallback = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        L.i(CoreService.class, "[SERVICE] onBind");
        String chatPartner = intent.getDataString();
        if ((chatPartner != null)) {
            mIsBoundTo.add(chatPartner);
        }
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action) && TextUtils.equals(action, LoginActivity.LOGIN_ACTION)) {
            mIsFirstLoginAction = true;
        } else {
            mIsFirstLoginAction = false;
        }
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        String chatPartner = intent.getDataString();
        if ((chatPartner != null)) {
            mIsBoundTo.add(chatPartner);
        }
        String action = intent.getAction();
        if (!TextUtils.isEmpty(action) && TextUtils.equals(action, LoginActivity.LOGIN_ACTION)) {
            mIsFirstLoginAction = true;
        } else {
            mIsFirstLoginAction = false;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        String chatPartner = intent.getDataString();
        if ((chatPartner != null)) {
            mIsBoundTo.remove(chatPartner);
        }
        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
//        XXBroadcastReceiver.mListeners.add(this);
        //	BaseActivity.mListeners.add(this);
        mActivityManager = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE));
        mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null
//                && TextUtils.equals(intent.getAction(), XXBroadcastReceiver.BOOT_COMPLETED_ACTION)
                ) {
            String account = PreferenceUtils.getPrefString(CoreService.this, PreferenceConstants.ACCOUNT, "");
            String password = PreferenceUtils.getPrefString(CoreService.this, PreferenceConstants.PASSWORD, "");
            if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(password))
                Login(account, password);
        }
        mMainHandler.removeCallbacks(monitorStatus);
        mMainHandler.postDelayed(monitorStatus, 1000L);// 检查应用是否在后台运行线程
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        XXBroadcastReceiver.mListeners.remove(this);
        //	BaseActivity.mListeners.remove(this);
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);// 取消重连闹钟
        unregisterReceiver(mAlarmReceiver);// 注销广播监听
//        logout();
    }

//    public void changeImage(final Bitmap f) {
//        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
//            connectionFailed(NETWORK_ERROR);
//            return;
//        }
//        Thread t = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    mSmackable = new SmackImpl(CoreService.this);
//                    if (!mSmackable.isAuthenticated()) {
//                        mSmackable.login(PreferenceUtils.getPrefString(getApplicationContext(), PreferenceConstants.ACCOUNT, ""),
//                                PreferenceUtils.getPrefString(getApplicationContext(), PreferenceConstants.PASSWORD, ""));
//                    }
//
//                } catch (XXMPException e1) {
//                    // TODO Auto-generated catch block
//                    e1.printStackTrace();
//                }
//
//                try {
//                    if (mSmackable.setUserImage(ImageTools.bitmapToBytes(f))) {
//                        handler.sendEmptyMessage(1);
//                    } else {
//                        handler.sendEmptyMessage(0);
//                    }
//
//                } catch (XMPPException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        });
//        t.start();
//
//    }

    // 登录
    public void Login(final String account, final String password) {
//        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
//            connectionFailed(NETWORK_ERROR);
//            return;
//        }
        if (mConnectingThread != null) {
            L.i("a connection is still goign on!");
            return;
        }
        mConnectingThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);
                    postConnecting();
                    mSmackable = new SmackImpl(CoreService.this);
                    if (mSmackable.login(account, password)) {
                        // 登陆成功
                        postConnectionScuessed();

//                        mSmackable.initAvatar();
//                        new GetPhoneContactsThread().start();
                    } else {
                        // 登陆失败
                        postConnectionFailed(LOGIN_FAILED);
                    }

                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (mConnectingThread != null)
                        synchronized (mConnectingThread) {
                            mConnectingThread = null;
                        }
                }
            }
        };
        mConnectingThread.start();
    }

//    // 发送消息
//    public void sendMessage(Message message, int ds, Boolean compress) {
//        if (mSmackable != null) {
//            mSmackable.sendMessage(message, ds, compress);
//        } else {
//            Log.e("net", "failed");
//            SmackImpl.saveAsOfflineMessage(getContentResolver(), message);
//        }
//    }

//    // 注册
//    public int regist(final String account, final String password) {
//        int registstate = 0;
//        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
//            connectionFailed(NETWORK_ERROR);
//        }
//
//        mSmackable = new SmackImpl(CoreService.this);
//        String state = mSmackable.register(account, password);
//
//        if (state.equals("1")) {
//            registstate = 1;
//        } else if (state.equals("2")) {
//            registstate = 2;
//        } else if (state.equals("3")) {
//            registstate = 3;
//        }
//        return registstate;
//    }

//    // 退出
//    public boolean logout() {
//        // mIsNeedReConnection = false;// 手动退出就不需要重连闹钟了
//        boolean isLogout = false;
//        if (mConnectingThread != null) {
//            synchronized (mConnectingThread) {
//                try {
//                    mConnectingThread.interrupt();
//                    mConnectingThread.join(50);
//                } catch (InterruptedException e) {
//                    L.e("doDisconnect: failed catching connecting thread");
//                } finally {
//                    mConnectingThread = null;
//                }
//            }
//        }
//        if (mSmackable != null) {
//            isLogout = mSmackable.logout();
//            mSmackable = null;
//        }
//        connectionFailed(LOGOUT);// 手动退出
//        return isLogout;
//    }
//
//    // 是否连接上服务器
//    public boolean isAuthenticated() {
//        if (mSmackable != null) {
//            return mSmackable.isAuthenticated();
//        }
//
//        return false;
//    }

//    // 设置连接状态
//    public void setStatusFromConfig() {
//        mSmackable.setStatusFromConfig();
//    }
//
//    // 新增联系人
//    public void addRosterItem(String user, String alias, String group) {
//        if (!mSmackable.isAuthenticated()) {
//            Toast.makeText(getApplicationContext(), "未连接服务器，请重新登陆！", Toast.LENGTH_LONG).show();
//            try {
//                mSmackable.login(PreferenceUtils.getPrefString(getApplicationContext(), PreferenceConstants.ACCOUNT, ""),
//                        PreferenceUtils.getPrefString(getApplicationContext(), PreferenceConstants.PASSWORD, ""));
//            } catch (XXMPException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//
//        try {
//            mSmackable.addRosterItem(user, alias, group);
//            requestAuthorizationForRosterItem(user);
//
//        } catch (XXMPException e) {
//            T.showShort(this, e.getMessage());
//            L.e("exception in addRosterItem(): " + e.getMessage());
//        }
//    }

//    // 新增分组
//    public void addRosterGroup(String group) {
//        mSmackable.addRosterGroup(group);
//    }

//    // 删除联系人
//    public void removeRosterItem(String user) {
//        try {
//            mSmackable.removeRosterItem(user);
//        } catch (XXMPException e) {
//            T.showShort(this, e.getMessage());
//            L.e("exception in removeRosterItem(): " + e.getMessage());
//        }
//    }

//    // 将联系人移动到其他组
//    public void moveRosterItemToGroup(String user, String group) {
//        try {
//            mSmackable.moveRosterItemToGroup(user, group);
//        } catch (XXMPException e) {
//            T.showShort(this, e.getMessage());
//            L.e("exception in moveRosterItemToGroup(): " + e.getMessage());
//        }
//    }
//
//    // 重命名联系人
//    public void renameRosterItem(String user, String newName) {
//        try {
//            mSmackable.renameRosterItem(user, newName);
//        } catch (XXMPException e) {
//            T.showShort(this, e.getMessage());
//            L.e("exception in renameRosterItem(): " + e.getMessage());
//        }
//    }
//
//    // 重命名组
//    public void renameRosterGroup(String group, String newGroup) {
//        mSmackable.renameRosterGroup(group, newGroup);
//    }
//
//    public void requestAuthorizationForRosterItem(String user) {
//        mSmackable.requestAuthorizationForRosterItem(user);
//    }

    // 清除通知栏
    public void clearNotifications(String Jid) {
        clearNotification(Jid);
    }

    /**
     * 非UI线程连接失败反馈
     *
     * @param reason
     */
    public void postConnectionFailed(final String reason) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                connectionFailed(reason);
            }
        });
    }

    /**
     * UI线程反馈连接失败
     *
     * @param reason
     */
    private void connectionFailed(String reason) {
        L.i(CoreService.class, "connectionFailed: " + reason);
        mConnectedState = DISCONNECTED;// 更新当前连接状态
        if (mSmackable != null)
//            mSmackable.setStatusOffline();// 将所有联系人标记为离线
            if (TextUtils.equals(reason, LOGOUT)) {// 如果是手动退出
                ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
                return;
            }
        // 回调
        if (mConnectionStatusCallback != null) {
            mConnectionStatusCallback.connectionStatusChanged(mConnectedState, reason);
            if (mIsFirstLoginAction)// 如果是第一次登录,就算登录失败也不需要继续
                return;
        }

        // 无网络连接时,直接返回
        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
            return;
        }

        String account = PreferenceUtils.getPrefString(CoreService.this, PreferenceConstants.ACCOUNT, "");
        String password = PreferenceUtils.getPrefString(CoreService.this, PreferenceConstants.PASSWORD, "");
        // 无保存的帐号密码时，也直接返回
        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
            L.d("account = null || password = null");
            return;
        }
        // 如果不是手动退出并且需要重新连接，则开启重连闹钟
        if (PreferenceUtils.getPrefBoolean(this, PreferenceConstants.AUTO_RECONNECT, true)) {
            L.d("connectionFailed(): registering reconnect in " + mReconnectTimeout + "s");
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + mReconnectTimeout
                    * 1000, mPAlarmIntent);
            mReconnectTimeout = mReconnectTimeout * 2;
            if (mReconnectTimeout > RECONNECT_MAXIMUM)
                mReconnectTimeout = RECONNECT_MAXIMUM;
        } else {
            ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
        }

    }

    private void postConnectionScuessed() {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                connectionScuessed();
            }

        });
    }

    private void connectionScuessed() {
        mConnectedState = CONNECTED;// 已经连接上
        mReconnectTimeout = RECONNECT_AFTER;// 重置重连的时间

        if (mConnectionStatusCallback != null)
            mConnectionStatusCallback.connectionStatusChanged(mConnectedState, "");
    }

//    // 收到新消息
//    public void newMessage(final String from, final String message) {
//        mMainHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                if (!PreferenceUtils.getPrefBoolean(CoreService.this, PreferenceConstants.SCLIENTNOTIFY, false))
//                    MediaPlayer.create(CoreService.this, R.raw.office).start();
//                if (!isAppOnForeground())
//                    notifyClient(from, mSmackable.getNameForJID(from), message, !mIsBoundTo.contains(from));
//                // T.showLong(CoreService.this, from + ": " + message);
//
//            }
//
//        });
//    }

    // 连接中，通知界面线程做一些处理
    private void postConnecting() {
        // TODO Auto-generated method stub
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                connecting();
            }
        });
    }

//    /**
//     * 更新通知栏
//     *
//     * @param message
//     */
//    public void updateServiceNotification(String message) {
//        if (!PreferenceUtils.getPrefBoolean(this, PreferenceConstants.FOREGROUND, true))
//            return;
//        String title = PreferenceUtils.getPrefString(this, PreferenceConstants.ACCOUNT, "");
//        Notification n = new Notification(R.mipmap.login_default_avatar, title, System.currentTimeMillis());
//        n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
//
//        Intent notificationIntent = new Intent(this, WanglaiMainActivty.class);
//        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        n.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        n.setLatestEventInfo(this, title, message, n.contentIntent);
//        startForeground(SERVICE_NOTIFICATION, n);
//    }

    private void connecting() {
        // TODO Auto-generated method stub
        mConnectedState = CONNECTING;// 连接中
        if (mConnectionStatusCallback != null)
            mConnectionStatusCallback.connectionStatusChanged(mConnectedState, "");
    }

    // 联系人改变
    public void rosterChanged() {
        // gracefully handle^W ignore events after a disconnect
        if (mSmackable == null)
            return;
        if (mSmackable != null && !mSmackable.isAuthenticated()) {
            L.i("rosterChanged(): disconnected without warning");
            connectionFailed(DISCONNECTED_WITHOUT_WARNING);
        }
    }

    public boolean isAppOnForeground() {
        List<ActivityManager.RunningAppProcessInfo> appProcesses = mActivityManager
                .getRunningAppProcesses();
        if (appProcesses == null)
            return false;
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(this.getPackageName())
                    && appProcess.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

//    @Override
//    public void onNetChange() {
//        if (NetUtil.getNetworkState(this) == NetUtil.NETWORN_NONE) {// 如果是网络断开，不作处理
//            connectionFailed(NETWORK_ERROR);
//            return;
//        }
//        if (mSmackable.isAuthenticated())// 如果已经连接上，直接返回
//            return;
//        String account = PreferenceUtils.getPrefString(CoreService.this, PreferenceConstants.ACCOUNT, "");
//        String password = PreferenceUtils.getPrefString(CoreService.this, PreferenceConstants.PASSWORD, "");
//        if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password))// 如果没有帐号，也直接返回
//            return;
//        if (!PreferenceUtils.getPrefBoolean(this, PreferenceConstants.AUTO_RECONNECT, true))// 不需要重连
//            return;
//        Login(account, password);// 重连
//    }

//	@Override
//	public void activityOnResume()
//	{
//		L.i("activity onResume ...");
//		mMainHandler.post(monitorStatus);
//	}
//
//	@Override
//	public void activityOnPause()
//	{
//		L.i("activity onPause ...");
//		mMainHandler.postDelayed(monitorStatus, 1000L);
//	}

    public class XXBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }

    // 自动重连广播
    private class ReconnectAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent i) {
            L.d("Alarm received.");
            if (!PreferenceUtils.getPrefBoolean(CoreService.this, PreferenceConstants.AUTO_RECONNECT, true)) {
                return;
            }
            if (mConnectedState != DISCONNECTED) {
                L.d("Reconnect attempt aborted: we are connected again!");
                return;
            }
            String account = PreferenceUtils.getPrefString(CoreService.this, PreferenceConstants.ACCOUNT, "");
            String password = PreferenceUtils.getPrefString(CoreService.this, PreferenceConstants.PASSWORD, "");
            if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
                L.d("account = null || password = null");
                return;
            }
            Login(account, password);
        }
    }

//    private class GetPhoneContactsThread extends Thread {
//
//        @Override
//        public void run() {
//            mSmackable.getPhoneContacts();
//        }
//    }
//
//    public void sendPacket(Presence response) {
//        mSmackable.sendPacket(response);
//    }


}
