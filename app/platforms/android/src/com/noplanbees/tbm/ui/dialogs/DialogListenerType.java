package com.noplanbees.tbm.ui.dialogs;

import android.app.Activity;
import android.app.Fragment;

/**
* Created by skamenkovych@codeminders.com on 2/12/2015.
*/
public enum DialogListenerType {
    ACTIVITY(Activity.class),
    FRAGMENT(Fragment.class),
    NONE(Object.class);

    private Class clazz;

    DialogListenerType(Class clazz) {
        this.clazz = clazz;
    }

    public static DialogListenerType getType(AbstractDialogFragment.DialogListener listener) {
        for (DialogListenerType type : values()) {
            if (type.clazz.isInstance(listener)) {
                return type;
            }
        }
        return NONE;
    }
}
