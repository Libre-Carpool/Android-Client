package home.climax708.librecarpool;

import android.os.AsyncTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

/**
 * Created by maxim on 24-Feb-17.
 */

public class GetGooglePlaceTask extends AsyncTask<String, Void, Place[]> {

    private GoogleApiClient mGoogleApiClient;

    public GetGooglePlaceTask(GoogleApiClient client) {
        mGoogleApiClient = client;
    }

    @Override
    protected Place[] doInBackground(String... params) {
        if (params == null)
            return null;

        int paramsLength = params.length;

        if (paramsLength == 0)
            return null;

        Place[] retrievedGooglePlaces = new Place[paramsLength];

        PendingResult<PlaceBuffer> placesBuffer;
        placesBuffer = Places.GeoDataApi
                .getPlaceById(mGoogleApiClient, params);

        PlaceBuffer placeBuffer = placesBuffer.await();

        // Freezing the place object is necessary for later use since we are closing the buffer.
        // refer to: https://developers.google.com/places/android-api/buffers
        for (int i = 0; i < paramsLength; i++) {
            retrievedGooglePlaces[i] = placeBuffer.get(i).freeze();
        }
        
        placeBuffer.release();

        return retrievedGooglePlaces;
    }
}
