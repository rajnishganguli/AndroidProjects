package com.google.firebase.udacity.friendlychat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.google.firebase.udacity.friendlychat.MainActivity.CURRENT_CHAT_ROOM_ID;


public class EditorActivity extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;

    private String mText;
    private String mPhotoUrl;
    private String mName;
    private String mId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        final Intent i = getIntent();
        mName = i.getStringExtra("name");
        mText = i.getStringExtra("text");
        mPhotoUrl = i.getStringExtra("photoUrl");
        mId = i.getStringExtra("id");

        Toast.makeText(this, "EditText fired", Toast.LENGTH_SHORT).show();

        final TextView senderView = findViewById(R.id.edit_chat_author);
        final EditText textView = findViewById((R.id.edit_chat_text));
        ImageView imageView = findViewById(R.id.edit_chat_image);

        Button chatDeleteButton = findViewById(R.id.chat_delete_button);
        Button chatUpdateButton = findViewById(R.id.chat_update_button);

        senderView.setText(mName);

        boolean isPhoto = !TextUtils.isEmpty(mPhotoUrl);
        Log.e("EditActivity: ", "id: " + mId);
        if (isPhoto) {
            textView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            Glide.with(imageView.getContext()).load(mPhotoUrl).into(imageView);
        } else {
            textView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            textView.setText(mText);
        }

        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        chatDeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), mId, Toast.LENGTH_SHORT).show();
                deleteChat(mId);
            }
        });

        chatUpdateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), mId, Toast.LENGTH_SHORT).show();
                updateChat(mId, textView.getText().toString(), senderView.getText().toString(), mPhotoUrl);
            }
        });
    }

    private void updateChat(String id, String text, String sender, String url) {
        FriendlyMessage friendlyMessage = new FriendlyMessage(id, text, sender, url);
        mDatabaseReference.child("DB").child(CURRENT_CHAT_ROOM_ID).child("messages").child(id).setValue(friendlyMessage).
                addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(EditorActivity.this, "Chat Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void deleteChat(String id) {
        mDatabaseReference.child("DB").child(CURRENT_CHAT_ROOM_ID).child("messages").child(id).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(EditorActivity.this, "Chat Deleted", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

}