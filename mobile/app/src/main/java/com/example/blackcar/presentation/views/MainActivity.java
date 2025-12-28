    package com.example.blackcar.presentation.views;

    import android.os.Bundle;
    import androidx.appcompat.app.AppCompatActivity;

    import com.example.blackcar.databinding.ActivityMainBinding;
    import com.example.blackcar.databinding.ContentMainBinding;

    public class MainActivity extends AppCompatActivity {

        private ActivityMainBinding binding;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Toolbar is optional — remove if you don’t want it
            setSupportActionBar(binding.toolbar);

            // Example landing action (remove if not needed)
            binding.fab.setOnClickListener(v -> {
                ContentMainBinding contentBinding = binding.contentMain;
                contentBinding.message.setText("Welcome to BlackCar!");                    }
            );
        }
    }
