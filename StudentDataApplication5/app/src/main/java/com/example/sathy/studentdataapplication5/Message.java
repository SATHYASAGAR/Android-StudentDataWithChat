package com.example.sathy.studentdataapplication5;

import java.util.Date;

/**
 * Created by sathy on 15-Apr-17.
 */

public class Message {
    private String text;
    private String sender;
    private Date date;

    public Message(String text, String sender) {
        this.text = text;
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String mSender) {
        this.sender = mSender;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
