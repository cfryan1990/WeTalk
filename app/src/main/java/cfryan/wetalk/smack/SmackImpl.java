package cfryan.wetalk.smack;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackInitialization;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.provider.ProviderManager;
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

    public AbstractXMPPConnection connection;
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
    public SmackImpl() {



        XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
        configBuilder.setHost("192.168.1.105");
        configBuilder.setPort(5222);
        configBuilder.setServiceName("chenfengdemacbook-pro.local");
        configBuilder.setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.disabled);
        configBuilder.setDebuggerEnabled(true);

        connection = new XMPPTCPConnection(configBuilder.build());

    }




    @Override
    public boolean login(String account, String password) throws SmackException.NotConnectedException, SmackException.AlreadyLoggedInException {
        return false;
    }

    @Override
    public boolean logout() throws SmackException.NotConnectedException, SmackException.NotLoggedInException {
        return false;
    }

    @Override
    public boolean isAuthenticated() throws SmackException.NotConnectedException, SmackException.NotLoggedInException {
        return false;
    }

    @Override
    public void addRosterItem(String user, String alias, String group) throws SmackException.NotConnectedException, SmackException.NotLoggedInException {

    }

    @Override
    public void removeRosterItem(String user) throws SmackException.NotConnectedException, SmackException.NotLoggedInException {

    }


}
