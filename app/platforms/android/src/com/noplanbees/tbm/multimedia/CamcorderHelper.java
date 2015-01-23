package com.noplanbees.tbm.multimedia;

import android.media.CamcorderProfile;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class CamcorderHelper {
	
	private final String TAG = this.getClass().getSimpleName();
	public HashMap<Integer, String> profilesH = new HashMap<Integer, String>();
	public ArrayList<Integer> profilesA = new ArrayList<Integer>();
	
	public CamcorderHelper(){
		load_profiles();
		print_profiles();
	}

	private void load_profiles() {
		load_profile(CamcorderProfile.QUALITY_1080P, "QUALITY_1080P");
		load_profile(CamcorderProfile.QUALITY_480P, "QUALITY_480P");
		load_profile(CamcorderProfile.QUALITY_720P, "QUALITY_720P");
		load_profile(CamcorderProfile.QUALITY_CIF, "QUALITY_CIF");
		load_profile(CamcorderProfile.QUALITY_HIGH, "QUALITY_HIGH");
		load_profile(CamcorderProfile.QUALITY_LOW, "QUALITY_LOW");
		load_profile(CamcorderProfile.QUALITY_QCIF, "QUALITY_QCIF");
		load_profile(CamcorderProfile.QUALITY_QVGA, "QUALITY_QVGA");
	}
	
	private void load_profile(Integer i, String s){
		profilesA.add(i);
		profilesH.put(i, s);
	}
	
	private void print_profiles() {
		for (Integer i : profilesA){
			Log.i(TAG, profilesH.get(i) +"-"+ CamcorderProfile.hasProfile(1, i));
		}
	}

}
