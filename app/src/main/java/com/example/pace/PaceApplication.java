package com.example.pace;

import android.app.Application;
import android.content.Context;
import com.example.pace.utils.LocaleHelper;

public class PaceApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleHelper.onAttach(this);
    }
}
