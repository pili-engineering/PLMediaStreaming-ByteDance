package com.bytedance.labcv.demo.contract.presenter;

import android.util.SparseArray;

import com.bytedance.labcv.demo.contract.EffectContract;
import com.bytedance.labcv.demo.model.ComposerNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.bytedance.labcv.demo.contract.ItemGetContract.MASK;
import static com.bytedance.labcv.demo.contract.ItemGetContract.TYPE_MAKEUP_OPTION;

/**
 * Created by QunZhang on 2019-07-22 13:57
 */
public class EffectPresenter extends EffectContract.Presenter {

    @Override
    public void removeNodesOfType(SparseArray<ComposerNode> composerNodeMap, int type) {
        removeNodesWithMakAndType(composerNodeMap, MASK, type & MASK);
    }

    private void removeNodesWithMakAndType(SparseArray<ComposerNode> map, int mask, int type) {
        int i = 0;
        ComposerNode node;
        while ((map.valueAt(i) instanceof ComposerNode) && (node = map.valueAt(i)) != null) {
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

    private boolean isAhead(ComposerNode node) {
        return (node.getId() & MASK) == TYPE_MAKEUP_OPTION;
    }
}
