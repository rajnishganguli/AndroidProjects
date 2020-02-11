package com.google.firebase.udacity.friendlychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class AllAvailableChatRoomsActivity extends AppCompatActivity {

    private String TAG = "AllAvailableChatRoomsactivity";

    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mChatRoomsDatabaseReference ;
    ValueEventListener mValueEventListener;
    ChatRoomsAdapter mChatRoomsAdapter;

    Query chatQuery;
    ArrayList<ChatRoom> chatRooms;

    private int CHAT_ROOM_COUNT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_available_chat_rooms);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mChatRoomsDatabaseReference = mFirebaseDatabase.getReference().child("DB");;

        chatRooms = new ArrayList<>();

        // Get all chat-rooms and their message count
        chatQuery = mChatRoomsDatabaseReference;
        createValueEventListener();
        chatQuery.addValueEventListener(mValueEventListener);

        mChatRoomsAdapter = new ChatRoomsAdapter(this, R.layout.item_chat_room, chatRooms);
        ListView listView = findViewById(R.id.available_chat_rooms_list);
        listView.setAdapter(mChatRoomsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatRoom chatRoom = mChatRoomsAdapter.getItem(position);
                // CURRENT_CHAT_ROOM_ID = chatRoom.getName();
                // Intent i = new Intent(AllAvailableChatRoomsActivity.this, MainActivity.class);
                // startActivity(i);

                Intent data = new Intent();
                data.putExtra("TAPPED_CHAT_ROOM", chatRoom.getName());
                setResult(RESULT_OK, data);
                finish();
            }
        });
    }

    private void createValueEventListener(){
        mValueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot ds: dataSnapshot.getChildren()) {

                    Query childChatQuery = ds.getRef().child("messages");
                    childChatQuery.addValueEventListener(new ValueEventListener() {

                        @Override
                        public void onDataChange(@NonNull DataSnapshot cds) {
                            chatRooms.add(new ChatRoom(cds.getRef().getParent().getKey(), (int) cds.getChildrenCount()));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Error reading the nested data" );
                        }
                    });
                }

                Log.d(TAG, "showAvailableChatRooms: CHAT_ROOM_COUNT: " + dataSnapshot.getChildrenCount());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "showAvailableChatRooms:" + "onCancelled", databaseError.toException());
            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
        chatQuery.removeEventListener(mValueEventListener);
        Log.e(TAG, "Removed value event listener");
        mValueEventListener = null;
        mChatRoomsAdapter.clear();
        chatQuery = null;
    }
}
