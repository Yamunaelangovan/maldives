package io.scanbot.example;

import android.app.Application;

import net.doo.snap.ScanbotSDK;
import net.doo.snap.ScanbotSDKInitializer;
import net.doo.snap.persistence.PageFactory;

/**
 * {@link ScanbotSDKInitializer} should be called
 * in {@code Application.onCreate()} method for RoboGuice modules initialization
 */
public class ExampleApplication extends Application {

    private final String licenseKey =
            "MIlacbvpA3InZaHWrMVVgEQjXd3fLv" +
                    "NRCG/sqXsQh8YK1KljwFYNdiba0N5L" +
                    "RKVjgIfzjiwKfiI+slp9S+5a99cJAd" +
                    "iCu5azyXzml82jmbblsssg+pr7x2Xc" +
                    "jAry23Jl4ZH6QJSjZADzTAJ1rRfZIW" +
                    "S7MKQqzmNZ+/WqxLo3vSgkTtRcnXGU" +
                    "BCwpB+meCmUkLsKbRUn0UVpGD4i60M" +
                    "L/gjTewgb7U/uF54u0+1nHO/yghX2f" +
                    "KkHLzleFhRsNwR5sxdAZsOlmj+Vq8W" +
                    "p1eM5CKRm7a60vRNidmSh0d3920Y+9" +
                    "81p8vCYMOHTO7s/V2qX0AFVRAZ7vrP" +
                    "n6uXNS3jNtyw==\nU2NhbmJvdFNESw" +
                    "pjb20uY2dtLnNjYW4udGVzdAoxNTE2" +
                    "NTc5MTk5Cjk0CjI=\n";

    @Override
    public void onCreate() {
        new ScanbotSDKInitializer()
                // TODO add your license
                 .license(this, licenseKey)
                .initialize(this);
        super.onCreate();
    }

    public PageFactory getpageFactory() {
        return new ScanbotSDK(this).pageFactory();
    }
}
