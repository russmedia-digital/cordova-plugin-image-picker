#import <Cordova/CDVPlugin.h>


@interface ImagePicker : CDVPlugin < UINavigationControllerDelegate, UIScrollViewDelegate>

@property (copy)   NSString* callbackId;

- (void) getPictures:(CDVInvokedUrlCommand *)command;
- (void) hasReadPermission:(CDVInvokedUrlCommand *)command;
- (void) requestReadPermission:(CDVInvokedUrlCommand *)command;

@property (nonatomic, assign) CGFloat maxWidth;
@property (nonatomic, assign) CGFloat maxHeight;
@property (nonatomic, assign) CGFloat quality;
@property (nonatomic, assign) NSInteger outputType;

@end
