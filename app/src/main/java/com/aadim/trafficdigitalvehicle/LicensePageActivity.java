package com.aadim.trafficdigitalvehicle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LicensePageActivity extends AppCompatActivity {
    public static final String resultParameter = "result";
    TextView category, bloodGroup, name, licenseNo, address, citizenshipNo, mobileNo, dob, doi, doe, fatherName;
    ImageView images;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_license_page);

        Intent i = getIntent();
        String text = i.getStringExtra(resultParameter);
        String strSplit[] = text.split("by");
        String mobileNumber = strSplit[0];
        String licenseNumber = strSplit[1];

        FirebaseDatabase rootNode = FirebaseDatabase.getInstance("https://digitalvehicle-5fc1b-default-rtdb.firebaseio.com/");
        DatabaseReference reference = rootNode.getReference("Licenses");
        Query checkUser = reference.orderByChild("licenseNo").equalTo(licenseNumber);
        checkUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullNameFromDB = snapshot.child(mobileNumber).child("name").getValue(String.class);
                    String addressFromDB = snapshot.child(mobileNumber).child("address").getValue(String.class);
                    String mobileNoFromDB = snapshot.child(mobileNumber).child("mobileNo").getValue(String.class);
                    String licenseNoFromDB = snapshot.child(mobileNumber).child("licenseNo").getValue(String.class);
                    String bloodGroupFromDB = snapshot.child(mobileNumber).child("bloodGroup").getValue(String.class);
                    String dobFromDB = snapshot.child(mobileNumber).child("dob").getValue(String.class);
                    String doeFromDB = snapshot.child(mobileNumber).child("doe").getValue(String.class);
                    String doiFromDB = snapshot.child(mobileNumber).child("doi").getValue(String.class);
                    String fatherNameFromDB = snapshot.child(mobileNumber).child("fatherName").getValue(String.class);
                    String licenseTypeFromDB = snapshot.child(mobileNumber).child("licenseType").getValue(String.class);
                    String citizenshipNoFromDB = snapshot.child(mobileNumber).child("citizenshipNo").getValue(String.class);
                    String imageFromDB = snapshot.child(mobileNumber).child("ProfileImage").getValue(String.class);

                    images = findViewById(R.id.imageUser);
                    category = findViewById(R.id.txtCategory);
                    bloodGroup = findViewById(R.id.txtBloodGroup);
                    licenseNo = findViewById(R.id.txtLicense);
                    name = findViewById(R.id.txtName);
                    citizenshipNo = findViewById(R.id.txtCitizenshipNo);
                    fatherName = findViewById(R.id.txtFatherName);
                    dob = findViewById(R.id.txtDOB);
                    doi = findViewById(R.id.txtDOI);
                    address = findViewById(R.id.txtAddress);
                    doe = findViewById(R.id.txtDOE);
                    mobileNo = findViewById(R.id.txtMobileNo);

                    name.setText(fullNameFromDB);
                    address.setText(addressFromDB);
                    mobileNo.setText(mobileNoFromDB);
                    licenseNo.setText(licenseNoFromDB);
                    bloodGroup.setText(bloodGroupFromDB);
                    dob.setText(dobFromDB);
                    doe.setText(doeFromDB);
                    doi.setText(doiFromDB);
                    fatherName.setText(fatherNameFromDB);
                    category.setText(licenseTypeFromDB);
                    citizenshipNo.setText(citizenshipNoFromDB);

                    Glide.with(LicensePageActivity.this)
                            .load(imageFromDB)
                            .apply(new RequestOptions().override(1500, 1500))
                            .centerCrop()
                            .into(images);

                } else {
                    Toast.makeText(getApplicationContext(), "No such license details!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }


        });
        Button compareLicense = findViewById(R.id.compareBtn);

        compareLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), CompareImageActivity.class);
                startActivity(i);
            }
        });

    }
}