package com.zazoapp.client.network.aws;

import android.content.Context;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Created by skamenkovych@codeminders.com on 9/9/2016.
 */
public class S3AvatarDownloadHelper {
    private static AmazonS3Client client;
    private static S3CredentialsStore credentialsStore;
    private static TransferUtility transferUtility;

    public static S3CredentialsStore getCredentialsStore(Context context) {
        if (credentialsStore == null) {
            credentialsStore = S3CredentialsStore.newInstance(context, "avatar");
        }
        return credentialsStore;
    }

    public static AmazonS3Client getS3Client(Context context) {
        if (client == null) {
            S3CredentialsStore store = getCredentialsStore(context);
            client = new AmazonS3Client(new BasicAWSCredentials(store.getS3AccessKey(), store.getS3SecretKey()));
        }
        return client;
    }

    public static TransferUtility getTransferUtility(Context context) {
        if (transferUtility == null) {
            transferUtility = new TransferUtility(getS3Client(context.getApplicationContext()),
                    context.getApplicationContext());
        }

        return transferUtility;
    }
}
