package com.example.sathy.studentdataapplication5;

/**
 * Created by sathy on 15-Apr-17.
 */

public class Student {
    String nickname;
    String country;
    String state;
    String city;
    Integer year;

    public Student(String nickname, String country, String state, String city, Integer year) {
        this.nickname = nickname;
        this.country = country;
        this.state = state;
        this.city = city;
        this.year = year;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }
}
