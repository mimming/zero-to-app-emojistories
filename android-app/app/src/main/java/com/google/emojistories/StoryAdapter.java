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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

//TODO: handle on change properly
public class StoryAdapter extends BaseAdapter {
    private class TaggedView {
        TaggedView(View view, String key) {
            this.view = view;
            this.key = key;
        }
        View view;
        String key;
    }
    private List<TaggedView> gridContents = new ArrayList<>();
    private Context myContext;

    public StoryAdapter(Context context) {
        myContext = context;
    }

    @Override
    public int getCount() {
        return gridContents.size();
    }

    @Override
    public Object getItem(int position) {
        return gridContents.get(gridContents.size() - position - 1);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return gridContents.get(gridContents.size() - position - 1).view;
    }

    public void addMessage(String message, String storyId) {
        storyId += "-message";

        removeStuffWithThisKey(storyId);

        TextView textThing = new TextView(myContext);
        textThing.setText(message);
        textThing.setTextSize(25);

        int gridThingPixels = (int) myContext.getResources().getDimension(R.dimen.grid_thing);
        textThing.setHeight(gridThingPixels);

        gridContents.add(new TaggedView(textThing, storyId));

        this.notifyDataSetChanged();
    }

    public void addImage(StorageReference storageRef, String filePath, String storyId) {
        storyId += "-photo";

        removeStuffWithThisKey(storyId);

        final ImageView imageThing = new ImageView(myContext);

        int gridThingPixels = (int) myContext.getResources().getDimension(R.dimen.grid_thing);
        imageThing.setMaxHeight(gridThingPixels);
        imageThing.setMaxWidth(gridThingPixels);

        gridContents.add(new TaggedView(imageThing, storyId));

        this.notifyDataSetChanged();


        // and a photo (up to 10mb)
        Task<byte[]> imageDownloadTask = storageRef.child(filePath).getBytes(100 * 1024 * 1024);
        imageDownloadTask.addOnCompleteListener(new OnCompleteListener<byte[]>() {
            @Override
            public void onComplete(@NonNull Task<byte[]> task) {
                byte[] imageBytes = task.getResult();

                Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                imageThing.setImageBitmap(imageBitmap);

                StoryAdapter.this.notifyDataSetChanged();
            }
        });
    }

    public void addImage(byte[] imageBytes, String storyId) {
        storyId += "-photo";

        removeStuffWithThisKey(storyId);

        ImageView imageThing = new ImageView(myContext);

        int gridThingPixels = (int) myContext.getResources().getDimension(R.dimen.grid_thing);
        imageThing.setMaxHeight(gridThingPixels);
        imageThing.setMaxWidth(gridThingPixels);

        Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        imageThing.setImageBitmap(imageBitmap);

        gridContents.add(new TaggedView(imageThing, storyId));

        this.notifyDataSetChanged();
    }

    public void addEmoji(List<String> emojis, String storyId) {
        String storyIdMessage = storyId + "-message";
        storyId += "-emoji";

        removeStuffWithThisKey(storyIdMessage);
        removeStuffWithThisKey(storyId);

        int gridThingPixels = (int) myContext.getResources().getDimension(R.dimen.grid_thing);

        for (String emoji : emojis) {
            TextView textThing = new TextView(myContext);
            textThing.setText(emoji);
            textThing.setTextSize(60);
            textThing.setHeight(gridThingPixels);

            gridContents.add(new TaggedView(textThing, storyId));
        }

        this.notifyDataSetChanged();
    }

    private void removeStuffWithThisKey(String key) {
        Set<TaggedView> toBeRemovedSet = new HashSet<>();
        for(TaggedView taggedView : gridContents) {
            if(taggedView.key.equals(key)) {
                toBeRemovedSet.add(taggedView);
            }
        }

        for(TaggedView taggedView : toBeRemovedSet) {
            gridContents.remove(taggedView);
        }
    }
}
