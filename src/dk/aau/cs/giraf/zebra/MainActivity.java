package dk.aau.cs.giraf.zebra;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import dk.aau.cs.giraf.gui.GButton;
import dk.aau.cs.giraf.gui.GDialogMessage;
import dk.aau.cs.giraf.oasis.lib.Helper;
import dk.aau.cs.giraf.oasis.lib.models.Profile;
import dk.aau.cs.giraf.zebra.PictogramView.OnDeleteClickListener;
import dk.aau.cs.giraf.zebra.SequenceListAdapter.OnAdapterGetViewListener;
import dk.aau.cs.giraf.zebra.models.Child;
import dk.aau.cs.giraf.zebra.models.Sequence;
import dk.aau.cs.giraf.zebra.serialization.SequenceFileStore;


public class MainActivity extends Activity {
    private boolean isInEditMode = true;
    private GridView sequenceGrid;
    private SequenceListAdapter sequenceAdapter;
	private List<Sequence> sequences = new ArrayList<Sequence>();
    public static Child selectedChild;
	private int guardianId;
    private int applicationColor = Color.WHITE;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		sequenceAdapter = setupAdapter();
		sequenceGrid = (GridView)findViewById(R.id.sequence_grid);
		sequenceGrid.setAdapter(sequenceAdapter);

        setChild();
        setColors();

        //TODO: find out what the guardianId is if its in childmode.
        if (guardianId != 100) {
            setupGuardianMode();
        }
        else {
            setupChildMode();
        }
	}

    private void setColors(){

        LinearLayout backgroundLayout = (LinearLayout) findViewById(R.id.parent_container);
        RelativeLayout topbarLayout = (RelativeLayout) findViewById(R.id.sequence_bar);
        backgroundLayout.setBackgroundColor(applicationColor);
        topbarLayout.setBackgroundColor(applicationColor);
    }

    //TODO: This can possibly be done better if we can get the (from launcher) selected child using context
    //Finds the child we want to work with. This is given through a passed extra, "currentChildID".
    //TODO: This is a temporary fix because there is currently no way of using the database! Uncomment and change Child c when possible to get a real child.
	private void setChild() {
		sequences.clear();
		//Bundle extras = getIntent().getExtras();
        //guardianId = extras.getInt("currentGuardianID");
        guardianId = 1;
        /*int childId = extras.getInt("currentChildID");

        try{helper = new Helper(this);}
        catch(Exception e){}
   		Profile guardian = helper.profilesHelper.getProfileById(guardianId);
   		List<Profile> childProfiles = helper.profilesHelper.getChildrenByGuardian(guardian);

    	for (Profile p : childProfiles) {
    		if (p.getId()==childId) {
                Child c = new Child(childId, p.getName(), p.getImage());
                String name = p.getName();
                Bitmap picture = p.getImage();*/
                Child c = new Child(0, "Hamun Leth Laustsen", null);
                selectedChild = c;
       //}
   		loadSequences();
   		refreshSelectedChild();
       }
   //}
	
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
/*
    // TODO: Make a functionality to delete a sequence. Require database syncronization
    public void deleteSequenceDialog (View v) {
        GDialogMessage deleteSequence = new GDialogMessage(v.getContext(),
            R.drawable.ic_launcher,
            "Slet Sekvens",
            "Du er ved at slette sekvensen, er du sikker på du vil det?",
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
    }
*/
    private boolean deleteSequenceDialog(final int position) {
        /*
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
				refreshSelectedChild();
			}		});


		final Button cancelButton = (Button)dialog.findViewById(R.id.btn_delete_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog.show();
        */
    	return true;

	}

	private void loadSequences() {
			List<Sequence> list = SequenceFileStore.getSequences(this, selectedChild);
			selectedChild.setSequences(list);
	}

	public void refreshSelectedChild() {
		((TextView)findViewById(R.id.child_name)).setText(selectedChild.getName());
		sequences.clear();
		sequences.addAll(selectedChild.getSequences());
        loadSequences();
		sequenceAdapter.notifyDataSetChanged();
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshSelectedChild();

		// Remove highlighting from all images
		for (int i = 0; i < sequenceGrid.getChildCount(); i++) {
			View view = sequenceGrid.getChildAt(i);

			if (view instanceof PictogramView) {
				((PictogramView)view).placeDown();
			}
		}
    }


    private void setupGuardianMode() {


        sequenceGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((PictogramView)arg1).liftUp();
                Sequence sequence = sequenceAdapter.getItem(arg2);
                enterSequence(sequence, false);
            }
        });

        final GButton addButton = (GButton)findViewById(R.id.add_button);
        final GButton deleteButton = (GButton)findViewById(R.id.delete_button);
        final GButton copyButton = (GButton)findViewById(R.id.copy_button);


        addButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                Sequence sequence = new Sequence();
                sequence.setSequenceId(selectedChild.getNextSequenceId());
                selectedChild.getSequences().add(sequence);

                enterSequence(sequence, true);
            }
        });

        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                showMainDeleteDialog(v);
            }
        });

        copyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {


            }
        });
    }


    void showMainDeleteDialog(View v) {

        //TODO: Call the deletesequence method here
        GDialogMessage deleteDialog = new GDialogMessage(v.getContext(),
                R.drawable.ic_launcher,
                "Slet Sekvens",
                "Du er ved at slette et antal sekvenser",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

        deleteDialog.show();
    }

    private void setupChildMode() {



    }

	private void enterSequence(Sequence sequence, boolean isNew) {
		Intent intent = new Intent(getApplication(), SequenceActivity.class);
		intent.putExtra("profileId", selectedChild.getProfileId());
		intent.putExtra("sequenceId", sequence.getSequenceId());
		intent.putExtra("guardianId", guardianId);
		intent.putExtra("editMode", isInEditMode);
		intent.putExtra("new", isNew);
        intent.putExtra("applicationColor",applicationColor);

		startActivity(intent);
	}
}