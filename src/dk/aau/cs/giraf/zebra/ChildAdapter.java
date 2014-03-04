package dk.aau.cs.giraf.zebra;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import dk.aau.cs.giraf.zebra.models.Child;

public class ChildAdapter extends BaseAdapter {

	private List<Child> items;
	private LayoutInflater inflater;
	private Activity activity;
	
	public ChildAdapter(Activity activity, List<Child> items) {
		
		this.items = items;
		this.inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.activity = activity;
	}

    public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		
		if (v == null) {
			v = inflater.inflate(R.layout.children_list_row, null);
		}
		
        TextView nameTextView = (TextView)v.findViewById(R.id.name);
        TextView countTextView = (TextView)v.findViewById(R.id.count);
        ImageView childImage = (ImageView)v.findViewById(R.id.child_image);
 
        Child c = items.get(position);
        
        nameTextView.setText(c.getName());

        if (c.getSequenceCount() == 1) {
        countTextView.setText(c.getSequenceCount() + " sekvens");
        } else {
            countTextView.setText(c.getSequenceCount() + " sekvenser");
        }

        if (c.getPicture() == null) {
        	childImage.setImageDrawable(activity.getResources().getDrawable(R.drawable.placeholder));
        } else {
        	childImage.setImageDrawable(c.getPicture());
        }
        
        return v;
    }

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Child getItem(int position) {
        return items.get(position);
    }
	
	@Override
	public long getItemId(int position) {
        return items.get(position).getProfileId();
    }
}
