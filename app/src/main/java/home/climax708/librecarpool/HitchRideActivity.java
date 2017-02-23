package home.climax708.librecarpool;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

import java.util.Calendar;

public class HitchRideActivity extends AppCompatActivity
    implements ServerConnection.OnRidesRetrievedListener,
        RidesAdapter.ViewHolder.OnRideCardClickListener {

    public final static String ARG_HOUR_OF_DAY = "ARG_HOD";
    public final static String ARG_MINUTE = "ARG_MINUTE";
    public static final String ARG_GAPI_CONNECTED = "ARG_GAPI_CONNECTED";

    private String[] departureIDs = {
            "ChIJxRQE9XRQAhURAgSWZPXXP8s",
            "ChIJ7YhnMzlwAhUR5sh_Q-vld3g"
    };

    private View mFilterOptionsLayout;
    private Button mFilterOptionsButton;
    private EditText mDestinationEditText;

    private GoogleApiClient mGoogleApiClient;

    private TextView mDepartureTimeTextView;

    private TextView mResultsTextView;

    private RideTime mRideTime;
    private Place mRideDestination = null;
    private String mRideOriginID;
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    private ServerConnection mServerConnection;
    private RidesAdapter mRidesAdapter;

    private Spinner mOriginSpinner;

    public void clearFields(View view) {
        if (mRideTime == null)
            mRideTime = new RideTime(0, 0);
        else {
            mRideTime.setHour(0);
            mRideTime.setMinute(0);
        }
        if (mDepartureTimeTextView != null)
            mDepartureTimeTextView.setText(mRideTime.toString());

        mRideDestination = null;
        if (mDestinationEditText != null)
            mDestinationEditText.setText("");
        mRideOriginID = null;
        if (mOriginSpinner != null)
            mOriginSpinner.setSelection(0, true);
    }

    public void filterRides(View view) {
        toggleExpandedFilterOptionsLayout(false);

        Ride filterRide;
        if (mRideDestination == null) {
             filterRide = new Ride(null, mRideTime, mRideOriginID, null, "", null);
        } else {
            filterRide = new Ride(null, mRideTime, mRideOriginID, null, mRideDestination, null);
        }
        updateRidesView(filterRide);
    }

    public void contactViaMessage(View view, Ride ride) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + ride.getDriverPhoneNumber()));
        String smsBody = getString(R.string.ride_request_sms_body,
                ride.getDestinationPlace().getName(),
                ride.getDeparturePlace().getName(),
                ride.getRideTime().getHour(), ride.getRideTime().getMinute());
        intent.putExtra("sms_body", smsBody);
        intent.putExtra(Intent.EXTRA_TEXT, smsBody);
        startActivity(Intent.createChooser(intent, ""));
    }

    public void contactViaCall(View view, Ride ride) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ride.getDriverPhoneNumber()));
        startActivity(intent);
    }

    public void expandLayout(View view) {
        toggleExpandedFilterOptionsLayout(true);

        if (mDestinationEditText.getText().length() == 0) {
            if (mGoogleApiClient.isConnected()) {
                try {
                    Intent intent = Utils.buildPlaceAutocompleteIntent(HitchRideActivity.this);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                    mDestinationEditText.requestFocus();
                } catch (GooglePlayServicesRepairableException e) {
                    // TODO: Handle the error.
                } catch (GooglePlayServicesNotAvailableException e) {
                    // TODO: Handle the error.
                }
            } else {
                mDestinationEditText.requestFocus();
                // Force the keyboard to show.
                Utils.forceToggleKeyboardForView(HitchRideActivity.this, mDestinationEditText, true);
            }
        }
    }

    public void collapseLayout(View view) {
        toggleExpandedFilterOptionsLayout(false);

        // Force the keyboard to hide.
        Utils.forceToggleKeyboardForView(HitchRideActivity.this, mDestinationEditText, false);
    }

    public void showTimePickerDialog(View v) {
        TimePickerFragment timePickerFragment = new TimePickerFragment();

        if (mRideTime != null) {
            Bundle bundle = new Bundle();
            bundle.putInt(ARG_HOUR_OF_DAY, mRideTime.getHour());
            bundle.putInt(ARG_MINUTE, mRideTime.getMinute());
            timePickerFragment.setArguments(bundle);
        }

        timePickerFragment.setOnTimeListener(new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mRideTime = new RideTime(hourOfDay, minute);
                mDepartureTimeTextView.setText(mRideTime.toString());
            }
        });
        timePickerFragment.show(getSupportFragmentManager(), "timePicker");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hitch_ride);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setupActionBar(toolbar);

        mFilterOptionsLayout = findViewById(R.id.ride_filter_layout);
        mFilterOptionsButton = (Button) findViewById(R.id.ride_filter_toggle_button);

        mDestinationEditText = (EditText) findViewById(R.id.hitch_destination_edittext);
        mDestinationEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mGoogleApiClient.isConnected()) {
                        try {
                            mDestinationEditText.requestFocus();
                            Intent intent = Utils.buildPlaceAutocompleteIntent(HitchRideActivity.this);
                            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                        } catch (GooglePlayServicesRepairableException e) {
                            // TODO: Handle the error.
                        } catch (GooglePlayServicesNotAvailableException e) {
                            // TODO: Handle the error.
                        }
                    }
                }
                return false;
            }
        });

        FloatingActionButton offerRideFAB = (FloatingActionButton) findViewById(R.id.offerFAB);
        setupOfferFAB(offerRideFAB);

        mDepartureTimeTextView = (TextView) findViewById(R.id.hitch_departure_time_textview);
        final Calendar calendar = Calendar.getInstance();
        mRideTime = new RideTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        mDepartureTimeTextView.setText(mRideTime.toString());

        mResultsTextView = (TextView) findViewById(R.id.resultsTextView);

        mOriginSpinner = (Spinner) findViewById(R.id.hitch_origin_spinner);
        setupOriginSpinner(mOriginSpinner);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, new GoogleApiConnectionFailListener())
                .build();

        mServerConnection = new ServerConnection();
        RecyclerView ridesRecyclerView = (RecyclerView) findViewById(R.id.rides_recycler_view);
        mRidesAdapter = new RidesAdapter(new Ride[0], this);
        if (ridesRecyclerView != null) {
            ridesRecyclerView.setAdapter(mRidesAdapter);
        }
        setupRidesRecyclerView(ridesRecyclerView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_hitch_ride, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // TODO: Launch settings activity.
                break;
            // TODO: Implement "about" menu item.
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mFilterOptionsLayout.getVisibility() == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                toggleExpandedFilterOptionsLayout(false);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Refresh rides when application is (re)started.
        // Make sure it takes filter rides into consideration (if any).
        if (mRideOriginID != null || mRideDestination != null)
            updateRidesView(new Ride(null, mRideTime, mRideOriginID, null, mRideDestination, null));
        else
            updateRidesView();
    }

    @Override
    public void onStop() {
        super.onStop();
        mServerConnection.cancelTask();
    }

    @Override
    public void onRidesRetrievingStarted() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.ridesProgressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        mResultsTextView.setVisibility(View.GONE);
    }

    @Override
    public void onRidesRetrieved(Ride[] rides) {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.ridesProgressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (rides.length == 0) {
            mResultsTextView.setText(R.string.no_rides_found);
            mResultsTextView.setVisibility(View.VISIBLE);
        } else {
            mRidesAdapter.addItems(rides);
        }
    }

    @Override
    public void onRidesRetrievingFailed(Exception exception) {
        // Clear current rides
        mRidesAdapter.clear();

        // Display error TextView
        mResultsTextView.setText(R.string.rides_retrieval_error);
        mResultsTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRideCardClick(View v, int position) {
        final Ride clickedRide = mRidesAdapter.getRide(position);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View dialogLayout = getLayoutInflater().inflate(
                R.layout.dialog_ride_info_layout, null);

        AlertDialog rideDialog = dialogBuilder.setTitle(R.string.ride_dialog_info_title)
                .setView(dialogLayout)
                .setIcon(R.mipmap.ic_time_to_leave_black_24dp)
                .setNeutralButton(R.string.dialog_button_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();

        int primaryColor = Utils.getThemeAttributeColor(HitchRideActivity.this,
                Utils.ColorType.PRIMARY);

        final TextView destinationTextView = (TextView) dialogLayout.findViewById(
                R.id.ride_info_destination_text_view);
        if (destinationTextView != null) {
            SpannableStringBuilder destinationName = new SpannableStringBuilder(
                    clickedRide.getDestinationPlace().getName());
            destinationName.setSpan(new ForegroundColorSpan(primaryColor), 0, destinationName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            SpannableStringBuilder departure = new SpannableStringBuilder(
                    clickedRide.getDeparturePlace().getName());
            departure.setSpan(new ForegroundColorSpan(primaryColor), 0, departure.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            // We don't care about locale for this specific string because it only displays time in 24 hour format.
            @SuppressLint("DefaultLocale") SpannableStringBuilder departureTime = new SpannableStringBuilder(
                    String.format("%d:%02d",  clickedRide.getRideTime().getHour(),
                            clickedRide.getRideTime().getMinute()));
            departureTime.setSpan(new ForegroundColorSpan(primaryColor), 0, departureTime.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            destinationTextView.setText(
                    Utils.getSpannedString(
                            getString(R.string.ride_dialog_info_destination),
                            destinationName,
                            departure,
                            departureTime));
        }
        final TextView throughTextView = (TextView) dialogLayout.findViewById(
                R.id.ride_info_through_text_view);
        if (throughTextView != null) {
            String passingThrough = clickedRide.getPassingThrough();
            if (passingThrough != null && !passingThrough.isEmpty()) {
                throughTextView.setVisibility(View.VISIBLE);
                SpannableStringBuilder passingThroughPlaceName = new SpannableStringBuilder(
                        passingThrough);
                passingThroughPlaceName.setSpan(new ForegroundColorSpan(primaryColor), 0,
                        passingThroughPlaceName.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                throughTextView.setText(Utils.getSpannedString(
                        getString(R.string.ride_dialog_info_through),
                        passingThroughPlaceName));
            }
        }

        String rideComments = clickedRide.getComments();
        if (rideComments != null && !rideComments.isEmpty()) {
            final TextView commentsTextView = (TextView) dialogLayout.findViewById(
                    R.id.ride_info_comments_text_view);
            if (commentsTextView != null && !rideComments.isEmpty()) {
                commentsTextView.setVisibility(View.VISIBLE);
                SpannableStringBuilder comments = new SpannableStringBuilder(rideComments);
                comments.setSpan(new ForegroundColorSpan(primaryColor), 0, comments.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                commentsTextView.setText(Utils.getSpannedString(
                        getString(R.string.ride_dialog_info_comments), comments));
            }
        }

        final TextView callTextView = (TextView) dialogLayout.findViewById(
                R.id.ride_dialog_call_textView);
        if (callTextView != null) {
            callTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    contactViaCall(v, clickedRide);
                }
            });
            Drawable topDrawable = callTextView.getCompoundDrawables()[1];
            if (topDrawable != null) {
                topDrawable.setColorFilter(
                        ContextCompat.getColor(
                                HitchRideActivity.this, android.R.color.holo_green_dark),
                        PorterDuff.Mode.SRC_ATOP);
            }
        }

        final TextView smsTextView = (TextView) dialogLayout.findViewById(
                R.id.ride_dialog_sms_textView);
        if (smsTextView != null) {
            smsTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    contactViaMessage(v, clickedRide);
                }
            });
            Drawable topDrawable = smsTextView.getCompoundDrawables()[1];
            if (topDrawable != null) {
                topDrawable.setColorFilter(
                        ContextCompat.getColor(
                                HitchRideActivity.this, android.R.color.holo_orange_light),
                        PorterDuff.Mode.SRC_ATOP);
            }
        }

        rideDialog.show();
        rideDialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(
                Utils.getThemeAttributeColor(HitchRideActivity.this, Utils.ColorType.ACCENT));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                mRideDestination = place;
                mDestinationEditText.setText(place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.

            } else if (resultCode == RESULT_CANCELED) {
            }
            findViewById(R.id.coordinator_layout).requestFocus();
            // Force the keyboard to hide.
            Utils.forceToggleKeyboardForView(HitchRideActivity.this, mDestinationEditText, false);
        }
    }

    private void setupRidesRecyclerView(RecyclerView recyclerView) {
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(
                (int) getResources().getDimension(R.dimen.ride_list_divider_height)));

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void updateRidesView() {
        updateRidesView(null);
    }

    private void updateRidesView(Ride... rides) {
        mRidesAdapter.clear();
        if (mGoogleApiClient != null) {
            mServerConnection.getRides(mGoogleApiClient, this, rides);
        }
    }

    private class GoogleApiConnectionFailListener
            implements GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Toast.makeText(HitchRideActivity.this,
                    "Connection to GoogleApiClient failed with result=" + result,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setupActionBar(Toolbar toolbar) {
        setSupportActionBar(toolbar);
    }

    private void setupOfferFAB(FloatingActionButton offerRideFAB) {
        offerRideFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HitchRideActivity.this, OfferRideActivity.class);
                intent.putExtra(ARG_GAPI_CONNECTED, mGoogleApiClient.isConnected());
                startActivity(intent);
            }
        });
    }

    private void setupOriginSpinner(Spinner originSpinner) {
        originSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View v, int position, long id) {
                if (position > 0 ) {
                    mRideOriginID = departureIDs[position - 1];
                } else {
                    mRideOriginID = null;
                }
            }
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        ArrayAdapter<CharSequence> originsAdapter = ArrayAdapter.createFromResource(this,
                R.array.origins, android.R.layout.simple_spinner_item);
        originsAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        originSpinner.setAdapter(originsAdapter);
    }

    private void toggleExpandedFilterOptionsLayout(boolean toggle) {
        int EXPAND_COLLAPSE_DURATION = 350;
        if (toggle) {
            Utils.collapseView(mFilterOptionsButton, EXPAND_COLLAPSE_DURATION);
            Utils.expandView(mFilterOptionsLayout, EXPAND_COLLAPSE_DURATION);
        } else {
            Utils.collapseView(mFilterOptionsLayout, EXPAND_COLLAPSE_DURATION);
            Utils.expandView(mFilterOptionsButton, EXPAND_COLLAPSE_DURATION);
        }
    }
}
