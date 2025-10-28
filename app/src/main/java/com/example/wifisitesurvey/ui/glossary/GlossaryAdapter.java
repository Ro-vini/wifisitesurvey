package com.example.wifisitesurvey.ui.glossary;

import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifisitesurvey.R;

import java.util.ArrayList;
import java.util.List;

public class GlossaryAdapter extends RecyclerView.Adapter<GlossaryAdapter.GlossaryViewHolder> {

    private final List<GlossaryItem> glossaryItems = new ArrayList<>();
    private int expandedPosition = -1;

    public void setGlossaryItems(List<GlossaryItem> items) {
        this.glossaryItems.clear();
        this.glossaryItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GlossaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_glossary, parent, false);
        return new GlossaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GlossaryViewHolder holder, int position) {
        GlossaryItem item = glossaryItems.get(position);
        final boolean isExpanded = position == expandedPosition;

        holder.bind(item, isExpanded);

        holder.itemView.setOnClickListener(v -> {
            if (isExpanded) {
                expandedPosition = -1;
                notifyItemChanged(position);
            } else {
                if (expandedPosition >= 0) {
                    int prevExpanded = expandedPosition;
                    expandedPosition = -1;
                    notifyItemChanged(prevExpanded);
                }
                expandedPosition = position;
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return glossaryItems.size();
    }

    static class GlossaryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTerm;
        private final TextView tvDefinition;
        private final ImageView ivExpandArrow;
        private final LinearLayout llDefinitionContent;

        GlossaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTerm = itemView.findViewById(R.id.tvGlossaryTerm);
            tvDefinition = itemView.findViewById(R.id.tvGlossaryDefinition);
            ivExpandArrow = itemView.findViewById(R.id.ivGlossaryExpandArrow);
            llDefinitionContent = itemView.findViewById(R.id.llGlossaryDefinition);
        }

        void bind(GlossaryItem item, boolean isExpanded) {
            tvTerm.setText(item.getTerm());

            // Processar HTML para formatar o texto
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tvDefinition.setText(Html.fromHtml(item.getDefinition(), Html.FROM_HTML_MODE_COMPACT));
            } else {
                tvDefinition.setText(Html.fromHtml(item.getDefinition()));
            }

            llDefinitionContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            ivExpandArrow.setRotation(isExpanded ? 180 : 0);
        }
    }
}
