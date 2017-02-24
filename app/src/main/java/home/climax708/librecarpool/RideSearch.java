package home.climax708.librecarpool;

/**
 * Created by maxim on 24-Feb-17.
 */

public class RideSearch {

    private final String mDeparturePlaceId;
    private final RideTime mDepartureTime;

    private final String mDestinationPlaceId;

    public RideSearch(RideTime rideTime, String departurePlaceID,
                      String destinationPlaceId) {
        mDepartureTime      = rideTime;
        mDeparturePlaceId   = departurePlaceID;
        mDestinationPlaceId = destinationPlaceId;
    }

    public String getDeparturePlaceId() {
        return mDeparturePlaceId;
    }

    public RideTime getDepartureTime() {
        return mDepartureTime;
    }

    public String getDestinationPlaceId() {
        return mDestinationPlaceId;
    }
}
