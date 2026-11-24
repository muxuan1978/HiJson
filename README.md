# HiJson

HiJson 是一个本地 JSON 查看与编辑工具，支持 Windows 和 macOS。可以在文本、树状视图和节点图之间查看和修改同一份 JSON。项目基于藏言（CangYan）的原版 HiJson。

## 下载与安装

| 平台 | 下载 | 启动方式 |
| --- | --- | --- |
| Windows x64 | [HiJson-Windows-x64.zip](https://github.com/muxuan1978/HiJson/releases/download/v2.5.0/HiJson-Windows-x64.zip) | 完整解压，双击 `HiJson/HiJson.exe`。包内附带 Java 运行时，请保留同目录的 `app` 和 `runtime` 文件夹。 |
| macOS | [HiJson-macOS.dmg](https://github.com/muxuan1978/HiJson/releases/download/v2.5.0/HiJson-macOS.dmg) | 安装 Java 8 或更高版本，打开 DMG，将 HiJson 拖入「应用程序」。 |
| 已有 Java 8 或更高版本 | [HiJson.jar](https://github.com/muxuan1978/HiJson/releases/download/v2.5.0/HiJson.jar) | 运行 `java -jar HiJson.jar`。 |

## Release 说明

### v2.5.0

- 图形模式支持逐个折叠对象和数组。点击左侧字段会展开并定位到图中的对应行，点击卡片或字段也会在编辑器中选中对应内容。
- 右键点击图形中的字段可复制键名、值或完整键值对。修复了 macOS 树状视图的右键菜单。
- 工具栏和文档标签分为两行，菜单和常用操作增加了图标。树状视图的字段表可以收起，为编辑区留出空间。
- 更新应用图标并调整 macOS Dock 图标尺寸。「关于 HiJson」分别列出原始贡献者、维护者、原版项目和当前仓库，两个项目链接均可点击。

### v2.4.0

- 新增 Windows x64 便携版，双击即可启动。
- 新增可编辑的节点图。双击可修改键名和值，拖动字段行可调整父子关系和数组顺序。修改会同步回 JSON 文本。
- 图形卡片可拖动、调整大小和自动排列。树状视图支持与编辑器双向定位，包括嵌套数组和同名字段。

完整版本记录和下载文件见 [GitHub Releases](https://github.com/muxuan1978/HiJson/releases)。

## 功能特性

- JSON 字符串格式化，支持树状视图和可编辑的节点图
- 多标签页支持
- 右键复制键名、键值、路径、节点内容等
- **展开嵌套 JSON**（Ctrl+E）会递归展开字符串中的 JSON 对象和数组。
- 树状视图双向定位。点击左侧键名或值，右侧自动展开到对应节点；选择树节点，左侧会选中完整字段、对象或数组。
- 文本查找、节点查找
- 支持从文件打开和保存

顶部工具栏用于新建、打开、保存和格式化，下一行的标签用于切换和关闭文档。窗口较窄时，可从右上角「更多操作」打开文件、保存或查找。树状视图右上角的字段表按钮可以收起键值表，为长路径留出空间，再次点击可恢复原来的宽度。

### 图形编辑

在右侧选择 **图形**。每张卡片代表一个对象或数组，连线指向嵌套的容器。

- 点击卡片标题右侧的箭头，可单独折叠或展开对象和数组。重新展开父卡片时，子卡片保留各自的折叠状态。
- 点击左侧 JSON 的键或值，画布会展开通往该字段的分支并滚动到对应位置。点击图中的字段行或卡片标题，编辑器会选中完整字段、对象或数组。
- 右键点击字段行可复制键名、值或完整键值对。数组行以 `[0]`、`[1]` 等下标作为键名。
- 双击字段或卡片标题，编辑键名和 JSON 值。例如，`"12"` 是字符串，`12` 是数字。
- 拖动卡片标题移动位置，拖右下角调整宽高。缩小后可用卡片底部箭头翻看字段。「自动排列」恢复当前标签页的默认布局。
- 把字段行拖到目标对象或数组。移入对象时可指定键名，移入数组时可指定最终下标（从 0 开始），也可在同一数组内调整顺序。
- 拖动空白处平移，通过右下角按钮缩放。修改立即写回左侧文本，Windows 按 Ctrl+Z、macOS 按 ⌘Z 可撤销。编辑左侧文本也会刷新图形。使用「文件 → 保存」将当前文本写入文件。

## macOS 增强内容（V2.3.0）

- **全局发送文本** — 选中任意文本，通过快捷键一键发送到 HiJson 自动格式化
- **macOS .app 打包** — 可直接拖入 /Applications 使用
- **自定义应用图标** — 紫蓝渐变风格图标
- **FlatLaf 现代主题** — 采用 FlatLightLaf 扁平化界面风格
- **界面精化** — 圆角按钮、统一字体、宽松间距、精致工具栏
- **右键菜单修复** — 修复 macOS 上树视图和文本区域右键菜单不弹出的问题
- **对象/数组节点复制增强** — 右键复制对象或数组节点的值时，输出完整 JSON 而非 "Object"/"Array"
- **Dock 图标修复** — 使用 `-Xdock:icon` 保持自定义图标不被 Java 默认图标覆盖

### 全局发送文本设置

HiJson 启动后会自动开启 Socket 服务（端口 19527），配合 macOS Quick Action 实现全局快捷发送：

1. 把 DMG 中的 `Send to HiJson.workflow` 复制到 `~/Library/Services/`。
2. 在「系统设置 → 键盘 → 键盘快捷键 → 服务」中启用「Send to HiJson」并设置快捷键，例如 ⌥⌘J。
3. 在其他应用中选中文本后按快捷键，HiJson 会新建标签并格式化。

## 从源码构建

使用 JDK 8 和 Apache Ant 在仓库根目录构建包含依赖的 JAR。下面的命令适用于 macOS 和 Linux，`JAVA_HOME` 指向本机 JDK。

```sh
ant "-Dplatforms.JDK_1.7.home=$JAVA_HOME" standalone
java -jar dist/standalone/HiJson.jar
```

在 Windows x64 上安装 JDK 17 和 Apache Ant，将 `JAVA_HOME` 指向该 JDK，在 PowerShell 中运行：

```powershell
.\packaging\windows\build.ps1
```

脚本会编译源码、生成带运行时的 `HiJson.exe`，验证主窗口能打开，再输出 `dist/HiJson-Windows-x64.zip`。仓库的「Windows portable build」GitHub Actions 工作流使用同一脚本。

## About

License: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

Original Author: [藏言(Cangyan)](mailto:beetle082@163.com)

macOS Enhancement: [xuanzi](https://github.com/muxuan1978)

Original Website: [HiJson](https://code.google.com/p/json-view/)
