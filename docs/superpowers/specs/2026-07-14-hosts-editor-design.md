# Hosts 编辑器功能设计

## 概述

为 Virtual Hosts 应用增加内置 hosts 文件编辑功能。支持编辑、新建、导入、下载 hosts 文件，提供纯文本编辑器，带语法高亮、行号、搜索替换、撤销重做等功能。

## 核心原则

- **导入而非引用**：所有 hosts 文件操作基于"导入"模式，文件复制到应用内部存储 `files/user_hosts.txt`，原始文件不受影响
- **编辑与配置分离**：编辑器独立 Activity；导入/下载入口通过主界面 BottomSheet 菜单访问

## 架构

```
VhostsActivity (主页)
  └─ "修改 Hosts" 按钮 → BottomSheet 菜单
       ├─ 📝 编辑 (已加载时可用)
       ├─ ✨ 新建 (空白编辑器)
       ├─ 📂 导入本地文件 (SAF 选择器)
       └─ 🌐 从 URL 下载 (弹出 URL 输入框)

HostsEditorActivity (编辑器)
  └─ toolbar: 保存 / 搜索替换 / 撤销 / 重做
  └─ 编辑区: 行号 + 语法高亮 EditText
  └─ FAB: 快捷插入模板
```

## 新增文件

| 文件 | 说明 |
|------|------|
| `HostsEditorActivity.java` | 编辑器 Activity，管理编辑状态、保存、搜索替换、撤销重做 |
| `res/layout/activity_hosts_editor.xml` | 编辑器布局：Toolbar + 行号 + EditText + FAB |
| `res/menu/hosts_editor_menu.xml` | 编辑器 Toolbar 菜单 |

## 修改文件

| 文件 | 改动 |
|------|------|
| `VhostsActivity.java` | "Re-select Hosts File" 按钮改为"修改 Hosts"，点击弹出 BottomSheet 菜单；菜单内实现编辑/新建/导入/下载逻辑 |
| `res/layout/activity_vhosts.xml` | 按钮文字修改 |
| `res/values/strings.xml` | 新增字符串资源（菜单项、编辑器标签等） |
| `vservice/DnsChange.java` | `handle_hosts()` 支持一行多域名（按空白字符拆分 group 3）；新增内部文件路径常量 |
| `vservice/VhostsService.java` | `setupHostFile()` 增加从 `user_hosts.txt` 读取的路径 |

## 编辑器功能

### 语法高亮

通过 `TextWatcher` + `SpannableString` 实现，在文本变化时异步更新：
- `#` 开头行 → 灰色 (#888888)
- 合法 IPv4/IPv6 → 蓝色 (#1565C0)
- 其余文本 → 默认色

### 行号

自定义 View（`LineNumberView`）作为 EditText 左侧边栏，监听滚动和文本变化同步显示行号。

### 撤销/重做

利用 Android API 26+ 的 `EditText.setUndoManager()` 启用在输入法中的撤销支持，同时 Toolbar 按钮手动调用 `textUndoManager.undo()` / `redo()`。

### 搜索替换

点击搜索按钮展开搜索栏（Toolbar 下方），输入关键词后：
- EditText 中匹配文本高亮（黄色背景）
- "上一个/下一个"导航
- "替换"/"全部替换"按钮

### 快捷插入 FAB

点击弹出 BottomSheet 模板列表：
- `127.0.0.1 .local` — 本地开发
- `127.0.0.1 .test` — 测试环境
- `0.0.0.0 .blocked` — 屏蔽域名
- 自定义... — 弹出输入框自行填写 IP 和域名

插入位置为当前光标处，如果光标所在行非空则自动换行。

### 一行多域名支持

修改 `DnsChange.handle_hosts()` 的域名解析逻辑：当前仅取 group 3 的整体作为一个域名，需改为按空白字符拆分 group 3，每个拆分结果作为独立域名映射到同一个 IP。

```
127.0.0.1 a.com b.com c.com
→ a.com. → 127.0.0.1
→ b.com. → 127.0.0.1
→ c.com. → 127.0.0.1
```

编辑器语法高亮同样适配：一行中第一个合法 IP 蓝色，后续域名部分默认色。

## 数据流

### 导入本地文件
1. 用户点击"导入本地文件" → SAF `ACTION_OPEN_DOCUMENT`
2. 选择文件后，读取内容 → 写入 `files/user_hosts.txt`
3. 触发 DNS 重新解析

### 从 URL 下载
1. 用户点击"从 URL 下载" → 弹出 AlertDialog 输入 URL
2. 下载内容 → 写入 `files/user_hosts.txt`
3. 触发 DNS 重新解析

### 编辑流程
1. BottomSheet 点击"编辑" → Intent 启动 HostsEditorActivity，传递当前文件路径
2. 编辑器加载 `files/user_hosts.txt` 内容
3. 用户编辑，点击保存 → 写回文件 → Activity.setResult(OK) → finish()
4. VhostsActivity.onActivityResult → 刷新 DNS 解析

### 新建流程
1. BottomSheet 点击"新建" → Intent 启动 HostsEditorActivity，无文件路径
2. 编辑器以空白状态打开
3. 用户编辑，点击保存 → 写入 `files/user_hosts.txt` → 返回
4. 首次保存后自动设为当前加载文件

## 服务适配

`VhostsService.setupHostFile()` 调整读取优先级，统一使用 `user_hosts.txt` 作为新的主要路径，保留旧路径兼容：

1. `files/user_hosts.txt` 存在 → 从此文件读取（新路径，编辑/导入/下载均写入此处）
2. `IS_NET=true` → 从 `openFileInput("net_hosts")` 读取（旧版远程下载遗留）
3. 否则 → 从 SAF URI 读取（旧版本地文件选择遗留）

## 错误处理

| 场景 | 处理 |
|------|------|
| 编辑时无文件（未导入过） | 编辑器以空白打开，首次保存时写入新文件 |
| SAF 导入失败/用户取消 | Toast 提示，无其他影响 |
| URL 下载失败 | Toast 显示错误信息 |
| 保存失败（磁盘满等） | Toast 提示保存失败，不关闭编辑器，允许用户重试 |
| 非法 IP 格式（语法高亮） | 不高亮，不影响编辑，解析时自动跳过 |

## 兼容性

- 最低 SDK：跟随项目设置
- 行号 View：自定义 View，无需额外依赖
- UndoManager：API 26+（项目 target 应已满足）

## 不涉及

- 不修改 VPN 核心逻辑（VPN 线程模型、数据包处理等）
- 不改动 SettingsActivity/SettingsFragment 的结构
- 不引入第三方编辑器库
