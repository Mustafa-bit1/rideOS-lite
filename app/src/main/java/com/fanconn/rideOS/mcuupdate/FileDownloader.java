package com.fanconn.rideOS.mcuupdate;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader {

    public static int fetchVersion(String jsonUrl) throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            URL url = new URL(jsonUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.connect();

            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                StringBuilder sb = new StringBuilder();
                int data = reader.read();
                while (data != -1) {
                    sb.append((char) data);
                    data = reader.read();
                }

                JSONObject jsonObject = new JSONObject(sb.toString());
                String versionString = jsonObject.getString("version");
                return Integer.parseInt(versionString);
            } else {
                throw new IOException("Failed to fetch JSON file: " + connection.getResponseCode());
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                is.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static String fetchDownloadUrl(String jsonUrl) throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;
        try {
            URL url = new URL(jsonUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.connect();

            if (connection.getResponseCode() == 200) {
                is = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(is, "UTF-8");
                StringBuilder sb = new StringBuilder();
                int data = reader.read();
                while (data != -1) {
                    sb.append((char) data);
                    data = reader.read();
                }

                JSONObject jsonObject = new JSONObject(sb.toString());
                return jsonObject.getString("url");
            } else {
                throw new IOException("Failed to fetch JSON file: " + connection.getResponseCode());
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null) {
                is.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }


    public static void downloadFile(String fileUrl, String saveDir) throws IOException {
        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.connect();

            if (connection.getResponseCode() == 200) {
                is = new BufferedInputStream(connection.getInputStream());
                String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
                os = new FileOutputStream(saveDir + "/" + fileName);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            } else {
                throw new IOException("Failed to download file: " + connection.getResponseCode());
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

    }


}
