package com.github.xfalcon.vhosts;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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

        boolean createNew = getIntent().getBooleanExtra("new_file", false);
        if (createNew) {
            isNewFile = true;
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.editor_title_new);
            }
        } else {
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
        }

        setupSyntaxHighlighting();
        setupScrollSync();
        setupSearchBar();
        setupQuickInsert();
    }

    private void setupSyntaxHighlighting() {
        highlightTask = () -> applySyntaxHighlighting();

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
                editable.setSpan(
                    new ForegroundColorSpan(0xFF888888),
                    pos, pos + lineLen,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
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
            pos += lineLen + 1;
        }

        editor.setSelection(selectionStart, selectionEnd);
        ignoreTextChange = false;
    }

    private void setupScrollSync() {
        editor.getViewTreeObserver().addOnScrollChangedListener(() -> lineNumberView.invalidate());
    }

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

    private void undo() {
        if (undoStack.size() <= 1) return;

        String current = editor.getText().toString();
        redoStack.push(current);

        undoStack.pop();
        String previous = undoStack.peek();
        ignoreTextChange = true;
        editor.setText(previous);
        editor.setSelection(previous.length());
        ignoreTextChange = false;
    }

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
