package com.dlogic.ufr_mobile_unique_id_via_nfc_examples_android;

import android.annotation.SuppressLint;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class HostCardEmulatorService extends HostApduService {

    private FirebaseAuth mAuth;


    class Const {
        static final String TAG = "Unique device ID example: ";
        static final String STATUS_SUCCESS = "9000";
        static final String STATUS_FAILED = "6F00";
        static final String FUNCTION_NOT_SUPPORTED = "6A81";
        static final String CLA_NOT_SUPPORTED = "6E00";
        static final String INS_NOT_SUPPORTED = "6D00";
        static final String AID = "F00102030405"; // Chose your own AID (RID + PIX). AID also needs to be registered with the service in res/xml/apduservice.xml file.
        static final String SELECT_INS = "A4";
        static final String DEFAULT_CLA = "00";
    }

    // process AID received and return ID


    @Override
    public void onCreate() {
        super.onCreate();
        mAuth = FirebaseAuth.getInstance();
    }

    @SuppressLint("MissingPermission")
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle bundle) {

        if (commandApdu == null) {
            return hexStringToByteArray(Const.STATUS_FAILED);
        }

        String hexCommandApdu = toHex(commandApdu);

        if (hexCommandApdu.substring(10, 22).equals(Const.AID)) {
            GoogleSignInAccount google_account = GoogleSignIn.getLastSignedInAccount(this);
            String UID_str = "";
            if (google_account != null) {
                UID_str = google_account.getId();
            } else {
                UID_str = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            }

            byte[] hashed_id = toSHA256(UID_str.getBytes(StandardCharsets.UTF_8));
            byte[] short_uid = calculateUID(hashed_id);
            short_uid[0] &= ~(1 << 7);
            byte[] status = hexStringToByteArray(Const.STATUS_SUCCESS);
            byte[] response = new byte[short_uid.length + status.length];
            System.arraycopy(short_uid, 0, response, 0, short_uid.length);
            System.arraycopy(status, 0, response, short_uid.length, status.length);
            return response;
        } else {
            return hexStringToByteArray(Const.FUNCTION_NOT_SUPPORTED);
        }
    }

    // helper functions

    public static byte[] calculateUID(byte[] data) {
        byte[] new_uid = new byte[8];
        for (int i = 0; i < 8; i++) {
            new_uid[i] = (byte) (data[i] ^ data[i + 8] ^ data[i + 16] ^ data[i + 24]);
        }

        return new_uid;

    }

    //////////////////////////////////////////////

    public static byte[] toSHA256(byte[] input) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hashed_id = digest.digest(input);

        return hashed_id;
    }

    //////////////////////////////////////////////
    public static String bin2hex(byte[] data) {
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1, data));
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String toHex(byte[] bytes) {

        char[] hexChars = new char[bytes.length * 2];
        int j = 0;
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[j++] = hexArray[v >>> 4];
            hexChars[j++] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String paramString) throws IllegalArgumentException {
        int j = paramString.length();

        if (j % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }

        byte[] arrayOfByte = new byte[j / 2];
        int hiNibble, loNibble;

        for (int i = 0; i < j; i += 2) {
            hiNibble = Character.digit(paramString.charAt(i), 16);
            loNibble = Character.digit(paramString.charAt(i + 1), 16);
            if (hiNibble < 0) {
                throw new IllegalArgumentException("Illegal hex digit at position " + i);
            }
            if (loNibble < 0) {
                throw new IllegalArgumentException("Illegal hex digit at position " + (i + 1));
            }
            arrayOfByte[(i / 2)] = ((byte) ((hiNibble << 4) + loNibble));
        }
        return arrayOfByte;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d("HCE", "Deactivated: " + reason);
    }
}
