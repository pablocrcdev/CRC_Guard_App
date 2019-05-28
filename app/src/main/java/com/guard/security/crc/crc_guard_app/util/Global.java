package com.guard.security.crc.crc_guard_app.util;

import android.app.Application;

public class Global extends Application {

    private String someVariable;

    public String getSomeVariable() {
        return someVariable;
    }

    public void setSomeVariable(String someVariable) {
        this.someVariable = someVariable;
    }
}
