package com.aadim.trafficdigitalvehicle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanBarCodeBluebookActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    ZXingScannerView scannerView;
    TextView vehicleNo, vehicleTypes, ownerText, vehicleText;
    ImageView Image1, Image2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerView = new ZXingScannerView(this);
        setContentView(scannerView);

        Dexter.withContext(getApplicationContext())
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        scannerView.startCamera();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    public void handleResult(Result rawResult) {
        String result = rawResult.getText();
        String strSplit[] = result.split("by");
        String mobileNumber = strSplit[0];
        String bluebookNumber = strSplit[1];
        //BluebookPageActivity.scantext.setText(rawResult.getText());

        FirebaseDatabase rootNode = FirebaseDatabase.getInstance("https://digitalvehicle-5fc1b-default-rtdb.firebaseio.com");
        DatabaseReference reference = rootNode.getReference("bluebooks");
        //Query
        Query checkUser = reference.orderByChild("vehicleNo").equalTo(bluebookNumber);
        checkUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String mobileNoFromDB = snapshot.child(mobileNumber).child("mobileNumber").getValue(String.class);
                    String vehicleTypeFromDB = snapshot.child(mobileNumber).child("vehicleType").getValue(String.class);
                    String vehicleNoFromDB = snapshot.child(mobileNumber).child("vehicleNo").getValue(String.class);
                    String ownerImageFromDB = snapshot.child(mobileNumber).child("imageURL").getValue(String.class);
                    String vehicleImageFromDB = snapshot.child(mobileNumber).child("imageURL1").getValue(String.class);
                    String lendStatusFromDB = snapshot.child(mobileNumber).child("lendStatus").getValue(String.class);
                    String lendToFromDB = snapshot.child(mobileNumber).child("lendTo").getValue(String.class);
                    String lendToLicenseNoFromDB = snapshot.child(mobileNumber).child("lendToLicenseNumber").getValue(String.class);
                    String lendToMobileNoFromDB = snapshot.child(mobileNumber).child("lendToMobileNo").getValue(String.class);
                    String taxExpireDateFromDB = snapshot.child(mobileNumber).child("taxExpireDate").getValue(String.class);
                    String taxRenewedDateFromDB = snapshot.child(mobileNumber).child("taxRenewedDate").getValue(String.class);
                    String taxStatusFromDB = snapshot.child(mobileNumber).child("taxStatus").getValue(String.class);
                    String theftStatusFromDB = snapshot.child(mobileNumber).child("theftStatus").getValue(String.class);

                    Intent intent = new Intent(getApplicationContext(), BluebookPageActivity.class);

                    intent.putExtra(BluebookPageActivity.mobileNoParameter, mobileNoFromDB);
                    intent.putExtra(BluebookPageActivity.vehicleNoParameter, vehicleNoFromDB);
                    intent.putExtra(BluebookPageActivity.vehicleTypeParameter, vehicleTypeFromDB);
                    intent.putExtra(BluebookPageActivity.ownerImageParameter, ownerImageFromDB);
                    intent.putExtra(BluebookPageActivity.vehicleImageParameter, vehicleImageFromDB);
                    intent.putExtra(BluebookPageActivity.lendStatusParameter, lendStatusFromDB);
                    intent.putExtra(BluebookPageActivity.lendToParameter, lendToFromDB);
                    intent.putExtra(BluebookPageActivity.lendToLicenseNoParameter, lendToLicenseNoFromDB);
                    intent.putExtra(BluebookPageActivity.lendToMobileNoParameter, lendToMobileNoFromDB);
                    intent.putExtra(BluebookPageActivity.taxExpireDateParameter, taxExpireDateFromDB);
                    intent.putExtra(BluebookPageActivity.taxRenewedDateParameter, taxRenewedDateFromDB);
                    intent.putExtra(BluebookPageActivity.taxStatusParameter, taxStatusFromDB);
                    intent.putExtra(BluebookPageActivity.theftStatusParameter, theftStatusFromDB);
                    startActivity(intent);
                    onBackPressed();
                } else {
                    Toast.makeText(ScanBarCodeBluebookActivity.this, "Please scan valid QR Code!", Toast.LENGTH_SHORT).show();
                    Intent in = new Intent(getApplicationContext(), ScanBarCodeBluebookActivity.class);
                    startActivity(in);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Bluebook not found!", Toast.LENGTH_SHORT).show();
                Intent in = new Intent(getApplicationContext(), ScanBarCodeBluebookActivity.class);
                startActivity(in);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        scannerView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }
}