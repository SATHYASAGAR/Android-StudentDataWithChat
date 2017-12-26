package com.example.sathy.studentdataapplication5;
import android.app.Application;

import com.firebase.client.Firebase;
/**
 * Created by sathy on 15-Apr-17.
 */

public class SetAndroidContext extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
    }
}
