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

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import androidx.appcompat.app.AlertDialog;
import pp.facerecognizer.env.BorderedText;
import pp.facerecognizer.env.FileUtils;
import pp.facerecognizer.env.ImageUtils;
import pp.facerecognizer.env.Logger;
import pp.facerecognizer.tracking.MultiBoxTracker;

/**
* An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
* objects.
*/
public class RegisterActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final int FACE_SIZE = 160;
    private static final int CROP_SIZE = 300;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private static final float TEXT_SIZE_DIP = 10;

    private Integer sensorOrientation;

    private Classifier classifier;

    private Bitmap rgbFrameBitmap = null;

    private boolean savingFile = false;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;


    private ArrayList<Uri> trainPhotoUris = new ArrayList<>();


    private Snackbar initSnackbar;
    private Snackbar trainSnackbar;
    private Snackbar saveSnackbar;

    private FloatingActionButton button;
    private MaterialButton completeButton;

    private boolean initialized = false;
    private boolean training = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FrameLayout container = findViewById(R.id.container);
        initSnackbar = Snackbar.make(container, "Initializing...", Snackbar.LENGTH_INDEFINITE);
        saveSnackbar = Snackbar.make(container, "Saving...", Snackbar.LENGTH_INDEFINITE);
        trainSnackbar = Snackbar.make(container, "Training data...", Snackbar.LENGTH_INDEFINITE);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edittext, null);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        AlertDialog editDialog = new AlertDialog.Builder(RegisterActivity.this)
                .setTitle(R.string.enter_name)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                    int idx = classifier.addPerson(editText.getText().toString());
                    startTraining(idx - 1);
                })
                .create();

        completeButton = findViewById(R.id.complete_button);
        completeButton.setOnClickListener(view ->
                new AlertDialog.Builder(RegisterActivity.this)
                        .setTitle(getString(R.string.select_name))
                        .setItems(classifier.getClassNames(), (dialogInterface, i) -> {
                            if (i == 0) {
                                editDialog.show();
                            } else {
                                startTraining(i - 1);
                            }
                        })
                        .show());
        button = findViewById(R.id.add_button);
        button.setOnClickListener(view -> {
            savingFile = true;
            runInBackground(
                    () -> {
                        LOGGER.i("Saving image...");
                        saveSnackbar.show();
                        File imageFile = null;
                        try {
                            imageFile = createImageFile();
                            FileOutputStream out = new FileOutputStream(imageFile);
                            Bitmap rotated = RotateBitmap(rgbFrameBitmap, sensorOrientation);
                            rotated.compress(Bitmap.CompressFormat.PNG, 90, out);
                            out.flush();
                            out.close();
                            trainPhotoUris.add(Uri.fromFile(imageFile));
                        } catch (IOException e) {
                            LOGGER.e(e, e.getMessage());
                            e.printStackTrace();
                        }
                        saveSnackbar.dismiss();
                        LOGGER.i("Saving image complete..." + imageFile.getAbsolutePath());
                        savingFile = false;
                    });
        });
    }
    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        if (!initialized)
            new Thread(this::init).start();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        CROP_SIZE, CROP_SIZE,
                        sensorOrientation, false);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
    }


    void init() {
        runOnUiThread(()-> initSnackbar.show());
        File dir = new File(FileUtils.ROOT);

        if (!dir.isDirectory()) {
            if (dir.exists()) dir.delete();
            dir.mkdirs();

            AssetManager mgr = getAssets();
            FileUtils.copyAsset(mgr, FileUtils.DATA_FILE);
            FileUtils.copyAsset(mgr, FileUtils.MODEL_FILE);
            FileUtils.copyAsset(mgr, FileUtils.LABEL_FILE);
        }

        try {
            classifier = Classifier.getInstance(getAssets(), FACE_SIZE, FACE_SIZE);
        } catch (Exception e) {
            LOGGER.e("Exception initializing classifier!", e);
            finish();
        }

        runOnUiThread(()-> initSnackbar.dismiss());
        initialized = true;
    }

    @Override
    protected void processImage() {
        // No mutex needed as this method is not reentrant.
        if (savingFile || !initialized || training) {
            readyForNextImage();
            return;
        }

        readyForNextImage();
        runInBackground(
                () -> {
                    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
                });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    public void startTraining(int index) {
        trainSnackbar.show();
        completeButton.setEnabled(false);
        button.setEnabled(false);
        training = true;

        new Thread(() -> {
            try {
                classifier.updateData(index, getContentResolver(), trainPhotoUris);
            } catch (Exception e) {
                LOGGER.e(e, "Exception!");
            } finally {
                training = false;
            }
            runOnUiThread(() -> {
                trainSnackbar.dismiss();
                completeButton.setEnabled(false);
                button.setEnabled(true);
            });
        }).start();

    }
}
