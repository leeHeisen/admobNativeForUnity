package com.hpc.admobnativeunity;

import android.os.Bundle;

import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.hpc.admobnative.AdLoadListener;
import com.hpc.admobnative.AdService;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private AdService _adService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _adService = new AdService(this, new String[] {
                "ca-app-pub-3940256099942544/2247696110",
                "ca-app-pub-3940256099942544/1044960115",
        }, 5);

        _adService.init(new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });

        _adService.setAdLoadListener(new AdLoadListener() {
            @Override
            public void onError(String errorMsg) {

            }

            @Override
            public void onSucceed() {
            }
        });
    }

    public void onShowBtnClick(android.view.View view) {
        _adService.show(0, 0, 945, 700);
    }

    public void onLoadBtnClick(android.view.View view) {
        _adService.load();
    }

    public void onHideBtnClick(android.view.View view) {
        _adService.hide();
    }
}
