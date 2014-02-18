package dk.aau.cs.giraf.zebra;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import dk.aau.cs.giraf.zebra.models.Pictogram;
import dk.aau.cs.giraf.zebra.models.Sequence;

public class SequenceAdapter extends BaseAdapter {
	
	private Sequence sequence;
	private Context context;
	
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
			return sequence.getPictograms().size();
	}

	@Override
	public Pictogram getItem(int position) {
		if (sequence == null) throw new IllegalStateException("No Sequence has been set for this Adapter");
		
		if (position >= 0 && position < sequence.getPictograms().size())
			return sequence.getPictograms().get(position);
		else
			return null;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	};
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		PictogramView view;
		Pictogram pictogram = getItem(position);
		
		if (convertView == null) {
			view = new PictogramView(context, 24f);
		} else
			view = (PictogramView)convertView;
		
		view.setImage(pictogram.getImage(context));
		
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
