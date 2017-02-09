package home.climax708.librecarpool;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.places.Place;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class RidesAdapter extends RecyclerView.Adapter<RidesAdapter.ViewHolder> {
    private ArrayList<Ride> mRides;

    private ViewHolder.OnRideCardClickListener mListener;

    // Provide a reference to the views for each data item.
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView mOriginTextView;
        public TextView mDestinationTextView;
        private String[] mOrigins;
        private String mDepartureTimeStr;
        private OnRideCardClickListener mInternalListener;
        public ViewHolder(View layout, Context context, OnRideCardClickListener listener) {
            super(layout);
            mOriginTextView = (TextView) layout.findViewById(R.id.ride_row_origin_text_view);
            mDestinationTextView = (TextView) layout.findViewById(
                    R.id.ride_row_destination_text_view);
            mOrigins = context.getResources().getStringArray(R.array.origins);
            mDepartureTimeStr = context.getString(R.string.ride_card_departure);
            mInternalListener = listener;
            layout.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mInternalListener.onRideCardClick(v, getAdapterPosition());
        }

        public interface OnRideCardClickListener {
            void onRideCardClick(View v, int position);
        }

    }

    public RidesAdapter(Ride[] rides, ViewHolder.OnRideCardClickListener listener) {
        mRides = new ArrayList<>();
        for (Ride ride : rides) {
            mRides.add(ride);
        }
        mListener = listener;
    }

    public void clear() {
        mRides.clear();
        this.notifyDataSetChanged();
    }

    public void addItem(Ride ride) {
        mRides.add(ride);
        this.notifyItemChanged(mRides.size()-1);
    }

    public void addItems(Collection<? extends Ride> collection) {
        mRides.addAll(collection);
        this.notifyDataSetChanged();
    }

    public Ride getRide(int position) {
        return mRides.get(position);
    }

    public void addItems(Ride... items) {
        for (Ride ride: items) {
            mRides.add(ride);
        }
        this.notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RidesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        Context parentContext = parent.getContext();
        // create a new view
        View rideCardLayout = LayoutInflater.from(parentContext)
                .inflate(R.layout.ride_card_layout, parent, false);
        ViewHolder viewHolder = new ViewHolder(rideCardLayout, parentContext, mListener);
        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Ride currentRide = mRides.get(position);
        if (currentRide != null) {
            RideTime rideTime = currentRide.getRideTime();
            Place rideDestination = currentRide.getDestinationPlace();

            viewHolder.mDestinationTextView.setText(rideDestination.getName());
            String originText = String.format(Locale.getDefault(), viewHolder.mDepartureTimeStr,
                    rideTime.getHour(), rideTime.getMinute(),
                    currentRide.getDeparturePlace().getName());
            viewHolder.mOriginTextView.setText(originText);
        }
    }

    @Override
    public int getItemCount() {
        return mRides.size();
    }
}
