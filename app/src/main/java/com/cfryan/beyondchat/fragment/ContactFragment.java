package com.cfryan.beyondchat.fragment;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cfryan.beyondchat.R;
import com.cfryan.beyondchat.activity.ItemDetailActivity;
import com.cfryan.beyondchat.adapter.ContactAdapter;
import com.cfryan.beyondchat.model.ContactItem;
import com.cfryan.beyondchat.ui.view.ClearEditText;
import com.cfryan.beyondchat.ui.view.IndexBar;
import com.cfryan.beyondchat.util.DensityUtil;
import com.cfryan.beyondchat.util.PreferenceConstants;
import com.cfryan.beyondchat.util.PreferenceUtils;

import java.math.BigDecimal;

//import com.cfryan.beyondchat.activity.ChatActivity;
//import com.cfryan.beyondchat.activity.DetailInfoActivity;

public class ContactFragment extends Fragment {
    private static final int ANIMATION_DURATION = 300;
    private static final int CONTACT_LIST_MODE = 0;
    private static final int SEARCH_VIEW_MODE = 1;

    private int UIMode = 0;

    private View titleBar;
    private View bottomTabBar;

    private LinearLayout searchArea;
    private Button searchCancelBtn;
    private RelativeLayout cancelBtnLayout;
    private ClearEditText mFilterEditText;
    private ListView mSearchResultList;

    //联系人列表的主要View
    private TextView mFooterView;
    private FrameLayout frameContactView;
    private ListView mContactList;
    private IndexBar mIndexBar;

    //搜索界面展开的辅助动画View，包括独立的搜索框，搜索图标，搜索文字标签
    private LinearLayout animSearchViewFrame;
    private TextView animSearchTextView;
    private ImageView animSearchImageView;

    private TextView mSelectLetterDialog;
    private ContactAdapter mContactAdapter;

    public ContactFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            // Currently in a layout without a container, so no
            // reason to create our view.
            return null;
        }
        LayoutInflater myInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = myInflater.inflate(R.layout.fragment_contact, container, false);

        initViews(layout);
        return layout;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor(int color) {
        getActivity().getWindow().setStatusBarColor(color);


    }

    private Animation CreateTitleBarTransAnimation(float toYMove, final int StatusBarColorAfterAnimation) {
        Animation transTitle = new TranslateAnimation(0, 0, 0, toYMove);
        transTitle.setDuration(ANIMATION_DURATION);
        transTitle.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                switch (UIMode) {
                    case SEARCH_VIEW_MODE:
                        titleBar.setVisibility(View.VISIBLE);
                        break;
                    case CONTACT_LIST_MODE:
                        titleBar.setVisibility(View.GONE);
                        break;
                }
                if (StatusBarColorAfterAnimation != 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setStatusBarColor(StatusBarColorAfterAnimation);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        return transTitle;
    }

    private Animation CreateScaleAnimation(float scale) {

        Animation scaleAnimation = new ScaleAnimation(1.0f, scale, 1.0f, 1.0f, Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_SELF, 0f);
        scaleAnimation.setDuration(ANIMATION_DURATION);
        scaleAnimation.setZAdjustment(Animation.ZORDER_TOP);
//                scaleAnimation.setRepeatCount(1);
//                scaleAnimation.setRepeatMode(Animation.REVERSE);//必须设置setRepeatCount此设置才生效，动画执行完成之后按照逆方式动画返回
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                switch (UIMode) {
                    case SEARCH_VIEW_MODE:
                        animSearchViewFrame.setVisibility(View.VISIBLE);
                        mFilterEditText.setVisibility(View.GONE);
                        cancelBtnLayout.setVisibility(View.GONE);
                        break;
                    case CONTACT_LIST_MODE:
                        animSearchViewFrame.setVisibility(View.GONE);
                        mFilterEditText.setVisibility(View.VISIBLE);
                        cancelBtnLayout.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        return scaleAnimation;
    }

    private Animation CreateIconTransAnimation(float displayWidth) {
        float viewsWidth = DensityUtil.getViewMeasure(animSearchImageView).getMeasureWidth()
                + DensityUtil.getViewMeasure(animSearchTextView).getMeasureWidth()
                + getResources().getDimension(R.dimen.ui_basic_margin);
        Log.i("animSearchImageView", viewsWidth + "");
        Animation transView = new TranslateAnimation(0, -((displayWidth - viewsWidth) / 2 - DensityUtil.dip2px(getActivity(), 16.0f)), 0, 0);
        transView.setDuration(ANIMATION_DURATION);
        transView.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                switch (UIMode) {
                    case SEARCH_VIEW_MODE:
                        animSearchImageView.setVisibility(View.VISIBLE);
                        animSearchTextView.setVisibility(View.VISIBLE);
                        break;
                    case CONTACT_LIST_MODE:
                        animSearchImageView.setVisibility(View.GONE);
                        animSearchTextView.setVisibility(View.GONE);
                        break;
                }

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        return transView;
    }

    private void setSearchView(View SearchViewTrigger) {
        //view from Activity, when search action happens, many views should change.
        titleBar = getActivity().findViewById(R.id.ui_title_bar);
        bottomTabBar = getActivity().findViewById(R.id.rg_tab);

        //搜索本体，位于MainTabActivity的布局中
        searchArea = (LinearLayout) getActivity().findViewById(R.id.frame_search_area);
        mFilterEditText = (ClearEditText) getActivity().findViewById(R.id.filter_edit);
        cancelBtnLayout = (RelativeLayout) getActivity().findViewById(R.id.layout_search_cancel);
        searchCancelBtn = (Button) getActivity().findViewById(R.id.btn_search_cancel);
        mSearchResultList = (ListView) getActivity().findViewById(R.id.lv_search_result);

        //搜索界面展开的辅助动画View，包括独立的搜索框，搜索图标，搜索文字标签
        animSearchViewFrame = (LinearLayout) getActivity().findViewById(R.id.anim_search_view_frame);
        animSearchTextView = (TextView) getActivity().findViewById(R.id.anim_tv_search);
        animSearchImageView = (ImageView) getActivity().findViewById(R.id.anim_iv_search);

        final float displayWidth = PreferenceUtils.getPrefFloat(getActivity(), PreferenceConstants.DISPLAY_WIDTH, 0.00f);
        BigDecimal b = new BigDecimal(displayWidth);
        float displayWidthInFloat = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
        final float searchViewScale = 1.00f - DensityUtil.dip2px(getActivity(), 50) / displayWidthInFloat;
        Log.i("SearchViewScale", searchViewScale + "");

        SearchViewTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UIMode = CONTACT_LIST_MODE;
                searchArea.setVisibility(View.VISIBLE);
                bottomTabBar.setVisibility(View.GONE);

                titleBar.startAnimation(CreateTitleBarTransAnimation(-getResources().getDimension(R.dimen.ui_title_bar_height), getResources().getColor(R.color.grey_deep)));
                searchArea.startAnimation(CreateTitleBarTransAnimation(-getResources().getDimension(R.dimen.ui_title_bar_height), 0));

                animSearchViewFrame.startAnimation(CreateScaleAnimation(searchViewScale));

                animSearchImageView.startAnimation(CreateIconTransAnimation(displayWidth));
                animSearchTextView.startAnimation(CreateIconTransAnimation(displayWidth));

            }
        });

        searchCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("btncancel", "click");
                UIMode = SEARCH_VIEW_MODE;
                titleBar.setVisibility(View.VISIBLE);
                bottomTabBar.setVisibility(View.VISIBLE);
                searchArea.setVisibility(View.GONE);

                animSearchViewFrame.setVisibility(View.VISIBLE);
                mFilterEditText.setVisibility(View.GONE);
                cancelBtnLayout.setVisibility(View.GONE);

                animSearchImageView.setVisibility(View.VISIBLE);
                animSearchTextView.setVisibility(View.VISIBLE);

//                titleBar.startAnimation(CreateTitleBarTransAnimation(getResources().getDimension(R.dimen.ui_title_bar_height), getResources().getColor(R.color.ui_green)));
//                searchArea.startAnimation(CreateTitleBarTransAnimation(getResources().getDimension(R.dimen.ui_title_bar_height), 0));
//
//                animSearchViewFrame.startAnimation(CreateScaleAnimation(searchViewScale));
//
//                animSearchImageView.startAnimation(CreateIconTransAnimation(displayWidth));
//                animSearchTextView.startAnimation(CreateIconTransAnimation(displayWidth));


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setStatusBarColor(getResources().getColor(R.color.ui_green));
                }

            }
        });
    }

    private void setContactListHeader() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        //Search View
        View searchViewInContactList = inflater.inflate(R.layout.search_view, null);
        setSearchView(searchViewInContactList);
        //new friend view
        View newFriendView = (View) inflater.inflate(R.layout.item_contact, null);
        newFriendView.setBackgroundColor(getResources().getColor(R.color.ui_white));
        TextView newFriendTextView = (TextView) newFriendView.findViewById(R.id.tv_contact_name);
        newFriendTextView.setText("新朋友");
        ImageView newFriendImageView = (ImageView) newFriendView.findViewById(R.id.iv_contact_avatar);
        newFriendImageView.setImageResource(R.mipmap.ic_push_friends);
        //动态加载view，viewstub要使用inflate必须已经设置了layout
        ViewStub vsDividerLeft = (ViewStub) newFriendView.findViewById(R.id.viewstub_divider);
        vsDividerLeft.setLayoutResource(R.layout.divider_margin_left);
        vsDividerLeft.inflate();
        //add friend view
        View addFriendView = (View) inflater.inflate(R.layout.item_contact, null);
        addFriendView.setBackgroundColor(getResources().getColor(R.color.ui_white));
        TextView addFriendTextView = (TextView) addFriendView.findViewById(R.id.tv_contact_name);
        addFriendTextView.setText("添加朋友");
        ImageView addFriendImageView = (ImageView) addFriendView.findViewById(R.id.iv_contact_avatar);
        addFriendImageView.setImageResource(R.mipmap.ic_add_friends);
        //动态加载view，viewstub要使用inflate必须已经设置了layout
        ViewStub vsDividerFull = (ViewStub) addFriendView.findViewById(R.id.viewstub_divider);
        vsDividerFull.setLayoutResource(R.layout.divider_full);
        vsDividerFull.inflate();
        //给联系人列表增加固定头
        mContactList.addHeaderView(searchViewInContactList);
        mContactList.addHeaderView(newFriendView);
        mContactList.addHeaderView(addFriendView);
    }

    public void setContactListFooter() {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        //listview底部设置
        mFooterView = (TextView) inflater.inflate(R.layout.item_tv_footer, null);
        String ContactSizeString = mContactAdapter.getContactSize() + "位联系人";
        mFooterView.setText(ContactSizeString);
        mContactList.addFooterView(mFooterView);
    }

    public void initViews(View layout) {
        frameContactView = (FrameLayout) layout.findViewById(R.id.fragment_layout);
        mContactList = (ListView) layout.findViewById(R.id.lv_contact_list_view);

        setContactListHeader();

        mContactAdapter = new ContactAdapter(getActivity());

        String[] index = new String[mContactAdapter.getSectionSize()];
        ContactItem[] tmp = mContactAdapter.getSection();
        for (int i = 0; i < mContactAdapter.getSectionSize(); i++) {
            index[i] = tmp[i].letter;
        }
        mIndexBar = new IndexBar(getActivity(), index);
        FrameLayout.LayoutParams layoutParams
                = new FrameLayout.LayoutParams(DensityUtil.dip2px(getActivity(), 30), DensityUtil.dip2px(getActivity(), 20) * index.length);
        layoutParams.gravity = Gravity.CENTER | Gravity.END;
        mIndexBar.setLayoutParams(layoutParams);
        frameContactView.addView(mIndexBar);
        // 设置右侧触摸监听
        mIndexBar.setOnTouchingLetterChangedListener(new IndexBar.OnTouchingLetterChangedListener() {

            @Override
            public void onTouchingLetterChanged(int index) {
                // 该字母首次出现的位置
                int position = mContactAdapter.getPositionForSection(index);
                if (position != -1) {
                    mContactList.setSelection(position + mContactList.getHeaderViewsCount());
                }
            }
        });

        mContactList.setAdapter(mContactAdapter);

        setContactListFooter();

//        mFilterEditText.setShakeAnimation();
        // 根据输入框输入值的改变来过滤搜索
        mFilterEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                // 当输入框里面的值为空，更新为原来的列表，否则为过滤数据列表
                mContactAdapter.setFilterString(s.toString());
                mFooterView.setText(mContactAdapter.getContactSize() + "位联系人");
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mContactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // 这里要利用adapter.getItem(position)来获取当前position所对应的对象
                String alias = mContactAdapter.getItem(position - 3).model.getRoster().getAlias();

                Snackbar.make(view, alias, Snackbar.LENGTH_LONG).show();
//                Toast.makeText(getActivity(), alias, Toast.LENGTH_SHORT).show();
                startDetailInfoActivity((mContactAdapter.getItem(position - 3)).model.getRoster().getJid(),
                        alias);
            }
        });

    }

    private void startDetailInfoActivity(String userJid, String alias) {

        Intent detailInfoIntent = new Intent(getActivity(),
                ItemDetailActivity.class);
        Uri userNameUri = Uri.parse(userJid);
//        detailInfoIntent.setData(userNameUri);
//        detailInfoIntent.putExtra(ItemDetailActivity.INTENT_EXTRA_USERNAME,
//                alias);
        startActivity(detailInfoIntent);
    }

    private void startChatActivity(String userJid, String userName) {
//		Intent chatIntent = new Intent(getActivity(), ChatActivity.class);
//		Uri userNameUri = Uri.parse(userJid);
//		chatIntent.setData(userNameUri);
//		chatIntent.putExtra(ChatActivity.INTENT_EXTRA_USERNAME, userName);
//		startActivity(chatIntent);
    }
//
//	private void startPhoneActivity() {
//		Intent intent = new Intent(getActivity(), PhonesActivity.class);
//		startActivity(intent);
//	}
//
//	private void startNewFriendsActivity()
//	{
//		Intent intent = new Intent(getActivity(), NewFriendsActivity.class);
//		startActivity(intent);
//	}


}
