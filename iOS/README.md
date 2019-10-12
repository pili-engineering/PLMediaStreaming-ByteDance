# PLMediaStreaming-ByteDance

注意！！！此项目中不含资源文件，如有需要，请联系字节获取。

### ios 端接入指南

#### 1. 接入准备

##### 1.1 推流方

在推流上接入美颜依赖于 PLMediaStreamingSession 的 delegate PLMediaStreamingSessionDelegate，用户需要实现 delegate 如下方法：

```objective-c
/// @abstract 获取到摄像头原数据时的回调, 便于开发者做滤镜等处理，需要注意的是这个回调在 camera 数据的输出线程，请不要做过于耗时的操作，否则可能会导致推流帧率下降
- (CVPixelBufferRef)mediaStreamingSession:(PLMediaStreamingSession *)session cameraSourceDidGetPixelBuffer:(CVPixelBufferRef)pixelBuffer;
```

这个函数提供了 CVPixelBufferRef 参数，这个便是相机输出的未经处理的数据（如果没有使用七牛云的本地渲染，则需要使用其他方法，将相机捕获的原始数据暴露出来），然后返回参数也是一个 CVPixelBufferRef ，这个则是经过第三方渲染之后。

##### 1.2 美颜方

接入美颜之前需要将我们的库文件和头文件加入项目，并将对 sdk 的一些封装代码拷贝到项目中，这些代码一般存在于 utils、models 和 Manager 文件夹下。

美颜调用的接口为 BEFrameProcessor 类，所以在美颜处理之前，需要先进行 BEFrameProcessor 的初始化，如果本地之前已经有 BEFrameProcessor 的初始化需要两个参数：

```objective-c
- (instancetype)initWithContext:(EAGLContext *)context videoSize:(CGSize)size;
```

第一个参数为 OpenGL 上下文，如果本地已经有 OpenGL 渲染相关的代码，可以直接使用已有的上下文，如果本地没有，可以使用如下代码创建一个：

```objective-c
EAGLContext *context = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
[EAGLContext setCurrentContext:context];
```

第二个参数就是需要处理的视频大小。

##### 1.3 素材

使用美颜 sdk 之前需要拿到功能授权文件，以及与授权文件对应的素材。

#### 2. 推流过程中加入美颜渲染

渲染对应的函数为 BEFrameProcessor#process ，这个函数就是放在 PLMediaStreamingSessionDelegate 的 mediaStreamingSession:cameraSourceDidGetPixelBuffer 回调方法中调用的，需要两个参数：

```objective-c
- (BEProcessResult *)process:(CVPixelBufferRef)pixelBuffer timeStamp:(double)timeStamp;
```

渲染函数需要两个参数，pixelBuffer 即为相机捕获的数据，timeStamp 是一个时间戳，如果能够直接获得相机的原始数据 CMSampleBufferRef ，可以通过如下代码获取：

```objective-c
CMTime sampleTime = CMSampleBufferGetPresentationTimeStamp(sampleBuffer);
double timeStamp = (double)sampleTime.value/sampleTime.timescale;
```

如果没有，则可以直接使用当前的系统时间。

返回参数为 BEProcessResult，表示 process 函数的处理结果，根据需要，它可以返回纹理、原始数据或者 CVPixelBufferRef，对应代码如下：

```objective-c
- (BEProcessResult *)process:(CVPixelBufferRef)pixelBuffer timeStamp:(double)timeStamp{
    BEProcessResult *result = [[BEProcessResult alloc] init];
    CVPixelBufferLockBaseAddress(pixelBuffer, 0);
    unsigned char *baseAddress = (unsigned char *) CVPixelBufferGetBaseAddress(pixelBuffer);
    int bytesPerRow = (int) CVPixelBufferGetBytesPerRow(pixelBuffer);
    int width = (int) CVPixelBufferGetWidth(pixelBuffer);
    int height = (int) CVPixelBufferGetHeight(pixelBuffer);

    //设置后续美颜以及其他识别功能的基本参数
    [_effectManager setWidth:width height:height orientation:[self getDeviceOrientation]];

    size_t iTop, iBottom, iLeft, iRight;
    CVPixelBufferGetExtendedPixels(pixelBuffer, &iLeft, &iRight, &iTop, &iBottom);

    width = width + (int) iLeft + (int) iRight;
    height = height + (int) iTop + (int) iBottom;
    bytesPerRow = bytesPerRow + (int) iLeft + (int) iRight;
    
    baseAddress = [self preProcessBuffer:baseAddress width:width height:height bytePerRow:bytesPerRow];
    
    // 设置 OpenGL 环境 , 需要与初始化 SDK 时一致
    if ([EAGLContext currentContext] != _glContext) {
        [EAGLContext setCurrentContext:_glContext];
    }
    GLuint textureResult;
    if (_effectOn) {
        //为美颜，瘦脸，滤镜分配输出纹理
        [_effectManager genInputAndOutputTexture:baseAddress width:width height:height];
        //美颜，瘦脸，滤镜的渲染， 返回渲染后的纹理
        textureResult = [_effectManager processInputTexture:timeStamp];
    } else {
        textureResult = [_effectManager genOutputTexture:baseAddress width:width height:height];
    }

    CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
    result.texture = textureResult;
//    result.rawData = [self transforTextureToRawData:textureResult width:width height:height];
//    result.pixelBuffer = [self transforTextureToCVPixelBuffer:textureResult pixelBuffer:pixelBuffer width:width height:height bytesPerRow:bytesPerRow];
//    glDeleteTextures(1, textureResult);
    result.size  = CGSizeMake(width, height);
    return result;
}
```

看最后几行，默认返回的是纹理，如果需要返回另外一种类型的数据，可以将对应的行的注释取消，注意，如果不需要使用纹理了，务必调用`glDeleteTextures(1, textureResult)`将纹理删除。

比如七牛云，对应的代码应该设置为：

```objective-c
result.pixelBuffer = [self transforTextureToCVPixelBuffer:textureResult pixelBuffer:pixelBuffer width:width height:height bytesPerRow:bytesPerRow];
glDeleteTextures(1, textureResult);
result.size  = CGSizeMake(width, height);
return result;
```

然后在调用方使用处理之后的 pixelBuffer 即可。

#### 3. 设置美颜参数

设置美颜参数也是使用 BEFrameProcessor 提供的函数，比如 setFilterPath 用于设置滤镜路径，setFilterIntensity 用于设置滤镜强度，更多的使用可以参考我们的 demo 里的做法。如果在接入的过程中出现不符合预期的问题，可以参考 [错误码对照表](http://ailab-cv-sdk.bytedance.com/docs/2036/17918/) 。