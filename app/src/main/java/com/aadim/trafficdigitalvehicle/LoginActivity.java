package com.aadim.trafficdigitalvehicle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Date;

public class LoginActivity extends AppCompatActivity {
    Button Login;
    TextInputLayout trafficID, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        trafficID = findViewById(R.id.trafficID);
        password = findViewById(R.id.trafficPassword);
        Login = findViewById(R.id.loginNext_btn);
        TextView Time = findViewById(R.id.time);

        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateTrafficID() || !validatePassword()) {
                    return;
                } else {
                    isUser();
                }
            }
        });
    }

    private Boolean validateTrafficID() {
        String trafficId = trafficID.getEditText().getText().toString();

        if (trafficId.isEmpty()) {
            trafficID.setError("Please enter your mobile number");
            return false;

        } else if (trafficId.length() > 5) {
            trafficID.setError("Please enter valid mobile Number.");
            return false;
        } else {
            trafficID.setError(null);
            trafficID.setErrorEnabled(false);
            return true;
        }
    }

    private Boolean validatePassword() {
        String trafficPass = password.getEditText().getText().toString();

        if (trafficPass.isEmpty()) {
            password.setError("Please enter your password!");
            return false;
        } else {
            password.setError(null);
            password.setErrorEnabled(false);
            return true;
        }
    }

    private void isUser() {
        final String userEnteredID = trafficID.getEditText().getText().toString();
        final String userEnteredPassword = password.getEditText().getText().toString();

        FirebaseDatabase rootNode = FirebaseDatabase.getInstance("https://digitalvehicle-5fc1b-default-rtdb.firebaseio.com");
        DatabaseReference reference = rootNode.getReference("Traffic Users");
        //Query
        Query checkUser = reference.orderByChild("ID").equalTo(userEnteredID);
        checkUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    trafficID.setError(null);
                    password.setError(null);
                    trafficID.setErrorEnabled(false);
                    password.setErrorEnabled(false);

                    String IDFromDB = snapshot.child(userEnteredID).child("ID").getValue(String.class);
                    String passwordFromDB = snapshot.child(userEnteredID).child("Password").getValue(String.class);
                    String nameFromDB = snapshot.child(userEnteredID).child("Name").getValue(String.class);
                    //trafficID.setError(passwordFromDB);
                    if (IDFromDB.equals(userEnteredID) && passwordFromDB.equals(userEnteredPassword)) {
                        Toast.makeText(LoginActivity.this, "Login Success!!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), MenuActivity.class);
                        intent.putExtra("Name", nameFromDB);
                        startActivity(intent);
                    } else {
                        password.setError("Incorrect Password");
                    }

                } else {
                    trafficID.setError("No such user exist");
                    trafficID.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "No users found", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
