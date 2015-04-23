package dk.aau.cs.giraf.sequence;

import android.content.Context;
import android.widget.RelativeLayout;

/**
 * A relative layout that will force the height to be the same as the width.
 */
public class SquaredRelativeLayout extends RelativeLayout {
	
	public SquaredRelativeLayout(Context context) {
		super(context);
	}

    // The width is used twice to ensure that the layout is squared.
    // This results in a lint warning, which we suppress.
	@SuppressWarnings("SuspiciousNameCombination")
    @Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
}
