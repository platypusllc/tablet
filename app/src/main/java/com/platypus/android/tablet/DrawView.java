package com.platypus.android.tablet;

/**
 * Created by shenty on 1/16/16.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;


import java.util.ArrayList;
import java.util.List;

//taken from stack overflow answer..
public class DrawView extends View {
    private Paint paint;
    private Path path;
    private boolean close = false;
    ArrayList<PointF> points;

    public DrawView(Context c){
        this(c, null);
    }

    public DrawView(Context c, AttributeSet s) {
        super(c, s);

        paint=new Paint();

        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
    }
    public void setPaint (String color, float width, boolean dotted){
        switch (color){
            case "blue":
                paint.setColor(Color.BLUE);
                break;
            case "green":
                paint.setColor(Color.GREEN);
                break;
            default:
                paint.setColor(Color.GRAY);
        }
        paint.setStrokeWidth(width);
        if(dotted){
            paint.setPathEffect(new DashPathEffect(new float[]{10,10}, 5));
        }
    }
    public void setClose (boolean c){
        close = c;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //canvas.drawLine(0,0,1000,1000,black);
        if (points == null || points.size() < 2)
        {
            return;
        }
        path = new Path();
        //path.reset();
        path.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i < points.size(); i++) {
            path.lineTo(points.get(i).x, points.get(i).y);
        }

        if(close){
            path.close();
        }

        canvas.drawPath(path, paint);
    }
}