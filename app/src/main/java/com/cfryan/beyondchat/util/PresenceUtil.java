package com.cfryan.beyondchat.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
public class PresenceUtil {
	/**
	 * 判断openfire用户的状态 strUrl : url格式 -
	 * http://my.openfire.com:9090/plugins/presence
	 * /status?jid=user1@my.openfire.com&type=xml 返回值 : 0 - 用户不存在; 1 - 用户在线; 2 -
	 * 用户离线 说明 ：必须要求 openfire加载 presence 插件，同时设置任何人都可以访问
	 */
	public static short IsUserOnline(String strUrl) {
		short state = 0; // 不存在

		try {
			URL url = new URL(strUrl);
			URLConnection conn = url.openConnection();
			if (conn != null) {
				BufferedReader bufferReader = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				if (null != bufferReader) {
					String strFlag = bufferReader.readLine();
					bufferReader.close();

					if (strFlag.indexOf("type=\"unavailable\"") >= 0) {
						state = 2;
					}else if (strFlag.indexOf("type=\"error\"") >= 0) {
						state = 0;
					}else if (strFlag.indexOf("priority") >= 0
							|| strFlag.indexOf("id=\"") >= 0) {
						state = 1;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return state;
	}
}
