package home.climax708.librecarpool;

import com.google.android.gms.location.places.Place;

/**
 * Created by maxim on 24-Feb-17.
 */

public class Ride {

    private final String mDriverPhoneNumber;

    private final RideTime mDepartureRideTime;
    private final String mDeparturePlaceID;
    private final Place mDeparturePlace;

    private final Place mDestinationPlace;
    private final String mDestinationPlaceID;

    private final String mPassingThrough;

    private final String mComments;

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

    public static class Builder {
        private String mDriverPhoneNumber;

        private RideTime mDepartureRideTime;
        private String mDeparturePlaceID;
        private Place mDeparturePlace;

        private Place mDestinationPlace;
        private String mDestinationPlaceID;

        private String mPassingThrough;

        private String mComments;

        public Builder setDriverPhoneNumber(String driverPhoneNumber) {
            mDriverPhoneNumber = driverPhoneNumber;
            return this;
        }

        public Builder setDepartureRideTime(RideTime departureRideTime) {
            mDepartureRideTime = departureRideTime;
            return this;
        }

        public Builder setDeparturePlaceId(String departurePlaceId) {
            mDeparturePlaceID = departurePlaceId;
            return this;
        }

        public Builder setDeparturePlace(Place departurePlace) {
            mDeparturePlace = departurePlace;
            return this;
        }

        public Builder setDestinationPlaceId(String destinationPlaceId) {
            mDestinationPlaceID = destinationPlaceId;
            return this;
        }

        public Builder setDestinationPlace(Place destinationPlace) {
            mDestinationPlace = destinationPlace;
            return this;
        }

        public Builder setPassingThrough(String passingThrough) {
            mPassingThrough = passingThrough;
            return this;
        }

        public Builder setComments(String comments) {
            mComments = comments;
            return this;
        }

        public Ride build() {
            return new Ride(mDriverPhoneNumber, mDepartureRideTime,
                    mDeparturePlaceID, mDeparturePlace, mDestinationPlaceID,
                    mDestinationPlace, mPassingThrough, mComments);
        }
    }

    private Ride(String driverPhoneNumber, RideTime departureRideTime,
                  String departurePlaceId, Place departurePlace,
                  String destinationPlaceId, Place destinationPlace,
                  String passingThrough, String comments) {
        mDriverPhoneNumber  = driverPhoneNumber;
        mDepartureRideTime  = departureRideTime;
        mDeparturePlaceID   = departurePlaceId;
        mDeparturePlace     = departurePlace;
        mDestinationPlaceID = destinationPlaceId;
        mDestinationPlace   = destinationPlace;
        mPassingThrough     = passingThrough;
        mComments           = comments;
    }
}
