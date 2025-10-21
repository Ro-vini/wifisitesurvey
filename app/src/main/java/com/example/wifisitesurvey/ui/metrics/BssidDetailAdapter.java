package com.example.wifisitesurvey.ui.metrics;

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

public class BssidDetailAdapter extends RecyclerView.Adapter<BssidDetailAdapter.BssidViewHolder> {

    private List<BssidInfo> bssidList = new ArrayList<>();
    private final LayoutInflater inflater;

    public BssidDetailAdapter(android.content.Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setBssidList(List<BssidInfo> bssidList) {
        this.bssidList = bssidList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BssidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla o NOVO layout de card
        View view = inflater.inflate(R.layout.list_item_bssid_detail, parent, false);
        return new BssidViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BssidViewHolder holder, int position) {
        // A lógica de "isLastItem" foi removida, pois o layout agora é um card
        holder.bind(bssidList.get(position));
    }

    @Override
    public int getItemCount() {
        return bssidList.size();
    }

    /**
     * ViewHolder reescrito para suportar o novo layout expansível
     */
    class BssidViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBssidHeader;
        private final TextView tvBssidDetails;
        private final TextView tvBssidCollision;
        private final ImageView ivBssidExpandArrow;
        private final LinearLayout llBssidDetailsContent;
        // O 'divider' não é mais controlado aqui, ele está dentro do llBssidDetailsContent

        public BssidViewHolder(@NonNull View itemView) {
            super(itemView);
            // Mapeia os IDs do NOVO layout
            tvBssidHeader = itemView.findViewById(R.id.tvBssidHeader);
            tvBssidDetails = itemView.findViewById(R.id.tvBssidDetails);
            tvBssidCollision = itemView.findViewById(R.id.tvBssidCollision);
            ivBssidExpandArrow = itemView.findViewById(R.id.ivBssidExpandArrow);
            llBssidDetailsContent = itemView.findViewById(R.id.llBssidDetailsContent);
        }

        /**
         * Método bind reescrito para controlar a expansão
         */
        public void bind(BssidInfo item) {
            // 1. Configurar Header (sempre visível)
            tvBssidHeader.setText("BSSID: " + item.getBssid());

            // 2. Configurar Detalhes (dentro do container expansível)
            tvBssidDetails.setText(item.getDetails());

            if (item.getCollisionReport() != null && !item.getCollisionReport().isEmpty()) {
                tvBssidCollision.setText(item.getCollisionReport());
                tvBssidCollision.setVisibility(View.VISIBLE);
            } else {
                tvBssidCollision.setVisibility(View.GONE);
            }

            // 3. Controlar Visibilidade e Seta baseado no estado do item
            if (item.isExpanded()) {
                llBssidDetailsContent.setVisibility(View.VISIBLE);
                ivBssidExpandArrow.setImageResource(R.drawable.ic_keyboard_arrow_up);
            } else {
                llBssidDetailsContent.setVisibility(View.GONE);
                ivBssidExpandArrow.setImageResource(R.drawable.ic_keyboard_arrow_down);
            }

            // 4. Configurar o Clique no card INTEIRO
            itemView.setOnClickListener(v -> {
                // Inverte o estado de expansão
                item.setExpanded(!item.isExpanded());

                // Notifica o adapter que este item mudou (para redesenhar)
                notifyItemChanged(getAdapterPosition());
            });
        }
    }
}