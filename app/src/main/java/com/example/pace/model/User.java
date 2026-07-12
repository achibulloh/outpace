package com.example.pace.model;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private String dob;
    private String weight;
    private String height;
    private String monthly_target;

    public User() {
        // Required for Firestore
    }

    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }

    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }

    public String getMonthlyTarget() { return monthly_target; }
    public void setMonthlyTarget(String monthly_target) { this.monthly_target = monthly_target; }
}
