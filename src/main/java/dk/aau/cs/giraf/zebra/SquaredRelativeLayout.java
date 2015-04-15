package dk.aau.cs.giraf.zebra;

import android.content.Context;
import android.widget.RelativeLayout;

/**
 * A relative layout that will force the height to be the same as the width.
 * This is the relativelayout used when using pictograms.
 */
public class SquaredRelativeLayout extends RelativeLayout {
	
	public SquaredRelativeLayout(Context context) {
		super(context);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
}
