package com.example.eric.roadsidemonitoringapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;

public class HeatMapView extends View {

    private ArrayList<HeatMapSeries> data = null;

    public HeatMapView(Context context) {
        super(context);
        init(null, 0);
    }

    public HeatMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public HeatMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.HeatMapView, defStyle, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (data != null) {

            float xScale = (float) getMeasuredWidth();
            float yScale = (float) getMeasuredHeight();
            float radScale;

            radScale = 6;

            for (HeatMapSeries series : data) {

                Paint paint = new Paint();
                paint.setStrokeWidth(0f);
                paint.setColor(series.color);
                paint.setAlpha(100);

                Path path = new Path();

                System.out.println("Series entries: " + series.entries.size());

                for (HeatMapEntry entry : series.entries) {
                    path.addCircle(entry.x * xScale, entry.y * yScale, entry.radius * radScale,
                            Path.Direction.CW);
                }

                canvas.drawPath(path, paint);

            }
        }
    }

    public void updateData(ArrayList<HeatMapSeries> data) {
        this.data = data;
        invalidate();
    }
}
