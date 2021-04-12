 package com.dlogic.ufr_mobile_unique_id_via_nfc_examples_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;



import org.w3c.dom.Text;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.dlogic.ufr_mobile_unique_id_via_nfc_examples_android.HostCardEmulatorService.calculateUID;
import static com.dlogic.ufr_mobile_unique_id_via_nfc_examples_android.HostCardEmulatorService.toHex;
import static com.dlogic.ufr_mobile_unique_id_via_nfc_examples_android.HostCardEmulatorService.toSHA256;

 public class MainActivity extends AppCompatActivity {

    GoogleSignInClient mGoogleSignInClient;

    private static int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInAccount signed_in_user;

    String web_client_id = "";

    private static NfcAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            web_client_id = getResources().getString(R.string.firebase_api_key);
        }
        catch (Exception ex)
        {
            Log.d("API KEY ERROR", "Check your 'firebase_key.xml' file for a valid API key, if you plan on utilizing Firebase API");
        }
        ////////////////////////////////////////////////////////////////
        mAdapter = NfcAdapter.getDefaultAdapter(this);

        String deviceIDStr = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID).toUpperCase();

        TextView txtDeviceID = findViewById(R.id.txtUID);
        txtDeviceID.setText(deviceIDStr);

        ////////////////////////////////////////////////////////////////
        GoogleSignInOptions gso;
        mAuth = FirebaseAuth.getInstance();
        if (web_client_id.contains("undefined") == false) {
            gso = new GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(web_client_id)
                    .requestEmail()
                    .build();
        } else
        {
            gso = new GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
        }

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        Api.ApiOptions opt = mGoogleSignInClient.getApiOptions();
        //
        FirebaseUser user = mAuth.getCurrentUser();
        GoogleSignInAccount google_account = GoogleSignIn.getLastSignedInAccount(this);
        updateUI(google_account);

        //onClick functions

        SignInButton btnSignIn = findViewById(R.id.sign_in_button);
        btnSignIn.setOnClickListener(v -> signIn());

        Button btnSignOut = findViewById(R.id.sign_out_button);
        btnSignOut.setOnClickListener(v -> SignOut());

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter  != null) {
            mAdapter.enableReaderMode(this, new NfcAdapter.ReaderCallback() {
                        @Override
                        public void onTagDiscovered(Tag tag) {
                            Intent i = new Intent().putExtra(NfcAdapter.EXTRA_TAG, tag);
                            resolveIntent(i);
                        }
                    }, NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
                            | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    null);
        }
    }

    public static void resolveIntent(Intent intent) {

        if (null != mAdapter) {
            if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
                Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (tagFromIntent != null) {
                    Log.d("TAG_FOUND", "resolveIntent: " + tagFromIntent.toString());
                }
                }
            }
        }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        //updateUI(currentUser);
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void SignOut() {
        mAuth.signOut();

        mGoogleSignInClient.signOut();

        updateUI(null);
    }

    private void updateUI(GoogleSignInAccount user) {
        if (user != null) // user is signed in
        {
            LinearLayout layoutUserData = findViewById(R.id.layoutUserData);
            layoutUserData.setVisibility(LinearLayout.VISIBLE);

            TextView txtEmail = findViewById(R.id.txtEmail);
            txtEmail.setText("E-mail: " + user.getEmail());

            TextView txtDisplayName = findViewById(R.id.txtDisplayName);
            txtDisplayName.setText("Display Name: " + user.getDisplayName());

            TextView txtDeviceID = findViewById(R.id.txtUID);
            String user_id = user.getId();
            byte[] hashed_id = toSHA256(user_id.getBytes(StandardCharsets.UTF_8));
            byte[] short_uid = calculateUID(hashed_id);
            txtDeviceID.setText(toHex(short_uid));

            Button btnSignOut = findViewById(R.id.sign_out_button);
            SignInButton btnSignIn = findViewById(R.id.sign_in_button);

            btnSignIn.setVisibility(Button.INVISIBLE);
            btnSignOut.setVisibility(Button.VISIBLE);
        } else //no signed in user
        {
            LinearLayout layoutUserData = findViewById(R.id.layoutUserData);
            layoutUserData.setVisibility(LinearLayout.INVISIBLE);

            Button btnSignOut = findViewById(R.id.sign_out_button);
            SignInButton btnSignIn = findViewById(R.id.sign_in_button);

            TextView txtDeviceID = findViewById(R.id.txtUID);
            String UserUID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            byte[] hashed_id = toSHA256(UserUID.getBytes(StandardCharsets.UTF_8));
            byte[] short_uid = calculateUID(hashed_id);
            txtDeviceID.setText(toHex(short_uid));

            btnSignIn.setVisibility(Button.VISIBLE);
            btnSignOut.setVisibility(Button.INVISIBLE);
        }
    }

    //SignIn Callbacks(GoogleSignIn, Firebase is optional)

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                signed_in_user = task.getResult(ApiException.class);
                Log.d("TAG", "firebaseAuthWithGoogle:" + signed_in_user.getId());
                String token = signed_in_user.getIdToken();
                String userID = signed_in_user.getId();
                updateUI(signed_in_user);
                if (web_client_id.contains("undefined") == false)
                {
                    try {
                        firebaseAuthWithGoogle(token);
                    } catch (Exception e) {
                        Log.d("Firebase Auth error: ", e.getLocalizedMessage());
                    }

                }
            } catch (ApiException e) {
                // Google Sign In failed
                Log.d("GoogleSignIn error: ", e.getLocalizedMessage());
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("firebaseAuth", "signInWithCredential:success");
                            //FirebaseUser user = mAuth.getCurrentUser();
                            //String test_uid = user.getUid();
                            //updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("firebaseAuth", "signInWithCredential:failure", task.getException());
                            //Snackbar.make(this.mainLayout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            //updateUI(null);
                        }
                        // ...
                    }
                });
    }
}