package com.miniai.facematch;

import static android.media.CamcorderProfile.get;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.graphics.drawable.Drawable;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.os.Handler;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import org.aviran.cookiebar2.CookieBar;

import de.hdodenhof.circleimageview.CircleImageView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.fm.face.FaceSDK;
import com.fm.face.FaceBox;
import android.content.res.AssetManager;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_GALLERY_PERMISSION = 101;
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private CircleImageView vImg1, vImg2, currentImageView;
    private ImageView ivGIFOverlayImg1, ivGIFOverlayImg2, ivInfoIcon, ivRefreshIcon;
    private Button btnCompareFaces;
    private View vCircularProgress;
    private TextView tvFirstImagePlaceholder,tvSecondImagePlaceholder;

    private TextView similarityScore, similarityDetail;
    public static  UserInfo firstImage, secondImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        setContentView(R.layout.activity_main);

        firstImage = new UserInfo();
        secondImage = new UserInfo();

        FaceSDK.createInstance(this);
        FaceSDK faceSDK = FaceSDK.getInstance();
        int ret = faceSDK.init(this.getAssets());

        if(ret != FaceSDK.SDK_SUCCESS) {
            if(ret == FaceSDK.SDK_ACTIVATE_APPID_ERROR) {
                showAlertDialog(getString(R.string.appid_error));
            } else if(ret == FaceSDK.SDK_ACTIVATE_INVALID_LICENSE) {
                showAlertDialog(getString(R.string.invalid_license));
            } else if(ret == FaceSDK.SDK_ACTIVATE_LICENSE_EXPIRED) {
                showAlertDialog(getString(R.string.license_expired));
            } else if(ret == FaceSDK.SDK_NO_ACTIVATED) {
                showAlertDialog(getString(R.string.no_activated));
            } else if(ret == FaceSDK.SDK_INIT_ERROR) {
                showAlertDialog(getString(R.string.init_error));
            }
        } else {

            // Initialize views
            vImg1 = findViewById(R.id.circle_img1);
            vImg2 = findViewById(R.id.circle_img2);

            ivGIFOverlayImg1 = findViewById(R.id.gif_overlay_img1);
            ivGIFOverlayImg2 = findViewById(R.id.gif_overlay_img2);

            btnCompareFaces = findViewById(R.id.btn_compare_faces);

            vCircularProgress = findViewById(R.id.circular_progress_view);

            ivInfoIcon = findViewById(R.id.iv_result_info_img);
            ivRefreshIcon = findViewById(R.id.iv_result_refresh_img);

            tvFirstImagePlaceholder = findViewById(R.id.tv_first_image_placeholder);
            tvSecondImagePlaceholder = findViewById(R.id.tv_second_image_placeholder);

            setOnClicks();
            setInitialValues();
            resetActivity();
        }
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set the dialog title and message
        builder.setTitle("Warning!");
        builder.setMessage(message + "\nYou may not able to test our SDK!\nContact US and Purchase License and Enjoy!");

        // Set positive button and its click listener
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle positive button click, if needed
                // You can perform some action or dismiss the dialog
                dialog.dismiss();
            }
        });

        // Set negative button and its click listener, if needed
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle negative button click, if needed
                // You can perform some action or dismiss the dialog
                dialog.dismiss();
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void displayInformationPopup() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    private void setInitialValues() {
        ivGIFOverlayImg1.setVisibility(View.INVISIBLE);
        ivGIFOverlayImg2.setVisibility(View.INVISIBLE);

        // Use Glide to load the GIF into the ImageView
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // Disable caching for GIFs
                .into(ivGIFOverlayImg1);

        Glide.with(this)
                .asGif()
                .load(R.drawable.loading)
                .diskCacheStrategy(DiskCacheStrategy.NONE)  // Disable caching for GIFs
                .into(ivGIFOverlayImg2);

        vCircularProgress.setVisibility(View.INVISIBLE);
    }

    private void setOnClicks() {
        vImg1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsDialog(vImg1);
            }
        });

        vImg2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsDialog(vImg2);
            }
        });

        btnCompareFaces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initiateFaceCompare();
            }
        });

        ivInfoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayInformationPopup();
            }
        });
        
        ivRefreshIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetActivity();
            }
        });
    }

    private void initiateFaceCompare() {
        if (checkSelectedImages()) {
            // Hide the GIF overlay after a delay
            ivGIFOverlayImg1.setVisibility(View.VISIBLE);
            ivGIFOverlayImg2.setVisibility(View.VISIBLE);

            similarityScore = findViewById(R.id.title_text);
            similarityDetail = findViewById(R.id.detail_text);

            float score = FaceSDK.getInstance().compareFeature(MainActivity.firstImage.featData, MainActivity.secondImage.featData);
            similarityScore.setText(String.format("%.2f%%", score * 100));
            if (score > 0.9) {
                similarityDetail.setText("Is same person: Probability very High.");
            } else if (score > 0.7) {
                similarityDetail.setText("Is same person: Probability High.");
            } else if (score > 0.5) {
                similarityDetail.setText("Is same person: Probability normal.");
            } else if (score > 0.3) {
                similarityDetail.setText("Is same person: Probability low.");
            } else {
                similarityDetail.setText("Is same person: Probability very low.");
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Hide the GIF overlay after a delay
                    ivGIFOverlayImg1.setVisibility(View.INVISIBLE);
                    ivGIFOverlayImg2.setVisibility(View.INVISIBLE);

                    // Update UI components as needed
                    btnCompareFaces.setVisibility(View.INVISIBLE);
                    vCircularProgress.setVisibility(View.VISIBLE);
                }
            }, 500);
        }
    }

    private boolean checkSelectedImages() {
        View rootView = findViewById(android.R.id.content);
        Drawable defaultDrawable = ContextCompat.getDrawable(this, R.drawable.user);
        if (vImg1.getDrawable().getConstantState().equals(defaultDrawable.getConstantState())) {
            showCustomCookieWithMessage(rootView, getString(R.string.add_first_image));
            return false;
        } else if (vImg2.getDrawable().getConstantState().equals(defaultDrawable.getConstantState())) {
            showCustomCookieWithMessage(rootView, getString(R.string.add_second_image));
            return false;
        }
        return  true;
    }

    private void showOptionsDialog(final CircleImageView imageView) {
        Dialog cameraGalleryOptionsDialog = new Dialog(this,R.style.MediaPickerDialog);
        cameraGalleryOptionsDialog.setContentView(R.layout.media_picker_dialog_layout);

        // Set the gravity to the bottom
        Window window = cameraGalleryOptionsDialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.BOTTOM;

        // Set the width of the dialog to match the parent
        params.width = WindowManager.LayoutParams.MATCH_PARENT;

        // Add a bottom margin (adjust the value as needed)
        params.verticalMargin = 0.1f; // 10% margin from the bottom

        // Apply the changes
        window.setAttributes(params);

        onCameraOptionClicked(cameraGalleryOptionsDialog,imageView);
        onGalleryOptionClicked(cameraGalleryOptionsDialog,imageView);
        dismissOnExternalTap(cameraGalleryOptionsDialog);

        cameraGalleryOptionsDialog.show();
    }

    private void onCameraOptionClicked(Dialog cameraGalleryOptionsDialog,final CircleImageView imageView){
        // Handle Action for camera
        Button btnCamera = cameraGalleryOptionsDialog.findViewById(R.id.btn_camera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraGalleryOptionsDialog.dismiss();
                checkCameraPermissionAndOpenCamera(imageView);
            }
        });
    }

    private void onGalleryOptionClicked(Dialog cameraGalleryOptionsDialog,final CircleImageView imageView){
        // Handle Action for Gallery
        Button btnGallery = cameraGalleryOptionsDialog.findViewById(R.id.btn_gallery);
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraGalleryOptionsDialog.dismiss();
                checkGalleryPermissionAndOpenGallery(imageView);
            }
        });
    }

    private void dismissOnExternalTap(Dialog cameraGalleryOptionsDialog){
        // Set touch listener to the cameraGalleryPopup layout of the custom popup
        View cameraGalleryPopup = cameraGalleryOptionsDialog.findViewById(R.id.camera_gallery_popup);
        cameraGalleryPopup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Dismiss the dialog when the background is clicked
                cameraGalleryOptionsDialog.dismiss();
            }
        });
    }
    
    private void checkCameraPermissionAndOpenCamera(CircleImageView imageView) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            currentImageView = imageView;
        } else {
            openCamera(imageView);
        }
    }

    private void checkGalleryPermissionAndOpenGallery(CircleImageView imageView) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_GALLERY_PERMISSION);
            currentImageView = imageView;
        } else {
            openGallery(imageView);
        }
    }

    private void openCamera(CircleImageView imageView) {
        Intent intent = new Intent(this, CameraActivity.class);
        if (imageView == vImg1) {
            intent.putExtra("ImageID", 1);
        } else if (imageView == vImg2) {
            intent.putExtra("ImageID", 2);
        }
        startActivityForResult(intent, REQUEST_CAMERA);
        currentImageView = imageView;
    }

    private void openGallery(CircleImageView imageView) {
        Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        currentImageView = imageView;
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    private void resetActivity() {
        vImg1.setImageResource(R.drawable.user);
        vImg2.setImageResource(R.drawable.user);

        btnCompareFaces.setVisibility(View.VISIBLE);
        vCircularProgress.setVisibility(View.INVISIBLE);

        tvFirstImagePlaceholder.setVisibility(View.VISIBLE);
        tvSecondImagePlaceholder.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        View rootView = findViewById(android.R.id.content);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY && data != null && currentImageView != null) {
                Uri selectedImageUri = data.getData();

                // Get the Bitmap from the Uri
                Bitmap photo = BitmapUtils.Companion.getBitmapFromUri(getContentResolver(), selectedImageUri);
                List<FaceBox> faceResult = FaceSDK.getInstance().detectFace(photo);

                if (faceResult != null && !faceResult.isEmpty()) {
                    if (faceResult.size() == 1) {
                        if (currentImageView == vImg1) {
                            firstImage.featData = FaceSDK.getInstance()
                                    .extractFeature(photo, faceResult.get(0));
                        } else if (currentImageView == vImg2) {
                            secondImage.featData = FaceSDK.getInstance()
                                    .extractFeature(photo, faceResult.get(0));
                        }
                        // Set the Bitmap to the ImageView
                        currentImageView.setImageBitmap(photo);
                        hidePlaceholderText(currentImageView);
                    } else {
                        showCustomCookieWithMessage(rootView, "Multi Face Detected");
                        currentImageView.setImageResource(R.drawable.user);
                        showPlaceholderText(currentImageView);
                    }
                } else {
                    showCustomCookieWithMessage(rootView, "No Face Detected");
                    currentImageView.setImageResource(R.drawable.user);
                    showPlaceholderText(currentImageView);
                }
            } else if (requestCode == REQUEST_CAMERA && currentImageView != null) {
                if (currentImageView == vImg1) {
                    currentImageView.setImageBitmap(firstImage.faceImage);
                    hidePlaceholderText(currentImageView);
                } else if (currentImageView == vImg2) {
                    currentImageView.setImageBitmap(secondImage.faceImage);
                    hidePlaceholderText(currentImageView);
                }
            }
        }
    }

    private void hidePlaceholderText(CircleImageView imageView) {
        // Get the corresponding TextView for the provided ImageView
        TextView placeholderText;
        if (imageView == vImg1) {
            placeholderText = tvFirstImagePlaceholder;
        } else if (imageView == vImg2) {
            placeholderText = tvSecondImagePlaceholder;
        } else {
            return;
        }
        // Hide the placeholder text
        placeholderText.setVisibility(View.GONE);   
    }

    private void showPlaceholderText(CircleImageView imageView) {
        // Get the corresponding TextView for the provided ImageView
        TextView placeholderText;
        if (imageView == vImg1) {
            placeholderText = tvFirstImagePlaceholder;
        } else if (imageView == vImg2) {
            placeholderText = tvSecondImagePlaceholder;
        } else {
            return;
        }
        // Hide the placeholder text
        placeholderText.setVisibility(View.VISIBLE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(currentImageView);
            }
        } else if (requestCode == REQUEST_GALLERY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery(currentImageView);
            }
        }
    }

    public void showCustomCookieWithMessage(View view, String errorMessage) {
        CookieBar.build(MainActivity.this)
                .setCustomView(R.layout.custom_cookie)
                .setCustomViewInitializer(new CookieBar.CustomViewInitializer() {
                    @Override
                    public void initView(View view) {
                        TextView message = view.findViewById(R.id.tv_description);
                        message.setText(errorMessage);
                    }
                })
                .setDuration(5000)
                .setCookiePosition(CookieBar.TOP)
                .show();
    }
}
