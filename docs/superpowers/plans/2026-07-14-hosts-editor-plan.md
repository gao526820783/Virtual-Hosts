# Hosts 编辑器功能 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Virtual Hosts 添加内置纯文本 hosts 编辑器（语法高亮、行号、搜索替换、撤销重做、快捷插入），并支持一行多域名解析。

**Architecture:** 新增 `HostsEditorActivity` 作为独立编辑器页面，通过主界面"修改 Hosts"按钮的 BottomSheet 菜单进入。所有 hosts 内容统一存储在 `files/user_hosts.txt`（导入/下载均写入此处），编辑后保存到该文件。`DnsChange` 增加一行多域名解析，`VhostsService` 增加新文件路径读取。

**Tech Stack:** Java 8, Android SDK (minSdk 19, target 36), AppCompatActivity, ConstraintLayout, TextWatcher + Spannable, UndoManager (API 26+) with fallback

---

### Task 1: 添加字符串资源

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh/strings.xml`

- [ ] **Step 1: 添加中英文字符串**

在 `res/values/strings.xml` 末尾 `</resources>` 前添加：

```xml
    <string name="btn_modify_hosts">Modify Hosts</string>
    <string name="menu_edit_hosts">Edit</string>
    <string name="menu_new_hosts">New</string>
    <string name="menu_import_hosts">Import from file</string>
    <string name="menu_download_hosts">Download from URL</string>
    <string name="hosts_source_info">Loaded: %1$s (%2$d records)</string>
    <string name="hosts_source_internal">internal file</string>
    <string name="hosts_source_net">remote URL</string>
    <string name="hosts_source_local">local file</string>
    <string name="hosts_source_none">none</string>
    <string name="editor_title">Hosts Editor</string>
    <string name="editor_title_new">New Hosts</string>
    <string name="action_save">Save</string>
    <string name="action_search">Search</string>
    <string name="action_undo">Undo</string>
    <string name="action_redo">Redo</string>
    <string name="search_hint">Find text...</string>
    <string name="search_replace_hint">Replace with...</string>
    <string name="search_prev">Previous</string>
    <string name="search_next">Next</string>
    <string name="search_replace">Replace</string>
    <string name="search_replace_all">Replace All</string>
    <string name="quick_insert_title">Insert Template</string>
    <string name="quick_insert_local">127.0.0.1 .local</string>
    <string name="quick_insert_test">127.0.0.1 .test</string>
    <string name="quick_insert_block">0.0.0.0 .blocked</string>
    <string name="quick_insert_custom">Custom...</string>
    <string name="quick_insert_custom_title">Custom Record</string>
    <string name="quick_insert_custom_ip">IP Address</string>
    <string name="quick_insert_custom_domain">Domain</string>
    <string name="save_success">Saved successfully</string>
    <string name="save_failed">Save failed, please retry</string>
    <string name="import_failed">Import failed</string>
    <string name="import_success">Imported %1$d host records</string>
    <string name="download_failed">Download failed</string>
    <string name="download_success">Downloaded %1$d host records</string>
    <string name="download_url_hint">Enter hosts file URL</string>
    <string name="no_hosts_loaded">No hosts file loaded yet</string>
```

在 `res/values-zh/strings.xml` 中添加对应中文：

```xml
    <string name="btn_modify_hosts">修改 Hosts</string>
    <string name="menu_edit_hosts">编辑</string>
    <string name="menu_new_hosts">新建</string>
    <string name="menu_import_hosts">导入本地文件</string>
    <string name="menu_download_hosts">从 URL 下载</string>
    <string name="hosts_source_info">当前：%1$s (%2$d 条记录)</string>
    <string name="hosts_source_internal">内部文件</string>
    <string name="hosts_source_net">远程 URL</string>
    <string name="hosts_source_local">本地文件</string>
    <string name="hosts_source_none">无</string>
    <string name="editor_title">Hosts 编辑器</string>
    <string name="editor_title_new">新建 Hosts</string>
    <string name="action_save">保存</string>
    <string name="action_search">搜索</string>
    <string name="action_undo">撤销</string>
    <string name="action_redo">重做</string>
    <string name="search_hint">查找...</string>
    <string name="search_replace_hint">替换为...</string>
    <string name="search_prev">上一个</string>
    <string name="search_next">下一个</string>
    <string name="search_replace">替换</string>
    <string name="search_replace_all">全部替换</string>
    <string name="quick_insert_title">插入模板</string>
    <string name="quick_insert_local">127.0.0.1 .local — 本地开发</string>
    <string name="quick_insert_test">127.0.0.1 .test — 测试环境</string>
    <string name="quick_insert_block">0.0.0.0 .blocked — 屏蔽域名</string>
    <string name="quick_insert_custom">自定义...</string>
    <string name="quick_insert_custom_title">自定义记录</string>
    <string name="quick_insert_custom_ip">IP 地址</string>
    <string name="quick_insert_custom_domain">域名</string>
    <string name="save_success">保存成功</string>
    <string name="save_failed">保存失败，请重试</string>
    <string name="import_failed">导入失败</string>
    <string name="import_success">已导入 %1$d 条 host 记录</string>
    <string name="download_failed">下载失败</string>
    <string name="download_success">已下载 %1$d 条 host 记录</string>
    <string name="download_url_hint">输入 hosts 文件 URL</string>
    <string name="no_hosts_loaded">尚未加载 hosts 文件</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh/strings.xml
git commit -m "feat: add hosts editor string resources (en + zh)"
```

---

### Task 2: 创建编辑器布局

**Files:**
- Create: `app/src/main/res/layout/activity_hosts_editor.xml`
- Create: `app/src/main/res/menu/hosts_editor_menu.xml`

- [ ] **Step 1: 创建 Toolbar 菜单**

`app/src/main/res/menu/hosts_editor_menu.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <item
        android:id="@+id/action_save"
        android:title="@string/action_save"
        android:icon="@android:drawable/ic_menu_save"
        app:showAsAction="always" />
    <item
        android:id="@+id/action_search"
        android:title="@string/action_search"
        android:icon="@android:drawable/ic_menu_search"
        app:showAsAction="always" />
    <item
        android:id="@+id/action_undo"
        android:title="@string/action_undo"
        android:icon="@android:drawable/ic_menu_revert"
        app:showAsAction="always" />
    <item
        android:id="@+id/action_redo"
        android:title="@string/action_redo"
        android:icon="@android:drawable/ic_menu_upload"
        app:showAsAction="always" />
</menu>
```

- [ ] **Step 2: 创建编辑器布局**

`app/src/main/res/layout/activity_hosts_editor.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <androidx.appcompat.widget.Toolbar
        android:id="@+id/editor_toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="?attr/colorPrimary"
        android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar"
        app:navigationIcon="?attr/homeAsUpIndicator"
        app:title="@string/editor_title" />

    <!-- 搜索替换栏，默认隐藏 -->
    <LinearLayout
        android:id="@+id/search_bar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="4dp"
        android:gravity="center_vertical"
        android:visibility="gone"
        android:background="@android:color/darker_gray">

        <EditText
            android:id="@+id/search_input"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="@string/search_hint"
            android:singleLine="true"
            android:padding="8dp"
            android:textColor="@android:color/white"
            android:textColorHint="@android:color/darker_gray" />

        <Button
            android:id="@+id/search_prev_btn"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/search_prev"
            style="?android:attr/buttonBarButtonStyle" />

        <Button
            android:id="@+id/search_next_btn"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/search_next"
            style="?android:attr/buttonBarButtonStyle" />

        <EditText
            android:id="@+id/replace_input"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="@string/search_replace_hint"
            android:singleLine="true"
            android:padding="8dp"
            android:textColor="@android:color/white"
            android:textColorHint="@android:color/darker_gray" />

        <Button
            android:id="@+id/search_replace_btn"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/search_replace"
            style="?android:attr/buttonBarButtonStyle" />

        <Button
            android:id="@+id/search_replace_all_btn"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/search_replace_all"
            style="?android:attr/buttonBarButtonStyle" />

        <Button
            android:id="@+id/search_close_btn"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="X"
            style="?android:attr/buttonBarButtonStyle" />
    </LinearLayout>

    <!-- 编辑器主区域 -->
    <HorizontalScrollView
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:scrollbars="none">

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="match_parent"
            android:orientation="horizontal">

            <com.github.xfalcon.vhosts.LineNumberView
                android:id="@+id/line_number_view"
                android:layout_width="48dp"
                android:layout_height="match_parent"
                android:background="#F0F0F0" />

            <EditText
                android:id="@+id/hosts_edit_text"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:minWidth="600dp"
                android:gravity="top|start"
                android:textSize="14sp"
                android:typeface="monospace"
                android:padding="8dp"
                android:scrollbars="vertical"
                android:background="@android:color/transparent"
                android:inputType="textMultiLine|textNoSuggestions" />
        </LinearLayout>
    </HorizontalScrollView>

    <com.github.clans.fab.FloatingActionButton
        android:id="@+id/fab_quick_insert"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:src="@android:drawable/ic_input_add"
        app:fab_label="@string/quick_insert_title" />
</LinearLayout>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/activity_hosts_editor.xml app/src/main/res/menu/hosts_editor_menu.xml
git commit -m "feat: add editor layout and toolbar menu"
```

---

### Task 3: 创建行号 View

**Files:**
- Create: `app/src/main/java/com/github/xfalcon/vhosts/LineNumberView.java`

- [ ] **Step 1: 实现 LineNumberView**

`app/src/main/java/com/github/xfalcon/vhosts/LineNumberView.java`:

```java
package com.github.xfalcon.vhosts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class LineNumberView extends View {

    private EditText editor;
    private Paint paint;

    public LineNumberView(Context context) {
        super(context);
        init();
    }

    public LineNumberView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(0xFF888888);
        paint.setTextSize(36f);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setAntiAlias(true);
    }

    public void setEditor(EditText editor) {
        this.editor = editor;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;
        if (editor != null && editor.getLayout() != null) {
            height = editor.getLayout().getHeight() + editor.getTotalPaddingTop()
                     + editor.getTotalPaddingBottom();
        }
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            Math.max(height, MeasureSpec.getSize(heightMeasureSpec)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (editor == null || editor.getLayout() == null) return;

        int lineCount = editor.getLineCount();
        if (lineCount == 0) return;

        int lineHeight = editor.getLineHeight();
        int paddingTop = editor.getTotalPaddingTop();
        int scrollY = editor.getScrollY();

        // 可见区域
        int firstLine = scrollY / lineHeight;
        if (firstLine < 0) firstLine = 0;

        int visibleLines = getHeight() / lineHeight + 1;
        int lastLine = firstLine + visibleLines;
        if (lastLine > lineCount) lastLine = lineCount;

        int y = paddingTop + (firstLine + 1) * lineHeight - scrollY;
        for (int i = firstLine; i < lastLine; i++) {
            canvas.drawText(String.valueOf(i + 1), getWidth() - 8f, y, paint);
            y += lineHeight;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/LineNumberView.java
git commit -m "feat: add LineNumberView custom view for editor"
```

---

### Task 4: 创建 HostsEditorActivity（核心编辑器）

**Files:**
- Create: `app/src/main/java/com/github/xfalcon/vhosts/HostsEditorActivity.java`

- [ ] **Step 1: 创建编辑器 Activity（含保存、语法高亮、撤销重做、搜索替换、快捷插入）**

`app/src/main/java/com/github/xfalcon/vhosts/HostsEditorActivity.java`:

```java
package com.github.xfalcon.vhosts;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.github.xfalcon.vhosts.util.LogUtils;
import com.github.clans.fab.FloatingActionButton;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostsEditorActivity extends AppCompatActivity {

    private static final String TAG = "HostsEditorActivity";
    private static final String USER_HOSTS_FILE = "user_hosts.txt";
    private static final Pattern IP_PATTERN = Pattern.compile(
        "\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"
    );

    private static final int HIGHLIGHT_DELAY = 300;
    private static final int MAX_UNDO_SIZE = 100;

    private EditText editor;
    private LineNumberView lineNumberView;
    private LinearLayout searchBar;
    private EditText searchInput;
    private EditText replaceInput;
    private boolean isNewFile;

    private Handler highlightHandler = new Handler();
    private Runnable highlightTask;

    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private boolean ignoreTextChange = false;

    private boolean searchVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hosts_editor);

        Toolbar toolbar = findViewById(R.id.editor_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        editor = findViewById(R.id.hosts_edit_text);
        lineNumberView = findViewById(R.id.line_number_view);
        searchBar = findViewById(R.id.search_bar);
        searchInput = findViewById(R.id.search_input);
        replaceInput = findViewById(R.id.replace_input);

        lineNumberView.setEditor(editor);

        // 加载内容
        String content = loadHostsContent();
        if (content != null) {
            editor.setText(content);
            isNewFile = false;
        } else {
            isNewFile = true;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.editor_title_new);
            }
        }

        setupSyntaxHighlighting();
        setupScrollSync();
        setupSearchBar();
        setupQuickInsert();
    }

    /**
     * 语法高亮
     */
    private void setupSyntaxHighlighting() {
        highlightTask = new Runnable() {
            @Override
            public void run() {
                applySyntaxHighlighting();
            }
        };

        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!ignoreTextChange) {
                    String current = s.toString();
                    redoStack.clear();
                    if (undoStack.isEmpty() || !undoStack.peek().equals(current)) {
                        undoStack.push(current);
                        if (undoStack.size() > MAX_UNDO_SIZE) {
                            undoStack.removeElementAt(0);
                        }
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                highlightHandler.removeCallbacks(highlightTask);
                highlightHandler.postDelayed(highlightTask, HIGHLIGHT_DELAY);
                lineNumberView.requestLayout();
                lineNumberView.invalidate();
            }
        });
    }

    private void applySyntaxHighlighting() {
        ignoreTextChange = true;
        Editable editable = editor.getText();
        int selectionStart = editor.getSelectionStart();
        int selectionEnd = editor.getSelectionEnd();

        // 清除旧的高亮 spans
        for (ForegroundColorSpan span : editable.getSpans(0, editable.length(), ForegroundColorSpan.class)) {
            editable.removeSpan(span);
        }

        String text = editable.toString();
        String[] lines = text.split("\n", -1);

        int pos = 0;
        for (String line : lines) {
            int lineLen = line.length();
            String trimmed = line.trim();

            if (trimmed.startsWith("#")) {
                // 注释行 — 灰色
                editable.setSpan(
                    new ForegroundColorSpan(0xFF888888),
                    pos, pos + lineLen,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
                // 高亮 IP 地址
                Matcher m = IP_PATTERN.matcher(line);
                boolean firstIp = true;
                while (m.find()) {
                    int color = firstIp ? 0xFF1565C0 : 0xFF4CAF50;
                    editable.setSpan(
                        new ForegroundColorSpan(color),
                        pos + m.start(), pos + m.end(),
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    firstIp = false;
                }
            }
            pos += lineLen + 1; // +1 for the newline
        }

        editor.setSelection(selectionStart, selectionEnd);
        ignoreTextChange = false;
    }

    /**
     * 行号与编辑区同步滚动
     */
    private void setupScrollSync() {
        editor.getViewTreeObserver().addOnScrollChangedListener(() -> {
            lineNumberView.invalidate();
        });
    }

    /**
     * 搜索替换栏
     */
    private void setupSearchBar() {
        findViewById(R.id.search_prev_btn).setOnClickListener(v -> findPrevious());
        findViewById(R.id.search_next_btn).setOnClickListener(v -> findNext());
        findViewById(R.id.search_replace_btn).setOnClickListener(v -> replaceCurrent());
        findViewById(R.id.search_replace_all_btn).setOnClickListener(v -> replaceAll());
        findViewById(R.id.search_close_btn).setOnClickListener(v -> {
            searchBar.setVisibility(View.GONE);
            searchVisible = false;
            clearSearchHighlights();
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                highlightSearchMatches(s.toString());
            }
        });
    }

    private void highlightSearchMatches(String query) {
        clearSearchHighlights();
        if (query.isEmpty()) return;

        Editable editable = editor.getText();
        String text = editable.toString().toLowerCase();
        String q = query.toLowerCase();
        int index = 0;
        while ((index = text.indexOf(q, index)) >= 0) {
            editable.setSpan(
                new BackgroundColorSpan(0xFFFFFF00),
                index, index + query.length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            index += query.length();
        }
    }

    private void clearSearchHighlights() {
        Editable editable = editor.getText();
        for (BackgroundColorSpan span : editable.getSpans(0, editable.length(), BackgroundColorSpan.class)) {
            editable.removeSpan(span);
        }
    }

    private void findNext() {
        String query = searchInput.getText().toString();
        if (query.isEmpty()) return;
        String text = editor.getText().toString().toLowerCase();
        int start = editor.getSelectionEnd();
        int index = text.indexOf(query.toLowerCase(), start);
        if (index < 0) index = text.indexOf(query.toLowerCase(), 0);
        if (index >= 0) {
            editor.setSelection(index, index + query.length());
        }
    }

    private void findPrevious() {
        String query = searchInput.getText().toString();
        if (query.isEmpty()) return;
        String text = editor.getText().toString().toLowerCase();
        int start = editor.getSelectionStart();
        int index = text.lastIndexOf(query.toLowerCase(), start - 1);
        if (index < 0) index = text.lastIndexOf(query.toLowerCase());
        if (index >= 0) {
            editor.setSelection(index, index + query.length());
        }
    }

    private void replaceCurrent() {
        String query = searchInput.getText().toString();
        String replacement = replaceInput.getText().toString();
        if (query.isEmpty()) return;
        String selected = editor.getText().toString().substring(
            editor.getSelectionStart(), editor.getSelectionEnd()
        );
        if (selected.equalsIgnoreCase(query)) {
            editor.getText().replace(editor.getSelectionStart(), editor.getSelectionEnd(), replacement);
        } else {
            findNext();
        }
    }

    private void replaceAll() {
        String query = searchInput.getText().toString();
        String replacement = replaceInput.getText().toString();
        if (query.isEmpty()) return;
        String text = editor.getText().toString();
        String newText = text.replaceAll("(?i)" + Pattern.quote(query), replacement);
        ignoreTextChange = true;
        editor.setText(newText);
        ignoreTextChange = false;
        highlightSearchMatches(query);
    }

    /**
     * 快捷插入 FAB
     */
    private void setupQuickInsert() {
        FloatingActionButton fab = findViewById(R.id.fab_quick_insert);
        fab.setOnClickListener(v -> showInsertMenu());
    }

    private void showInsertMenu() {
        String[] items = {
            getString(R.string.quick_insert_local),
            getString(R.string.quick_insert_test),
            getString(R.string.quick_insert_block),
            getString(R.string.quick_insert_custom)
        };

        new AlertDialog.Builder(this)
            .setTitle(R.string.quick_insert_title)
            .setItems(items, (dialog, which) -> {
                switch (which) {
                    case 0: insertAtCursor("127.0.0.1 .local"); break;
                    case 1: insertAtCursor("127.0.0.1 .test"); break;
                    case 2: insertAtCursor("0.0.0.0 .blocked"); break;
                    case 3: showCustomInsertDialog(); break;
                }
            })
            .show();
    }

    private void showCustomInsertDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_custom_host, null);
        EditText ipInput = view.findViewById(R.id.custom_ip);
        EditText domainInput = view.findViewById(R.id.custom_domain);
        new AlertDialog.Builder(this)
            .setTitle(R.string.quick_insert_custom_title)
            .setView(view)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String ip = ipInput.getText().toString().trim();
                String domain = domainInput.getText().toString().trim();
                if (!ip.isEmpty() && !domain.isEmpty()) {
                    insertAtCursor(ip + " " + domain);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void insertAtCursor(String text) {
        int start = editor.getSelectionStart();
        Editable editable = editor.getText();
        if (start > 0 && editable.charAt(start - 1) != '\n') {
            text = "\n" + text;
        }
        editable.insert(start, text);
    }

    /**
     * 加载 hosts 内容
     */
    private String loadHostsContent() {
        File file = new File(getFilesDir(), USER_HOSTS_FILE);
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            LogUtils.e(TAG, "Error loading hosts", e);
            return null;
        }
    }

    /**
     * 保存 hosts 内容
     */
    private boolean saveHostsContent(String content) {
        try (FileOutputStream fos = new FileOutputStream(
                new File(getFilesDir(), USER_HOSTS_FILE))) {
            fos.write(content.getBytes());
            return true;
        } catch (IOException e) {
            LogUtils.e(TAG, "Error saving hosts", e);
            return false;
        }
    }

    /**
     * 撤销
     */
    private void undo() {
        if (undoStack.size() <= 1) return;

        String current = editor.getText().toString();
        redoStack.push(current);

        undoStack.pop(); // 移除当前
        String previous = undoStack.peek();
        ignoreTextChange = true;
        editor.setText(previous);
        editor.setSelection(previous.length());
        ignoreTextChange = false;
    }

    /**
     * 重做
     */
    private void redo() {
        if (redoStack.isEmpty()) return;

        String next = redoStack.pop();
        undoStack.push(next);
        ignoreTextChange = true;
        editor.setText(next);
        editor.setSelection(next.length());
        ignoreTextChange = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hosts_editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            String content = editor.getText().toString();
            if (saveHostsContent(content)) {
                setResult(RESULT_OK);
                Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.action_search) {
            if (searchVisible) {
                searchBar.setVisibility(View.GONE);
                searchVisible = false;
                clearSearchHighlights();
            } else {
                searchBar.setVisibility(View.VISIBLE);
                searchVisible = true;
                searchInput.requestFocus();
            }
            return true;
        } else if (id == R.id.action_undo) {
            undo();
            return true;
        } else if (id == R.id.action_redo) {
            redo();
            return true;
        } else if (id == android.R.id.home) {
            handleExit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        handleExit();
    }

    private void handleExit() {
        if (!isNewFile || editor.getText().toString().trim().isEmpty()) {
            finish();
        } else {
            new AlertDialog.Builder(this)
                .setMessage("保存修改？")
                .setPositiveButton("保存", (d, w) -> {
                    saveHostsContent(editor.getText().toString());
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton("不保存", (d, w) -> finish()).show();
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/HostsEditorActivity.java
git commit -m "feat: add HostsEditorActivity with full editor features"
```

---

### Task 5: 创建自定义插入对话框布局

**Files:**
- Create: `app/src/main/res/layout/dialog_custom_host.xml`

- [ ] **Step 1: 创建对话框布局**

`app/src/main/res/layout/dialog_custom_host.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText
        android:id="@+id/custom_ip"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/quick_insert_custom_ip"
        android:singleLine="true"
        android:inputType="textNoSuggestions" />

    <EditText
        android:id="@+id/custom_domain"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/quick_insert_custom_domain"
        android:singleLine="true"
        android:inputType="textUri"
        android:layout_marginTop="8dp" />
</LinearLayout>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/dialog_custom_host.xml
git commit -m "feat: add custom host insert dialog layout"
```

---

### Task 6: 修改 VhostsActivity — 按钮改"修改 Hosts" + BottomSheet

**Files:**
- Modify: `app/src/main/java/com/github/xfalcon/vhosts/VhostsActivity.java`
- Modify: `app/src/main/res/layout/activity_vhosts.xml`

- [ ] **Step 1: 修改布局 — 按钮文字**

`app/src/main/res/layout/activity_vhosts.xml` 中，将 `button_select_hosts` 的 `android:text="@string/re_select_hosts"` 改为 `android:text="@string/btn_modify_hosts"`。

- [ ] **Step 2: 修改 VhostsActivity — 替换按钮逻辑为 BottomSheet**

在 `VhostsActivity.java` 中，将 `selectHosts` 的 `OnClickListener` 和 `OnLongClickListener` 替换为 BottomSheet 菜单。修改 `onCreate()` 中的 `selectHosts` 按钮初始化部分：

```java
// 删除旧的 selectHosts OnClickListener 和 OnLongClickListener
// 替换为：
selectHosts.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        showHostsMenu();
    }
});
```

在 `VhostsActivity` 类中添加 `showHostsMenu()` 方法：

```java
private void showHostsMenu() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_hosts_menu, null);
    builder.setView(sheetView);

    LinearLayout editItem = sheetView.findViewById(R.id.menu_edit);
    LinearLayout newItem = sheetView.findViewById(R.id.menu_new);
    LinearLayout importItem = sheetView.findViewById(R.id.menu_import);
    LinearLayout downloadItem = sheetView.findViewById(R.id.menu_download);
    TextView statusText = sheetView.findViewById(R.id.menu_status);

    final AlertDialog dialog = builder.create();

    // 更新状态
    int recordCount = updateHostsStatus(statusText);

    // 编辑 — 仅当有内容时可用
    if (recordCount > 0) {
        editItem.setAlpha(1.0f);
        editItem.setClickable(true);
    } else {
        editItem.setAlpha(0.4f);
        editItem.setClickable(false);
    }

    editItem.setOnClickListener(v -> {
        dialog.dismiss();
        Intent intent = new Intent(VhostsActivity.this, HostsEditorActivity.class);
        startActivityForResult(intent, EDIT_HOSTS_REQUEST_CODE);
    });

    newItem.setOnClickListener(v -> {
        dialog.dismiss();
        Intent intent = new Intent(VhostsActivity.this, HostsEditorActivity.class);
        intent.putExtra("new_file", true);
        startActivityForResult(intent, EDIT_HOSTS_REQUEST_CODE);
    });

    importItem.setOnClickListener(v -> {
        dialog.dismiss();
        selectFile();
    });

    downloadItem.setOnClickListener(v -> {
        dialog.dismiss();
        showDownloadDialog();
    });

    dialog.show();
}

private int updateHostsStatus(TextView statusText) {
    File file = new File(getFilesDir(), "user_hosts.txt");
    if (file.exists()) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)))) {
            int count = 0;
            while (reader.readLine() != null) count++;
            statusText.setText(getString(R.string.hosts_source_info,
                getString(R.string.hosts_source_internal), count));
            return count;
        } catch (Exception e) {
            statusText.setText(R.string.no_hosts_loaded);
            return 0;
        }
    } else {
        // 检查旧路径
        int oldCount = checkHostUri();
        if (oldCount > 0) {
            statusText.setText(getString(R.string.hosts_source_info,
                oldCount == 2 ? getString(R.string.hosts_source_net) : getString(R.string.hosts_source_local), oldCount));
            return oldCount;
        }
        statusText.setText(R.string.no_hosts_loaded);
        return 0;
    }
}

private void showDownloadDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    View inputView = getLayoutInflater().inflate(R.layout.dialog_url_input, null);
    EditText urlInput = inputView.findViewById(R.id.url_input);
    urlInput.setHint(R.string.download_url_hint);
    builder.setView(inputView);
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            String url = urlInput.getText().toString().trim();
            if (!url.isEmpty()) {
                downloadHosts(url);
            }
        }
    });
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.show();
}

private void downloadHosts(String url) {
    new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                Looper.prepare();
                String result = HttpUtils.get(url);
                FileUtils.writeFile(
                    openFileOutput("user_hosts.txt", Context.MODE_PRIVATE), result);
                int count = DnsChange.handle_hosts(
                    openFileInput("user_hosts.txt"));
                Toast.makeText(VhostsActivity.this,
                    String.format(getString(R.string.download_success), count),
                    Toast.LENGTH_SHORT).show();
                refreshService();
                Looper.loop();
            } catch (Exception e) {
                Looper.prepare();
                Toast.makeText(VhostsActivity.this,
                    R.string.download_failed, Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }
    }).start();
}

private void refreshService() {
    if (VhostsService.isRunning()) {
        // 重启 VPN 服务以重新加载 hosts
        shutdownVPN();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startVPN();
            }
        }, 500);
    }
}
```

添加必要的字段和新常量：

在 `VhostsActivity` 类顶部添加：
```java
private static final int EDIT_HOSTS_REQUEST_CODE = 0x06;
```

并更新 `onActivityResult`，在现有分支后添加：

```java
} else if (requestCode == EDIT_HOSTS_REQUEST_CODE && resultCode == RESULT_OK) {
    // 编辑器保存后刷新服务
    if (VhostsService.isRunning()) {
        refreshService();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/VhostsActivity.java app/src/main/res/layout/activity_vhosts.xml
git commit -m "feat: replace select hosts button with BottomSheet menu"
```

---

### Task 7: 创建 BottomSheet 菜单布局

**Files:**
- Create: `app/src/main/res/layout/bottom_sheet_hosts_menu.xml`

- [ ] **Step 1: 创建 BottomSheet 布局**

`app/src/main/res/layout/bottom_sheet_hosts_menu.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="@android:color/white">

    <LinearLayout
        android:id="@+id/menu_edit"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:clickable="true"
        android:focusable="true"
        android:background="?attr/selectableItemBackground"
        android:paddingStart="16dp"
        android:paddingEnd="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/menu_edit_hosts"
            android:textSize="16sp"
            android:textColor="@android:color/black" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="✏️"
            android:textSize="18sp" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/menu_new"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:clickable="true"
        android:focusable="true"
        android:background="?attr/selectableItemBackground"
        android:paddingStart="16dp"
        android:paddingEnd="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/menu_new_hosts"
            android:textSize="16sp"
            android:textColor="@android:color/black" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="✨"
            android:textSize="18sp" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/menu_import"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:clickable="true"
        android:focusable="true"
        android:background="?attr/selectableItemBackground"
        android:paddingStart="16dp"
        android:paddingEnd="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/menu_import_hosts"
            android:textSize="16sp"
            android:textColor="@android:color/black" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="📂"
            android:textSize="18sp" />
    </LinearLayout>

    <LinearLayout
        android:id="@+id/menu_download"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:clickable="true"
        android:focusable="true"
        android:background="?attr/selectableItemBackground"
        android:paddingStart="16dp"
        android:paddingEnd="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/menu_download_hosts"
            android:textSize="16sp"
            android:textColor="@android:color/black" />
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="🌐"
            android:textSize="18sp" />
    </LinearLayout>

    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="@android:color/darker_gray"
        android:layout_marginTop="8dp"
        android:layout_marginBottom="8dp" />

    <TextView
        android:id="@+id/menu_status"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/no_hosts_loaded"
        android:textSize="13sp"
        android:textColor="@android:color/darker_gray"
        android:paddingStart="16dp"
        android:paddingEnd="16dp" />
</LinearLayout>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/bottom_sheet_hosts_menu.xml
git commit -m "feat: add BottomSheet menu layout for hosts actions"
```

---

### Task 8: 创建 URL 输入对话框布局

**Files:**
- Create: `app/src/main/res/layout/dialog_url_input.xml`

- [ ] **Step 1: 创建 URL 输入对话框布局**

`app/src/main/res/layout/dialog_url_input.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <EditText
        android:id="@+id/url_input"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/download_url_hint"
        android:singleLine="true"
        android:inputType="textUri" />
</LinearLayout>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/dialog_url_input.xml
git commit -m "feat: add URL input dialog layout"
```

---

### Task 9: 更新 DnsChange — 一行多域名支持

**Files:**
- Modify: `app/src/main/java/com/github/xfalcon/vhosts/vservice/DnsChange.java`

- [ ] **Step 1: 修改 handle_hosts() 支持多域名拆分**

在 `handle_hosts()` 方法中，替换当前处理 domain 的逻辑（第 125-129 行附近）。将：

```java
if (ip.contains(":")) {
    DOMAINS_IP_MAPS6.put(matcher.group(3).trim() + ".", ip);
} else {
    DOMAINS_IP_MAPS4.put(matcher.group(3).trim() + ".", ip);
}
```

替换为：

```java
String domainsStr = matcher.group(3).trim();
if (domainsStr.isEmpty()) continue;
String[] domains = domainsStr.split("\\s+");
for (String domain : domains) {
    domain = domain.trim();
    if (domain.isEmpty()) continue;
    if (ip.contains(":")) {
        DOMAINS_IP_MAPS6.put(domain + ".", ip);
    } else {
        DOMAINS_IP_MAPS4.put(domain + ".", ip);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/vservice/DnsChange.java
git commit -m "feat: support multiple domains per host entry"
```

---

### Task 10: 更新 VhostsService — 优先读取 user_hosts.txt

**Files:**
- Modify: `app/src/main/java/com/github/xfalcon/vhosts/vservice/VhostsService.java`

- [ ] **Step 1: 修改 setupHostFile() 增加内部文件路径**

将 `setupHostFile()` 方法（第 135-167 行）中的 InputStream 获取逻辑替换为：

```java
private void setupHostFile() {
    SharedPreferences settings = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
    final boolean is_net = settings.getBoolean(SettingsFragment.IS_NET, false);
    String uri_path = settings.getString(SettingsFragment.HOSTS_URI, null);
    try {
        final InputStream inputStream;
        // 优先读取新的内部 user_hosts.txt
        File userHostsFile = new File(getFilesDir(), "user_hosts.txt");
        if (userHostsFile.exists()) {
            inputStream = new FileInputStream(userHostsFile);
        } else if (is_net) {
            inputStream = openFileInput(SettingsFragment.NET_HOST_FILE);
        } else {
            inputStream = getContentResolver().openInputStream(Uri.parse(uri_path));
        }
        new Thread() {
            public void run() {
                if (DnsChange.handle_hosts(inputStream) == 0) {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), R.string.no_local_record, Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }
        }.start();
    } catch (Exception e) {
        Toast.makeText(getApplicationContext(), R.string.no_local_record, Toast.LENGTH_LONG).show();
        LogUtils.e(TAG, "error setup host file service", e);
    }
}
```

- [ ] **Step 2: 添加必要的 import**

在 VhostsService.java 顶部添加：
```java
import java.io.File;
import java.io.FileInputStream;
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/github/xfalcon/vhosts/vservice/VhostsService.java
git commit -m "feat: read from user_hosts.txt as primary hosts source"
```

---

### Task 11: 注册 HostsEditorActivity 到 AndroidManifest

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 添加 Activity 声明**

在 `DonationActivity` 声明之后添加：

```xml
<activity android:name=".HostsEditorActivity"
          android:label="Hosts Editor"
          android:parentActivityName=".VhostsActivity"
          android:exported="false">
    <meta-data
        android:name="android.support.PARENT_ACTIVITY"
        android:value=".VhostsActivity"/>
</activity>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: register HostsEditorActivity in manifest"
```
