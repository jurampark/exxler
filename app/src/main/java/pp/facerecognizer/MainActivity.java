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

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.ArrayAdapter;

import android.widget.ListView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.android.material.textfield.TextInputEditText;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import pp.facerecognizer.env.Logger;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class MainActivity extends AppCompatActivity {

  private static final Logger LOGGER = new Logger();
  private static int REGISTER_MODE = 1;
  private static int Add_MORE_MODE = 1;
  private static int RECOGNITION_REQUEST_CODE = 1;

  private FloatingActionButton button;
  private List<String> users = new ArrayList<>();
  private ListView userListView;
  private TextInputEditText usernameInput;
  private ArrayAdapter<String> userListViewAdapter;

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    LOGGER.i("onActivityResult", data.toString());
    if (requestCode == RECOGNITION_REQUEST_CODE) {
      if (data.hasExtra("response") &&
          data.getIntExtra("response", 0) == android.R.string.no) {
        Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
        // TODO(jurampark): add mode depending on entrance flow
        intent.putExtra("Mode", REGISTER_MODE);
        // TODO(jurampark): pass label(ldap) after selection
        intent.putExtra("Label", "jurampark");
        startActivity(intent);

        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
      }
    }
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_main);

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
        startActivityForResult(new Intent(getApplicationContext(), RecognitionActivity.class),
            RECOGNITION_REQUEST_CODE));

  }

}
