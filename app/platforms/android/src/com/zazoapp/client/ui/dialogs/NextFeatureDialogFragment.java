package com.zazoapp.client.ui.dialogs;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.ui.ZazoManagerProvider;
import com.zazoapp.client.ui.animations.SlideVerticalAnimation;

import java.util.Random;

/**
 * Created by skamenkovych@codeminders.com on 7/22/2015.
 */
public class NextFeatureDialogFragment extends DialogFragment implements View.OnClickListener, View.OnTouchListener {

    private boolean resumed;

    private static final String AFTER_FEATURE_UNLOCK = "after_feature_unlock";

    public static NextFeatureDialogFragment getInstance(boolean justAfterFeatureUnlock) {
        NextFeatureDialogFragment d = new NextFeatureDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(AFTER_FEATURE_UNLOCK, justAfterFeatureUnlock);
        d.setArguments(args);
        return d;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.base_dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.feature_unlock_another, container, false);
        v.findViewById(R.id.body).setOnClickListener(this);
        v.setOnTouchListener(this);
        TextView message = ButterKnife.findById(v, R.id.message);
        String[] messageList = getResources().getStringArray(R.array.unlock_another_feature);
        if (getArguments() != null && getArguments().getBoolean(AFTER_FEATURE_UNLOCK, false)) {
            message.setText(messageList[0]);
        } else {
            Random random = new Random();
            message.setText(messageList[Math.abs(random.nextInt(messageList.length))]);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismissAnimated();
            }
        }, 5000);
    }

    @Override
    public void onPause() {
        super.onPause();
        resumed = false;
        dismiss();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.body:
                dismiss();
                TbmApplication.getInstance().getManagerProvider().getBenchViewManager().showBench();
                break;
        }
    }

    private void dismissAnimated() {
        if (!isHidden() && resumed) {
            getFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_up, R.anim.slide_down).hide(this).commitAllowingStateLoss();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        dismissAnimated();
        return false;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, final int nextAnim) {
        if (nextAnim != R.anim.slide_up && nextAnim != R.anim.slide_down) {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
        Animation anim = SlideVerticalAnimation.get(getActivity(), nextAnim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (nextAnim == R.anim.slide_down) {
                    dismissAllowingStateLoss();
                    ZazoManagerProvider managers = TbmApplication.getInstance().getManagerProvider();
                    if (managers != null) {
                        managers.getTutorial().onDismiss();
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        return anim;
    }

}
