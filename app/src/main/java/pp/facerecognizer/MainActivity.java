/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pp.facerecognizer;

import android.content.ClipData;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;

import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.google.android.material.textfield.TextInputEditText;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import androidx.appcompat.app.AlertDialog;
import pp.facerecognizer.Classifier.Recognition;
import pp.facerecognizer.env.BorderedText;
import pp.facerecognizer.env.FileUtils;
import pp.facerecognizer.env.ImageUtils;
import pp.facerecognizer.env.Logger;
import pp.facerecognizer.tracking.MultiBoxTracker;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class MainActivity extends AppCompatActivity {

  private FloatingActionButton button;
  private List<String> users = new ArrayList<>();
  private ListView userListView;
  private TextInputEditText usernameInput;
  private ArrayAdapter<String> userListViewAdapter;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.actiivty_main);

    userListView = findViewById(R.id.userListView);
    usernameInput = findViewById(R.id.usernameInput);

    try {
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(getAssets().open("users")));

      // do reading, usually loop until end of file reading
      String mLine;
      while ((mLine = reader.readLine()) != null) {
        users.add(mLine);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    userListViewAdapter = new ArrayAdapter<String>(this,
        R.layout.user_list_item, R.id.username, users);
    userListView.setAdapter(userListViewAdapter);

    usernameInput.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        userListViewAdapter.getFilter().filter(charSequence);
      }

      @Override
      public void afterTextChanged(Editable editable) {

      }
    });

    button = findViewById(R.id.camera_button);
    button.setOnClickListener(view ->
        startActivity(new Intent(getApplicationContext(), RecognitionActivity.class)));

  }

}
