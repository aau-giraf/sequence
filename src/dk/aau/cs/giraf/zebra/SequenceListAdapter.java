package dk.aau.cs.giraf.zebra;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import dk.aau.cs.giraf.zebra.models.Child;
import dk.aau.cs.giraf.zebra.models.Sequence;

/**
 * Adapter for a List of Sequences typically associated with a {@link Child}.
 *
 */
public class SequenceListAdapter extends BaseAdapter {

	
	private List<Sequence> items;
	private Context context;
	private boolean isInEditMode;
	private OnAdapterGetViewListener onAdapterGetViewListener;
	
	public SequenceListAdapter(Context context, List<Sequence> items) {
		
		this.items = items;
		this.context = context;
	}

    public View getView(int position, View convertView, ViewGroup parent) {
		PictogramView v = (PictogramView) convertView;
		
		if (v == null) {
			v = new PictogramView(context, 16f);
		}
		
        Sequence sequence = items.get(position);
        
        v.setTitle(sequence.getTitle());
        v.setEditModeEnabled(isInEditMode);
		v.setImage(sequence.getImage(context));
        
        
        if (onAdapterGetViewListener != null)
			onAdapterGetViewListener.onAdapterGetView(position, v);

        return v;
    }

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Sequence getItem(int position) {
        return items.get(position);
    }
	
	@Override
	public long getItemId(int position) {
        return position;
    }
	
	public void setEditModeEnabled(boolean editEnabled) {
		if (isInEditMode != editEnabled) {
			isInEditMode = editEnabled;
		}
	}
	
	public void setOnAdapterGetViewListener(OnAdapterGetViewListener onCreateViewListener) {
		this.onAdapterGetViewListener = onCreateViewListener;
	}
	
	public OnAdapterGetViewListener getOnAdapterGetViewListener() {
		return this.onAdapterGetViewListener;
	}
	
	public interface OnAdapterGetViewListener {
		public void onAdapterGetView(int position, View view);
	}
}
