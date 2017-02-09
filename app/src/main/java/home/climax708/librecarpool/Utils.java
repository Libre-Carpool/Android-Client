package home.climax708.librecarpool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern STRING_FORMAT_ARGS_PATTERN = Pattern.compile(".[%\\d]\\$(\\w+)");

    enum ColorType {
        PRIMARY,
        PRIMARY_DARK,
        ACCENT
    }

    public static Intent buildPlaceAutocompleteIntent(Activity activity)
            throws GooglePlayServicesRepairableException, GooglePlayServicesNotAvailableException
    {
        return new PlaceAutocomplete.IntentBuilder(
                PlaceAutocomplete.MODE_OVERLAY)
                .setFilter(new AutocompleteFilter.Builder()
                        .setCountry("IL")
                        .build())
                .build(activity);
    }

    public static void expandView(final View view) {
        // Use 1dp/ms
        expandView(view,
                (int)(view.getMeasuredHeight() / view.getContext().getResources().getDisplayMetrics().density));
    }
    public static void expandView(final View view, int duration) {
        view.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        final int finalViewHeight = view.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        view.getLayoutParams().height = (Build.VERSION.SDK_INT < 21) ?  1 : 0;
        view.setVisibility(View.VISIBLE);

        Animation expandAnimation = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                view.getLayoutParams().height = interpolatedTime == 1
                        ? LayoutParams.WRAP_CONTENT
                        : (int)(finalViewHeight * interpolatedTime);
                view.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        expandAnimation.setDuration(duration);
        view.startAnimation(expandAnimation);
    }

    public static void collapseView(final View view) {
        // Use 1dp/ms
        collapseView(view,
                (int)(view.getMeasuredHeight() / view.getContext().getResources().getDisplayMetrics().density));
    }

    public static void collapseView(final View view, int duration) {
        final int initialHeight = view.getMeasuredHeight();

        Animation collapseAnimation = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    view.setVisibility(View.GONE);
                }else{
                    view.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    view.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };
        collapseAnimation.setDuration(duration);
        view.startAnimation(collapseAnimation);
    }

    public static void forceToggleKeyboardForView(Activity activity, View view, boolean toggle){
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);

        if (!toggle){
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0); // hide
        } else {
            imm.showSoftInput(view, 0);
        }
    }

    public static SpannedString getSpannedString(CharSequence format, Object... args) {
        return getSpannedString(Locale.getDefault(), format, args);
    }

    public static SpannedString getSpannedString(Locale locale, CharSequence format,
                                                 Object... args) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(format);

        Matcher matcher = STRING_FORMAT_ARGS_PATTERN.matcher(stringBuilder);
        int findStart = 0;
        int argIndex = 0;
        while (matcher.find(findStart)) {
            String argType = matcher.group(1);
            int argStart = matcher.start(0);
            int argEnd = matcher.end(0);
            if (argType.equals("s") && args[argIndex] instanceof Spanned) {
                stringBuilder.replace(argStart, argEnd, (Spanned) args[argIndex++]);
            } else {
                stringBuilder.replace(argStart, argEnd,
                        String.format(locale, '%' + matcher.group(1), args[argIndex++]));
            }

            if (argEnd > stringBuilder.length())
                break;
            else
                findStart = argStart + 1;

            matcher = STRING_FORMAT_ARGS_PATTERN.matcher(stringBuilder);
        }

        return new SpannedString(stringBuilder);
    }

    public static int getThemeAttributeColor(Context context, Utils.ColorType type) {
        TypedValue typedValue = new TypedValue();

        int[] colorAttribute = new int[1];
        switch (type) {
            case PRIMARY:
                colorAttribute[0] = R.attr.colorPrimary;
                break;
            case PRIMARY_DARK:
                colorAttribute[0] = R.attr.colorPrimaryDark;
                break;
            case ACCENT:
                colorAttribute[0] = R.attr.colorAccent;
                break;
        }

        TypedArray a = context.obtainStyledAttributes(typedValue.data, colorAttribute);
        int color = a.getColor(0, 0);

        a.recycle();

        return color;
    }
}