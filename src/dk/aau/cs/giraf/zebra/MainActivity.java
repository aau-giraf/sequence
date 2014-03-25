package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
//import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
// import android.widget.Toast;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceListAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.models.Child;
import dk.aau.cs.giraf.zebra.models.Sequence;
import dk.aau.cs.giraf.zebra.serialization.SequenceFileStore;


public class MainActivity extends Activity {

	//private List<Child> children = ZebraApplication.getChildren();
	private List<Sequence> sequences = new ArrayList<Sequence>();
	
	//private ChildAdapter childAdapter;
	
	private GridView sequenceGrid;
	private boolean isInEditMode = false;
	
	private long guardianId;
	
	private SequenceListAdapter sequenceAdapter;
	
	public static Child selectedChild;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//childAdapter = new ChildAdapter(this, children);
		
		//ListView childList = (ListView)findViewById(R.id.child_list);
		//childList.setAdapter(childAdapter);

		sequenceAdapter = setupAdapter();
		
		sequenceGrid = (GridView)findViewById(R.id.sequence_grid);
		sequenceGrid.setAdapter(sequenceAdapter);
		
		// Load the (from launcher) selected child
		setChild();
		
		/*childList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				
				selectedChild = childAdapter.getItem(arg2);
				refreshSelectedChild();

                //TODO:These lines of code doesn't seem to have an effect on the program

				// final GridView sequenceGridView = ((GridView)findViewById(R.id.sequence_grid));

                //Auto scrolls to the top of the gridview every time
				//sequenceGridView.smoothScrollToPositionFromTop(0, 0, 0);
				
			}
		});*/
		
		//Load Sequence
		sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				((PictogramView)arg1).liftUp();
				
				Sequence sequence = sequenceAdapter.getItem(arg2);
				enterSequence(sequence, false);
			}
		});
		
			
		// Creates clean sequence and starts the sequence activity - ready to add pictograms.
		final ImageButton createButton = (ImageButton)findViewById(R.id.add_button);
		createButton.setVisibility(isInEditMode ? View.VISIBLE : View.GONE);
		
		createButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Sequence sequence = new Sequence();
				sequence.setSequenceId(selectedChild.getNextSequenceId());
				selectedChild.getSequences().add(sequence);
				
				enterSequence(sequence, true);
			}
		});
		
		
		
		// Edit mode switcher button
		ToggleButton button = (ToggleButton) findViewById(R.id.edit_mode_toggle);
		
		button.setOnClickListener(new ImageButton.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ToggleButton button = (ToggleButton)v;
				isInEditMode = button.isChecked();
				
				// Make sure that all views currently not visible will have the correct editmode when they become visible
				sequenceAdapter.setEditModeEnabled(isInEditMode);

				createButton.setVisibility(isInEditMode ? View.VISIBLE : View.GONE);
				
				// Update the editmode of all visible views in the grid
				for (int i = 0; i < sequenceGrid.getChildCount(); i++) {
					View view = sequenceGrid.getChildAt(i);
					
					if (view instanceof PictogramView) {
						((PictogramView)view).setEditModeEnabled(isInEditMode);
					}
				}
			}
		});
	}

	private void setChild() {
		sequences.clear();
		Bundle extras = getIntent().getExtras();
        if (extras != null) {        	
        	guardianId = extras.getLong("currentGuardianID");
        	long childId = extras.getLong("currentChildID");
        	
    		Helper helper = new Helper(this);
    		Profile guardian = helper.profilesHelper.getProfileById(guardianId);
    		List<Profile> childProfiles = helper.profilesHelper.getChildrenByGuardian(guardian);
    		/*Collections.sort(childProfiles, new Comparator<Profile>() {
    	        @Override
    	        public int compare(Profile p1, Profile p2) {
    	            return p1.getFirstname().compareToIgnoreCase(p2.getFirstname());
    	        }
    		});*/
    		
    		for (Profile p : childProfiles) {
    			if (p.getId()==childId) {
                    String name = p.getFirstname() + " " + p.getSurname();
                    Drawable picture = Drawable.createFromPath(p.getPicture());
                    Child c = new Child(p.getId(), name, picture);
                    selectedChild = c;
                }
    		}
    		loadSequences();
    		refreshSelectedChild();
        }/*
        else {
        	//TODO: UNCOMMENT WHEN LAUNCHER IS READY - Displays toast and closes App if not launched from launcher
//        	Toast toast = Toast.makeText(this, "Zebra must be started from the GIRAF Launcher", Toast.LENGTH_LONG);
//        	toast.show();
//
//        	finish();
        	
        	//TODO: REMOVE THE FOLLOWING WHEN ADMIN IS READY - rewrite code to adjust to the admin program (this code finds the first guardian and uses it in order to have a guardian)
       		Helper helper = new Helper(this);

   	    	Profile guardian = helper.profilesHelper.getGuardians().get(0);
   	    	guardianId = guardian.getId();

    		List<Profile> childProfiles = helper.profilesHelper.getChildrenByGuardian(guardian);
    		Collections.sort(childProfiles, new Comparator<Profile>() {
    	        @Override
    	        public int compare(Profile p1, Profile p2) {
    	            return p1.getFirstname().compareToIgnoreCase(p2.getFirstname());
    	        }
    		});
    		
    		for (Profile p : childProfiles) {
    			
    			String name = p.getFirstname() + " " + p.getSurname();
    			Drawable picture = Drawable.createFromPath(p.getPicture());
    			
    			Child c = new Child(p.getId(), name, picture);
    			children.add(c);
    		}
    		selectedChild = children.get(0);
    		loadSequences();
    		refreshSelectedChild();
        	
        } */
	}
	
	private SequenceListAdapter setupAdapter() {
		final SequenceListAdapter adapter = new SequenceListAdapter(this, sequences);
		
		adapter.setOnAdapterGetViewListener(new OnAdapterGetViewListener() {
			
			@Override
			public void onAdapterGetView(final int position, View view) {
				if (view instanceof PictogramView) {
					PictogramView pictoView = (PictogramView) view;
					
					pictoView.setOnDeleteClickListener(new OnDeleteClickListener() {
						
						@Override
						public void onDeleteClick() {
							deleteSequenceDialog(position);
						}
					});
				}
				
			}
		});
		
		return adapter;
	}

	private boolean deleteSequenceDialog(final int position) {

		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.delete_dialog_box);
		dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		
		TextView questionField = (TextView)dialog.findViewById(R.id.question);
		String sequenceName = selectedChild.getSequences().get(position).getTitle();
		String question;
		
		
		if (sequenceName.length() == 0) {
			question = "Du er ved at slette sekvensen. Er du sikker?";
		} else {
			question = "Du er ved at slette sekvensen \"" + sequenceName + "\". Er du sikker?";
		}
		
		questionField.setText(question);
		
		final Button deleteButton = (Button)dialog.findViewById(R.id.btn_delete);
		deleteButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				dialog.dismiss();
				selectedChild.getSequences().remove(position);
				SequenceFileStore.writeSequences(MainActivity.this, selectedChild, selectedChild.getSequences());
				//childAdapter.notifyDataSetChanged();
				refreshSelectedChild();

			}
		});
		
		final Button cancelButton = (Button)dialog.findViewById(R.id.btn_delete_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
		
		return true;
	}
	

	private void loadSequences() {
		//for (Child child : selectedChild) {
			List<Sequence> list = SequenceFileStore.getSequences(this, selectedChild);
			selectedChild.setSequences(list);
		//}
	}
	
	public void refreshSelectedChild() {
		((TextView)findViewById(R.id.child_name)).setText(selectedChild.getName());
		sequences.clear();
		sequences.addAll(selectedChild.getSequences());
        loadSequences();
		sequenceAdapter.notifyDataSetChanged();
	}


    /* When the user presses the home button, the application should close and be destroyed.
    This overrides default Android behaviour, but is done by customer request */
    /*This is commented because it will create a bug where it closes Zebra when returning from Pictosearch

    protected void onDestory(){
        super.onDestroy();
        finish();
    }
    @Override
    protected void onStop(){
        super.onStop();
        finish();
    }
    */

	@Override
	protected void onResume() {

		super.onResume();

		//childAdapter.notifyDataSetChanged();
		refreshSelectedChild();

		// Remove highlighting from all images
		for (int i = 0; i < sequenceGrid.getChildCount(); i++) {
			View view = sequenceGrid.getChildAt(i);

			if (view instanceof PictogramView) {
				((PictogramView)view).placeDown();
			}
		}

	}

	private void enterSequence(Sequence sequence, boolean isNew) {
		Intent intent = new Intent(getApplication(), SequenceActivity.class);
		intent.putExtra("profileId", selectedChild.getProfileId());
		intent.putExtra("sequenceId", sequence.getSequenceId());
		intent.putExtra("guardianId", guardianId);
		intent.putExtra("editMode", isInEditMode);
		intent.putExtra("new", isNew);

		startActivity(intent);
	}
}