# Hosts 文件手动编辑功能 — 设计文档

## 目标

在 Virtual Hosts App 中增加手动编辑 hosts 文件的功能，支持文本模式和逐条记录模式，保存后即时生效无需重启 VPN。

## 新增/修改的文件

| 文件 | 状态 | 说明 |
|------|------|------|
| `HostsEditorActivity.java` | 新建 | 编辑页面 Activity，持有统一数据模型 |
| `TextEditorFragment.java` | 新建 | 文本编辑 Fragment |
| `RecordEditorFragment.java` | 新建 | 逐条记录编辑 Fragment |
| `RecordAdapter.java` | 新建 | 逐条模式 RecyclerView Adapter |
| `HostsContent.java` | 新建 | hosts 数据模型（List<HostsEntry>），负责解析/拼接 |
| `res/layout/activity_hosts_editor.xml` | 新建 | 编辑器布局（TabLayout + ViewPager） |
| `res/layout/fragment_text_editor.xml` | 新建 | 文本模式布局（EditText） |
| `res/layout/fragment_record_editor.xml` | 新建 | 逐条模式布局（RecyclerView + 添加按钮） |
| `res/layout/item_record.xml` | 新建 | 单条记录列表项布局（IP 输入框 + 域名输入框 + 删除按钮） |
| `VhostsActivity.java` | 修改 | 增加编辑按钮及"新建并编辑"逻辑 |
| `res/layout/activity_vhosts.xml` | 修改 | 增加编辑按钮 |

## 入口与导航

### 入口 1：主界面编辑按钮

在 `button_select_hosts` 旁边增加「编辑 Hosts」按钮，点击跳转 `HostsEditorActivity`，传入当前 hosts 文件来源参数：

- 本地文件：传入 `HOST_URI`
- 远程缓存：传入 `IS_NET=true` 标记，目标文件为 `net_hosts`

### 入口 2：新建并编辑

在 hosts 文件未选择时的对话框中增加「新建并编辑」按钮：
1. 创建空白文件到内部存储
2. 设置 `HOST_URI` 指向新文件，`IS_NET` 设为 false
3. 跳转 `HostsEditorActivity`

## 编辑模式

### 文本模式（TextEditorFragment）

- 全屏 `EditText`，等宽字体（`monospace`）
- 顶部提示栏说明格式："每行一条记录，格式：IP 域名。支持 # 注释"
- 支持通配符条目（如 `127.0.0.1 .a.com`）
- 支持 IPv6 地址（如 `::1 example.com`）

### 逐条模式（RecordEditorFragment）

- 顶部「添加记录」按钮（+）
- `RecyclerView` 显示所有解析出来的记录
- 每条记录卡片包含：IP 输入框、域名输入框、删除按钮（x）
- 注释行显示为灰色样式，可编辑但不会被当作记录解析
- 空行保留，显示为空白条目

### 数据同步

两个 Fragment 不直接持有数据，通过 `HostsEditorActivity` 作为中介：

- `HostsEditorActivity` 持有 `HostsContent` 数据模型（内部是 `List<HostsEntry>`）
- 切换 Tab 时，Fragment 从 Activity 获取当前数据重新渲染
- 任一 Fragment 中内容变更时，提交变更到 Activity 的共享数据模型
- `HostsContent` 提供 `parse(text) → entries` 和 `serialize(entries) → text` 方法

## 保存与即时生效

保存按钮在 ActionBar 右上角，点击后：

1. **校验**：检查所有 IP 格式合法性，非法时标记具体行号，拦截保存
2. **序列化**：`HostsContent.serialize()` 将 entries 拼接为完整文本
3. **写回文件**：
   - 本地文件：通过 `ContentResolver.openOutputStream(uri)` 写入
   - 远程缓存：通过 `Context.openFileOutput("net_hosts")` 写入
4. **更新内存映射**：调用 `DnsChange.handle_hosts(InputStream)` 重新解析，更新 `DOMAINS_IP_MAPS4/6`
5. `finish()` 返回主界面

DNS 映射表已更新，下次 DNS 查询即使用新规则，无需重启 VPN。

## 错误处理

| 场景 | 处理方式 |
|------|----------|
| 文件不可写（权限不足/被删除） | 捕获 `IOException`，Toast 提示"保存失败"，不退出编辑器 |
| IP 格式不合法 | 保存前逐行校验，Toast 提示"第 X 行 IP 格式错误：[内容]" |
| hosts 文件为空 | 允许保存，`DnsChange.handle_hosts()` 返回 0 条记录 |
| 文本模式下格式解析失败 | 无法识别为 IP+域名 的行保留为注释行（自动添加 `#` 前缀） |
| 外部修改冲突 | 进入编辑器时检测文件最后修改时间，不一致则弹出提示 |

## 测试

- `HostsContent.parse()` 边界情况：空行、注释、通配符条目、IPv6 地址、无效行
- `HostsContent.serialize()` 还原度：parse → serialize 应与原始文本一致（注释和空行保留）
- 保存后 `DnsChange.handle_hosts()` 正确更新映射表
