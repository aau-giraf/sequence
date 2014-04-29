package dk.aau.cs.giraf.zebra;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Rounds the corner of the image set.
 * 
 * Maybe cut of parts of the image.
 */
public class RoundedImageView extends ImageView {

	private static final int DEFAULT_CORNER_RADIUS = -1;
	
	private float cornerRadius;
	
	RectF imageRect = new RectF();
	Paint opacity = new Paint();
	int height = -1;
	int width = -1;
	float radius;
	Xfermode oldMode;
	int saveCount;
	Paint paint;
	Drawable image;
	Bitmap bitmap;
	
 
    public RoundedImageView(Context context, float radius) {
        super(context);
        cornerRadius = radius;
        ensureClipPathSupport();
    }
    
    public RoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        cornerRadius = getCornerRadius(context, attrs);
        ensureClipPathSupport();
    }
 
    public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        cornerRadius = getCornerRadius(context, attrs);
        ensureClipPathSupport();
    }
    
	private int getCornerRadius(Context context, AttributeSet attrs) {
		int cornerRadius = DEFAULT_CORNER_RADIUS;
		
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.RoundedImageView);
		try {
			cornerRadius = a.getDimensionPixelSize(
					R.styleable.RoundedImageView_cornerRadius, DEFAULT_CORNER_RADIUS);
		} finally {
			a.recycle();
		}
		
		return cornerRadius;
	}
    
    private void ensureClipPathSupport()
    {
    	// Disable hardware acceleration to support clipPath in newer versions of android
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
    	   this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }
 
    @Override
    protected void onDraw(Canvas canvas) {
    	int width = getWidth() - getPaddingLeft() - getPaddingRight();
    	int height = getHeight() - getPaddingTop() - getPaddingBottom();
    	Drawable maiDrawable = getDrawable();
    	
    	if (maiDrawable == null) {
    		super.onDraw(canvas);
    	}
    	else {
    		// Update the bitmap if the image should be redrawn
    		if (width != this.width || height != this.height || cornerRadius != this.radius || maiDrawable != this.image)
        	{
        		this.width = width;
        		this.height = height;
        		this.radius = cornerRadius;
        		this.image = maiDrawable;
        		
        		//Create proper bitmap
        		bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        		Canvas bitmapCanvas = new Canvas(bitmap);
        		
        		float fCornerRadius = cornerRadius;
            	if (cornerRadius == -1) 
            		fCornerRadius = Math.min(width, height) / 11.f;

            	
            	paint = ((BitmapDrawable)maiDrawable).getPaint();
                //Color is: transparency/red/green/blue. ff is no transparency
            	final int color = 0xff000000; //Old background color: 0xfffde18d
            	
            	
            	maiDrawable.setColorFilter(Color.rgb(253, 225, 141), Mode.DST_OVER);
            	
            	imageRect.set(0, 0, width, height);
            	
            	saveCount = bitmapCanvas.saveLayer(imageRect, null,
                        Canvas.MATRIX_SAVE_FLAG |
                        Canvas.CLIP_SAVE_FLAG |
                        Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
                        Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
                        Canvas.CLIP_TO_LAYER_SAVE_FLAG);
            	
            	
            	paint.setAntiAlias(true);
                bitmapCanvas.drawARGB(0, 0, 0, 0);

                //Set background color of application
                paint.setColor(color);

                bitmapCanvas.drawRoundRect(imageRect, fCornerRadius, fCornerRadius, paint);
                
                oldMode = paint.getXfermode();
                
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                super.onDraw(bitmapCanvas);
                paint.setXfermode(oldMode);
                bitmapCanvas.restoreToCount(saveCount);
        	}
    		
    		canvas.drawBitmap(this.bitmap, 0, 0, null);
    	}
    }    
}