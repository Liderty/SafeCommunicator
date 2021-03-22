package com.marlib.safecommunicator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

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

    private int encrypting_method = 0;

    /* RSA */
    private String public_exponent = "";
    private String prime_factor_a = "";
    private String prime_factor_b = "";
    private String public_key = "";

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

    private ElGamal elGamal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        name = getIntent().getStringExtra("name");
        server_ip_adress = getIntent().getStringExtra("adress");
        encrypting_method = getIntent().getIntExtra("encrypting", 0);

        if(encrypting_method==0) {
            prime_factor_a = getIntent().getStringExtra("first_prime");
            prime_factor_b = getIntent().getStringExtra("second_prime");
            public_exponent = getIntent().getStringExtra("exponent");

            public_key = getPublicKey(prime_factor_a, prime_factor_b, public_exponent);

        } else if (encrypting_method==1) {
            prime_p = getIntent().getStringExtra("prime_p");
            alpha = getIntent().getStringExtra("alpha");
            factor_g = getIntent().getStringExtra("factor_g");

            number_b = getPublicKeyB(prime_p, alpha, factor_g);
        }



        initiateSocketConnection();
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
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            super.onClosed(webSocket, code, reason);
            rejectConnection();
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            super.onMessage(webSocket, text);

            runOnUiThread(() -> {
                try {
                    JSONObject jsonObject  = new JSONObject(text);
                    if(jsonObject.has("task")){

                        String task = jsonObject.getString("task");

                        if(task.equals(GET_ENCRYPTING_METHOD)) {
                            server_encrypthing_method = jsonObject.getString("encrypting_method");
                            System.out.println("SERVER_ENCRYPTING_METHOD:"+server_encrypthing_method);
                            resolveServerKeys();

                        } else if(task.equals(GET_CONNECTIONS)) {
                            keys = jsonObject;
                            System.out.println(keys.toString());
                            System.out.println("CONNECTION ADDED");
                            System.out.println(jsonObject.toString());
                        }

                    } else {
                        String deencrypted_message = "";

                        if(encrypting_method==0) {
                            deencrypted_message = decryptMessageRSA(jsonObject.getString("message"));
                        } else if(encrypting_method==1) {
                            deencrypted_message = decryptMessageElGamal(jsonObject.getString("message"));
                        }

                        jsonObject.put("isSent", false);
                        jsonObject.put("message", deencrypted_message);
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


        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();
            String encrypted_message = "";

            if(keys != null) {
                System.out.println("KLUCZE " + keys.toString());
                if(encrypting_method==0) {
                    try {
                        encrypted_message = encryptMessageRSA(message, keys.getString("public_key"), keys.getString("public_exponent"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if(encrypting_method==1) {
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
                        jsonObject.put("message", message);
                        webSocket.send(jsonObject.toString());

                        jsonObject.put("isSent", true);
                        jsonObject.put("message", encrypted_message);

                        messageAdapter.addItem(jsonObject);
                        mainChatRecycleView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                        resetMessageEdit();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(ChatActivity.this, "NULL", Toast.LENGTH_SHORT).show();
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

    private void sendRSAKeys() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("task", SET_ENCRYPTING_METHOD);
            jsonObject.put("encrypting_method", Integer.toString(encrypting_method));
            jsonObject.put("public_key", public_key);
            jsonObject.put("public_exponent", public_exponent);

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

    private void rejectConnection() { // TODO: make better rejection
        finish();
    }

    private void resolveServerKeys() {
        if(server_encrypthing_method.equals("-1")){
            setServerEncryptingMethod();
            sendPublicKey();

        } else if(server_encrypthing_method.equals(Integer.toString(encrypting_method))) {
            sendPublicKey();

        } else {
            Toast.makeText(ChatActivity.this, "Wrong Encrypting", Toast.LENGTH_SHORT).show();
            rejectConnection();
        }
    }

    private String StringToASCII(String message) {
        return StringUtils.convertToAsciiString(message);
    }

    private String ASCIItoString(String message) {
        return StringUtils.convertAsciiStringToString(message);
    }

    private String getPublicKey(String first_prime, String second_prime, String exponent) { //TODO: calculate RSA public key
        return first_prime + second_prime + exponent;
    }

    private String getPublicKeyB(String prime_p, String alpha, String factor_g) { //TODO: calculate ElGamal public B number
        elGamal = new ElGamal(prime_p, alpha, factor_g);
        String message = StringToASCII("Test jakiejs wiadomosci");
        System.out.println("MGS: "+message);

        String encrypted = elGamal.encrypt(message);
        System.out.println("ENCRYPTED: "+encrypted);

        String decrypted = elGamal.decrypt(encrypted);
        System.out.println("DECRYPTED: "+decrypted);

        System.out.println("DECRYPTED MSG: "+ASCIItoString(decrypted));
        return elGamal.getB().toString();
    }

    private String encryptMessageRSA(String message, String public_key, String public_exponent) { //TODO: encrypting RSA
        return "RSA " +message+ " ENCRYPTED ";
    }

    private String decryptMessageRSA(String message) { //TODO: decrypting RSA
        return message + " DECRYPTED";
    }

    private String encryptMessageElGamal(String message, String public_p, String public_b, String public_g) { //TODO: encrypting ElGamal
        return "ELGAMAL " +message+ " ENCRYPTED ";
    }

    private String decryptMessageElGamal(String message) { //TODO: deencrypting ElGamal
        return message + " DECRYPTED";
    }

}