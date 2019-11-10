package com.androidwind.tinypull2refresh;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * @author ddnosh
 * @website http://blog.csdn.net/ddnosh
 */
public class MainActivity extends AppCompatActivity implements Pull2RefreshLayout.Pull2RefreshListener {

    private Pull2RefreshLayout p2r;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        p2r = findViewById(R.id.p2r);
        p2r.setPullToRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                p2r.stopRefreshing();
            }
        }, 2000);
    }
}
