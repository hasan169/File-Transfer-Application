package com.example.shago_000.filetransfer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    Button btnClient;
    Button btnServer;
    ProgressDialog progressDialog;
    static ServerSocket welcomeSocket;
    static Socket connectionSocket;
    static String ipAddress;
    static Socket clientSocket;
    Intent intent = null;
    public void showMessage(){
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Downloading Video ...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        btnServer=(Button)findViewById(R.id.send);
        btnServer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    welcomeSocket = new ServerSocket(6789);
                    welcomeSocket.setSoTimeout(20000);
                    connectionSocket = welcomeSocket.accept();
                    connectionSocket.setSoTimeout(2000);
                    intent = new Intent(getApplicationContext(), Main2Activity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    try {
                        welcomeSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    welcomeSocket = null;
                    Toast.makeText(MainActivity.this,"Not Found", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        });
        btnClient=(Button)findViewById(R.id.receieve);
        btnClient.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setCancelable(false);
                final EditText edittext= new EditText(getApplicationContext());
                edittext.setTextColor(Color.BLACK);
                alert.setMessage("Enter the IP address to connect");
                alert.setTitle("IP Address");
                alert.setView(edittext);
                alert.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ipAddress = edittext.getText().toString();
                        try {
                            clientSocket = new Socket();
                            clientSocket.setSoTimeout(2000);
                            clientSocket.connect(new InetSocketAddress(ipAddress,6789),10000);
                            intent = new Intent(getApplicationContext(), Main3Activity.class);
                            startActivity(intent);
                        } catch (Exception e) {
                            clientSocket = null;
                            Toast.makeText(MainActivity.this,"Not Found", Toast.LENGTH_LONG).show();
                        };

                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {}
                });
                alert.show();
            }
        });
    }
}
