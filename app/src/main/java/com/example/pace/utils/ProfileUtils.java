package com.example.pace.utils;

import com.google.firebase.firestore.DocumentSnapshot;

public class ProfileUtils {
    public static boolean isProfileComplete(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return false;
        
        return doc.contains("phone") && doc.get("phone") != null && !String.valueOf(doc.get("phone")).isEmpty() &&
               doc.contains("weight") && doc.get("weight") != null && !String.valueOf(doc.get("weight")).isEmpty() &&
               doc.contains("height") && doc.get("height") != null && !String.valueOf(doc.get("height")).isEmpty() &&
               doc.contains("gender") && doc.get("gender") != null && !String.valueOf(doc.get("gender")).isEmpty() &&
               doc.contains("dob") && doc.get("dob") != null && !String.valueOf(doc.get("dob")).isEmpty() &&
               doc.contains("name") && doc.get("name") != null && !String.valueOf(doc.get("name")).isEmpty();
    }
}
