package com.zazoapp.client.tests;

import android.content.Context;
import android.content.res.AssetManager;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import com.zazoapp.client.Config;
import com.zazoapp.client.Utils;
import com.zazoapp.client.multimedia.ThumbnailRetriever;
import junit.framework.Assert;

import java.io.File;

/**
 * Created by skamenkovych@codeminders.com on 3/18/2015.
 */
public class ThumbnailGenerationTest extends ActivityInstrumentationTestCase2<TestActivity> {

    private static final String FILE_NAME = "VID_20150317_124910.mp4";

    private File video;
    public ThumbnailGenerationTest() {
        super(TestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AssetManager am = getInstrumentation().getContext().getAssets();
        Context context = getActivity();
        video = Utils.createFileFromAssets(am, buildPath(context), FILE_NAME);
    }

    @UiThreadTest
    public void testThumbnailGeneration() throws Exception {
        Assert.assertNotNull(video);
        final StringBuilder errors = new StringBuilder();
        int i = 0;
        for (; i < 10000; i++) {
            ThumbnailRetriever retriever = new ThumbnailRetriever();
            try {
                retriever.getThumbnail(video.getPath());
            } catch (Exception e) {
                errors.append(i).append(" repeat: ").append(e.getMessage()).append("\n\n");
            }
        }
        boolean noErrors = errors.length() == 0;
        errors.append("Total tries: ").append(i);
        Assert.assertEquals(errors.toString(), true, noErrors);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (video != null && video.exists()) {
            video.delete();
        }
    }

    private String buildPath(Context context) {
        return Config.homeDirPath(context) + File.separator + FILE_NAME;
    }
}
