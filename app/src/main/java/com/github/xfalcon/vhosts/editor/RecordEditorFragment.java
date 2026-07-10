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
