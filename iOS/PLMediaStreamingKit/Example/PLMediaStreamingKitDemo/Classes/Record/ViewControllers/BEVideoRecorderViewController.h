// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.

#import <UIKit/UIKit.h>

@interface BEVideoRecorderViewController : UIViewController

- (void)initProcessor:(CGSize)videoSize;

- (CVPixelBufferRef)process:(CVPixelBufferRef)pixelBuffer timeStamp:(double)timeStamp;

@end

