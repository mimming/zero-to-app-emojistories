/*
* Copyright 2017 Google Inc.
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package com.google.emojistories;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A stripped down version of MainActivityComplete that only displays emoji (ignores images & titles)
 */
public class EmojiActivity extends AppCompatActivity {

    public static final short RC_SIGN_IN = 42;
    public static final short RC_IMAGE_CAPTURE = 1337;


    private FirebaseStorage mStorage;
    private FirebaseDatabase mDatabase;
    private FirebaseAuth mAuth;
    private StoryAdapter mStoryAdapter;
    private GridView mGridView;

    private String mStoryTextString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init the grid layout and adapter
        mGridView = (GridView) findViewById(R.id.storyGrid);
        mStoryAdapter = new StoryAdapter(this);
        mGridView.setAdapter(mStoryAdapter);

        // Handle photo button clicks
        Button photoButton = (Button) findViewById(R.id.photoButton);
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText storyText = (EditText) findViewById(R.id.storyText);

                // Copy the text because we're about to clear it
                mStoryTextString = storyText.getText().toString();

                // Clear the UI
                storyText.setText("");

                takeStoryPhoto();
            }
        });

        SignInButton signInButton = (SignInButton)findViewById(R.id.sign_in_button);
        Button signOutButton = (Button)findViewById(R.id.sign_out_button);

        // Sign out button
        signOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AuthUI.getInstance().signOut(EmojiActivity.this);
            }
        });


        // Start the awesome live coding here!!11
        // Step 0 - Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mStorage = FirebaseStorage.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Auth step 1 - Use FirebaseUI to set up Firebase Authentication
        final Intent signinIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setProviders(Arrays.asList(
                        new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                        new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                .build();


        // Auth step 2 - Start FirebaseUI
        signInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(signinIntent, RC_SIGN_IN);
            }
        });

        // Auth step 3 - Listen to authentication state changes
        //               show appropriate UI
        mAuth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() != null) {
                    displayUserLoggedInState();
                } else {
                    displayUserLoggedOutState();
                }
            }
        });
    }

    private void uploadStory(byte[] storyPhotoBytes) {
        // Storage step 1 - Generate a random name for the image
        //                  Get a Storage reference.
        String fileName = generateName();
        StorageReference storageRef = mStorage.getReference()
                .child(mAuth.getCurrentUser().getUid())
                .child(fileName);

        // Storage step 2 - Upload image
        storageRef.putBytes(storyPhotoBytes)
                // Storage step 3 - Handle upload event
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    public void onSuccess(UploadTask.TaskSnapshot snapshot) {
                        // Display the story (replaced by listener stuffs)
                        displayStory(new Story(snapshot.getDownloadUrl().toString(),
                                 mStoryTextString));

                        // Database step 1 - Get a Database reference
                        DatabaseReference dbRef = mDatabase.getReference("stories").push();

                        // Database step 2 - Write the data to the database
                        dbRef.setValue(new Story(
                                snapshot.getMetadata().getPath(),
                                snapshot.getDownloadUrl().toString(),
                                mStoryTextString,
                                mAuth.getCurrentUser().getUid()
                        ));

                        toast("Photo uploaded successfully!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    public void onFailure(@NonNull Exception e) {
                        toast("Failed to upload photo :(");
                    }
                });
    }

    private void fetchRecentStories() {
        DatabaseReference database = mDatabase.getReference();

        // Database step 3 - Listen from the latest 10 stories onwards
        //                   Display them
        database.child("emojis")
                .limitToLast(10)
                .addChildEventListener(new ChildEventListener() {
                    public void onChildAdded(DataSnapshot snapshot, String s) {
                        Map emojiMap = snapshot.getValue(Map.class);
                        Story story = new Story();
                        story.setEmoji((String)emojiMap.get("characters"));

                        displayStory(story, snapshot.getKey());
                    }

                    // Database step 4 - Listen to whenever a story changes
                    public void onChildChanged(DataSnapshot snapshot, String s) {
                        Map emojiMap = snapshot.getValue(Map.class);
                        Story story = new Story();
                        story.setEmoji((String)emojiMap.get("characters"));

                        displayStory(story, snapshot.getKey());
                    }

                    public void onChildRemoved(DataSnapshot snapshot) { }
                    public void onChildMoved(DataSnapshot snapshot, String s) { }
                    public void onCancelled(DatabaseError error) { }
                });
    }

    private void displayStory(Story story) {
        displayStory(story, "");
    }

    private void displayStory(Story story, final String storyId) {
        final StorageReference storageRef = mStorage.getReference();

        if (story.getEmoji() != null && story.getEmoji().length() > 0) {
            // prioritize emoji
            String[] emojiArray = story.getEmoji().split(" ");

            mStoryAdapter.addEmoji(Arrays.asList(emojiArray), storyId);

        }

        // Refresh the screen
        ViewGroup rootLayout = (ViewGroup) findViewById(R.id.rootLayout);
        rootLayout.invalidate();
    }


    private void toast(String message) {
        Toast.makeText(EmojiActivity.this, message,
                Toast.LENGTH_SHORT).show();
    }

    private void takeStoryPhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, RC_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            ByteArrayOutputStream compressinatorSteam = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, compressinatorSteam);
            byte[] storyPhotoBytes = compressinatorSteam.toByteArray();

            toast("Great photo!  Starting upload.");
            uploadStory(storyPhotoBytes);
        }
    }

    public void displayUserLoggedInState() {
        // Toggle auth ui (sign out shows, sign in hides)
        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);

        // Show the awesome stories!!!1!
        fetchRecentStories();
    }

    public void displayUserLoggedOutState() {
        // Toggle auth ui (sign in shows, sign out hides)
        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
    }

    private String generateName() {
        return "android-" + (int) (Math.random() * 1000000) + ".jpg";
    }
}
