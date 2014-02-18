package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.List;

import android.app.Application;
import dk.aau.cs.giraf.zebra.models.Child;

public class ZebraApplication extends Application {

	private static List<Child> children = new ArrayList<Child>();
	
	public static List<Child> getChildren() {
		return children;
	}
	
	public static void setChildren(List<Child> value) {
		children = value;
	}
	
	public static Child getChildFromId(long id) {
		for (Child child : children) {
			if (child.getProfileId() == id)
				return child;
		}
		
		return null;
	}
	

}
