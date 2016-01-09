package com.cfryan.beyondchat.model;

import com.cfryan.beyondchat.db.RosterProvider;

/**
 * Created by feng on 2015/8/30.
 */
public class ContactModel {
    private RosterModel roster;
    private String sortLetters; // 显示数据拼音的首字母

    // 联系人查询序列
    private static final String[] ROSTER_QUERY = new String[]
            {RosterProvider.RosterConstants._ID, RosterProvider.RosterConstants.JID,
                    RosterProvider.RosterConstants.ALIAS, RosterProvider.RosterConstants.STATUS_MODE,
                    RosterProvider.RosterConstants.STATUS_MESSAGE};


    public RosterModel getRoster()
    {
        return roster;
    }

    public void setRoster(RosterModel roster)
    {
        this.roster = roster;
    }

    public String getSortLetters()
    {
        return sortLetters;
    }

    public void setSortLetters(String sortLetters)
    {
        this.sortLetters = sortLetters;
    }

}
