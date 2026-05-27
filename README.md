# HiJson for macOS

一款 JSON 字符串格式化查看的桌面工具，macOS 增强版。

基于原版 HiJson（藏言/CangYan）改编，针对 macOS 平台进行了大量增强和优化。

## 功能特性

- JSON 字符串格式化与树状视图展示
- 多标签页支持
- 右键复制键名、键值、路径、节点内容等
- **展开全部(E)** — 自动递归展开嵌套的 JSON 字符串（Ctrl+E）
- 文本查找、节点查找
- 支持从文件打开和保存

## macOS 增强内容（V2.2.0）

- **macOS .app 打包** — 可直接拖入 /Applications 使用
- **自定义应用图标** — 紫蓝渐变风格图标
- **FlatLaf 现代主题** — 采用 FlatLightLaf 扁平化界面风格
- **界面精化** — 圆角按钮、统一字体、宽松间距、精致工具栏
- **右键菜单修复** — 修复 macOS 上树视图和文本区域右键菜单不弹出的问题
- **对象/数组节点复制增强** — 右键复制对象或数组节点的值时，输出完整 JSON 而非 "Object"/"Array"
- **Dock 图标修复** — 使用 `-Xdock:icon` 保持自定义图标不被 Java 默认图标覆盖

## 编译构建

### 依赖

- JDK 8
- Apache Ant（可选，也可直接用 javac）

### 手动编译

```bash
# 编译
CLASSPATH="lib/appframework-1.0.3.jar:lib/swing-worker-1.1.jar:lib/flatlaf-3.5.4.jar:lib/commons-lang-2.4.jar:lib/fastjson-1.1.28.jar:lib/gson.jar:lib/org-netbeans-swing-tabcontrol.jar:lib/org-openide-awt.jar:lib/org-openide-util.jar:lib/org-openide-windows.jar:lib/rsyntaxtextarea.jar"

find src -name "*.java" | xargs javac -source 1.7 -target 1.7 -encoding UTF-8 -d build/classes -cp "$CLASSPATH"

# 打包
cp -r src/META-INF build/classes/
cp -r src/hi/chyl/json/resources build/classes/hi/chyl/json/resources
jar cfm dist/HiJson.jar manifest.mf -C build/classes .
```

### 运行

```bash
java -jar dist/HiJson.jar
# 或直接打开 macOS app
open dist/HiJson.app
```

## Requirement

- JDK 1.8 or higher
- macOS 10.9+

## About

License: [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)

Original Author: [藏言(Cangyan)](mailto:beetle082@163.com)

macOS Enhancement: [xuanzi](https://github.com/muxuan1978)

Original Website: [HiJson](https://code.google.com/p/json-view/)
