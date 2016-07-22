package com.zazoapp.client.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.zazoapp.client.R;
import com.zazoapp.client.core.IntentHandlerService;
import com.zazoapp.client.core.Settings;
import com.zazoapp.client.model.Friend;
import com.zazoapp.client.model.FriendFactory;
import com.zazoapp.client.notification.NotificationAlertManager;
import com.zazoapp.client.notification.NotificationSuggestion;
import com.zazoapp.client.ui.helpers.ThumbsHelper;
import com.zazoapp.client.ui.view.ThumbView;
import com.zazoapp.client.utilities.StringUtils;

public class LockScreenAlertActivity extends Activity {

	private static final String TAG = LockScreenAlertActivity.class.getSimpleName();

    private boolean isStopped = true;
    @InjectView(R.id.title_text) TextView titleText;
    @InjectView(R.id.subtitle_text) TextView subtitleText;
    @InjectView(R.id.logoImage) ImageView smallIconView;
    @InjectView(R.id.thumbImage) ThumbView thumbImage;
    @InjectView(R.id.thumb_title) TextView thumbTitle;
    @InjectView(R.id.action_main_btn) Button mainButton;
    @InjectView(R.id.action_second_btn) Button secondButton;
    @InjectView(R.id.action_third_btn) Button thirdButton;
    @InjectView(R.id.message_info_layout) View messageInfoLayout;

    private ThumbsHelper tHelper;
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
        ButterKnife.inject(this);
        setupViews(getIntent());
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
            setupViews(intent);
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

    private void setupViews(Intent i) {
        titleText.setText(i.getStringExtra(NotificationAlertManager.TITLE_KEY));
        subtitleText.setText(i.getStringExtra(NotificationAlertManager.SUB_TITLE_KEY));
        int smallIconId = i.getIntExtra(NotificationAlertManager.SMALL_ICON_KEY, 0);
        if (smallIconId != 0) {
            smallIconView.setBackgroundResource(R.drawable.ic_zazo_blue);
        }
        if (IntentHandlerService.IntentActions.PLAY_VIDEO.equals(i.getAction())) {
            Friend friend = FriendFactory.getFactoryInstance().getFriendFromIntent(i);
            if (friend == null) {
                dismiss();
                return;
            }
            if (friend.thumbExists()) {
                thumbImage.setImageBitmap(friend.thumbBitmap());
                thumbImage.setMapArea(ThumbView.MapArea.FULL);
                thumbTitle.setText("");
            } else {
                thumbImage.setImageResource(R.drawable.navigation_background_pattern);
                thumbImage.setFillColor(tHelper.getColor(friend.getDisplayName()));
                thumbImage.setMapArea(tHelper.getMapArea(friend.getDisplayName()));
                thumbTitle.setText(friend.getInitials());
            }
            secondButton.setText(R.string.action_dismiss);
            mainButton.setText(R.string.action_view);
            thirdButton.setText("");
            thirdButton.setVisibility(View.INVISIBLE);
        } else if (IntentHandlerService.IntentActions.FRIEND_JOINED.equals(i.getAction())) {
            NotificationSuggestion suggestion = i.getParcelableExtra(IntentHandlerService.FriendJoinedIntentFields.DATA);
            String name = suggestion.getName();
            if (name == null) {
                name = "";
            }
            thumbImage.setImageResource(R.drawable.navigation_background_pattern);
            thumbImage.setMapArea(tHelper.getMapArea(name));
            thumbImage.setFillColor(tHelper.getColor(name));
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
                    startHomeActivity();
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
}
