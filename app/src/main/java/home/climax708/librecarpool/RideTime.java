package home.climax708.librecarpool;

public class RideTime {

    private int mHour;
    private int mMinute;

    public RideTime(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
    }

    public RideTime(String timeStr) {
        String[] splitStr = timeStr.split(":");
        mHour   = Integer.parseInt(splitStr[0]);
        mMinute = Integer.parseInt(splitStr[1]);
    }

    public void setHour(int hour) {
        if (hour < 0 || hour > 23)
            mHour = 0;
        else
            mHour = hour;
    }

    public void setMinute(int minute) {
        if (minute < 0 || minute > 59)
            mMinute = 0;
        else
            mMinute = minute;
    }

    public int getHour() {
        return mHour;
    }

    public int getMinute() {
        return mMinute;
    }

    @Override
    public String toString() {
        return String.format("%02d:%02d", mHour, mMinute);
    }

}
