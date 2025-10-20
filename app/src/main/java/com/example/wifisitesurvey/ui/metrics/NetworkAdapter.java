package com.example.wifisitesurvey.ui.metrics;

import android.content.Context;
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

public class NetworkAdapter extends RecyclerView.Adapter<NetworkAdapter.NetworkViewHolder> {

    private List<NetworkItem> networkList = new ArrayList<>();
    private final Context context;

    public NetworkAdapter(Context context) {
        this.context = context;
    }

    public void setNetworkList(List<NetworkItem> networkList) {
        this.networkList = networkList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NetworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_network, parent, false);
        return new NetworkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NetworkViewHolder holder, int position) {
        NetworkItem item = networkList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return networkList.size();
    }

    class NetworkViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSsid;
        private final TextView tvBssid;
        private final TextView tvDetails;
        private final TextView tvCollision;
        private final LinearLayout llDetails;
        private final ImageView ivIcon;
        private final ImageView ivExpandArrow;

        public NetworkViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSsid = itemView.findViewById(R.id.tvItemSsid);
            tvBssid = itemView.findViewById(R.id.tvItemBssid);
            tvDetails = itemView.findViewById(R.id.tvItemDetails);
            tvCollision = itemView.findViewById(R.id.tvItemCollision);
            llDetails = itemView.findViewById(R.id.llDetails);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            ivExpandArrow = itemView.findViewById(R.id.ivExpandArrow);
        }

        public void bind(NetworkItem item) {
            tvSsid.setText("SSID: " + item.getSsid());
            tvBssid.setText("BSSID: " + item.getBssid());
            tvDetails.setText(item.getDetails());

            // Ícone: 'ic_network_check' para rede atual, 'ic_wifi' para as demais
            if (item.isCurrentNetwork()) {
                ivIcon.setImageResource(R.drawable.ic_network_check);
            } else {
                ivIcon.setImageResource(R.drawable.ic_wifi);
            }

            // Visibilidade dos detalhes (colapso/expansão)
            llDetails.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);

            // Seta de expansão
            ivExpandArrow.setImageResource(item.isExpanded() ?
                    R.drawable.ic_keyboard_arrow_up :
                    R.drawable.ic_keyboard_arrow_down);

            // Visibilidade da Colisão (só para a rede atual E se estiver expandido)
            if (item.isCurrentNetwork() && item.getCollisionReport() != null && !item.getCollisionReport().isEmpty()) {
                tvCollision.setText(item.getCollisionReport());
                tvCollision.setVisibility(item.isExpanded() ? View.VISIBLE : View.GONE);
            } else {
                tvCollision.setVisibility(View.GONE);
            }

            // Ação de Clique: Inverte o estado "expandido" e notifica o adapter
            itemView.setOnClickListener(v -> {
                item.setExpanded(!item.isExpanded());
                notifyItemChanged(getAdapterPosition());
            });
        }
    }
}