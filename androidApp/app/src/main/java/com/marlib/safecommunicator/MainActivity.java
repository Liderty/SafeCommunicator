package com.marlib.safecommunicator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private String DEFAULT_IP_ADRESS = "192.168.1.23:3000";
    private int encryptingId = 0;

    EditText serverAdressEditText;
    EditText enterNameEditText;
    Button enterChatButton;
    RadioButton rsaRadioButton;
    RadioButton elgamalRadioButton;

    LinearLayout rsaSectionLayout;
    EditText firstPrimeEditText;
    EditText secondPrimeEditText;
    EditText publicExponentEditText;

    LinearLayout elgamalSectionLayout;
    EditText primePEditText;
    EditText alphaEditText;
    EditText factorGEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 10);
        }

        serverAdressEditText = findViewById(R.id.etvIPadress);
        enterNameEditText = findViewById(R.id.etvName);
        enterChatButton = findViewById(R.id.btnEnterChat);
        rsaRadioButton = findViewById(R.id.radRSA);
        elgamalRadioButton = findViewById(R.id.radElgamal);

        rsaSectionLayout = findViewById(R.id.sectionRSA);
        firstPrimeEditText = findViewById(R.id.etvFirstPrime);
        secondPrimeEditText = findViewById(R.id.etvSecondPrime);
        publicExponentEditText = findViewById(R.id.etvPublicExponent);

        elgamalSectionLayout = findViewById(R.id.sectionElGamal);
        primePEditText = findViewById(R.id.etvPrimeP);
        alphaEditText = findViewById(R.id.etvAlpha);
        factorGEditText = findViewById(R.id.etvFactorG);

        serverAdressEditText.setText(DEFAULT_IP_ADRESS);



        enterChatButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);

            switch (encryptingId) {
                case 0:
                    if(isRsaFieldsNotEmpty()) {
                        intent.putExtra("name", enterNameEditText.getText().toString());
                        intent.putExtra("adress", serverAdressEditText.getText().toString());
                        intent.putExtra("encrypting", encryptingId);
                        intent.putExtra("first_prime", Long.parseLong(firstPrimeEditText.getText().toString()));
                        intent.putExtra("second_prime", Long.parseLong(secondPrimeEditText.getText().toString()));
                        intent.putExtra("exponent", Long.parseLong(publicExponentEditText.getText().toString()));
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Empty fields!", Toast.LENGTH_SHORT).show();
                    }

                case 1:
                    if(isElgamalFieldsNotEmpty()) {
                        intent.putExtra("name", enterNameEditText.getText().toString());
                        intent.putExtra("adress", serverAdressEditText.getText().toString());
                        intent.putExtra("encrypting", encryptingId);
                        intent.putExtra("prime_p", Long.parseLong(primePEditText.getText().toString()));
                        intent.putExtra("alpha", Long.parseLong(alphaEditText.getText().toString()));
                        intent.putExtra("factor_g", Long.parseLong(factorGEditText.getText().toString()));
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Empty fields!", Toast.LENGTH_SHORT).show();
                    }
            }
        });

        rsaRadioButton.setOnClickListener(v -> {
            elgamalSectionLayout.setVisibility(View.GONE);
            rsaSectionLayout.setVisibility(View.VISIBLE);
            encryptingId = 0;
        });

        elgamalRadioButton.setOnClickListener(v -> {
            elgamalSectionLayout.setVisibility(View.VISIBLE);
            rsaSectionLayout.setVisibility(View.GONE);
            encryptingId = 1;
        });
    }

    private boolean isRsaFieldsNotEmpty() {
        return !(enterNameEditText.getText().toString().isEmpty()
                || serverAdressEditText.getText().toString().isEmpty()
                || firstPrimeEditText.getText().toString().isEmpty()
                || secondPrimeEditText.getText().toString().isEmpty()
                || publicExponentEditText.getText().toString().isEmpty());
    }

    private boolean isElgamalFieldsNotEmpty() {
        return !(enterNameEditText.getText().toString().isEmpty()
                || serverAdressEditText.getText().toString().isEmpty()
                || primePEditText.getText().toString().isEmpty()
                || alphaEditText.getText().toString().isEmpty()
                || factorGEditText.getText().toString().isEmpty());
    }
}
