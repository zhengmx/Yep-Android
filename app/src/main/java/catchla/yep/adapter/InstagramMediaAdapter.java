package catchla.yep.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import catchla.yep.R;
import catchla.yep.model.InstagramMedia;
import catchla.yep.view.holder.InstagramMediaViewHolder;

/**
 * Created by mariotaku on 15/6/4.
 */
public class InstagramMediaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final LayoutInflater mLayoutInflater;
    private List<InstagramMedia> mShots;

    public InstagramMediaAdapter(final Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new InstagramMediaViewHolder(mLayoutInflater.inflate(R.layout.grid_item_gallery_provider_type, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        ((InstagramMediaViewHolder) holder).displayShot(mShots.get(position));
    }

    @Override
    public int getItemCount() {
        if (mShots == null) return 0;
        return mShots.size();
    }

    public void setData(List<InstagramMedia> shots) {
        mShots = shots;
        notifyDataSetChanged();
    }
}
