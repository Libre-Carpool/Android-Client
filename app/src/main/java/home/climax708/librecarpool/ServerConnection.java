package home.climax708.librecarpool;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class ServerConnection {

    private GetPlacesTask mGetPlacesTask;
    private final String URL = "https://rawgit.com/Libre-Carpool/Android-Client/master/rides.html"; // TODO: Set this to your web servers URL

    public interface OnRidesRetrievedListener {
        void onRidesRetrievingStarted();
        void onRidesRetrieved(Ride[] rides);
    }

    public void cancelTask() {
        if (isTaskRunning()) {
            mGetPlacesTask.cancel(true);
        }
    }

    public void getRides(GoogleApiClient googleApiClient, OnRidesRetrievedListener listener,
                         Ride... params) {
        if (isTaskRunning()) {
            mGetPlacesTask.cancel(true);
        }
        mGetPlacesTask = new GetPlacesTask(googleApiClient, listener);
        mGetPlacesTask.execute(params);
    }

    private boolean isTaskRunning() {
        return (mGetPlacesTask != null
                && mGetPlacesTask.getStatus() == AsyncTask.Status.RUNNING);
    }

    private class GetPlacesTask extends AsyncTask<Ride, Integer, Ride[]> {

        private GoogleApiClient mGoogleApiClient;
        private OnRidesRetrievedListener mListener;

        public GetPlacesTask(GoogleApiClient googleApiClient, OnRidesRetrievedListener listener) {
            mGoogleApiClient = googleApiClient;
            mListener = listener;
        }

        @Override
        protected Ride[] doInBackground(Ride... params) {
            if (isCancelled())
                return new Ride[0];

            Ride[] rides = null;
            try {
                Document doc = Jsoup.connect(URL).timeout(10*1000).get();
                Elements rows = doc.select("tr");
                int rowCount = rows.size();
                rides = new Ride[rowCount - 1];
                // Start from 1 because first row displays column names.
                for (int i = 1; i < rowCount; i++) {
                    Element row = rows.get(i);
                    Elements cols = row.select("td");

                    String phone        = cols.get(0).text();
                    String destination  = cols.get(1).text();
                    String origin       = cols.get(2).text();
                    String date         = cols.get(3).text();
                    String time         = cols.get(4).text();
                    String through      = cols.get(5).text();
                    String comments     = cols.get(6).text();

                    rides[i - 1] =  new Ride(phone, new RideTime(time), origin, through, destination, comments);
                }
            }
            // TODO: Handle exceptions
            /* Possible exceptions
             * + MalformedURLException
             * + IOException
             */
            catch (MalformedURLException e) {
                Log.d("ServerConnection", "doInBackground :: MalformedURLException");
                e.printStackTrace();
            }
            catch (IOException e) {
                Log.d("ServerConnection", "doInBackground :: IOException");
                e.printStackTrace();
            }
            catch(Exception e) {
                e.printStackTrace();
            }

            for (int i = 0; i < rides.length; i++) {
                if (isCancelled())
                    break;

                Ride currentRide = rides[i];
                if (currentRide == null)
                    continue;

                PendingResult<PlaceBuffer> destinationPlacesBuffer;
                destinationPlacesBuffer = Places.GeoDataApi.getPlaceById(
                        mGoogleApiClient,
                        currentRide.getDeparturePlaceID(),
                        currentRide.getDestinationPlaceID());
                PlaceBuffer places = destinationPlacesBuffer.await();

                // Freezing the place object is necessary for later use since we are closing the buffer.
                // refer to: https://developers.google.com/places/android-api/buffers
                currentRide.setDeparturePlace(places.get(0).freeze());
                currentRide.setDestinationPlace(places.get(1).freeze());

                places.release();
            }

            if (params == null || isCancelled()) {
                // Get all available rides
                return rides;
            } else {
                // Filter rides according to parameters.
                return filterRides(rides, params);
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
            if (mListener != null)
                mListener.onRidesRetrieved(isCancelled() ? new Ride[0] : rides);
        }
    }

    private Ride[] filterRides(Ride[] ridesPool, Ride... filterRides) {
        ArrayList<Ride> filteredRides = new ArrayList<>();
        for (Ride rideFromPool : ridesPool) {
            for (Ride filterRide : filterRides) {
                if (filterRide == null)
                    continue;
                if (filterRide.getDeparturePlaceID() != null
                        && !rideFromPool.getDeparturePlaceID().equals(filterRide.getDeparturePlaceID()))
                    continue;
                if (filterRide.getDestinationPlace() != null
                        && !rideFromPool.getDestinationPlace().getId().equals(filterRide.getDestinationPlace().getId()))
                    continue;
                if (filterRide.getRideTime() != null) {
                    if (rideFromPool.getRideTime().getHour() < filterRide.getRideTime().getHour())
                        continue;
                    else {
                        if (rideFromPool.getRideTime().getHour() == filterRide.getRideTime().getHour()) {
                            if (rideFromPool.getRideTime().getMinute() < filterRide.getRideTime().getMinute())
                                continue;
                        }
                    }
                }
                if (filteredRides.contains(rideFromPool))
                    continue;
                filteredRides.add(rideFromPool);
            }
        }
        Ride[] filteredRidesArray = new Ride[filteredRides.size()];
        for (int i = 0; i < filteredRidesArray.length; i++) {
            filteredRidesArray[i] = filteredRides.get(i);
        }

        return filteredRidesArray;
    }
}
