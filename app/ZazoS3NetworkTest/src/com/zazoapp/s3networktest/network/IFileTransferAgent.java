package com.zazoapp.s3networktest.network;

import android.content.Intent;

public interface IFileTransferAgent {

	boolean upload() throws InterruptedException;
	boolean download() throws InterruptedException;
	boolean delete() throws InterruptedException;
	void setInstanceVariables(Intent intent) throws InterruptedException;
}
