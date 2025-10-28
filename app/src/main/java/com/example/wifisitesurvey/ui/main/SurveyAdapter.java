package com.example.wifisitesurvey.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.data.model.Survey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SurveyAdapter extends ListAdapter<Survey, SurveyAdapter.SurveyViewHolder> {

    private OnItemClickListener listener;
    private OnEditClickListener editClickListener;
    private OnDeleteClickListener deleteClickListener;

    public SurveyAdapter() {
        super(DIFF_CALLBACK);
    }

    // DiffUtil calcula a diferença entre duas listas e permite animar as mudanças
    private static final DiffUtil.ItemCallback<Survey> DIFF_CALLBACK = new DiffUtil.ItemCallback<Survey>() {
        @Override
        public boolean areItemsTheSame(@NonNull Survey oldItem, @NonNull Survey newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Survey oldItem, @NonNull Survey newItem) {
            return oldItem.name.equals(newItem.name) &&
                    oldItem.creationTimestamp == newItem.creationTimestamp;
        }
    };

    @NonNull
    @Override
    public SurveyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_survey, parent, false);
        return new SurveyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SurveyViewHolder holder, int position) {
        Survey currentSurvey = getItem(position);
        holder.textViewName.setText(currentSurvey.name);
        holder.textViewDate.setText(formatTimestamp(currentSurvey.creationTimestamp));
    }

    private String formatTimestamp(long timestamp) {
        // Formata o timestamp (milisegundos) para uma data e hora legível
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Holder para cada item da lista
    class SurveyViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewName;
        private final TextView textViewDate;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;

        public SurveyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.text_view_survey_name);
            textViewDate = itemView.findViewById(R.id.text_view_survey_date);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);

            // Configura o listener de clique para o item
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(position));
                }
            });

            buttonEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (editClickListener != null && position != RecyclerView.NO_POSITION) {
                    editClickListener.onEditClick(getItem(position));
                }
            });

            buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (deleteClickListener != null && position != RecyclerView.NO_POSITION) {
                    deleteClickListener.onDeleteClick(getItem(position));
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(Survey survey);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnEditClickListener {
        void onEditClick(Survey survey);
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.editClickListener = listener;
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Survey survey);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }
}