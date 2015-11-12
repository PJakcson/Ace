package com.aceft.data;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.List;

public class ScreenSlidePagerAdapter extends FragmentPagerAdapter {
    private List<Fragment> fragments;

    public ScreenSlidePagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0: return "Overview";
            case 1: return "Chat";
        }
        return "" + position;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}
