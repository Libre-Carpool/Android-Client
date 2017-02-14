package home.climax708.librecarpool;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class ServerConnection {

    private AsyncStatus _status;
    private GetPlacesTask mGetPlacesTask;
    private final String URL = "https://placeholder/rides.html"; // TODO: Set this to your web servers URL

    private enum AsyncStatus {
        finished,
        canceled,
        noInternet,
        unknownexception, unreachableUrl, connectionProblem, readingProblem, finishedReading,
    }

    private void setStatus(AsyncStatus stat) {
        this._status = stat;
    }

    private AsyncStatus getFetchStatus() {
        return this._status;
    }

    public interface OnRidesRetrievedListener {
        void onRidesRetrievingStarted();
        void onRidesRetrieved(Ride[] rides);
        void failedToFetchRiders(String message);
    }

    public void cancelTask() {
        if (isTaskRunning()) {
            setStatus(AsyncStatus.canceled);
        }
    }

    public void getRides(GoogleApiClient googleApiClient, OnRidesRetrievedListener listener,
                         Ride... params) {
        if (isTaskRunning()) {
            setStatus(AsyncStatus.canceled);
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
            // Sets the status to finished because in every error we change it and then we know that something happened
            setStatus(AsyncStatus.finished);

            Ride[] rides = new Ride[0];

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
                setStatus(AsyncStatus.unreachableUrl);
                Log.d("ServerConnection", "doInBackground :: MalformedURLException");
                e.printStackTrace();
            }
            catch (HttpStatusException e) {
                setStatus(AsyncStatus.connectionProblem);
                Log.d("ServerConnection", "doInBackground :: MalformedURLException");
                e.printStackTrace();
            }
            catch (IOException e) {
                setStatus(AsyncStatus.readingProblem);
                Log.d("ServerConnection", "doInBackground :: IOException");
                e.printStackTrace();
            }
            catch(Exception e) {
                setStatus(AsyncStatus.unknownexception);
                e.printStackTrace();
            }

            for (int i = 0; i < rides.length && !isCancelled(); i++) {
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

            if (getFetchStatus() != AsyncStatus.finished) {
                return null;
            } else {
                return (params == null ? rides : filterRides(rides, params));
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
            switch(getFetchStatus()) {
                case finished:
                    if (mListener != null)
                        mListener.onRidesRetrieved(rides);
                    break;
                case unknownexception:
                    mListener.failedToFetchRiders("שגיאה לא צפויה קרתה, אנא נסה שנית מאוחר יותר");
                    break;
                case readingProblem:
                    mListener.failedToFetchRiders("לא ניתן לאמת את השרת, אנא נסה שנית מאוחר יותר");
                    break;
                case unreachableUrl:
                    mListener.failedToFetchRiders("לא ניתן לאמת את השרת, אנא נסה שנית מאוחר יותר");
                    break;
                case canceled:
                    mListener.failedToFetchRiders("הפעולה בוטלה");
                    break;
                case noInternet:
                    mListener.failedToFetchRiders("לא נמצא חיבור רשת");
                    break;
                case connectionProblem:
                    break;
            }

            this.mListener.onRidesRetrieved(null);
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
