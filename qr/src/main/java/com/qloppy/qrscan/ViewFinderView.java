package com.qloppy.qrscan;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import me.dm7.barcodescanner.zbar.R;

class ViewFinderView extends View {
    private static final String TAG = "ViewFinderView";

    private Rect mFramingRect;

    private static final float LANDSCAPE_WIDTH_RATIO = 5f / 8;
    private static final float LANDSCAPE_HEIGHT_RATIO = 5f / 8;

    private static final float PORTRAIT_WIDTH_RATIO = 7f / 8;
    private static final float PORTRAIT_HEIGHT_RATIO = 3f / 8;

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private int scannerAlpha;
    private static final int POINT_SIZE = 10;
    private static final long ANIMATION_DELAY = 80l;

    public ViewFinderView(Context context) {
        super(context);
    }

    public ViewFinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mFramingRect == null) {
            return;
        }

        drawViewFinderMask(canvas);
        drawViewFinderBorder(canvas);
        drawLaser(canvas);
    }

    public void drawViewFinderMask(Canvas canvas) {
        Paint paint = new Paint();
        Resources resources = getResources();
        paint.setColor(resources.getColor(R.color.viewfinder_mask));

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawRect(0, 0, width, mFramingRect.top, paint);
        canvas.drawRect(0, mFramingRect.top, mFramingRect.left, mFramingRect.bottom + 1, paint);
        canvas.drawRect(mFramingRect.right + 1, mFramingRect.top, width, mFramingRect.bottom + 1, paint);
        canvas.drawRect(0, mFramingRect.bottom + 1, width, height, paint);
    }

    public void drawViewFinderBorder(Canvas canvas) {
        Paint paint = new Paint();
        Resources resources = getResources();
        paint.setColor(resources.getColor(R.color.viewfinder_border));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(resources.getInteger(R.integer.viewfinder_border_width));
        int lineLength = resources.getInteger(R.integer.viewfinder_border_length);

        canvas.drawLine(mFramingRect.left - 1, mFramingRect.top - 1, mFramingRect.left - 1, mFramingRect.top - 1 + lineLength, paint);
        canvas.drawLine(mFramingRect.left - 1, mFramingRect.top - 1, mFramingRect.left - 1 + lineLength, mFramingRect.top - 1, paint);

        canvas.drawLine(mFramingRect.left - 1, mFramingRect.bottom + 1, mFramingRect.left - 1, mFramingRect.bottom + 1 - lineLength, paint);
        canvas.drawLine(mFramingRect.left - 1, mFramingRect.bottom + 1, mFramingRect.left - 1 + lineLength, mFramingRect.bottom + 1, paint);

        canvas.drawLine(mFramingRect.right + 1, mFramingRect.top - 1, mFramingRect.right + 1, mFramingRect.top - 1 + lineLength, paint);
        canvas.drawLine(mFramingRect.right + 1, mFramingRect.top - 1, mFramingRect.right + 1 - lineLength, mFramingRect.top - 1, paint);

        canvas.drawLine(mFramingRect.right + 1, mFramingRect.bottom + 1, mFramingRect.right + 1, mFramingRect.bottom + 1 - lineLength, paint);
        canvas.drawLine(mFramingRect.right + 1, mFramingRect.bottom + 1, mFramingRect.right + 1 - lineLength, mFramingRect.bottom + 1, paint);
    }

    public void drawLaser(Canvas canvas) {
        Paint paint = new Paint();
        Resources resources = getResources();
        // Draw a red "laser scanner" line through the middle to show decoding is active
        paint.setColor(resources.getColor(R.color.viewfinder_laser));
        paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
        paint.setStyle(Paint.Style.FILL);
        scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
        int middle = mFramingRect.height() / 2 + mFramingRect.top;
        canvas.drawRect(mFramingRect.left + 2, middle - 1, mFramingRect.right - 1, middle + 2, paint);

        postInvalidateDelayed(ANIMATION_DELAY,
                mFramingRect.left - POINT_SIZE,
                mFramingRect.top - POINT_SIZE,
                mFramingRect.right + POINT_SIZE,
                mFramingRect.bottom + POINT_SIZE);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int xoffset = (int) ((right - left) * 0.05);
        int yoffset = (int) ((bottom - top) * 0.05);
        mFramingRect = new Rect(left + xoffset, top + yoffset, right - xoffset, bottom - yoffset);
    }


    private static int findDesiredDimensionInRange(float ratio, int resolution, int hardMin, int hardMax) {
        int dim = (int) (ratio * resolution);
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }

}
