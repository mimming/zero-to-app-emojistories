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
const img2emo = require('./emojify');
const gcs = require('@google-cloud/storage')();
const Vision = require('@google-cloud/vision');

exports.emojifyTitle = functions.database.ref('/stories/{storyId}')
.onWrite(event => {

  // Skip if we've already emoji'd the title.
  if (event.data.child('emoji').exists()) {
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

const visionClient = Vision({
  projectId: 'YOUR_FIREBASE_PROJECT_ID'
});
const bucket = 'YOUR_FIREBASE_PROJECT_ID.appspot.com';

exports.emojifyStory = functions.database.ref('/stories/{storyId}')
.onWrite(event => {

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
