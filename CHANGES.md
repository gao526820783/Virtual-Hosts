# vitHost — 变更记录

> 基于 Virtual Hosts v2.2.3，新增内置 hosts 编辑器，支持一行多域名解析。

## 应用标识

| 项 | 原值 | 新值 |
|----|------|------|
| 应用名 | Virtual Hosts | **vitHost** |
| applicationId | com.github.xfalcon.vhosts | **com.github.xfalcon.vithost** |

## 新增文件

| 文件 | 说明 |
|------|------|
| `HostsEditorActivity.java` | 文本编辑器：语法高亮、行号、搜索替换、撤销重做、快捷插入模板 |
| `LineNumberView.java` | 自定义行号视图，与 EditText 同步滚动 |
| `res/layout/activity_hosts_editor.xml` | 编辑器布局（Toolbar + 搜索栏 + 编辑区 + FAB） |
| `res/layout/bottom_sheet_hosts_menu.xml` | 主界面菜单弹窗（编辑/新建/导入/下载） |
| `res/layout/dialog_url_input.xml` | URL 输入对话框 |
| `res/layout/dialog_custom_host.xml` | 自定义 hosts 记录对话框 |
| `res/menu/hosts_editor_menu.xml` | 编辑器 Toolbar 菜单 |

## 修改文件

| 文件 | 变更 |
|------|------|
| `VhostsActivity.java` | 按钮改为"修改 Hosts"，弹出菜单，下载/刷新逻辑，新增 imports |
| `activity_vhosts.xml` | 按钮文字 `re_select_hosts` → `btn_modify_hosts` |
| `VhostsService.java` | 优先读取 `files/user_hosts.txt`，回退到旧路径 |
| `DnsChange.java` | `handle_hosts()` 一行 IP 支持空格分隔多域名 |
| `AndroidManifest.xml` | 注册 `HostsEditorActivity` |
| `strings.xml` (en) | 新增 38 条英文字符串 |
| `strings.xml` (zh) | 新增 38 条中文字符串 |
| `app/build.gradle` | applicationId → `com.github.xfalcon.vithost` |
| `google-services.json` | 添加 vithost 包名 client 条目 |

## 编辑器功能

- **语法高亮** — 注释行灰色，首个 IP 蓝色，后续 IP 绿色
- **行号** — 与编辑区同步滚动，只绘制可见行
- **搜索替换** — 高亮匹配、上下导航、单个/全部替换
- **撤销重做** — Stack 存储全文本快照，最多 100 步
- **快捷插入** — 3 个预设模板 + 自定义 IP/域名

## hosts 数据流

```
[编辑/新建] ──保存──→ files/user_hosts.txt ──优先──→ VhostsService
[URL 下载] ──写入──→ files/user_hosts.txt ──优先──→ VhostsService
[SAF 导入] ──URI──→ SharedPreferences ──回退──→ VhostsService
```
