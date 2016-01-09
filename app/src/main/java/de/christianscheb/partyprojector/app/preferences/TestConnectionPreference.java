package de.christianscheb.partyprojector.app.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;

public class TestConnectionPreference extends Preference {

    public TestConnectionPreference(Context context) {
        super(context);
    }

    public TestConnectionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestConnectionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TestConnectionPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
}
