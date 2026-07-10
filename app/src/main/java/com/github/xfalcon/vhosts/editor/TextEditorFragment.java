package com.github.xfalcon.vhosts.editor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import com.github.xfalcon.vhosts.R;

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
