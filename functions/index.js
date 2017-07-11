/*
 Copyright 2017 Google

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);
const emojify = require('./emojify');
const img2emo = require('./img2emo');
const gcs = require('@google-cloud/storage')();
const Vision = require('@google-cloud/vision');

function ifEnabled(feature) {
  console.log("Checking if feature '"+feature+"' is enabled");
  return new Promise((resolve, reject) => {
    admin.database().ref("/config/features")
      .child(feature)
      .once('value')
      .then(snapshot => {
        if (snapshot.val()) {
          resolve(snapshot.val());
        } else {
          reject("No value or 'falsy' value found");
        }
      });
  });
}

exports
.emojifyTitle = functions.database.ref('/stories/{storyId}')
.onWrite(event => {
  return ifEnabled('emojifyTitle').then(() => {

    // Skip if we've already emoji'd the title.
    if (event.data.child('emoji').exists()) {
      return;
    }

    // Also skip if there's no title
    if (!event.data.child('title').exists()) {
      return;
    }

    // Get the title from the *gasp* snapshot
    var title = event.data.val().title;

    // Super secret sauce emojify it
    var emojiTitle = emojify.emojifyTitle(title);

    // Set the emoji title in the database.
    return admin.database()
      .ref('stories')
      .child(event.params.storyId)
      .update({
        emoji: emojiTitle
      });
  });
});

const visionClient = Vision({
  projectId: 'YOUR_FIREBASE_PROJECT_ID'
});
const bucket = 'YOUR_FIREBASE_PROJECT_ID.appspot.com';

function emojifyStory(event) {
  return ifEnabled('emojifyStory').then(() => {

    // Get the file
    const filePath = event.data.val().filePath;
    const file = gcs.bucket(bucket).file(filePath);


    // Use the Vision API to detect labels
    return visionClient.detectLabels(file)
      .then(data => {
        // data returns [labels, apiResponse]
        // and we only care about the labels
        return data[0];
      }).then(labels => {
        // Super glittery emojification happens here
        const emoji = emojify.emojifyLabels(labels);

        // Set the resulting labels and emojis into the database
        return admin.database()
          .ref('emojis')
          .child(event.params.storyId)
          .set({
            labels: labels,
            emoji: emoji
          });
      });
  });
}

exports
.emojifyStory = functions.database.ref('/stories/{storyId}')
.onWrite(emojifyStory);

exports
.emojifyPublicUpload = functions.database.ref('/public/{storyId}')
.onWrite(emojifyStory);

exports
.imageEmojify = functions.database.ref('/images/{pushId}')
.onWrite(event => {
    var filePath = event.data.val().fullPath;
    console.log(`Emojifying node ${event.params.pushId}, file ${filePath}...`)
    return img2emo.emojify('YOUR_FIREBASE_PROJECT_ID', "YOUR_FIREBASE_PROJECT_ID.appspot.com", filePath).then((emoji) => {
        console.log(`Setting ${emoji} to path /emojis/${event.params.pushId}`)
        return admin.database()
            .ref('emojis')
            .child(event.params.pushId)
            .update({
                characters: emoji.join(''),
                timestamp: admin.database.ServerValue.TIMESTAMP
            });
    }).catch((error) => {
        console.log(error);
    });
});


