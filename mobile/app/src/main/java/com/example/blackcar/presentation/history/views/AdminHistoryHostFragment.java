package com.example.blackcar.presentation.history.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.blackcar.R;
import com.example.blackcar.databinding.FragmentAdminHistoryHostBinding;
import com.example.blackcar.presentation.stats.views.AdminStatsFragment;
import com.google.android.material.tabs.TabLayoutMediator;

public class AdminHistoryHostFragment extends Fragment {

    private FragmentAdminHistoryHostBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminHistoryHostBinding.inflate(inflater, container, false);

        FragmentStateAdapter adapter = new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 2;
            }

            @Override
            public Fragment createFragment(int position) {
                return position == 0 ? new AdminHistoryFragment() : new AdminStatsFragment();
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
