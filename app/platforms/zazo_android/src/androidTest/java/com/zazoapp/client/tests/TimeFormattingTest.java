package com.zazoapp.client.tests;

import android.test.InstrumentationTestCase;
import android.text.format.DateFormat;
import com.zazoapp.client.utilities.StringUtils;
import junit.framework.Assert;

/**
 * Created by skamenkovych@codeminders.com on 2/19/2016.
 */
public class TimeFormattingTest extends InstrumentationTestCase {

    public void testGetEventTime() {
        long currentTime = System.currentTimeMillis();
        String[][] testSamplesUS24 = new String[][] {
                {"1455892482000", "16:34"},
                {"1455832800000", "00:00"},
                {"1455746400000", "Thu 00:00"},
                {"1455314400000", "Sat 00:00"},
                {"1455314399000", "Feb 12"},
                {"1455227999000", "Feb 11"},
        };

        String[][] testSamplesUS12 = new String[][] {
                {"1455892482000", "4:34 PM"},
                {"1455832800000", "12:00 AM"},
                {"1455746400000", "Thu 12:00 AM"},
                {"1455314400000", "Sat 12:00 AM"},
                {"1455314399000", "Feb 12"},
                {"1455227999000", "Feb 11"},
        };
        String[][] samplesUnderTest = (DateFormat.is24HourFormat(getInstrumentation().getContext())) ? testSamplesUS24 : testSamplesUS12;
        for (String[] sample : samplesUnderTest) {
            Assert.assertEquals(sample[1], StringUtils.getEventTime(sample[0]));
        }
    }

    private String st(long timestamp) {
        return String.valueOf(timestamp);
    }
}
