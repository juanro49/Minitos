package org.juanro.minitos.gui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.juanro.minitos.R;
import org.juanro.minitos.data.api.MinitsApiClient;
import org.juanro.minitos.data.api.MinitsAuthenticator;
import org.juanro.minitos.data.api.MinitsModels;
import org.juanro.minitos.data.api.config.NetworkConstants;

public class MinitosProfileActivity extends AppCompatActivity {
    private static final String TAG = "MinitosProfileActivity";

    // Login views
    private View llLoginContainer;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    
    // Profile views
    private View llProfileContainer;
    private TextView tvName, tvProfileEmail, tvAddress, tvCP, tvCity, tvBirth, tvPhone;
    private Spinner spSede;
    private Button btnSave, btnChangePhone;

    private ProgressBar pbLoading;
    private MinitsApiClient minitsClient;
    private MinitsAuthenticator minitsAuth;
    private List<MinitsModels.Sede> sedesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minitos_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        AppBarLayout appBarLayout = findViewById(R.id.app_bar);
        View root = findViewById(R.id.profile_root_layout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBarLayout.setPadding(0, insets.top, 0, 0);
            v.setPadding(0, 0, 0, insets.bottom);
            return windowInsets;
        });

        // Initialize views
        llLoginContainer = findViewById(R.id.llLoginContainer);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        llProfileContainer = findViewById(R.id.llProfileContainer);
        tvName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvAddress = findViewById(R.id.tvProfileAddress);
        tvCP = findViewById(R.id.tvProfileCP);
        tvCity = findViewById(R.id.tvProfileCity);
        tvBirth = findViewById(R.id.tvProfileBirth);
        tvPhone = findViewById(R.id.tvProfilePhone);
        spSede = findViewById(R.id.spSede);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnChangePhone = findViewById(R.id.btnChangePhone);
        Button btnLogout = findViewById(R.id.btnLogout);

        pbLoading = findViewById(R.id.pbLoading);

        minitsClient = new MinitsApiClient(this);
        minitsAuth = new MinitsAuthenticator(this, minitsClient);
        minitsAuth.loadSavedAuth();

        updateUI();

        btnLogin.setOnClickListener(v -> performLogin());
        btnLogout.setOnClickListener(v -> {
            minitsAuth.logout();
            updateUI();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        });
        btnSave.setOnClickListener(v -> saveProfile());
        btnChangePhone.setOnClickListener(v -> showPhoneInputDialog());
    }

    private void updateUI() {
        if (minitsAuth.isLoggedIn()) {
            llLoginContainer.setVisibility(View.GONE);
            llProfileContainer.setVisibility(View.VISIBLE);
            loadUserData();
            loadSedes();
        } else {
            llLoginContainer.setVisibility(View.VISIBLE);
            llProfileContainer.setVisibility(View.GONE);
        }
    }

    private void loadUserData() {
        tvName.setText(minitsAuth.getName());
        tvProfileEmail.setText(minitsAuth.getEmail());
        tvAddress.setText(minitsAuth.getAddress());
        tvCP.setText(minitsAuth.getCP());
        tvCity.setText(minitsAuth.getCity());
        tvBirth.setText(minitsAuth.getBirthDate());
        tvPhone.setText(minitsAuth.getPrefix() + " " + minitsAuth.getPhone());
    }

    private void performLogin() {
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    minitsAuth.login(email, password);
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        updateUI();
                        Toast.makeText(this, "Sesión iniciada", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
    }

    private void loadSedes() {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    MinitsModels.DatosSQLRequest req = new MinitsModels.DatosSQLRequest("Sedes", Collections.singletonList(""));
                    MinitsModels.SedesResponse resp = minitsClient.post(NetworkConstants.PATH_DATOS_SQL, req, MinitsModels.SedesResponse.class);
                    
                    if (resp != null && resp.data != null) {
                        sedesList = List.of(resp.data);
                        List<String> names = new ArrayList<>();
                        int selection = 0;
                        int i = 0;
                        while (i < sedesList.size()) {
                            MinitsModels.Sede s = sedesList.get(i);
                            names.add(s.DESCRIPCION);
                            if (s.ID == minitsAuth.getSedeId()) {
                                selection = i;
                            }
                            i++;
                        }

                        final int finalSelection = selection;
                        runOnUiThread(() -> {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spSede.setAdapter(adapter);
                            spSede.setSelection(finalSelection);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading sedes", e);
                }
            });
        }
    }

    private void saveProfile() {
        int pos = spSede.getSelectedItemPosition();
        if (pos < 0 || pos >= sedesList.size()) return;
        
        MinitsModels.Sede selected = sedesList.get(pos);
        btnSave.setEnabled(false);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    MinitsModels.DatosPersonalesRequest req = new MinitsModels.DatosPersonalesRequest(
                            minitsAuth.getEmail(),
                            minitsAuth.getName(),
                            minitsAuth.getBirthDate(),
                            minitsAuth.getAddress(),
                            minitsAuth.getCP(),
                            minitsAuth.getCity(),
                            selected.ID
                    );
                    
                    MinitsModels.GenericResponse<?> resp = minitsClient.post(NetworkConstants.PATH_DATOS_PERSONALES, req, MinitsModels.GenericResponse.class);
                    
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        if (resp != null && !resp.hasError) {
                            minitsAuth.updateSedeId(selected.ID);
                            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Error al guardar el perfil", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving profile", e);
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void showPhoneInputDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Cambiar número de teléfono");
        
        View view = getLayoutInflater().inflate(R.layout.dialog_change_phone, null);
        final EditText etPref = view.findViewById(R.id.etPrefix);
        final EditText etPhone = view.findViewById(R.id.etPhone);
        
        etPref.setText(minitsAuth.getPrefix());
        etPhone.setText(minitsAuth.getPhone());
        
        builder.setView(view);
        builder.setPositiveButton("Enviar SMS", (dialog, which) -> {
            String prefix = etPref.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            if (!prefix.isEmpty() && !phone.isEmpty()) {
                sendSmsVerification(prefix, phone);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void sendSmsVerification(String prefix, String phone) {
        pbLoading.setVisibility(View.VISIBLE);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    MinitsModels.SendSmsRequest req = new MinitsModels.SendSmsRequest(minitsAuth.getEmail(), prefix, phone);
                    MinitsModels.GenericResponse<?> resp = minitsClient.post(NetworkConstants.PATH_SEND_CODE_SMS, req, MinitsModels.GenericResponse.class);
                    
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        if (resp != null && !resp.hasError) {
                            showCodeInputDialog(prefix, phone);
                        } else {
                            Toast.makeText(this, "Error enviando SMS: " + (resp != null ? resp.message : "null"), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "SMS error", e);
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void showCodeInputDialog(String prefix, String phone) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Verificar código");
        builder.setMessage("Introduce el código recibido por SMS");
        
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);
        
        builder.setPositiveButton("Verificar", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if (!code.isEmpty()) {
                updatePhoneNumber(prefix, phone);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void updatePhoneNumber(String prefix, String phone) {
        pbLoading.setVisibility(View.VISIBLE);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.execute(() -> {
                try {
                    MinitsModels.UpdatePhoneRequest req = new MinitsModels.UpdatePhoneRequest(minitsAuth.getEmail(), prefix, phone);
                    MinitsModels.GenericResponse<?> resp = minitsClient.post(NetworkConstants.PATH_UPDATE_PHONE, req, MinitsModels.GenericResponse.class);
                    
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        if (resp != null && !resp.hasError) {
                            Toast.makeText(this, "Teléfono actualizado con éxito", Toast.LENGTH_LONG).show();
                            updateUI();
                        } else {
                            Toast.makeText(this, "Error actualizando teléfono", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Update phone error", e);
                    runOnUiThread(() -> {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }
}
