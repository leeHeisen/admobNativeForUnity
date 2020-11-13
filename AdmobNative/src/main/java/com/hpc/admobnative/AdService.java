package com.hpc.admobnative;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.UnifiedNativeAd;
import com.google.android.gms.ads.formats.UnifiedNativeAdView;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import static com.google.android.gms.ads.formats.NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE;

public class AdService {
    private Activity activity;
    private UnifiedNativeAd _nativeAd;
    private AdLoadListener adLoadListener;
    private boolean isShowing;
    private NativeAdOptions _nativeAdOptions;
    private UnifiedNativeAdView _nativeAdView;
    final private IAdLoader _adLoader;

    public AdService(final Activity activity, String[] adUnitIds, int numOfAdsToLoad) {
        this.activity = activity;

        _adLoader = new WaterfallAdLoader(adUnitIds, numOfAdsToLoad, new IAdUnitLoader() {
            @Override
            public void load(String unitId, final IAdLoadResult loadResult) {
                AdLoader adLoader = new AdLoader.Builder(activity, unitId)
                        .forUnifiedNativeAd(new UnifiedNativeAd.OnUnifiedNativeAdLoadedListener() {
                            @Override
                            public void onUnifiedNativeAdLoaded(UnifiedNativeAd unifiedNativeAd) {
                                loadResult.onSuccess(unifiedNativeAd);
                            }
                        }).withAdListener(new AdListener() {
                            @Override
                            public void onAdFailedToLoad(LoadAdError loadAdError) {
                                super.onAdFailedToLoad(loadAdError);
                                loadResult.onFail(loadAdError.getMessage());
                            }
                        }).withNativeAdOptions(_nativeAdOptions).build();

                adLoader.loadAd(new AdRequest.Builder().build());
            }
        });
    }

    public void init(OnInitializationCompleteListener listener) {
        MobileAds.initialize(activity, listener);

        VideoOptions videoOptions = new VideoOptions.Builder()
                .setStartMuted(true)
                .build();

        _nativeAdOptions = new NativeAdOptions.Builder()
                .setMediaAspectRatio(NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                .setVideoOptions(videoOptions)
                .build();
    }

    public void load() {
        _adLoader.load(new IAdLoadResult() {
            @Override
            public void onSuccess(UnifiedNativeAd nativeAd) {
                if (_nativeAd != null) {
                    _nativeAd.destroy();
                }
                _nativeAd = nativeAd;

                if (adLoadListener != null) {
                    adLoadListener.onSucceed();
                }
            }

            @Override
            public void onFail(String errorMsg) {
                if (adLoadListener != null) {
                    adLoadListener.onError(errorMsg);
                }
            }
        });
    }

    private void populateUnifiedNativeAdView(UnifiedNativeAd nativeAd, UnifiedNativeAdView adView) {
        MediaView mediaView = adView.findViewById(R.id.ad_media);
        mediaView.setImageScaleType(ImageView.ScaleType.FIT_CENTER);
        adView.setMediaView(mediaView);

        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        adView.getMediaView().setMediaContent(nativeAd.getMediaContent());
        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
//            ((Button)adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        adView.setNativeAd(nativeAd);
        VideoController vc = nativeAd.getVideoController();
        if (vc.hasVideoContent()) {
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                public void onVideoEnd() {
//                    MainActivity.this.onNativeVideoEnd();
                    super.onVideoEnd();
                }
            });
        }
    }

    public void show(final int x, final int y, final int width, final int height) {
        if (_nativeAd == null || isShowing) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (_nativeAdView == null) {
                    View rootView = activity.getLayoutInflater().inflate(R.layout.ad_unified, (ViewGroup) activity.findViewById(android.R.id.content));
                    _nativeAdView = rootView.findViewById(R.id.unified_ad_view);
                }

                populateUnifiedNativeAdView(_nativeAd, _nativeAdView);
                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) _nativeAdView.getLayoutParams();
                layoutParams.width = width;
                layoutParams.height = height;
                layoutParams.bottomMargin = y;
                layoutParams.leftMargin = x;
                _nativeAdView.setLayoutParams(layoutParams);
                _nativeAdView.setVisibility(View.VISIBLE);

                isShowing = true;
            }
        });
    }

    public void hide() {
        if (_nativeAd == null || !isShowing) {
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _nativeAdView.setVisibility(View.INVISIBLE);
                isShowing = false;
            }
        });
    }

    public boolean isReady() {
        return _nativeAd != null;
    }

    public void setAdLoadListener(AdLoadListener adLoadListener) {
        this.adLoadListener = adLoadListener;
    }
}
