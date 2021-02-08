package com.dlogic.ufr_mobile_unique_id_via_nfc_examples_android;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String deviceIDStr = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID).toUpperCase();

        TextView txtDeviceID = findViewById(R.id.txtUID);
        txtDeviceID.setText(deviceIDStr);
    }
}