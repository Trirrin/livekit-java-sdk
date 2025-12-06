# LiveKit Java SDK

[English](README.md) | 简体中文

[LiveKit](https://livekit.io) 实时通信平台的 Java 客户端 SDK。

## 功能特性

- **房间管理** - 加入/离开房间，管理参与者和媒体轨道
- **WebRTC 集成** - 通过 [webrtc-java](https://github.com/niclasvaneyk/webrtc-java) 实现音视频发布和订阅
- **数据通道** - 可靠和非可靠数据消息传输
- **端到端加密** - 数据通道的 AES-GCM 加密，支持密钥轮换
- **自动重连** - 指数退避的自动重连机制
- **网络监控** - 恢复令牌和网络变化检测

## 环境要求

- Java 21+
- Gradle 9.x（已包含 wrapper）

## 安装

[![](https://jitpack.io/v/Trirrin/livekit-java-sdk.svg)](https://jitpack.io/#Trirrin/livekit-java-sdk)

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // 完整 SDK（含 WebRTC 支持）
    implementation("com.github.Trirrin.livekit-java-sdk:rtc:v0.1.0")
    
    // 或仅信令模块（无音视频）
    // implementation("com.github.Trirrin.livekit-java-sdk:signaling:v0.1.0")
}
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Trirrin.livekit-java-sdk:rtc:v0.1.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Trirrin.livekit-java-sdk</groupId>
        <artifactId>rtc</artifactId>
        <version>v0.1.0</version>
    </dependency>
</dependencies>
```

## 快速开始

```java
import io.livekit.sdk.Room;
import io.livekit.sdk.RoomOptions;
import io.livekit.sdk.RoomListener;
import io.livekit.sdk.signaling.LiveKitClient;

public class Example {
    public static void main(String[] args) {
        RoomOptions options = new RoomOptions();
        LiveKitClient client = new LiveKitClient(options);
        Room room = client.getRoom();

        room.addListener(new RoomListener() {
            @Override
            public void onConnected(Room r) {
                System.out.println("已连接到房间: " + r.getName());
            }

            @Override
            public void onParticipantConnected(Room r, RemoteParticipant participant) {
                System.out.println("参与者加入: " + participant.getIdentity());
            }
            
            // ... 其他回调
        });

        client.connect("wss://your-server.livekit.cloud", "your-access-token");
    }
}
```

## 示例

`examples` 模块包含完整的使用示例：

| 示例 | 描述 |
|------|------|
| `BasicRoomExample` | 加入房间并处理事件 |
| `PublishExample` | 发布音视频轨道 |
| `SubscribeExample` | 订阅远程轨道 |
| `DataChannelExample` | 交互式数据消息 |

运行示例：

```bash
export LIVEKIT_URL=wss://your-server.livekit.cloud
export LIVEKIT_TOKEN=your-access-token
./gradlew :examples:run
```

## 文档

- [快速入门](docs/getting-started.md) - 安装和基本用法
- [发布媒体](docs/publishing.md) - 音视频发布指南
- [数据通道](docs/data-channels.md) - 实时消息传输
- [E2EE 指南](docs/e2ee.md) - 端到端加密
- [API 参考](docs/api-reference.md) - 完整 API 文档

## 项目结构

```
livekit-java-sdk/
├── protocol/    # Protobuf 定义和生成的类
├── core/        # Room, Participant, Track, E2EE API
├── signaling/   # WebSocket 信令客户端
├── rtc/         # WebRTC 集成
├── examples/    # 使用示例
└── docs/        # 文档
```

## 构建

```bash
# 构建所有模块
./gradlew build

# 运行测试
./gradlew test

# 格式化代码
./gradlew spotlessApply

# 发布到本地仓库
./gradlew publishToMavenLocal
```

## 端到端加密 (E2EE)

通过 `E2EEManager` 支持数据通道加密：

```java
import io.livekit.sdk.e2ee.E2EEManager;
import io.livekit.sdk.e2ee.BaseKeyProvider;

BaseKeyProvider keyProvider = new BaseKeyProvider();
keyProvider.setSharedKey(secretKeyBytes);

E2EEManager e2ee = new E2EEManager(keyProvider);
byte[] encrypted = e2ee.encryptData(plaintext, participantId);
byte[] decrypted = e2ee.decryptData(encrypted, participantId);
```

**注意：** 媒体轨道的 E2EE 需要 `FrameCryptor` API，该 API 尚未被 webrtc-java 暴露。

## 已知限制

- 屏幕共享尚未暴露（webrtc-java 支持但未集成）
- Simulcast/SVC 图层选择未实现
- 媒体轨道 E2EE 受限于 webrtc-java 库

## 许可证

Apache License 2.0

## 相关链接

- [LiveKit 文档](https://docs.livekit.io)
- [LiveKit 服务器](https://github.com/livekit/livekit)
- [webrtc-java](https://github.com/niclasvaneyk/webrtc-java)
