package com.zazoapp.client.ui.dialogs;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.core.TbmApplication;
import com.zazoapp.client.features.Features;
import com.zazoapp.client.ui.ZazoManagerProvider;

/**
 * Created by skamenkovych@codeminders.com on 7/22/2015.
 */
public class FeatureAwardDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String FEATURE = "feature";

    private boolean resumed;
    private boolean showAction;
    private Features.Feature feature;

    public static DialogFragment getInstance(Features.Feature feature) {
        FeatureAwardDialogFragment f = new FeatureAwardDialogFragment();
        Bundle args = new Bundle();
        args.putInt(FEATURE, feature.ordinal());
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.base_dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.feature_unlock_reward_dialog, container, false);
        feature = Features.Feature.values()[getArguments().getInt(FEATURE, 0)];
        TextView textView = ButterKnife.findById(v, R.id.hint_message);
        textView.setText(feature.getAction(getActivity()));
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
            getFragmentManager().beginTransaction().setCustomAnimations(R.anim.fade_in, R.anim.fade_out).hide(this).commitAllowingStateLoss();
        }
        Log.i("Feature", "" + System.currentTimeMillis());
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
        resumed = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        resumed = true;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, final int nextAnim) {
        if (nextAnim != R.anim.fade_in && nextAnim != R.anim.fade_out) {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }
        Animation anim = AnimationUtils.loadAnimation(getActivity(), nextAnim);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (nextAnim == R.anim.fade_out) {
                    dismissAllowingStateLoss();
                    ZazoManagerProvider managers = TbmApplication.getInstance().getManagerProvider();
                    if (managers != null) {
                        managers.getTutorial().onFeatureAwardDialogHidden();
                    }
//                    if (showAction) {
//
//                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        return anim;
    }

    public boolean isShown() {
        return resumed;
    }

}
