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

const emoji = require('emoji');

// Takes in string
exports.emojifyTitle = function(title) {
  return exports.emojifyLabels(title.split(" "));
};

// Takes in array of strings
exports.emojifyLabels = function(labels) {
  var results = [];
  labels.forEach((label) => {
    Object.keys(emoji.EMOJI_MAP).forEach((em) => {
      if (emoji.EMOJI_MAP[em][1].indexOf(label) >= 0) {
        results.push(em);
      }
    });
  });
  return results.join(" ");
};
