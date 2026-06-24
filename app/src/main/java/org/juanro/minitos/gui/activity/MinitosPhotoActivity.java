package org.juanro.minitos.gui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.juanro.minitos.R;
import org.juanro.minitos.data.api.MinitsApiClient;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.data.api.MinitsModels;
import org.juanro.minitos.data.api.config.NetworkConstants;

public class MinitosPhotoActivity extends Activity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 100;

    private String currentSlot = "";
    private String matricula;
    private String reservationId;
    private String userEmail;
    private double totalCost = 0.0;

    private File fileDelantera, fileTrasera, fileInterior;
    private ImageView ivDelantera, ivTrasera, ivInterior;
    private Button btnSubmit, btnSkip;
    private ProgressBar progressBar;
    private MinitsApiClient minitsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minitos_photo);

        matricula = getIntent().getStringExtra("matricula");
        reservationId = getIntent().getStringExtra("reservationId");
        totalCost = getIntent().getDoubleExtra("totalCost", 0.0);

        minitsClient = new MinitsApiClient(this);
        MinitsAuthenticator auth = new MinitsAuthenticator(this, minitsClient);
        auth.loadSavedAuth();
        userEmail = auth.getEmail();
        minitsClient.setAuth(auth.getToken(), userEmail);

        ivDelantera = findViewById(R.id.ivPhotoDelantera);
        ivTrasera = findViewById(R.id.ivPhotoTrasera);
        ivInterior = findViewById(R.id.ivPhotoInterior);
        btnSubmit = findViewById(R.id.btnSubmitPhotos);
        btnSkip = findViewById(R.id.btnSkipPhotos);
        progressBar = findViewById(R.id.pbUpload);

        if (totalCost == 0.0) {
            btnSkip.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.llPhotoDelantera).setOnClickListener(v -> checkPermissionAndStartCamera("FOTOEXTDEL_ENT"));
        findViewById(R.id.llPhotoTrasera).setOnClickListener(v -> checkPermissionAndStartCamera("FOTOEXTATRAS_ENT"));
        findViewById(R.id.llPhotoInterior).setOnClickListener(v -> checkPermissionAndStartCamera("FOTOINT_ENT"));

        btnSubmit.setOnClickListener(v -> uploadAllPhotos());
        btnSkip.setOnClickListener(v -> showSummaryAndFinish());
    }

    private void checkPermissionAndStartCamera(String slot) {
        currentSlot = slot;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Se requiere permiso de cámara para tomar fotos de evidencia", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            if (extras == null) return;
            
            Bitmap imageBitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                imageBitmap = extras.getParcelable("data", Bitmap.class);
            } else {
                @SuppressWarnings("deprecation")
                Bitmap b = (Bitmap) extras.get("data");
                imageBitmap = b;
            }
            
            if (imageBitmap != null) {
                try {
                    File photoFile = saveBitmapToCache(imageBitmap, currentSlot);
                    updateUI(photoFile);
                } catch (IOException e) {
                    Toast.makeText(this, "Error guardando foto", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private File saveBitmapToCache(Bitmap bitmap, String slot) throws IOException {
        String fileName = "ph_" + UUID.randomUUID().toString() + ".jpg";
        File file = new File(getCacheDir(), fileName);
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        out.flush();
        out.close();

        switch (slot) {
            case "FOTOEXTDEL_ENT":
                fileDelantera = file;
                break;
            case "FOTOEXTATRAS_ENT":
                fileTrasera = file;
                break;
            case "FOTOINT_ENT":
                fileInterior = file;
                break;
        }

        return file;
    }

    private void updateUI(File file) {
        ImageView target = switch (currentSlot) {
            case "FOTOEXTDEL_ENT" -> ivDelantera;
            case "FOTOEXTATRAS_ENT" -> ivTrasera;
            case "FOTOINT_ENT" -> ivInterior;
            default -> null;
        };

        if (target != null) {
            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            target.setImageBitmap(myBitmap);
        }

        btnSubmit.setEnabled(fileDelantera != null && fileTrasera != null && fileInterior != null);
    }

    private void uploadAllPhotos() {
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        btnSkip.setEnabled(false);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    uploadSinglePhoto(fileDelantera, "FOTOEXTDEL_ENT");
                    uploadSinglePhoto(fileTrasera, "FOTOEXTATRAS_ENT");
                    uploadSinglePhoto(fileInterior, "FOTOINT_ENT");

                    runOnUiThread(() -> {
                        Toast.makeText(MinitosPhotoActivity.this, "Fotos subidas con éxito. Viaje cerrado.", Toast.LENGTH_LONG).show();
                        showSummaryAndFinish();
                    });
                } catch (final Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        btnSkip.setEnabled(true);
                        Toast.makeText(MinitosPhotoActivity.this, "Error subiendo fotos: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private void showSummaryAndFinish() {
        Intent intent = new Intent(MinitosPhotoActivity.this, MinitosTripSummaryActivity.class);
        intent.putExtra("vehicle", "N. " + matricula);
        intent.putExtra("dateRange", getIntent().getStringExtra("dateRange"));
        intent.putExtra("totalCost", totalCost);
        intent.putExtra("initialCost", getIntent().getDoubleExtra("initialCost", 0.0));
        intent.putExtra("standbyCost", getIntent().getDoubleExtra("standbyCost", 0.0));
        intent.putExtra("drivingCost", getIntent().getDoubleExtra("drivingCost", 0.0));
        intent.putExtra("batteryStart", getIntent().getStringExtra("batteryStart"));
        intent.putExtra("batteryEnd", getIntent().getStringExtra("batteryEnd"));
        intent.putExtra("distance", getIntent().getStringExtra("distance"));
        startActivity(intent);
        finish();
    }

    private void uploadSinglePhoto(File file, String pos) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("tipo", "1");
        params.put("pos", pos);
        params.put("matricula", "N. " + matricula);
        params.put("usuario", userEmail);
        params.put("numeroreserva", reservationId);
        params.put("name", file.getName());

        minitsClient.postMultipart(NetworkConstants.PATH_UPLOAD_FOTO, params, file, file.getName(), MinitsModels.GenericResponse.class);
    }
}
