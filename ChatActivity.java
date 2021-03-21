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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import static java.lang.Math.sqrt;

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
    private String public_exponent = ""; //todo : do usunięcia, potrzebuje tylko 2 pola na klucz
    private String prime_factor_a = "";
    private String prime_factor_b = "";
    private String public_key = "";
    private String private_key = "";

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
            String public_private_key_ = getPublicKey(prime_factor_a, prime_factor_b);
            if( public_private_key_ != "ErrorRSA" ) {
                String[] public_private_key = public_private_key_.split(";");
                public_key = public_private_key[0] + ";" + public_private_key[1];
                private_key = public_private_key[0] + ";" + public_private_key[2];
            }
            else {
                System.out.println("error creating keys");
            }
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
                        String[] tempKeys = keys.getString("public_key").split(";");
                        String key1 = tempKeys[ 0 ];
                        String key2 = tempKeys[ 1 ];
                        encrypted_message = encryptMessageRSA(message, key1, key2 );
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
                        jsonObject.put("message", encrypted_message);
                        webSocket.send(jsonObject.toString());

                        jsonObject.put("isSent", true);
                        jsonObject.put("message", message);

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

    private String messageToASCII(String message) { //TODO: message to ASCII String
        return message;
    }

    private String ASCIItoMessage(String message) { //TODO: ASCII String to message
        return message;
    }

    private List<Long> divisorsNumber(long number )
    {
        List<Long> divisors = new ArrayList<Long>();
        divisors.add( number );
        for( int i = 2; i <= sqrt( number ); i++ )
        {
            if( number % i == 0 )
            {
                divisors.add( (long) i );
                divisors.add( number / i );
            }
        }
        return divisors;
    }
    private boolean listCompare( List<Long> list1, List<Long> list2 )
    {
        for( int i = 0; i < list1.size(); i++ )
        {
            int ind = list2.indexOf( list1.get( i ) );
            if( ind >= 0 )
            {
                return true;
            }
        }
        return false;
    }
    private long modulo( long number, long mod )
    {
        while( number < 0 ){ number += mod; }
        return number % mod;
    }
    private String getPublicKey(String first_prime, String second_prime) { //TODO: calculate RSA public key
        long first_factor = Long.parseLong( first_prime );
        long second_factor = Long.parseLong( second_prime );
        long n = first_factor * second_factor;
        long euler = ( first_factor - 1 ) * ( second_factor - 1 );
        long e = euler;
        List<Long> e_div = divisorsNumber( e );
        List<Long> euler_div = divisorsNumber( euler );
        while( listCompare( e_div, euler_div ) == true )
        {
            e = (long) ( ( Math.random() * ( euler - 3 ) ) + 2 );
            if( e % 2 == 0 ){ continue; }
            e_div = divisorsNumber( e );
        }
        long param1 = euler, param2 = euler;
        long param3 = e, param4 = 1;
        long newParam1, newParam2, temp;
        while( param3 != 1 )
        {
            temp = param1 / param3;
            newParam1 = param1 - ( temp * param3 );
            newParam2 = temp * param4;
            newParam2 = ( param2 - newParam2 );

            param1 = param3;
            param2 = param4;
            param3 = modulo( newParam1, euler);
            param4 = modulo( newParam2, euler);
        }
        long d = param4;
        System.out.println( "!@!@#!@!" + Long.toString( n ) + ';' + Long.toString( e ) + ';' + Long.toString( d ) + ';' + Long.toString( euler ) );
        BigInteger longD = new BigInteger( Long.toString( d ) );
        BigInteger longE = new BigInteger( Long.toString( e ) );
        BigInteger longEULER = new BigInteger( Long.toString( euler ) );
        BigInteger longlong = longD.multiply(longE);
        BigInteger longlong2 = longlong.mod( longEULER );
        if( longlong2.compareTo( BigInteger.ONE ) == 0 )
        {
            return Long.toString( n ) + ';' + Long.toString( e ) + ';' + Long.toString( d );
        }
        System.out.println("error creating keys value " + longD.toString() + "|" + longlong.toString() + "|" + longlong2.toString() );
        return "ErrorRSA";
    }

    private int charToInt( char ch)
    {
        String chars = "QWERTYUIOPASDFGHJKLZXCVBNM 1234567890-=qwertyuiop[]asdfghjkl;'zxcvbnm,./{}:ąęśćżźńół\"\\_+!@#$%^&*()`~";
        return chars.indexOf( ch );
    }
    private char intToChar( int index)
    {
        String chars = "QWERTYUIOPASDFGHJKLZXCVBNM 1234567890-=qwertyuiop[]asdfghjkl;'zxcvbnm,./{}:ąęśćżźńół\"\\_+!@#$%^&*()`~";
        return chars.charAt( index );
    }

    private String getPublicKeyB(String prime_p, String alpha, String factor_g) { //TODO: calculate ElGamal public B number
        return prime_p + alpha + factor_g;
    }

    private String encryptMessageRSA(String message, String public_key1, String public_key2 ) { //TODO: encrypting RSA
        long n = Long.parseLong( public_key1 );
        long e = Long.parseLong( public_key2 );
        BigInteger longE = new BigInteger( Long.toString( e ) );
        BigInteger longN = new BigInteger( Long.toString( n ) );
        long m = 0;
        long m_, ind;
        int J = 0;
        while( J < message.length() % 10){
            J++;
            //message += "_";
        }
        String encryptMsg = "";
        System.out.println(message);
        for( int i = 0; i < message.length(); i++ )
        {
            m_ = charToInt( message.charAt( i ) );
            //System.out.println("symbol#: " + m_ );
            if( m * 100 + m_ < n )
            {
                m = m * 100 + m_;
            }
            else
            {

                BigInteger longM = new BigInteger( Long.toString( m ) );
                BigInteger longind = longM.modPow( longE, longN );
                String indToString = longind.toString( );
                while( indToString.length() < 10 ){
                    indToString = "0" + indToString;
                }
                System.out.println("ind-" + indToString + " m:" + Long.toString( m ) );
                encryptMsg += indToString;
                m = m_;
            }
        }
        BigInteger longM = new BigInteger( Long.toString( m ) );
        BigInteger longind = longM.modPow( longE, longN );
        String indToString = longind.toString( );
        while( indToString.length() < 10 ){
            indToString = "0" + indToString;
        }
        System.out.println("ind-" + indToString + " m:" + Long.toString( m ) );
        encryptMsg += indToString;

        return encryptMsg;
    }

    private String decryptMessageRSA(String message) { //TODO: decrypting RSA
        String[] privKeys = private_key.split(";");
        long n = Long.valueOf( privKeys[ 0 ] ).longValue();
        long d = Long.parseLong( privKeys[ 1 ] );
        BigInteger longD = new BigInteger( Long.toString( d ) );
        BigInteger longN = new BigInteger( Long.toString( n ) );
        String msg = "", msg2 = "";
        long blok;
        int i;
        System.out.println( "DLUGOSC:" + message.length() );
        for( i = 0; i < message.length() - 9; i += 10 )
        {
            blok = Long.parseLong( message.substring( i, i + 10 ) );
            BigInteger longBLOK = new BigInteger( Long.toString( blok ) );
            BigInteger m = longBLOK.modPow( longD, longN );
            System.out.println( Long.toString( blok ) + "->" + m.toString() );
            msg += m.toString();
        }
        if( i < message.length() - 1 ) {
            blok = Long.parseLong( message.substring( i ) );
            BigInteger longBLOK = new BigInteger(Long.toString(blok));
            BigInteger m = longBLOK.modPow(longD, longN);
            msg += m.toString();
        }
        for( i = 0; i < msg.length() - 1; i += 2 )
        {
            //System.out.println("symbol$: " + msg.substring( i, i + 2 ) );
            char ch = intToChar( Integer.parseInt( msg.substring( i, i + 2 ) ) );
            msg2 += ch;
        }
        return msg2 + "||" + msg + " DECRYPTED";
    }

    private String encryptMessageElGamal(String message, String public_p, String public_b, String public_g) { //TODO: encrypting ElGamal
        return "ELGAMAL " +message+ " ENCRYPTED ";
    }

    private String decryptMessageElGamal(String message) { //TODO: deencrypting ElGamal
        return message + " DECRYPTED";
    }

}