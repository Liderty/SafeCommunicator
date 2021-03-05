package com.marlib.safecommunicator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter {

    private final static int TYPE_MESSAGE_SENT = 0;
    private final static int TYPE_MESSAGE_RECIVED = 1;

    private LayoutInflater inflater;
    private List<JSONObject> messages = new ArrayList<>();

    public MessageAdapter (LayoutInflater inflater) {
        this.inflater = inflater;
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        public SentMessageHolder(@NonNull View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.tvSentMessage);
        }
    }

    private class RecivedMessageHolder extends RecyclerView.ViewHolder {

        TextView nameTextView;
        TextView messageTextView;

        public RecivedMessageHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.tvName);
            messageTextView = itemView.findViewById(R.id.tvRecivedMessage);
        }
    }

    @Override
    public int getItemViewType(int position) {
        JSONObject message = messages.get(position);

        try {
            if(message.getBoolean("isSent")){
                if(message.has("message")) return TYPE_MESSAGE_SENT;
            } else {
                if(message.has("message")) return TYPE_MESSAGE_RECIVED;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        switch (viewType) {
            case TYPE_MESSAGE_SENT:
                view = inflater.inflate(R.layout.item_sent_message, parent, false);
                return new SentMessageHolder(view);

            case TYPE_MESSAGE_RECIVED:
                view = inflater.inflate(R.layout.item_recived_message, parent, false);
                return new RecivedMessageHolder(view);

        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        JSONObject message = messages.get(position);

        try {
            if(message.getBoolean("isSent")) {
                if(message.has("message")){
                    SentMessageHolder messageHolder = (SentMessageHolder) holder;
                    messageHolder.messageText.setText(message.getString("message"));
                }
            } else {
                if(message.has("message")){
                    RecivedMessageHolder messageHolder = (RecivedMessageHolder) holder;
                    messageHolder.nameTextView.setText(message.getString("name"));
                    messageHolder.messageTextView.setText(message.getString("message"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void addItem (JSONObject jsonObject) {
        messages.add(jsonObject);
        notifyDataSetChanged();
    }
}
