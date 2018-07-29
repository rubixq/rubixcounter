package com.rubixq.rubixcounter.ui.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.rubixq.rubixcounter.R;
import com.rubixq.rubixcounter.core.Config;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.concurrent.ExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {
    ImageButton settingsButton;
    TextView ticketNumberLabel;
    TextView counterNumberLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String baseUrl = sharedPreferences.getString(Config.RUBIX_CORE_HOST, "");
        String counterNumber = sharedPreferences.getString(Config.APP_COUNTER_NUMBER,"");
        baseUrl = (baseUrl.equals("") ? "" : baseUrl);
        counterNumber = (counterNumber.equals("") ? "" : counterNumber);
        if (!TextUtils.isEmpty(baseUrl) && !TextUtils.isEmpty(counterNumber)) {
            connectToServer(baseUrl);
        } else {
            String message = "Configure server connection settings and counter information";
            showInfoDialog("No Configuration", message);
        }
    }

    private void initViews() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        settingsButton = findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSettingsActivity();
            }
        });

        ticketNumberLabel = findViewById(R.id.tick_number_label);
        ticketNumberLabel.setText("TICKET # 0000");

        counterNumberLabel = findViewById(R.id.counter_number_label);
        String counterNumber = sharedPreferences.getString(Config.APP_COUNTER_NUMBER, "");
        counterNumber = (counterNumber.equals("") ? "0" : counterNumber);
        counterNumberLabel.setText(String.format("COUNTER NO. %s", counterNumber));

    }

    @Override
    public void onBackPressed() {

    }

    private void connectToServer(String baseUrl) {
        Request request = new Request.Builder()
                .url(baseUrl)
                .build();

        WSListener wsListener = new WSListener();
        OkHttpClient client = new OkHttpClient();
        WebSocket webSocket = client.newWebSocket(request, wsListener);
        client.dispatcher().executorService().shutdown();

        Log.d("WS",baseUrl);
    }

    public void showInfoDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        builder.show();
    }

    private void showSettingsActivity() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Enter Password");

        final EditText inputField = new EditText(getApplicationContext());
        inputField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(inputField);

        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String adminPassword = sharedPreferences.getString(Config.APP_ADMIN_PASS, "");
                adminPassword = (adminPassword.equals("") ? Config.DEFAULT_APP_ADMIN_PASS : adminPassword);
                String userPassword = inputField.getText().toString().trim();

                if (userPassword.equals(adminPassword) || userPassword.equals(Config.DEFAULT_APP_ADMIN_PASS)) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Wrong password specified", Toast.LENGTH_SHORT).show();
                }

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }

    private void updateTicketNumber(String ticketNumber){
        ticketNumberLabel.setText(String.format("TICKET # %s",ticketNumber));
    }

    private class WSListener extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            Log.d("OPS", response.toString());
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            String counterNumber = sharedPreferences.getString(Config.APP_COUNTER_NUMBER,"");
            try{
                JSONObject json = new JSONObject();
                json.put("counterId", counterNumber);

                webSocket.send(json.toString());
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            Log.d("MSG",text);
            try{
                JSONObject json = new JSONObject(text);
                String type = (json.has("type")? json.getString("type") : "");
                final String data = (json.has("data")? json.getString("data") : "");

                if(type.equalsIgnoreCase("welcome")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Connected to server successfully", Toast.LENGTH_LONG).show();
                        }
                    });
                }else if(type.equalsIgnoreCase("update")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateTicketNumber(data);
                        }
                    });
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            Log.d("FIL", t.getMessage());
        }
    }
}
