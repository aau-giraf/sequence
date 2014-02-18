package dk.aau.cs.giraf.zebra.serialization;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import dk.aau.cs.giraf.zebra.models.Pictogram;
import dk.aau.cs.giraf.zebra.models.Sequence;

public class JSONSerializer {
	
	private final String KEY_SETTINGS_SEQUENCES = "sequences";
	
	private final String KEY_SEQUENCE_TITLE = "title";
	private final String KEY_SEQUENCE_ID = "sequenceId";
	private final String KEY_SEQUENCE_IMAGE_ID = "imageId";
	private final String KEY_SEQUENCE_PICTOGRAMS = "pictograms";
	
	private final String KEY_PICTOGRAM_ID = "pictogramId";

	public String serialize(List<Sequence> sequences) throws JSONException {
		final JSONObject jSonSettings = new JSONObject();
		
		final JSONArray jsonSequences = new JSONArray();
		
		for (Sequence s : sequences) {
			final JSONObject jsonSequence = writeSequence(s);
			jsonSequences.put(jsonSequence);
		}
		
		jSonSettings.put(KEY_SETTINGS_SEQUENCES, jsonSequences);
	
		return jSonSettings.toString();
	}

	private JSONObject writeSequence(Sequence s) throws JSONException {
		final JSONObject jsonSequence = new JSONObject();
		jsonSequence.put(KEY_SEQUENCE_ID, s.getSequenceId());
		jsonSequence.put(KEY_SEQUENCE_TITLE, s.getTitle());
		jsonSequence.put(KEY_SEQUENCE_IMAGE_ID, s.getImageId());
		
		JSONArray jsonPictograms = writePictograms(s.getPictograms());
		
		jsonSequence.put(KEY_SEQUENCE_PICTOGRAMS, jsonPictograms);
		
		return jsonSequence;
	}
	
	private JSONArray writePictograms(List<Pictogram> pictograms) throws JSONException {
		final JSONArray jsonPictograms = new JSONArray();
		for (Pictogram p : pictograms) {
			final JSONObject jsonPictogram = writePictogram(p);
			jsonPictograms.put(jsonPictogram);
		}
		return jsonPictograms;
	}

	private JSONObject writePictogram(Pictogram p) throws JSONException {
		final JSONObject jsonPictogram = new JSONObject();
		jsonPictogram.put(KEY_PICTOGRAM_ID, p.getPictogramId());
		
		return jsonPictogram;
	}

	public List<Sequence> deserialize(String jsonData) throws JSONException {
		
		JSONObject jsonSettings = new JSONObject(jsonData);
		
		JSONArray jsonSequences = jsonSettings.getJSONArray(KEY_SETTINGS_SEQUENCES);
		
		List<Sequence> sequences = new ArrayList<Sequence>();
		
		for (int i = 0; i < jsonSequences.length(); i++) {
			JSONObject jsonSequence = jsonSequences.getJSONObject(i);
			Sequence sequence = readSequence(jsonSequence);
			sequences.add(sequence);
		}
		
		return sequences;
	}

	private Sequence readSequence(JSONObject jsonSequence) throws JSONException {
		long sequenceId = jsonSequence.getLong(KEY_SEQUENCE_ID);
		String title = jsonSequence.getString(KEY_SEQUENCE_TITLE);
		long imageId = jsonSequence.getLong(KEY_SEQUENCE_IMAGE_ID);
		
		Sequence sequence = new Sequence();
		sequence.setSequenceId(sequenceId);
		sequence.setTitle(title);
		sequence.setImageId(imageId);
		
		JSONArray jsonPictograms = jsonSequence.getJSONArray(KEY_SEQUENCE_PICTOGRAMS);
		
		List<Pictogram> pictograms = readPictograms(jsonPictograms);
		
		for (Pictogram p : pictograms) {
			sequence.addPictogramAtEnd(p);
		}
		
		return sequence;
	}

	private List<Pictogram> readPictograms(JSONArray jsonPictograms) throws JSONException {
		List<Pictogram> pictograms = new ArrayList<Pictogram>();
		
		for (int i = 0; i < jsonPictograms.length(); i++) {
			JSONObject jsonPictogram = jsonPictograms.getJSONObject(i);
			Pictogram pictogram = readPictogram(jsonPictogram);
			pictograms.add(pictogram);
		}
		
		return pictograms;
	}

	private Pictogram readPictogram(JSONObject jsonPictogram)
			throws JSONException {
		Pictogram pictogram = new Pictogram();
		pictogram.setPictogramId(jsonPictogram.getLong(KEY_PICTOGRAM_ID));
		return pictogram;
	}
	
}
