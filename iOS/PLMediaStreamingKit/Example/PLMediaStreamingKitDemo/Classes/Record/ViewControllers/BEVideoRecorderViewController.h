// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.

#import <UIKit/UIKit.h>


@protocol BETapDelegate <NSObject>

- (void)onTap;

@end

@protocol BEDefaultTapDelegate <NSObject>

- (void)onDefaultTap;

@end

@interface BEVideoRecorderViewController : UIViewController

- (void)initProcessor;

- (CVPixelBufferRef)process:(CVPixelBufferRef)pixelBuffer timeStamp:(double)timeStamp;

@end

