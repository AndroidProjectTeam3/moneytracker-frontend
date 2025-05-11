package kr.ac.uc.money_tracker;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 프래그먼트 기본 설정
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MainFragment())
                    .commit();
        }

        // 탭 클릭 시 프래그먼트 교체
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;

            int id = item.getItemId();
            if (id == R.id.nav_goal) {
                selectedFragment = new goalFragment();
            } else if (id == R.id.nav_list) {
                selectedFragment = new ListFragment();
            } else if (id == R.id.nav_main) {
                selectedFragment = new MainFragment();
            } else if (id == R.id.nav_analysis) {
                selectedFragment = new AnalysisFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else {
                return false;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
        bottomNavigationView.setSelectedItemId(R.id.nav_main);
    }
}
