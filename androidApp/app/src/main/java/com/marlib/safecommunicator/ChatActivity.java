package com.marlib.safecommunicator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatActivity extends AppCompatActivity {

    private WebSocket webSocket;
    private String name = "User";
    private String server_ip_adress;

    private static final String SET_ENCRYPTING_METHOD = "set_encrypting_method";
    private static final String SET_SERVER_ENCRYPTING_METHOD = "set_server_encrypting_method";
    private static final String GET_ENCRYPTING_METHOD = "get_encrypting_method";
    private static final String GET_CONNECTIONS = "get_connections";
    private static final String SET_PENDING_CONNECTION = "set_pending_connection";
    private static final String GET_KEYS = "get_keys";
    private static final String REMOVE_CONNECTION = "remove_connection";


    private static final String ENCRYPTING_RSA = "RSA";
    private static final String ENCRYPTING_ELGAMAL = "ElGamal";
    private static final String PENDING_CONNCECTION = "Pending connection...";
    private static final String CONNECTED = "Connected";
    private static final String CONNECTION_FAILED = "Connection Failed!";

    private int encrypting_method = 0;

    /* RSA */
    private String prime_factor_a = "";
    private String prime_factor_b = "";
    private String public_key = "";
    private RSA rsa;

    /* ELGAMAL */
    private String prime_p = "";
    private String alpha = "";
    private String factor_g = "";
    private String number_b = "";

    /* SERVER SIDE */
    private String server_encrypthing_method = "";

    /* OTHER KEYS */
    private JSONObject keys;

    private String server_path = "ws://";
    private EditText messageEditText;
    private View sendButton;
    private RecyclerView mainChatRecycleView;
    private MessageAdapter messageAdapter;
    private TextView statusTextView;
    private TextView usernameTextView;
    private TextView encryptingTextView;

    private ElGamal elGamal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        name = getIntent().getStringExtra("name");
        server_ip_adress = getIntent().getStringExtra("adress");
        encrypting_method = getIntent().getIntExtra("encrypting", 0);

        if (encrypting_method == 0) {
            prime_factor_a = getIntent().getStringExtra("first_prime");
            prime_factor_b = getIntent().getStringExtra("second_prime");
            rsa = new RSA(prime_factor_a, prime_factor_b);
            String public_private_key_ = rsa.getPublicKey();

            if (public_private_key_ != "ErrorRSA") {
                String[] public_private_key = public_private_key_.split(";");
                public_key = public_private_key[0] + ";" + public_private_key[1] + ";" + public_private_key[2];
            } else {
                Toast.makeText(ChatActivity.this, "Error while keys creating!", Toast.LENGTH_SHORT).show();
            }

        } else if (encrypting_method == 1) {
            if (!name.equals("testname2")) {
                prime_p = getIntent().getStringExtra("prime_p");
                alpha = getIntent().getStringExtra("alpha");
                factor_g = getIntent().getStringExtra("factor_g");
            } else {
                prime_p = "2357";
                alpha = "1751";
                factor_g = "2";
            }
            number_b = getPublicKeyB(prime_p, alpha, factor_g);
        }

        initiateSocketConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocket.close(1000, "Closing Connection");
    }

    private void setUsername(String username) {
        usernameTextView.setText(username);
    }

    private void setEncryptingName() {
        if(encrypting_method==0){
            encryptingTextView.setText(ENCRYPTING_RSA);
        } else encryptingTextView.setText(ENCRYPTING_ELGAMAL);
    }

    private void setStatus(Boolean status) {
        if(status){
            statusTextView.setText(CONNECTED);
            statusTextView.setTextColor(Color.parseColor("#00FF00"));
        } else {
            statusTextView.setText(PENDING_CONNCECTION);
            statusTextView.setTextColor(Color.WHITE);
        }
    }

    private void initiateSocketConnection() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(server_path + server_ip_adress).build();
        webSocket = client.newWebSocket(request, new SocketListener());
    }

    private class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            super.onOpen(webSocket, response);

            runOnUiThread(() -> {
                Toast.makeText(ChatActivity.this, "Socket Connection Successful", Toast.LENGTH_SHORT).show();

                initializeView();
                getServerEncryptingMethod();
            });
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            rejectConnection();
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);

            runOnUiThread(() -> {
                try {
                    JSONObject jsonObject = new JSONObject(text);
                    if (jsonObject.has("task")) {

                        String task = jsonObject.getString("task");

                        if (task.equals(GET_ENCRYPTING_METHOD)) {
                            server_encrypthing_method = jsonObject.getString("encrypting_method");
                            resolveServerKeys();

                        } else if (task.equals(GET_CONNECTIONS)) {
                            keys = jsonObject;
                            setStatus(true);

                        } else if (task.equals(SET_PENDING_CONNECTION)) {
                            setStatus(false);
                        }
                    } else {
                        String decrypted_message = "";

                        if (encrypting_method == 0) {
                            decrypted_message = rsa.decryptMessageRSA(jsonObject.getString("message"));
                        } else if (encrypting_method == 1) {
                            decrypted_message = decryptMessageElGamal(jsonObject.getString("message"));
                        }

                        jsonObject.put("isSent", false);
                        jsonObject.put("message", decrypted_message);
                        messageAdapter.addItem(jsonObject);
                        mainChatRecycleView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void initializeView() {
        messageEditText = findViewById(R.id.etvMessage);
        sendButton = findViewById(R.id.btnSend);

        mainChatRecycleView = findViewById(R.id.rvMainChat);
        messageAdapter = new MessageAdapter(getLayoutInflater());
        mainChatRecycleView.setAdapter(messageAdapter);
        mainChatRecycleView.setLayoutManager(new LinearLayoutManager(this));
        usernameTextView = findViewById(R.id.tvUsername);
        encryptingTextView = findViewById(R.id.tvEncrypting);
        statusTextView = findViewById(R.id.tvStatus);

        setUsername(name);
        setEncryptingName();

        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();
            String encrypted_message = "";

            if (keys != null) {
                if (encrypting_method == 0) {
                    try {
                        String[] tempKeys = keys.getString("public_key").split(";");
                        String key1 = tempKeys[0];
                        String key2 = tempKeys[1];
                        int key3 = Integer.parseInt(tempKeys[2]);

                        encrypted_message = rsa.encryptMessageRSA(message, key1, key2, key3);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (encrypting_method == 1) {
                    try {
                        encrypted_message = encryptMessageElGamal(message, keys.getString("public_p"), keys.getString("public_b"), keys.getString("public_g"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                if (encrypted_message != "") {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("name", name);
                        jsonObject.put("message", encrypted_message);
                        jsonObject.put("isSent", true);
                        webSocket.send(jsonObject.toString());

                        jsonObject.put("message", message);
                        messageAdapter.addItem(jsonObject);
                        mainChatRecycleView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);

                        resetMessageEdit();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(ChatActivity.this, "Need interlocutor!", Toast.LENGTH_SHORT).show();
                requestKeys();
            }
        });
    }

    private void resetMessageEdit() {
        messageEditText.setText("");
    }

    private void sendPublicKey() {
        if (encrypting_method == 0) {
            sendRSAKeys();
        } else if (encrypting_method == 1) {
            sendElGamalKeys();
        }
    }

    private void requestKeys() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("task", GET_KEYS);
            webSocket.send(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendRSAKeys() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("task", SET_ENCRYPTING_METHOD);
            jsonObject.put("encrypting_method", Integer.toString(encrypting_method));
            jsonObject.put("public_key", public_key);

            webSocket.send(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendElGamalKeys() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("task", SET_ENCRYPTING_METHOD);
            jsonObject.put("encrypting_method", Integer.toString(encrypting_method));
            jsonObject.put("public_p", prime_p);
            jsonObject.put("public_b", number_b);
            jsonObject.put("public_g", factor_g);

            webSocket.send(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getServerEncryptingMethod() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("task", GET_ENCRYPTING_METHOD);

            webSocket.send(jsonObject.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setServerEncryptingMethod() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("task", SET_SERVER_ENCRYPTING_METHOD);
            jsonObject.put("encrypting_method", Integer.toString(encrypting_method));

            webSocket.send(jsonObject.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void rejectConnection() {
        encryptingTextView.setTextColor(Color.RED);
        statusTextView.setText(CONNECTION_FAILED);
        statusTextView.setTextColor(Color.RED);

        Toast.makeText(ChatActivity.this, "Wrong Encrypting", Toast.LENGTH_SHORT).show();
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                finish();
            }
        }, 3000L);
    }

    private void resolveServerKeys() {
        if (server_encrypthing_method.equals("-1")) {
            setServerEncryptingMethod();
            sendPublicKey();

        } else if (server_encrypthing_method.equals(Integer.toString(encrypting_method))) {
            sendPublicKey();

        } else {
            Toast.makeText(ChatActivity.this, "Wrong Encrypting", Toast.LENGTH_SHORT).show();
            rejectConnection();
        }
    }

    private String getPublicKeyB(String prime_p, String alpha, String factor_g) {
        elGamal = new ElGamal(prime_p, alpha, factor_g);
        return elGamal.getB().toString();
    }

    private String encryptMessageElGamal(String message, String public_p, String public_b, String public_g) {
        return elGamal.encrypt(StringUtils.convertToAsciiString(message), public_p, public_b, public_g);
    }

    private String decryptMessageElGamal(String encryptedMessage) {
        String decrypted = elGamal.decrypt(encryptedMessage);
        return StringUtils.convertAsciiStringToString(decrypted);
    }
}