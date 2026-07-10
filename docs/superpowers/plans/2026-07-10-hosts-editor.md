# Hosts 文件手动编辑功能 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Virtual Hosts App 中增加 hosts 文件编辑器，支持文本模式和逐条记录模式，保存后即时更新 DNS 映射表。

**Architecture:** 新建 `editor` 包，包含 `HostsContent` 数据模型（解析/序列化 hosts 文本）、两个 Fragment（文本/逐条）、一个 Activity（ViewPager 切换）。主界面 `VhostsActivity` 增加编辑按钮入口。保存时通过 `ContentResolver` 写回文件，调用 `DnsChange.handle_hosts()` 即时更新内存映射。

**Tech Stack:** Java 8, Android Support Library (appcompat-v7), 自定义 Button 切换 Fragment（无需新依赖）

---

## 文件清单

| 操作 | 路径 |
|------|------|
| 创建 | `app/src/main/java/com/github/xfalcon/vhosts/editor/HostsContent.java` |
| 创建 | `app/src/test/java/com/github/xfalcon/vhosts/editor/HostsContentTest.java` |
| 创建 | `app/src/main/res/layout/activity_hosts_editor.xml` |
| 创建 | `app/src/main/res/layout/fragment_text_editor.xml` |
| 创建 | `app/src/main/res/layout/fragment_record_editor.xml` |
| 创建 | `app/src/main/res/layout/item_host_record.xml` |
| 创建 | `app/src/main/java/com/github/xfalcon/vhosts/editor/TextEditorFragment.java` |
| 创建 | `app/src/main/java/com/github/xfalcon/vhosts/editor/RecordEditorFragment.java` |
| 创建 | `app/src/main/java/com/github/xfalcon/vhosts/editor/RecordAdapter.java` |
| 创建 | `app/src/main/java/com/github/xfalcon/vhosts/editor/HostsEditorActivity.java` |
| 修改 | `app/src/main/java/com/github/xfalcon/vhosts/VhostsActivity.java` |
| 修改 | `app/src/main/res/layout/activity_vhosts.xml` |
| 修改 | `app/src/main/res/values/strings.xml` |
| 修改 | `app/src/main/AndroidManifest.xml` |

---

### Task 1: HostsContent 数据模型

**Files:**
- Create: `app/src/main/java/com/github/xfalcon/vhosts/editor/HostsContent.java`
- Create: `app/src/test/java/com/github/xfalcon/vhosts/editor/HostsContentTest.java`

- [ ] **Step 1: 写失败的单元测试**

创建 `app/src/main/java/com/github/xfalcon/vhosts/editor/` 目录：

```bash
mkdir -p app/src/main/java/com/github/xfalcon/vhosts/editor
mkdir -p app/src/test/java/com/github/xfalcon/vhosts/editor
```

创建测试文件 `app/src/test/java/com/github/xfalcon/vhosts/editor/HostsContentTest.java`：

```java
package com.github.xfalcon.vhosts.editor;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class HostsContentTest {

    @Test
    public void parse_basicEntry() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("127.0.0.1 example.com");
        assertEquals(1, entries.size());
        assertEquals("127.0.0.1", entries.get(0).ip);
        assertEquals("example.com", entries.get(0).domain);
        assertFalse(entries.get(0).isComment);
    }

    @Test
    public void parse_wildcardEntry() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("127.0.0.1 .a.com");
        assertEquals(1, entries.size());
        assertEquals("127.0.0.1", entries.get(0).ip);
        assertEquals(".a.com", entries.get(0).domain);
    }

    @Test
    public void parse_ipv6Entry() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("::1 localhost");
        assertEquals(1, entries.size());
        assertEquals("::1", entries.get(0).ip);
        assertEquals("localhost", entries.get(0).domain);
    }

    @Test
    public void parse_commentLine() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("# this is a comment");
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isComment);
        assertEquals("# this is a comment", entries.get(0).comment);
    }

    @Test
    public void parse_emptyLine() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("");
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isEmpty);
    }

    @Test
    public void parse_invalidIpline_returnsComment() {
        // 无效 IP 格式的行应该被当作注释行保留
        List<HostsContent.HostsEntry> entries = HostsContent.parse("notanip example.com");
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isComment);
    }

    @Test
    public void parse_multipleLines() {
        String input = "127.0.0.1 example.com\n# comment\n::1 localhost\n\n192.168.1.1 .test.com";
        List<HostsContent.HostsEntry> entries = HostsContent.parse(input);
        assertEquals(5, entries.size());
        assertFalse(entries.get(0).isComment);
        assertTrue(entries.get(1).isComment);
        assertEquals("::1", entries.get(2).ip);
        assertTrue(entries.get(3).isEmpty);
        assertEquals(".test.com", entries.get(4).domain);
    }

    @Test
    public void serialize_basicEntries() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("127.0.0.1 example.com\n# note");
        String result = HostsContent.serialize(entries);
        assertEquals("127.0.0.1 example.com\n# note", result);
    }

    @Test
    public void serialize_roundTrip() {
        String input = "127.0.0.1 example.com\n# a comment\n192.168.1.1 .test.com\n\n::1 localhost";
        List<HostsContent.HostsEntry> entries = HostsContent.parse(input);
        String result = HostsContent.serialize(entries);
        assertEquals(input, result);
    }

    @Test
    public void parse_nullOrEmpty_returnsEmptyList() {
        assertEquals(0, HostsContent.parse(null).size());
        assertEquals(0, HostsContent.parse("").size());
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

```bash
./gradlew test --tests "com.github.xfalcon.vhosts.editor.HostsContentTest"
```

预期：编译失败，`HostsContent` 类不存在。

- [ ] **Step 3: 实现 HostsContent**

创建 `app/src/main/java/com/github/xfalcon/vhosts/editor/HostsContent.java`：

```java
package com.github.xfalcon.vhosts.editor;

import org.xbill.DNS.Address;

import java.util.ArrayList;
import java.util.List;

public class HostsContent {

    public static class HostsEntry {
        public String ip;
        public String domain;
        public String comment;
        public boolean isComment;
        public boolean isEmpty;

        public static HostsEntry createRecord(String ip, String domain) {
            HostsEntry e = new HostsEntry();
            e.ip = ip;
            e.domain = domain;
            e.isComment = false;
            e.isEmpty = false;
            return e;
        }

        public static HostsEntry createComment(String comment) {
            HostsEntry e = new HostsEntry();
            e.comment = comment;
            e.isComment = true;
            e.isEmpty = false;
            return e;
        }

        public static HostsEntry createEmpty() {
            HostsEntry e = new HostsEntry();
            e.isEmpty = true;
            e.isComment = false;
            return e;
        }
    }

    public static List<HostsEntry> parse(String text) {
        List<HostsEntry> entries = new ArrayList<>();
        if (text == null || text.isEmpty()) return entries;

        String[] lines = text.split("\\n", -1); // -1 to keep trailing empty lines
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                entries.add(HostsEntry.createEmpty());
            } else if (trimmed.startsWith("#")) {
                entries.add(HostsEntry.createComment(trimmed));
            } else {
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length == 2) {
                    String ip = parts[0].trim();
                    String domain = parts[1].trim();
                    try {
                        Address.getByAddress(ip);
                        entries.add(HostsEntry.createRecord(ip, domain));
                    } catch (Exception e) {
                        // Invalid IP, treat entire line as a comment to preserve it
                        entries.add(HostsEntry.createComment(trimmed));
                    }
                } else {
                    // Can't parse as record, keep as comment
                    entries.add(HostsEntry.createComment(trimmed));
                }
            }
        }
        return entries;
    }

    public static String serialize(List<HostsEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            HostsEntry entry = entries.get(i);
            if (entry.isEmpty) {
                sb.append("");
            } else if (entry.isComment) {
                sb.append(entry.comment != null ? entry.comment : "");
            } else {
                sb.append(entry.ip).append(" ").append(entry.domain);
            }
            if (i < entries.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

```bash
./gradlew test --tests "com.github.xfalcon.vhosts.editor.HostsContentTest"
```

预期：所有 10 个测试 PASS。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/editor/HostsContent.java \
        app/src/test/java/com/github/xfalcon/vhosts/editor/HostsContentTest.java
git commit -m "feat: add HostsContent data model for hosts file parsing/serialization

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: 编辑器布局 XML 文件

**Files:**
- Create: `app/src/main/res/layout/activity_hosts_editor.xml`
- Create: `app/src/main/res/layout/fragment_text_editor.xml`
- Create: `app/src/main/res/layout/fragment_record_editor.xml`
- Create: `app/src/main/res/layout/item_host_record.xml`

- [ ] **Step 1: 创建 Editor Activity 布局**

创建 `app/src/main/res/layout/activity_hosts_editor.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <LinearLayout
        android:id="@+id/tab_bar"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:orientation="horizontal"
        android:background="@color/primary">

        <Button
            android:id="@+id/tab_text"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:text="文本模式"
            android:textColor="@color/icons"
            android:background="?attr/selectableItemBackground"
            style="?android:attr/borderlessButtonStyle" />

        <Button
            android:id="@+id/tab_record"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:text="逐条模式"
            android:textColor="@color/icons"
            android:background="?attr/selectableItemBackground"
            style="?android:attr/borderlessButtonStyle" />
    </LinearLayout>

    <FrameLayout
        android:id="@+id/fragment_container"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
</LinearLayout>
```

- [ ] **Step 2: 创建文本编辑器 Fragment 布局**

创建 `app/src/main/res/layout/fragment_text_editor.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <TextView
        android:id="@+id/text_format_hint"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="8dp"
        android:background="#FFF3E0"
        android:text="每行一条记录，格式：IP 域名。支持 # 注释"
        android:textColor="@color/primary_text"
        android:textSize="13sp" />

    <EditText
        android:id="@+id/edit_text_hosts"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="12dp"
        android:gravity="top|start"
        android:typeface="monospace"
        android:textSize="14sp"
        android:background="@android:color/white"
        android:textColor="@color/primary_text"
        android:inputType="textMultiLine|textNoSuggestions" />
</LinearLayout>
```

- [ ] **Step 3: 创建逐条编辑器 Fragment 布局**

创建 `app/src/main/res/layout/fragment_record_editor.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <Button
        android:id="@+id/btn_add_record"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="+ 添加记录"
        android:background="@color/primary"
        android:textColor="@color/icons" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recycler_records"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="8dp" />
</LinearLayout>
```

- [ ] **Step 4: 创建单条记录列表项布局**

创建 `app/src/main/res/layout/item_host_record.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    app:cardElevation="2dp"
    app:cardCornerRadius="4dp"
    app:cardUseCompatPadding="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="8dp">

        <!-- 非注释/非空行：显示 IP + 域名输入框 -->
        <LinearLayout
            android:id="@+id/record_fields"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:visibility="visible">

            <EditText
                android:id="@+id/edit_ip"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="0.35"
                android:hint="IP"
                android:textSize="14sp"
                android:singleLine="true"
                android:inputType="textNoSuggestions"
                android:padding="4dp" />

            <EditText
                android:id="@+id/edit_domain"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="0.5"
                android:hint="域名"
                android:textSize="14sp"
                android:singleLine="true"
                android:inputType="textNoSuggestions"
                android:padding="4dp" />

            <ImageButton
                android:id="@+id/btn_delete"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="0.15"
                android:src="@android:drawable/ic_menu_close_clear_cancel"
                android:background="?attr/selectableItemBackground"
                android:contentDescription="删除" />
        </LinearLayout>

        <!-- 注释行：显示编辑框 -->
        <EditText
            android:id="@+id/edit_comment"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="# 注释"
            android:textSize="14sp"
            android:singleLine="true"
            android:inputType="textNoSuggestions"
            android:textColor="@color/secondary_text"
            android:padding="4dp"
            android:visibility="gone" />

        <!-- 空行：只显示占位 -->
        <TextView
            android:id="@+id/text_empty"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="(空行)"
            android:textColor="@color/secondary_text"
            android:textSize="14sp"
            android:padding="4dp"
            android:visibility="gone" />
    </LinearLayout>
</androidx.cardview.widget.CardView>
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/res/layout/activity_hosts_editor.xml \
        app/src/main/res/layout/fragment_text_editor.xml \
        app/src/main/res/layout/fragment_record_editor.xml \
        app/src/main/res/layout/item_host_record.xml
git commit -m "feat: add editor layout XML files

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: 字符串资源

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 添加新字符串**

在 `app/src/main/res/values/strings.xml` 的 `</resources>` 前添加：

```xml
    <string name="edit_hosts">编辑 Hosts</string>
    <string name="new_hosts">新建并编辑</string>
    <string name="tab_text_mode">文本模式</string>
    <string name="tab_record_mode">逐条模式</string>
    <string name="add_record">+ 添加记录</string>
    <string name="save">保存</string>
    <string name="save_success">保存成功</string>
    <string name="save_failed">保存失败</string>
    <string name="ip_format_error">第 %d 行 IP 格式错误：%s</string>
    <string name="text_format_hint">每行一条记录，格式：IP 域名。支持 # 注释</string>
    <string name="record_empty">(空行)</string>
    <string name="delete_record">删除</string>
    <string name="file_changed_external">文件已被外部修改，是否覆盖？</string>
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add editor string resources

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: TextEditorFragment

**Files:**
- Create: `app/src/main/java/com/github/xfalcon/vhosts/editor/TextEditorFragment.java`

- [ ] **Step 1: 实现 TextEditorFragment**

创建 `app/src/main/java/com/github/xfalcon/vhosts/editor/TextEditorFragment.java`：

```java
package com.github.xfalcon.vhosts.editor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import com.github.xfalcon.vhosts.R;
import java.util.List;

public class TextEditorFragment extends Fragment {

    private EditText editText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_text_editor, container, false);
        editText = view.findViewById(R.id.edit_text_hosts);
        return view;
    }

    public void setContent(String text) {
        if (editText != null && text != null) {
            editText.setText(text);
        }
    }

    public String getContent() {
        if (editText != null) {
            return editText.getText().toString();
        }
        return "";
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/editor/TextEditorFragment.java
git commit -m "feat: add TextEditorFragment for raw hosts text editing

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: RecordAdapter

**Files:**
- Create: `app/src/main/java/com/github/xfalcon/vhosts/editor/RecordAdapter.java`

- [ ] **Step 1: 实现 RecordAdapter**

创建 `app/src/main/java/com/github/xfalcon/vhosts/editor/RecordAdapter.java`：

```java
package com.github.xfalcon.vhosts.editor;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.github.xfalcon.vhosts.R;
import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.ViewHolder> {

    private List<HostsContent.HostsEntry> entries = new ArrayList<>();

    public void setEntries(List<HostsContent.HostsEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<HostsContent.HostsEntry>();
        notifyDataSetChanged();
    }

    public List<HostsContent.HostsEntry> getEntries() {
        // Sync any pending edits back to the model before returning
        notifyDataSetChanged();
        return entries;
    }

    public void addEntry() {
        entries.add(HostsContent.HostsEntry.createRecord("", ""));
        notifyItemInserted(entries.size() - 1);
    }

    public void deleteEntry(int position) {
        if (position >= 0 && position < entries.size()) {
            entries.remove(position);
            notifyItemRemoved(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        HostsContent.HostsEntry entry = entries.get(position);
        if (entry.isEmpty) return 2;
        if (entry.isComment) return 1;
        return 0;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_host_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        HostsContent.HostsEntry entry = entries.get(position);
        holder.bind(entry, position);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout recordFields;
        EditText editIp, editDomain, editComment;
        TextView textEmpty;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            recordFields = itemView.findViewById(R.id.record_fields);
            editIp = itemView.findViewById(R.id.edit_ip);
            editDomain = itemView.findViewById(R.id.edit_domain);
            editComment = itemView.findViewById(R.id.edit_comment);
            textEmpty = itemView.findViewById(R.id.text_empty);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(HostsContent.HostsEntry entry, int position) {
            // Remove old listeners to avoid triggering on rebind
            editIp.setOnFocusChangeListener(null);
            editDomain.setOnFocusChangeListener(null);
            editComment.setOnFocusChangeListener(null);

            if (entry.isEmpty) {
                recordFields.setVisibility(View.GONE);
                editComment.setVisibility(View.GONE);
                textEmpty.setVisibility(View.VISIBLE);
            } else if (entry.isComment) {
                recordFields.setVisibility(View.GONE);
                textEmpty.setVisibility(View.GONE);
                editComment.setVisibility(View.VISIBLE);
                editComment.setText(entry.comment);
                editComment.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            entry.comment = editComment.getText().toString();
                        }
                    }
                });
            } else {
                recordFields.setVisibility(View.VISIBLE);
                editComment.setVisibility(View.GONE);
                textEmpty.setVisibility(View.GONE);
                editIp.setText(entry.ip);
                editDomain.setText(entry.domain);
                editIp.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            entry.ip = editIp.getText().toString();
                        }
                    }
                });
                editDomain.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            entry.domain = editDomain.getText().toString();
                        }
                    }
                });
            }

            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteEntry(getAdapterPosition());
                }
            });
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/editor/RecordAdapter.java
git commit -m "feat: add RecordAdapter for structured hosts entry editing

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: RecordEditorFragment

**Files:**
- Create: `app/src/main/java/com/github/xfalcon/vhosts/editor/RecordEditorFragment.java`

- [ ] **Step 1: 实现 RecordEditorFragment**

创建 `app/src/main/java/com/github/xfalcon/vhosts/editor/RecordEditorFragment.java`：

```java
package com.github.xfalcon.vhosts.editor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.xfalcon.vhosts.R;
import java.util.ArrayList;
import java.util.List;

public class RecordEditorFragment extends Fragment {

    private RecordAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record_editor, container, false);
        recyclerView = view.findViewById(R.id.recycler_records);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecordAdapter();
        recyclerView.setAdapter(adapter);

        Button btnAdd = view.findViewById(R.id.btn_add_record);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.addEntry();
                recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });

        return view;
    }

    public void setContent(List<HostsContent.HostsEntry> entries) {
        if (adapter != null) {
            adapter.setEntries(entries);
        }
    }

    public List<HostsContent.HostsEntry> getContent() {
        if (adapter != null) {
            return adapter.getEntries();
        }
        return new ArrayList<>();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/editor/RecordEditorFragment.java
git commit -m "feat: add RecordEditorFragment for structured hosts editing

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: HostsEditorActivity

**Files:**
- Create: `app/src/main/java/com/github/xfalcon/vhosts/editor/HostsEditorActivity.java`

- [ ] **Step 1: 实现 HostsEditorActivity**

创建 `app/src/main/java/com/github/xfalcon/vhosts/editor/HostsEditorActivity.java`：

```java
package com.github.xfalcon.vhosts.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.github.xfalcon.vhosts.R;
import com.github.xfalcon.vhosts.SettingsFragment;
import com.github.xfalcon.vhosts.util.LogUtils;
import com.github.xfalcon.vhosts.vservice.DnsChange;
import org.xbill.DNS.Address;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

public class HostsEditorActivity extends AppCompatActivity {

    private static final String TAG = HostsEditorActivity.class.getSimpleName();
    public static final String EXTRA_IS_NET = "is_net";
    public static final String EXTRA_HOSTS_URI = "hosts_uri";

    private TextEditorFragment textEditorFragment;
    private RecordEditorFragment recordEditorFragment;
    private Button tabText, tabRecord;

    private boolean isNet;
    private String hostsUri;
    private List<HostsContent.HostsEntry> currentEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hosts_editor);

        Intent intent = getIntent();
        isNet = intent.getBooleanExtra(EXTRA_IS_NET, false);
        hostsUri = intent.getStringExtra(EXTRA_HOSTS_URI);

        tabText = findViewById(R.id.tab_text);
        tabRecord = findViewById(R.id.tab_record);

        textEditorFragment = new TextEditorFragment();
        recordEditorFragment = new RecordEditorFragment();

        tabText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToTextMode();
            }
        });
        tabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToRecordMode();
            }
        });

        loadContent();
        switchToTextMode();
    }

    private void loadContent() {
        String text = readHostsFile();
        currentEntries = HostsContent.parse(text);
        textEditorFragment.setContent(text);
    }

    private String readHostsFile() {
        try {
            InputStream inputStream = openHostsInputStream();
            if (inputStream == null) return "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            inputStream.close();
            // Remove trailing newline
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            return sb.toString();
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to read hosts file", e);
            return "";
        }
    }

    private void switchToTextMode() {
        // Sync record mode changes first
        if (recordEditorFragment.isAdded()) {
            currentEntries = recordEditorFragment.getContent();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, textEditorFragment)
                .commit();
        // Update text mode with latest entries
        textEditorFragment.setContent(HostsContent.serialize(currentEntries));
        tabText.setSelected(true);
        tabRecord.setSelected(false);
    }

    private void switchToRecordMode() {
        // Sync text mode changes first
        if (textEditorFragment.isAdded()) {
            currentEntries = HostsContent.parse(textEditorFragment.getContent());
        }
        recordEditorFragment.setContent(currentEntries);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, recordEditorFragment)
                .commit();
        tabText.setSelected(false);
        tabRecord.setSelected(true);
    }

    private InputStream openHostsInputStream() throws Exception {
        if (isNet) {
            return openFileInput(SettingsFragment.NET_HOST_FILE);
        }
        Uri uri = Uri.parse(hostsUri);
        if ("file".equals(uri.getScheme())) {
            return new java.io.FileInputStream(uri.getPath());
        }
        return getContentResolver().openInputStream(uri);
    }

    private OutputStream openHostsOutputStream() throws Exception {
        if (isNet) {
            return openFileOutput(SettingsFragment.NET_HOST_FILE, Context.MODE_PRIVATE);
        }
        Uri uri = Uri.parse(hostsUri);
        if ("file".equals(uri.getScheme())) {
            return new java.io.FileOutputStream(uri.getPath());
        }
        return getContentResolver().openOutputStream(uri);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(android.R.menu.class, menu);
        // Add a save item
        menu.add(0, 1, 0, R.string.save)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            save();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        // Get latest entries
        if (textEditorFragment.isAdded()) {
            currentEntries = HostsContent.parse(textEditorFragment.getContent());
        } else if (recordEditorFragment.isAdded()) {
            currentEntries = recordEditorFragment.getContent();
        }

        // Validate IPs
        for (int i = 0; i < currentEntries.size(); i++) {
            HostsContent.HostsEntry entry = currentEntries.get(i);
            if (!entry.isComment && !entry.isEmpty) {
                try {
                    Address.getByAddress(entry.ip);
                } catch (Exception e) {
                    String msg = getString(R.string.ip_format_error, i + 1, entry.ip);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        String content = HostsContent.serialize(currentEntries);

        // Write to file
        try {
            OutputStream outputStream = openHostsOutputStream();
            if (outputStream == null) {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
                return;
            }
            outputStream.write(content.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to write hosts file", e);
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
            return;
        }

        // Update in-memory DNS maps
        try {
            InputStream inputStream = new java.io.ByteArrayInputStream(content.getBytes());
            DnsChange.handle_hosts(inputStream);
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to update DNS maps", e);
        }

        Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();
        finish();
    }
}
```

- [ ] **Step 2: 注册 Activity 到 AndroidManifest**

在 `app/src/main/AndroidManifest.xml` 的 `<application>` 内，`</application>` 前添加：

```xml
        <activity android:name=".editor.HostsEditorActivity"
                  android:label="编辑 Hosts"
                  android:parentActivityName=".VhostsActivity"
                  android:exported="false">
            <meta-data
                    android:name="android.support.PARENT_ACTIVITY"
                    android:value=".VhostsActivity"/>
        </activity>
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/editor/HostsEditorActivity.java \
        app/src/main/AndroidManifest.xml
git commit -m "feat: add HostsEditorActivity with dual-mode editor

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 8: 修改 VhostsActivity — 主界面入口

**Files:**
- Modify: `app/src/main/res/layout/activity_vhosts.xml`
- Modify: `app/src/main/java/com/github/xfalcon/vhosts/VhostsActivity.java`

- [ ] **Step 1: 修改布局 — 添加编辑按钮**

在 `app/src/main/res/layout/activity_vhosts.xml` 中，在 `button_select_hosts` 下方添加编辑按钮。

找到：
```xml
    <Button
            android:id="@+id/button_select_hosts"
```
在其后添加：
```xml

    <Button
            android:id="@+id/button_edit_hosts"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="编辑 Hosts"
            android:paddingTop="16dp"
            android:paddingBottom="16dp"
            android:textSize="16sp"
            android:visibility="gone"
            android:layout_marginStart="8dp"
            android:layout_marginEnd="8dp"
            android:layout_marginRight="8dp"
            app:layout_constraintRight_toRightOf="parent" android:layout_marginLeft="8dp"
            app:layout_constraintLeft_toLeftOf="parent" android:layout_marginTop="8dp"
            app:layout_constraintTop_toBottomOf="@+id/button_select_hosts" app:layout_constraintBottom_toBottomOf="parent"
            android:layout_marginBottom="8dp"/>
```

同时更新 `button_select_hosts` 的底部约束，从 `app:layout_constraintBottom_toBottomOf="parent"` 改为 `app:layout_constraintBottom_toTopOf="@+id/button_edit_hosts"`。

- [ ] **Step 2: 修改 VhostsActivity 代码**

在 `app/src/main/java/com/github/xfalcon/vhosts/VhostsActivity.java` 中：

**a) 添加 import：**
```java
import com.github.xfalcon.vhosts.editor.HostsEditorActivity;
```

**b) 在 `onCreate` 方法的类字段区域，添加 Button 声明：**
```java
private Button editHostsButton;
```

**c) 在 `onCreate` 方法中，`Button selectHosts = findViewById(R.id.button_select_hosts);` 之后添加：**
```java
editHostsButton = findViewById(R.id.button_edit_hosts);
```

**d) 在 `FloatingActionButton fab_donation` 的 onClickListener 注册之后，添加编辑按钮点击事件：**
```java
editHostsButton.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        SharedPreferences settings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(VhostsActivity.this);
        Intent intent = new Intent(VhostsActivity.this, HostsEditorActivity.class);
        intent.putExtra(HostsEditorActivity.EXTRA_IS_NET, settings.getBoolean(SettingsFragment.IS_NET, false));
        intent.putExtra(HostsEditorActivity.EXTRA_HOSTS_URI, settings.getString(SettingsFragment.HOSTS_URI, ""));
        startActivity(intent);
    }
});
```

**e) 更新 `setButton` 方法，控制编辑按钮可见性：**

在 `setButton` 方法中，`enable` 为 true 时隐藏编辑按钮（因为还没选择文件），`enable` 为 false 时显示编辑按钮：

```java
private void setButton(boolean enable) {
    final SwitchButton vpnButton = (SwitchButton) findViewById(R.id.button_start_vpn);
    final Button selectHosts = (Button) findViewById(R.id.button_select_hosts);
    final Button editHosts = (Button) findViewById(R.id.button_edit_hosts);
    if (enable) {
        vpnButton.setChecked(false);
        selectHosts.setAlpha(1.0f);
        selectHosts.setClickable(true);
        editHosts.setVisibility(View.GONE);
    } else {
        vpnButton.setChecked(true);
        selectHosts.setAlpha(.5f);
        selectHosts.setClickable(false);
        editHosts.setVisibility(View.VISIBLE);
    }
}
```

需要额外 import `android.view.View`（已存在，无需改动）。

**f) 在 `showDialog` 方法中添加「新建并编辑」按钮：**

修改 `showDialog` 方法，在现有两个按钮之后添加第三个按钮：

```java
builder.setNeutralButton(getString(R.string.new_hosts), new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        // Create empty hosts file
        try {
            java.io.OutputStream os = openFileOutput("custom_hosts", Context.MODE_PRIVATE);
            os.write("# Custom Hosts File\n".getBytes());
            os.flush();
            os.close();

            SharedPreferences settings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(VhostsActivity.this);
            SharedPreferences.Editor editor = settings.edit();
            // Use a file URI path
            String filePath = getFilesDir().getAbsolutePath() + "/custom_hosts";
            Uri fileUri = Uri.parse("file://" + filePath);
            editor.putString(SettingsFragment.HOSTS_URI, fileUri.toString());
            editor.putBoolean(SettingsFragment.IS_NET, false);
            editor.apply();

            Intent intent = new Intent(VhostsActivity.this, HostsEditorActivity.class);
            intent.putExtra(HostsEditorActivity.EXTRA_IS_NET, false);
            intent.putExtra(HostsEditorActivity.EXTRA_HOSTS_URI, fileUri.toString());
            startActivity(intent);
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to create new hosts file", e);
            Toast.makeText(VhostsActivity.this, "创建文件失败", Toast.LENGTH_SHORT).show();
        }
    }
});
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/res/layout/activity_vhosts.xml \
        app/src/main/java/com/github/xfalcon/vhosts/VhostsActivity.java
git commit -m "feat: add edit button and new-file flow to VhostsActivity

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 9: 构建验证

- [ ] **Step 1: 编译验证**

```bash
./gradlew assembleDebug
```

预期：BUILD SUCCESSFUL，无编译错误。

- [ ] **Step 2: 运行测试**

```bash
./gradlew test
```

预期：所有测试 PASS，特别是 `HostsContentTest` 的 10 个测试。

- [ ] **Step 3: 最终提交（如有修改）**

```bash
git status
```
