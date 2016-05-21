package com.example.alexander.pdfreader;

import android.support.v4.app.Fragment;

public class PagesActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new PagesFragment();
    }
}