package com.zazoapp.client.tests;

import com.zazoapp.client.ui.view.VideoProgressBar;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Random;

/**
 * Created by Serhii on 23.06.2015.
 */
public class VideoProgressSchemeTest extends TestCase {
    public void testEmpty() {
        VideoProgressBar.Scheme.SchemeBuilder b = new VideoProgressBar.Scheme.SchemeBuilder();
        check(b, 0, 0, "<Empty>");
    }

    public void testSingleBar() {
        VideoProgressBar.Scheme.SchemeBuilder b = new VideoProgressBar.Scheme.SchemeBuilder();
        b.addBar();
        check(b, 1, 1, "-");
    }

    public void testSinglePoint() {
        VideoProgressBar.Scheme.SchemeBuilder b = new VideoProgressBar.Scheme.SchemeBuilder();
        b.addPoint();
        check(b, 1, 0, ".");
    }

    public void testManyBars() {
        VideoProgressBar.Scheme.SchemeBuilder b = new VideoProgressBar.Scheme.SchemeBuilder();
        int count = 5;
        StringBuilder scheme = new StringBuilder();
        for (int i = 0; i < count; i++) {
            b.addBar();
            scheme.append('-');
        }
        check(b, count, count, scheme.toString());
    }

    public void testManyPoints() {
        VideoProgressBar.Scheme.SchemeBuilder b = new VideoProgressBar.Scheme.SchemeBuilder();
        int count = 5;
        StringBuilder scheme = new StringBuilder();
        for (int i = 0; i < count; i++) {
            b.addPoint();
            scheme.append('.');
        }
        check(b, count, 0, scheme.toString());
    }

    public void testSomeScheme() {
        VideoProgressBar.Scheme.SchemeBuilder b = new VideoProgressBar.Scheme.SchemeBuilder();
        String someScheme = "---.-.--.-";
        int bars = 0;
        for (int i = 0; i < someScheme.length(); i++) {
            switch (someScheme.charAt(i)) {
                case '-':
                    b.addBar();
                    bars++;
                    break;
                case '.':
                    b.addPoint();
                    break;
            }
        }
        check(b, someScheme.length(), bars, someScheme);
    }

    public void testRandomScheme() {
        VideoProgressBar.Scheme.SchemeBuilder b = new VideoProgressBar.Scheme.SchemeBuilder();
        Random random = new Random();

        int size = random.nextInt(100) + 10;
        int bars = 0;
        StringBuilder randomScheme = new StringBuilder();
        for (int i = 0; i < size; i++) {
            switch (random.nextInt(2)) {
                case 0:
                    b.addBar();
                    bars++;
                    randomScheme.append('-');
                    break;
                case 1:
                    b.addPoint();
                    randomScheme.append('.');
                    break;
            }
        }
        check(b, size, bars, randomScheme.toString());
    }

    private void check(VideoProgressBar.Scheme.SchemeBuilder b, int size, int bars, String r) {
        VideoProgressBar.Scheme scheme = b.build();
        Assert.assertEquals("Size", scheme.getCount(), size);
        Assert.assertEquals("BarCount", scheme.getBarCount(), bars);
        Assert.assertEquals("Scheme", scheme.toString(), r);
    }
}
