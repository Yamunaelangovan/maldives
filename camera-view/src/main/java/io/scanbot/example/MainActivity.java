package io.scanbot.example;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.WindowCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import net.doo.snap.ScanbotSDK;
import net.doo.snap.camera.AutoSnappingController;
import net.doo.snap.camera.CameraOpenCallback;
import net.doo.snap.camera.ContourDetectorFrameHandler;
import net.doo.snap.camera.PictureCallback;
import net.doo.snap.camera.ScanbotCameraView;
import net.doo.snap.entity.Document;
import net.doo.snap.entity.Page;
import net.doo.snap.entity.SnappingDraft;
import net.doo.snap.lib.detector.ContourDetector;
import net.doo.snap.lib.detector.DetectionResult;
import net.doo.snap.persistence.PageFactory;
import net.doo.snap.persistence.cleanup.Cleaner;
import net.doo.snap.process.DocumentProcessingResult;
import net.doo.snap.process.DocumentProcessor;
import net.doo.snap.process.draft.DocumentDraftExtractor;
import net.doo.snap.process.util.DocumentDraft;
import net.doo.snap.ui.PolygonView;
import net.doo.snap.util.FileChooserUtils;
import net.doo.snap.util.bitmap.BitmapUtils;
import net.doo.snap.util.thread.MimeUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements PictureCallback,
        ContourDetectorFrameHandler.ResultHandler {

    private ScanbotCameraView cameraView;
    private PolygonView polygonView;
    private ImageView resultView;
    private ContourDetectorFrameHandler contourDetectorFrameHandler;
    private AutoSnappingController autoSnappingController;
    private Toast userGuidanceToast;


    private boolean flashEnabled = false;
    private boolean autoSnappingEnabled = true;
    private PageFactory pageFactory;
    private DocumentDraftExtractor documentDraftExtractor;
    private DocumentProcessor documentProcessor;
    private Cleaner cleaner;

    private View progressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getSupportActionBar().hide();

        cameraView = (ScanbotCameraView) findViewById(R.id.camera);
        cameraView.setCameraOpenCallback(new CameraOpenCallback() {
            @Override
            public void onCameraOpened() {
                cameraView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cameraView.setAutoFocusSound(false);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            cameraView.setShutterSound(false);
                        }

                        cameraView.continuousFocus();
                        cameraView.useFlash(flashEnabled);
                    }
                }, 700);
            }
        });

        resultView = (ImageView) findViewById(R.id.result);
        progressView = findViewById(R.id.progressBar);

        contourDetectorFrameHandler = ContourDetectorFrameHandler.attach(cameraView);

        // Please note: https://github.com/doo/Scanbot-SDK-Examples/wiki/Detecting-and-drawing-contours#contour-detection-parameters
        contourDetectorFrameHandler.setAcceptedAngleScore(60);
        contourDetectorFrameHandler.setAcceptedSizeScore(70);

        polygonView = (PolygonView) findViewById(R.id.polygonView);
        contourDetectorFrameHandler.addResultHandler(polygonView);
        contourDetectorFrameHandler.addResultHandler(this);

        autoSnappingController = AutoSnappingController.attach(cameraView, contourDetectorFrameHandler);

        cameraView.addPictureCallback(this);

        userGuidanceToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);


        findViewById(R.id.snap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.takePicture(false);
            }
        });

        findViewById(R.id.flash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flashEnabled = !flashEnabled;
                cameraView.useFlash(flashEnabled);
            }
        });

        findViewById(R.id.autoSnappingToggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoSnappingEnabled = !autoSnappingEnabled;
                setAutoSnapEnabled(autoSnappingEnabled);
            }
        });

        setAutoSnapEnabled(autoSnappingEnabled);

        initializeDependencies();
    }

    private void initializeDependencies() {
        ScanbotSDK scanbotSDK = new ScanbotSDK(this);
        pageFactory = scanbotSDK.pageFactory();
        documentDraftExtractor = scanbotSDK.documentDraftExtractor();
        documentProcessor = scanbotSDK.documentProcessor();
        cleaner = scanbotSDK.cleaner();
    }


    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.onPause();
    }

    @Override
    public boolean handleResult(final ContourDetectorFrameHandler.DetectedFrame detectedFrame) {
        // Here you are continuously notified about contour detection results.
        // For example, you can show a user guidance text depending on the current detection status.
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showUserGuidance(detectedFrame.detectionResult);
            }
        });

        return false; // typically you need to return false
    }

    private void showUserGuidance(final DetectionResult result) {
        if (!autoSnappingEnabled) {
            return;
        }

        switch (result) {
            case OK:
                userGuidanceToast.setText("Don't move");
                userGuidanceToast.show();
                break;
            case OK_BUT_TOO_SMALL:
                userGuidanceToast.setText("Move closer");
                userGuidanceToast.show();
                break;
            case OK_BUT_BAD_ANGLES:
                userGuidanceToast.setText("Perspective");
                userGuidanceToast.show();
                break;
            case ERROR_NOTHING_DETECTED:
                userGuidanceToast.setText("No Document");
                userGuidanceToast.show();
                break;
            case ERROR_TOO_NOISY:
                userGuidanceToast.setText("Background too noisy");
                userGuidanceToast.show();
                break;
            case ERROR_TOO_DARK:
                userGuidanceToast.setText("Poor light");
                userGuidanceToast.show();
                break;
            default:
                userGuidanceToast.cancel();
                break;
        }
    }

    @Override
    public void onPictureTaken(byte[] image, int imageOrientation) {
        // Here we get the full image from the camera.
        // Implement a suitable async(!) detection and image handling here.
        // This is just a demo showing detected image as downscaled preview image.

        // Decode Bitmap from bytes of original image:
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8; // use 1 for original size (if you want no downscale)!
                                  // in this demo we downscale the image to 1/8 for the preview.
        Bitmap originalBitmap = BitmapFactory.decodeByteArray(image, 0, image.length, options);

        // rotate original image if required:
        if (imageOrientation > 0) {
            final Matrix matrix = new Matrix();
            matrix.setRotate(imageOrientation, originalBitmap.getWidth() / 2f, originalBitmap.getHeight() / 2f);
            originalBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, false);
        }

        // Run document detection on original image:
        final ContourDetector detector = new ContourDetector();
        detector.detect(originalBitmap);
        final Bitmap documentImage = detector.processImageAndRelease(originalBitmap, detector.getPolygonF(), ContourDetector.IMAGE_FILTER_NONE);

        resultView.post(new Runnable() {
            @Override
            public void run() {
                new ProcessDocumentTask(documentImage).execute();
                cameraView.stopPreview();
            }
        });
    }

    private void setAutoSnapEnabled(boolean enabled) {
        autoSnappingController.setEnabled(enabled);
        contourDetectorFrameHandler.setEnabled(enabled);
        polygonView.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    private class ProcessDocumentTask extends AsyncTask<Void, Void, List<DocumentProcessingResult>> {

        private final Bitmap bitmap;
        private final int screenWidth;
        private final int screenHeight;

        private ProcessDocumentTask(Bitmap bmp) {
            this.bitmap = bmp;

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;
        }

        @Override
        protected List<DocumentProcessingResult> doInBackground(Void... voids) {
            List<DocumentProcessingResult> results = new ArrayList<>();

            try {
                Bitmap result = applyFilters(bitmap);

                Page page = pageFactory.buildPage(result, screenWidth, screenHeight).page;
                SnappingDraft snappingDraft = new SnappingDraft(page);
                DocumentDraft[] drafts = documentDraftExtractor.extract(snappingDraft);

                for (DocumentDraft draft : drafts) {
                    try {
                        results.add(documentProcessor.processDocument(draft));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                cleaner.cleanUp();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return results;
        }

        private Bitmap applyFilters(Bitmap bitmap) {
            ContourDetector detector = new ContourDetector();
            detector.detect(bitmap);
            List<PointF> polygon = detector.getPolygonF();

            /*
             * This operation crops original bitmap and creates a new one. Old bitmap is recycled
             * and can't be used anymore. If that's not what you need, use processImageF() instead
             */
            return detector.processImageAndRelease(bitmap, polygon, ContourDetector.IMAGE_FILTER_GRAY);
        }

        @Override
        protected void onPostExecute(List<DocumentProcessingResult> documentProcessingResults) {
            progressView.setVisibility(View.GONE);

            //open first document
            if (documentProcessingResults.size() > 0) {
                openDocument(documentProcessingResults.get(0));
            }
        }

    }


    private void openDocument(DocumentProcessingResult documentProcessingResult) {
        Document document = documentProcessingResult.getDocument();
        File documentFile = documentProcessingResult.getDocumentFile();

        Intent openIntent = new Intent();
        openIntent.setAction(Intent.ACTION_VIEW);
        openIntent.setDataAndType(
                Uri.fromFile(documentFile),
                MimeUtils.getMimeByName(document.getName())
        );

        if (openIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(openIntent);
        } else {
            Toast.makeText(MainActivity.this, "Error while opening the document", Toast.LENGTH_LONG).show();
        }
    }

}
