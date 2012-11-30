/*
 * A sliding menu for Android, very much like the Google+ and Facebook apps have.
 * 
 * Copyright (C) 2012 CoboltForge
 * 
 * Based upon the great work done by stackoverflow user Scirocco (http://stackoverflow.com/a/11367825/361413), thanks a lot!
 * The XML parsing code comes from https://github.com/darvds/RibbonMenu, thanks!
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coboltforge.slidemenu;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

public class SlideMenu extends LinearLayout {

    // keys for saving/restoring instance state
    private final static String KEY_MENUSHOWN = "menuWasShown";
    private final static String KEY_STATUSBARHEIGHT = "statusBarHeight";
    private final static String KEY_SUPERSTATE = "superState";

    public static final int DEFAULT_SLIDE_DURATION = 400;
    private int mSlideDuration = 0;


    public boolean isMenuShown() {
        return menuShown;
    }

    private boolean menuShown = false;
    private int statusHeight;
    private static View menu;
    private static ViewGroup content;
    private static FrameLayout parent;
    private static int menuSize;
    private Activity act;
    private TranslateAnimation mSlideMenuRightAnim;
    private TranslateAnimation mSlideMenuLeftAnim;
    private TranslateAnimation mSlideContentLeftAnim;
    private static final String TAG = SlideMenu.class.getSimpleName();

    public void setCallback(SlideMenuInterface.OnSlideMenuItemClickListener callback) {
        this.callback = callback;
    }

    private SlideMenuInterface.OnSlideMenuItemClickListener callback;

    private ListAdapter mListAdapter;

    public void setListAdapter(ListAdapter mListAdapter) {
        this.mListAdapter = mListAdapter;
    }

    /**
     * Constructor used by the inflation apparatus.
     * To be able to use the SlideMenu, call the {@link #init init()} method.
     *
     * @param context
     */
    public SlideMenu(Context context) {
        super(context);
    }

    /**
     * Constructor used by the inflation apparatus.
     * To be able to use the SlideMenu, call the {@link #init init()} method.
     *
     * @param attrs
     */
    public SlideMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * Constructs a SlideMenu.
     *
     * @param act The calling activity.
     */
    public SlideMenu(Activity act) {
        super(act);
        init(act);
    }

    /**
     * If inflated from XML, initializes the SlideMenu.
     *
     * @param act The calling activity.
     */
    public void init(Activity act) {
        this.act = act;

        // set size
        menuSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, act.getResources().getDisplayMetrics());

        setSlideDuration(DEFAULT_SLIDE_DURATION);
    }

    private void setSlideDuration(int slideDuration) {
        if (mSlideDuration == slideDuration) {
            return;
        }
        // create animations accordingly
        mSlideMenuRightAnim = new TranslateAnimation(-menuSize, 0, 0, 0);
        mSlideMenuRightAnim.setDuration(slideDuration);
        mSlideMenuRightAnim.setFillAfter(true);
        mSlideMenuRightAnim.setInterpolator(new FadeInInterpolator());

        mSlideMenuLeftAnim = new TranslateAnimation(0, -menuSize, 0, 0);
        mSlideMenuLeftAnim.setDuration(slideDuration);
        mSlideMenuLeftAnim.setFillAfter(true);
        mSlideMenuLeftAnim.setInterpolator(new FadeOutInterpolator());
        mSlideMenuLeftAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                parent.removeView(menu);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mSlideContentLeftAnim = new TranslateAnimation(menuSize, 0, 0, 0);
        mSlideContentLeftAnim.setDuration(slideDuration);
        mSlideContentLeftAnim.setFillAfter(true);

        mSlideDuration = slideDuration;
    }

    /** Slide the menu in. */
    public void show() {
        this.show(true);
    }

    /** Set the menu to shown status without displaying any slide animation. */
    public void setAsShown() {
        this.show(false);
    }

    private void show(boolean animate) {
        /*
           *  We have to adopt to status bar height in most cases,
           *  but not if there is a support actionbar!
           */
        try {
            Method getSupportActionBar = act.getClass().getMethod("getSupportActionBar", (Class[]) null);
            Object sab = getSupportActionBar.invoke(act, (Object[]) null);
            sab.toString(); // check for null

            if (android.os.Build.VERSION.SDK_INT >= 11) {
                // over api level 11? add the margin
                applyStatusbarOffset();
            }
        } catch (Exception es) {
            Log.d(TAG, "getSupportActionBar", es);
            // there is no support action bar!
            applyStatusbarOffset();
        }


        // modify content layout params
        try {
            content = ((LinearLayout) act.findViewById(android.R.id.content).getParent());
        } catch (ClassCastException e) {
            /*
                * When there is no title bar (android:theme="@android:style/Theme.NoTitleBar"),
                * the android.R.id.content FrameLayout is directly attached to the DecorView,
                * without the intermediate LinearLayout that holds the titlebar plus content.
                */
            content = (FrameLayout) act.findViewById(android.R.id.content);
        }
        FrameLayout.LayoutParams parm = new FrameLayout.LayoutParams(-1, -1, 3);
        parm.setMargins(menuSize, 0, -menuSize, 0);
        content.setLayoutParams(parm);

        // animation for smooth slide-out
        if (animate)
            content.startAnimation(mSlideMenuRightAnim);

        // add the slide menu to parent
        parent = (FrameLayout) content.getParent();
        LayoutInflater inflater = (LayoutInflater) act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        menu = inflater.inflate(R.layout.slidemenu, null);

        Log.d(TAG, "StatusBarHeight: " + statusHeight);
        FrameLayout.LayoutParams lays = new FrameLayout.LayoutParams(-1, -1, 3);
        lays.setMargins(0, statusHeight, 0, 0);
        menu.setLayoutParams(lays);
        parent.addView(menu);

        // Set the menu container size dynamically.
        final View container = menu.findViewById(R.id.f_slidemenu_container);
        final ViewGroup.LayoutParams layoutParams = container.getLayoutParams();
        layoutParams.width = menuSize;
        container.setLayoutParams(layoutParams);

        if (mListAdapter == null) {
            throw new RuntimeException("mListAdapter is null");
        }

        // connect the menu's listview
        ListView list = (ListView) act.findViewById(R.id.menu_listview);
        list.setAdapter(mListAdapter);
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (callback != null) {
                    if (callback.onSlideMenuItemClick(position, id)) {
                        hide();
                    }
                } else {
                    hide();
                }
            }
        });

        // slide menu in
        if (animate) {
            menu.startAnimation(mSlideMenuRightAnim);
        }


        menu.findViewById(R.id.overlay).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SlideMenu.this.hide();
            }
        });
        enableDisableViewGroup(content, false);

        menuShown = true;
    }


    /** Slide the menu out. */
    public void hide() {
        menu.startAnimation(mSlideMenuLeftAnim);

        content.startAnimation(mSlideContentLeftAnim);

        FrameLayout.LayoutParams parm = (FrameLayout.LayoutParams) content.getLayoutParams();
        parm.setMargins(0, 0, 0, 0);
        content.setLayoutParams(parm);
        enableDisableViewGroup(content, true);

        menuShown = false;
    }


    private void applyStatusbarOffset() {
        // Perhaps we got this information via a restored state.
        if (statusHeight > 0) {
            return;
        }

        Rect r = new Rect();
        Window window = act.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(r);
        statusHeight = r.top;
    }

    //originally: http://stackoverflow.com/questions/5418510/disable-the-touch-events-for-all-the-views
    //modified for the needs here
    private void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            if (view.isFocusable())
                view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            } else if (view instanceof ListView) {
                if (view.isFocusable())
                    view.setEnabled(enabled);
                ListView listView = (ListView) view;
                int listChildCount = listView.getChildCount();
                for (int j = 0; j < listChildCount; j++) {
                    if (view.isFocusable())
                        listView.getChildAt(j).setEnabled(false);
                }
            }
        }
    }

    public void restoreInstanceState(Parcelable state) {
        onRestoreInstanceState(state);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Log.d(TAG, "onRestoreInstanceState: " + state);
        try {

            if (state instanceof Bundle) {
                Bundle bundle = (Bundle) state;
                Log.d(TAG, "Got: " + bundle);

                statusHeight = bundle.getInt(KEY_STATUSBARHEIGHT);

                Log.d(TAG, "Height: " + statusHeight);

                if (bundle.getBoolean(KEY_MENUSHOWN))
                    show(false); // show without animation

                super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPERSTATE));

                return;
            }

            super.onRestoreInstanceState(state);

        } catch (NullPointerException e) {
            // in case the menu was not declared via XML but added from code
        }
    }

    public Parcelable getInstanceState() {
        return onSaveInstanceState();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_SUPERSTATE, super.onSaveInstanceState());
        bundle.putBoolean(KEY_MENUSHOWN, menuShown);
        bundle.putInt(KEY_STATUSBARHEIGHT, statusHeight);

        Log.d(TAG, "onSaveInstanceState: " + bundle);

        return bundle;
    }

    private class FadeInInterpolator extends AccelerateInterpolator {
        @Override
        public float getInterpolation(float input) {
//            Log.d(TAG, "Interpolation for point: " + input);
//            menu.setAlpha(input);
            return super.getInterpolation(input);
        }
    }

    private class FadeOutInterpolator extends AccelerateDecelerateInterpolator {
        @Override
        public float getInterpolation(float input) {
//            Log.d(TAG, "Interpolation for point: " + input);
//            menu.setAlpha(1 - (input * 1.1f));
            return super.getInterpolation(input);
        }
    }
}