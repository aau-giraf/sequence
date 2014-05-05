package dk.aau.cs.giraf.zebra.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import dk.aau.cs.giraf.pictogram.PictoFactory;
import dk.aau.cs.giraf.zebra.R;

public class Pictogram {
	private int pictogramId;
	private String imagePath;
    private String type;
	
	public long getPictogramId() {
		return pictogramId;
	}
	
	public void setPictogramId(int pictogramId) {
		this.pictogramId = pictogramId;
		this.imagePath = null;
	}
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
