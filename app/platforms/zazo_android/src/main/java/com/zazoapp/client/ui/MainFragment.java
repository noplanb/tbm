package com.zazoapp.client.ui;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuView;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.zazoapp.client.R;
import com.zazoapp.client.bench.BenchController;
import com.zazoapp.client.bench.GeneralContactsGroup;
import com.zazoapp.client.bench.InviteManager;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.core.VersionHandler;
import com.zazoapp.client.debug.ZazoGestureListener;
import com.zazoapp.client.dispatch.Dispatch;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.model.Contact;
import com.zazoapp.client.model.User;
import com.zazoapp.client.model.UserFactory;
import com.zazoapp.client.network.aws.S3CredentialsGetter;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.gcm.GcmHandler;
import com.zazoapp.client.ui.dialogs.AbstractDialogFragment;
import com.zazoapp.client.ui.dialogs.ActionInfoDialogFragment;
import com.zazoapp.client.ui.dialogs.DoubleActionDialogFragment;
import com.zazoapp.client.ui.dialogs.InviteIntent;
import com.zazoapp.client.ui.dialogs.ProgressDialogFragment;
import com.zazoapp.client.ui.dialogs.SelectPhoneNumberDialog;
import com.zazoapp.client.ui.dialogs.SendLinkThroughDialog;
import com.zazoapp.client.ui.helpers.UnexpectedTerminationHelper;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.DialogShower;

/**
 * Created by skamenkovych@codeminders.com on 11/6/2015.
 */
public class MainFragment extends ZazoFragment implements UnexpectedTerminationHelper.TerminationCallback, VersionHandler.Callback,
        ActionInfoDialogFragment.ActionInfoDialogListener, InviteManager.InviteDialogListener, SelectPhoneNumberDialog.Callbacks,
        DoubleActionDialogFragment.DoubleActionDialogListener, DrawerLayout.DrawerListener, NavigationView.OnNavigationItemSelectedListener, BenchController.BenchListener {

    private static final String TAG = MainFragment.class.getSimpleName();

    public static final int CONNECTED_DIALOG = 0;
    public static final int NUDGE_DIALOG = 1;
    public static final int SMS_DIALOG = 2;
    public static final int SENDLINK_DIALOG = 3;
    public static final int NO_SIM_DIALOG = 4;

    private static final String TAB = "mf_tab";
    private static final int TAB_MAIN = 0;
    private static final int TAB_FRIENDS = 1;
    private static final String EXTRA_HANDLED = "extra_handled";

    private GcmHandler gcmHandler;
    private VersionHandler versionHandler;
    private ManagerHolder managerHolder;
    private Context context;
    private boolean isStopped = true;
    private final BroadcastReceiver serviceReceiver = new MainActivityReceiver();

    private DialogFragment pd;
    private GridViewFragment gridFragment;
    private ContactsFragment contactsFragment;

    private boolean isNavigationOpened;

    private ZazoPagerAdapter pagerAdapter;

    private ZazoTopFragment topFragment;

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
        handleIntent(getActivity().getIntent());
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            // TODO Do action specified in https://zazo.fogbugz.com/f/cases/1062/
            intent.setAction(IntentHandlerService.IntentActions.NONE);
        } else if (IntentHandlerService.IntentActions.SUGGESTIONS.equals(intent.getAction())) {
            if (!intent.getBooleanExtra(EXTRA_HANDLED, false)) {
                if (topFragment != null) {
                    getFragmentManager().popBackStack();
                    topFragment = null;
                }
                showSuggestions(intent);
                intent.putExtra(EXTRA_HANDLED, true);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (managerHolder.getBenchViewManager().isBenchShown()) {
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
        tabsLayout.addTab(tabsLayout.newTab().setIcon(R.drawable.tab_grid), true);
        tabsLayout.addTab(tabsLayout.newTab().setIcon(R.drawable.tab_contacts));
        tabsLayout.setOnTabSelectedListener(new MyOnTabSelectedListener());
        contentFrame.addOnPageChangeListener(new MyOnPageChangeListener(tabsLayout));
        menuView.setState(MaterialMenuDrawable.IconState.BURGER);
        menuView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationPanel();
            }
        });
        drawerLayout.setDrawerListener(this);
        navigationView.setNavigationItemSelectedListener(this);

        TextView accountName = ButterKnife.findById(navigationView.getHeaderView(0), R.id.account_name);
        TextView accountId = ButterKnife.findById(navigationView.getHeaderView(0), R.id.account_id);
        User user = UserFactory.current_user();
        if (user != null) {
            accountName.setText(user.getFullName());
            accountId.setText(user.getPhoneNumber(PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL));
        } else {
            accountName.setText("");
            accountId.setText("");
        }
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
        DialogShower.showDialog(getActivity().getSupportFragmentManager(), pd, null);
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
        managerHolder.getBenchViewManager().hideBench();
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
        showTopFragment(new ManageFriendsFragment(), R.anim.slide_left_fade_in, R.anim.slide_right_fade_out);
    }

    private void showSettings() {
        showTopFragment(new SettingsFragment(), R.anim.slide_left_fade_in, R.anim.slide_right_fade_out);
    }

    private void showSuggestions(@Nullable Intent intent) {
        showTopFragment(SuggestionsFragment.getInstance(intent), R.anim.slide_bottom_fade_in, R.anim.slide_bottom_fade_out);
    }

    private void showTopFragment(ZazoTopFragment f, @AnimRes int in, @AnimRes int out) {
        FragmentTransaction tr = getFragmentManager().beginTransaction();
        tr.setCustomAnimations(in, out, in, out);
        tr.add(R.id.top_frame, f);
        tr.addToBackStack(null);
        tr.commit();
        f.setOnBackListener(new ZazoTopFragment.OnBackListener() {
            @Override
            public void onBack() {
                topFragment = null;
            }
        });
        topFragment = f;
    }

    private void inviteFriends() {
        BenchController bench = managerHolder.getBenchViewManager();
        bench.showBench();
        bench.selectGroup(GeneralContactsGroup.CONTACTS);
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
            case R.id.navigation_item_settings:
                showSettings();
                break;
            case R.id.navigation_item_invite_friends:
                inviteFriends();
                break;
            case R.id.navigation_item_contacts:
                selectTab(TAB_FRIENDS);
                break;
            case R.id.navigation_item_suggestions:
                showSuggestions(null);
                break;
            default:
                DialogShower.showToast(context, String.valueOf(item.getTitle()));
                break;
        }
        toggleNavigationPanel();
        return true;
    }

    @Override
    public void onBenchStateChangeRequest(boolean visible) {
        int newTabId = visible ? TAB_FRIENDS : TAB_MAIN;
        if (newTabId != tabsLayout.getSelectedTabPosition()) {
            selectTab(newTabId);
        } else {
            managerHolder.getBenchViewManager().setBenchShown(visible);
        }
    }

    private void selectTab(int index) {
        TabLayout.Tab tab = tabsLayout.getTabAt(index);
        if (tab != null) {
            tab.select();
        }
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
        if (topFragment != null) {
            return topFragment.onKeyDown(keyCode, event);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                toggleNavigationPanel();
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (managerHolder.getTutorial().isShown()) {
                    managerHolder.getTutorial().dismissHint();
                    return true;
                }
                if (managerHolder.getBenchViewManager().isBenchShown()) {
                    boolean result = tabsLayout.getSelectedTabPosition() != TAB_MAIN;
                    managerHolder.getBenchViewManager().hideBench();
                    return result;
                }
                break;
        }
        return false;
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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (gridFragment != null) {
            gridFragment.onWindowFocusChanged(hasFocus);
        }
        if (hasFocus) {
            handleIntent(getActivity().getIntent());
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
                    gridFragment = new GridViewFragment();
                    return gridFragment;
                case TAB_FRIENDS:
                    contactsFragment = new ContactsFragment();
                    return contactsFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    private class MyOnTabSelectedListener implements TabLayout.OnTabSelectedListener {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            switch (tab.getPosition()) {
                case TAB_FRIENDS:
                    if (gridFragment != null) {
                        if (managerHolder.getPlayer().isPlaying()) {
                            managerHolder.getPlayer().stop();
                        }
                        if (managerHolder.getRecorder().isRecording()) {
                            selectTab(TAB_MAIN);
                            return;
                        }
                    }
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
    }

    private class MyOnPageChangeListener extends TabLayout.TabLayoutOnPageChangeListener {
        public MyOnPageChangeListener(TabLayout tabLayout) {
            super(tabLayout);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                managerHolder.getBenchViewManager().setBenchShown(tabsLayout.getSelectedTabPosition() == TAB_FRIENDS);
            }
        }
    }

}
