package com.zazoapp.client.ui;

import android.content.*;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.*;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.zazoapp.client.R;
import com.zazoapp.client.bench.InviteManager;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.core.VersionHandler;
import com.zazoapp.client.debug.ZazoGestureListener;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.network.aws.S3CredentialsGetter;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.gcm.GcmHandler;
import com.zazoapp.client.ui.dialogs.*;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

/**
 * Created by skamenkovych@codeminders.com on 11/6/2015.
 */
public class MainFragment extends ZazoFragment implements UnexpectedTerminationHelper.TerminationCallback, VersionHandler.Callback,
        ActionInfoDialogFragment.ActionInfoDialogListener, InviteManager.InviteDialogListener, SelectPhoneNumberDialog.Callbacks,
        DoubleActionDialogFragment.DoubleActionDialogListener, DrawerLayout.DrawerListener, NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainFragment.class.getSimpleName();

    public static final int CONNECTED_DIALOG = 0;
    public static final int NUDGE_DIALOG = 1;
    public static final int SMS_DIALOG = 2;
    public static final int SENDLINK_DIALOG = 3;
    public static final int NO_SIM_DIALOG = 4;

    private static final String TAB = "mf_tab";
    private static final int TAB_MAIN = 0;
    private static final int TAB_FRIENDS = 1;

    private GcmHandler gcmHandler;
    private VersionHandler versionHandler;
    private ManagerHolder managerHolder;
    private Context context;
    private boolean isStopped = true;
    private final BroadcastReceiver serviceReceiver = new MainActivityReceiver();

    private DialogFragment pd;
    private GridViewFragment gridFragment;
    private boolean isNavigationOpened;

    private ZazoPagerAdapter pagerAdapter;

    @InjectView(R.id.action_bar_icon) ImageView actionBarIcon;
    @InjectView(R.id.tabs) TabLayout tabsLayout;
    @InjectView(R.id.navigation_view) NavigationView navigationView;
    @InjectView(R.id.menu_view) MaterialMenuView menuView;
    @InjectView(R.id.drawer_layout) DrawerLayout drawerLayout;
    @InjectView(R.id.content_frame) ViewPager contentFrame;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = TbmApplication.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.main_fragment_layout, null);
        ButterKnife.inject(this, v);
        setupActionBar();
        return v;
    }

    private void setupGrid() {
        pagerAdapter = new ZazoPagerAdapter(getChildFragmentManager());
        contentFrame.setAdapter(pagerAdapter);
        showMainTab();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        gcmHandler = new GcmHandler(getActivity());
        versionHandler = new VersionHandler(context, this);
        managerHolder = (ManagerHolder) TbmApplication.getInstance().getManagerProvider();
        if (managerHolder == null) {
            managerHolder = new ManagerHolder();
        }
        managerHolder.init(context, this, getActivity());
        TbmApplication.getInstance().initManagerProvider(managerHolder);
        TbmApplication.getInstance().addTerminationCallback(this);
        new S3CredentialsGetter(context);
        context.startService(new Intent(context, IntentHandlerService.class));
        setupGrid();
        //if (savedInstanceState != null) { TODO discuss
        //    tabsLayout.getTabAt(savedInstanceState.getInt(TAB, TAB_MAIN)).select();
        //}
    }

    @Override
    public void onStart() {
        super.onStart();
        isStopped = false;
        versionHandler.checkVersionCompatibility();
        checkPlayServices();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Convenience.ON_NOT_ENOUGH_SPACE_ACTION);
        LocalBroadcastManager.getInstance(context).registerReceiver(serviceReceiver, filter);
        Convenience.checkAndNotifyNoSpace(context);
        NotificationAlertManager.init(context);
    }

    @Override
    public void onStop() {
        super.onStop();
        isStopped = true;
        NotificationAlertManager.cleanUp();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(serviceReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        managerHolder.registerManagers();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (managerHolder.getBenchViewManager().isBenchShowed()) {
            managerHolder.getBenchViewManager().hideBench();
        }
        releaseManagers();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(TAB, tabsLayout.getSelectedTabPosition());
        super.onSaveInstanceState(outState);
    }

    private void setupActionBar() {
        actionBarIcon.setOnTouchListener(new ZazoGestureListener(context));
        tabsLayout.addTab(tabsLayout.newTab().setIcon(R.drawable.ic_action_view_as_list), true);
        tabsLayout.addTab(tabsLayout.newTab().setIcon(R.drawable.ic_friends));
        tabsLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case TAB_MAIN:
                        managerHolder.getBenchViewManager().hideBench();
                        break;
                    case TAB_FRIENDS:
                        if (gridFragment != null) {
                            if (managerHolder.getPlayer().isPlaying()) {
                                managerHolder.getPlayer().stop();
                            }
                            if (managerHolder.getRecorder().isRecording()) {
                                tabsLayout.getTabAt(0).select();
                                return;
                            }
                        }
                        managerHolder.getBenchViewManager().showBench();
                        break;
                }
                contentFrame.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        contentFrame.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabsLayout));
        menuView.setState(MaterialMenuDrawable.IconState.BURGER);
        menuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationPanel();
            }
        });
        drawerLayout.setDrawerListener(this);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void releaseManagers() {
        managerHolder.unregisterManagers();
    }

    @Override
    public void onTerminate() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                releaseManagers();
            }
        });
    }

    @Override
    public void showVersionHandlerDialog(String message, boolean negativeButton) {
        if (!isStopped) {
            DialogShower.showVersionHandlerDialog(getActivity(), message, negativeButton);
        }
    }

    private void checkPlayServices() {
        Log.i(TAG, "checkPlayServices");
        if (gcmHandler.checkPlayServices()) {
            gcmHandler.registerGcm();
        } else {
            Dispatch.dispatch("No valid Google Play Services APK found.");
        }
    }

    @Override
    public void onActionClicked(int id, Bundle bundle) {
        switch (id) {
            case CONNECTED_DIALOG:
                managerHolder.getInviteHelper().moveFriendToGrid();
                break;
            case NUDGE_DIALOG:
                managerHolder.getInviteHelper().showSmsDialog();
                break;
            case SMS_DIALOG:
                managerHolder.getInviteHelper().inviteNewFriend();
                break;
            case NO_SIM_DIALOG:
                managerHolder.getInviteHelper().finishInvitation();
                break;
        }
    }

    @Override
    public void onDialogActionClicked(int id, int button, Bundle params) {
        if (id == SENDLINK_DIALOG) {
            switch (button) {
                case BUTTON_POSITIVE:
                    if (params != null) {
                        if (params.getBoolean(SendLinkThroughDialog.SEND_SMS_KEY, false)) {
                            managerHolder.getInviteHelper().sendInvite(AbstractDialogFragment.getEditedMessage(params), getActivity());
                        } else {
                            Intent invite = params.getParcelable(SendLinkThroughDialog.INTENT_KEY);
                            String name = params.getString(SendLinkThroughDialog.APP_NAME_KEY);
                            if (invite != null && !TextUtils.isEmpty(name)) {
                                try {
                                    startActivityForResult(invite, InviteIntent.INVITATION_REQUEST_ID);
                                    managerHolder.getInviteHelper().notifyInviteVector(name, true);
                                } catch (ActivityNotFoundException e) {
                                    managerHolder.getInviteHelper().notifyInviteVector(name, false);
                                }
                            }
                        }
                    }
                    break;
                case BUTTON_NEGATIVE:
                    managerHolder.getInviteHelper().failureNoSimDialog();
                    break;
            }
        }
    }

    @Override
    public void phoneSelected(Contact contact, int phoneIndex) {
        managerHolder.getInviteHelper().invite(contact, phoneIndex);
    }

    @Override
    public void onShowInfoDialog(String title, String msg) {
        if (!isStopped) {
            DialogShower.showInfoDialog(getActivity(), title, msg);
        }
    }

    @Override
    public void onShowActionInfoDialog(String title, String msg, String actionTitle, boolean isNeedCancel, boolean editable, int actionId) {
        if (!isStopped) {
            DialogShower.showActionInfoDialog(getActivity(), title, msg, actionTitle, isNeedCancel, editable, actionId, this);
        }
    }

    @Override
    public void onShowDoubleActionDialog(String title, String msg, String posText, String negText, int id, boolean editable) {
        if (!isStopped) {
            DialogShower.showDoubleActionDialog(getActivity(), title, msg, posText, negText, id, editable, this);
        }
    }

    @Override
    public void onShowSendLinkDialog(int id, String phone, String msg) {
        if (!isStopped) {
            DialogShower.showSendLinkDialog(getActivity(), id, phone, msg, this);
        }
    }

    @Override
    public void onShowProgressDialog(String title, String msg) {
        dismissProgressDialog();
        pd = ProgressDialogFragment.getInstance(title, msg);
        DialogShower.showDialog(getActivity(), pd, null);
    }

    @Override
    public void onShowSelectPhoneNumberDialog(Contact contact) {
        DialogShower.showSelectPhoneNumberDialog(getActivity(), contact, this);
    }

    @Override
    public void onDismissProgressDialog() {
        dismissProgressDialog();
    }

    @Override
    public void onFinishInvitation() {
        showMainTab();
    }

    private void showMainTab() {
        if (getActivity() != null) {
            contentFrame.setCurrentItem(TAB_MAIN, false);
        }
    }

    private void dismissProgressDialog() {
        if (pd != null)
            pd.dismissAllowingStateLoss();
    }

    private void sendFeedback() {
        Bundle bundle = new Bundle();
        bundle.putString(InviteIntent.EMAIL_KEY, "feedback@zazoapp.com");
        bundle.putString(InviteIntent.SUBJECT_KEY, getString(R.string.feedback_subject));
        bundle.putString(InviteIntent.MESSAGE_KEY, "");
        Intent feedback = InviteIntent.EMAIL.getIntent(bundle);
        try {
            startActivity(feedback);
        } catch (ActivityNotFoundException e) {
            DialogShower.showToast(context, R.string.feedback_send_fails);
        }
    }

    private void showEditFriends() {
        FragmentTransaction tr = getFragmentManager().beginTransaction();
        tr.setCustomAnimations(R.anim.slide_left_fade_in, R.anim.slide_right_fade_out, R.anim.slide_left_fade_in, R.anim.slide_right_fade_out);
        tr.add(R.id.top_frame, new ManageFriendsFragment());
        tr.addToBackStack(null);
        tr.commit();
    }

    private void toggleNavigationPanel() {
        if (managerHolder.getPlayer().isPlaying()) {
            managerHolder.getPlayer().stop();
        }
        if (managerHolder.getRecorder().isRecording()) {
            return;
        }
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
        } else {
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    private void showNotEnoughSpaceDialog() {
        DialogShower.showBlockingDialog(getActivity(), R.string.alert_not_enough_space_title, R.string.alert_not_enough_space_message);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_item_edit_friends:
                showEditFriends();
                break;
            case R.id.navigation_item_help:
                sendFeedback();
                break;
            default:
                DialogShower.showToast(context, String.valueOf(item.getTitle()));
                break;
        }
        toggleNavigationPanel();
        return true;
    }

    private class MainActivityReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Convenience.ON_NOT_ENOUGH_SPACE_ACTION.equals(intent.getAction())) {
                showNotEnoughSpaceDialog();
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_MENU:
                toggleNavigationPanel();
                return true;
        }
        return false;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (gridFragment != null) {
            gridFragment.onWindowFocusChanged(hasFocus);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == InviteIntent.INVITATION_REQUEST_ID) {
            managerHolder.getInviteHelper().finishInvitation();
        }
    }

    @Override
    public void onDrawerSlide(View view, float v) {
        if (view.getId() == R.id.navigation_view) {
            menuView.getDrawable().setTransformationOffset(MaterialMenuDrawable.AnimationState.BURGER_ARROW,
                    isNavigationOpened ? 2 - v : v);
        }
    }

    @Override
    public void onDrawerOpened(View view) {
        if (view.getId() == R.id.navigation_view) {
            isNavigationOpened = true;
        }
    }

    @Override
    public void onDrawerClosed(View view) {
        if (view.getId() == R.id.navigation_view) {
            isNavigationOpened = false;
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        if (newState == DrawerLayout.STATE_IDLE) {
            if (isNavigationOpened) {
                menuView.setState(MaterialMenuDrawable.IconState.ARROW);
            } else {
                menuView.setState(MaterialMenuDrawable.IconState.BURGER);
            }
        }
        if (!isNavigationOpened && (newState == DrawerLayout.STATE_DRAGGING || newState == DrawerLayout.STATE_SETTLING)) {
            navigationView.getMenu().findItem(R.id.navigation_item_edit_friends)
                    .setVisible(managerHolder.getFeatures().isUnlocked(Features.Feature.DELETE_FRIEND));
        }
    }

    public class ZazoPagerAdapter extends FragmentPagerAdapter {

        public ZazoPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_MAIN:
                    gridFragment = (GridViewFragment) getChildFragmentManager().findFragmentByTag("grid");
                    if (gridFragment == null) {
                        gridFragment = new GridViewFragment();
                    }
                    return gridFragment;
                case TAB_FRIENDS:
                    Fragment fragment = new ContactsFragment();
                    return fragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

}
