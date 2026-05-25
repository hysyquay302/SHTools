package com.quayquay.shtools.extention;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class ActivityVisibilityObserver implements LifecycleObserver {

    private boolean isActivityStarted = false;

    public boolean isActivityStarted() {
        return isActivityStarted;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        isActivityStarted = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        isActivityStarted = false;
    }
}
