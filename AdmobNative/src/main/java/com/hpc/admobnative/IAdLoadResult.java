package com.hpc.admobnative;

import com.google.android.gms.ads.formats.UnifiedNativeAd;

public interface IAdLoadResult {
    void onSuccess(UnifiedNativeAd nativeAd);
    void onFail(String errorMsg);
}
