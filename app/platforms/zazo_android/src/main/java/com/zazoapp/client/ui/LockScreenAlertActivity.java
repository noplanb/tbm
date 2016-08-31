package com.zazoapp.client.ui;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.view.ContextThemeWrapper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.zazoapp.client.R;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.Settings;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.model.IncomingMessage;
import com.zazoapp.client.model.IncomingMessageFactory;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.NotificationSuggestion;
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.ui.helpers.UiUtils;
import com.zazoapp.client.ui.view.CircleThumbView;
import com.zazoapp.client.ui.view.ThumbView;
import com.zazoapp.client.utilities.Convenience;
import com.zazoapp.client.utilities.StringUtils;

public class LockScreenAlertActivity extends FragmentActivity {

	private static final String TAG = LockScreenAlertActivity.class.getSimpleName();

    private boolean isStopped = true;
    @InjectView(R.id.title_text) TextView titleText;
    @InjectView(R.id.subtitle_text) TextView subtitleText;
    @InjectView(R.id.logoImage) ImageView smallIconView;
    @InjectView(R.id.thumbImage) ImageView thumbImage;
    @InjectView(R.id.thumb_title) TextView thumbTitle;
    @InjectView(R.id.action_main_btn) Button mainButton;
    @InjectView(R.id.action_second_btn) Button secondButton;
    @InjectView(R.id.action_third_btn) Button thirdButton;

    private ThumbsHelper tHelper;
    private ZazoTopFragment topFragment;

    public static final int MAIN_VIEW = 0;
    public static final int TEXT_VIEW = 1;
    public static final String VIEW_TYPE = "viewType";
	//-------------------
	// Activity lifecycle
	//-------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate");
        tHelper = new ThumbsHelper(this);
        setupWindow();
        setContentView(R.layout.lock_screen_alert);
        setupViews(getIntent(), true);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i(TAG, "onRestart");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i(TAG, "onNewIntent");
        setIntent(intent);
        if (isStopped) {
            recreate();
        } else {
            setupViews(intent, false);
        }
    }

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause");
		super.onPause();
	}

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
        isStopped = true;
    }

	@Override
	protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
	}

	@Override
	protected void onStart() {
		super.onStart();
        isStopped = false;
        Log.i(TAG, "onStart");
	}

	@SuppressLint("NewApi")
	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		Log.i(TAG, "isKeyGuardRestrictedInputMode=" + ((Boolean) km.inKeyguardRestrictedInputMode()).toString());
		Log.i(TAG, "isKeyguardLocked=" + ((Boolean) km.isKeyguardLocked()).toString());
		Log.i(TAG, "isKeyguardSecure=" + ((Boolean) km.isKeyguardSecure()).toString());
	}

	private void setupWindow(){
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
        LayoutParams lp = getWindow().getAttributes();
        if (Settings.Bool.LIGHT_SCREEN_FOR_NOTIFICATIONS.isSet()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        }
        lp.type = WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG;
        getWindow().setAttributes(lp);
    }

    private void setupViews(Intent i, boolean force) {
        int type = i.getIntExtra(VIEW_TYPE, MAIN_VIEW);
        boolean textType = type == TEXT_VIEW;
        ViewAnimator viewAnimator = ButterKnife.findById(this, R.id.view_animator);
        if (type != viewAnimator.getDisplayedChild()) {
            if (textType) {
                viewAnimator.showNext();
                View currentView = viewAnimator.getCurrentView();
                ButterKnife.inject(this, currentView);
                UiUtils.applyTint(secondButton, R.color.suggestions_btn_tint);
                UiUtils.applyTint(mainButton, R.color.suggestions_btn_tint);
            } else {
                viewAnimator.showNext();
                View currentView = viewAnimator.getCurrentView();
                ButterKnife.inject(this, currentView);
            }
        } else if (force) {
            View currentView = viewAnimator.getCurrentView();
            ButterKnife.inject(this, currentView);
            viewAnimator.setDisplayedChild(type);
            if (textType) {
                UiUtils.applyTint(secondButton, R.color.suggestions_btn_tint);
                UiUtils.applyTint(mainButton, R.color.suggestions_btn_tint);
            }
        }
        if (!textType) {
            titleText.setText(i.getStringExtra(NotificationAlertManager.TITLE_KEY));
            subtitleText.setText(i.getStringExtra(NotificationAlertManager.SUB_TITLE_KEY));
        }

        int smallIconId = i.getIntExtra(NotificationAlertManager.SMALL_ICON_KEY, 0);
        if (smallIconId != 0) {
            smallIconView.setBackgroundResource(R.drawable.ic_zazo_blue);
        }
        if (IntentHandlerService.IntentActions.NEW_MESSAGE.equals(i.getAction())) {
            Friend friend = FriendFactory.getFactoryInstance().getFriendFromIntent(i);
            IncomingMessage message = IncomingMessageFactory.getFactoryInstance().find(i.getStringExtra(NotificationAlertManager.MESSAGE_ID_KEY));
            if (friend == null || message == null) {
                dismiss();
                return;
            }
            if (textType) {
                titleText.setText(friend.getFullName());
                subtitleText.setText(Convenience.getTextFromFile(Friend.File.IN_TEXT.getPath(friend, message.getId())));
                subtitleText.setVerticalScrollBarEnabled(true);
                subtitleText.setMovementMethod(new ScrollingMovementMethod());
            }
            if (friend.thumbExists()) {
                thumbImage.setImageBitmap(friend.thumbBitmap());
                if (!textType) {
                    ((ThumbView) thumbImage).setMapArea(ThumbView.MapArea.FULL);
                }
                thumbTitle.setText("");
            } else {
                thumbImage.setImageResource(R.drawable.navigation_background_pattern);
                if (textType) {
                    ((CircleThumbView) thumbImage).setFillColor(tHelper.getColor(friend.getDisplayName()));
                } else {
                    ((ThumbView) thumbImage).setFillColor(tHelper.getColor(friend.getDisplayName()));
                    ((ThumbView) thumbImage).setMapArea(tHelper.getMapArea(friend.getDisplayName()));
                }

                thumbTitle.setText(friend.getInitials());
            }
            if (!textType) {
                secondButton.setText(R.string.action_dismiss);
                mainButton.setText(R.string.action_view);
                thirdButton.setText("");
                thirdButton.setVisibility(View.INVISIBLE);
            }

        } else if (IntentHandlerService.IntentActions.FRIEND_JOINED.equals(i.getAction())) {
            NotificationSuggestion suggestion = i.getParcelableExtra(IntentHandlerService.FriendJoinedIntentFields.DATA);
            String name = suggestion.getName();
            if (name == null) {
                name = "";
            }
            thumbImage.setImageResource(R.drawable.navigation_background_pattern);
            ((ThumbView) thumbImage).setMapArea(tHelper.getMapArea(name));
            ((ThumbView) thumbImage).setFillColor(tHelper.getColor(name));
            thumbTitle.setText(StringUtils.getInitials(name));
            mainButton.setText(R.string.action_add_joined_friend);
            secondButton.setText(R.string.action_ignore_joined_friend);
            thirdButton.setText(R.string.action_unsubscribe);
            thirdButton.setVisibility(View.VISIBLE);
        }
	}


	private void startHomeActivity(){
		Intent i = new Intent(this, MainActivity.class);
		i.setAction(getIntent().getAction());
		i.setData(getIntent().getData());
		Log.i(TAG, "startHomeActivity: ");
		// FLAG_DISMISS_KEYGUARD is put here rather than in setupWindow becuase of an Android bug I found and reported: 
		// SingleInstance type activity with layoutParams FLAG_DISMISS_KEYGUARD does not receive onNewIntent callback.
		// https://code.google.com/p/android/issues/detail?id=72242&thanks=72242&ts=1403540783
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		startActivity(i);
		finish();
	}
	
	private void dismiss(){
		// If you really want to know. This moveTaskToBack is so that after dismissing this activity if the user decides to unlock his screen 
		// or if he doesnt have a security lock he doesnt see us on top and launched when he opens the phone.
		moveTaskToBack(true); 
		finish();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		dismiss();
	}

    @OnClick({R.id.action_main_btn, R.id.action_second_btn, R.id.action_third_btn})
    public void onClick(View v) {
        final boolean textType = getIntent().getIntExtra(VIEW_TYPE, MAIN_VIEW) == TEXT_VIEW;
        if (!textType) {
            switch (v.getId()) {
                case R.id.action_main_btn:
                    if (isFriendJoinedIntent()) {
                        final NotificationSuggestion suggestion = getIntent().getParcelableExtra(IntentHandlerService.FriendJoinedIntentFields.DATA);
                        if (suggestion != null && suggestion.hasMultiplePhones()) {
                            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(v.getContext(), R.style.AppTheme);
                            SuggestionsFragment.displayPhoneChooserPopup(new SuggestionsAdapter.OnPhoneItemSelected() {
                                @Override
                                public void onPhoneItemSelected(int index) {
                                    Intent intent = getIntent();
                                    intent.putExtra(IntentHandlerService.FriendJoinedIntentFields.CHOSEN_PHONE, suggestion.getPhone(index));
                                    openSuggestions(IntentHandlerService.FriendJoinedActions.ADD);
                                }
                            }, suggestion, v, contextThemeWrapper);
                        } else {
                            openSuggestions(IntentHandlerService.FriendJoinedActions.ADD);
                        }
                    } else {
                        Intent i = getIntent();
                        Friend friend = FriendFactory.getFactoryInstance().getFriendFromIntent(i);
                        Intent viewIntent = new Intent(i);
                        viewIntent.setClass(this, MainActivity.class);
                        String action = IntentHandlerService.IntentActions.PLAY_VIDEO;
                        viewIntent.setAction(action);
                        Uri uri = new Uri.Builder().appendPath(action).appendQueryParameter(
                                IntentHandlerService.IntentParamKeys.FRIEND_ID, friend.getId()).build();
                        viewIntent.setData(uri);
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                        startActivity(viewIntent);
                    }
                    break;
                case R.id.action_second_btn:
                    if (isFriendJoinedIntent()) {
                        openSuggestions(IntentHandlerService.FriendJoinedActions.IGNORE);
                    } else {
                        dismiss();
                    }
                    break;
                case R.id.action_third_btn:
                    if (isFriendJoinedIntent()) {
                        openSuggestions(IntentHandlerService.FriendJoinedActions.UNSUBSCRIBE);
                    } else {
                        dismiss();
                    }
                    break;
            }
        } else {
            switch (v.getId()) {
                case R.id.action_main_btn: {
                    Intent i = getIntent();
                    Friend friend = FriendFactory.getFactoryInstance().getFriendFromIntent(i);
                    IncomingMessage message = IncomingMessageFactory.getFactoryInstance().find(i.getStringExtra(NotificationAlertManager.MESSAGE_ID_KEY));
                    if (friend == null || message == null) {
                        dismiss();
                        break;
                    }
                    Intent replyIntent = new Intent(getIntent());
                    replyIntent.setClass(this, MainActivity.class);
                    String action = IntentHandlerService.IntentActions.ZAZO_REPLY;
                    replyIntent.setAction(action);
                    Uri uri = new Uri.Builder().appendPath(action).appendQueryParameter(
                            IntentHandlerService.IntentParamKeys.FRIEND_ID, friend.getId()).build();
                    replyIntent.setData(uri);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                    startActivity(replyIntent);
                    dismiss();
                }
                case R.id.action_second_btn: {
                    Friend friend = FriendFactory.getFactoryInstance().getFriendFromIntent(getIntent());
                    final ChatFragment fragment = ChatFragment.getInstance(friend);
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
                    transaction.addToBackStack(null);
                    transaction.add(R.id.top_frame, fragment);
                    transaction.commitAllowingStateLoss();
                    fragment.setOnBackListener(new ZazoTopFragment.OnBackListener() {
                        @Override
                        public void onBack() {
                            if (fragment.isTaskCompleted()) {
                                NotificationAlertManager.cancelNativeAlert(getApplicationContext(), NotificationAlertManager.NotificationType.NEW_VIDEO.id());
                                dismiss();
                            }
                            if (fragment == topFragment) {
                                topFragment = null;
                            }
                        }
                    });
                    topFragment = fragment;
                }
                    break;
                case R.id.action_third_btn:
                    dismiss();
                    break;
            }
        }
    }

    private boolean isFriendJoinedIntent() {
        return IntentHandlerService.IntentActions.FRIEND_JOINED.equals(getIntent().getAction());
    }

    private void openSuggestions(String action) {
        Intent i = NotificationAlertManager.makeSuggestionIntent(getIntent(), action);
        i.setClass(this, MainActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (topFragment != null) {
            return topFragment.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }
}
