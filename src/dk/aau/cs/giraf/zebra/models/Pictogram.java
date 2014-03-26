package dk.aau.cs.giraf.zebra.models;

import android.content.Context;
import android.graphics.Bitmap;
import dk.aau.cs.giraf.pictogram.PictoFactory;

public class Pictogram {
	private int pictogramId;
	private String imagePath;
	
	public long getPictogramId() {
		return pictogramId;
	}
	
	public void setPictogramId(int pictogramId) {
		this.pictogramId = pictogramId;
		this.imagePath = null;
	}
	
	public Pictogram getClone() {
		Pictogram clone = new Pictogram();
		clone.pictogramId = this.pictogramId;
		
		return clone;
	}

	public Bitmap getImage(Context context) {

		return PictoFactory.getPictogram(context, pictogramId).getImageData();
	}
}
