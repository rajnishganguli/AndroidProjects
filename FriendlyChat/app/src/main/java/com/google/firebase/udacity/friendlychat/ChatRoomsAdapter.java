package com.google.firebase.udacity.friendlychat;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ChatRoomsAdapter extends ArrayAdapter<ChatRoom> {

    public ChatRoomsAdapter(Context context, int resource, List<ChatRoom> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            // listItemView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_chat_room, parent, false);
            listItemView = LayoutInflater.from(getContext()).inflate( R.layout.item_chat_room, parent, false);
        }

        ChatRoom currentChatRoom = getItem(position);

        TextView chatRoomNameTextView = listItemView.findViewById(R.id.chat_room_name_text_view);
        TextView chatRoomMessageCountTextView = listItemView.findViewById(R.id.chat_room_message_count_text_view);

        chatRoomNameTextView.setText(currentChatRoom.getName());
        chatRoomMessageCountTextView.setText(Integer.toString(currentChatRoom.getMessageCount()));

        return listItemView;
    }

}
