# Android Debug Lib
![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)
![Api Level](https://img.shields.io/badge/api-14%2B-brightgreen.svg)
![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.itlgl.android/androiddebuglib/badge.svg)


![English Doc](https://github.com/itlgl/AndroidDebugLib/raw/master/README.md)
![中文文档](https://github.com/itlgl/AndroidDebugLib/raw/master/README-zh.md)

## Android Debug Lib是一个功能强大的调试库，方便开发者调试应用中的数据库、首选项、浏览文件等功能

### **不要在release版本中导入这个库，这将使你的应用毫无安全性可言**

## Android Debug Lib可以做什么

* 查看和下载所有当前app可访问的文件，包括数据库和首选项，不需要设备进行root
* 提供txt、xml、json、jpg、mp3、mp4文件的预览功能
* 网页支持国际化，和app语言环境一致
* 查看应用内的数据库，执行任意sql语句
* 查看应用内的首选项
* 复制内容到手机的粘贴板
* 可扩展自定义Command交互（多用于和设备进行Apdu通信的场景，结合这个功能可以配合PC上的Ucard测试蓝牙、usb中间件稳定性）

## 如何使用

#### 1. 在项目build.gradle文件中加入依赖

```groovy
debugImplementation 'com.itlgl.android:androiddebuglib:0.1.0'
```

#### 2. PC开启adb透传

在连接测试机的PC上输入adb指令：

```cmd
adb forward tcp:8080 tcp:8080
```

现在你可以在PC的浏览器上访问`http://localhost:8080`开始Debug了。

#### 如果你需要自定义其他服务端口，在`build.gradle`文件中`buildTypes`下增加配置

```groovy
debug {
    resValue("string", "adl_port", "8081")
}
```

## 辅助api

Android Debug Lib提供了三个辅助api，网页上也提供了响应的测试界面

#### 1. 在App上显示Toast

curl方式调用
```
curl 'http://localhost:8080/api/toast' -H 'Content-Type: application/json;charset=utf-8' --data-raw $'{"message":"Hello world!"}'
```

#### 2. 发送文字到手机粘贴板

curl方式调用
```
curl 'http://localhost:8080/api/clipboard' -H 'Content-Type: application/json;charset=utf-8' --data-raw $'{"message":"00a4040000"}'
```

#### 3. 可扩展自定义Command交互

如果你是开发蓝牙、USB通信中间件的，想要通过pc工具（比如：Ucard、SmartBlue）将脚本指令直接下发到设备执行，那么你肯定需要这样一个调试api

Android Debug Lib打通了PC到Android设备的通路，只需要简单的几步就可以完成这个自动化的测试流程

使用步骤：
 - 1. 实现`AndroidDebugLib.ICommand`接口，提供你的中间件通信透传功能
 - 2. 调用`AndroidDebugLib.registerCommandCallback(callback)`方法注册实现
 - 3. 需要你在PC的脚本工具上实现http发送json格式数据的功能，http接口调用示例如下

curl方式调用
```
curl 'http://localhost:8080/api/command' -H 'Content-Type: application/json;charset=utf-8' --data-raw $'{"message":"00a4040000"}'
```

## 使用截图

<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/1.png" height="300" /><br/>
<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/2.png" height="300" /><br/>
<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/3.png" height="300" /><br/>
<img src="https://gitee.com/itlgl/AndroidDebugLib/raw/master/screenshot/4.png" height="300" /><br/>

## 鸣谢

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