package com.example.sathy.studentdataapplication5;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.ArrayList;

public class CountryListFragment extends Fragment {

    ArrayAdapter<String> countryListArrayAdapter;
    ArrayList<String> countryArrayList;
    ListView countryListView;

    public CountryListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        countryArrayList=new ArrayList<String>();
        countryListArrayAdapter=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,countryArrayList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_country_list, container, false);
        countryListView = (ListView) v.findViewById(R.id.list);

        StringRequest request = new StringRequest(Request.Method.GET, "http://bismarck.sdsu.edu/hometown/countries",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        response=response.replace("[","").replace("]","").replace("\"","");
                        String[] responseStringArray = response.split(",");

                        for(int i=0;i<responseStringArray.length;i++){
                            countryArrayList.add(responseStringArray[i]);
                        }

                        countryListView.setAdapter(countryListArrayAdapter);

                        countryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                callResultListener countryReturnResult =(callResultListener) getActivity();
                                countryReturnResult.returnCountry(parent.getItemAtPosition(position).toString());
                                String countryTostate = parent.getItemAtPosition(position).toString();
                                StateListFragment stateListFragment =new StateListFragment();

                                Bundle countryBundle = new Bundle();
                                countryBundle.putString("country", countryTostate);
                                stateListFragment.setArguments(countryBundle);

                                FragmentManager frag = getFragmentManager();
                                android.support.v4.app.FragmentTransaction Frag_transaction = frag.beginTransaction();
                                Frag_transaction.replace(R.id.countryStateFrameLayoutID, stateListFragment);
                                Frag_transaction.commit();
                            }
                        });
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        VolleyQueue.instance(getActivity()).add(request);
        return v;
    }
    public interface callResultListener{
        public void returnCountry(String countrySelected);
    }
}