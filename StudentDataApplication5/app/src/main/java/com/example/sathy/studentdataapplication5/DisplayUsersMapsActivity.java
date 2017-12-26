package com.example.sathy.studentdataapplication5;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.example.sathy.studentdataapplication5.FilterActivity.firebaseUsers;
import static com.example.sathy.studentdataapplication5.FilterActivity.spinnerCountry;
import static com.example.sathy.studentdataapplication5.FilterActivity.spinnerState;
import static com.example.sathy.studentdataapplication5.FilterActivity.spinnerYear;

public class DisplayUsersMapsActivity extends FragmentActivity implements OnMapReadyCallback , GoogleMap.OnInfoWindowClickListener{

    private GoogleMap mMap;

    MarkerOptions mapMarkerOptions = new MarkerOptions();

    Button displayMoreButton;
    Button backButton;

    String unEqualBeginUrl;
    String nicknameDb;
    String countryDb;
    String stateDb;
    String cityDb;

    double cameraLatitude;
    double cameraLongitude;
    double latitudeDb;
    double longitudeDb;

    int serverNextId;
    int page=0;
    int offset=0;
    int nextIdFromResponse;
    int idDb;
    int yearDb;
    int beforeId;

    DatabaseHelper studentHelper;

    SQLiteDatabase sqLiteDatabaseWrite;
    SQLiteDatabase sqLiteDatabaseRead;

    Boolean dbDataAndUrlDataNotEqual = false;
    Boolean dbDataAndUrlDataEqual = false;

    String urlIfDataUnEqual;
    String urlIfDataEqual;
    String dbQueryIfDataEqual;
    String currentUserLoggedIn;
    String baseUrl = "http://bismarck.sdsu.edu/hometown/users?reverse=true&";
    String nextIDUrl = "http://bismarck.sdsu.edu/hometown/nextid";
    String baseSelectQuery = "SELECT * FROM STUDENT";
    String orderByDbQuery = "ORDER BY id DESC limit 100 OFFSET";

    JSONObject jsonObjectResponse;

    Integer countryZoomLevel=3;
    Integer stateZoomLevel=5;
    Integer worldZoomLevel=1;
    Integer mapZoomLevel=1;

    int mapPageSize = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_users_maps);

        displayMoreButton = (Button) this.findViewById(R.id.displayMoreButtonID);
        backButton = (Button) this.findViewById(R.id.backButtonID);

        FirebaseAuth authInstance = FirebaseAuth.getInstance();
        currentUserLoggedIn = authInstance.getCurrentUser().getDisplayName();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        studentHelper = (new DatabaseHelper(this));
        sqLiteDatabaseWrite = studentHelper.getWritableDatabase();
        sqLiteDatabaseRead = studentHelper.getReadableDatabase();

        displayMoreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                displayMoreUsers();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                goBack();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(this);
        mMap.clear();

        unEqualBeginUrl = getIntent().getStringExtra("url");
        cameraLatitude = getIntent().getDoubleExtra("latitudeDouble",0.0);
        cameraLongitude = getIntent().getDoubleExtra("longitudeDouble",0.0);

        if(spinnerCountry==null || spinnerCountry.length()==0){
            mapZoomLevel=worldZoomLevel;
        }
        else if(spinnerState==null || spinnerState.length()==0){
            mapZoomLevel=countryZoomLevel;
        }
        else{
            mapZoomLevel=stateZoomLevel;
        }

        LatLng movedMarkerLocation = new LatLng(cameraLatitude,cameraLongitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(movedMarkerLocation));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(mapZoomLevel),2000,null);

        String url = nextIDUrl;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                serverNextId = Integer.parseInt(response);
                page = 0;
                Cursor sqLiteDatabaseReadCursor = sqLiteDatabaseRead.rawQuery("SELECT coalesce(max(id),0) FROM STUDENT",null);
                sqLiteDatabaseReadCursor.moveToFirst();
                int tableMaxId = sqLiteDatabaseReadCursor.getInt(0);
                if (serverNextId == tableMaxId+1){
                    dbDataAndUrlDataEqual = true;
                    dbDataAndUrlDataNotEqual = false;
                    offset = 0;
                    fetchDataFromSql();
                }
                else {
                    dbDataAndUrlDataNotEqual = true;
                    dbDataAndUrlDataEqual = false;
                    fetchDataFromServerAndSql();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        VolleyQueue.instance(this).add(stringRequest);
    }

    public void goBack(){
        finish();
    }

    public void displayMoreUsers(){
        if (dbDataAndUrlDataNotEqual){
            String url = nextIDUrl;
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    nextIdFromResponse = Integer.parseInt(response);
                    displayMoreUsersContinued();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
            VolleyQueue.instance(this).add(stringRequest);
        }
        if (dbDataAndUrlDataEqual){
            fetchDataFromSql();
        }
    }

    public void fetchDataFromServerAndSql(){
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
                        insertResponse(id,nickName,city,longitude,state,year,latitude,timeStamp,country);
                        if(latitude != 0.0 && longitude != 0.0)
                        {
                            LatLng location = new LatLng(latitude, longitude);
                            mapMarkerOptions.position(location).title(nickName);
                            mMap.addMarker(mapMarkerOptions);
                        }
                        else
                        {
                            asyncTaskCall(state,country,nickName);
                        }
                    }catch (JSONException error){
                        error.printStackTrace();
                    }
                }

            }
        };
        Response.ErrorListener failure = new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                }
        };
        JsonArrayRequest getRequest = new JsonArrayRequest(unEqualBeginUrl, success, failure);
        VolleyQueue.instance(this).add(getRequest);
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

    public void displayMoreUsersContinued(){
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

        String urlIfDataUnEqualBeginUrl = baseUrl+"page="+page+"&pagesize="+mapPageSize;

        if ((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==0)){
            urlIfDataUnEqual = urlIfDataUnEqualBeginUrl +"&beforeid="+nextIdFromResponse;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==0)){
            urlIfDataUnEqual = urlIfDataUnEqualBeginUrl +"&beforeid="+nextIdFromResponse+"&year="+spinnerYear;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==0)){
            urlIfDataUnEqual= urlIfDataUnEqualBeginUrl +"&beforeid="+nextIdFromResponse+"&state="+spinnerState;
        }
        if((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==1)){
            urlIfDataUnEqual= urlIfDataUnEqualBeginUrl +"&beforeid="+nextIdFromResponse+"&country="+spinnerCountry;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==0)){
            urlIfDataUnEqual= urlIfDataUnEqualBeginUrl +"&beforeid="+nextIdFromResponse+"&year="+spinnerYear+"&state="+spinnerState;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==1)){
            urlIfDataUnEqual= urlIfDataUnEqualBeginUrl +"&beforeid="+nextIdFromResponse+"&year="+spinnerYear+"&country="+spinnerCountry;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==1)){
            urlIfDataUnEqual= urlIfDataUnEqualBeginUrl +"&beforeid="+nextIdFromResponse+"&state="+spinnerState+"&country="+spinnerCountry;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==1)){
            urlIfDataUnEqual= urlIfDataUnEqualBeginUrl +"&beforeid="+nextIdFromResponse+"&year="+spinnerYear+"&state="+spinnerState+"&country="+spinnerCountry;
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
                        insertResponse(id,nickName,city,longitude,state,year,latitude,timeStamp,country);

                        if( longitude != 0.0 && latitude != 0.0)
                        {
                            LatLng markerLocation = new LatLng(latitude, longitude);
                            mapMarkerOptions.position(markerLocation).title(nickName);
                            mMap.addMarker(mapMarkerOptions);
                        }
                        else
                        {
                            asyncTaskCall(state,country,nickName);
                        }
                    }catch (JSONException error){
                        error.printStackTrace();
                    }
                }
            }
        };
        Response.ErrorListener failure = new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                    }
        };
        JsonArrayRequest getRequest = new JsonArrayRequest(url, success, failure);
        VolleyQueue.instance(this).add(getRequest);
    }

    public void fetchDataFromSql(){

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
            dbQueryIfDataEqual = baseSelectQuery +" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==0)){
            dbQueryIfDataEqual = baseSelectQuery +" where year = "+spinnerYear+" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==0)){
            dbQueryIfDataEqual = baseSelectQuery +" where state = \""+spinnerState+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==1)){
            dbQueryIfDataEqual = baseSelectQuery +" where country = \""+spinnerCountry+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==0)){
            dbQueryIfDataEqual = baseSelectQuery +" where year = "+spinnerYear+" and state = \""+spinnerState+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==1)){
            dbQueryIfDataEqual = baseSelectQuery +" where year = "+spinnerYear+" and country = \""+spinnerCountry+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==1)){
            dbQueryIfDataEqual = baseSelectQuery +" where state = \""+spinnerState+"\" and country = \""+spinnerCountry+"\" "+orderByDbQuery+" "+offset;
        }
        if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==1)){
            dbQueryIfDataEqual = baseSelectQuery +" where year = "+spinnerYear+" and state = \""+spinnerState+"\" and country = \""+spinnerCountry+"\" "+orderByDbQuery+" "+offset;
        }

        Cursor sqLiteDatabaseReadcursor = sqLiteDatabaseRead.rawQuery(dbQueryIfDataEqual,null);

        if (sqLiteDatabaseReadcursor.getCount()< mapPageSize) {
            while(sqLiteDatabaseReadcursor.moveToNext()){
                idDb = sqLiteDatabaseReadcursor.getInt(0);
                nicknameDb = sqLiteDatabaseReadcursor.getString(1);
                cityDb = sqLiteDatabaseReadcursor.getString(2);
                longitudeDb = sqLiteDatabaseReadcursor.getDouble(3);
                stateDb = sqLiteDatabaseReadcursor.getString(4);
                yearDb = sqLiteDatabaseReadcursor.getInt(5);
                latitudeDb = sqLiteDatabaseReadcursor.getDouble(6);
                countryDb = sqLiteDatabaseReadcursor.getString(8);
                LatLng mapLocation = new LatLng(latitudeDb, longitudeDb);
                mapMarkerOptions.position(mapLocation).title(nicknameDb);
                mMap.addMarker(mapMarkerOptions);
                beforeId = sqLiteDatabaseReadcursor.getInt(0);
            }

            String urlIfDataEqualBeginUrl = baseUrl+"page=0&pagesize="+mapPageSize+"&beforeid="+beforeId;

            if ((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==0)){
                urlIfDataEqual = urlIfDataEqualBeginUrl;
            }
            if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==0)){
                urlIfDataEqual = urlIfDataEqualBeginUrl+"&year="+spinnerYear;
            }
            if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==0)){
                urlIfDataEqual = urlIfDataEqualBeginUrl+"&state="+spinnerState;
            }
            if((isYearSelected==0)&&(isStateSelected==0)&&(isCountrySelected==1)){
                urlIfDataEqual = urlIfDataEqualBeginUrl+"&country="+spinnerCountry;
            }
            if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==0)){
                urlIfDataEqual = urlIfDataEqualBeginUrl+"&year="+spinnerYear+"&state="+spinnerState;
            }
            if((isYearSelected==1)&&(isStateSelected==0)&&(isCountrySelected==1)){
                urlIfDataEqual = urlIfDataEqualBeginUrl+"&year="+spinnerYear+"&country="+spinnerCountry;
            }
            if((isYearSelected==0)&&(isStateSelected==1)&&(isCountrySelected==1)){
                urlIfDataEqual = urlIfDataEqualBeginUrl+"&state="+spinnerState+"&country="+spinnerCountry;
            }
            if((isYearSelected==1)&&(isStateSelected==1)&&(isCountrySelected==1)){
                urlIfDataEqual = urlIfDataEqualBeginUrl+"&year="+spinnerYear+"&state="+spinnerState+"&country="+spinnerCountry;
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
                            insertResponse(id,nickName,city,longitude,state,year,latitude,timeStamp,country);
                            beforeId = jsonObjectResponse.getInt("id");
                            if(latitude != 0.0 && longitude != 0.0)
                            {
                                LatLng location = new LatLng(latitude, longitude);
                                mapMarkerOptions.position(location).title(nickName);
                                mMap.addMarker(mapMarkerOptions);
                            }
                            else
                            {
                                asyncTaskCall(state,country,nickName);
                            }
                        }catch (JSONException error){
                            error.printStackTrace();
                        }
                    }
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
            while(sqLiteDatabaseReadcursor.moveToNext()) {
                idDb = sqLiteDatabaseReadcursor.getInt(0);
                nicknameDb = sqLiteDatabaseReadcursor.getString(1);
                cityDb = sqLiteDatabaseReadcursor.getString(2);
                longitudeDb = sqLiteDatabaseReadcursor.getDouble(3);
                stateDb = sqLiteDatabaseReadcursor.getString(4);
                yearDb = sqLiteDatabaseReadcursor.getInt(5);
                latitudeDb = sqLiteDatabaseReadcursor.getDouble(6);
                countryDb = sqLiteDatabaseReadcursor.getString(8);
                LatLng location = new LatLng(latitudeDb, longitudeDb);
                mapMarkerOptions.position(location).title(nicknameDb);
                mMap.addMarker(mapMarkerOptions);
                beforeId = sqLiteDatabaseReadcursor.getInt(0);
            }
        }
        offset = offset + mapPageSize;
    }

    public void asyncTaskCall(String country,String state,String name){
        String[] countryStateNickname = {country,state,name};
        new asyncTaskClass().execute(countryStateNickname);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        String nicknameBismarck = marker.getTitle();
        if (nicknameBismarck.equals(currentUserLoggedIn)){
            Toast.makeText(getBaseContext(),"Cannot chat with self!",Toast.LENGTH_SHORT).show();
        }else if(firebaseUsers.contains(nicknameBismarck)){
            goToChat(nicknameBismarck);
        }
        else{
            Toast.makeText(getBaseContext(),"User "+nicknameBismarck+" does not exist in Firebase!",Toast.LENGTH_SHORT).show();
        }
    }

    public void goToChat(String nicknameBismarck){
        Intent go = new Intent(this, ChatUserListActivity.class);
        go.putExtra("nicknameBismarck",nicknameBismarck);
        startActivity(go);
    }

    class asyncTaskClass extends AsyncTask<String,String,LatLng> {
        public LatLng latitudeLongitudeCheckerAndSetter(String state, String country  ){
            double latitude = 0.0;
            double longitude = 0.0;
            Geocoder locator = new Geocoder(getBaseContext());
            try {
                List<Address> address = locator.getFromLocationName(state + ", " + country, 1);
                for (Address location: address) {
                    if (location.hasLatitude())
                        latitude = location.getLatitude();
                    if (location.hasLongitude())
                        longitude = location.getLongitude();
                }
            } catch (Exception error) {
                }
            LatLng resultLatitudeLongitude = new LatLng(latitude, longitude);
            return resultLatitudeLongitude;
        }

        String query;

        public LatLng doInBackground(String... latLongArray){
            LatLng mapLocation;
            mapLocation = latitudeLongitudeCheckerAndSetter(latLongArray[0],latLongArray[1]);
            query = latLongArray[2];
            return (mapLocation);
        }

        public void onPostExecute(LatLng mapLocation){
            mMap.addMarker(mapMarkerOptions.position(mapLocation).title(query));
            ContentValues contentValuesLatLong = new ContentValues();
            contentValuesLatLong.put("latitude",mapLocation.latitude);
            contentValuesLatLong.put("longitude",mapLocation.longitude);
            String[] contentName = new String[]{query};
            sqLiteDatabaseWrite.update("STUDENT",contentValuesLatLong,"nickname=?",contentName);
        }
    }
}