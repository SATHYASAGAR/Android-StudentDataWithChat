package com.example.sathy.studentdataapplication5;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class CountryStateListActivity extends AppCompatActivity implements CountryListFragment.callResultListener , StateListFragment.callstateResultListener {

    Button doneButton;
    Button cancelButton;

    String country;
    String state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_state_list);

        doneButton = (Button) this.findViewById(R.id.doneButtonID);
        cancelButton = (Button) this.findViewById(R.id.cancelButtonID);

        FragmentManager fragmentActivity = getSupportFragmentManager();
        android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentActivity.beginTransaction();
        CountryListFragment countryFragment = new CountryListFragment();
        fragmentTransaction.replace(R.id.countryStateFrameLayoutID, countryFragment);
        fragmentTransaction.commit();

        doneButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                done(view);
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                cancel(view);
            }
        });
    }

    public void done(View v) {
        Intent toPassback = getIntent();
        toPassback.putExtra("countryname", country);
        toPassback.putExtra("statename", state);
        setResult(RESULT_OK, toPassback);
        finish();
    }

    public void cancel(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void returnCountry(String countrySelected) {
        country = countrySelected;
    }

    public void returnState(String stateName) {
        state = stateName;
    }

}