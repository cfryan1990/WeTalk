package cfryan.wetalk.smack;

import android.content.ContentValues;
import android.provider.Telephony;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackInitialization;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
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

import cfryan.wetalk.service.CoreService;
import cfryan.wetalk.util.L;
import cfryan.wetalk.util.PreferenceConstants;
import cfryan.wetalk.util.PreferenceUtils;
import cfryan.wetalk.util.T;

/**
 * IM核心功能实现
 * Created by cf on 2016/1/6.
 */
public class SmackImpl implements Smack {

    // 通信实体的身份定义名称和类型。主要是向服务器登记，有点类似QQ显示iphone或者Android手机在线的功能
    public static final String XMPP_IDENTITY_CATEGORY = "client";// 实体类型
    public static final String XMPP_IDENTITY_NAME = "XMPP_WeTalk";// 客户端名称
    public static final String XMPP_IDENTITY_TYPE = "android";// 客户端类型

    private static final int PACKET_TIMEOUT = 30000;// 超时时间30s

    static
    {
        registerSmackProviders();
    }

    // 做一些基本XMPP协议配置
    static void registerSmackProviders()
    {
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

    private AbstractXMPPConnection mXMPPConnection;
    private static CoreService mService;// 主服务

    private Roster mRoster;

    private RosterListener mRosterListener;// 联系人动态监听
    private StanzaListener mPacketListener;// 消息动态监听
    private StanzaListener mSendFailureListener;// 消息发送失败动态监听
    private StanzaListener mPongListener;// ping pong服务器动态监听
    private StanzaListener mAvatarListener;// Avatar的动态监听
    private StanzaListener mAddListener;//添加朋友监听


    public SmackImpl(CoreService service) {
        String ServerHost = PreferenceUtils.getPrefString(service, PreferenceConstants.Server_IP,
                PreferenceConstants.MacBook_SERVER_IP);// 默认的服务器IP
        String ServerName = PreferenceUtils.getPrefString(service, PreferenceConstants.Server_Name,
                PreferenceConstants.MacBook_SERVER_NAME);//默认的服务器名
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
    }


    /**
     * 登录功能实现
     * @param account
     * @param password
     * @return
     *
     */
    @Override
    public boolean login(String account, String password) throws SmackException, IOException, XMPPException {
        if(mXMPPConnection.isConnected())
        {
            try
            {
                mXMPPConnection.disconnect();
            } catch (Exception e)
            {
                L.d("conn.disconnect() failed: "+e);
            }
        }

        SmackConfiguration.setDefaultPacketReplyTimeout(PACKET_TIMEOUT); //设置超时时间

        mXMPPConnection.connect();
        if (!mXMPPConnection.isConnected())
        {
            L.e(SmackImpl.class, "smack connect failed");
            throw new SmackException.NotConnectedException();
        }
        mXMPPConnection.addConnectionListener(new ConnectionListener() {
            @Override
            public void connected(XMPPConnection connection) {

            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
//                registerAllListener();
                Presence presence = new Presence(Presence.Type.available);
                presence.setStatus("I am online");
                try {
                    mXMPPConnection.sendStanza(presence);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
                L.i(SmackImpl.class,"Login success");
            }

            @Override
            public void connectionClosed() {
//                removeAllListener();
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

        if (!mXMPPConnection.isAuthenticated())
        {
            String ressource = PreferenceUtils.getPrefString(mService, PreferenceConstants.RESSOURCE, XMPP_IDENTITY_NAME);
            mXMPPConnection.login(account, password, ressource);
        }

        return mXMPPConnection.isAuthenticated();
    }

    @Override
    public boolean logout() throws SmackException.NotConnectedException, SmackException.NotLoggedInException {
        return false;
    }

    /**
     * 是否与服务器连接上，供本类和外部服务调用
     * @return
     */
    @Override
    public boolean isAuthenticated()  {
        if (mXMPPConnection != null)
        {
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


//    /**
//     * 注册所有的监听
//     */
//    private void registerAllListener()
//    {
//        // actually, authenticated must be true now, or an exception must have
//        // been thrown.
//        if (isAuthenticated())
//        {
//
//            registerMessageListener();// 注册新消息监听
//            registerMessageSendFailureListener();// 注册消息发送失败监听
//            registerPongListener();// 注册服务器回应ping消息监听
//            registerRosterListener();// 监听联系人动态变化
//            registerAvatarListener();// 注册头像更新监听
//            registerAddLinster();//注册好友添加监听
//
//            sendOfflineMessages();// 发送离线消息
//            if (mService == null)
//            {
//                mXMPPConnection.disconnect();
//                return;
//            }
//            // we need to "ping" the service to let it know we are actually
//            // connected, even when no roster entries will come in
//            mService.rosterChanged();
//        }
//    }

//    /**
//     * 反注册所有的监听
//     */
//    private void removeAllListener()
//    {
//        if (!isAuthenticated())
//        {
//            mXMPPConnection.removeAsyncStanzaListener(mPacketListener);// 反注册新消息监听
//            mRoster.removeRosterListener(mRosterListener); //反注册联系人监听
//            registerMessageSendFailureListener();// 反注册消息发送失败监听
////            registerPongListener();// 反注册服务器回应ping消息监听
//            registerAvatarListener();// 反注册头像更新监听
//            registerAddLinster();//反注册好友添加监听
//
//            sendOfflineMessages();// 发送离线消息
//            if (mService == null)
//            {
//                mXMPPConnection.disconnect();
//                return;
//            }
//            mService.rosterChanged();
//
//        }
//    }

//    /******************************* start 联系人数据库事件处理 **********************************/
//    private void registerRosterListener()
//    {
//        mRoster = Roster.getInstanceFor(mXMPPConnection);
//        mRosterListener = new RosterListener()
//        {
//            private boolean isFristRoter;
//
//            @Override
//            public void presenceChanged(Presence presence)
//            {// 联系人状态改变，比如在线或离开、隐身之类
//                L.i("presenceChanged(" + presence.getFrom() + "): " + presence);
//                String jabberID = getJabberID(presence.getFrom());
//                RosterEntry rosterEntry = mRoster.getEntry(jabberID);
//                updateRosterEntryInDB(rosterEntry);// 更新联系人数据库
//                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
//            }
//
//            @Override
//            public void entriesUpdated(Collection<String> entries)
//            {// 更新数据库，第一次登陆
//                // TODO
//                // Auto-generated
//                // method
//                // stub
//                L.i("entriesUpdated(" + entries + ")");
//                for (String entry : entries)
//                {
//                    RosterEntry rosterEntry = mRoster.getEntry(entry);
//                    updateRosterEntryInDB(rosterEntry);
//                }
//                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
//            }
//
//            @Override
//            public void entriesDeleted(Collection<String> entries)
//            {// 有好友删除时，
//                L.i("entriesDeleted(" + entries + ")");
//                for (String entry : entries)
//                {
//                    deleteRosterEntryFromDB(entry);
//                }
//                mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
//            }
//
//            @Override
//            public void entriesAdded(Collection<String> entries)
//            {// 有人添加好友时，我这里没有弹出对话框确认，直接添加到数据库
//                L.i("entriesAdded(" + entries + ")");
//                ContentValues[] cvs = new ContentValues[entries.size()];
//                int i = 0;
//                for (String entry : entries)
//                {
//                    RosterEntry rosterEntry = mRoster.getEntry(entry);
//                    cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
//                }
//                mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
//                if (isFristRoter)
//                {
//                    isFristRoter = false;
//                    mService.rosterChanged();// 回调通知服务，主要是用来判断一下是否掉线
//                }
//            }
//        };
//        mRoster.addRosterListener(mRosterListener);
//    }


}
