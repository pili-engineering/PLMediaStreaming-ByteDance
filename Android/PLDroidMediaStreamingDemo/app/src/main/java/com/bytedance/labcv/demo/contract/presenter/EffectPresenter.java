package com.bytedance.labcv.demo.contract.presenter;

import android.annotation.SuppressLint;
import android.util.SparseArray;

import com.bytedance.labcv.demo.contract.EffectContract;
import com.bytedance.labcv.demo.contract.ItemGetContract;
import com.bytedance.labcv.demo.model.ButtonItem;
import com.bytedance.labcv.demo.model.ComposerNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bytedance.labcv.demo.contract.ItemGetContract.MASK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY_LONG_LEG;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_BODY_THIN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_SHARPEN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_SMOOTH;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_WHITEN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_BRIGHTEN_EYE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_REMOVE_POUCH;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_SMILE_FOLDS;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_FACE_WHITEN_TEETH;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_CHEEK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_CHIN;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_DARK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_DECREE;
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
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_EYE_SPACING;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_EYE_MOVE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_BEAUTY_RESHAPE_MOUTH_MOVE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_CLOSE;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_FILTER;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_BLUSHER;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_EYEBROW;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_EYESHADOW;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_FACIAL;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_HAIR;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_LIP;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_OPTION;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_PUPIL;

/**
 * Created by QunZhang on 2019-07-22 13:57
 */
public class EffectPresenter extends EffectContract.Presenter {
    public static final Map<Integer, Float> DEFAULT_VALUE;
    static {
        @SuppressLint("UseSparseArrays") Map<Integer, Float> map = new HashMap<>();
        // 美颜
        // beauty face
        map.put(TYPE_BEAUTY_FACE_SMOOTH, 0.6F);
        map.put(TYPE_BEAUTY_FACE_WHITEN, 0.3F);
        map.put(TYPE_BEAUTY_FACE_SHARPEN, 0.7F);
        map.put(TYPE_BEAUTY_FACE_BRIGHTEN_EYE, 0.0F);
        map.put(TYPE_BEAUTY_FACE_REMOVE_POUCH, 0.0F);
        map.put(TYPE_BEAUTY_FACE_SMILE_FOLDS, 0.0F);
        map.put(TYPE_BEAUTY_FACE_WHITEN_TEETH, 0.0F);
        // 美型
        // beaury reshape
        map.put(TYPE_BEAUTY_RESHAPE_FACE_OVERALL, 0.5F);
        map.put(TYPE_BEAUTY_RESHAPE_FACE_SMALL, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_FACE_CUT, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_EYE, 0.3F);
        map.put(TYPE_BEAUTY_RESHAPE_EYE_ROTATE, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_CHEEK, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_JAW, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_NOSE_LEAN, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_NOSE_LONG, 0.25F);
        map.put(TYPE_BEAUTY_RESHAPE_CHIN, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_FOREHEAD, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_MOUTH_ZOOM, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_MOUTH_SMILE, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_DECREE, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_DARK, 0.5F);
        map.put(TYPE_BEAUTY_RESHAPE_EYE_SPACING, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_EYE_MOVE, 0.0F);
        map.put(TYPE_BEAUTY_RESHAPE_MOUTH_MOVE, 0.0F);
        // 美体
        map.put(TYPE_BEAUTY_BODY_THIN, 0.4f);
        map.put(TYPE_BEAUTY_BODY_LONG_LEG, 0.4f);
        // 美妆
        map.put(TYPE_MAKEUP_LIP, 0.3F);
        map.put(TYPE_MAKEUP_HAIR, 0.5F);
        map.put(TYPE_MAKEUP_BLUSHER, 0.3F);
        map.put(TYPE_MAKEUP_FACIAL, 0.3F);
        map.put(TYPE_MAKEUP_EYEBROW, 0.3F);
        map.put(TYPE_MAKEUP_EYESHADOW, 0.4F);
        map.put(TYPE_MAKEUP_PUPIL, 0.4F);
        // 滤镜
        // filter
        map.put(TYPE_FILTER, 0.8F);
        DEFAULT_VALUE = Collections.unmodifiableMap(map);
    }

    private ItemGetContract.Presenter mItemGet;

    @Override
    public void removeNodesOfType(SparseArray<ComposerNode> composerNodeMap, int type) {
        removeNodesWithMakAndType(composerNodeMap, MASK, type & MASK);
    }

    @Override
    public void removeProgressInMap(SparseArray<Float> map, int type) {
        List<Integer> nodeToRemove = new ArrayList<>(map.size());
        for (int i = 0; i < map.size(); i++) {
            int key = map.keyAt(i);
            if ((key & MASK) == type) {
                nodeToRemove.add(key);
            }
        }
        for (Integer i : nodeToRemove) {
            map.remove(i);
        }
    }

    private void removeNodesWithMakAndType(SparseArray<ComposerNode> map, int mask, int type) {
        int i = 0;
        ComposerNode node;
        while (i < map.size() && (node = map.valueAt(i)) != null) {
            if ((node.getId() & mask) == type) {
                map.removeAt(i);
            } else {
                i++;
            }
        }
    }

    @Override
    public String[] generateComposerNodes(SparseArray<ComposerNode> composerNodeMap) {
        List<String> list = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < composerNodeMap.size(); i++) {
            ComposerNode node = composerNodeMap.valueAt(i);
            if (set.contains(node.getNode())) {
                continue;
            } else {
                set.add(node.getNode());
            }
            if (isAhead(node)) {
                list.add(0, node.getNode());
            } else {
                list.add(node.getNode());
            }
        }

        return list.toArray(new String[0]);
    }

    @Override
    public void generateDefaultBeautyNodes(SparseArray<ComposerNode> composerNodeMap) {
        if (mItemGet == null) {
            mItemGet = new ItemGetPresenter();
            mItemGet.attachView(getView());
        }
        List<ButtonItem> beautyItems = new ArrayList<>();
        beautyItems.addAll(mItemGet.getItems(TYPE_BEAUTY_FACE));
        beautyItems.addAll(mItemGet.getItems(TYPE_BEAUTY_RESHAPE));

        for (ButtonItem item : beautyItems) {
            if (item.getNode().getId() == TYPE_CLOSE) {
                continue;
            }
            item.getNode().setValue(DEFAULT_VALUE.get(item.getNode().getId()));
            composerNodeMap.put(item.getNode().getId(), item.getNode());
        }
    }

    @Override
    public float getDefaultValue(int type) {
        return DEFAULT_VALUE.get(type);
    }

    @Override
    public boolean hasIntensity(int type) {
        int parent = type & MASK;
        return parent == TYPE_BEAUTY_FACE || parent == TYPE_BEAUTY_RESHAPE ||
                parent == TYPE_BEAUTY_BODY || parent == TYPE_MAKEUP || parent == TYPE_MAKEUP_OPTION;
    }

    private boolean isAhead(ComposerNode node) {
        return (node.getId() & MASK) == TYPE_MAKEUP_OPTION;
    }
}
