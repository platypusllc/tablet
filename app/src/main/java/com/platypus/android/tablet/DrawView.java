package com.platypus.android.tablet;

/**
 * Created by shenty on 1/16/16.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.mapbox.mapboxsdk.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

//taken from stack overflow answer..
public class DrawView extends View {
    private Paint paint;
    private Paint black;
    ArrayList<PointF> points;

    public DrawView(Context c){
        this(c, null);
    }

    public DrawView(Context c, AttributeSet s) {
        super(c, s);

        black=new Paint();
        black.setColor(Color.BLACK);
        black.setStrokeWidth(5);
        black.setAntiAlias(true);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawLine(0,0,1000,1000,black);
        if (points == null || points.size() < 2)
        {
            return;
        }
        for (int i = 1; i < points.size(); i++) {
            canvas.drawLine(points.get(i-1).x,points.get(i-1).y,points.get(i).x,points.get(i).y,black);
        }
    }
}