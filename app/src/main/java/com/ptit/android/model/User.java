package com.ptit.android.model;

public class User {
    public String name, phone, birthday, email;
    public User() {

    }

    public User(String name, String phone, String birthday, String email) {
        this.name = name;
        this.phone = phone;
        this.birthday = birthday;
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
}
