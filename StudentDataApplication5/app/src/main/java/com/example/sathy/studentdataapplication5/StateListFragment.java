package com.example.sathy.studentdataapplication5;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayList;

public class StateListFragment extends Fragment {

    ListView listViewState;

    ArrayAdapter<String> arrayAdapterState;
    ArrayList<String> stateArrayList;

    String countryName = null;

    public StateListFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        countryName = getArguments().getString("country");
        stateArrayList=new ArrayList<String>();
        arrayAdapterState=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1,stateArrayList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState){
        View view= inflater.inflate(R.layout.fragment_state_list, container, false);
        listViewState = (ListView) view.findViewById(R.id.list);

        StringRequest request = new StringRequest(Request.Method.GET, "http://bismarck.sdsu.edu/hometown/states?country="+countryName+"",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        response=response.replace("[", "").replace("]","").replace("\"","");
                        String[] arrTmp = response.split(",");
                        for(int i=0;i<arrTmp.length;i++){
                            stateArrayList.add(arrTmp[i]);
                        }
                        listViewState.setAdapter(arrayAdapterState);
                        listViewState.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                callstateResultListener stateReturnResult = (callstateResultListener) getActivity();
                                stateReturnResult.returnState(parent.getItemAtPosition(position).toString());
                                Toast.makeText(getContext(),"Country, State Selected", Toast.LENGTH_SHORT ).show();
                            }
                        });
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        });
        VolleyQueue.instance(getActivity()).add(request);
        return view;
    }
    public interface callstateResultListener{
        void returnState(String stateName);
    }
}
