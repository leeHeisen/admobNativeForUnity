package com.hpc.admobnative;

import com.google.android.gms.ads.formats.UnifiedNativeAd;

import java.util.HashMap;
import java.util.Map;

public class WaterfallAdLoader implements IAdLoader {
    final private String[] _unitIds;
    final private int _numOfAdsToLoad;
    final private IAdUnitLoader _adLoader;

    private int _maxPriceUnitIdIndex;
    private int _loadStartIndex;
    private int _loadEndIndex;
    private int _curUnitIdIndex;

    private int _successNum;
    private int _failureNum;
    private boolean _isLoading;
    private Map<String, String> _unitId2ErrorMsg;
    private Map<String, UnifiedNativeAd> _index2NativeAd;

    public WaterfallAdLoader(String[] unitIds, int numOfAdsToLoad, IAdUnitLoader adLoader) {
        _unitIds = unitIds;
        _numOfAdsToLoad = numOfAdsToLoad;
        _adLoader = adLoader;
        _unitId2ErrorMsg = new HashMap<String, String>();
        _maxPriceUnitIdIndex = _unitIds.length;
        _index2NativeAd = new HashMap<String, UnifiedNativeAd>();
        _curUnitIdIndex = _unitIds.length - 1;
        resetLoadRange();
    }

    @Override
    public void load(final IAdLoadResult adLoadResult) {
        if (_isLoading) {
            return;
        }

        _isLoading = true;

        for (int i = _loadStartIndex; i <= _loadEndIndex; i++) {
            final int finalI = i;
            final String unitId = _unitIds[i];

            _adLoader.load(unitId, new IAdLoadResult() {
                @Override
                public void onSuccess(UnifiedNativeAd nativeAd) {
                    if (_maxPriceUnitIdIndex > finalI) {
                        _maxPriceUnitIdIndex = finalI;
                    }

                    _index2NativeAd.put(Integer.toString(finalI), nativeAd);
                    ++_successNum;
                    checkForComplete(adLoadResult);
                }

                @Override
                public void onFail(String errorMsg) {
                    ++_failureNum;
                    _unitId2ErrorMsg.put(unitId, errorMsg);
                    checkForComplete(adLoadResult);
                }
            });
        }
    }

    void reset() {
        _successNum = 0;
        _failureNum = 0;
        _isLoading = false;
        _maxPriceUnitIdIndex = _unitIds.length;
    }

    void resetLoadRange() {
        _loadStartIndex = 0;
        _loadEndIndex = Math.min(_numOfAdsToLoad - 1, _curUnitIdIndex);
    }

    void checkForComplete(IAdLoadResult adLoadResult) {
        if ((_successNum + _failureNum) != (_loadEndIndex - _loadStartIndex + 1)) {
            return;
        }

        if (_successNum > 0) {
            _curUnitIdIndex = _maxPriceUnitIdIndex;
            reset();
            resetLoadRange();

            String indexStr = Integer.toString(_curUnitIdIndex);
            if (adLoadResult != null) {
                adLoadResult.onSuccess(_index2NativeAd.get(indexStr));
            }

            for (Map.Entry<String, UnifiedNativeAd> entry : _index2NativeAd.entrySet()) {
                if (entry.getKey() == indexStr) {
                    continue;
                }
                entry.getValue().destroy();
            }
            _index2NativeAd.clear();
            return;
        }

        reset();

        _loadStartIndex = _loadEndIndex + 1;
        if (_loadStartIndex > _curUnitIdIndex) {
            resetLoadRange();
            if (adLoadResult != null) {
                adLoadResult.onFail(_unitId2ErrorMsg.toString());
            }
            _unitId2ErrorMsg.clear();
            return;
        }

        _loadEndIndex = Math.min(_loadStartIndex + _numOfAdsToLoad, _curUnitIdIndex);
        load(adLoadResult);
    }
}

