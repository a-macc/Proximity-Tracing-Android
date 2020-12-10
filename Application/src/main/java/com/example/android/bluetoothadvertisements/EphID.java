package com.example.android.bluetoothadvertisements;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class EphID extends AppCompatActivity {

    public static HashSet<String> getEphIDs() {
        return ephIDs;
    }

    public static HashSet<String> ephIDs = new HashSet<>();

    private static final EphID instance = new EphID();

    public static EphID getInstance() {
        return instance;
    }

    public static String getSecretKey() {
        return secretKey;
    }

    public static String secretKey = generateFirstSKt();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eph_i_d);
    }

    public static String toHexString(byte[] hash) {
        BigInteger number = new BigInteger(1, hash);

        StringBuilder hexString = new StringBuilder(number.toString(16));

        while (hexString.length() < 32) {
            hexString.insert(0, '0');
        }

        return hexString.toString();
    }

    public static byte[] getSHA(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateFirstSKt() {
        String SKt = "";
        int length = 64;

        String AlphaNumericString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz";

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());

            sb.append(AlphaNumericString.charAt(index));
        }

        SKt = sb.toString();

        return SKt;
    }

    public static String generateNextSKt(String curr_SKt) {
        String new_SKt = "";
        try {
            new_SKt = toHexString(getSHA(curr_SKt));
        } catch (NoSuchAlgorithmException e) {
            ;
        }

        return new_SKt;
    }


    public static String encrypt(String strToEncrypt, String secret) {
        try {
            SecretKeySpec secretKey;
            byte[] key;
            MessageDigest sha = null;
            String myKey = secret;
            key = myKey.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static ArrayList<String> generateEphID(String curSKt) {
        String broadcast = "Y8IXyQ3Ui6Tg/XSjaKgv5MeBMQWDa5MrQUFJwJRyyjnQkfP1ajrlOEFR8m6D+zZkNXJqa94lonyz3yddDOUqHKMUg6A945OOGk7fKVpIeLn7bwbIpFd5q5oWGvdR+yxIBxk/NSA/Jv9ikPw5HUs6/P4eCUOB7IaAGs6VoXkb/oei9A0YxfZz6VkJypeD1ULwVMpFg0DtvTkfaH4CAu+7sUzcyvXq9SGhfKJpCcx9OIi0zjCWDqBpnDJF9uZP2YNdMOjFl28VNRnF5E7JVq2VVKCahgf+nQdDH4/7B02JzM8=";

        String prString = encrypt(broadcast, curSKt);
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < prString.length(); i++) {
            indexes.add(i);
        }
        Set<Integer> st = new HashSet<>();

        for (int i = 0; i < prString.length() - 384; i++) {
//            int idx = (int) (indexes.size() * Math.random());
            int idx = i;
            st.add(indexes.get(idx));
            indexes.remove(idx);
        }
        String str = "";
        for (int i = 0; i < prString.length(); i++) {
            if (!st.contains(i)) {
                str += prString.charAt(i);
            }
        }

        prString = str;


        ArrayList<String> ephID = new ArrayList<>();

        for (int i = 0; i < 384; i += 4) {
            String cur = "";
            for (int j = i; j < i + 4; j++) {
                cur += prString.charAt(j);
            }
            ephID.add(cur);
        }

        indexes.clear();
        for (int i = 0; i < 96; i++) {
            indexes.add(i);
        }

        ArrayList<String> finalEPHID = new ArrayList<>();

        for (int i = 0; i < 96; i++) {
            int idx = (int) (indexes.size() * Math.random());
            finalEPHID.add(ephID.get(indexes.get(idx)));
            indexes.remove(idx);
        }

        return finalEPHID;

    }
}