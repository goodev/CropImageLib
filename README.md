Android CropImage Library
==========================

该项目是从Android系统的[Gallery3D][1]中提出的一个裁图的类库。

由于Gallery3D中附带的裁图功能不是标准的Android系统功能，导致在很多第三方定制ROM（meizu、miui等）中无法正常使用裁图功能，
所以提炼一个类库出来，如果您的项目中需要该功能，则可以把该项目做为Android 库项目导入。

使用
=====

*默认启动截图的ACTION为“org.goodev.action.CROP”，您可以下载项目代码后，自己在AndroidManifest.xml中修改

支持如下参数：
  1. circleCrop ：string，如果不为null则为圆形裁图
  2. android.provider.MediaStore.EXTRA_OUTPUT：Uri，如果不为null，则指定把裁好的图保存的路径
  3. outputFormat： String，如果设置了2中保存的Uri，则这个设置指定使用的图片格式，支持JPEG、PNG、WEBP(4.0+系统才支持) 3中格式。
           默认为PNG
  4. data：Bitmap，需要裁图的图片数据，如果图片过大的话，则通过Intent参数无法传输，则可以通过Intent data来传递图片的Uri地址
  5. aspectX、aspectY：裁图的宽高比率，如果设置为圆形的话，则宽高比为1:1
  6. outputX、outputY: 裁剪后图片的大小
  7. scale、scaleUpIfNeeded：如果用户选择的大小和6中指定的大小不一样，是否缩小、放大为指定的大小，默认值为true,如果设置为false，当
       用户选择的图片比较大的时候， 只返回在裁剪后图片中间再次裁剪为指定大小的内容
  8. noFaceDetection： 是否禁用脸谱检测，默认为true
  
  

注意
-------

只能裁剪本地图片，在Uri中只能为file或者content开头的本地图片，不能裁剪网络上的图片。
网络图片需要自己下载本地后再裁剪。

在您的项目中使用
-------------------------

下载该项目，然后在IDE中设置为library项目，在您的项目中引用该项目即可。


Developed By
============

 * Goodev.org - <admin@goodev.org>


License
=======

    Copyright 2012 Goodev.org
    Copyright 2012 Android

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.






 [1]: https://android.googlesource.com/platform/packages/apps/Gallery3D/
