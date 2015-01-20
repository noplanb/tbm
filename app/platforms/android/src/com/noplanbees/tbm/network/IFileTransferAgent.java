package com.noplanbees.tbm.network;

import android.content.Intent;

public interface IFileTransferAgent {

	boolean upload();
	boolean download() throws IllegalStateException;
	boolean delete();
	void setInstanceVariables(Intent intent) throws InterruptedException;
}
