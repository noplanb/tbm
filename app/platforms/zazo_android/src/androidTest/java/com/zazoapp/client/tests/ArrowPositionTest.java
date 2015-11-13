package com.zazoapp.client.tests;

import com.zazoapp.client.tutorial.TutorialLayout.ArrowPosition;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Created by Serhii on 23.06.2015.
 */
public class ArrowPositionTest extends TestCase {
    public void testLeft() {
        Assert.assertEquals(ArrowPosition.BOTTOM_LEFT + "->" + ArrowPosition.BOTTOM_LEFT, ArrowPosition.BOTTOM_LEFT.left(), ArrowPosition.BOTTOM_LEFT);
        Assert.assertEquals(ArrowPosition.BOTTOM_CENTER + "->" + ArrowPosition.BOTTOM_LEFT, ArrowPosition.BOTTOM_CENTER.left(), ArrowPosition.BOTTOM_LEFT);
        Assert.assertEquals(ArrowPosition.BOTTOM_RIGHT + "->" + ArrowPosition.BOTTOM_LEFT, ArrowPosition.BOTTOM_RIGHT.left(), ArrowPosition.BOTTOM_LEFT);
        Assert.assertEquals(ArrowPosition.TOP_LEFT + "->" + ArrowPosition.TOP_LEFT, ArrowPosition.TOP_LEFT.left(), ArrowPosition.TOP_LEFT);
        Assert.assertEquals(ArrowPosition.TOP_CENTER + "->" + ArrowPosition.TOP_LEFT, ArrowPosition.TOP_CENTER.left(), ArrowPosition.TOP_LEFT);
        Assert.assertEquals(ArrowPosition.TOP_RIGHT + "->" + ArrowPosition.TOP_LEFT, ArrowPosition.TOP_RIGHT.left(), ArrowPosition.TOP_LEFT);
    }

    public void testRight() {
        Assert.assertEquals(ArrowPosition.BOTTOM_LEFT + "->" + ArrowPosition.BOTTOM_RIGHT, ArrowPosition.BOTTOM_LEFT.right(), ArrowPosition.BOTTOM_RIGHT);
        Assert.assertEquals(ArrowPosition.BOTTOM_CENTER + "->" + ArrowPosition.BOTTOM_RIGHT, ArrowPosition.BOTTOM_CENTER.right(), ArrowPosition.BOTTOM_RIGHT);
        Assert.assertEquals(ArrowPosition.BOTTOM_RIGHT + "->" + ArrowPosition.BOTTOM_RIGHT, ArrowPosition.BOTTOM_RIGHT.right(), ArrowPosition.BOTTOM_RIGHT);
        Assert.assertEquals(ArrowPosition.TOP_LEFT + "->" + ArrowPosition.TOP_RIGHT, ArrowPosition.TOP_LEFT.right(), ArrowPosition.TOP_RIGHT);
        Assert.assertEquals(ArrowPosition.TOP_CENTER + "->" + ArrowPosition.TOP_RIGHT, ArrowPosition.TOP_CENTER.right(), ArrowPosition.TOP_RIGHT);
        Assert.assertEquals(ArrowPosition.TOP_RIGHT + "->" + ArrowPosition.TOP_RIGHT, ArrowPosition.TOP_RIGHT.right(), ArrowPosition.TOP_RIGHT);
    }

    public void testCenter() {
        Assert.assertEquals(ArrowPosition.BOTTOM_LEFT + "->" + ArrowPosition.BOTTOM_CENTER, ArrowPosition.BOTTOM_LEFT.center(), ArrowPosition.BOTTOM_CENTER);
        Assert.assertEquals(ArrowPosition.BOTTOM_CENTER + "->" + ArrowPosition.BOTTOM_CENTER, ArrowPosition.BOTTOM_CENTER.center(), ArrowPosition.BOTTOM_CENTER);
        Assert.assertEquals(ArrowPosition.BOTTOM_RIGHT + "->" + ArrowPosition.BOTTOM_CENTER, ArrowPosition.BOTTOM_RIGHT.center(), ArrowPosition.BOTTOM_CENTER);
        Assert.assertEquals(ArrowPosition.TOP_LEFT + "->" + ArrowPosition.TOP_CENTER, ArrowPosition.TOP_LEFT.center(), ArrowPosition.TOP_CENTER);
        Assert.assertEquals(ArrowPosition.TOP_CENTER + "->" + ArrowPosition.TOP_CENTER, ArrowPosition.TOP_CENTER.center(), ArrowPosition.TOP_CENTER);
        Assert.assertEquals(ArrowPosition.TOP_RIGHT + "->" + ArrowPosition.TOP_CENTER, ArrowPosition.TOP_RIGHT.center(), ArrowPosition.TOP_CENTER);
    }

    public void testTop() {
        Assert.assertEquals(ArrowPosition.BOTTOM_LEFT + "->" + ArrowPosition.TOP_LEFT, ArrowPosition.BOTTOM_LEFT.top(), ArrowPosition.TOP_LEFT);
        Assert.assertEquals(ArrowPosition.BOTTOM_CENTER + "->" + ArrowPosition.TOP_CENTER, ArrowPosition.BOTTOM_CENTER.top(), ArrowPosition.TOP_CENTER);
        Assert.assertEquals(ArrowPosition.BOTTOM_RIGHT + "->" + ArrowPosition.TOP_RIGHT, ArrowPosition.BOTTOM_RIGHT.top(), ArrowPosition.TOP_RIGHT);
        Assert.assertEquals(ArrowPosition.TOP_LEFT + "->" + ArrowPosition.TOP_LEFT, ArrowPosition.TOP_LEFT.top(), ArrowPosition.TOP_LEFT);
        Assert.assertEquals(ArrowPosition.TOP_CENTER + "->" + ArrowPosition.TOP_CENTER, ArrowPosition.TOP_CENTER.top(), ArrowPosition.TOP_CENTER);
        Assert.assertEquals(ArrowPosition.TOP_RIGHT + "->" + ArrowPosition.TOP_RIGHT, ArrowPosition.TOP_RIGHT.top(), ArrowPosition.TOP_RIGHT);
    }

    public void testBottom() {
        Assert.assertEquals(ArrowPosition.BOTTOM_LEFT + "->" + ArrowPosition.BOTTOM_LEFT, ArrowPosition.BOTTOM_LEFT.bottom(), ArrowPosition.BOTTOM_LEFT);
        Assert.assertEquals(ArrowPosition.BOTTOM_CENTER + "->" + ArrowPosition.BOTTOM_CENTER, ArrowPosition.BOTTOM_CENTER.bottom(), ArrowPosition.BOTTOM_CENTER);
        Assert.assertEquals(ArrowPosition.BOTTOM_RIGHT + "->" + ArrowPosition.BOTTOM_RIGHT, ArrowPosition.BOTTOM_RIGHT.bottom(), ArrowPosition.BOTTOM_RIGHT);
        Assert.assertEquals(ArrowPosition.TOP_LEFT + "->" + ArrowPosition.BOTTOM_LEFT, ArrowPosition.TOP_LEFT.bottom(), ArrowPosition.BOTTOM_LEFT);
        Assert.assertEquals(ArrowPosition.TOP_CENTER + "->" + ArrowPosition.BOTTOM_CENTER, ArrowPosition.TOP_CENTER.bottom(), ArrowPosition.BOTTOM_CENTER);
        Assert.assertEquals(ArrowPosition.TOP_RIGHT + "->" + ArrowPosition.BOTTOM_RIGHT, ArrowPosition.TOP_RIGHT.bottom(), ArrowPosition.BOTTOM_RIGHT);
    }
}
