package com.github.xfalcon.vhosts.editor;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            return sb.toString();
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to read hosts file", e);
            return "";
        }
    }

    private void switchToTextMode() {
        if (recordEditorFragment.isAdded()) {
            currentEntries = recordEditorFragment.getContent();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, textEditorFragment)
                .commit();
        textEditorFragment.setContent(HostsContent.serialize(currentEntries));
        tabText.setSelected(true);
        tabRecord.setSelected(false);
    }

    private void switchToRecordMode() {
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
        if (textEditorFragment.isAdded()) {
            currentEntries = HostsContent.parse(textEditorFragment.getContent());
        } else if (recordEditorFragment.isAdded()) {
            currentEntries = recordEditorFragment.getContent();
        }

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
