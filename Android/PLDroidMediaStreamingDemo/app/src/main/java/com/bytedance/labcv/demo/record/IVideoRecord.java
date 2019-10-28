package com.bytedance.labcv.demo.record;

public interface IVideoRecord {
    void start(String videoPath, boolean isTV);
    void stop();
    void release();
}
