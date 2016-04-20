package com.aceft.data;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by marcneumann on 20.04.16.
 */
public class Storage {

    @Nullable
    public static String saveMappingOnDisk(Context context, HashMap<String, String> data, String filename) {
        if (context == null) return null;

        Gson gson = new Gson();
        String s = gson.toJson(data);
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(s.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filename;
    }

    @NonNull
    public static HashMap<String, String> getMappingFromFile(Context context, String filename) {
        if (context == null) return new HashMap<>();

        try {
            FileInputStream fis = context.openFileInput(filename);

            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            String json = sb.toString();
            Gson gson = new Gson();
            return gson.fromJson(json, HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }
}
