package home.climax708.librecarpool;

import com.google.android.gms.location.places.Place;

public class Ride {

    private String mDriverPhoneNumber;

    private RideTime mDepartureRideTime;
    private String mDeparturePlaceID;
    private Place mDeparturePlace;

    private Place mDestinationPlace;
    private String mDestinationPlaceID;

    private String mPassingThrough;

    private String mComments;

    String getDriverPhoneNumber() {
        return mDriverPhoneNumber;
    }

    RideTime getRideTime() {
        return mDepartureRideTime;
    }

    String getDeparturePlaceID() {
        return mDeparturePlaceID;
    }

    Place getDeparturePlace() {
        return mDeparturePlace;
    }

    String getDestinationPlaceID() {
        return mDestinationPlaceID;
    }

    Place getDestinationPlace() {
        return mDestinationPlace;
    }

    String getPassingThrough() {
        return mPassingThrough;
    }

    String getComments() {
        return mComments;
    }

    void setDeparturePlace(Place place) {
        mDeparturePlace = place;
        mDeparturePlaceID = place.getId();
    }

    void setDestinationPlace(Place place) {
        mDestinationPlace = place;
        mDestinationPlaceID = place.getId();
    }

    public Ride(String driverPhoneNumber, RideTime departureTime,
                String departurePlaceID,
                String passingThrough,
                Place destinationPlace,
                String driverComments) {
        this(driverPhoneNumber, departureTime, departurePlaceID, passingThrough,
                destinationPlace.getId(), driverComments);
        mDestinationPlace = destinationPlace;
    }

    public Ride(String driverPhoneNumber, RideTime departureTime,
                String departurePlaceID, String passingThrough, String destinationPlaceID,
                String driverComments) {
        mDriverPhoneNumber = driverPhoneNumber;
        mDepartureRideTime = departureTime;
        mDeparturePlaceID = departurePlaceID;
        mPassingThrough = passingThrough;
        mDestinationPlaceID = destinationPlaceID;
        mComments = driverComments;
    }
}
