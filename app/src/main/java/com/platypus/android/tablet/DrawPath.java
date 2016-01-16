package com.platypus.android.tablet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by zeshengxi on 1/5/16.
 */
public class DrawPath extends SurfaceView {
    private Bitmap bmp;
    private SurfaceHolder holder;
    private int color;
    private TeleOpPanel.Point[] points;

    private static final String logTag = DrawPath.class.getName();
    public  DrawPath(Context context, int _color, TeleOpPanel.Point[] _points){
        super(context);
        holder = getHolder();
        color = _color;
        points = _points;
        holder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Canvas canvas = holder.lockCanvas(null);
                canvas.drawColor(Color.BLUE);
                canvas.drawBitmap(bmp, 10, 10, null);
                holder.unlockCanvasAndPost(canvas);
            }


            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }
        });
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.boat);
    }

//   public void setColor(int color){
//       this.color = color;
//   }
//
//   public void setPoints (Point[] points){
//       this.points = points;
//   }
    @Override
    protected void onDraw(Canvas canvas){
        canvas.drawColor(Color.BLUE);
        canvas.drawBitmap(bmp, 10, 10, null);
    }
//   public class Point{
//
//       public float x = 0;
//       public float y = 0;
//
//       public Point(float x, float y){
//           this.x = x;
//           this.y = y;
//       }
//   }

    /**
     * Draw polygon
     *
     * @param canvas The canvas to draw on
     * @param color  Integer representing a fill color (see http://developer.android.com/reference/android/graphics/Color.html)
     * @param points Polygon corner points
     */
    private void drawPoly(Canvas canvas, int color, TeleOpPanel.Point[] points) {
        // line at minimum...
        if (points.length < 2) {
            return;
        }

        // paint
        Paint polyPaint = new Paint();
        polyPaint.setColor(color);
        polyPaint.setStyle(Paint.Style.FILL);

        // path
        Path polyPath = new Path();
        polyPath.moveTo(points[0].x, points[0].y);
        int i, len;
        len = points.length;
        for (i = 0; i < len; i++) {
            polyPath.lineTo(points[i].x, points[i].y);
        }
        polyPath.lineTo(points[0].x, points[0].y);

        // draw
        canvas.drawPath(polyPath, polyPaint);
    }



}

