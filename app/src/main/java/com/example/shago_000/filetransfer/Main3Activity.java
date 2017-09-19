package com.example.shago_000.filetransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.spec.ECField;

public class Main3Activity extends Activity{

    BufferedInputStream inFromServer;
    BufferedOutputStream outToServer;
    byte data[] = new byte[10000];
    int bytesRead;
    String fileName;
    long fileSize;
    boolean nameReceived = true;
    boolean fileReceived = false;
    boolean fileSizeReceived = false;
    FileOutputStream fos;
    boolean flag = true;
    TCPClient client;
    ProgressDialog progressDialog;
    int progressStatus;
    boolean imageReceive;
    boolean isFile;
    byte[] byteArray;
    Bitmap bitmap;
    File dir;
    File receivedFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);
        Toast.makeText(Main3Activity.this,"Connected", Toast.LENGTH_LONG).show();
        try {
            inFromServer = new BufferedInputStream(MainActivity.clientSocket.getInputStream());
            outToServer = new BufferedOutputStream(MainActivity.clientSocket.getOutputStream());
        }catch(Exception e){}
        File sdCard = Environment.getExternalStorageDirectory();
        dir = new File(sdCard.getAbsolutePath() + "/Files");
        dir.mkdirs();
        final Button conn = (Button) findViewById(R.id.connection);
        conn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final AlertDialog.Builder alert = new AlertDialog.Builder(Main3Activity.this);
                alert.setCancelable(false);
                alert.setMessage("Do you want to disconnect the connection?");
                alert.setTitle("Disconnect");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                       flag = false;
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                alert.show();
            }
        });
        client = new TCPClient();
        client.start();
    }
    Handler handle = new Handler();
    public void showProgressBar(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                progressDialog.setProgress(progressStatus);
            }
        };
        handle.post(r);
    }
    public void updateUI(){
        Runnable r  = new Runnable(){
            @Override
            public void run() {
                final AlertDialog.Builder alert = new AlertDialog.Builder(Main3Activity.this);
                alert.setCancelable(false);
                alert.setMessage("Server wants to send " + fileName);
                alert.setTitle("File Send");
                alert.setPositiveButton("Receieve", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        int pos = fileName.lastIndexOf('.');
                        String extension = fileName.substring(pos+1,fileName.length());
                        if(extension != null &&  extension.equals("png"))isFile = false;
                        else isFile = true;
                        try {
                            fileSizeReceived = true;
                            outToServer.write("1".getBytes());
                            outToServer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        nameReceived = true;
                        try {
                            outToServer.write("0".getBytes());
                            outToServer.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
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
                Toast.makeText(Main3Activity.this,txt, Toast.LENGTH_LONG).show();
            }
        };
        handle.post(r);
    }
    void makeProgresbar(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                progressDialog = new ProgressDialog(Main3Activity.this);
                progressDialog.setTitle("Please Wait..");
                progressDialog.setMessage("Receiving File ...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
                progressDialog.setProgress(0);
                progressDialog.show();
            }
        };
        handle.post(r);
    }
    public class TCPClient extends  Thread{
        @Override
        public void run(){
            while(flag){
                inFromServer.mark(5);
                try {
                    if(inFromServer.read() == -1 )break;
                    inFromServer.reset();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep(500);
                } catch (Exception e) {
                }
                if(fileSizeReceived){
                    try {
                        if (inFromServer.available() > 0) {
                            fileSizeReceived = false;
                            bytesRead = inFromServer.read(data, 0, data.length);
                            String len = new String(data,0,bytesRead);
                            fileSize = Long.parseLong(len);
                            outToServer.write("1".getBytes());
                            outToServer.flush();
                            if(isFile) fileReceived = true;
                            else imageReceive = true;
                        }
                    } catch (Exception e) {
                    }
                }
                else if (fileReceived) {
                    try {
                        if (inFromServer.available() > 0) {
                            fileReceived = false;
                            nameReceived = true;
                            receivedFile = new File(dir, fileName);
                            fos = new FileOutputStream(receivedFile);
                            long counter = 0;
                            progressStatus = 0;
                            makeProgresbar();
                            while(counter < fileSize) {
                                try{
                                    bytesRead = inFromServer.read(data, 0, data.length);
                                    fos.write(data, 0, bytesRead);
                                    showProgressBar();
                                    counter+=bytesRead;
                                    progressStatus = (int)(counter*100/fileSize);
                                }catch(Exception e){
                                }
                            }
                            progressDialog.dismiss();
                            fos.flush();
                            fos.close();
                            showMessage("File Received");
                        }
                    } catch (Exception e) {
                    }
                }
                else if (nameReceived) {
                    try {
                        if (inFromServer.available() > 0) {
                            bytesRead = inFromServer.read(data, 0, data.length);
                            fileName = new String(data, 0, bytesRead);
                            nameReceived = false;
                            updateUI();
                        }
                    } catch (Exception e) {
                    }
                }
                else if(imageReceive){
                    try {
                        if (inFromServer.available() > 0) {
                            makeProgresbar();
                            imageReceive = false;
                            File file = new File(dir, fileName);
                            fos = new FileOutputStream(file);
                            byteArray = new byte[(int)fileSize];
                            progressStatus = 1;
                            showProgressBar();
                            for(int i= 0; i < byteArray.length; i++ ){
                                inFromServer.read(byteArray,i,1);
                            }
                            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                            progressStatus = 100;
                            showProgressBar();
                            progressDialog.dismiss();
                            fos.flush();
                            fos.close();
                            byteArray = null;
                            showMessage("Image Received");
                            nameReceived = true;
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
    public void onBackPressed() {}
    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            MainActivity.clientSocket.close();
            outToServer.close();
            inFromServer.close();
            MainActivity.clientSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
