package com.example.sathy.studentdataapplication5;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class FilterActivity extends AppCompatActivity {

    DatabaseHelper studentHelper;

    SQLiteDatabase sqLiteDatabaseWrite;
    SQLiteDatabase sqLiteDatabaseRead;

    int offset = 0;
    int beforeId;
    int yearDb;
    int idDb;
    int nextIdFromResponse;
    int serverNextId;
    int mapPageSize = 100;

    private int page = 0;

    String nicknameDb;
    String countryDb;
    String stateDb;
    String cityDb;
    String dbQueryIfDataEqual;
    String baseUrl = "http://bismarck.sdsu.edu/hometown/users?reverse=true&";
    String nextIDUrl = "http://bismarck.sdsu.edu/hometown/nextid";
    String baseSelectQuery = "SELECT * FROM STUDENT";
    String orderByDbQuery = "ORDER BY id DESC limit 25 OFFSET";
    String url = baseUrl;
    String currentUserLoggedIn;
    String baseUrlIfDataNotEqual;
    String urlIfDataUnEqual;
    String urlIfDataEqual;

    Boolean dbDataAndUrlDataNotEqual = false;
    Boolean dbDataAndUrlDataEqual = false;

    JSONObject jsonObjectResponse;

    FirebaseAuth authInstance;

    ArrayAdapter<String> itemsAdapter;
    public static ArrayList<String> firebaseUsers = new ArrayList<String>();
    ListView userListView;

    ArrayList<String> userDetails = new ArrayList<String>();
    ArrayList<Address> addressList = new ArrayList<Address>();
    ArrayList<String> spinnerYears = new ArrayList<String>();
    ArrayList<String> countrySpinnerList;
    ArrayList<String> stateSpinnerList;

    public static String spinnerYear;
    public static String spinnerCountry;
    public static String spinnerState;

    Spinner stateSpinner;

    Button userListButton;
    Button userMapButton;
    Button logOutButton;
    Button chatHistoryButton;

    DatabaseHelper userInfoHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        String url = nextIDUrl;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                serverNextId = Integer.parseInt(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        VolleyQueue.instance(this).add(stringRequest);

        userInfoHelper = new DatabaseHelper(this);

        studentHelper = (new DatabaseHelper(this));
        sqLiteDatabaseWrite = studentHelper.getWritableDatabase();
        sqLiteDatabaseRead = studentHelper.getReadableDatabase();

        authInstance = FirebaseAuth.getInstance();
        currentUserLoggedIn = authInstance.getCurrentUser().getDisplayName();

        setTitle("Logged in to Student Data Application - "+currentUserLoggedIn);

        userListView = (ListView) this.findViewById(R.id.userList);
        itemsAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, userDetails);
        userListView.setAdapter(itemsAdapter);

        countrySpinnerList = new ArrayList<String>();
        countrySpinnerList.add("");
        stateSpinnerList = new ArrayList<String>();

        stateSpinner = (Spinner) findViewById(R.id.filterSpinnerState);

        logOutButton = (Button) this.findViewById(R.id.logOutButtonID);
        userListButton = (Button) this.findViewById(R.id.userListViewButton);
        userMapButton = (Button) this.findViewById(R.id.userMapViewButton);
        chatHistoryButton = (Button) this.findViewById(R.id.chatHistoryButtonID);

        FirebaseDatabase firebaseDb = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDb.getReference();

        setTitle("Logged In To Student Data Application - "+currentUserLoggedIn);

        databaseReference.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                   firebaseUsers.add(child.getKey());
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        StringRequest request = new StringRequest(Request.Method.GET, "http://bismarck.sdsu.edu/hometown/countries",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        response = response.replace("[", "").replace("]", "").replace("\"", "");
                        String[] arrTmp = response.split(",");
                        for (int i = 0; i < arrTmp.length; i++) {
                            countrySpinnerList.add(arrTmp[i]);
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, countrySpinnerList);
                        Spinner countrySpinner = (Spinner) findViewById(R.id.filterSpinnerCountry);
                        countrySpinner.setAdapter(adapter);
                        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {
                                spinnerCountry = adapter.getItemAtPosition(position).toString();
                                generateUrl();
                                if (spinnerCountry.length() == 0 || spinnerCountry == null) {
                                    stateSpinnerList.clear();
                                    stateSpinnerList.add("");
                                    ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, stateSpinnerList);
                                    stateSpinner.setAdapter(adapter1);
                                    spinnerState = "";
                                    generateUrl();
                                } else {
                                    StringRequest request = new StringRequest(Request.Method.GET, "http://bismarck.sdsu.edu/hometown/states?country=" + spinnerCountry,
                                            new Response.Listener<String>() {
                                                @Override
                                                public void onResponse(String response) {
                                                    response = response.replace("[", "").replace("]", "").replace("\"", "");
                                                    String[] arrTmp = response.split(",");
                                                    stateSpinnerList.clear();
                                                    stateSpinnerList.add("");
                                                    for (int i = 0; i < arrTmp.length; i++) {
                                                        stateSpinnerList.add(arrTmp[i]);
                                                    }
                                                    ArrayAdapter<String> adapter1 = new ArrayAdapter<>(getBaseContext(), android.R.layout.simple_spinner_dropdown_item, stateSpinnerList);
                                                    stateSpinner.setAdapter(adapter1);
                                                    stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                        @Override
                                                        public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {
                                                            spinnerState = adapter.getItemAtPosition(position).toString();
                                                            generateUrl();
                                                        }

                                                        @Override
                                                        public void onNothingSelected(AdapterView<?> arg0) {

                                                        }
                                                    });
                                                }
                                            }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {

                                        }
                                    });
                                    VolleyQueue.instance(getBaseContext()).add(request);
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> arg0) {
                            }
                        });
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        VolleyQueue.instance(this).add(request);

        spinnerYears.add("");
        for (int i = 1970; i <= 2017; i++) {
            spinnerYears.add(Integer.toString(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, spinnerYears);
        Spinner spinYear = (Spinner) findViewById(R.id.filterSpinnerYear);
        spinYear.setAdapter(adapter);
        spinYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {
                spinnerYear = adapter.getItemAtPosition(position).toString();
                generateUrl();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        userListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String nicknameBismarckRow = parent.getItemAtPosition(position).toString();
                String[] nicknameBismarckRowArray = nicknameBismarckRow.split(" \\|\\| ");
                String[] nicknameBismarckKeyValue = nicknameBismarckRowArray[0].split(":");
                String nicknameBismarck = nicknameBismarckKeyValue[1].trim();

                if(nicknameBismarck.equals(currentUserLoggedIn)){
                    Toast.makeText(getBaseContext(),"Cannot chat with self!",Toast.LENGTH_SHORT).show();
                }
                else if(firebaseUsers.contains(nicknameBismarck)){
                    goToChat(nicknameBismarck);
                }
                else{
                    Toast.makeText(getBaseContext(),nicknameBismarck+" does not exist in Firebase!",Toast.LENGTH_SHORT).show();
                }
            }
        });

        userListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (!view.canScrollList(1) && scrollState == SCROLL_STATE_IDLE) {
                    Toast.makeText(getBaseContext(), "Fetching more records . . . ", Toast.LENGTH_SHORT).show();
                    if (dbDataAndUrlDataNotEqual){
                        String url = nextIDUrl;
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                nextIdFromResponse = Integer.parseInt(response);
                                page = page + 1;

                                Integer isCountrySelected;
                                Integer isStateSelected;
                                Integer isYearSelected;

                                if (spinnerCountry == null) {
                                    isCountrySelected = 0;
                                } else if (spinnerCountry.length() == 0) {
                                    isCountrySelected = 0;
                                } else {
                                    isCountrySelected = 1;
                                }

                                if (spinnerState == null) {
                                    isStateSelected = 0;
                                } else if (spinnerState.length() == 0) {
                                    isStateSelected = 0;
                                } else {
                                    isStateSelected = 1;
                                }

                                if (spinnerYear == null) {
                                    isYearSelected = 0;
                                } else if (spinnerYear.length() == 0) {
                                    isYearSelected = 0;
                                } else {
                                    isYearSelected = 1;
                                }

                                String urlIfDataUnEqualBeginUrl = baseUrl+"page="+page+"&beforeid=";

                                if ((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==0)){
                                    urlIfDataUnEqual = urlIfDataUnEqualBeginUrl+nextIdFromResponse;
                                }
                                if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==0)){
                                    urlIfDataUnEqual = urlIfDataUnEqualBeginUrl+nextIdFromResponse+"&year="+spinnerYear;
                                }
                                if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==0)){
                                    urlIfDataUnEqual= urlIfDataUnEqualBeginUrl+nextIdFromResponse+"&state="+spinnerState;
                                }
                                if((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==1)){
                                    urlIfDataUnEqual= urlIfDataUnEqualBeginUrl+nextIdFromResponse+"&country="+spinnerCountry;
                                }
                                if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==0)){
                                    urlIfDataUnEqual= urlIfDataUnEqualBeginUrl+nextIdFromResponse+"&year="+spinnerYear+"&state="+spinnerState;
                                }
                                if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==1)){
                                    urlIfDataUnEqual= urlIfDataUnEqualBeginUrl+nextIdFromResponse+"&year="+spinnerYear+"&country="+spinnerCountry;
                                }
                                if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==1)){
                                    urlIfDataUnEqual= urlIfDataUnEqualBeginUrl+nextIdFromResponse+"&state="+spinnerState+"&country="+spinnerCountry;
                                }
                                if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==1)){
                                    urlIfDataUnEqual= urlIfDataUnEqualBeginUrl+nextIdFromResponse+"&year="+spinnerYear+"&state="+spinnerState+"&country="+spinnerCountry;
                                }

                                String url = urlIfDataUnEqual;

                                Response.Listener<JSONArray> success = new Response.Listener<JSONArray>() {
                                    public void onResponse(JSONArray response) {
                                        for (int i=0;i<response.length();i++) {
                                            try {
                                                jsonObjectResponse = (JSONObject) response.get(i);
                                                int id = jsonObjectResponse.getInt("id");
                                                int year = jsonObjectResponse.getInt("year");
                                                String nickName = jsonObjectResponse.getString("nickname");
                                                String country = jsonObjectResponse.getString("country");
                                                String state = jsonObjectResponse.getString("state");
                                                String city = jsonObjectResponse.getString("city");
                                                String timeStamp = jsonObjectResponse.getString("time-stamp");
                                                double latitude = jsonObjectResponse.getDouble("latitude");
                                                double longitude = jsonObjectResponse.getDouble("longitude");
                                                userDetails.add("NickName: "+nickName+" || "+"Country: "+country+" || "+"State: "+state+" || "+"City: "+city+" || "+"Year: "+year);
                                                insertResponse(id,nickName,city,longitude,state,year,latitude,timeStamp,country);
                                                if(latitude == 0.0 || longitude == 0.0)
                                                {
                                                    asyncTaskCall(state,country,nickName);
                                                }
                                            }catch (JSONException error){
                                                error.printStackTrace();
                                            }
                                        }
                                        itemsAdapter.notifyDataSetChanged();
                                    }
                                };
                                Response.ErrorListener failure = new Response.ErrorListener() {
                                    public void onErrorResponse(VolleyError error) {
                                    }
                                };
                                JsonArrayRequest getRequest = new JsonArrayRequest(url, success, failure);
                                VolleyQueue.instance(getBaseContext()).add(getRequest);
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                            }
                        });
                        VolleyQueue.instance(getBaseContext()).add(stringRequest);
                    }
                    if (dbDataAndUrlDataEqual){
                        fetchRecordsFromDataBase();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }

        });

        logOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                sqLiteDatabaseWrite.close();
                sqLiteDatabaseRead.close();
                finish();
            }
        });

        userListButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                displayUserListView();
            }
        });

        userMapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                displayUserMapView();
            }
        });

        chatHistoryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                displayChatHistoryUserListView();
            }
        });
    }

    public void fetchRecordsFromDataBase(){

        Integer isCountrySelected;
        Integer isStateSelected;
        Integer isYearSelected;

        if (spinnerCountry == null) {
            isCountrySelected = 0;
        } else if (spinnerCountry.length() == 0) {
            isCountrySelected = 0;
        } else {
            isCountrySelected = 1;
        }

        if (spinnerState == null) {
            isStateSelected = 0;
        } else if (spinnerState.length() == 0) {
            isStateSelected = 0;
        } else {
            isStateSelected = 1;
        }

        if (spinnerYear == null) {
            isYearSelected = 0;
        } else if (spinnerYear.length() == 0) {
            isYearSelected = 0;
        } else {
            isYearSelected = 1;
        }

        if ((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==0)){
            dbQueryIfDataEqual = baseSelectQuery+" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==0)){
            dbQueryIfDataEqual = baseSelectQuery+" WHERE year = \""+spinnerYear+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==0)){
            dbQueryIfDataEqual = baseSelectQuery+" WHERE state = \""+spinnerState+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==1)){
            dbQueryIfDataEqual = baseSelectQuery+" WHERE country = \""+spinnerCountry+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==0)){
            dbQueryIfDataEqual = baseSelectQuery+" WHERE year = "+spinnerYear+" and state = \""+spinnerState+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==1)){
            dbQueryIfDataEqual = baseSelectQuery+" WHERE year = "+spinnerYear+" and country = \""+spinnerCountry+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==1)){
            dbQueryIfDataEqual = baseSelectQuery+" WHERE state = \""+spinnerState+"\" and country = \""+spinnerCountry+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==1)){
            dbQueryIfDataEqual = baseSelectQuery+" WHERE year = "+spinnerYear+" and state = \""+spinnerState+"\" and country = \""+spinnerCountry+"\" "+orderByDbQuery+" "+offset;
        }

        Cursor databaseReadCursor = sqLiteDatabaseRead.rawQuery(dbQueryIfDataEqual,null);
        if (databaseReadCursor.getCount()< 25) {
            while(databaseReadCursor.moveToNext()){
                idDb = databaseReadCursor.getInt(0);
                yearDb = databaseReadCursor.getInt(5);
                nicknameDb = databaseReadCursor.getString(1);
                countryDb = databaseReadCursor.getString(8);
                stateDb = databaseReadCursor.getString(4);
                cityDb = databaseReadCursor.getString(2);
                userDetails.add("NickName: "+nicknameDb+" || "+"Country: "+countryDb+" || "+"State: "+stateDb+" || "+"City: "+cityDb+" || "+"Year: "+yearDb);
                beforeId = databaseReadCursor.getInt(0);
            }

            String urlIfDataEqualBeginUrl = baseUrl+"page=0&beforeid="+beforeId;

            if ((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==0)){
                urlIfDataEqual = urlIfDataEqualBeginUrl;
            }
            if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==0)){
                urlIfDataEqual = urlIfDataEqualBeginUrl +"&year="+spinnerYear;
            }
            if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==0)){
                urlIfDataEqual = urlIfDataEqualBeginUrl +"&state="+spinnerState;
            }
            if((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==1)){
                urlIfDataEqual = urlIfDataEqualBeginUrl +"&country="+spinnerCountry;
            }
            if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==0)){
                urlIfDataEqual = urlIfDataEqualBeginUrl +"&year="+spinnerYear+"&state="+spinnerState;
            }
            if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==1)){
                urlIfDataEqual = urlIfDataEqualBeginUrl +"&year="+spinnerYear+"&country="+spinnerCountry;
            }
            if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==1)){
                urlIfDataEqual = urlIfDataEqualBeginUrl +"&state="+spinnerState+"&country="+spinnerCountry;
            }
            if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==1)){
                urlIfDataEqual = urlIfDataEqualBeginUrl +"&year="+spinnerYear+"&state="+spinnerState+"&country="+spinnerCountry;
            }

            String url = urlIfDataEqual;
            Response.Listener<JSONArray> success = new Response.Listener<JSONArray>() {
                public void onResponse(JSONArray response) {
                    for (int i=0;i<response.length();i++) {
                        try {
                            jsonObjectResponse = (JSONObject) response.get(i);
                            int id = jsonObjectResponse.getInt("id");
                            int year = jsonObjectResponse.getInt("year");
                            String nickName = jsonObjectResponse.getString("nickname");
                            String country = jsonObjectResponse.getString("country");
                            String state = jsonObjectResponse.getString("state");
                            String city = jsonObjectResponse.getString("city");
                            String timeStamp = jsonObjectResponse.getString("time-stamp");
                            double latitude = jsonObjectResponse.getDouble("latitude");
                            double longitude = jsonObjectResponse.getDouble("longitude");
                            userDetails.add("NickName: "+nickName+" || "+"Country: "+country+" || "+"State: "+state+" || "+"City: "+city+" || "+"Year: "+year);
                            insertResponse(id,nickName,city,longitude,state,year,latitude,timeStamp,country);
                            beforeId = jsonObjectResponse.getInt("id");
                            if(latitude == 0.0 || longitude == 0.0)
                            {
                                asyncTaskCall(state,country,nickName);
                            }
                        }catch (JSONException error){
                            error.printStackTrace();
                        }
                    }
                    itemsAdapter.notifyDataSetChanged();
                }
            };
            Response.ErrorListener failure = new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                }
            };
            JsonArrayRequest getRequest = new JsonArrayRequest(url, success, failure);
            VolleyQueue.instance(this).add(getRequest);
        }
        else
        {
            while(databaseReadCursor.moveToNext()) {
                idDb = databaseReadCursor.getInt(0);
                nicknameDb = databaseReadCursor.getString(1);
                cityDb = databaseReadCursor.getString(2);
                stateDb = databaseReadCursor.getString(4);
                yearDb = databaseReadCursor.getInt(5);
                countryDb = databaseReadCursor.getString(8);
                userDetails.add("NickName: "+nicknameDb+" || "+"Country: "+countryDb+" || "+"State: "+stateDb+" || "+"City: "+cityDb+" || "+"Year: "+yearDb);
                beforeId = databaseReadCursor.getInt(0);
            }
        }
        itemsAdapter.notifyDataSetChanged();
        offset = offset + 25;
    }

    public void insertResponse(int id,String nickname,String city,double longitude,String state,int year,double latitude,String timeStamp,String country){
        ContentValues contentValues = new ContentValues();
        contentValues.put("id",id);
        contentValues.put("nickname",nickname);
        contentValues.put("city",city);
        contentValues.put("longitude",longitude);
        contentValues.put("state",state);
        contentValues.put("year",year);
        contentValues.put("latitude",latitude);
        contentValues.put("timestamp",timeStamp);
        contentValues.put("country",country);
        sqLiteDatabaseWrite.insertWithOnConflict("STUDENT",null,contentValues,sqLiteDatabaseWrite.CONFLICT_IGNORE);
    }

    public void asyncTaskCall(String country,String state,String nickname){
        String[] countryStateNickname = {country,state,nickname};
        new asyncTaskClass().execute(countryStateNickname);
    }

    class asyncTaskClass extends AsyncTask<String,String,LatLng> {
        public LatLng latitudeLongitudeCheckerAndSetter(String state, String country  ){
            double latitude = 0.0;
            double longitude = 0.0;
            Geocoder locator = new Geocoder(getBaseContext());
            try {
                List<Address> address = locator.getFromLocationName(state + ", " + country, 1);
                for (Address mapLocation: address) {
                    if (mapLocation.hasLatitude())
                        latitude = mapLocation.getLatitude();
                    if (mapLocation.hasLongitude())
                        longitude = mapLocation.getLongitude();
                }
            } catch (Exception error) {
                }
            LatLng resultLatitudeLongitude = new LatLng(latitude, longitude);
            return resultLatitudeLongitude;
        }

        String query;

        public LatLng doInBackground(String... LatLongArray){
            LatLng mapLocation;
            mapLocation = latitudeLongitudeCheckerAndSetter(LatLongArray[0],LatLongArray[1]);
            query = LatLongArray[2];
            return (mapLocation);
        }

        public void onPostExecute(LatLng location){
            ContentValues contentValuesLatLong = new ContentValues();
            contentValuesLatLong.put("latitude",location.latitude);
            contentValuesLatLong.put("longitude",location.longitude);
            String[] nicknameArray = new String[]{query};
            sqLiteDatabaseWrite.update("STUDENT",contentValuesLatLong,"nickname=?",nicknameArray);
            Cursor sqLiteDatabaseWriteCursor = sqLiteDatabaseWrite.rawQuery("SELECT nickname,latitude,longitude FROM STUDENT WHERE nickname = \""+query+"\"",null);
            sqLiteDatabaseWriteCursor.moveToFirst();
        }
    }

    public void goToChat(String nicknameBismarck) {
        Intent go = new Intent(this, ChatUserListActivity.class);
        go.putExtra("nicknameBismarck",nicknameBismarck);
        startActivity(go);
    }

    public void generateUrl() {
        Integer isCountrySelected;
        Integer isStateSelected;
        Integer isYearSelected;

        if (spinnerCountry == null) {
            isCountrySelected = 0;
        } else if (spinnerCountry.length() == 0) {
            isCountrySelected = 0;
        } else {
            isCountrySelected = 1;
        }

        if (spinnerState == null) {
            isStateSelected = 0;
        } else if (spinnerState.length() == 0) {
            isStateSelected = 0;
        } else {
            isStateSelected = 1;
        }

        if (spinnerYear == null) {
            isYearSelected = 0;
        } else if (spinnerYear.length() == 0) {
            isYearSelected = 0;
        } else {
            isYearSelected = 1;
        }

        if (isCountrySelected == 0 && isStateSelected == 0 && isYearSelected == 0) {
            url = baseUrl;
        }
        if (isCountrySelected == 0 && isStateSelected == 0 && isYearSelected == 1) {
            url = baseUrl + "year=" + spinnerYear;
        }
        if (isCountrySelected == 0 && isStateSelected == 1 && isYearSelected == 0) {
            url = baseUrl + "state=" + spinnerState;
        }
        if (isCountrySelected == 0 && isStateSelected == 1 && isYearSelected == 1) {
            url = baseUrl + "year=" + spinnerYear + "&state=" + spinnerState;
        }
        if (isCountrySelected == 1 && isStateSelected == 0 && isYearSelected == 0) {
            url = baseUrl + "country=" + spinnerCountry;
        }
        if (isCountrySelected == 1 && isStateSelected == 0 && isYearSelected == 1) {
            url = baseUrl + "year=" + spinnerYear + "&country=" + spinnerCountry;
        }
        if (isCountrySelected == 1 && isStateSelected == 1 && isYearSelected == 0) {
            url = baseUrl + "state=" + spinnerState + "&country=" + spinnerCountry;
        }
        if (isCountrySelected == 1 && isStateSelected == 1 && isYearSelected == 1) {
            url = baseUrl + "year=" + spinnerYear + "&state=" + spinnerState + "&country=" + spinnerCountry;
        }
    }

    public void displayChatHistoryUserListView(){
        Intent go = new Intent(this,ChatHistoryList.class);
        startActivity(go);
    }

    public void displayUserListView() {
        userDetails.clear();

        try {
            spinnerState = URLEncoder.encode(spinnerState,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Integer isCountrySelected;
        Integer isStateSelected;
        Integer isYearSelected;

        if (spinnerCountry == null) {
            isCountrySelected = 0;
        } else if (spinnerCountry.length() == 0) {
            isCountrySelected = 0;
        } else {
            isCountrySelected = 1;
        }

        if (spinnerState == null) {
            isStateSelected = 0;
        } else if (spinnerState.length() == 0) {
            isStateSelected = 0;
        } else {
            isStateSelected = 1;
        }

        if (spinnerYear == null) {
            isYearSelected = 0;
        } else if (spinnerYear.length() == 0) {
            isYearSelected = 0;
        } else {
            isYearSelected = 1;
        }

        if ((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==0)){
            baseUrlIfDataNotEqual=baseUrl+"page=0";
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==0)){
            baseUrlIfDataNotEqual=baseUrl+"page=0&year="+spinnerYear;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==0)){
            baseUrlIfDataNotEqual=baseUrl+"page=0&state="+spinnerState;
        }
        if((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==1)){
            baseUrlIfDataNotEqual=baseUrl+"page=0&country="+spinnerCountry;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==0)){
            baseUrlIfDataNotEqual=baseUrl+"page=0&year="+spinnerYear+"&state="+spinnerState;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==1)){
            baseUrlIfDataNotEqual = baseUrl+"page=0&year="+spinnerYear+"&country="+spinnerCountry;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==1)){
            baseUrlIfDataNotEqual = baseUrl+"page=0&state="+spinnerState+"&country="+spinnerCountry;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==1)){
            baseUrlIfDataNotEqual = baseUrl+"page=0&year="+spinnerYear+"&state="+spinnerState+"&country"+spinnerCountry;
        }

        page = 0;
        Cursor sqLiteDatabaseReadCursor = sqLiteDatabaseRead.rawQuery("select coalesce(max(id),0) from STUDENT",null);
        sqLiteDatabaseReadCursor.moveToFirst();
        int tableMaxId = sqLiteDatabaseReadCursor.getInt(0);
        if (serverNextId == tableMaxId+1){
            dbDataAndUrlDataEqual = true;
            dbDataAndUrlDataNotEqual = false;
            offset = 0;
            userListView.setAdapter(itemsAdapter);
            fetchRecordsFromDataBase();
        }
        else {
            dbDataAndUrlDataNotEqual = true;
            dbDataAndUrlDataEqual = false;
            url = baseUrlIfDataNotEqual;
            Response.Listener<JSONArray> success = new Response.Listener<JSONArray>() {
                public void onResponse(JSONArray response) {
                    for (int i=0;i<response.length();i++) {
                        try {
                            jsonObjectResponse = (JSONObject) response.get(i);
                            int id = jsonObjectResponse.getInt("id");
                            int year = jsonObjectResponse.getInt("year");
                            String nickName = jsonObjectResponse.getString("nickname");
                            String country = jsonObjectResponse.getString("country");
                            String state = jsonObjectResponse.getString("state");
                            String city = jsonObjectResponse.getString("city");
                            String timeStamp = jsonObjectResponse.getString("time-stamp");
                            double latitude = jsonObjectResponse.getDouble("latitude");
                            double longitude = jsonObjectResponse.getDouble("longitude");
                            userDetails.add("NickName: "+nickName+" || "+"Country: "+country+" || "+"State: "+state+" || "+"City: "+city+" || "+"Year: "+year);
                            insertResponse(id,nickName,city,longitude,state,year,latitude,timeStamp,country);
                            if(latitude == 0.0 || longitude == 0.0)
                            {
                                asyncTaskCall(state,country,nickName);
                            }
                        }catch (JSONException error){
                            error.printStackTrace();
                        }
                    }
                    userListView.setAdapter(itemsAdapter);
                }
            };
            Response.ErrorListener failure = new Response.ErrorListener() {
                public void onErrorResponse(VolleyError error) {
                }
            };
            JsonArrayRequest getRequest = new JsonArrayRequest(url, success, failure);
            VolleyQueue.instance(this).add(getRequest);
        }
    }

    public void displayUserMapView() {
        Double latitudeDouble = 0.0, longitudeDouble = 0.0;
        if ((spinnerState == null || spinnerState.length() == 0) && (spinnerCountry == null || spinnerCountry.length() == 0)) {
            latitudeDouble = 0.0;
            longitudeDouble = 0.0;
        } else {
            try {
                Geocoder geocoder = new Geocoder(this);
                addressList = (ArrayList<Address>) geocoder.getFromLocationName(spinnerState + ", " + spinnerCountry, 1);
                for (Address addressValue : addressList) {
                    latitudeDouble = addressValue.getLatitude();
                    longitudeDouble = addressValue.getLongitude();
                }
            } catch (IOException error) {
                }
        }

        try {
            spinnerState = URLEncoder.encode(spinnerState,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Integer isCountrySelected;
        Integer isStateSelected;
        Integer isYearSelected;

        if (spinnerCountry == null) {
            isCountrySelected = 0;
        } else if (spinnerCountry.length() == 0) {
            isCountrySelected = 0;
        } else {
            isCountrySelected = 1;
        }

        if (spinnerState == null) {
            isStateSelected = 0;
        } else if (spinnerState.length() == 0) {
            isStateSelected = 0;
        } else {
            isStateSelected = 1;
        }

        if (spinnerYear == null) {
            isYearSelected = 0;
        } else if (spinnerYear.length() == 0) {
            isYearSelected = 0;
        } else {
            isYearSelected = 1;
        }

        String unEqualBeginUrlStartUrl = baseUrl+"page=0&pagesize=";

        if ((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==0)){
            baseUrlIfDataNotEqual = unEqualBeginUrlStartUrl+mapPageSize;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==0)){
            baseUrlIfDataNotEqual = unEqualBeginUrlStartUrl+mapPageSize+"&year="+spinnerYear;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==0)){
            baseUrlIfDataNotEqual = unEqualBeginUrlStartUrl+mapPageSize+"&state="+spinnerState;
        }
        if((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==1)){
            baseUrlIfDataNotEqual = unEqualBeginUrlStartUrl+mapPageSize+"&country="+spinnerCountry;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==0)){
            baseUrlIfDataNotEqual = unEqualBeginUrlStartUrl+mapPageSize+"&year="+spinnerYear+"&state="+spinnerState;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==1)){
            baseUrlIfDataNotEqual = unEqualBeginUrlStartUrl+mapPageSize+"&year="+spinnerYear+"&country="+spinnerCountry;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==1)){
            baseUrlIfDataNotEqual = unEqualBeginUrlStartUrl+mapPageSize+"&state="+spinnerState+"&country="+spinnerCountry;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==1)){
            baseUrlIfDataNotEqual = unEqualBeginUrlStartUrl+mapPageSize+"&year="+spinnerYear+"&state="+spinnerState+"&country"+spinnerCountry;
        }

        Intent go = new Intent(this, DisplayUsersMapsActivity.class);
        go.putExtra("url", baseUrlIfDataNotEqual);
        go.putExtra("spinnerCountry", spinnerCountry);
        go.putExtra("spinnerState", spinnerState);
        go.putExtra("latitudeDouble", latitudeDouble);
        go.putExtra("longitudeDouble", longitudeDouble);
        startActivity(go);
    }
}