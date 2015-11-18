package com.zazoapp.client.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.zazoapp.client.R;
import com.zazoapp.client.core.TbmApplication;

/**
 * Created by skamenkovych@codeminders.com on 11/18/2015.
 */
public class ContactsFragment extends Fragment {

    private ZazoManagerProvider managers;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bench, container, false);
        managers = TbmApplication.getInstance().getManagerProvider();
        managers.getBenchViewManager().attachView(rootView);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        managers.getBenchViewManager().detachView();
    }
}
