package dk.aau.cs.giraf.zebra.serialization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;

import android.content.Context;
import android.os.Environment;
import dk.aau.cs.giraf.zebra.models.Child;
import dk.aau.cs.giraf.zebra.models.Sequence;

public class SequenceFileStore {

	private static final Locale fixedLocale = Locale.US;
	
	public static List<Sequence> getSequences(Context context, Child child) {

		checkProfile(child);
		
		List<Sequence> sequences = new ArrayList<Sequence>();
		
		String filename = getFilename(child);
		
		BufferedReader br = null;
		try {
			FileInputStream fis = context.openFileInput(filename);
			InputStreamReader isr = new InputStreamReader(fis, "UTF8");
			br = new BufferedReader(isr);
						
			StringBuilder sb = new StringBuilder();
			CharBuffer charBuffer = CharBuffer.allocate(1024);
						
			while (br.read(charBuffer) != -1) {
				sb.append(charBuffer.flip());
			}
			
			JSONSerializer jsonSerializer = new JSONSerializer();
			sequences.addAll(jsonSerializer.deserialize(sb.toString()));

		} catch (FileNotFoundException e) {
			//Return empty list at end
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Device does not support UFT8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		
		return sequences;
	}
	
	private static void checkProfile(Child child) {
		if (child.getProfileId() < 0)
			throw new IllegalArgumentException("Invalid profile id");
	}
	
	public static void writeSequences(Context context, Child child, List<Sequence> sequences) {
		checkProfile(child);
		
		String filename = getFilename(child);
		
		BufferedWriter bw = null;
		try {
			FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
			bw = new BufferedWriter(osw);
			
			JSONSerializer serializer = new JSONSerializer();
			String json = serializer.serialize(sequences);
			bw.write(json);
		}  catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Device does not support UFT8");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (bw != null)
				try {
					bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
	
	private static String getFilename(Child child) {
		String filename = String.format(fixedLocale, "seq_c%06d.json", child.getProfileId());
		return filename;
	}
	
	private static File getPath(Child child) {	
		String filename = String.format(fixedLocale, "seq_c%06d.json", child.getProfileId());
	
		File file = new File(Environment.getExternalStorageDirectory() + File.separator + 
				"giraf" + File.separator + 
				"zebra" + File.separator +
				filename);
		
		return file;
	}
	
	private static void createFileIfNotExists(File file) throws IOException {
		if (!file.isFile()) {
			file.createNewFile();
		}
	}
	
}
