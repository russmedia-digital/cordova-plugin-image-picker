#import "ImagePicker.h"
#import <Photos/Photos.h>
#import <PhotosUI/PhotosUI.h>

#define FILE_URI 0
#define BASE64_STRING 1

@interface ImagePicker (UIImagePickerControllerDelegate) <UINavigationControllerDelegate, UIImagePickerControllerDelegate>
@end

#if __has_include(<PhotosUI/PHPicker.h>)
@interface ImagePicker (PHPickerViewControllerDelegate) <PHPickerViewControllerDelegate>
@end
#endif

@implementation ImagePicker

@synthesize callbackId;

- (void) hasReadPermission:(CDVInvokedUrlCommand *)command {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:[PHPhotoLibrary authorizationStatus] == PHAuthorizationStatusAuthorized];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) requestReadPermission:(CDVInvokedUrlCommand *)command {
    PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
    if (status == PHAuthorizationStatusAuthorized) {
        NSLog(@"Access has been granted.");
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else if (status == PHAuthorizationStatusDenied) {
        NSString* message = @"Access has been denied. Change your setting > this app > Photo enable";
        NSLog(@"%@", message);
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else if (status == PHAuthorizationStatusNotDetermined) {
        // Access has not been determined. requestAuthorization: is available
        [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {}];
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    } else if (status == PHAuthorizationStatusRestricted) {
        NSString* message = @"Access has been restricted. Change your setting > Privacy > Photo enable";
        NSLog(@"%@", message);
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void) getPictures:(CDVInvokedUrlCommand *)command {
    NSDictionary *options = [command.arguments objectAtIndex: 0];
    NSInteger maximumImagesCount = [[options objectForKey:@"maximumImagesCount"] integerValue];
    self.outputType = [[options objectForKey:@"outputType"] integerValue];
    self.maxWidth = [[options objectForKey:@"width"] floatValue];
    self.maxHeight = [[options objectForKey:@"height"] floatValue];
    self.quality = [[options objectForKey:@"quality"] floatValue] / 100;
    self.callbackId = command.callbackId;

    #if __has_include(<PhotosUI/PHPicker.h>)
        if (@available(iOS 14, *)) {
            PHPhotoLibrary *photoLibrary = [PHPhotoLibrary sharedPhotoLibrary];
            PHPickerConfiguration *configuration = [[PHPickerConfiguration alloc] initWithPhotoLibrary:photoLibrary];
            configuration.filter = [PHPickerFilter imagesFilter];
            configuration.selectionLimit = maximumImagesCount;

            PHPickerViewController *picker = [[PHPickerViewController alloc] initWithConfiguration:configuration];
            picker.delegate = self;
            [self checkPhotosPermissions:^(BOOL granted) {
                if (granted) {
                    [self.viewController presentViewController:picker animated:YES completion:nil];
                } else {
                    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Access has been denied. Change your setting > this app > Photo enable"];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                }
            }];
            return;
        }
    #endif

    UIImagePickerController *picker = [[UIImagePickerController alloc] init];
    picker.sourceType = UIImagePickerControllerSourceTypePhotoLibrary;
    picker.delegate = self;
    [self checkPhotosPermissions:^(BOOL granted) {
        if (granted) {
            [self.viewController presentViewController:picker animated:YES completion:nil];
        } else {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Access has been denied. Change your setting > this app > Photo enable"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
}

#pragma mark - Helpers

- (void)checkPhotosPermissions:(void(^)(BOOL granted))callback
{
    PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
    if (status == PHAuthorizationStatusAuthorized) {
        callback(YES);
        return;
    } else if (status == PHAuthorizationStatusNotDetermined) {
        [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
            if (status == PHAuthorizationStatusAuthorized) {
                callback(YES);
                return;
            }
            else {
                callback(NO);
                return;
            }
        }];
    }
    else {
        callback(NO);
    }
}

- (NSString*) getFileType:(NSData *)imageData
{
    const uint8_t firstByteJpg = 0xFF;
    const uint8_t firstBytePng = 0x89;
    const uint8_t firstByteGif = 0x47;
    
    uint8_t firstByte;
    [imageData getBytes:&firstByte length:1];
    switch (firstByte) {
      case firstByteJpg:
        return @"jpg";
      case firstBytePng:
        return @"png";
      case firstByteGif:
        return @"gif";
      default:
        return @"jpg";
    }
}

- (NSString*) generateFilePath:(NSString *)ext 
{
    NSString* tempPath = [NSTemporaryDirectory()stringByStandardizingPath];
    NSString *fileName = [[NSUUID UUID] UUIDString];
    return [NSString stringWithFormat:@"%@/%@.%@", tempPath, fileName, ext];
}

- (UIImage*)resizeImage:(UIImage*)image maxWidth:(float)maxWidth maxHeight:(float)maxHeight
{
    if ((maxWidth == 0) && (maxHeight == 0)) {
        return image;
    }
    
    if (image.size.width <= maxWidth && image.size.height <= maxHeight) {
        return image;
    }
    

    CGSize newSize = CGSizeMake(image.size.width, image.size.height);
    if (maxWidth != 0 && maxWidth < newSize.width) {
        newSize = CGSizeMake(maxWidth, (maxWidth / newSize.width) * newSize.height);
    }
    if (maxHeight != 0 && maxHeight < newSize.height) {
        newSize = CGSizeMake((maxHeight / newSize.height) * newSize.width, maxHeight);
    }
    
    newSize.width = (int)newSize.width;
    newSize.height = (int)newSize.height;

    UIGraphicsBeginImageContext(newSize);
    [image drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    if (newImage == nil) {
        NSLog(@"could not scale image");
        newImage = image;
    }
    UIGraphicsEndImageContext();

    return newImage;
}

- (NSString *)mapImageToAsset:(UIImage *)image data:(NSData *)data {
    NSString *fileType = [self getFileType:data];
    NSString *filePath = [self generateFilePath:fileType];
    
    image = [self resizeImage:image maxWidth:self.maxWidth maxHeight:self.maxHeight];

    if ([fileType isEqualToString:@"jpg"]) {
        data = UIImageJPEGRepresentation(image, self.quality);
    } else if ([fileType isEqualToString:@"png"]) {
        data = UIImagePNGRepresentation(image);
    }

    if (self.outputType == BASE64_STRING) {
        NSString *base64 = [data base64EncodedStringWithOptions:0];
        return base64 ? base64 : @"";
    } else {
        [data writeToFile:filePath atomically:YES];
        return filePath;
    }
}

@end


@implementation ImagePicker (UIImagePickerControllerDelegate)

- (void)imagePickerController:(UIImagePickerController *)picker didFinishPickingMediaWithInfo:(NSDictionary<NSString *,id> *)info
{
    [picker dismissViewControllerAnimated:YES completion:nil];

    dispatch_async(dispatch_get_main_queue(), ^{
        NSMutableArray<NSString *> *assets = [[NSMutableArray alloc] initWithCapacity:1];

        UIImage *image = info[UIImagePickerControllerEditedImage];
        if (!image) {
            image = info[UIImagePickerControllerOriginalImage];
        }

        NSURL *dataUrl;
        if (@available(iOS 11.0, *)) {
            dataUrl = info[UIImagePickerControllerImageURL];
        }
        else {
            dataUrl = info[UIImagePickerControllerReferenceURL];
        }
        [assets addObject:[self mapImageToAsset:image data:[NSData dataWithContentsOfURL:dataUrl]]];

        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:assets];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    });
}

@end


#if __has_include(<PhotosUI/PHPicker.h>)
@implementation ImagePicker (PHPickerViewControllerDelegate)

- (void)picker:(PHPickerViewController *)picker didFinishPicking:(NSArray<PHPickerResult *> *)results API_AVAILABLE(ios(14))
{
    [picker dismissViewControllerAnimated:YES completion:nil];

    if (results.count == 0) {
        dispatch_async(dispatch_get_main_queue(), ^{
            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray: @[]];
            [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
        });
        return;
    }

    dispatch_group_t completionGroup = dispatch_group_create();
    NSMutableArray<NSString *> *assets = [[NSMutableArray alloc] initWithCapacity:results.count];
    for (int i = 0; i < results.count; i++) {
        [assets addObject:@""];
    }
    
    for (PHPickerResult *result in results) {
        NSItemProvider *provider = result.itemProvider;
        
        dispatch_group_enter(completionGroup);

        if ([provider canLoadObjectOfClass:[UIImage class]]) {
            NSString *identifier = provider.registeredTypeIdentifiers.firstObject;
            // Matches both com.apple.live-photo-bundle and com.apple.private.live-photo-bundle
            if ([identifier containsString:@"live-photo-bundle"]) {
                // Handle live photos
                identifier = @"public.jpeg";
            }

            [provider loadFileRepresentationForTypeIdentifier:identifier completionHandler:^(NSURL * _Nullable url, NSError * _Nullable error) {
                NSData *data = [[NSData alloc] initWithContentsOfURL:url];
                UIImage *image = [[UIImage alloc] initWithData:data];
                assets[[results indexOfObject:result]] = [self mapImageToAsset:image data:data];
                dispatch_group_leave(completionGroup);
            }];
        } else {
            // The provider didn't have an item matching photo or video (fails on M1 Mac Simulator)
            dispatch_group_leave(completionGroup);
        }
    }

    dispatch_group_notify(completionGroup, dispatch_get_main_queue(), ^{
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:assets];
        [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    });
}

@end
#endif
