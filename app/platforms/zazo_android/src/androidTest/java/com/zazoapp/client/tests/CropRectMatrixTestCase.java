package com.zazoapp.client.tests;

import android.graphics.Matrix;
import android.graphics.RectF;
import junit.framework.TestCase;

/**
 * Created by skamenkovych@codeminders.com on 9/30/2016.
 */

public class CropRectMatrixTestCase extends TestCase {
    public void testRotation() {
        Matrix matrix = new Matrix();
        RectF rect = new RectF(0, 0, 4, 3);
        RectF init = new RectF(rect);
        float[] a = new float[2];
        float[] b = new float[2];
        RectF selectedRect = new RectF(2, 2, 4, 3);
        printRect(matrix, rect);
        matrix.postRotate(90, 1, 1);
        printRect(matrix, rect); // [-1.0,0.0][2.0,4.0]
        printRect(matrix, selectedRect); // [-1.0,2.0][0.0,4.0]
        matrix.reset();
        matrix.postRotate(90, 1, 1);
        setPoint(a, 0, 0);
        matrix.mapPoints(b, a);
        printPoint(b);
        matrix.postRotate(-90, b[0], b[1]);
        printRect(matrix, selectedRect);
        matrix.postTranslate(-b[0], -b[1]);
        printRect(matrix, selectedRect); // [2.0,2.0][4.0,3.0]
    }

    private static void printRect(Matrix matrix, RectF init) {
        RectF rect = new RectF();
        matrix.mapRect(rect, init);
        System.out.println(rect.toShortString());
    }

    private static void printPoint(float[] point) {
        System.out.println(String.format("[%f, %f]", point[0], point[1]));
    }

    private static void setPoint(float[] point, float x, float y) {
        point[0] = x;
        point[1] = y;
    }
}
