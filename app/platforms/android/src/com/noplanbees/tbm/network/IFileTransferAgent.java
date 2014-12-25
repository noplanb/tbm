package com.noplanbees.tbm.network;

import android.content.Intent;

public interface IFileTransferAgent {

	boolean upload();
	boolean download();
	void setInstanceVariables(Intent intent) throws InterruptedException;
}
