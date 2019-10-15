package com.qiniu.pili.droid.streaming.effect;

/**
 * 定义一个回调接口，用于当用户选择其中一个面板时，
 * 关闭其他面板的回调，此接口由各 Fragment 实现，
 * 在 onClose() 方法中要完成各 Fragment 中 UI 的初始化，
 * 即关闭用户已经开启的开关
 *
 * Define a callback interface for when a user selects one of the panels，
 * close the callback of the other panel, which is implemented by each Fragment
 * In the onClose() method, initialize the UI of each Fragment:
 * turn off the switch that the user has already turned on
 */
public interface OnCloseListener {
    void onClose();
}
