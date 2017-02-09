package home.climax708.librecarpool;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

import java.util.Calendar;

public class OfferRideActivity extends AppCompatActivity implements View.OnTouchListener {

    private EditText mDestinationEditText;
    private TextView mDepartureTimeTextView;
    private RideTime mRideTime;
    private EditText mThroughEditText;

    private boolean isGoogleApiClientConnected;

    private static final int DESTINATION_PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int THROUGH_PLACE_AUTOCOMPLETE_REQUEST_CODE     = 2;

    public void saveRide(View view) {
        if (mRideTime == null) {
            Toast.makeText(getApplicationContext(), R.string.missing_time, Toast.LENGTH_SHORT).show();
        } else if (mDestinationEditText.getText().length() <= 0) {
            Toast.makeText(getApplicationContext(), R.string.missing_destination, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), R.string.saved, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void showTimePickerDialog(View v) {
        TimePickerFragment timePickerFragment = new TimePickerFragment();

        if (mRideTime != null) {
            Bundle bundle = new Bundle();
            bundle.putInt(HitchRideActivity.ARG_HOUR_OF_DAY, mRideTime.getHour());
            bundle.putInt(HitchRideActivity.ARG_MINUTE, mRideTime.getMinute());
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
        setContentView(R.layout.activity_offer_ride);

        Toolbar toolbar = (Toolbar) findViewById(R.id.offerRideToolbar);
        setupActionBar(toolbar);

        isGoogleApiClientConnected = getIntent().getExtras().getBoolean(HitchRideActivity.ARG_GAPI_CONNECTED);

        mDestinationEditText = (EditText) findViewById(R.id.origin_destination_edittext);
        if (mDestinationEditText != null) {
            mDestinationEditText.setOnTouchListener(this);
        }

        mThroughEditText = (EditText) findViewById(R.id.origin_through_edittext);
        if (mThroughEditText != null) {
            mThroughEditText.setOnTouchListener(this);
        }

        mDepartureTimeTextView = (TextView) findViewById(R.id.offer_departure_time_textview);
        final Calendar calendar = Calendar.getInstance();
        mRideTime = new RideTime(calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE));
        mDepartureTimeTextView.setText(mRideTime.toString());

        Spinner originSpinner = (Spinner) findViewById(R.id.offer_origin_spinner);
        setupOriginSpinner(originSpinner);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_offer_ride, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                // TODO: Launch settings activity.
                break;
            case android.R.id.home: {
                finish();
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DESTINATION_PLACE_AUTOCOMPLETE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(this, data);
                    mDestinationEditText.setText(place.getName());
                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Status status = PlaceAutocomplete.getStatus(this, data);
                    // TODO: Handle the error.

                } else if (resultCode == RESULT_CANCELED) {
                }
                // Force the keyboard to hide.
                Utils.forceToggleKeyboardForView(OfferRideActivity.this, mDestinationEditText, false);
                break;
        }
    }

    private void setupActionBar(Toolbar toolbar) {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupOriginSpinner(Spinner originSpinner) {
        originSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View v, int position, long id) {
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.origin_destination_edittext:
                mDestinationEditText.requestFocus();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (isGoogleApiClientConnected) {
                        try {
                            Intent intent = Utils.buildPlaceAutocompleteIntent(OfferRideActivity.this);
                            startActivityForResult(intent, DESTINATION_PLACE_AUTOCOMPLETE_REQUEST_CODE);
                        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                            // TODO: Handle the error.
                        }
                    }
                }
                break;
            case R.id.origin_through_edittext:
                mThroughEditText.requestFocus();
                break;
        }
        return false;
    }
}
