package com.zazoapp.client.ui.dialogs;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import butterknife.ButterKnife;
import com.zazoapp.client.R;
import com.zazoapp.client.ui.helpers.ContactsManager;
import com.zazoapp.client.utilities.DialogShower;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SendLinkThroughDialog extends DoubleActionDialogFragment implements RadioGroup.OnCheckedChangeListener {

    public static final String INTENT_KEY = "intent";
    public static final String SEND_SMS_KEY = "send_sms";
    public static final String APP_NAME_KEY = "application_name";
    private String phoneNumber;
    private Set<String> emails;
    private RadioGroup radioGroup;
    private AppInfo selectedAppInfo;

    public static DialogFragment getInstance(int id, String phoneNumber, Context context, String message, DialogListener listener) {
        String title = context.getString(R.string.dialog_invite_sms_title);
        String posText = context.getString(R.string.dialog_invite_sms_action);
        String negText = context.getString(R.string.dialog_action_cancel);
        SendLinkThroughDialog f = new SendLinkThroughDialog();
        DoubleActionDialogFragment.putData(id, title, message, posText, negText, true, listener, f);
        f.getArguments().putString(InviteIntent.PHONE_NUMBER_KEY, phoneNumber);
        return f;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        phoneNumber = getArguments().getString(InviteIntent.PHONE_NUMBER_KEY);
        radioGroup = (RadioGroup) View.inflate(getActivity(), R.layout.send_through_layout, null);
        radioGroup.setOnCheckedChangeListener(this);
        radioGroup.check(R.id.send_through_sms);
        setCustomView(radioGroup);
    }

    @Override
    public void onCheckedChanged(final RadioGroup group, int checkedId) {
        ButterKnife.findById(getView(), R.id.btn_dialog_ok).setEnabled(
                positiveButtonMightBeEnabled() && !getEditedMessage().isEmpty());
        if (checkedId == R.id.send_through_other && (((RadioButton) group.findViewById(R.id.send_through_other)).isChecked())) {
            List<AppInfo> apps = getApplications(getActivity().getApplicationContext(), getIntentBundle());
            if (apps.isEmpty()) {
                DialogShower.showToast(getActivity(), R.string.no_supported_app_available);
                group.check(R.id.send_through_sms);
                return;
            }
            final ListPopupWindow popupWindow = new ListPopupWindow(getActivity());
            final RadioButton otherButton = ButterKnife.findById(group, R.id.send_through_other);
            popupWindow.setAdapter(new ApplicationAdapter(getActivity(), apps));
            popupWindow.setAnchorView(otherButton);
            popupWindow.setModal(true); // to dismiss only popup on touch outside
            popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (selectedAppInfo == null) {
                        group.check(R.id.send_through_sms);
                        DialogShower.showToast(getActivity(), R.string.no_app_selected);
                    }
                }
            });
            popupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    selectedAppInfo = (AppInfo) parent.getItemAtPosition(position);
                    otherButton.setText(getString(R.string.dialog_invite_send_through_other_selected, selectedAppInfo.loadName(getActivity())));
                    popupWindow.dismiss();
                }
            });
            popupWindow.show();
        }
    }

    @Override
    protected void doPositiveAction() {
        if (getListener() instanceof DoubleActionDialogListener) {
            DoubleActionDialogListener listener = ((DoubleActionDialogListener) getListener());
            Bundle bundle = new Bundle();
            if (radioGroup.getCheckedRadioButtonId() == R.id.send_through_sms) {
                putEditedMessage(bundle, getEditedMessage());
                bundle.putBoolean(SEND_SMS_KEY, true);
            } else {
                ComponentName name = new ComponentName(selectedAppInfo.getPackageName(), selectedAppInfo.getClassName());
                Intent invite = InviteIntent.TEXT.getIntent(getIntentBundle());
                invite.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                invite.setComponent(name);
                bundle.putParcelable(INTENT_KEY, invite);
                bundle.putString(APP_NAME_KEY, selectedAppInfo.getSystemName());
            }
            listener.onDialogActionClicked(getDialogId(), DoubleActionDialogListener.BUTTON_POSITIVE, bundle);
        }
    }

    @Override
    protected boolean positiveButtonMightBeEnabled() {
        return radioGroup != null && radioGroup.getCheckedRadioButtonId() != -1;
    }

    private static List<AppInfo> getApplications(Context context, Bundle data) {
        List<AppInfo> infos = new ArrayList<>();
        List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(InviteIntent.TEXT.getIntent(data), 0);
        Iterator<ResolveInfo> i = apps.iterator();
        String[] supportedApps = context.getResources().getStringArray(R.array.supported_invite_apps);
        String[] supportedAppNames = context.getResources().getStringArray(R.array.supported_invite_app_names);
        List<String> supportedAppsList = null;
        if (supportedApps != null) {
            supportedAppsList = Arrays.asList(supportedApps);
        }
        while (i.hasNext()) {
            ResolveInfo info  = i.next();
            String packageName = info.activityInfo.packageName;
            int index;
            if ((index = supportedAppsList.indexOf(packageName)) >= 0) {
                infos.add(new AppInfo(info, supportedAppNames[index]));
            }
        }
        return infos;
    }

    private Bundle getIntentBundle() {
        Bundle data = new Bundle();
        data.putString(InviteIntent.PHONE_NUMBER_KEY, phoneNumber);
        data.putString(InviteIntent.MESSAGE_KEY, getEditedMessage());
        data.putString(InviteIntent.EMAIL_KEY, "");
        if (phoneNumber != null) {
            emails = ContactsManager.getEmailsForPhone(getActivity(), phoneNumber, null);
            if (!emails.isEmpty()) {
                data.putString(InviteIntent.EMAIL_KEY, emails.iterator().next());
            }
        }
        data.putString(InviteIntent.SUBJECT_KEY, getString(R.string.invite_subject));
        return data;
    }

    private class ApplicationAdapter extends BaseAdapter {

        private final List<AppInfo> applications;
        private Context context;

        ApplicationAdapter(Context context, List<AppInfo> applications) {
            this.context = context;
            this.applications = applications;
        }

        @Override
        public int getCount() {
            return applications.size();
        }

        @Override
        public AppInfo getItem(int position) {
            return applications.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.popup_applications_list_item, null);
                holder = new Holder();
                holder.icon = ButterKnife.findById(convertView, R.id.icon);
                holder.title = ButterKnife.findById(convertView, R.id.title);
                convertView.setTag(holder);
            } else {
                holder = (Holder) convertView.getTag();
            }
            holder.icon.setImageDrawable(getItem(position).loadIcon(context));
            holder.title.setText(getItem(position).loadName(context));
            return convertView;
        }

        private class Holder {
            ImageView icon;
            TextView title;
        }
    }

    private static class AppInfo {
        private final ResolveInfo info;
        private final String name;

        AppInfo(ResolveInfo info, String name) {
            this.name = name;
            this.info = info;
        }

        String loadName(Context context) {
            return String.valueOf(info.loadLabel(context.getPackageManager()));
        }

        String getSystemName() {
            return name;
        }

        String getClassName() {
            return info.activityInfo.name;
        }

        String getPackageName() {
            return info.activityInfo.applicationInfo.packageName;
        }

        Drawable loadIcon(Context context) {
            return info.loadIcon(context.getPackageManager());
        }
    }

}
