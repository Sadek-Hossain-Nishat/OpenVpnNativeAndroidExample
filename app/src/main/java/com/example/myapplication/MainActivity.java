package com.example.myapplication;

import static android.system.Os.open;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.blinkt.openvpn.OpenVpnApi;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.VpnStatus;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private Button button;
    boolean vpnStart = false;

    private OpenVPNThread vpnThread = new OpenVPNThread();
    private OpenVPNService vpnService = new OpenVPNService();

    private Server server;
    private CheckInternetConnection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connection = new CheckInternetConnection();


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    200);
        } else {
            // Permission already granted, perform desired operation
            // Access the storage or perform other operations here
            readFileData();
        }





        server = new Server(

                "usa.ovpn",
                "freeopenvpn",
                "636072992"
        );

        editText = findViewById(R.id.editTextText);
        button = findViewById(R.id.button);




        button.setText("Connect");

        VpnStatus.initLogCache(getCacheDir());

        isServiceRunning();


        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                Log.d("click", "onClick: "+button.getText());


                CharSequence text = button.getText();
                if (vpnStart) {
                    confirmDisconnect();
                }else {
                    prepareVpn();
                }




            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            // Check if the permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, perform desired operation
                // Access the storage or perform other operations here
                readFileData();
            } else {
                // Permission denied, handle accordingly
                // You may show a message or disable functionality
            }
        }
    }

    private void readFileData()  {
        File file = new File(Environment.getExternalStorageDirectory(), "usa.ovpn");

        Log.d("readfiledata", "readFileData file exists: \n\n"+file.exists());
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
            String config = "";
            String line;

            while (true) {
                line = br.readLine();
                if (line == null) break;
                config += line + "\n";
            }
            br.readLine();
            Log.d("readfiledata", "readFileData file data: \n\n"+config);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("readfiledata", "readFileData ERROR: \n\n"+e.getMessage());
        }


        Log.d("readfiledata", "readFileData: "+ Environment.DIRECTORY_DOWNLOADS);

//        for (File file:getFilesDir().listFiles()
//             ) {
//
//            Log.d("readfiledata", "readFile Name: "+file.getPath());
//
//        }

        /***

        try {
            // .ovpn file
            FileInputStream conf = openFileInput(new File("/storage/emulated/0/Download/usa.ovpn").getAbsolutePath());

            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            String config = "";
            String line;

            while (true) {
                line = br.readLine();
                if (line == null) break;
                config += line + "\n";
            }

            br.readLine();
//            OpenVpnApi.startVpn(this, config, "USA", server.getOvpnUserName(), server.getOvpnUserPassword());

            // Update log
            Log.d("readfiledata", "readFileData: "+config);

        } catch (IOException e) {
            e.printStackTrace();
        }

        ***/



    }

    private void closeVpn() {
    }

    private void prepareVpn() {
        if (!vpnStart) {
            if (getInternetStatus()) {

                // Checking permission for network monitor
                Intent intent = VpnService.prepare(this);

                if (intent != null) {
                    startActivityForResult(intent, 1);
                } else startVpn();//have already permission

                // Update confection status
                status("connecting");

            } else {

                // No internet connection available
                Toast.makeText(this,"you have no internet connection !!",Toast.LENGTH_LONG);
            }

        } else if (stopVpn()) {

            // VPN is stopped, show a Toast message.
          Toast.makeText(this,"Disconnect Successfully",Toast.LENGTH_LONG).show();
        }
    }

    public boolean stopVpn() {
        try {
            vpnThread.stop();

            button.setText("connect");
            vpnStart = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void startVpn() {
        try {
            // .ovpn file
            InputStream conf = getAssets().open(server.getOvpn());

            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            String config = "";
            String line;

            while (true) {
                line = br.readLine();
                if (line == null) break;
                config += line + "\n";
            }

            br.readLine();
            OpenVpnApi.startVpn(this, config, "USA", server.getOvpnUserName(), server.getOvpnUserPassword());

            // Update log
            button.setText("Connecting...");
            vpnStart = true;

        } catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
    }


    public void isServiceRunning() {
        button.setText(vpnService.getStatus());
    }


    public boolean getInternetStatus() {
        return connection.netCheck(this);
    }



    public void confirmDisconnect(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you close the Vpn?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                stopVpn();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                String duration = intent.getStringExtra("duration");
                String lastPacketReceive = intent.getStringExtra("lastPacketReceive");
                String byteIn = intent.getStringExtra("byteIn");
                String byteOut = intent.getStringExtra("byteOut");

                if (duration == null) duration = "00:00:00";
                if (lastPacketReceive == null) lastPacketReceive = "0";
                if (byteIn == null) byteIn = " ";
                if (byteOut == null) byteOut = " ";
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };



    public void setStatus(String connectionState) {
        if (connectionState!= null)
            switch (connectionState) {
                case "DISCONNECTED":
                    status("connect");
                    vpnStart = false;
                    vpnService.setDefaultStatus();
                    button.setText("");
                    break;
                case "CONNECTED":
                    vpnStart = true;// it will use after restart this activity
                    status("connected");
                    button.setText("");
                    break;
                case "WAIT":
                    button.setText("waiting for server connection!!");
                    break;
                case "AUTH":
                    button.setText("server authenticating!!");
                    break;
                case "RECONNECTING":
                    status("connecting");
                    button.setText("Reconnecting...");
                    break;
                case "NONETWORK":
                    button.setText("No network connection");
                    break;
            }

    }



    public void status(String status) {

        if (status.equals("connect")) {
            button.setText("Connect");
        } else if (status.equals("connecting")) {
            button.setText("Connecting");
        } else if (status.equals("connected")) {

            button.setText("Connected");

        } else if (status.equals("tryDifferentServer")) {


            button.setText("Try Different\nServer");
        } else if (status.equals("loading")) {

            button.setText("Loading Server..");
        } else if (status.equals("invalidDevice")) {

            button.setText("Invalid Device");
        } else if (status.equals("authenticationCheck")) {

            button.setText("Authentication \n Checking...");
        }

    }


    public void updateConnectionStatus(String duration, String lastPacketReceive, String byteIn, String byteOut) {
        button.setText("Duration: " + duration);
        button.setText("Packet Received: " + lastPacketReceive + " second ago");
        button.setText("Bytes In: " + byteIn);
        button.setText("Bytes Out: " + byteOut);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            //Permission granted, start the VPN
            startVpn();
        } else {
           Toast.makeText(this,"Permission Deny !! ",Toast.LENGTH_LONG).show();
        }
    }

}