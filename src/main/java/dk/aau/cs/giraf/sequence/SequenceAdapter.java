package dk.aau.cs.giraf.sequence;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import dk.aau.cs.giraf.dblib.models.Frame;
import dk.aau.cs.giraf.dblib.models.Sequence;

/**
 * Adapter used for sequences, in the context of the given child
 */
public class SequenceAdapter extends BaseAdapter {

    private final Context context;
    private final Sequence sequence;

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
        if (sequence == null)
            throw new IllegalStateException("No Sequence has been set for this Adapter");

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
        } else {
            view = (PictogramView) convertView;
        }

        // If the element is a choice, then use the "choose" icon
        //if (sequence.getFramesList().get(position).getPictogramList().size() > 0) {
        //    view.setImageFromId(0);
        //} else {
            view.setImageFromId(frame.getPictogramId());
        //}

        if (onAdapterGetViewListener != null)
            onAdapterGetViewListener.onAdapterGetView(position, view);

        return view;
    }

    public void setOnAdapterGetViewListener(OnAdapterGetViewListener onCreateViewListener) {
        this.onAdapterGetViewListener = onCreateViewListener;
    }

    public interface OnAdapterGetViewListener {
        public void onAdapterGetView(int position, View view);
    }
}

