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
package com.google.emojistories

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.Toast

import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.SignInButton
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

import java.io.ByteArrayOutputStream
import java.util.Arrays

class MainActivityKotlin : AppCompatActivity() {


    private var mStorage: FirebaseStorage? = null
    private var mDatabase: FirebaseDatabase? = null
    private var mAuth: FirebaseAuth? = null
    private var mStoryAdapter: StoryAdapter? = null
    private var mGridView: GridView? = null

    private var mStoryTextString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init the grid layout and adapter
        mGridView = findViewById(R.id.storyGrid) as GridView
        mStoryAdapter = StoryAdapter(this)
        mGridView!!.adapter = mStoryAdapter

        // Handle photo button clicks
        val photoButton = findViewById(R.id.photoButton) as Button
        photoButton.setOnClickListener {
            val storyText = findViewById(R.id.storyText) as EditText

            // Copy the text because we're about to clear it
            mStoryTextString = storyText.text.toString()

            // Clear the UI
            storyText.setText("")

            takeStoryPhoto()
        }

        val signInButton = findViewById(R.id.sign_in_button) as SignInButton
        val signOutButton = findViewById(R.id.sign_out_button) as Button

        // Sign out button
        signOutButton.setOnClickListener { AuthUI.getInstance().signOut(this@MainActivityKotlin) }


        // Start the awesome live coding here!!11

    }

    private fun uploadStory(storyPhotoBytes: ByteArray) {

    }

    private fun fetchRecentStories() {
        val database = mDatabase!!.reference

    }

    private fun displayStory(story: Story, storyId: String = "") {
        val storageRef = mStorage!!.reference

        if (story.emoji != null && story.emoji.length > 0) {
            // prioritize emoji
            val emojiArray = story.emoji.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            mStoryAdapter!!.addEmoji(Arrays.asList(*emojiArray), storyId)

            // and a photo (up to 10mb)
            if (story.filePath != null && story.filePath.length > 0) {

                mStoryAdapter!!.addImage(storageRef, story.filePath, storyId)
            }

        } else if (story.title != null && story.title.length > 0) {

            // and a photo (up to 10mb)
            if (story.filePath != null && story.filePath.length > 0) {
                val imageDownloadTask = storageRef.child(story.filePath).getBytes((100 * 1024 * 1024).toLong())
                imageDownloadTask.addOnCompleteListener { task -> mStoryAdapter!!.addImage(task.result, storyId) }
            }

            // no emoji?  Display title
            mStoryAdapter!!.addMessage(story.title, storyId)
        }

        // Refresh the screen
        val rootLayout = findViewById(R.id.rootLayout) as ViewGroup
        rootLayout.invalidate()
    }


    private fun toast(message: String) {
        Toast.makeText(this@MainActivityKotlin, message,
                Toast.LENGTH_SHORT).show()
    }

    private fun takeStoryPhoto() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, RC_IMAGE_CAPTURE.toInt())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == RC_IMAGE_CAPTURE.toInt() && resultCode == Activity.RESULT_OK) {
            val extras = data.extras
            val imageBitmap = extras.get("data") as Bitmap

            val compressinatorSteam = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, compressinatorSteam)
            val storyPhotoBytes = compressinatorSteam.toByteArray()

            toast("Great photo!  Starting upload.")
            uploadStory(storyPhotoBytes)
        }
    }

    fun displayUserLoggedInState() {
        // Toggle auth ui (sign out shows, sign in hides)
        findViewById(R.id.sign_in_button).visibility = View.GONE
        findViewById(R.id.sign_out_button).visibility = View.VISIBLE

        // Show the awesome stories!!!1!
        fetchRecentStories()
    }

    fun displayUserLoggedOutState() {
        // Toggle auth ui (sign in shows, sign out hides)
        findViewById(R.id.sign_out_button).visibility = View.GONE
        findViewById(R.id.sign_in_button).visibility = View.VISIBLE
    }

    private fun generateName(): String {
        return "android-" + (Math.random() * 1000000).toInt() + ".jpg"
    }

    companion object {

        val RC_SIGN_IN: Short = 42
        val RC_IMAGE_CAPTURE: Short = 1337
    }
}
