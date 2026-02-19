package com.example.blackcar.presentation.history.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.blackcar.R;
import com.example.blackcar.databinding.FragmentPassengerHistoryHostBinding;
import com.example.blackcar.presentation.stats.views.PassengerStatsFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PassengerHistoryHostFragment extends Fragment {

    private FragmentPassengerHistoryHostBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPassengerHistoryHostBinding.inflate(inflater, container, false);

        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 2;
            }

            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? new PassengerHistoryFragment() : new PassengerStatsFragment();
            }
        };
        binding.viewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setText(position == 0 ? getString(R.string.tab_history) : getString(R.string.tab_statistics));
        }).attach();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
