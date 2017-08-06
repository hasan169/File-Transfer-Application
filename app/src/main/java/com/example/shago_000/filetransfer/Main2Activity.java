package com.example.shago_000.filetransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;

import static android.R.attr.name;

public class Main2Activity extends Activity {

    Activity activity;
    ProgressDialog progressDialog;
    int progressStatus;
    byte[] data = new byte[10000];
    BufferedInputStream inFromClient = null;
    BufferedOutputStream outToClient = null;
    boolean fileNameSend = false;
    boolean waitForConfirmation = false;
    boolean finalConfirmation = false;
    File fileTosend;
    String fileName;
    Bitmap bitmap;
    boolean flag = true;
    long fileSize;
    boolean isFile;
    boolean isAudio;
    String Type;
    byte[] byteArray;
    InputStream iStream;
    ByteArrayOutputStream stream;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toast.makeText(Main2Activity.this,"Connected", Toast.LENGTH_LONG).show();
        try {
            stream = new ByteArrayOutputStream();
            outToClient = new BufferedOutputStream(MainActivity.connectionSocket.getOutputStream());
            inFromClient = new BufferedInputStream(MainActivity.connectionSocket.getInputStream());
        } catch (Exception e) {

        }
        activity = this;
        final Button fileChoose = (Button) findViewById(R.id.choose);
        fileChoose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent,42);
            }

        });
        final Button conn = (Button) findViewById(R.id.connection);
        conn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(Main2Activity.this);
                alert.setCancelable(false);
                alert.setMessage("Do you want to disconnect the connection?");
                alert.setTitle("Disconnect");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {flag = false;}
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {}
                });
                alert.show();
            }
        });
        new TCPServer().start();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == 42 && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                fileName = uri.getPath();
                int pos = fileName.lastIndexOf('/');
                if (pos >= 0) {
                    fileName = fileName.substring(pos + 1, fileName.length());
                }
                Type = getContentResolver().getType(uri);
                if( Type == null){
                    isFile = true;
                }
                else{
                    if(Type.length() >= 5 ){
                        if(Type.substring(0,5).equals("image")){
                            isFile = false;
                        }
                        else{
                            isFile = true;
                        }
                    }
                    else{
                        isFile = true;
                    }
                }
                if (!isFile) {
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fileName = fileName + ".png";
                    updateUI();
                }
                else {
                    isAudio = false;
                    if( Type != null ){
                        if(Type.substring(0,5).equals("audio")){
                            isAudio = true;
                        }
                    }
                    if(isAudio) {
                        try {
                            fileName = fileName +".mp3";
                            iStream = getContentResolver().openInputStream(uri);
                            Cursor cursor = this.getContentResolver().query(uri,
                                    null, null, null, null);
                            cursor.moveToFirst();
                            fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                            cursor.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        fileTosend = new File(uri.getPath());
                        fileSize = fileTosend.length();
                    }
                    updateUI();
                }
            }

        }
    }
    Handler handle = new Handler();
    public void updateUI() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                final AlertDialog.Builder alert = new AlertDialog.Builder(Main2Activity.this);
                alert.setCancelable(false);
                alert.setMessage("Do you want to send the file "+fileName+ "?");
                alert.setTitle("Send FIle");
                alert.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {fileNameSend = true;}
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {}
                });
                alert.show();
            }
        };
        handle.post(r);
    }
    public void showMessage(final String txt){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Main2Activity.this,txt, Toast.LENGTH_LONG).show();
            }
        };
        handle.post(r);
    }
    void makeProgresbar(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(Main2Activity.this);
                progressDialog.setTitle("Please Wait..");
                progressDialog.setMessage("Transfering File ...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
                progressDialog.setProgress(0);
                progressDialog.show();
            }
        };
        handle.post(r);
    }
    public void showProgressBar(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                progressDialog.setProgress(progressStatus);
            }
        };
        handle.post(r);
    }
    public void confirmation(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(Main2Activity.this);
                progressDialog.setMessage("Waiting for client's confirmation ...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        };
        handle.post(r);
    }
    public class TCPServer extends Thread {
        @Override
        public void run() {
            while (flag) {
                inFromClient.mark(5);
                try {
                    if(inFromClient.read() == -1 )break;
                    inFromClient.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep(500);
                } catch (Exception e) {
                }
                if (fileNameSend) {
                    fileNameSend = false;
                    try {
                        outToClient.write(fileName.getBytes());
                        outToClient.flush();
                        waitForConfirmation = true;
                        confirmation();
                    } catch (Exception e) {

                    }
                } else if (waitForConfirmation) {
                    try {
                        if (inFromClient.available() > 0) {
                            int bytesRead = inFromClient.read(data, 0, data.length);
                            String confirmation = new String(data, 0, bytesRead);
                            progressDialog.dismiss();
                            if (confirmation.equals("1")) {
                                if(isFile) {
                                    String len = String.valueOf(fileSize);
                                    outToClient.write(len.getBytes());
                                    outToClient.flush();
                                }
                                else{
                                    makeProgresbar();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                                    byteArray = stream.toByteArray();
                                    String len = String.valueOf(byteArray.length);
                                    outToClient.write(len.getBytes());
                                    outToClient.flush();
                                    stream.flush();
                                }
                                finalConfirmation = true;
                            }
                            waitForConfirmation = false;
                        }
                    } catch (Exception e) {

                    }
                }
                else if(finalConfirmation){
                    try {
                        if(inFromClient.available() > 0 ){
                            if(isFile) {
                                int bytesRead;
                                if(isAudio){
                                    inFromClient.read(data, 0, data.length);
                                    finalConfirmation = false;
                                    makeProgresbar();
                                    long counter = 0;
                                    try {
                                        while ((bytesRead = iStream.read(data, 0, data.length)) != -1) {
                                            outToClient.write(data,0,bytesRead);
                                            counter = counter + bytesRead;
                                            progressStatus = (int)(counter*100/fileSize);
                                            showProgressBar();
                                        }

                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    iStream.close();
                                    progressDialog.dismiss();
                                    outToClient.flush();
                                }
                                else {
                                    finalConfirmation = false;
                                    inFromClient.read(data, 0, data.length);
                                    RandomAccessFile raf = new RandomAccessFile(fileTosend, "r");
                                    long counter = 0;
                                    progressStatus = 0;
                                    makeProgresbar();
                                    while ((bytesRead = raf.read(data, 0, data.length)) != -1) {
                                        outToClient.write(data, 0, bytesRead);
                                        showProgressBar();
                                        counter += bytesRead;
                                        progressStatus = (int) (counter * 100 / fileSize);
                                    }
                                    progressDialog.dismiss();
                                    outToClient.flush();
                                    raf.close();
                                }
                            }
                            else{
                                inFromClient.read(data, 0, data.length);
                                finalConfirmation = false;
                                for(int i = 0; i < byteArray.length; i++){
                                    outToClient.write(byteArray[i]);
                                }
                                progressStatus = 100;
                                showProgressBar();
                                progressDialog.dismiss();
                                byteArray = null;
                                bitmap = null;
                                outToClient.flush();
                            }
                            showMessage("File Transfer Complete");
                        }
                    } catch (Exception e) {
                    }
                }
            }
            showMessage("Connection lost");
            finish();
        }
    }
    @Override
    public void onBackPressed() {
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            stream.close();
            inFromClient.close();
            MainActivity.connectionSocket.close();
            outToClient.close();
            MainActivity.welcomeSocket.close();
            MainActivity.welcomeSocket = null;
            MainActivity.connectionSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}