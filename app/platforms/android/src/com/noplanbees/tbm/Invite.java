package com.noplanbees.tbm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;


public class Invite {
	
	private Context context;
	
	public Invite(Context context){
		this.context = context;
		showInviteDialog();
	}
	
	private void showInviteDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("Invite")
		.setMessage("To add a friend you need to send an email to us.\n\nWhy?\n\n" + 
				Config.appName + " is currently in private testing. Invitations require approval.")
		.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				showEmail();
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
			}
	    });
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}
	
	private void showEmail(){
		User user = UserFactory.current_user();
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL  , new String[]{"elfishawy.sani@gmail.com"});
		i.putExtra(Intent.EXTRA_SUBJECT, Config.appName + " Invite Request");
		i.putExtra(Intent.EXTRA_TEXT   , "Please provide name and phone of the person you want to invite:\n\n"
				+ "Full name: \n\n"
				+ "Mobile number with area code:\n\n\n\n"
				+ " -- " + user.getFullName() + " " + user.getId());
		try {
		    context.startActivity(Intent.createChooser(i, "Send mail..."));
		} catch (android.content.ActivityNotFoundException ex) {
		    Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
		}

	}
}
