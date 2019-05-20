package com.guard.security.crc.crc_guard_app.model;

import android.app.Application;

public class GlobalVariables extends Application {


    private GlobalVariables Instance = new GlobalVariables();

    public GlobalVariables getInstance() {
        return Instance;
    }

    public void setInstance(GlobalVariables instance) {
        Instance = instance;
    }

    private String mUrl;

    public GlobalVariables() {

    }

    public String getmUrl() {
        return mUrl;
    }

    public void setmUrl(String mUrl) {
        if (this.mUrl == null) {
            this.mUrl = mUrl;
        }
    }

}
