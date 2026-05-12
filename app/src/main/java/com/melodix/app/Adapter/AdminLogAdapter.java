package com.melodix.app.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.melodix.app.Model.AuditLog;
import com.melodix.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AdminLogAdapter extends RecyclerView.Adapter<AdminLogAdapter.LogViewHolder> {

    private List<AuditLog> logList;

    public AdminLogAdapter(List<AuditLog> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        AuditLog log = logList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTable.setText("bảng: " + log.getTableName());
        holder.tvRecordId.setText("Record ID: " + log.getRecordId());

        String action = log.getAction() != null ? log.getAction().toUpperCase() : "UNKNOWN";
        holder.tvAction.setText(action);

        if (action.equals("INSERT")) {
            holder.tvAction.setTextColor(Color.parseColor("#4CAF50")); // Xanh lá
        } else if (action.equals("UPDATE")) {
            holder.tvAction.setTextColor(Color.parseColor("#2196F3")); // Xanh dương
        } else if (action.equals("DELETE")) {
            holder.tvAction.setTextColor(Color.parseColor("#F44336")); // Đỏ
        }

        holder.tvTime.setText(formatDate(log.getChangedAt()));
        holder.tvDataDetail.setText("Nhấn để xem chi tiết dữ liệu thay đổi");
        holder.tvDataDetail.setTextColor(Color.parseColor("#03A9F4")); // Màu xanh click được
        holder.itemView.setOnClickListener(v -> {
            String details = parseDataChanges(log, action);
            new AlertDialog.Builder(context)
                    .setTitle("Chi tiết hành động: " + action)
                    .setMessage(details)
                    .setPositiveButton("Đóng", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }
    private String parseDataChanges(AuditLog log, String action) {
        StringBuilder sb = new StringBuilder();

        try {
            if (action.equals("UPDATE") && log.getOldData() != null && log.getNewData() != null) {
                JsonObject oldObj = log.getOldData().getAsJsonObject();
                JsonObject newObj = log.getNewData().getAsJsonObject();

                int changeCount = 0;

                for (String key : newObj.keySet()) {
                    String oldVal = getJsonValueAsString(oldObj.get(key));
                    String newVal = getJsonValueAsString(newObj.get(key));

                    if (!oldVal.equals(newVal) && !key.equals("updated_at") && !key.equals("fts")) {
                        sb.append("• [").append(key).append("]:\n")
                                .append("  Từ: ").append(oldVal).append("\n")
                                .append("  Thành: ").append(newVal).append("\n\n");
                        changeCount++;
                    }
                }

                if (changeCount == 0) {
                    sb.append("Chỉ có sự thay đổi ngầm (updated_at) hoặc không có thay đổi đáng kể.");
                }

            } else if (action.equals("INSERT") && log.getNewData() != null) {
                JsonObject newObj = log.getNewData().getAsJsonObject();
                sb.append("Dữ liệu được thêm mới:\n\n");
                for (String key : newObj.keySet()) {
                    if (!key.equals("fts")) { // Ẩn bớt mấy trường search không cần thiết
                        sb.append("• ").append(key).append(": ").append(getJsonValueAsString(newObj.get(key))).append("\n");
                    }
                }
            } else if (action.equals("DELETE") && log.getOldData() != null) {
                JsonObject oldObj = log.getOldData().getAsJsonObject();
                sb.append("Dữ liệu đã bị xóa:\n\n");
                for (String key : oldObj.keySet()) {
                    if (!key.equals("fts")) {
                        sb.append("• ").append(key).append(": ").append(getJsonValueAsString(oldObj.get(key))).append("\n");
                    }
                }
            } else {
                sb.append("Không có dữ liệu chi tiết.");
            }
        } catch (Exception e) {
            return "Lỗi khi đọc dữ liệu: " + e.getMessage();
        }

        return sb.toString().trim();
    }

    private String getJsonValueAsString(JsonElement element) {
        if (element == null || element.isJsonNull()) return "Trống (Null)";
        if (element.isJsonPrimitive()) return element.getAsString(); // Lấy chữ không có ngoặc kép
        return element.toString(); // Nếu là mảng hay obj thì lấy nguyên chuỗi
    }

    private String formatDate(String supabaseDate) {
        if (supabaseDate == null || supabaseDate.isEmpty()) return "";
        try {
            String mainPart = supabaseDate.length() >= 19 ? supabaseDate.substring(0, 19) : supabaseDate;
            mainPart = mainPart.replace("T", " ");

            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(mainPart);

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            outputFormat.setTimeZone(TimeZone.getDefault());
            return date != null ? outputFormat.format(date) : mainPart;
        } catch (Exception e) {
            return supabaseDate;
        }
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvAction, tvTable, tvTime, tvRecordId, tvDataDetail;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAction = itemView.findViewById(R.id.tvLogAction);
            tvTable = itemView.findViewById(R.id.tvLogTable);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            tvRecordId = itemView.findViewById(R.id.tvLogRecordId);
            tvDataDetail = itemView.findViewById(R.id.tvLogDataDetail);
        }
    }
}