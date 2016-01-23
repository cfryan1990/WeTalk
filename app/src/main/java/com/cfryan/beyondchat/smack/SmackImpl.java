package com.cfryan.beyondchat.smack;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import com.cfryan.beyondchat.db.RosterProvider;
import com.cfryan.beyondchat.service.CoreService;
import com.cfryan.beyondchat.util.L;
import com.cfryan.beyondchat.util.PreferenceConstants;
import com.cfryan.beyondchat.util.PreferenceUtils;
import com.cfryan.beyondchat.util.StatusMode;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.carbons.packet.Carbon;
import org.jivesoftware.smackx.carbons.provider.CarbonManagerProvider;
import org.jivesoftware.smackx.delay.provider.DelayInformationProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.forward.provider.ForwardedProvider;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;

import java.io.IOException;
import java.util.Collection;

/**
 * IM核心功能实现
 * Created by cf on 2016/1/6.
 */
public class SmackImpl implements Smack {

    // 通信实体的身份定义名称和类型。主要是向服务器登记，有点类似QQ显示iphone或者Android手机在线的功能
    public static final String XMPP_IDENTITY_CATEGORY = "client";// 实体类型
    public static final String XMPP_IDENTITY_NAME = "beyondchat_android";// 客户端名称
    public static final String XMPP_IDENTITY_TYPE = "android";// 客户端类型

    private static final int PACKET_TIMEOUT = 30000;// 超时时间30s
    private static CoreService mService;// 主服务

    static {
        registerSmackProviders();
    }

    private AbstractXMPPConnection mXMPPConnection;
    private ContentResolver mContentResolver = null;// 数据库操作对象
    private Roster mRoster;
    private RosterListener mRosterListener;// 联系人动态监听
    private StanzaListener mPacketListener;// 消息动态监听
    private StanzaListener mSendFailureListener;// 消息发送失败动态监听
    private StanzaListener mPongListener;// ping pong服务器动态监听
    private StanzaListener mAvatarListener;// Avatar的动态监听
    private StanzaListener mAddListener;//添加朋友监听

    public SmackImpl(CoreService service) {
        String ServerHost = PreferenceUtils.getPrefString(service, PreferenceConstants.Server_IP,
                PreferenceConstants.DEFAULT_SERVER_IP);// 默认的服务器IP
        String ServerName = PreferenceUtils.getPrefString(service, PreferenceConstants.Server_Name,
                PreferenceConstants.DEFAULT_SERVER_NAME);//默认的服务器名
        int port = PreferenceUtils.getPrefInt(service, PreferenceConstants.PORT,
                PreferenceConstants.DEFAULT_PORT_INT);// 端口号，也是留给用户手动设置的

        boolean SmackDebug = PreferenceUtils.getPrefBoolean(service, PreferenceConstants.SMACKDEBUG,
                false);// 是否需要smack的debug功能

        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
        configBuilder.setHost(ServerHost);
        configBuilder.setPort(port);
        configBuilder.setServiceName(ServerName);
        configBuilder.setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.disabled);
        configBuilder.setCompressionEnabled(false);
        configBuilder.setSendPresence(true);
        configBuilder.setDebuggerEnabled(SmackDebug);
        this.mXMPPConnection = new XMPPTCPConnection(configBuilder.build());

        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(mXMPPConnection);
        reconnectionManager.enableAutomaticReconnection();

        this.mService = service;
        this.mContentResolver = service.getContentResolver();
    }

    // 做一些基本XMPP协议配置
    static void registerSmackProviders() {
        ProviderManager pm = new ProviderManager();
        // add IQ handling
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
        // add delayed delivery notifications
        pm.addExtensionProvider("delay", "urn:xmpp:delay", new DelayInformationProvider());
        pm.addExtensionProvider("x", "jabber:x:delay", new DelayInformationProvider());
        // add carbons and forwarding,carbon是抄送的意思
        pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE, new ForwardedProvider());
        pm.addExtensionProvider("sent", Carbon.NAMESPACE, new CarbonManagerProvider());
        pm.addExtensionProvider("received", Carbon.NAMESPACE, new CarbonManagerProvider());
        // add delivery receipts
        pm.addExtensionProvider(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
        pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceiptRequest.Provider());
        // add my message extension
        // pm.addExtensionProvider(MessagePacketExtension.ELEMENT,
        // MessagePacketExtension.NAMESPACE, new
        // MessagePacketExtensionProvider());

        // add XMPP Ping (XEP-0199)
        pm.addIQProvider("ping", "urn:xmpp:ping", new PingProvider());
        // VCard
        pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

        DiscoverInfo.Identity identity
                = new DiscoverInfo.Identity(XMPP_IDENTITY_CATEGORY, XMPP_IDENTITY_NAME, XMPP_IDENTITY_TYPE);
        ServiceDiscoveryManager.setDefaultIdentity(identity);
    }

    /**
     * 登录功能实现
     *
     * @param account
     * @param password
     * @return
     */
    @Override
    public boolean login(String account, String password) throws SmackException, IOException, XMPPException {
        if (mXMPPConnection.isConnected()) {
            try {
                mXMPPConnection.disconnect();
            } catch (Exception e) {
                L.d("conn.disconnect() failed: " + e);
            }
        }

        SmackConfiguration.setDefaultPacketReplyTimeout(PACKET_TIMEOUT); //设置超时时间

        try {
            mXMPPConnection.connect();
        }finally {
            if (!mXMPPConnection.isConnected()) {
                L.e(SmackImpl.class, "smack connect server failed");
                return false;
            }
        }

        mXMPPConnection.addConnectionListener(new ConnectionListener() {
            @Override
            public void connected(XMPPConnection connection) {
                L.i(SmackImpl.class, "connected");
            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                registerAllListener();
                Presence presence = new Presence(Presence.Type.available);
                presence.setStatus("I am online");
                try {
                    mXMPPConnection.sendStanza(presence);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
                L.i(SmackImpl.class, "authenticated");
            }

            @Override
            public void connectionClosed() {
                removeAllListener();
                L.i(SmackImpl.class, "connectionClosed");
            }

            @Override
            public void connectionClosedOnError(Exception e) {
                mService.postConnectionFailed(e.getMessage());// 连接关闭时，动态反馈给服务
            }

            @Override
            public void reconnectionSuccessful() {

            }

            @Override
            public void reconnectingIn(int seconds) {

            }

            @Override
            public void reconnectionFailed(Exception e) {

            }
        });

        if (!mXMPPConnection.isAuthenticated()) {
            String resource = PreferenceUtils.getPrefString(mService, PreferenceConstants.RESSOURCE,
                    XMPP_IDENTITY_NAME);
            mXMPPConnection.login(account, password, resource);
        }

        return mXMPPConnection.isAuthenticated();
    }

    @Override
    public boolean logout() throws SmackException.NotConnectedException, SmackException.NotLoggedInException {
        return false;
    }

    /**
     * 是否与服务器连接上，供本类和外部服务调用
     *
     * @return
     */
    @Override
    public boolean isAuthenticated() {
        if (mXMPPConnection != null) {
            return (mXMPPConnection.isConnected() && mXMPPConnection.isAuthenticated());
        }
        return false;
    }

    @Override
    public void addRosterItem(String user, String alias, String group) throws SmackException.NotConnectedException, SmackException.NotLoggedInException {

    }

    @Override
    public void removeRosterItem(String user) throws SmackException.NotConnectedException, SmackException.NotLoggedInException {

    }


    /**
     * 注册所有的监听
     */
    private void registerAllListener() {
        // actually, authenticated must be true now, or an exception must have
        // been thrown.
        if (isAuthenticated()) {

//            registerMessageListener();// 注册新消息监听
//            registerMessageSendFailureListener();// 注册消息发送失败监听
//            registerPongListener();// 注册服务器回应ping消息监听
            registerRosterListener();// 监听联系人动态变化
//            registerAvatarListener();// 注册头像更新监听
//            registerAddLinster();//注册好友添加监听
//
//            sendOfflineMessages();// 发送离线消息
            if (mService == null) {
                mXMPPConnection.disconnect();
                return;
            }
            // we need to "ping" the service to let it know we are actually
            // connected, even when no roster entries will come in
            mService.rosterChanged();
        }
    }

    /**
     * 反注册所有的监听
     */
    private void removeAllListener() {
        if (!isAuthenticated()) {
//            mXMPPConnection.removeAsyncStanzaListener(mPacketListener);// 反注册新消息监听
            mRoster.removeRosterListener(mRosterListener); //反注册联系人监听
//            registerMessageSendFailureListener();// 反注册消息发送失败监听
////            registerPongListener();// 反注册服务器回应ping消息监听
//            registerAvatarListener();// 反注册头像更新监听
//            registerAddLinster();//反注册好友添加监听
//
//            sendOfflineMessages();// 发送离线消息
            if (mService == null) {
                mXMPPConnection.disconnect();
                return;
            }
            mService.rosterChanged();

        }
    }

    /*******************************
     * start 联系人数据库事件处理
     **********************************/
    private void registerRosterListener() {
        mRoster = Roster.getInstanceFor(mXMPPConnection);
        mRosterListener = new RosterListener() {
//            private boolean isFristRoter;

            @Override
            public void presenceChanged(Presence presence) {// 联系人状态改变，比如在线或离开、隐身之类
                L.i("presenceChanged(" + presence.getFrom() + "): " + presence);
                String jabberID = getJabberID(presence.getFrom());
                RosterEntry rosterEntry = mRoster.getEntry(jabberID);
                updateRosterEntryInDB(rosterEntry);// 更新联系人数据库
//                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
            }

            @Override
            public void entriesUpdated(Collection<String> entries) {// 更新数据库，第一次登陆
                L.i("entriesUpdated(" + entries + ")");
                for (String entry : entries) {
                    RosterEntry rosterEntry = mRoster.getEntry(entry);
                    updateRosterEntryInDB(rosterEntry);
                }
//                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
            }

            @Override
            public void entriesDeleted(Collection<String> entries) {// 有好友删除时，
                L.i("entriesDeleted(" + entries + ")");
                for (String entry : entries) {
                    deleteRosterEntryFromDB(entry);
                }
//                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
            }

            @Override
            public void entriesAdded(Collection<String> entries) {// 有人添加好友时，我这里没有弹出对话框确认，直接添加到数据库
                L.i("entriesAdded(" + entries + ")");
                ContentValues[] cvs = new ContentValues[entries.size()];
                int i = 0;
                for (String entry : entries) {
                    RosterEntry rosterEntry = mRoster.getEntry(entry);
                    cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
                }
                mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
//                if (isFristRoter)
//                {
//                    isFristRoter = false;
//                    mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
//                }
            }
        };
        mRoster.addRosterListener(mRosterListener);
    }

    private String getJabberID(String from) {
        String[] res = from.split("/");
        return res[0].toLowerCase();
    }

    /**
     * 更新联系人数据库
     *
     * @param entry 联系人RosterEntry对象
     */
    private void updateRosterEntryInDB(final RosterEntry entry) {
        final ContentValues values = getContentValuesForRosterEntry(entry);

        if (mContentResolver.update(RosterProvider.CONTENT_URI, values, RosterProvider.RosterConstants.JID + " = ?", new String[]
                {entry.getUser()}) == 0)// 如果数据库无此好友
            addRosterEntryToDB(entry);// 则添加到数据库
    }

    /**
     * 添加到数据库
     *
     * @param entry 联系人RosterEntry对象
     */
    private void addRosterEntryToDB(final RosterEntry entry) {
        ContentValues values = getContentValuesForRosterEntry(entry);
        Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
        L.i("addRosterEntryToDB: Inserted " + uri);
    }

    /**
     * 将联系人从数据库中删除
     *
     * @param jabberID
     */
    private void deleteRosterEntryFromDB(final String jabberID) {
        int count = mContentResolver.delete(RosterProvider.CONTENT_URI, RosterProvider.RosterConstants.JID + " = ?", new String[]
                {jabberID});
        L.i("deleteRosterEntryFromDB: Deleted " + count + " entries");
    }

    /**
     * 将联系人RosterEntry转化成ContentValues，方便存储数据库
     *
     * @param entry
     * @return
     */
    private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
        final ContentValues values = new ContentValues();

        values.put(RosterProvider.RosterConstants.JID, entry.getUser());
        values.put(RosterProvider.RosterConstants.ALIAS, getName(entry));


        Presence presence = mRoster.getPresence(entry.getUser());
        values.put(RosterProvider.RosterConstants.STATUS_MODE, getStatusInt(presence));
        values.put(RosterProvider.RosterConstants.STATUS_MESSAGE, presence.getStatus());
        values.put(RosterProvider.RosterConstants.GROUP, getGroup(entry.getGroups()));
        values.put(RosterProvider.RosterConstants.OWNER, mXMPPConnection.getUser());

        return values;
    }

    /**
     * 遍历获取组名
     *
     * @param groups
     * @return
     */
    private String getGroup(Collection<RosterGroup> groups) {
        for (RosterGroup group : groups) {
            return group.getName();
        }
        return "";
    }

    /**
     * 获取联系人名称
     *
     * @param rosterEntry
     * @return
     */
    private String getName(RosterEntry rosterEntry) {
        String name = rosterEntry.getName();
        if (name != null && name.length() > 0) {
            return name;
        }
        name = rosterEntry.getUser();
        if (name.length() > 0) {
            return name;
        }
        return rosterEntry.getUser();
    }

    /**
     * 获取状态
     *
     * @param presence
     * @return
     */
    private StatusMode getStatus(Presence presence) {
        if (presence.getType() == Presence.Type.available) {
            if (presence.getMode() != null) {
                return StatusMode.valueOf(presence.getMode().name());
            }
            return StatusMode.available;
        }
        return StatusMode.offline;
    }

    private int getStatusInt(final Presence presence) {
        return getStatus(presence).ordinal();
    }


}
