package home.climax708.librecarpool;

import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class ServerConnection {

    private GetPlacesTask mGetPlacesTask;
    private final String URL = "https://rawgit.com/Libre-Carpool/Android-Client/master/rides.html"; // TODO: Set this to your web servers URL

    private final static int MAX_TIMEOUT_MS = 10000; // 10 seconds.

    public interface OnRidesRetrievedListener {
        void onRidesRetrievingStarted();
        void onRidesRetrieved(Ride[] rides);
        void onRidesRetrievingFailed(Exception exception);
    }

    public void cancelTask() {
        if (isTaskRunning()) {
            mGetPlacesTask.cancel(true);
        }
    }

    public void getRides(GoogleApiClient googleApiClient, OnRidesRetrievedListener listener,
                         RideSearch rideSearch) {
        if (isTaskRunning()) {
            mGetPlacesTask.cancel(true);
        }
        mGetPlacesTask = new GetPlacesTask(googleApiClient, listener);
        mGetPlacesTask.execute(rideSearch);
    }

    private boolean isTaskRunning() {
        return (mGetPlacesTask != null
                && mGetPlacesTask.getStatus() == AsyncTask.Status.RUNNING);
    }

    private class GetPlacesTask extends AsyncTask<RideSearch, Integer, Ride[]> {

        private GoogleApiClient mGoogleApiClient;
        private OnRidesRetrievedListener mListener;

        private boolean mIsRetrievalSuccessful = true;
        private Exception mRetrievalException;

        public GetPlacesTask(GoogleApiClient googleApiClient, OnRidesRetrievedListener listener) {
            mGoogleApiClient = googleApiClient;
            mListener = listener;
        }

        @Override
        protected Ride[] doInBackground(RideSearch... params) {
            if (isCancelled())
                return new Ride[0];

            Ride[] rides;
            Ride.Builder[] rideBuilder;
            try {
                Document doc = Jsoup.connect(URL).timeout(MAX_TIMEOUT_MS).get();
                Elements rows = doc.select("tr");
                int rowCount = rows.size();

                // Skip the first row as it displays column names.
                int ridesCount = rowCount - 1;
                rides = new Ride[ridesCount];
                rideBuilder = new Ride.Builder[ridesCount];

                // Double the length since we need to convert origin and destination for every ride.
                String[] placeIds = new String[ridesCount * 2];

                // Start from 1 because first row displays column names.
                for (int i = 1; i < rowCount; i++) {
                    if (isCancelled())
                        break;

                    Element row = rows.get(i);
                    Elements cols = row.select("td");

                    String phone        = cols.get(0).text();
                    String destination  = cols.get(1).text();
                    String origin       = cols.get(2).text();
                    String date         = cols.get(3).text();
                    String time         = cols.get(4).text();
                    String through      = cols.get(5).text();
                    String comments     = cols.get(6).text();

                    int ridesIndex = i - 1;

                    rideBuilder[ridesIndex] = new Ride.Builder();
                    rideBuilder[ridesIndex]
                            .setDriverPhoneNumber(phone)
                            .setDepartureRideTime(new RideTime(time))
                            .setDeparturePlaceId(origin)
                            .setPassingThrough(through)
                            .setDestinationPlaceId(destination)
                            .setComments(comments);

                    // Set departure place id
                    placeIds[ridesIndex] = origin;
                    // Set destination place id.
                    // Offset the index by rides.length, essentially split the array to destination and departure place ids.
                    placeIds[ridesIndex + ridesCount] = destination;
                }

                // Convert Google Place ID to Google Place objects.
                PendingResult<PlaceBuffer> destinationPlacesBuffer;
                destinationPlacesBuffer = Places.GeoDataApi.getPlaceById(
                        mGoogleApiClient, placeIds);
                PlaceBuffer places = destinationPlacesBuffer.await();

                int placesCount = places.getCount();
                if (placesCount == rides.length * 2) {
                    for (int i = 0; i < rides.length; i++) {
                        if (isCancelled())
                            break;

                        // Freezing the place object is necessary for later use since we are closing the buffer.
                        // refer to: https://developers.google.com/places/android-api/buffers
                        rideBuilder[i]
                                .setDeparturePlace(places.get(i).freeze())
                                .setDestinationPlace(places.get(i + rides.length).freeze());

                        // Finish the building.
                        rides[i] = rideBuilder[i].build();
                    }
                }

                places.release();
            } catch (Exception e) {
                // Pass errors to be handled by implementing listeners.
                mIsRetrievalSuccessful = false;
                mRetrievalException = e;
                return new Ride[0];
            }

            if (params[0] == null || isCancelled()) {
                // Get all available rides
                return rides;
            } else {
                // Filter rides according to parameters.
                return filterRides(rides, params[0]);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!isCancelled() && mListener != null)
                mListener.onRidesRetrievingStarted();
        }

        @Override
        protected void onPostExecute(Ride[] rides) {
            if (mListener != null) {
                if (mIsRetrievalSuccessful) {
                    mListener.onRidesRetrieved(isCancelled() ? new Ride[0] : rides);
                } else {
                    mListener.onRidesRetrievingFailed(mRetrievalException);
                }
            }
        }
    }

    private Ride[] filterRides(Ride[] ridesPool, RideSearch rideSearch) {
        ArrayList<Ride> filteredRides = new ArrayList<>();
        for (Ride rideFromPool : ridesPool) {
            if (rideSearch != null) {
                String rideSearchDeparturePlaceId = rideSearch.getDeparturePlaceId();
                if (rideSearchDeparturePlaceId != null
                        && !rideFromPool.getDeparturePlaceID().equals(rideSearchDeparturePlaceId))
                    continue;

                String rideSearchDestinationPlaceId = rideSearch.getDestinationPlaceId();
                if (rideSearchDestinationPlaceId != null
                        && !rideFromPool.getDestinationPlaceID().equals(rideSearchDestinationPlaceId))
                    continue;

                RideTime rideSearchDepartureTime = rideSearch.getDepartureTime();
                if (rideSearchDepartureTime != null) {
                    if (rideFromPool.getRideTime().getHour() < rideSearchDepartureTime.getHour())
                        continue;
                    else {
                        if (rideFromPool.getRideTime().getHour() == rideSearchDepartureTime.getHour()) {
                            if (rideFromPool.getRideTime().getMinute() < rideSearchDepartureTime.getMinute())
                                continue;
                        }
                    }
                }
            }

            if (filteredRides.contains(rideFromPool))
                continue;
            filteredRides.add(rideFromPool);
        }
        Ride[] filteredRidesArray = new Ride[filteredRides.size()];
        for (int i = 0; i < filteredRidesArray.length; i++) {
            filteredRidesArray[i] = filteredRides.get(i);
        }

        return filteredRidesArray;
    }
}
