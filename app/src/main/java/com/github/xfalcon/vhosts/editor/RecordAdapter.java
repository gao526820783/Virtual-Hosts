package com.github.xfalcon.vhosts.editor;

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
        // The entries are mutated in-place via ViewHolder bindings, so return as-is
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
