package cfryan.wetalk.activity;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.ping.android.ServerPingWithAlarmManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import cfryan.wetalk.R;

public class MainActivity extends Activity {

	private LinearLayout msgList;
	private EditText msg;
	private Button send;
	private Chat topChat;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		msgList = (LinearLayout) findViewById(R.id.messages);
		msg = (EditText) findViewById(R.id.msg);
		send = (Button) findViewById(R.id.send);


		XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
		configBuilder.setHost("10.1.17.5");
		configBuilder.setPort(5222);
		configBuilder.setServiceName("desktop-cfryan1990");
		configBuilder.setSecurityMode(XMPPTCPConnectionConfiguration.SecurityMode.disabled);
		configBuilder.setDebuggerEnabled(true);

//		configBuilder.setKeystorePath("/system/etc/security/cacerts.bks");
//		configBuilder.setKeystoreType("bks");


//		configBuilder.setHostnameVerifier(new HostnameVerifier() {
//			@Override
//			public boolean verify(String hostname, SSLSession session) {
//				return true;
//			}
//		});

//		try {
//			SSLContext sc = SSLContext.getInstance("TLS");
//			sc.init(null, MemorizingTrustManager.getInstanceList(context), new SecureRandom());
//			configBuilder.setCustomSSLContext(sc);
//		} catch (NoSuchAlgorithmException e) {
//			throw new IllegalStateException(e);
//		} catch (KeyManagementException e) {
//			throw new IllegalStateException(e);
//		}

//		SASLMechanism mechanism = new SASLDigestMD5Mechanism();
//		SASLAuthentication.registerSASLMechanism(mechanism);
//		SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
//		SASLAuthentication.unBlacklistSASLMechanism("DIGEST-MD5");

		final AbstractXMPPConnection connection =
				new XMPPTCPConnection(configBuilder.build());
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(3000);
					connection.connect();
					setText("连接服务器成功@10.1.17.5:5222");

					connection.login("cfryan1990", "663368");
					Presence presence = new Presence(Presence.Type.available);
					presence.setStatus("I am online");

					connection.sendStanza(presence);
					setText("登陆成功");

					try {
						VCard vCard = VCardManager.getInstanceFor(connection).loadVCard();
						vCard.setNickName("陈峰");
						VCardManager.getInstanceFor(connection).saveVCard(vCard);
					} catch (SmackException.NoResponseException e) {
						e.printStackTrace();
					} catch (XMPPException.XMPPErrorException e) {
						e.printStackTrace();
					} catch (NotConnectedException e) {
						e.printStackTrace();
					}

					try {
						VCard vCard = VCardManager.getInstanceFor(connection).loadVCard();
						String name = vCard.getNickName();
						Log.i("name",name);
					} catch (SmackException.NoResponseException e) {
						e.printStackTrace();
					} catch (XMPPException.XMPPErrorException e) {
						e.printStackTrace();
					} catch (NotConnectedException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();

		connection.isAuthenticated();
		ChatManager cm = ChatManager.getInstanceFor(connection);

		send.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!msg.getText().toString().equals("") && topChat != null) {
					try {
						String string = msg.getText().toString();
						topChat.sendMessage(string);
						TextView view = new TextView(MainActivity.this);
						view.setText("我： " + string);
						msgList.addView(view);
						msg.setText("");
					} catch (NotConnectedException e) {
						Toast.makeText(MainActivity.this, "发送失败", Toast.LENGTH_LONG).show();
						e.printStackTrace();
					}
				}
			}
		});

		cm.addChatListener(new ChatManagerListener() {

			@Override
			public void chatCreated(Chat arg0, boolean arg1) {
				arg0.addMessageListener(new ChatMessageListener() {

					@Override
					public void processMessage(Chat arg0, Message arg1) {
						topChat = arg0;
						if (null != arg1.getBody()) {
							String from = arg1.getFrom().substring(0, arg1.getFrom().indexOf("@"));
							setText("from " + from + " : " + arg1.getBody());
						}
					}
				});
			}
		});
	}

	private void setText(final String text) {
		runOnUiThread(new Runnable() {
			public void run() {
				TextView tv = new TextView(MainActivity.this);
				tv.setText(text);
				msgList.addView(tv);
			}

			;
		});
	}

}
