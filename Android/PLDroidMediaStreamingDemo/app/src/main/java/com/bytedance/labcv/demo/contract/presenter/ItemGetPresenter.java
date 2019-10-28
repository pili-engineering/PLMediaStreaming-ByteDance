package com.bytedance.labcv.demo.contract.presenter;

import android.content.Context;

import com.bytedance.labcv.demo.contract.ItemGetContract;
import com.bytedance.labcv.demo.model.ButtonItem;
import com.bytedance.labcv.demo.model.ComposerNode;
import com.qiniu.pili.droid.streaming.demo.R;

import java.util.ArrayList;
import java.util.List;

import static com.bytedance.labcv.demo.contract.ItemGetContract.MASK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.NODE_BEAUTY;
import static com.bytedance.labcv.demo.contract.ItemGetContract.NODE_LONG_LEG;
import static com.bytedance.labcv.demo.contract.ItemGetContract.NODE_RESHAPE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.NODE_THIN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.SUB_MASK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY_LONG_LEG;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY_THIN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_SHARPEN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_SMOOTH;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_WHITEN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_CHEEK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_CHIN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_EYE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_EYE_ROTATE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_FACE_CUT;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_FACE_OVERALL;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_FACE_SMALL;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_FOREHEAD;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_JAW;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_MOUTH_SMILE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_MOUTH_ZOOM;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_NOSE_LEAN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_NOSE_LONG;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_CLOSE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_BLUSHER;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_EYEBROW;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_EYELASH;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_EYESHADOW;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_HAIR;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_LIP;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_OPTION;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_PUPIL;

/**
 * Created by QunZhang on 2019-07-21 12:27
 */
public class ItemGetPresenter extends ItemGetContract.Presenter {

    @Override
    public List<ButtonItem> getItems(int type) {
        List<ButtonItem> items = new ArrayList<>();
        switch (type & MASK) {
            case TYPE_BEAUTY_FACE:
                getBeautyFaceItems(items);
                break;
            case TYPE_BEAUTY_RESHAPE:
                getBeautyReshapeItems(items);
                break;
            case TYPE_BEAUTY_BODY:
                getBeautyBodyItems(items);
                break;
            case TYPE_MAKEUP:
                getMakeupItems(items);
                break;
            case TYPE_MAKEUP_OPTION:
                getMakeupOptionItems(items, type);
                break;
        }
        return items;
    }

    private void getBeautyFaceItems(List<ButtonItem> items) {
        Context context = getView().getContext();
        items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_CLOSE)));
        items.add(new ButtonItem(R.drawable.ic_beauty_smooth, context.getString(R.string.beauty_face_smooth), new ComposerNode(TYPE_BEAUTY_FACE_SMOOTH, NODE_BEAUTY, "epm/frag/blurAlpha")));
        items.add(new ButtonItem(R.drawable.ic_beauty_whiten, context.getString(R.string.beauty_face_whiten), new ComposerNode(TYPE_BEAUTY_FACE_WHITEN, NODE_BEAUTY, "epm/frag/whiten")));
        items.add(new ButtonItem(R.drawable.ic_beauty_sharpen, context.getString(R.string.beauty_face_sharpen), new ComposerNode(TYPE_BEAUTY_FACE_SHARPEN, NODE_BEAUTY, "epm/frag/sharpen")));
    }

    private void getBeautyReshapeItems(List<ButtonItem> items) {
        Context context = getView().getContext();
        items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_CLOSE)));
        items.add(new ButtonItem(R.drawable.ic_beauty_cheek_reshape, context.getString(R.string.beauty_reshape_face_overall), new ComposerNode(TYPE_BEAUTY_RESHAPE_FACE_OVERALL, NODE_RESHAPE, "Internal_Deform_Overall")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_face_cut, context.getString(R.string.beauty_reshape_face_cut), new ComposerNode(TYPE_BEAUTY_RESHAPE_FACE_CUT, NODE_RESHAPE, "Internal_Deform_CutFace")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_face_small, context.getString(R.string.beauty_reshape_face_small), new ComposerNode(TYPE_BEAUTY_RESHAPE_FACE_SMALL, NODE_RESHAPE, "Internal_Deform_Face")));
        items.add(new ButtonItem(R.drawable.ic_beauty_eye_reshape, context.getString(R.string.beauty_reshape_eye), new ComposerNode(TYPE_BEAUTY_RESHAPE_EYE, NODE_RESHAPE, "Internal_Deform_Eye")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_eye_rotate, context.getString(R.string.beauty_reshape_eye_rotate), new ComposerNode(TYPE_BEAUTY_RESHAPE_EYE_ROTATE, NODE_RESHAPE, "Internal_Deform_RotateEye")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_cheek, context.getString(R.string.beauty_reshape_cheek), new ComposerNode(TYPE_BEAUTY_RESHAPE_CHEEK, NODE_RESHAPE, "Internal_Deform_Zoom_Cheekbone")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_jaw, context.getString(R.string.beauty_reshape_jaw), new ComposerNode(TYPE_BEAUTY_RESHAPE_JAW, NODE_RESHAPE, "Internal_Deform_Zoom_Jawbone")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_nose_lean, context.getString(R.string.beauty_reshape_nose_lean), new ComposerNode(TYPE_BEAUTY_RESHAPE_NOSE_LEAN, NODE_RESHAPE, "Internal_Deform_Nose")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_nose_long, context.getString(R.string.beauty_reshape_nose_long), new ComposerNode(TYPE_BEAUTY_RESHAPE_NOSE_LONG, NODE_RESHAPE, "Internal_Deform_MovNose")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_chin, context.getString(R.string.beauty_reshape_chin), new ComposerNode(TYPE_BEAUTY_RESHAPE_CHIN, NODE_RESHAPE, "Internal_Deform_Chin")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_forehead, context.getString(R.string.beauty_reshape_forehead), new ComposerNode(TYPE_BEAUTY_RESHAPE_FOREHEAD, NODE_RESHAPE, "Internal_Deform_Forehead")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_mouth_zoom, context.getString(R.string.beauty_reshape_mouth_zoom), new ComposerNode(TYPE_BEAUTY_RESHAPE_MOUTH_ZOOM, NODE_RESHAPE, "Internal_Deform_ZoomMouth")));
        items.add(new ButtonItem(R.drawable.ic_beauty_reshape_mouth_smile, context.getString(R.string.beauty_reshape_mouth_smile), new ComposerNode(TYPE_BEAUTY_RESHAPE_MOUTH_SMILE, NODE_RESHAPE, "Internal_Deform_MouthCorner")));
//        items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.beauty_reshape_decree), new ComposerNode(TYPE_BEAUTY_RESHAPE_DECREE, NODE_RESHAPE_DECREE_DARK, "removePouch")));
//        items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.beauty_reshape_dark_circle), new ComposerNode(TYPE_BEAUTY_RESHAPE_DARK, NODE_RESHAPE_DECREE_DARK, "removeNasolabialFolds")));
    }

    private void getBeautyBodyItems(List<ButtonItem> items) {
        Context context = getView().getContext();
        items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_CLOSE)));
        items.add(new ButtonItem(R.drawable.ic_beauty_body_thin, context.getString(R.string.beauty_body_thin), new ComposerNode(TYPE_BEAUTY_BODY_THIN, NODE_THIN, "")));
        items.add(new ButtonItem(R.drawable.ic_beauty_body_long_leg, context.getString(R.string.beauty_body_long_leg), new ComposerNode(TYPE_BEAUTY_BODY_LONG_LEG, NODE_LONG_LEG, "")));
    }

    private void getMakeupItems(List<ButtonItem> items) {
        Context context = getView().getContext();
        items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_CLOSE)));
        items.add(new ButtonItem(R.drawable.ic_makeup_blusher, context.getString(R.string.makeup_blusher), new ComposerNode(TYPE_MAKEUP_BLUSHER)));
        items.add(new ButtonItem(R.drawable.ic_makeup_lip, context.getString(R.string.makeup_lip), new ComposerNode(TYPE_MAKEUP_LIP)));
        items.add(new ButtonItem(R.drawable.ic_makeup_eyelash, context.getString(R.string.makeup_eyelash), new ComposerNode(TYPE_MAKEUP_EYELASH)));
        items.add(new ButtonItem(R.drawable.ic_makeup_pupil, context.getString(R.string.makeup_pupil), new ComposerNode(TYPE_MAKEUP_PUPIL)));
        items.add(new ButtonItem(R.drawable.ic_makeup_hair, context.getString(R.string.makeup_hair), new ComposerNode(TYPE_MAKEUP_HAIR)));
        items.add(new ButtonItem(R.drawable.ic_makeup_eye, context.getString(R.string.makeup_eye), new ComposerNode(TYPE_MAKEUP_EYESHADOW)));
        items.add(new ButtonItem(R.drawable.ic_makeup_eyebrow, context.getString(R.string.makeup_eyebrow), new ComposerNode(TYPE_MAKEUP_EYEBROW)));
    }

    private void getMakeupOptionItems(List<ButtonItem> items, int type) {
        Context context = getView().getContext();
        switch (type & SUB_MASK) {
            case TYPE_MAKEUP_LIP:
                items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_MAKEUP_LIP)));
                items.add(new ButtonItem(R.drawable.ic_makeup_lip, context.getString(R.string.lip_huluopohong), new ComposerNode(TYPE_MAKEUP_LIP, "lip/huluobohong")));
                items.add(new ButtonItem(R.drawable.ic_makeup_lip, context.getString(R.string.lip_huoliju), new ComposerNode(TYPE_MAKEUP_LIP, "lip/huoliju")));
                items.add(new ButtonItem(R.drawable.ic_makeup_lip, context.getString(R.string.lip_yingsuhong), new ComposerNode(TYPE_MAKEUP_LIP, "lip/yingsuhong")));
                break;
            case TYPE_MAKEUP_BLUSHER:
                items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_MAKEUP_BLUSHER)));
                items.add(new ButtonItem(R.drawable.ic_makeup_blusher, context.getString(R.string.blusher_shaishanghong), new ComposerNode(TYPE_MAKEUP_BLUSHER, "blush/shaishanghong")));
                items.add(new ButtonItem(R.drawable.ic_makeup_blusher, context.getString(R.string.blusher_weixunfen), new ComposerNode(TYPE_MAKEUP_BLUSHER, "blush/weixunfen")));
                items.add(new ButtonItem(R.drawable.ic_makeup_blusher, context.getString(R.string.blusher_yuanqiju), new ComposerNode(TYPE_MAKEUP_BLUSHER, "blush/yuanqiju")));
                break;
            case TYPE_MAKEUP_EYELASH:
                items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_MAKEUP_EYELASH)));
                items.add(new ButtonItem(R.drawable.ic_makeup_eyelash, context.getString(R.string.eyelash_nongmi), new ComposerNode(TYPE_MAKEUP_EYELASH, "eyelash/nongmi")));
                items.add(new ButtonItem(R.drawable.ic_makeup_eyelash, context.getString(R.string.eyelash_shanxing), new ComposerNode(TYPE_MAKEUP_EYELASH, "eyelash/shanxing")));
                items.add(new ButtonItem(R.drawable.ic_makeup_eyelash, context.getString(R.string.eyelash_wumei), new ComposerNode(TYPE_MAKEUP_EYELASH, "eyelash/wumei")));
                break;
            case TYPE_MAKEUP_PUPIL:
                items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_MAKEUP_PUPIL)));
                items.add(new ButtonItem(R.drawable.ic_makeup_pupil, context.getString(R.string.pupil_babizi), new ComposerNode(TYPE_MAKEUP_PUPIL, "pupil/babizi")));
                items.add(new ButtonItem(R.drawable.ic_makeup_pupil, context.getString(R.string.pupil_hunxuelan), new ComposerNode(TYPE_MAKEUP_PUPIL, "pupil/hunxuelan")));
                items.add(new ButtonItem(R.drawable.ic_makeup_pupil, context.getString(R.string.pupil_hunxuelv), new ComposerNode(TYPE_MAKEUP_PUPIL, "pupil/hunxuelv")));
                break;
            case TYPE_MAKEUP_HAIR:
                items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_MAKEUP_HAIR)));
                items.add(new ButtonItem(R.drawable.ic_makeup_hair, context.getString(R.string.hair_anlan), new ComposerNode(TYPE_MAKEUP_HAIR, "hair/anlan")));
                items.add(new ButtonItem(R.drawable.ic_makeup_hair, context.getString(R.string.hair_molv), new ComposerNode(TYPE_MAKEUP_HAIR, "hair/molv")));
                items.add(new ButtonItem(R.drawable.ic_makeup_hair, context.getString(R.string.hair_shenzong), new ComposerNode(TYPE_MAKEUP_HAIR, "hair/shenzong")));
                break;
            case TYPE_MAKEUP_EYESHADOW:
                items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_MAKEUP_EYESHADOW)));
                items.add(new ButtonItem(R.drawable.ic_makeup_eye, context.getString(R.string.eye_shaonvfen), new ComposerNode(TYPE_MAKEUP_EYESHADOW, "eyeshadow/shaonvfen")));
                items.add(new ButtonItem(R.drawable.ic_makeup_eye, context.getString(R.string.eye_yanxunzong), new ComposerNode(TYPE_MAKEUP_EYESHADOW, "eyeshadow/yanxunzong")));
                items.add(new ButtonItem(R.drawable.ic_makeup_eye, context.getString(R.string.eye_ziranlan), new ComposerNode(TYPE_MAKEUP_EYESHADOW, "eyeshadow/ziranlan")));
                break;
            case TYPE_MAKEUP_EYEBROW:
                items.add(new ButtonItem(R.drawable.ic_none, context.getString(R.string.close), new ComposerNode(TYPE_MAKEUP_EYEBROW)));
                items.add(new ButtonItem(R.drawable.ic_makeup_eyebrow, context.getString(R.string.eyebrow_chunhei), new ComposerNode(TYPE_MAKEUP_EYEBROW, "eyebrow/chunhei")));
                items.add(new ButtonItem(R.drawable.ic_makeup_eyebrow, context.getString(R.string.eyebrow_danhui), new ComposerNode(TYPE_MAKEUP_EYEBROW, "eyebrow/danhui")));
                items.add(new ButtonItem(R.drawable.ic_makeup_eyebrow, context.getString(R.string.eyebrow_ziranhei), new ComposerNode(TYPE_MAKEUP_EYEBROW, "eyebrow/ziranhei")));
                break;
        }
    }
}
