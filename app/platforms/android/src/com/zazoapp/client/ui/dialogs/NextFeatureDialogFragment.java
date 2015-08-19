package com.zazoapp.client.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.MainActivity;
import com.zazoapp.client.ui.ZazoManagerProvider;

import java.util.Random;

/**
 * Created by skamenkovych@codeminders.com on 7/22/2015.
 */
public class NextFeatureDialogFragment extends DialogFragment implements View.OnClickListener, View.OnTouchListener {

    private boolean resumed;

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
        Random random = new Random();
        String[] messageList = getResources().getStringArray(R.array.unlock_another_feature);
        message.setText(messageList[Math.abs(random.nextInt(messageList.length))]);
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
                ((MainActivity) getActivity()).getBenchViewManager().showBench();
                break;
        }
    }

    private void dismissAnimated() {
        if (!isHidden() && resumed) {
            getFragmentManager().beginTransaction().setCustomAnimations(R.animator.slide_up, R.animator.slide_down).hide(this).commitAllowingStateLoss();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        dismissAnimated();
        return false;
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, final int nextAnim) {
        if (nextAnim != R.animator.slide_up && nextAnim != R.animator.slide_down) {
            return super.onCreateAnimator(transit, enter, nextAnim);
        }
        Animator anim = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (nextAnim == R.animator.slide_down) {
                    dismissAllowingStateLoss();
                    ZazoManagerProvider managers = (ZazoManagerProvider) getActivity();
                    if (managers != null) {
                        managers.getTutorial().onDismiss();
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return anim;
    }
}
