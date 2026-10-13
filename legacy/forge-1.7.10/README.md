# ChatImage — Forge 1.7.10 / GTNH 客户端版

目标环境：GTNH **2.9.0-rc2**、**Java 25**、Minecraft **1.7.10**。
本模块独立维护，放在 `legacy/` 下，避免被主项目的 ModMultiVersion 初始化脚本覆盖。

## 安装和使用

构建后的 `ChatImage-<版本>+1.7.10+forge.jar` 放入 GTNH 客户端实例的 `mods/`。
不要使用文件名带 `-dev` 或 `-sources` 的开发包。无需给服务器安装，也无需安装 ActionLib。

打开聊天框，将鼠标移到绿色的 `[图片]` 标签上预览。支持 PNG、JPEG/JFIF、BMP 和 GIF：

```text
https://example.org/image.png
[[CICode,url=https://example.org/image.gif,name=示例]]
[CQ:image,url=https://example.org/image.png]
```

CICode 支持 `url`、`name`、`nsfw`、`pre`、`suf`。
标记 `nsfw=true` 的图片在点击确认前不会下载或显示。
普通图片在首次悬浮时下载；GIF 使用原始帧延时播放。
失败的图片可点击重试。

客户端命令：

- `/chatimage url <链接>`：发送图片链接。
- `/chatimage send <名称> <链接>`：发送 CICode，名称不含空格、逗号和右方括号。
- `/chatimage preview <链接>`：仅在本地聊天中加入预览，不发送给其他玩家。
- `/chatimage reload`：重载配置并清除图片缓存。
- `/chatimage help`：显示用法。

Minecraft 1.7.10 聊天输入限制为 **100 字符**，发送命令也会检查这个限制。
链接加 CICode 超过限制时可发送较短的裸图片链接。
`preview` 不发送聊天包，但在原版输入框手动输入命令时仍受输入框长度限制。
其他玩家需要在自己的客户端安装本模组才能预览图片；服务器聊天插件可能改写图片链接。

配置文件为实例下的 `config/chatimage-1.7.10.json`，首次启动自动生成：

```json
{
  "limitWidth": 320,
  "limitHeight": 240,
  "timeout": 10,
  "MaxFileSize": 4096,
  "cacheEntries": 8,
  "nsfw": false,
  "cqCode": true,
  "checkImageUri": true
}
```

`timeout` 单位为秒，`MaxFileSize` 单位为 KiB；图片缓存只保留在内存中。
切换服务器或断开连接时释放图片纹理。GIF 最多 128 帧，每张图片累计最多 8,388,608 像素。

## 当前范围

仅处理 HTTP(S) 网络图片。暂不包含本地文件分享、拖拽、剪贴板图片、配置界面、成书图片或自定义 `/tellraw` 悬浮动作。
使用 Forge 的聊天接收与界面绘制事件，不替换 `GuiChat`，不注入 Minecraft 类。
已在 GTNH 2.9.0-rc2 / Angelica 整合包环境中实测确认可用。

## 构建和验证

在本目录执行（Gradle 运行环境使用 JDK 25）：

```bash
./gradlew build
```

RetroFuturaGradle 2.0.6 下载 Forge 10.13.4.1614 开发环境；Java 8 编译工具链通过 Foojay 自动获取。
模组编译为 Java 8 字节码；单元测试运行在 Java 25 下。
可安装的重混淆 jar 输出到 `build/libs/`，并复制到根目录 `ChatImage-jar/<版本>/`。

测试覆盖链接解析、CQ 参数、聊天组件格式、HTTP 重定向与流大小限制、透明 PNG、GIF 帧合成及像素限制。

游戏内验证步骤：

1. 在 GTNH 2.9.0-rc2 / Java 25 中安装 jar，进入单人世界。
2. 发一个已知可访问的 PNG 链接，打开聊天框悬浮，检查图片及屏幕边缘位置。
3. 发一个 GIF 链接，检查动画、透明背景，退出聊天框后检查游戏渲染。
4. 发 `nsfw=true` 的 CICode，检查确认、拒绝和确认后的预览。
5. 连接未安装本模组的服务器，检查可以登录以及图片标签显示。
6. 重载配置、断开连接并重连，检查没有旧图片或纹理残留。
