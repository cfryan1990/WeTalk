package cfryan.wetalk.smack;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import java.io.IOException;

/**
 * Created by cf on 2016/1/6.
 * 定义一些接口，明确实现的功能
 */
public interface Smack {

    /**
     * 登录
     * @param account
     * @param password
     * @return 登陆结果

     */
    public boolean login(String account, String password) throws SmackException, IOException, XMPPException;

    /**
     * 注销登陆
     * @return 登出结果
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NotLoggedInException
     */
    public boolean logout()
            throws SmackException.NotConnectedException, SmackException.NotLoggedInException;

    /**
     * 检查登录和认证状态
     * @return
     */
    public boolean isAuthenticated();

    /**
     * 添加好友
     *
     * @param user  好友id
     * @param alias 昵称
     * @param group 所在的分组
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NotLoggedInException
     */
    public void addRosterItem(String user, String alias, String group)
            throws SmackException.NotConnectedException, SmackException.NotLoggedInException;

    /**
     * 删除好友
     *
     * @param user 好友id
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NotLoggedInException
     */
    public void removeRosterItem(String user)
            throws SmackException.NotConnectedException, SmackException.NotLoggedInException;
}
