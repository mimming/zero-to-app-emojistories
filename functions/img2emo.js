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

const gcs = require('@google-cloud/storage')();
const Vision = require('@google-cloud/vision');
const emoji = require('emoji');


// Takes in string
exports.emojifyTitle = function(title) {
  return exports.emojifyWords(title.split(" "));
};

// Takes in array of strings 
exports.emojifyWords = function(words) {
  var results = [];
  words.forEach((word) => {
    Object.keys(emoji.EMOJI_MAP).forEach((em) => {
      if (emoji.EMOJI_MAP[em][1].indexOf(word) >= 0) {
        results.push(em);
      }
    });
  });
  return results;
};

exports.emojify = function(projectId, bucket, fileName, options) {
    options = options || {};
    return new Promise(function(resolve, reject) {
        const visionClient = Vision({
            projectId: projectId
        });

        const file = gcs.bucket(bucket).file(fileName);

        visionClient.detectLabels( file )
        .then((results) => {
            var result = [];
            const labels = results[0];
            labels.forEach((label) => {
                if (options.debug) result.push(label+": ");
                Object.keys(emoji.EMOJI_MAP).forEach((em) => {
                    if (emoji.EMOJI_MAP[em][1].indexOf(label) >= 0) {                        
                        result.push(em);
                    }
                });
            });
            console.log(result);
            resolve(result);

        })
        .catch((error) => {
            console.error(error);
            reject(error);
        });
    });
}

exports.base64 = function(projectId, base64) {
    return new Promise(function(resolve, reject) {
        const visionClient = Vision({
            projectId: projectId
        });

        visionClient.detectLabels( base64 )
        .then((results) => {
            var result = [];
            const labels = results[0];
            labels.forEach((label) => {
                result.push(label+": ");
                Object.keys(emoji.EMOJI_MAP).forEach((em) => {
                    if (emoji.EMOJI_MAP[em][1].indexOf(label) >= 0) {                        
                        result.push(em);
                    }
                });
            });
            console.log(result);
            resolve(result);

        })
        .catch((error) => {
            console.error(error);
            reject(error);
        });
    });
}

// by passing the image URL and gcloud access token, you can test this module
if (process.argv.length > 2) {
  process.argv.forEach(a => console.log(a));

