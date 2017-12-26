package com.example.sathy.studentdataapplication5;

import android.util.Log;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by sathy on 15-Apr-17.
 */

public class ChatMessagesSource {

    private static final Firebase firebaseReference = new Firebase("https://studentdataapplication5.firebaseio.com/chat");
    private static SimpleDateFormat messageSimpleDateFormat = new SimpleDateFormat("yyyyMMddmmss");
    private static final String TAG = "ChatMessagesSource";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_SENDER = "sender";

    public static void saveMessage(Message message, String convoId){
        Date date = message.getDate();
        String key = messageSimpleDateFormat.format(date);
        HashMap<String, String> msg = new HashMap<>();
        msg.put(COLUMN_TEXT, message.getText());
        msg.put(COLUMN_SENDER,message.getSender());
        firebaseReference.child(convoId).child(key).setValue(msg);
    }

    public static MessagesListener addMessagesListener(String convoId, final MessagesCallbacks callbacks){
        MessagesListener listener = new MessagesListener(callbacks);
        firebaseReference.child(convoId).addChildEventListener(listener);
        return listener;
    }

    public static void stop(MessagesListener listener){
        firebaseReference.removeEventListener(listener);
    }

    public static class MessagesListener implements ChildEventListener {
        private MessagesCallbacks callbacks;
        MessagesListener(MessagesCallbacks callbacks){
            this.callbacks = callbacks;
        }
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap<String,String> msg = (HashMap)dataSnapshot.getValue();
            Message message = new Message(msg.get(COLUMN_TEXT),msg.get(COLUMN_SENDER));
            try {
                message.setDate(messageSimpleDateFormat.parse(dataSnapshot.getKey()));
            }catch (Exception e){
                Log.d(TAG, "Error parsing date: "+e);
            }
            if(callbacks != null){
                callbacks.onMessageAdded(message);
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

        }
    }

    public interface MessagesCallbacks{
        void onMessageAdded(Message message);
    }
}
