package dk.aau.cs.giraf.zebra.models;

import java.util.ArrayList;
import java.util.List;

import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;

import android.content.Context;
import android.graphics.Bitmap;


public class Child {
	
	private int profileId;
	private String name;
	private Bitmap picture;
    Helper helper;
	
	private List<Sequence> sequences = new ArrayList<Sequence>();
	public Child(int profileId, String name, Bitmap picture) {
		this.profileId = profileId;
		this.name = name;
		this.picture = picture;
	}
	
	public int getProfileId() {
		return profileId;
	}
	
	public String getName() {
		return name;
	}

	public List<Sequence> getSequences() {
        return sequences;
	}
	
	public void setSequences(List<Sequence> sequences) {
		this.sequences = sequences;
	}
	
	public int getSequenceCount() {
		return sequences.size();
	}
	
	public int getNextSequenceId() {
		if (sequences.size() != 0)
			return sequences.get(sequences.size() - 1).getId() + 1;

		return 1;
	}
	
	public Sequence getSequenceFromId(int sequenceId) {
		for (Sequence sequence : sequences) {
			if (sequence.getId() == sequenceId)
				return sequence;
		}
		
		return null;
	}
}
