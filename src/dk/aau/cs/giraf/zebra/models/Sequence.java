package dk.aau.cs.giraf.zebra.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import dk.aau.cs.giraf.pictogram.PictoFactory;


/**
 * Represents a ordered collection (sequence) of pictograms.
 *
 */
public class Sequence {

	private long sequenceId;
	private String title;
	private long imageId;
	private String imagePath;
	
	public Sequence() {
	}
	
	// Ordered list of pictograms
	private List<Pictogram> pictograms = new ArrayList<Pictogram>();
	
	public long getSequenceId() {
		return sequenceId;
	}
	
	public void setSequenceId(long sequenceId) {
		this.sequenceId = sequenceId;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public long getImageId() {
		return imageId;
	}
	
	public void setImageId(long imageId) {
		this.imageId = imageId;
		this.imagePath = null;
	}
	
	public Drawable getImage(Context context) {
		
		if (imagePath == null) {
			imagePath = PictoFactory.getPictogram(context, imageId).getImagePath();
		}
		
		return Drawable.createFromPath(imagePath);
	}

	public List<Pictogram> getPictograms() {
		return Collections.unmodifiableList(pictograms);
	}
	
	/**
	 * Swaps the position of two drawables.
	 * @param oldIndex
	 * @param newIndex
	 */
	public void rearrange(int oldIndex, int newIndex) {
		if (oldIndex < 0 || oldIndex >= pictograms.size()) throw new IllegalArgumentException("oldIndex out of range");
		if (newIndex < 0 || newIndex >= pictograms.size()) throw new IllegalArgumentException("newIndex out of range");
		
		Pictogram temp = pictograms.remove(oldIndex);
		pictograms.add(newIndex, temp);
	}
	
	public void addPictogramAtEnd(Pictogram pictogram) {
		pictograms.add(pictogram);
	}
	
	public void deletePictogram(Pictogram pictogram) {
		pictograms.remove(pictogram);
	}
	
	public void deletePictogram(int position) {
		pictograms.remove(position);
	}
	
	public Sequence getClone() {
		Sequence clone = new Sequence();
		clone.sequenceId = this.sequenceId;
		clone.title = this.title;
		clone.imageId = this.imageId;
		
		for (Pictogram pictogram : pictograms) {
			clone.pictograms.add(pictogram.getClone());
		}
		
		return clone;
	}
	
	public void copyFromSequence(Sequence sequence) {
		this.setSequenceId(sequence.sequenceId);
		this.setTitle(sequence.title);
		this.setImageId(sequence.imageId);
		
		this.pictograms.clear();
		for (Pictogram pictogram : sequence.pictograms) {
			this.pictograms.add(pictogram);
		}
	}
}
