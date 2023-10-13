package com.aadim.trafficdigitalvehicle;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.aadim.trafficdigitalvehicle.databinding.ActivityImageCompareBinding;
import com.aadim.trafficdigitalvehicle.util.SimilarityClassifier;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ImageCompareActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityImageCompareBinding binding;

    private static final int OUTPUT_SIZE = 192;
    private static final int INPUT_SIZE = 112;
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    private static final int CAM_FACE = CameraSelector.LENS_FACING_BACK;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private FaceDetector detector;
    private ProcessCameraProvider processCameraProvider;

    Interpreter tfLite;

    String modelFile = "mobile_face_net.tflite";

    boolean flipX = false;
    boolean startRecognition = false;

    float distance = 1.0f;

    float[][] embeedings;
    int[] intValues;

    private int faceRecognitionCount = 0;

    private HashMap<String, SimilarityClassifier.Recognition> registered = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageCompareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initRegister();
        initTensorFlowModel();
        initFaceDetection();
        addFaceToPreference();
        cameraBind();
        initListener();
    }

    private void initFaceDetection() {
        FaceDetectorOptions highAccuracyOpts = new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL).build();
        detector = FaceDetection.getClient(highAccuracyOpts);
    }

    private void cameraBind() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                processCameraProvider = cameraProviderFuture.get();

                bindPreview(processCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                showToastMessage(e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CAM_FACE).build();

        preview.setSurfaceProvider(binding.prvCamera.getSurfaceProvider());
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(640, 480)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Latest frame is shown
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, imageProxy -> {
            Log.d("DEBUG_DATA", "bindPreview: inside of analysis");
            try {
                Thread.sleep(0);  //Camera preview refreshed every 10 millisec(adjust as required)
            } catch (InterruptedException e) {
                showToastMessage(e.getMessage());
            }
            InputImage image = null;


            @SuppressLint("UnsafeExperimentalUsageError") Image mediaImage = imageProxy.getImage();

            if (mediaImage != null) {
                image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            }


            detector.process(image).addOnSuccessListener(faces -> {
                Log.d("DEBUG_DEBUG", "Total registered faces: " + registered.size());
                if (faces.size() != 0) {
                    Face face = faces.get(0);
                    Bitmap frame_bmp = toBitmap(mediaImage);

                    int rot = imageProxy.getImageInfo().getRotationDegrees();

                    Bitmap frame_bmp1 = rotateBitmap(frame_bmp, rot, false, false);


                    RectF boundingBox = new RectF(face.getBoundingBox());

                    Bitmap cropped_face = getCropBitmapByCPU(frame_bmp1, boundingBox);

                    if (flipX) cropped_face = rotateBitmap(cropped_face, 0, flipX, false);
                    Bitmap scaled = getResizedBitmap(cropped_face, 112, 112);

                    if (startRecognition) {
                        recognizeImage(scaled);
                    }
                } else {
                    binding.txvRecognition.setText("Unknown");
                }

            }).addOnFailureListener(e -> {
                showToastMessage(e.getMessage());
            }).addOnCompleteListener(task -> {
                imageProxy.close();
            });


        });


        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);


    }

    private void initRegister() {
        registered = readFromSP();
    }

    private void initTensorFlowModel() {
        try {
            tfLite = new Interpreter(loadModelFile(ImageCompareActivity.this, modelFile));
        } catch (IOException e) {
            showToastMessage(e.getMessage());
        }
    }

    private void initListener() {
        binding.btnRecognize.setOnClickListener(this);
    }

    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private HashMap<String, SimilarityClassifier.Recognition> readFromSP() {
        SharedPreferences sharedPreferences = getSharedPreferences("HashMap", MODE_PRIVATE);
        String defValue = new Gson().toJson(new HashMap<String, SimilarityClassifier.Recognition>());
        String json = sharedPreferences.getString("map", defValue);
        TypeToken<HashMap<String, SimilarityClassifier.Recognition>> token = new TypeToken<HashMap<String, SimilarityClassifier.Recognition>>() {
        };
        HashMap<String, SimilarityClassifier.Recognition> retrievedMap = new Gson().fromJson(json, token.getType());
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : retrievedMap.entrySet()) {
            float[][] output = new float[1][OUTPUT_SIZE];
            ArrayList arrayList = (ArrayList) entry.getValue().getExtra();
            arrayList = (ArrayList) arrayList.get(0);
            for (int counter = 0; counter < arrayList.size(); counter++) {
                output[0][counter] = ((Double) arrayList.get(counter)).floatValue();
            }
            entry.getValue().setExtra(output);
        }
        Toast.makeText(ImageCompareActivity.this, "Recognitions Loaded", Toast.LENGTH_SHORT).show();
        return retrievedMap;
    }

    private void addFaceToPreference() {
        Intent i = getIntent();
        String imagePath = i.getStringExtra("filePath");
        String drivingLicenseHolderName = i.getStringExtra("holderName");
        if (imagePath != null) {
            SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition("0", "", -1f);
            InputImage image;
            Uri imageUri = Uri.fromFile(new File(imagePath));
            try {
                image = InputImage.fromFilePath(this, imageUri);

                detector.process(image).addOnSuccessListener(faces -> {
                    if (faces.size() != 0) {

                        Face face = faces.get(0);
                        InputStream imageStream = null;
                        try {
                            imageStream = getContentResolver().openInputStream(imageUri);
                            Bitmap imageBitmap = BitmapFactory.decodeStream(imageStream);

                            RectF boundingBox = new RectF(face.getBoundingBox());

                            Bitmap cropped_face = getCropBitmapByCPU(imageBitmap, boundingBox);

                            Bitmap scaled = getResizedBitmap(cropped_face, INPUT_SIZE, INPUT_SIZE);
                            recognizeImage(scaled);
                            result.setExtra(embeedings);
                            registered.put(drivingLicenseHolderName, result);
                        } catch (FileNotFoundException e) {
                            binding.txvRecognition.setText("Unknown");
                        }
                    } else {
                        binding.txvRecognition.setText("Unknown");
                    }
                }).addOnFailureListener(e -> {
                    binding.txvRecognition.setText("Unknown");
                });

            } catch (IOException e) {
                binding.txvRecognition.setText("Unknown");
            }
        } else {
            showToastMessage("Image url is not given");
        }
    }

    public void recognizeImage(final Bitmap bitmap) {

        ByteBuffer imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4);
        imgData.order(ByteOrder.nativeOrder());

        intValues = new int[INPUT_SIZE * INPUT_SIZE];

        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();

        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                int pixelValue = intValues[i * INPUT_SIZE + j];
//                if (isModelQuantized) {
//                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
//                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
//                    imgData.put((byte) (pixelValue & 0xFF));
//                } else {
                imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
//                }
            }
        }
        Object[] inputArray = {imgData};

        Map<Integer, Object> outputMap = new HashMap<>();


        embeedings = new float[1][OUTPUT_SIZE];

        outputMap.put(0, embeedings);

        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);

        if (startRecognition) {
            float distance_local = Float.MAX_VALUE;
            String id = "0";
            String label = "?";

            if (registered.size() > 0 && embeedings != null) {

                final List<Pair<String, Float>> nearest = findNearest(embeedings[0]);

                if (nearest.get(0) != null) {

                    final String name = nearest.get(0).first;
                    distance_local = nearest.get(0).second;
                    if (distance_local < distance) {
                        faceRecognitionCount++;
                        if (faceRecognitionCount >= 10) {
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("verified", true);
                            setResult(RESULT_OK, returnIntent);
                            finish();
                        }
                    }
                    if (distance_local < distance) binding.txvRecognition.setText("Holder Name");
                    else binding.txvRecognition.setText("Unknown");
                }


            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == binding.btnRecognize.getId()) {

            startRecognition = !startRecognition;
            if (startRecognition) {
                binding.btnRecognize.setText("Stop recognition");
            } else {
                binding.btnRecognize.setText("Start Recognition");
            }
        }
    }

    private static byte[] YUV_420_888toNV21(Image image) {

        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width * height;
        int uvSize = width * height / 4;

        byte[] nv21 = new byte[ySize + uvSize * 2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();
        assert (image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        } else {
            long yBufferPos = -rowStride; // not an actual position
            for (; pos < ySize; pos += width) {
                yBufferPos += rowStride;
                yBuffer.position((int) yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        assert (rowStride == image.getPlanes()[1].getRowStride());
        assert (pixelStride == image.getPlanes()[1].getPixelStride());

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.position(0);
                    uBuffer.position(0);
                    vBuffer.get(nv21, ySize, 1);
                    uBuffer.get(nv21, ySize + 1, uBuffer.remaining());

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                // unfortunately, we cannot check if vBuffer and uBuffer overlap
            }

            // unfortunately, the check failed. We must save U and V pixel by pixel
            vBuffer.put(1, savePixel);
        }

        for (int row = 0; row < height / 2; row++) {
            for (int col = 0; col < width / 2; col++) {
                int vuPos = col * pixelStride + row * rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }

        return nv21;
    }

    private static Bitmap getCropBitmapByCPU(Bitmap source, RectF cropRectF) {
        Bitmap resultBitmap = Bitmap.createBitmap((int) cropRectF.width(), (int) cropRectF.height(), Bitmap.Config.ARGB_8888);
        Canvas cavas = new Canvas(resultBitmap);

        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        paint.setColor(Color.WHITE);
        cavas.drawRect(new RectF(0, 0, cropRectF.width(), cropRectF.height()), paint);

        Matrix matrix = new Matrix();
        matrix.postTranslate(-cropRectF.left, -cropRectF.top);

        cavas.drawBitmap(source, matrix, paint);

        if (source != null && !source.isRecycled()) {
            source.recycle();
        }

        return resultBitmap;
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private Bitmap toBitmap(Image image) {

        byte[] nv21 = YUV_420_888toNV21(image);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    private static Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
        Matrix matrix = new Matrix();

        matrix.postRotate(rotationDegrees);

        matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }

    private List<Pair<String, Float>> findNearest(float[] emb) {
        List<Pair<String, Float>> neighbour_list = new ArrayList<Pair<String, Float>>();
        Pair<String, Float> ret = null; //to get closest match
        Pair<String, Float> prev_ret = null; //to get second closest match
        for (Map.Entry<String, SimilarityClassifier.Recognition> entry : registered.entrySet()) {
            final String name = entry.getKey();
            final float[] knownEmb = ((float[][]) entry.getValue().getExtra())[0];

            float distance = 0;
            for (int i = 0; i < emb.length; i++) {
                float diff = emb[i] - knownEmb[i];
                distance += diff * diff;
            }
            distance = (float) Math.sqrt(distance);
            if (ret == null || distance < ret.second) {
                prev_ret = ret;
                ret = new Pair<>(name, distance);
            }
        }
        if (prev_ret == null) prev_ret = ret;
        neighbour_list.add(ret);
        neighbour_list.add(prev_ret);

        return neighbour_list;

    }

    void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}