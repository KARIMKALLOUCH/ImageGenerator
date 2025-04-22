package com.example.imagegenerator;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    String selectedWord = "";
    Map<String, String> notesMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView3);
        textView.setTextIsSelectable(true);
        makeWordsClickable(); // مهمّة باش الكلمات تولي تتضغط

        textView.setOnLongClickListener(v -> {
            int start = textView.getSelectionStart();
            int end = textView.getSelectionEnd();

            if (start < end) {
                selectedWord = textView.getText().subSequence(start, end).toString().trim();
                showPopupMenu(v);
            }
            return true;
        });
    }

    private void showPopupMenu(View anchorView) {
        PopupMenu popup = new PopupMenu(this, anchorView);
        popup.getMenu().add("✅ نسخ الكلمة");
        popup.getMenu().add("🎨 تغيير لون الكلمة");
        popup.getMenu().add("📝 إضافة ملاحظة");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("نسخ")) {
                copyToClipboard(selectedWord);
            } else if (title.contains("لون")) {
                highlightWord(selectedWord);
            } else if (title.contains("ملاحظة")) {
                showAddNoteDialog(selectedWord);
            }
            return true;
        });

        popup.show();
    }

    private void copyToClipboard(String word) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("word", word));
        Toast.makeText(this, "✅ تم النسخ!", Toast.LENGTH_SHORT).show();
    }

    private void highlightWord(String word) {
        final String[] colors = {"🔴 أحمر", "🟢 أخضر", "🟡 أصفر", "🔵 أزرق", "🟣 بنفسجي", "⚫ أسود"};
        final int[] colorValues = {
                Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.MAGENTA, Color.BLACK
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎨 اختر لون الكلمة:");

        builder.setItems(colors, (dialog, which) -> {
            int selectedColor = colorValues[which];

            // نجيب الـ Spannable الحالي مباشرة
            CharSequence originalText = textView.getText();
            Spannable spannable;

            if (originalText instanceof Spannable) {
                spannable = (Spannable) originalText;
            } else {
                spannable = new SpannableString(originalText);
            }

            // نحذف اللون السابق للكلمة (اختياري باش ميوقعش تراكب ديال الألوان)
            ForegroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) {
                int start = spannable.getSpanStart(span);
                int end = spannable.getSpanEnd(span);
                if (spannable.subSequence(start, end).toString().equals(word)) {
                    spannable.removeSpan(span);
                }
            }

            // استعمل Pattern باش نلقاو الكلمة بدقة
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(word) + "\\b");
            java.util.regex.Matcher matcher = pattern.matcher(spannable.toString());

            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                spannable.setSpan(new ForegroundColorSpan(selectedColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            textView.setText(spannable);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            makeWordsClickable(); // رجّع ClickableSpan
        });

        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }


    private void showAddNoteDialog(String word) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📝 أضف ملاحظة لـ: " + word);

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("حفظ", (dialog, which) -> {
            String note = input.getText().toString();
            notesMap.put(word, note);
            Toast.makeText(this, "✅ تم حفظ الملاحظة!", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showNote(String word) {
        String note = notesMap.get(word);
        if (note != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("📌 ملاحظة");
            builder.setMessage("🔹 " + word + ":\n" + note);
            builder.setPositiveButton("حسناً", null);
            builder.show();
        }
    }

    private void makeWordsClickable() {
        String text = textView.getText().toString();
        SpannableString spannable = new SpannableString(text);
        String[] words = text.split(" ");
        int start = 0;

        for (String word : words) {
            int end = start + word.length();
            final String currentWord = word.replaceAll("[^\\p{L}\\p{N}]", ""); // حذف الرموز

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (notesMap.containsKey(currentWord)) {
                        showNote(currentWord);
                    }
                }
            };
            spannable.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = end + 1;
        }

        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
