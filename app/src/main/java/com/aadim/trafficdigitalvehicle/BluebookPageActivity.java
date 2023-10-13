package com.aadim.trafficdigitalvehicle;


import android.content.DialogInterface;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Calendar;


public class BluebookPageActivity extends AppCompatActivity {

    public static final String mobileNoParameter = "mobileNumber";
    public static final String vehicleNoParameter = "vehicleNo";
    public static final String vehicleTypeParameter = "vehicleType";
    public static final String ownerImageParameter = "ownerImage";
    public static final String vehicleImageParameter = "vehicleImage";
    public static final String lendStatusParameter = "lendStatus";
    public static final String lendToParameter = "lendTo";
    public static final String lendToLicenseNoParameter = "lendToLicenseNo";
    public static final String lendToMobileNoParameter = "lendToMobileNo";
    public static final String taxExpireDateParameter = "taxExpireDate";
    public static final String taxRenewedDateParameter = "taxRenewedDate";
    public static final String taxStatusParameter = "taxStatus";
    public static final String theftStatusParameter = "theftStatus";
    //TextView vehicleNo, vehicleTypes;
    //ImageView Image1, Image2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluebook_page);



        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String Todaydate = sdf.format(c.getTime());

        Intent i = getIntent();
        String vehicleNumber = i.getStringExtra(vehicleNoParameter);
        String vehicleType = i.getStringExtra(vehicleTypeParameter);
        String ownerImage = i.getStringExtra(ownerImageParameter);
        String vehicleImage = i.getStringExtra(vehicleImageParameter);
        String lendStatus = i.getStringExtra(lendStatusParameter);
        String lendTo = i.getStringExtra(lendToParameter);
        String lendToLicenseNo = i.getStringExtra(lendToLicenseNoParameter);
        String lendToMobileNo = i.getStringExtra(lendToMobileNoParameter);
        String taxExpiredDate = i.getStringExtra(taxExpireDateParameter);
        String taxRenewedDate = i.getStringExtra(taxRenewedDateParameter);
        String taxStatus = i.getStringExtra(taxStatusParameter);
        String theftStatus = i.getStringExtra(theftStatusParameter);

        ImageButton ownerImage1 = findViewById(R.id.ownerImage);
        ownerImage1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), imageOneActivity.class);
                i.putExtra(imageOneActivity.ownerImageParameter, ownerImage);
                startActivity(i);
            }
        });

        ImageButton detailImage = findViewById(R.id.vehicleImage);
        detailImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), imageTwoActivity.class);
                i.putExtra(imageTwoActivity.detailImageParameter, vehicleImage);
                startActivity(i);
            }
        });

        ImageView Image1 = findViewById(R.id.ownerImage);
        ImageView Image2 = findViewById(R.id.vehicleImage);

        TextView vehicleNo = findViewById(R.id.vehicleNo);
        TextView lendStat = findViewById(R.id.lendsta);
        TextView lendTO = findViewById(R.id.lendto);
        TextView taxStat = findViewById(R.id.taxsta);
        TextView theftStat = findViewById(R.id.theftsta);
        TextView lendToMobileNumber = findViewById(R.id.lendToMobileNumber);
        TextView lendToLicenseNumber = findViewById(R.id.lendToLicenseNumber);
        TextView taxExpireDateText = findViewById(R.id.taxExpireDateText);
        TextView taxRenewedDateText = findViewById(R.id.taxRenewedDateText);
        TextView vehicleTypes = findViewById(R.id.vehicleType);
        String lend = "Lended";
        String tax = "Clear";
        String theft = "Reported Theft/Lost";

        if (lendStatus.isEmpty() && Todaydate.compareTo(taxExpiredDate) < 0 && theftStatus.isEmpty()) {
            lendStat.setText(lendStatus);
            lendTO.setText(lendTo);
            taxStat.setText("Clear");
            theftStat.setText(theftStatus);
            lendToMobileNumber.setText(lendToMobileNo);
            lendToLicenseNumber.setText(lendToLicenseNo);
            taxExpireDateText.setText(taxExpiredDate);
            taxRenewedDateText.setText(taxRenewedDate);
            theftStat.setText(theftStatus);
            vehicleNo.setText(vehicleNumber);
            vehicleTypes.setText(vehicleType);

            Glide.with(BluebookPageActivity.this)
                    .load(ownerImage)
                    .apply(new RequestOptions().override(1500, 1500))
                    .centerCrop()
                    .into(Image1);

            Glide.with(BluebookPageActivity.this)
                    .load(vehicleImage)
                    .apply(new RequestOptions().override(1500, 1500))
                    .centerCrop()
                    .into(Image2);
        } else {

            if (Todaydate.compareTo(taxExpiredDate) > 0) {
                AlertDialog.Builder alert = new AlertDialog.Builder(BluebookPageActivity.this);
                alert.setTitle("!Tax Alert!");
                alert.setMessage("Vehicle Tax date has been expired..");
                alert.setMessage("Expired Date : " + taxExpiredDate);
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        lendStat.setText(lendStatus);
                        lendTO.setText(lendTo);
                        taxStat.setText("Tax not clear");
                        theftStat.setText(theftStatus);
                        lendToMobileNumber.setText(lendToMobileNo);
                        lendToLicenseNumber.setText(lendToLicenseNo);
                        taxExpireDateText.setText(taxExpiredDate);
                        taxRenewedDateText.setText(taxRenewedDate);
                        theftStat.setText(theftStatus);
                        vehicleNo.setText(vehicleNumber);
                        vehicleTypes.setText(vehicleType);

                        Glide.with(BluebookPageActivity.this)
                                .load(ownerImage)
                                .apply(new RequestOptions().override(1500, 1500))
                                .centerCrop()
                                .into(Image1);

                        Glide.with(BluebookPageActivity.this)
                                .load(vehicleImage)
                                .apply(new RequestOptions().override(1500, 1500))
                                .centerCrop()
                                .into(Image2);

                    }
                });
                alert.show();
            }

            if (lendStatus.equals(lend)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(BluebookPageActivity.this);
                alert.setTitle("!Lend Alert!");
                alert.setMessage("Lend To : " + lendTo + "\nLend To Mobile Number: " + lendToMobileNo + "\nLend To License Number: " + lendToLicenseNo);
                alert.setPositiveButton("Done", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        lendStat.setText(lendStatus);
                        lendTO.setText(lendTo);
                        taxStat.setText("Clear");
                        theftStat.setText(theftStatus);
                        lendToMobileNumber.setText(lendToMobileNo);
                        lendToLicenseNumber.setText(lendToLicenseNo);
                        taxExpireDateText.setText(taxExpiredDate);
                        taxRenewedDateText.setText(taxRenewedDate);
                        theftStat.setText(theftStatus);
                        vehicleNo.setText(vehicleNumber);
                        vehicleTypes.setText(vehicleType);

                        Glide.with(BluebookPageActivity.this)
                                .load(ownerImage)
                                .apply(new RequestOptions().override(1500, 1500))
                                .centerCrop()
                                .into(Image1);

                        Glide.with(BluebookPageActivity.this)
                                .load(vehicleImage)
                                .apply(new RequestOptions().override(1500, 1500))
                                .centerCrop()
                                .into(Image2);
                    }
                });
                alert.show();
            }
            if (theftStatus.equals(theft)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(BluebookPageActivity.this);
                alert.setTitle("!Theft Alert!");
                alert.setMessage("This vehicle has been reported theft or lost by owner!!");
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        lendStat.setText(lendStatus);
                        lendTO.setText(lendTo);
                        taxStat.setText("Clear");
                        theftStat.setText(theftStatus);
                        lendToMobileNumber.setText(lendToMobileNo);
                        lendToLicenseNumber.setText(lendToLicenseNo);
                        taxExpireDateText.setText(taxExpiredDate);
                        taxRenewedDateText.setText(taxRenewedDate);
                        theftStat.setText(theftStatus);
                        vehicleNo.setText(vehicleNumber);
                        vehicleTypes.setText(vehicleType);

                        Glide.with(BluebookPageActivity.this)
                                .load(ownerImage)
                                .apply(new RequestOptions().override(1500, 1500))
                                .centerCrop()
                                .into(Image1);

                        Glide.with(BluebookPageActivity.this)
                                .load(vehicleImage)
                                .apply(new RequestOptions().override(1500, 1500))
                                .centerCrop()
                                .into(Image2);
                    }
                });
                alert.show();
            }
        }
    }
}