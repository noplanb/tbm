package com.zazoapp.client.ui.dialogs;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.zazoapp.client.R;

/**
 * Created by skamenkovych@codeminders.com on 7/22/2015.
 */
public class FeatureAwardDialogFragment extends DialogFragment implements View.OnClickListener {
    private boolean resumed;
    private boolean showAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.base_dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.feature_unlock_reward_dialog, container, false);
        v.findViewById(R.id.btn_show_me).setOnClickListener(this);
        v.findViewById(R.id.feature_unlock_body).setOnClickListener(this);
        v.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_show_me:
                showAction = true;
                dismissAnimated();
                break;
            case R.id.container:
                dismissAnimated();
                break;
            default:
                break;
        }
    }

    private void dismissAnimated() {
        if (!isHidden() && resumed) {
            resumed = false;
            getFragmentManager().beginTransaction().setCustomAnimations(R.animator.fade_in, R.animator.fade_out).hide(this).commitAllowingStateLoss();
        }
        Log.i("Feature", "" + System.currentTimeMillis());
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, final int nextAnim) {
        if (nextAnim != R.animator.fade_in && nextAnim != R.animator.fade_out) {
            return super.onCreateAnimator(transit, enter, nextAnim);
        }
        Animator anim = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (nextAnim == R.animator.fade_out) {
                    dismissAllowingStateLoss();
                    if (showAction) {
                        FragmentTransaction tr = getFragmentManager().beginTransaction();
                        tr.setCustomAnimations(R.animator.slide_up, R.animator.slide_down);
                        tr.replace(R.id.feature_frame, new NextFeatureDialogFragment(), "featureFrame");
                        tr.commitAllowingStateLoss();
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
