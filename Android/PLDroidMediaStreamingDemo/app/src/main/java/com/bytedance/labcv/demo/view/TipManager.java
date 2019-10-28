package com.bytedance.labcv.demo.view;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import com.bytedance.labcv.effectsdk.BefDistanceInfo;
import com.bytedance.labcv.effectsdk.BefHandInfo;
import com.bytedance.labcv.effectsdk.BefPetFaceInfo;
import com.bytedance.labcv.effectsdk.library.LogUtils;

import static com.bytedance.labcv.effectsdk.PetFaceDetect.MAX_PET_FACE_NUM;

/**
 * 结果tip管理器
 */
public class TipManager {
    private static final String TAG = "TipManager";
    private FrameLayout rootContainer;
    private Context mContext;
    private HandInfoTip[] handTips = new HandInfoTip[2];
    private PetFaceInfoTip[] petFaceTips = new PetFaceInfoTip[MAX_PET_FACE_NUM];
    private HumanDistanceTip[] humanDistanceTips = new HumanDistanceTip[10];
    // flag to check whether the hand tips has been added to screen
    private boolean mHandTipsAdded = false;
    private boolean mPetFaceTipsAdded = false;
    private boolean mDisTipsAdded = false;

    public void init(Context context, FrameLayout frameLayout) {
        mContext = context;
        rootContainer = frameLayout;
    }

    /**
     * should setVisibility = INVISIABLE when add the view to screen
     */
    public void addHandTip(){
        if (mHandTipsAdded) return;
        handTips[0] = new HandInfoTip(mContext);
        handTips[0].setVisibility(View.INVISIBLE);
        handTips[1] = new HandInfoTip(mContext);
        handTips[1].setVisibility(View.INVISIBLE);

        rootContainer.addView(handTips[0]);
        rootContainer.addView(handTips[1]);
        mHandTipsAdded = true;
    }


    /**
     * should setVisibility = INVISIABLE when add the view to screen
     */
    public void addPetFaceTip(){
        LogUtils.d("addPetFaceTip");
        if (mPetFaceTipsAdded){
            return;
        }
        for (int i = 0; i < MAX_PET_FACE_NUM; i++){
            petFaceTips[i] = new PetFaceInfoTip(mContext);
            petFaceTips[i].setVisibility(View.INVISIBLE);
            rootContainer.addView(petFaceTips[i]);
        }

        mPetFaceTipsAdded = true;
    }

    public void addDistanceTips(){
        if (mDisTipsAdded) return;
        for (int i = 0; i < 10;i++){
            humanDistanceTips[i] =  new HumanDistanceTip(mContext);
            humanDistanceTips[i].setVisibility(View.GONE);
            rootContainer.addView(humanDistanceTips[i]);
        }
        mDisTipsAdded = true;

    }

    /**
     * remove distance tips out of the screen
     */
    public void removeDistanceTips(){
        if (!mDisTipsAdded) return;
        for (int i = 0; i < 10;i++){
            rootContainer.removeView(humanDistanceTips[i]);
        }
        mDisTipsAdded = false;
    }

    /**
     * remove hand tips out of the screen
     */
    public void removeHandTip(){
        if (!mHandTipsAdded) return;
        rootContainer.removeView(handTips[0]);
        rootContainer.removeView(handTips[1]);
        mHandTipsAdded = false;
    }

    /**
     * remove hand tips out of the screen
     */
    public void removePetFaceTip(){
        if (!mPetFaceTipsAdded) return;
        for (int i = 0; i < 10;i++){
            rootContainer.removeView(petFaceTips[i]);
        }
        mPetFaceTipsAdded = false;
    }


    public void updateHandInfo(BefHandInfo handInfo, int preViewHeight, int previewWidth, int surfaceViewHeight, int surfaceViewWidth) {
        if (!mHandTipsAdded) return;
        if (null ==  handInfo || handInfo.getHandCount() == 0)  {
            handTips[0].setVisibility(View.GONE);
            handTips[1].setVisibility(View.GONE);
            return;
        }
        if (previewWidth == 0 || preViewHeight == 0) return;

        BefHandInfo.BefHand[] hands = handInfo.getHands();
        switch (handInfo.getHandCount()) {
            case 1:
                handTips[0].setVisibility(View.VISIBLE);
                handTips[1].setVisibility(View.GONE);
                handTips[0].updateInfo(hands[0],  preViewHeight,  previewWidth,  surfaceViewHeight,  surfaceViewWidth);

                break;
            case 2:
                handTips[0].setVisibility(View.VISIBLE);
                handTips[1].setVisibility(View.VISIBLE);

                handTips[0].updateInfo(hands[0], preViewHeight,  previewWidth,  surfaceViewHeight,  surfaceViewWidth);
                handTips[1].updateInfo(hands[1], preViewHeight,  previewWidth,  surfaceViewHeight,  surfaceViewWidth);

                break;
        }


    }

    public void updateDistanceInfo(BefDistanceInfo distanceInfo, int preViewHeight, int previewWidth, int surfaceViewHeight, int surfaceViewWidth) {
        if (!mDisTipsAdded) return;
        if (null ==  distanceInfo )  {
            for (HumanDistanceTip tip: humanDistanceTips){
                tip.setVisibility(View.GONE);
            }
            return;
        }
        for (int i = 9;i > (distanceInfo.getFaceCount() -1) ; i--){

            humanDistanceTips[i].setVisibility(View.GONE);
        }
        if (previewWidth == 0 || preViewHeight == 0) return;

        BefDistanceInfo.BefDistance[] results = distanceInfo.getBefDistance();
        for (int i = 0; i < results.length;i++){
            humanDistanceTips[i].setVisibility(View.VISIBLE);
            humanDistanceTips[i].updateInfo(results[i], preViewHeight,  previewWidth,  surfaceViewHeight,  surfaceViewWidth);
        }

    }


    public void updatePetFaceInfo(BefPetFaceInfo petFaceInfo, int preViewHeight, int previewWidth, int surfaceViewHeight, int surfaceViewWidth) {
        if (!mPetFaceTipsAdded) return;
        if (null ==  petFaceInfo )  {
            for (PetFaceInfoTip tip: petFaceTips){
                tip.setVisibility(View.GONE);
            }
            return;
        }
        for (int i = 9;i > (petFaceInfo.getFaceCount() -1) ; i--){

            petFaceTips[i].setVisibility(View.GONE);
        }
        if (previewWidth == 0 || preViewHeight == 0) return;

        BefPetFaceInfo.PetFace[] results = petFaceInfo.getFace90();
        for (int i = 0; i < results.length;i++){
            petFaceTips[i].setVisibility(View.VISIBLE);
            petFaceTips[i].updateInfo(results[i], preViewHeight,  previewWidth,  surfaceViewHeight,  surfaceViewWidth);
        }


    }
}
