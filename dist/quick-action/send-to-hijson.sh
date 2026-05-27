#!/bin/bash
# Send to HiJson - 将选中文本发送到 HiJson
# 使用方法：在 macOS 系统设置 → 键盘 → 键盘快捷键 → 服务 中设置快捷键

# 复制选中文本
osascript -e 'tell application "System Events" to keystroke "c" using command down'
sleep 0.3

# 获取剪贴板内容
TEXT=$(pbpaste)

if [ -z "$TEXT" ]; then
    echo "No text selected"
    exit 1
fi

# 确保 HiJson 在运行
open -a HiJson
sleep 0.5

# 通过 Socket 发送文本到 HiJson
echo -n "$TEXT" | nc -w 2 127.0.0.1 19527

# 再次激活 HiJson 窗口
osascript -e 'tell application "HiJson" to activate'
