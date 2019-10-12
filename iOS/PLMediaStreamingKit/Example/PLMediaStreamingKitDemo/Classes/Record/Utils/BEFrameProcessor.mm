// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
#import "BEFrameProcessor.h"
#import <CoreMotion/CoreMotion.h>
#import <OpenGLES/ES2/glext.h>
#import "bef_effect_ai_api.h"
#import "bef_effect_ai_yuv_process.h"

#import "BERender.h"
#import <memory>
#import "BEEffectManager.h"
#import "BEVideoRecorderViewController.h"

@implementation BEProcessResult
@end

@interface BEFrameProcessor() {
    
    EAGLContext *_glContext;
    
    BOOL                    _effectOn;
    BEEffectManager         *_effectManager;
    BERender                *_render;
    
    unsigned char * pixel_buff_pointer;
    unsigned char * buff_out_pointer;
}

@end

@implementation BEFrameProcessor

/**
 * license有效时间2019-03-01到2019-04-30
 * license只是为了追踪使用情况，可以随时申请无任何限制license
 */

static NSString * LICENSE_PATH = @"/labcv_test_20190920_20191022_com.bytedance.labcv.demo_labcv_test_v3.0.0.licbag";



- (instancetype)initWithContext:(EAGLContext *)context videoSize:(CGSize)size {
    self = [super init];
    if (self) {
        _glContext = context;
        _videoDimensions = size;
        
        pixel_buff_pointer = NULL;
        buff_out_pointer = NULL;
        
        _effectOn = YES;
        
        _effectManager = [[BEEffectManager alloc] init];
        _render = [[BERender alloc] init];

        [self _setupEffectSDK];
    }
    return self;
}

/*
 * 初始化SDK
 */
- (void)_setupEffectSDK {
    [_effectManager setupEffectMangerWithLicenseVersion:LICENSE_PATH];
}

- (void)_releaseSDK {
    // 要在opengl上下文中调用
    [_effectManager releaseEffectManager];
}

- (void)reset {
    NSLog(@"BEFrameProcessor reset");
    [self _releaseSDK];
    [self _setupEffectSDK];
}

- (void)dealloc {
    NSLog(@"BEFrameProcessor dealloc %@", NSStringFromSelector(_cmd));
    [EAGLContext setCurrentContext:_glContext];
    [self _releaseSDK];
}

/*
 * 帧处理流程
 */
- (BEProcessResult *)process:(CVPixelBufferRef)pixelBuffer timeStamp:(double)timeStamp{
    BEProcessResult *result = [[BEProcessResult alloc] init];
    CVPixelBufferLockBaseAddress(pixelBuffer, 0);
    unsigned char *baseAddress = (unsigned char *) CVPixelBufferGetBaseAddress(pixelBuffer);
    int iBytesPerRow = (int) CVPixelBufferGetBytesPerRow(pixelBuffer);
    int iWidth = (int) CVPixelBufferGetWidth(pixelBuffer);
    int iHeight = (int) CVPixelBufferGetHeight(pixelBuffer);

    //设置后续美颜以及其他识别功能的基本参数
    [_effectManager setWidth:iWidth height:iHeight orientation:[self getDeviceOrientation]];

    size_t iTop, iBottom, iLeft, iRight;
    CVPixelBufferGetExtendedPixels(pixelBuffer, &iLeft, &iRight, &iTop, &iBottom);

    iWidth = iWidth + (int) iLeft + (int) iRight;
    iHeight = iHeight + (int) iTop + (int) iBottom;
    iBytesPerRow = iBytesPerRow + (int) iLeft + (int) iRight;
    
    baseAddress = [self preProcessBuffer:baseAddress width:iWidth height:iHeight bytePerRow:iBytesPerRow];
    
    // 设置 OpenGL 环境 , 需要与初始化 SDK 时一致
    if ([EAGLContext currentContext] != _glContext) {
        [EAGLContext setCurrentContext:_glContext];
    }
    GLuint textureResult;
    if (_effectOn) {
        //为美颜，瘦脸，滤镜分配输出纹理
        [_effectManager genInputAndOutputTexture:baseAddress width:iWidth height:iHeight];
        //美颜，瘦脸，滤镜的渲染， 返回渲染后的纹理
        textureResult = [_effectManager processInputTexture:timeStamp];
    } else {
        textureResult = [_effectManager genOutputTexture:baseAddress width:iWidth height:iHeight];
    }
   
    

//    result.texture = textureResult;
    result.pixelBuffer = [self transforTextureToCVPixelBuffer:textureResult pixelBuffer:pixelBuffer width:iWidth height:iHeight bytesPerRow:iBytesPerRow];
    result.size  = CGSizeMake(iWidth, iHeight);
    glDeleteTextures(1, &textureResult);
    CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
    return result;
}

- (unsigned char *)preProcessBuffer:(unsigned char *)buffer width:(int)iWidth height:(int)iHeight bytePerRow:(int)iBytesPerRow {
    if(pixel_buff_pointer==NULL){
        pixel_buff_pointer = (unsigned char*)malloc(iWidth*iHeight*4*(sizeof(unsigned char)));
    }
    if (iBytesPerRow == iWidth * 4) {
        return buffer;
    }
    unsigned char* to = pixel_buff_pointer;
    unsigned char* from = buffer;
    for(int i =0; i<iHeight; i++) {
        memcpy(to, from, iBytesPerRow*sizeof(unsigned char));
        to = to  + iWidth*4;
        from = from +iBytesPerRow;
    }
    return pixel_buff_pointer;
}

- (unsigned char *)transforTextureToRawData:(GLuint)texture width:(int)iWidth height:(int)iHeight {
    if(buff_out_pointer == NULL) {
        buff_out_pointer = (unsigned char *)malloc(iWidth * iHeight * 4 * sizeof(unsigned char));
    }
    [_render transforTextureToImage:texture buffer:buff_out_pointer width:iWidth height:iHeight format:GL_RGBA];
    return buff_out_pointer;
}

- (CVPixelBufferRef)transforTextureToCVPixelBuffer:(GLuint)texture pixelBuffer:(CVPixelBufferRef)pixelBuffer width:(int)width height:(int)height bytesPerRow:(int)bytesPerRow {
    if(buff_out_pointer == NULL) {
        buff_out_pointer = (unsigned char *)malloc(width * height * 4);
    }
    [_render transforTextureToImage:texture buffer:buff_out_pointer width:width height:height format:GL_BGRA];
    unsigned char *baseAddres = (unsigned char *)CVPixelBufferGetBaseAddress(pixelBuffer);
    unsigned char *from = buff_out_pointer;
    int realBytesPerRow = width * 4;
    if (bytesPerRow == realBytesPerRow) {
        memcpy(baseAddres, from, realBytesPerRow * height);
    } else {
        for (int i = 0; i < height; i++) {
            memcpy(baseAddres, from, realBytesPerRow);
            baseAddres += bytesPerRow;
            from += realBytesPerRow;
        }
    }
    return pixelBuffer;
}

- (long) currentTimeInMillis {
    return [[NSDate date] timeIntervalSince1970] * 1000;
}

//- (void)setIndensity:(float)intensity type:(BEEffectFaceBeautyType)type {
//    [_effectManager setIntensityWithType:type intensity:intensity];
//}

/*
 * 设置滤镜强度
 */
-(void)setFilterIntensity:(float)intensity{
    [_effectManager setFilterIntensity:intensity];
}

/*
 * 设置贴纸资源
 */
- (void)setStickerPath:(NSString *)path{
    [_effectManager setStickerPath:path];
}

- (void)updateComposerNodes:(NSArray<NSNumber *> *)nodes {
    [_effectManager updateComposerNodes:nodes];
}

- (void)updateComposerNodeIntensity:(BEEffectNode)node intensity:(CGFloat)intensity {
    [_effectManager updateComposerNodeIntensity:node intensity:intensity];
}

/*
 * 设置license
 */
- (void) setRenderLicense:(NSString *)license{
    [_effectManager setEffectMangerLicense:license];
}
/*
 * 设置滤镜资源路径和系数
 */
- (void)setFilterPath:(NSString *)path {
    [_effectManager setFilterPath:path];
}

/*
 * 获取设备旋转角度
 */
- (int)getDeviceOrientation {
    UIDeviceOrientation orientation = [[UIDevice currentDevice] orientation];
    switch (orientation) {
        case UIDeviceOrientationPortrait:
            return BEF_AI_CLOCKWISE_ROTATE_0;

        case UIDeviceOrientationPortraitUpsideDown:
            return BEF_AI_CLOCKWISE_ROTATE_180;

        case UIDeviceOrientationLandscapeLeft:
            return BEF_AI_CLOCKWISE_ROTATE_270;

        case UIDeviceOrientationLandscapeRight:
            return BEF_AI_CLOCKWISE_ROTATE_90;

        default:
            return BEF_AI_CLOCKWISE_ROTATE_0;
    }
}


- (void)setEffectOn:(BOOL)on
{
    _effectOn = on;
}

#pragma mark - 特效相关功能设置
/*
 * 重新切换回美颜特效的状态，与贴纸分离
 */
- (void)effectManagerSetInitalStatus{
    [_effectManager initEffectCompose];
}

@end

