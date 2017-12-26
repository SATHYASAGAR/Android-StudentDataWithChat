package com.example.sathy.studentdataapplication5;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class LoginOrRegister extends AppCompatActivity {

    String currentUser;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private final String TAG = "Create Email ";

    Button setCountryState, setLatLong, registerUser, clearAllFields, loginButton, backButton;
    EditText countryField, stateField;
    EditText email, nickName, password, loginEmail, loginPassword, city, longitude, latitude;
    Spinner spinYear;

    String spinnerYear;

    public static int COUNTRY_STATE_INTENT_ID = 123;
    public static int MAPS_INTENT_ID = 456;

    ArrayList<String> spinnerYears = new ArrayList<String>();
    ArrayList<Address> addressList = new ArrayList<Address>();
    Double latitudeDouble;
    Double longitudeDouble;
    Integer isFormValid = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_or_register);
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        setCountryState = (Button) findViewById(R.id.setCountryStateButtonID);
        registerUser = (Button) findViewById(R.id.registerUserID);
        registerUser.setEnabled(false); //Button will be disabled until Nick Name entered is valid
        setLatLong = (Button) findViewById(R.id.setLatLongID);
        clearAllFields = (Button) findViewById(R.id.clearAllFieldsButton);
        loginButton = (Button) findViewById(R.id.loginButtonID);

        email = (EditText) findViewById(R.id.emailID);
        password = (EditText) findViewById(R.id.passwordID);
        loginEmail = (EditText) findViewById(R.id.loginEmailID);
        loginPassword = (EditText) findViewById(R.id.loginPasswordID);
        nickName = (EditText) findViewById(R.id.nickNameID);
        city = (EditText) findViewById(R.id.cityID);
        longitude = (EditText) findViewById(R.id.longitudeID);
        longitude.setEnabled(false);
        latitude = (EditText) findViewById(R.id.latitudeID);
        latitude.setEnabled(false);
        countryField = (EditText) findViewById(R.id.countryID);
        countryField.setEnabled(false);
        stateField = (EditText) findViewById(R.id.stateID);
        stateField.setEnabled(false);

        spinYear = (Spinner) findViewById(R.id.yearSpinnerID);

        for (int i = 1970; i <= 2017; i++) {
            spinnerYears.add(Integer.toString(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerYears);
        spinYear.setAdapter(adapter);
        spinYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v,
                                       int position, long id) {
                spinnerYear = adapter.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        setCountryState.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                callIntentToCountryList(view);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if(loginEmail.getText().toString().length()==0 || loginPassword.getText().toString().length()==0){
                    Toast.makeText(getBaseContext(),"Error: Please enter both email id and password",Toast.LENGTH_SHORT).show();
                }
                else if(!(loginEmail.getText().toString().contains("@") && loginEmail.getText().toString().contains("."))){
                    Toast.makeText(getBaseContext(),"Error: Please enter a valid email id",Toast.LENGTH_SHORT).show();
                }
                else{
                    signIn(loginEmail.getText().toString(), loginPassword.getText().toString());
                }

            }
        });

        setLatLong.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setLatLongOnMaps(view);
            }
        });

        registerUser.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                uploadDataToServer();

            }
        });

        clearAllFields.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                clearFields();
            }
        });

        nickName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                nickName.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        RequestQueue queue = Volley.newRequestQueue(getBaseContext());
                        StringRequest request = new StringRequest(Request.Method.GET, "http://bismarck.sdsu.edu/hometown/nicknameexists?name=" + nickName.getText().toString(),
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        if (response.equalsIgnoreCase("true")) {
                                            nickName.setError("Nick Name Exists!");
                                            registerUser.setEnabled(false);
                                        } else {
                                            registerUser.setEnabled(true);
                                        }
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                            }
                        });
                        queue.add(request);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }


    public void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                        if (task.isSuccessful()) {
                                    Intent goToFilterActivity= new Intent(getBaseContext(),FilterActivity.class);
                                    startActivity(goToFilterActivity);
                        }
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(getBaseContext(), "Error: Sign in Authentication failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    public void clearFields() {
        nickName.setText("");
        password.setText("");
        city.setText("");
        longitude.setText("");
        latitude.setText("");
        countryField.setText("");
        stateField.setText("");
    }

    public void uploadDataToServer() {
        final String nickNameText = nickName.getText().toString();
        String passwordText = password.getText().toString();
        String cityText = city.getText().toString();
        String countryText = countryField.getText().toString();
        String stateText = stateField.getText().toString();

        validateFormData(nickNameText, passwordText, cityText, countryText, stateText);

        if (isFormValid == 1) {
            try {
                setDefaultLatitudeLongitude(stateText, countryText);
            } catch (IOException error) {
                Log.i("rew", "Geocoder IOException Error in Form Activity " + error);
            }
        }

        if (isFormValid == 1) {
            JSONObject data = new JSONObject();
            try {
                data.put("latitude", latitudeDouble);
                data.put("longitude", longitudeDouble);
                data.put("nickname", nickNameText);
                data.put("password", passwordText);
                data.put("country", countryText);
                data.put("state", stateText);
                data.put("city", cityText);
                data.put("year", Integer.parseInt(spinnerYear));
            } catch (JSONException error) {
                Log.e("rew", "JSON error", error);
                return;
            }
            Response.Listener<JSONObject> success = new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Toast.makeText(getBaseContext(), "Data Uploaded to Bismarck!", Toast.LENGTH_SHORT).show();
                }
            };
            Response.ErrorListener failure = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.i("rew", "post fail " + new String(error.networkResponse.data));
                }
            };
            String url = "http://bismarck.sdsu.edu/hometown/adduser";
            JsonObjectRequest postRequest = new JsonObjectRequest(url, data, success, failure);
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(postRequest);


            mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                            if(task.isSuccessful()){
                                FirebaseUser user = task.getResult().getUser();
                                final UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(nickNameText)
                                        .build();
                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                if (task.isSuccessful()) {
                                                    Toast.makeText(LoginOrRegister.this, "User registration successful!", Toast.LENGTH_SHORT)
                                                            .show();
                                                }
                                            }
                                        });

                            }else{
                                Toast.makeText(getBaseContext(), "Error: Email entered is incorrect", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

            postDataToFirebase(nickNameText,countryText,stateText,cityText,Integer.parseInt(spinnerYear));

        }

    }

    public void postDataToFirebase(String nickNameText, String countryText, String stateText, String cityText, Integer yearInt){
        FirebaseDatabase reference = FirebaseDatabase.getInstance();
        Student student=new Student(nickNameText,countryText,stateText,cityText,yearInt);
        DatabaseReference dbRef = reference.getReference("users");
        dbRef.child(nickNameText).setValue(student);
        Toast.makeText(this, "Data Uploaded to Firebase!", Toast.LENGTH_SHORT).show();
    }

    public void validateFormData(String nickNameText, String passwordText, String cityText, String countryText, String stateText) {
        isFormValid = 1;
        if(email.getText().toString().length()==0){
            Toast.makeText(this,"Error: Please enter your email id",Toast.LENGTH_SHORT).show();
        }
        if(!(email.getText().toString().contains("@") && loginEmail.getText().toString().contains("."))){
            Toast.makeText(this,"Error: Please enter a valid email id",Toast.LENGTH_SHORT).show();
        }
        if (nickNameText.length() == 0) {
            Toast.makeText(this, "Error: Please enter your Nick Name", Toast.LENGTH_SHORT).show();
            isFormValid = 0;
        } else if (nickNameText.contains(" ")) {
            Toast.makeText(this, "Error: Nick Name cannot have white space", Toast.LENGTH_SHORT).show();
            isFormValid = 0;
        }
        if (passwordText.length() < 3) {
            Toast.makeText(this, "Error: Password must be at least three characters", Toast.LENGTH_SHORT).show();
            isFormValid = 0;
        }
        if (countryText.length() == 0 || stateText.length() == 0) {
            Toast.makeText(this, "Error: Please select your Country and State", Toast.LENGTH_SHORT).show();
            isFormValid = 0;
        }
        if (cityText.length() == 0) {
            Toast.makeText(this, "Error: Please enter your City", Toast.LENGTH_SHORT).show();
            isFormValid = 0;
        }
    }

    public void setLatLongOnMaps(View view) {
        Intent go = new Intent(this, SetLatLongMapsActivity.class);
        startActivityForResult(go, MAPS_INTENT_ID);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == COUNTRY_STATE_INTENT_ID) {
            switch (resultCode) {
                case RESULT_OK:
                    String countryname = data.getStringExtra("countryname");
                    String stateName = data.getStringExtra("statename");
                    countryField.setText(countryname);
                    stateField.setText(stateName);
                    break;
                case RESULT_CANCELED:
                    break;
            }
        }
        if (requestCode == MAPS_INTENT_ID) {
            switch (resultCode) {
                case RESULT_OK:
                    latitudeDouble = data.getDoubleExtra("latitude", 0.0);
                    longitudeDouble = data.getDoubleExtra("longitude", 0.0);
                    latitude.setText(latitudeDouble.toString());
                    longitude.setText(longitudeDouble.toString());
                    break;
                case RESULT_CANCELED:
                    break;
            }
        }
    }

    public void setDefaultLatitudeLongitude(String stateText, String countryText) throws IOException {
        if (latitude.getText().toString().length() == 0 || longitude.getText().toString().length() == 0) {
            Geocoder geocoder = new Geocoder(this);
            addressList = (ArrayList<Address>) geocoder.getFromLocationName(stateText + ", " + countryText, 1);
            for (Address addressValue : addressList) {

                latitudeDouble = addressValue.getLatitude();
                longitudeDouble = addressValue.getLongitude();
            }
            latitude.setText(latitudeDouble.toString());
            longitude.setText(longitudeDouble.toString());

        }
    }

    public void callIntentToCountryList(View view) {
        Intent go = new Intent(this, CountryStateListActivity.class);
        startActivityForResult(go, COUNTRY_STATE_INTENT_ID);
    }
}
