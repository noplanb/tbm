package com.zazoapp.client.ui.view.transferview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.view.ITransferView;
import com.zazoapp.client.ui.view.transferview.animation.UploadAnimationController;
import com.zazoapp.client.ui.view.transferview.background.IBackground;
import com.zazoapp.client.ui.view.transferview.background.UploadingBackground;

/**
 * Created by sergii on 21.11.15.
 */
public class UploadingView extends ITransferView {
    private static final float IMAGE_SIZE_COEF = 0.6f;
    private UploadAnimationController animationController;
    private UploadingBackground background;
    private ImageView uploadImageView;

    public UploadingView(Context context) {
        super(context);
    }

    public UploadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(attrs, 0);
    }

    public UploadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(attrs, defStyleAttr);
    }

    private void initAttributes(AttributeSet attrs, int defStyle) {
        final TypedArray typedArray = getContext().obtainStyledAttributes(
                attrs, R.styleable.UploadingView, defStyle, 0);

        animationController = new UploadAnimationController();

        background = createBackground(typedArray);
        addView(background);
        animationController.setFirstRingView(background);
        animationController.setSecondRingView(background);
        animationController.setOpacityChangeColor(background);

        uploadImageView = createIconView( typedArray );
        uploadImageView.setVisibility(INVISIBLE);
        addView(uploadImageView);
        animationController.setSpiralView( uploadImageView );

        typedArray.recycle();
    }

    private ImageView createIconView(TypedArray typedArray) {

        Drawable drawable = typedArray.getDrawable(R.styleable.UploadingView_upload_icon_src);
        if ( drawable == null ){
            drawable = getResources().getDrawable(R.drawable.upload_icon_src_default);
        }

        final ImageView imageView = new ImageView(getContext());
        imageView.setImageDrawable(drawable);

        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        imageView.setLayoutParams(params);
        return imageView;
    }

    private UploadingBackground createBackground(TypedArray typedArray) {
        final int bgCircleColor = typedArray.getColor(R.styleable.UploadingView_upload_bg_color,
                getResources().getColor(R.color.upload_bg_color_default));

        final int bgRingColorFirst = typedArray.getColor(R.styleable.UploadingView_upload_bg_ring_color_first,
                getResources().getColor(R.color.upload_bg_ring_color_first_default));

        final int bgRingColorSecond = typedArray.getColor(R.styleable.UploadingView_upload_bg_ring_color_second,
                getResources().getColor(R.color.upload_bg_ring_color_second_default));

        final float bgThickness = typedArray.getDimension(R.styleable.UploadingView_upload_ring_thickness,
                getResources().getDimension(R.dimen.upload_ring_thickness_default));

        UploadingBackground background = new UploadingBackground( getContext() );
        background.setCircleColor(bgCircleColor);
        background.setFirstRingColor(bgRingColorFirst);
        background.setSecondRingColor(bgRingColorSecond);
        background.setRingThickness(bgThickness);
        background.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        return background;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateUploadImageViewSize();
    }

    private void updateUploadImageViewSize() {
        ViewGroup.LayoutParams value = uploadImageView.getLayoutParams();
        value.width = (int) (getWidth()*IMAGE_SIZE_COEF);
        value.height = (int) (getHeight()*IMAGE_SIZE_COEF);
    }

    public IBackground getUploadBackground() {
        return background;
    }

    @Override
    public UploadAnimationController getAnimationController() {
        return animationController;
    }
}
