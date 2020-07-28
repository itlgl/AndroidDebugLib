# Android Debug Lib
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Api Level](https://img.shields.io/badge/api-14%2B-brightgreen.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.itlgl.android/androiddebuglib/badge.svg)

Android Debug Lib是一个功能强大的调试库，方便开发者调试应用中的数据库、首选项等功能

**不要在release版本中导入这个库，这将使你的应用毫无安全性可言**

## Android Debug Lib可以做什么

* 查看应用内的数据库，执行任意sql语句
* 查看应用内的首选项
* 复制内容到手机的粘贴板
* 下载手机文件到PC，包括应用内的数据库和首选项文件
* 向手机发送Apdu（多用于和设备进行Apdu通信的场景，结合这个功能可以配合PC上的Ucard测试蓝牙、usb中间件稳定性）
* 可扩展指令sendCommand，提供给开发者实现自定义交互功能

## 如何使用

#### 1. 导入Android Debug Lib

```gradle
debugImplementation 'com.itlgl.android:androiddebuglib:0.0.4'
```

应用安装到手机上将出现一个logo和你的应用一样，名字叫`DebugLib`的图标，提供debug界面。
Debug界面可以设置服务器的端口号，默认是8080。
点击`开始DEBUG`按钮，开始Debug，此时手机上已经在指定端口上启动了一个小型Web服务。

#### 2. PC开启adb透传

在连接测试机的PC上输入adb指令：

```cmd
adb forward tcp:8080 tcp:8080
```

现在你可以在PC的浏览器上访问`http://localhost:8080`开始Debug了。由于本人网页知识有限，所以网页界面并不美观。


## 使用截图

<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/1.png" height="300" /><br/>
<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/2.png" height="300" /><br/>

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