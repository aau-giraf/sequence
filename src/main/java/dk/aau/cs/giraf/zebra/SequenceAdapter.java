package dk.aau.cs.giraf.zebra;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import dk.aau.cs.giraf.oasis.lib.models.Frame;
import dk.aau.cs.giraf.oasis.lib.models.Sequence;

public class SequenceAdapter extends BaseAdapter {

    // Adapter for list of sequences in the context of the given child

    private Context context;
	private Sequence sequence;
	
	private OnAdapterGetViewListener onAdapterGetViewListener;

	public SequenceAdapter(Context context, Sequence sequence) {
		this.context = context;
		this.sequence = sequence;
	}

	@Override
	public int getCount() {
		if (sequence == null)
			return 0;
		else
			return sequence.getFramesList().size();
	}

	@Override
	public Frame getItem(int position) {
		if (sequence == null) throw new IllegalStateException("No Sequence has been set for this Adapter");
		
		if (position >= 0 && position < sequence.getFramesList().size())
			return sequence.getFramesList().get(position);
		else
			return null;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		PictogramView view;
		Frame frame = getItem(position);
		
		if (convertView == null) {
			view = new PictogramView(context, 24f);
		}
        else {
            view = (PictogramView)convertView;
        }

		view.setImageFromId(frame.getPictogramId());
		
		if (onAdapterGetViewListener != null)
			onAdapterGetViewListener.onAdapterGetView(position, view);
		
		return view;
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
