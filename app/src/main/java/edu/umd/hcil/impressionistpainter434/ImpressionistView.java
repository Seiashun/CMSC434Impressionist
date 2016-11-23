package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private VelocityTracker _velocityTracker = null;

    private int _alpha = 170;
    private int _splatterNum = 5;
    private int _defaultRadius = 25;
    private int _minBrushRadius = 10;
    private int _maxBrushRadius = 70;
    private int _scaleSpeed = _maxBrushRadius * _defaultRadius;        // Pixels per second

    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private MirrorType _mirrorType = MirrorType.None;

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);
        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){ _imageView = imageView; }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    /**
     * Sets the mirror type.
     * @param mirrorType
     */
    public void setMirrorType(MirrorType mirrorType) { _mirrorType = mirrorType; }

    /**
     * Clears the painting
     */
    public void clearPainting() {
        _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
        _offScreenCanvas = new Canvas(_offScreenBitmap);
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // Velocity Tracker: https://developer.android.com/training/gestures/movement.html
        int index = motionEvent.getActionIndex();
        int action = motionEvent.getActionMasked();
        int pointerId = motionEvent.getPointerId(index);

        Rect rect = getBitmapPositionInsideImageView(_imageView);
        Bitmap imageViewBitmap = _imageView.getDrawingCache();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:       // For multi-touching
                if (_velocityTracker == null) {
                    _velocityTracker = VelocityTracker.obtain();
                } else {
                    _velocityTracker.clear();
                }
                _velocityTracker.addMovement(motionEvent);
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < motionEvent.getPointerCount(); i++) {
                    float touchX = motionEvent.getX(i);
                    float touchY = motionEvent.getY(i);

                    int roundX = (int) touchX;
                    int roundY = (int) touchY;

                    if (imageViewBitmap != null && isInDrawingBorder(rect, roundX, roundY)) {
                        _velocityTracker.addMovement(motionEvent);
                        _velocityTracker.computeCurrentVelocity(1000);      // In ms

                        // Calculate velocity of finger as it moves across the device
                        double actualVelocity = Math.sqrt(Math.pow(_velocityTracker.getXVelocity(pointerId), 2) + Math.pow(_velocityTracker.getYVelocity(pointerId), 2));
                        float scale = (float) (actualVelocity / _scaleSpeed);
                        float actualRadius = Math.max(Math.min(_defaultRadius * scale, _maxBrushRadius), _minBrushRadius);

                        int xDist = Math.max(rect.centerX(), roundX) - Math.min(rect.centerX(), roundX);
                        int yDist = Math.max(rect.centerY(), roundY) - Math.min(rect.centerY(), roundY);

                        int xMirror = (roundX < rect.centerX()) ? xDist : -xDist;
                        int yMirror = (roundY < rect.centerY()) ? yDist : -yDist;

                        _paint.setColor(imageViewBitmap.getPixel(roundX, roundY));
                        _paint.setAlpha(_alpha);

                        if (_brushType == BrushType.Circle) {
                            _offScreenCanvas.drawCircle(roundX, roundY, actualRadius, _paint);

                            if (_mirrorType == MirrorType.XAxis || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(roundX, rect.centerY() + yMirror));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawCircle(roundX, rect.centerY() + yMirror, actualRadius, _paint);
                            }

                            if (_mirrorType == MirrorType.YAxis || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(rect.centerX() + xMirror, roundY));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawCircle(rect.centerX() + xMirror, roundY, actualRadius, _paint);
                            }

                            if (_mirrorType == MirrorType.Diagonal || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(rect.centerX() + xMirror, rect.centerY() + yMirror));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawCircle(rect.centerX() + xMirror, rect.centerY() + yMirror, actualRadius, _paint);
                            }

                        } else if (_brushType == BrushType.Square) {
                            _offScreenCanvas.drawRect(roundX - actualRadius, roundY - actualRadius, roundX + actualRadius, roundY + actualRadius, _paint);

                            if (_mirrorType == MirrorType.XAxis || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(roundX, rect.centerY() + yMirror));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawRect(roundX - actualRadius, rect.centerY() + yMirror - actualRadius, roundX + actualRadius, rect.centerY() + yMirror + actualRadius, _paint);
                            }

                            if (_mirrorType == MirrorType.YAxis || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(rect.centerX() + xMirror, roundY));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawRect(rect.centerX() + xMirror - actualRadius, roundY - actualRadius, rect.centerX() + xMirror + actualRadius, roundY + actualRadius, _paint);
                            }

                            if (_mirrorType == MirrorType.Diagonal || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(rect.centerX() + xMirror, rect.centerY() + yMirror));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawRect(rect.centerX() + xMirror - actualRadius, rect.centerY() + yMirror - actualRadius,
                                        rect.centerX() + xMirror + actualRadius, rect.centerY() + yMirror + actualRadius, _paint);
                            }

                        } else if (_brushType == BrushType.Line) {
                            _offScreenCanvas.drawLine(roundX, roundY - actualRadius, roundX, roundY + actualRadius, _paint);

                            if (_mirrorType == MirrorType.XAxis || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(roundX, rect.centerY() + yMirror));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawLine(roundX, rect.centerY() + yMirror - actualRadius, roundX, rect.centerY() + yMirror + actualRadius, _paint);
                            }

                            if (_mirrorType == MirrorType.YAxis || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(rect.centerX() + xMirror, roundY));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawLine(rect.centerX() + xMirror, roundY - actualRadius, rect.centerX() + xMirror, roundY + actualRadius, _paint);
                            }

                            if (_mirrorType == MirrorType.Diagonal || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(rect.centerX() + xMirror, rect.centerY() + yMirror));
                                _paint.setAlpha(_alpha);
                                _offScreenCanvas.drawLine(rect.centerX() + xMirror, rect.centerY() + yMirror - actualRadius,
                                        rect.centerX() + xMirror, rect.centerY() + yMirror + actualRadius, _paint);
                            }

                        } else if (_brushType == BrushType.CircleSplatter) {
                            drawCircleSplatter(rect, roundX, roundY, actualRadius, imageViewBitmap);

                            if (_mirrorType == MirrorType.XAxis || _mirrorType == MirrorType.All) {
                                drawCircleSplatter(rect, roundX, rect.centerY() + yMirror, actualRadius, imageViewBitmap);
                            }

                            if (_mirrorType == MirrorType.YAxis || _mirrorType == MirrorType.All) {
                                drawCircleSplatter(rect, rect.centerX() + xMirror, roundY, actualRadius, imageViewBitmap);
                            }

                            if (_mirrorType == MirrorType.Diagonal || _mirrorType == MirrorType.All) {
                                drawCircleSplatter(rect, rect.centerX() + xMirror, rect.centerY() + yMirror, actualRadius, imageViewBitmap);
                            }

                        } else if (_brushType == BrushType.LineSplatter) {
                            drawLineSplatter(rect, roundX, roundY, actualRadius, imageViewBitmap);

                            if (_mirrorType == MirrorType.XAxis || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(roundX, rect.centerY() + yMirror));
                                _paint.setAlpha(_alpha);
                                drawLineSplatter(rect, roundX, rect.centerY() + yMirror, actualRadius, imageViewBitmap);
                            }

                            if (_mirrorType == MirrorType.YAxis || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(rect.centerX() + xMirror, roundY));
                                _paint.setAlpha(_alpha);
                                drawLineSplatter(rect, rect.centerX() + xMirror, roundY, actualRadius, imageViewBitmap);
                            }

                            if (_mirrorType == MirrorType.Diagonal || _mirrorType == MirrorType.All) {
                                _paint.setColor(imageViewBitmap.getPixel(rect.centerX() + xMirror, rect.centerY() + yMirror));
                                _paint.setAlpha(_alpha);
                                drawLineSplatter(rect, rect.centerX() + xMirror, rect.centerY() + yMirror, actualRadius, imageViewBitmap);
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:     // For multi-touching
                break;
            case MotionEvent.ACTION_CANCEL:
                _velocityTracker.recycle();
                break;
        }

        invalidate();
        return true;
    }

    // Return the bitmap, intended for saving.
    public Bitmap getBitmap() {
        return _offScreenBitmap;
    }

    private void drawCircleSplatter(Rect rect, float centerX, float centerY, float radius, Bitmap imageViewBitmap) {
        int randX;
        int randY;
        float randScale;

        Random rand = new Random();
        for (int i = 0; i < _splatterNum; i++) {
            do {
                randX = (int) ((rand.nextFloat() * (4 * radius + 1)) + (centerX - 2 * radius));
                randY = (int) ((rand.nextFloat() * (4 * radius + 1)) + (centerY - 2 * radius));
            } while (!isInDrawingBorder(rect, randX, randY));
            randScale = (rand.nextFloat() * (7 * radius / 8)) + (radius / 8);

            _paint.setColor(imageViewBitmap.getPixel(randX, randY));
            _paint.setAlpha(_alpha);
            _offScreenCanvas.drawCircle(randX, randY, randScale, _paint);
        }
    }

    private void drawLineSplatter(Rect rect, float centerX, float centerY, float radius, Bitmap imageViewBitmap) {
        int randX;
        int randY;
        double randRadian;
        float topX;
        float topY;
        float botX;
        float botY;
        float randScale;

        Random rand = new Random();
        for (int i = 0; i < 2 * _splatterNum; i++) {
            do {
                randX = (int) ((rand.nextFloat() * (2 * radius + 1)) + (centerX - 1 * radius));
                randY = (int) ((rand.nextFloat() * (2 * radius + 1)) + (centerY - 1 * radius));
            } while (!isInDrawingBorder(rect, randX, randY));
            randRadian = rand.nextDouble() * 179;
            randScale = (rand.nextFloat() * (9 * radius / 10)) + (radius / 10);

            topX = randX + (float) ((randScale * Math.cos(randRadian)) - (randScale * Math.sin(randRadian)));
            topY = randY - (float) ((randScale * Math.sin(randRadian)) + (randScale * Math.cos(randRadian)));

            botX = randX + (float) ((randScale * Math.cos(-randRadian)) - (randScale * Math.sin(-randRadian)));
            botY = randY + (float) ((randScale * Math.sin(randRadian)) + (randScale * Math.cos(randRadian)));

            _paint.setColor(imageViewBitmap.getPixel(randX, randY));
            _paint.setAlpha(_alpha);
            _offScreenCanvas.drawLine(topX, topY, botX, botY, _paint);

        }
    }

    private boolean isInDrawingBorder(Rect rect, int xPos, int yPos) {
        if (xPos > rect.left && xPos < rect.right && yPos > rect.top && yPos < rect.bottom)
            return true;
        else
            return false;
    }

    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

