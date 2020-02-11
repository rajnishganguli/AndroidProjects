package com.google.firebase.udacity.friendlychat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final boolean DEVELOPER_MODE_ENABLED = true;

    private static final String TAG = "MainActivity";
    private static final String ANONYMOUS = "anonymous";
    private static final int DEFAULT_MSG_LENGTH_LIMIT = 140;
    private static final String MSG_LENGTH_KEY = "friendly_msg_length";

    // Choose an arbitrary request code value
    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;
    private static final int RC_CHAT_ROOM_PICKER = 3;

    // public as it is accessible by EditorActivity
    public static String CURRENT_CHAT_ROOM_ID = "chat_room_1";

    private static int CHAT_ROOM_COUNT = 0;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    private ChildEventListener mChildEventListenerForMessages;
    private ValueEventListener mValueEventListenerForChatRoomsCount;

    // Firebase Authentication: All in one (Auth0 + Email)
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    // Firebase realtime database
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private DatabaseReference mChatRoomsDatabaseReference;

    // Firebase storage to store media files mainly pictures.
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotoStorageReference;

    // Firebase remote configue to modify the app parameters remotely
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Using shared preferences to get the last chat room id.
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_preferences_file_for_friendly_chat), Context.MODE_PRIVATE);
        String defaultValue = CURRENT_CHAT_ROOM_ID;
        CURRENT_CHAT_ROOM_ID = sharedPref.getString(getString(R.string.current_chat_room_id), defaultValue);

        mUsername = ANONYMOUS;

        // Initialize references to views
        mProgressBar = findViewById(R.id.progressBar);
        mMessageListView = findViewById(R.id.messageListView);
        mPhotoPickerButton = findViewById(R.id.photoPickerButton);
        mMessageEditText = findViewById(R.id.messageEditText);
        mSendButton = findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);
        setItemClickListenerOnListView();

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        if(DEVELOPER_MODE_ENABLED){
            Toast.makeText(MainActivity.this, "OnCreate", Toast.LENGTH_SHORT).show();
        }

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("DB").child(CURRENT_CHAT_ROOM_ID).child("messages");
        mChatRoomsDatabaseReference = mFirebaseDatabase.getReference().child("DB");
        mChatPhotoStorageReference = mFirebaseStorage.getReference().child("chat_photos");

        mAuthStateListener = createAuthStateListener();

        setupPhotoPicker();
        setupEditTextMessageBox();
        setupSendButton();

        fireRemoteConfig();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);

        Toast.makeText(this, "onPause", Toast.LENGTH_SHORT).show();
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_preferences_file_for_friendly_chat), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.current_chat_room_id), CURRENT_CHAT_ROOM_ID);
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mAuthStateListener != null){
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mMessageAdapter.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){

            case R.id.available_chat_rooms_menu:
                showAvailableChatRooms();
                return true;

            case R.id.create_chat_room_menu:
                startNewChatRoom();
                return true;

            case R.id.sign_out_menu:
                // Sign out
                AuthUI.getInstance().signOut(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /***********************************************************************************
     *          Receiving the result from another activity and acting accordingly
     ***********************************************************************************/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_SIGN_IN:
                handleSignInResultFromActivity(resultCode, data);
                break;

            case RC_PHOTO_PICKER:
                handlePhotoPickerResultFromActivity(resultCode, data);
                break;

            case RC_CHAT_ROOM_PICKER:
                handleChatRoomPickerResultFromActivity(resultCode, data);
                break;
        }
    }

    private void handleSignInResultFromActivity(int resultCode, Intent data){
        if (resultCode == RESULT_OK) {
            // Successfully Signed In
            Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
        } else if (resultCode == RESULT_CANCELED) {
            // Sign in failed.
            // If response is null the user canceled the sign-in flow using the back button.
            Toast.makeText(this, "Sign in Cancelled!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handlePhotoPickerResultFromActivity(int resultCode, Intent data){
        if (resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();

            // Get a reference to store file at chat_photos/<FILE_NAME>
            final StorageReference photoRef =
                    mChatPhotoStorageReference.child(selectedImageUri.getLastPathSegment());

            // Upload file to firebase storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri url) {
                                    Uri downloadUrl = url;
                                    String id=mMessagesDatabaseReference.push().getKey();
                                    FriendlyMessage friendlyMessage =
                                            new FriendlyMessage(id, null, mUsername, downloadUrl.toString());
                                    mMessagesDatabaseReference.child(id).setValue(friendlyMessage);

                                    //Do what you want with the url
                                    Toast.makeText(MainActivity.this, "Upload Done", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
        }
    }

    private  void handleChatRoomPickerResultFromActivity(int resultCode, Intent data){

        if (resultCode == RESULT_OK) {
            detachDatabaseReadListener();
            mMessageAdapter.clear();
            CURRENT_CHAT_ROOM_ID = data.getStringExtra("TAPPED_CHAT_ROOM");
            mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("DB")
                    .child(CURRENT_CHAT_ROOM_ID).child("messages");
            attachDatabaseReadListener();
        } else if (resultCode == RESULT_CANCELED) {
            // Pressed the back button without selecting the chat room
            Toast.makeText(this, "Photo-picked cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    /*******************************************************************************
     Setting the Chat: PhotoPicker, EditText, SendButton
     *******************************************************************************/

    private void setupPhotoPicker(){
        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Fire an intent to show an image picker
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(
                        Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });
    }

    private void setupEditTextMessageBox(){
        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        mMessageEditText.setFilters(
                new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});
    }

    private void setupSendButton(){
        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id=mMessagesDatabaseReference.push().getKey();
                FriendlyMessage friendlyMessage =
                        new FriendlyMessage(id, mMessageEditText.getText().toString(), mUsername, null);

                mMessagesDatabaseReference.child(id).setValue(friendlyMessage);

                mMessageEditText.setText("");
            }
        });
    }

    /*******************************************************************************
     Setting AuthStateListener
     *******************************************************************************/

    private FirebaseAuth.AuthStateListener createAuthStateListener(){
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    // User is logged in
                    onSignedInInitialize(user.getDisplayName());
                    Toast.makeText(MainActivity.this, "You are signed in!", Toast.LENGTH_SHORT).show();
                } else {
                    // User is not logged in: So launch the sign in flow
                    onSignedOutCleanup();
                    Toast.makeText(MainActivity.this, "Not Signed in!", Toast.LENGTH_SHORT).show();

                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build());

                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
    }

    private void onSignedInInitialize(String username){
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void attachDatabaseReadListener(){
        // attach the listener only once.
        if(mValueEventListenerForChatRoomsCount == null){
            mValueEventListenerForChatRoomsCount = createValueEventListenerForChatRoomsCount();
            mChatRoomsDatabaseReference.addValueEventListener(mValueEventListenerForChatRoomsCount);
        }
        if(mChildEventListenerForMessages == null){
            mChildEventListenerForMessages = createChildEventListenerForMessages();
            mMessagesDatabaseReference.addChildEventListener(mChildEventListenerForMessages);
        }
    }

    private ChildEventListener createChildEventListenerForMessages(){
        return new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                mMessageAdapter.add(friendlyMessage);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {   }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "createChildEventListenerForMessages: Failed to read value.", databaseError.toException());
            }
        };
    }

    private ValueEventListener createValueEventListenerForChatRoomsCount(){
        return new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                CHAT_ROOM_COUNT =  (int) dataSnapshot.getChildrenCount();
                Log.d(TAG, "createValueEventListenerForChatRoomsCount CHAT_ROOM_COUNT: " + CHAT_ROOM_COUNT);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        };
    }

    private void onSignedOutCleanup(){
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReadListener();
    }

    private void detachDatabaseReadListener(){
        // Detach the listener only if it is attached
        if(mChildEventListenerForMessages != null){
            mMessagesDatabaseReference.removeEventListener(mChildEventListenerForMessages);
            mChildEventListenerForMessages = null;
        }
        if(mValueEventListenerForChatRoomsCount != null){
            mChatRoomsDatabaseReference.removeEventListener(mValueEventListenerForChatRoomsCount);
            mValueEventListenerForChatRoomsCount = null;
        }
    }

    /*****************************************************************
     Working with Remote Config
     *****************************************************************/

    private void fireRemoteConfig(){
        long cacheExpiration = DEVELOPER_MODE_ENABLED ? 0L : 3600L;

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().
                setMinimumFetchIntervalInSeconds(cacheExpiration).build();

        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(MSG_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaultsAsync(defaultConfigMap);

        fetchConfig();
    }

    // Fetch the config to determine the allowed length of messages.
    private void fetchConfig() {
        mFirebaseRemoteConfig.fetch()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available
                        // via FirebaseRemoteConfig get<type> calls, e.g., getLong, getString.
                        mFirebaseRemoteConfig.activate();

                        // Update the EditText length limit with the
                        // newly retrieved values from Remote Config.
                        applyRetrievedLengthLimit();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "An error occurred when fetching the config: ", e);

                        // Update the EditText length limit with the default values
                        applyRetrievedLengthLimit();
                    }
                });
    }

    // Apply retrieved length limit to edit text field.
    // This result may be fresh from the server or it may be from cached values.
    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(MSG_LENGTH_KEY);

        mMessageEditText.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(friendly_msg_length.intValue())});
    }

    /***********************************************************************************************
     *                  Setting Item Click Listener on the Listview for editing/deleting
     **********************************************************************************************/

    private void setItemClickListenerOnListView(){
        mMessageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                FriendlyMessage friendlyMessage = mMessageAdapter.getItem(position);
                // Only start the Editor activity if the original author of the chat is same as current logged in user.
                if(friendlyMessage.getName().equals(mUsername)){

                    Intent intent = new Intent(MainActivity.this, EditorActivity.class);

                    intent.putExtra("text", friendlyMessage.getText());
                    intent.putExtra("name", friendlyMessage.getName());
                    intent.putExtra("photoUrl", friendlyMessage.getPhotoUrl());
                    intent.putExtra("id", friendlyMessage.getId());

                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Can't edit other's messages", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /***********************************************************************************************
     * Creating a new chat room and showing all the available chat rooms
     **********************************************************************************************/

    private void showAvailableChatRooms(){
        // Query the database to get all the chat rooms with their message count
        // Create a new listview in a new activity and show them there
        // set an onItemClickListener to return the result with tapped chat_room id.
        Intent allChatRoomsIntent = new Intent(MainActivity.this, AllAvailableChatRoomsActivity.class);
        startActivityForResult(allChatRoomsIntent, RC_CHAT_ROOM_PICKER);
    }

    private void startNewChatRoom(){
        detachDatabaseReadListener();
        mMessageAdapter.clear();
        CURRENT_CHAT_ROOM_ID = "chat_room_" + Integer.toString(CHAT_ROOM_COUNT + 1);
        Log.d(TAG, "CURRENT_CHAT_ROOM_ID: " + CURRENT_CHAT_ROOM_ID);
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("DB")
                .child(CURRENT_CHAT_ROOM_ID).child("messages");
        attachDatabaseReadListener();
    }

}
