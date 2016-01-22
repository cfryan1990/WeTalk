package com.cfryan.beyondchat.fragment;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
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
import com.cfryan.beyondchat.adapter.ContactAdapter;
import com.cfryan.beyondchat.model.ContactItem;
import com.cfryan.beyondchat.ui.view.ClearEditText;
import com.cfryan.beyondchat.ui.view.IndexBar;
import com.cfryan.beyondchat.util.DensityUtil;

//import com.cfryan.beyondchat.activity.ChatActivity;
//import com.cfryan.beyondchat.activity.DetailInfoActivity;

public class ContactFragment extends Fragment {
    private View titleBar;
    private View bottomTabBar;

    private LinearLayout searchArea;

    private Button searchCancelBtn;

    private TextView mFooterView;
    private ListView mContactList;
    private ListView mSearchResultList;
    private IndexBar mIndexBar;
    private TextView mSelectLetterDialog;
    private ContactAdapter mContactAdapter;
    private ClearEditText mFilterEditText;

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

    public void initViews(View layout) {
        titleBar = (View) getActivity().findViewById(R.id.ui_title_bar);
        bottomTabBar = (View) getActivity().findViewById(R.id.rg_tab);

        searchArea = (LinearLayout) getActivity().findViewById(R.id.frame_search_area);
        searchCancelBtn = (Button) getActivity().findViewById(R.id.btn_search_cancel);
        mSearchResultList = (ListView) getActivity().findViewById(R.id.lv_search_result);

        final LinearLayout animSearchViewFrame = (LinearLayout) getActivity().findViewById(R.id.anim_search_view_frame);

        final TextView animSearchTextView = (TextView) getActivity().findViewById(R.id.anim_tv_search);
        final ImageView animSearchImageView = (ImageView) getActivity().findViewById(R.id.anim_iv_search);

        final RelativeLayout cancelBtnRL = (RelativeLayout) getActivity().findViewById(R.id.rl_search_cancel);
        mFilterEditText = (ClearEditText) getActivity().findViewById(R.id.filter_edit);

        final FrameLayout frameContactView = (FrameLayout) layout.findViewById(R.id.fragment_layout);
        mContactList = (ListView) layout.findViewById(R.id.lv_contact_list_view);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View searchViewInContactList = (View) inflater.inflate(R.layout.search_view, null);
        searchViewInContactList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frameContactView.setVisibility(View.GONE);
                searchArea.setVisibility(View.VISIBLE);
                bottomTabBar.setVisibility(View.GONE);

                Animation transTitle = new TranslateAnimation(0, 0, 0, -DensityUtil.dip2px(getActivity(),35.0f));
                transTitle.setDuration(500);
                transTitle.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        titleBar.setVisibility(View.GONE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            setStatusBarColor(getResources().getColor(R.color.grey_deep));
                        }

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });


                titleBar.startAnimation(transTitle);
                searchArea.startAnimation(transTitle);

                ScaleAnimation _scaleAnimation = new ScaleAnimation(1.0f, 0.85f, 1.0f, 1.0f, Animation.RELATIVE_TO_PARENT, 0f, Animation.RELATIVE_TO_SELF, 0f);
                _scaleAnimation.setDuration(500);
                _scaleAnimation.setZAdjustment(Animation.ZORDER_TOP);
//                _scaleAnimation.setRepeatCount(1);
//                _scaleAnimation.setRepeatMode(Animation.REVERSE);//必须设置setRepeatCount此设置才生效，动画执行完成之后按照逆方式动画返回
                animSearchViewFrame.startAnimation(_scaleAnimation);
                _scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        animSearchViewFrame.setVisibility(View.GONE);
                        mFilterEditText.setVisibility(View.VISIBLE);
                        cancelBtnRL.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                DisplayMetrics dm = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
                int width = dm.widthPixels;    //得到宽度
                int height = dm.heightPixels;  //得到高度

                Animation transView = new TranslateAnimation(0,-width/2+20 , 0, 0);
                transView.setDuration(500);

                animSearchImageView.startAnimation(transView);
                animSearchTextView.startAnimation(transView);


                transView.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        animSearchImageView.setVisibility(View.GONE);
                        animSearchTextView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });


//                Animation trans = new TranslateAnimation(0,0,0,-20);
//                trans.setDuration(3000);
//                searchView.setAnimation(trans);
//                trans.startNow();


            }
        });

        searchCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                titleBar.setVisibility(View.VISIBLE);
                bottomTabBar.setVisibility(View.VISIBLE);
                searchArea.setVisibility(View.GONE);
                frameContactView.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setStatusBarColor(getResources().getColor(R.color.ui_green));
                }
            }
        });

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

        View addFriendView = (View) inflater.inflate(R.layout.item_contact, null);
        addFriendView.setBackgroundColor(getResources().getColor(R.color.ui_white));
        TextView addFriendTextView = (TextView) addFriendView.findViewById(R.id.tv_contact_name);
        addFriendTextView.setText("添加朋友");
        ImageView addFriendImageView = (ImageView) addFriendView.findViewById(R.id.iv_contact_avatar);
        addFriendImageView.setImageResource(R.mipmap.ic_add_friends);
        ViewStub vsDividerFull = (ViewStub) addFriendView.findViewById(R.id.viewstub_divider);
        vsDividerFull.setLayoutResource(R.layout.divider_full);
        vsDividerFull.inflate();

        mContactList.addHeaderView(searchViewInContactList);
        mContactList.addHeaderView(newFriendView);
        mContactList.addHeaderView(addFriendView);

        mContactAdapter = new ContactAdapter(getActivity());

        String[] index = new String[mContactAdapter.getSectionSize()];
        ContactItem[] tmp = mContactAdapter.getSection();
        for (int i = 0; i < mContactAdapter.getSectionSize(); i++) {
            index[i] = tmp[i].letter;
        }


        mIndexBar = new IndexBar(getActivity(), index);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(50, 30 * index.length);
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
                    mContactList.setSelection(position);
                }

            }
        });

        //listview底部设置
        mFooterView = (TextView) inflater.inflate(R.layout.item_tv_footer, null);
        String ContactSizeString = mContactAdapter.getContactSize() + "位联系人";
        mFooterView.setText(ContactSizeString);
        mContactList.addFooterView(mFooterView);

        mContactList.setAdapter(mContactAdapter);



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
                String alias = mContactAdapter.getItem(position).model.getRoster().getAlias();

                Snackbar.make(view, alias, Snackbar.LENGTH_LONG).show();
//                Toast.makeText(getActivity(), alias, Toast.LENGTH_SHORT).show();
                startDetailInfoActivity((mContactAdapter
                                .getItem(position)).model.getRoster().getJid(),
                        alias);
            }
        });

    }

    private void startDetailInfoActivity(String userJid, String alias) {

//		Intent detailInfoIntent = new Intent(getActivity(),
//				DetailInfoActivity.class);
//		Uri userNameUri = Uri.parse(userJid);
//		detailInfoIntent.setData(userNameUri);
//		detailInfoIntent.putExtra(DetailInfoActivity.INTENT_EXTRA_USERNAME,
//				alias);
//		startActivity(detailInfoIntent);
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
