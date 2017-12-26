package com.example.sathy.studentdataapplication5;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryList extends AppCompatActivity {

    ArrayAdapter<String> itemsAdapter;
    ArrayList<String> chatHistoryUsersList = new ArrayList<String>();

    ListView userHistoryList;

    FirebaseAuth authInstance;

    String currentLoggedInUser;

    Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_history_list);

        userHistoryList = (ListView) this.findViewById(R.id.userHistoryListID);
        itemsAdapter = new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, chatHistoryUsersList);
        userHistoryList.setAdapter(itemsAdapter);

        backButton = (Button)findViewById(R.id.chatListBackButtonID);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference();

        authInstance = FirebaseAuth.getInstance();
        currentLoggedInUser = authInstance.getCurrentUser().getDisplayName();

         databaseReference.child("chat").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Iterable<DataSnapshot> children = dataSnapshot.getChildren();
                for (DataSnapshot child : children) {
                    String[] childList = child.getKey().split("-");
                    if(childList[0].equals(currentLoggedInUser)){
                        chatHistoryUsersList.add(childList[1]);
                    }
                    else if(childList[1].equals(currentLoggedInUser)){
                        chatHistoryUsersList.add(childList[0]);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        itemsAdapter.notifyDataSetChanged();

        userHistoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String chatRecepient = parent.getItemAtPosition(position).toString();
                    goToChat(chatRecepient);
                }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                goBack();
            }
        });
    }

    public void goBack(){
        finish();
    }

    public void goToChat(String chatRecepient){
        Intent go = new Intent(this, ChatUserListActivity.class);
        go.putExtra("nicknameBismarck",chatRecepient);
        startActivity(go);
    }
}
