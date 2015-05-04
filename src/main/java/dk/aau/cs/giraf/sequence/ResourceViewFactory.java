package dk.aau.cs.giraf.sequence;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Creates View from XML resources.
 * 
 * Meant for View that cannot be in the XML and are only used few (or a single) place.
 *
 */
class ResourceViewFactory {

	public static View getAddPictogramButton(Context context) {
		return getViewFromResource(context, R.layout.add_pictogram);
	}

    private static View getViewFromResource(Context context, int resource) {
		return getInflater(context).inflate(resource, null);
	}

	private static LayoutInflater getInflater(Context context) {
		return (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
}
