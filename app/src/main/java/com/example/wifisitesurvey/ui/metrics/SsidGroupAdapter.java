package com.example.wifisitesurvey.ui.metrics;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.ui.bssidDetail.BssidDetailActivity;

import java.util.ArrayList;
import java.util.List;

// Renomeado de NetworkAdapter para SsidGroupAdapter
public class SsidGroupAdapter extends RecyclerView.Adapter<SsidGroupAdapter.SsidGroupViewHolder> {

    private List<SsidGroupItem> groupList = new ArrayList<>();
    private final LayoutInflater inflater;

    public SsidGroupAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setSsidGroups(List<SsidGroupItem> groupList) {
        this.groupList = groupList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SsidGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usar o novo layout do grupo
        View view = inflater.inflate(R.layout.list_item_ssid_group, parent, false);
        return new SsidGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SsidGroupViewHolder holder, int position) {
        SsidGroupItem item = groupList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    class SsidGroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSsid;
        private final TextView tvBssidCount;
        private final ImageView ivIcon;
        private final ImageView ivExpandArrow;

        public SsidGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSsid = itemView.findViewById(R.id.tvItemSsid);
            tvBssidCount = itemView.findViewById(R.id.tvItemBssidCount);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            ivExpandArrow = itemView.findViewById(R.id.ivExpandArrow);
        }

        public void bind(SsidGroupItem item) {
            // 1. Configurar o Header
            tvSsid.setText("SSID: " + item.getSsidName());

            int count = item.getBssidCount();
            tvBssidCount.setText(count + (count > 1 ? " Pontos de Acesso" : " Ponto de Acesso"));

            ivIcon.setImageResource(item.isCurrentNetwork() ?
                    R.drawable.ic_network_check :
                    R.drawable.ic_wifi);

            // 2. Lógica de Expansão REMOVIDA
            // A seta agora é fixa (ou pode ser removida do layout)
            ivExpandArrow.setImageResource(R.drawable.ic_keyboard_arrow_down); // Ou mude para ic_keyboard_arrow_right

            // 4. NOVA Ação de Clique: Abrir a BssidDetailActivity
            itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, BssidDetailActivity.class);

                // Passa o objeto inteiro para a próxima tela
                intent.putExtra(BssidDetailActivity.EXTRA_SSID_GROUP, item);

                context.startActivity(intent);
            });
        }
    }
}