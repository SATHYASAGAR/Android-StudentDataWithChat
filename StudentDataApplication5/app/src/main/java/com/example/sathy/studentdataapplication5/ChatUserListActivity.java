package com.example.sathy.studentdataapplication5;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


public class ChatUserListActivity extends AppCompatActivity implements ChatMessagesSource.MessagesCallbacks{

    FirebaseAuth authInstance;
    ArrayList<Message> messageList;
    MessagesAdapter messageAdapter;
    String messageReceiver;
    String conversationId;
    String currentLoggedInUser;
    ListView messagesListView;
    ChatMessagesSource.MessagesListener messageListener;
    EditText newMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_user_list);

        Button sendMessage = (Button)findViewById(R.id.sendMessageButtonID);

        messageReceiver = getIntent().getStringExtra("nicknameBismarck");
        authInstance = FirebaseAuth.getInstance();
        currentLoggedInUser = authInstance.getCurrentUser().getDisplayName();
        String[] ids = {currentLoggedInUser, messageReceiver};
        Arrays.sort(ids);
        conversationId = ids[0]+"-"+ids[1];
        messagesListView = (ListView)findViewById(R.id.list);
        messageList = new ArrayList<>();
        messageAdapter = new MessagesAdapter(messageList);
        messagesListView.setAdapter(messageAdapter);

        setTitle(messageReceiver);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        messageListener = ChatMessagesSource.addMessagesListener(conversationId, this);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sendMessageMethod(view);
            }
        });
    }

    public void sendMessageMethod(View v) {
        newMessageView = (EditText)findViewById(R.id.typeMessageID);
        String newMessage = newMessageView.getText().toString();
        newMessageView.setText("");

        Message msg = new Message(newMessage,currentLoggedInUser);
        msg.setDate(new Date());

        ChatMessagesSource.saveMessage(msg, conversationId);
    }

    @Override
    public void onMessageAdded(Message message) {
        messageList.add(message);
        messageAdapter.notifyDataSetChanged();
    }

       @Override
    protected void onDestroy() {
        super.onDestroy();
        ChatMessagesSource.stop(messageListener);
    }

    private class MessagesAdapter extends ArrayAdapter<Message> {
        MessagesAdapter(ArrayList<Message> messages){
            super(ChatUserListActivity.this, R.layout.message_item, R.id.message, messages);
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = super.getView(position, convertView, parent);
            Message message = getItem(position);

            TextView messageView = (TextView)convertView.findViewById(R.id.message);
            messageView.setText(message.getText());

            LinearLayout.LayoutParams layoutParameters = (LinearLayout.LayoutParams)messageView.getLayoutParams();

            if (message.getSender().equals(currentLoggedInUser)){
                layoutParameters.gravity = Gravity.RIGHT;
            }else{
                layoutParameters.gravity = Gravity.LEFT;
            }

            messageView.setLayoutParams(layoutParameters);
            return convertView;
        }

    }
}
