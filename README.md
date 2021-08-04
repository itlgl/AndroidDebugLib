# Android Debug Lib
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Api Level](https://img.shields.io/badge/api-14%2B-brightgreen.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.itlgl.android/androiddebuglib/badge.svg)


![English Doc](https://github.com/itlgl/AndroidDebugLib/raw/master/README.md)
![中文文档](https://github.com/itlgl/AndroidDebugLib/raw/master/README-zh.md)

## Android Debug Lib is a powerful debugging library, which is convenient for developers to debug the database, preferences, file browsing and other functions in applications

### **Do not import this library in the release version, which will make your application insecure**

## What can Android Debug Lib do

* View and download all the files accessible by the current app, including databases and preferences, without the need for the device to root
* Provides the preview function of TXT, XML, JSON, JPG, MP3 and MP4 files
* The web page supports internationalization and is consistent with the app language environment
* View the database in the application and execute any SQL statement
* View preferences within the app
* Copy content to the phone's clipboard
* Extensible custom command interaction (mostly used for APDU communication with devices. Combined with this function, you can test the stability of Bluetooth and USB middleware with UCard on PC)

## How to use

#### 1. Add this to your app's build.gradle

```gradle
debugImplementation 'com.itlgl.android:androiddebuglib:0.1.0'
```

#### 2. adb command on PC

Input the ADB command on the PC connected to the test machine:

```cmd
adb forward tcp:8080 tcp:8080
```

Now you can access it on your PC's browser `http://localhost:8080` Start debugging.

#### If you want use different port other than 8080.

In the app build.gradle file under buildTypes do the following change

```groovy
debug {
    resValue("string", "adl_port", "8081")
}
```

## Auxiliary API

Three auxiliary APIs are provided, and the response test interface is also provided on the web page.

#### 1. Show toast on app

curl
```
curl 'http://localhost:8080/api/toast' -H 'Content-Type: application/json;charset=utf-8' --data-raw $'{"message":"Hello world!"}'
```

#### 2. Send text to phone pasteboard

curl
```
curl 'http://localhost:8080/api/clipboard' -H 'Content-Type: application/json;charset=utf-8' --data-raw $'{"message":"00a4040000"}'
```

#### 3. Extensible custom command interaction

If you are developing Bluetooth and USB communication middleware and want to directly send script instructions to the device for execution through PC Tools (such as UCard and smartblue), you certainly need such a debugging API

Android debug lib opens the path from PC to Android device. It only takes a few simple steps to complete this automated test process.

Use steps:
 - 1. Implement the `AndroidDebugLib.ICommand` interface to provide your middleware communication transparent transmission function
 - 2. Call the 'AndroidDebugLib.registerCommandCallback(callback)' method to register the implementation
 - 3. You need to implement the function of HTTP sending JSON format data on the PC script tool. The example of HTTP interface call is as follows

curl
```
curl 'http://localhost:8080/api/command' -H 'Content-Type: application/json;charset=utf-8' --data-raw $'{"message":"00a4040000"}'
```

## Screenshot

<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/1.png" height="300" /><br/>
<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/2.png" height="300" /><br/>
<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/3.png" height="300" /><br/>
<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/4.png" height="300" /><br/>

## Thanks

[Android Debug Database](https://github.com/amitshekhariitbhu/Android-Debug-Database)

## License

```
Copyright (c) itlgl, Android Debug Lib Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```