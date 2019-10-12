package ru.dimasokol.learning.multipainter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Рисовалка с многопальцевым режимом
 */
public class MultiPaintingView extends View {
    private Paint mBorderPaint;
    private Paint mBackgroundPaint;

    private Rect mDrawingBounds;

    private int mCurrentColor = Color.BLACK;

    private boolean mPaintingMode = true;

    /**
     * Слушатель событий скролла
     */
    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            for (FigureDrawable figure : mFigures) {
                for (PointF point : figure.mPoints) {
                    float x = mDrawingBounds.left + (point.x * mDrawingBounds.width());
                    float y = mDrawingBounds.top + (point.y * mDrawingBounds.height());

                    point.x = toRangeX(x - distanceX);
                    point.y = toRangeY(y - distanceY);
                }
            }

            invalidate();
            return true;
        }
    };
    private GestureDetector mDetector;

    private List<FigureDrawable> mFigures = new ArrayList<>(64);
    private List<FigureDrawable> mScaledFigures = null;

    private FigureDrawable mLastFigure;

    public MultiPaintingView(Context context) {
        super(context);
        initPaints();
    }

    public MultiPaintingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    public MultiPaintingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaints();
    }

    public MultiPaintingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initPaints();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int size = Math.min(w, h);
        int left = (w - size) / 2;
        int top = (h - size) / 2;

        int oneDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());

        mDrawingBounds = new Rect(left, top, left + size, top + size);
        mDrawingBounds.inset(oneDp, oneDp);
    }

    public void setCurrentColor(int currentColor) {
        mCurrentColor = currentColor;
    }

    /**
     * Устанавливает режим рисования
     *
     * @param paintingMode режим рисования: true для рисовалки, false для прокрутки
     */
    public void setPaintingMode(boolean paintingMode) {
        mPaintingMode = paintingMode;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // В режиме прокрутки отдаём события детектору
        return mPaintingMode? processPaintingModeEvent(event) : mDetector.onTouchEvent(event);
    }

    private boolean processPaintingModeEvent(MotionEvent event) {
        PointF currentPoint = null;

        // masked!
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastFigure = new FigureDrawable(mCurrentColor);
                currentPoint = mLastFigure.getPoint(0);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                int pointerId = event.getPointerId(event.getActionIndex());
                currentPoint = mLastFigure.getPoint(pointerId);
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pId = event.getPointerId(i);
                    mLastFigure.getPoint(pId).x = toRangeX(event.getX(i));
                    mLastFigure.getPoint(pId).y = toRangeY(event.getY(i));
                }
                break;
            case MotionEvent.ACTION_UP:
                mFigures.add(mLastFigure);
                mLastFigure = null;
                break;
        }

        if (currentPoint != null) {
            currentPoint.x = toRangeX(event.getX(event.getActionIndex()));
            currentPoint.y = toRangeY(event.getY(event.getActionIndex()));
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(mDrawingBounds, mBackgroundPaint);
        canvas.drawRect(mDrawingBounds, mBorderPaint);

        canvas.clipRect(mDrawingBounds);
        canvas.translate(mDrawingBounds.left, mDrawingBounds.top);

        for (FigureDrawable figure : (mScaledFigures != null)? mScaledFigures : mFigures) {
            figure.draw(canvas);
        }

        if (mLastFigure != null) {
            mLastFigure.draw(canvas);
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.mDrawables = mFigures;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        SavedState ourState = (SavedState) state;
        mFigures = ourState.mDrawables;
        invalidate();
    }

    private void initPaints() {
        mBackgroundPaint = new Paint();
        mBorderPaint = new Paint();

        mBackgroundPaint.setColor(Color.WHITE);
        mBorderPaint.setColor(Color.BLACK);

        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBackgroundPaint.setStyle(Paint.Style.FILL);

        mBorderPaint.setStrokeCap(Paint.Cap.SQUARE);
        mBorderPaint.setStrokeJoin(Paint.Join.BEVEL);

        mDetector = new GestureDetector(getContext(), mGestureListener);
    }

    private float toRangeX(float x) {
        float localX = x - mDrawingBounds.left;
        return localX / mDrawingBounds.width();
    }

    private float toRangeY(float y) {
        float localY = y - mDrawingBounds.top;
        return localY / mDrawingBounds.height();
    }

    private static class FigureDrawable extends Drawable implements Parcelable, Cloneable {

        private Paint mPaint;
        private Paint mComplexPaint;

        private Path mComplexPath;
        private int mColor;
        private float mLineWidth = 8f;
        private List<PointF> mPoints = new ArrayList<>(16);

        protected FigureDrawable(int color) {
            mColor = color;
            initPaint();
        }

        protected FigureDrawable(Parcel in) {
            mColor = in.readInt();
            mLineWidth = in.readFloat();
            mPoints = in.createTypedArrayList(PointF.CREATOR);
            initPaint();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mColor);
            dest.writeFloat(mLineWidth);
            dest.writeTypedList(mPoints);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @NonNull
        @Override
        protected FigureDrawable clone() throws CloneNotSupportedException {
            FigureDrawable cloned = new FigureDrawable(mColor);
            cloned.mLineWidth = mLineWidth;
            cloned.mPoints = new ArrayList<>(mPoints.size());

            for (PointF point : mPoints) {
                cloned.mPoints.add(new PointF(point.x, point.y));
            }

            return cloned;
        }

        // Требуется для Parcelable
        public static final Creator<FigureDrawable> CREATOR = new Creator<FigureDrawable>() {
            @Override
            public FigureDrawable createFromParcel(Parcel in) {
                return new FigureDrawable(in);
            }

            @Override
            public FigureDrawable[] newArray(int size) {
                return new FigureDrawable[size];
            }
        };

        @Override
        public void draw(@NonNull Canvas canvas) {
            switch (mPoints.size()) {
                case 1:
                    drawSinglePoint(mPoints.get(0), canvas);
                    break;
                case 2:
                    drawLine(mPoints.get(0), mPoints.get(1), canvas);
                    break;
                default:
                    drawComplexFigure(canvas);
            }
        }

        public PointF getPoint(int index) {
            while (index >= mPoints.size()) {
                mPoints.add(new PointF());
            }

            return mPoints.get(index);
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        private void initPaint() {
            mPaint = new Paint();
            mPaint.setColor(mColor);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(mLineWidth);

            mComplexPaint = new Paint(mPaint);
            mComplexPaint.setStyle(Paint.Style.FILL);
        }

        private void drawSinglePoint(PointF point, Canvas canvas) {
            float size = Math.min(canvas.getWidth(), canvas.getHeight());
            float x = point.x * size;
            float y = point.y * size;

            canvas.drawPoint(x, y, mPaint);
        }



        private void drawLine(PointF one, PointF two, Canvas canvas) {
            float size = Math.min(canvas.getWidth(), canvas.getHeight());
            canvas.drawLine(one.x * size, one.y * size, two.x * size, two.y * size, mPaint);
        }

        private void drawComplexFigure(Canvas canvas) {
            if (mComplexPath == null) {
                mComplexPath = new Path();
            }

            mComplexPath.reset();

            float size = Math.min(canvas.getWidth(), canvas.getHeight());

            for (PointF point : mPoints) {
                if (mComplexPath.isEmpty()) {
                    mComplexPath.moveTo(point.x * size, point.y * size);
                } else {
                    mComplexPath.lineTo(point.x * size, point.y * size);
                }
            }

            mComplexPath.close();
            canvas.drawPath(mComplexPath, mComplexPaint);
        }
    }

    private static class SavedState extends BaseSavedState {    // Удобный базовый класс сохраненного состояния

        // Необходимая часть Parcelable, создаёт один объект и массив
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private List<FigureDrawable> mDrawables;

        // Этот конструктор вызывается при восстановлении
        public SavedState(Parcel source) {
            super(source);
            mDrawables = source.createTypedArrayList(FigureDrawable.CREATOR);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public SavedState(Parcel source, ClassLoader loader) {
            super(source, loader);
            mDrawables = source.createTypedArrayList(FigureDrawable.CREATOR);
        }

        // А этот — при сохранении
        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeTypedList(mDrawables);
        }
    }
}
