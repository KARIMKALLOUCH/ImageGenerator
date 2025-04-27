package com.example.imagegenerator;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.Pair;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class Test extends AppCompatActivity {
    TextView textView;
    NotesDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        dbHelper = new NotesDatabaseHelper(this);
        textView = findViewById(R.id.textview4);
        textView.setTextIsSelectable(true);
        applySavedNotes();

        textView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                menu.add(0, 1, 0, "Note");
                menu.add(0, 2, 1, "Color");
                menu.add(0, 3, 2, "Copy");
                int start = textView.getSelectionStart();
                int end = textView.getSelectionEnd();

                // نتحقق هل هذه الكلمة لديها لون
                String existingColor = dbHelper.getColorForPosition(start, end);
                if (existingColor != null) {
                    menu.add(0, 4, 3, "Remove Color"); // نضيف الخيار فقط للكلمات الملوّنة
                }
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                menu.removeItem(android.R.id.selectAll);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int start = textView.getSelectionStart();
                int end = textView.getSelectionEnd();

                if (start < 0 || end < 0 || start == end) {
                    mode.finish();
                    return false;
                }

                String selectedText = textView.getText().subSequence(start, end).toString().trim();

                if (selectedText.isEmpty()) {
                    mode.finish();
                    return false;
                }

                switch (item.getItemId()) {
                    case 1:
                        showNoteDialog(selectedText, start, end);
                        break;
                    case 2:
                        showColorPopup(selectedText, start, end);
                        break;
                    case 3:
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("Copied Text", selectedText));
                        Toast.makeText(getApplicationContext(), "Copied", Toast.LENGTH_SHORT).show();
                        break;
                    case 4:
                        dbHelper.removeOnlyColorForPosition(start, end);
                        applySavedNotes();
                        Toast.makeText(getApplicationContext(), "Color removed", Toast.LENGTH_SHORT).show();
                        break;
                }

                mode.finish();
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });
    }
    //
    private void applySavedNotes() {
        SpannableString spannable = new SpannableString(textView.getText());
        String fullText = textView.getText().toString();
        // 🧼 تنظيف كل الـ spans القديمة
        Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
        for (Object span : spans) {
            spannable.removeSpan(span);
        }

        // تطبيق الألوان
        for (Map.Entry<Pair<Integer, Integer>, String> entry : dbHelper.getAllColorsWithPositions().entrySet()) {
            Pair<Integer, Integer> position = entry.getKey();
            String colorName = entry.getValue();

            int color = getColorFromName(colorName);
            spannable.setSpan(new BackgroundColorSpan(color),
                    position.first, position.second,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // تطبيق الملاحظات (خط تحت + ClickableSpan)
        for (Map.Entry<Pair<Integer, Integer>, String> entry : dbHelper.getAllNotesWithPositions().entrySet()) {
            Pair<Integer, Integer> position = entry.getKey();

            spannable.setSpan(new UnderlineSpan(),
                    position.first, position.second,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    // نجلب الملاحظة الحقيقية وقت الضغط
                    String latestNote = dbHelper.getNoteForPosition(position.first, position.second);
                    String word = fullText.substring(position.first, position.second);
                    showNotePopup(word, latestNote);
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(true);
                }
            };

            spannable.setSpan(clickableSpan,
                    position.first, position.second,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView.setText(spannable);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private int getColorFromName(String colorName) {
        switch (colorName) {
            case "Blue":
                return Color.BLUE;
            case "Red":
                return Color.RED;
            case "Orange":
                return Color.parseColor("#FF9800");
            default:
                return Color.YELLOW;
        }
    }

    private void showNotePopup(String word, String note) {
        // 1. إنشاء AlertDialog مخصص
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. تضخيم واجهة الحوار المخصصة
        View dialogView = LayoutInflater.from(this).inflate(R.layout.note_popup_layout, null);

        // 3. الحصول على عناصر الواجهة
        TextView popupTitle = dialogView.findViewById(R.id.popupTitle);
        TextView noteContent = dialogView.findViewById(R.id.noteContent);
        Button okButton = dialogView.findViewById(R.id.okButton);
        ImageButton editButton = dialogView.findViewById(R.id.editButton);

        // 4. تعيين القيم
        popupTitle.setText("Note for: " + word);
        noteContent.setText(note);

        // 5. إعداد AlertDialog
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // 6. إعداد معالج حدث زر الموافقة
        okButton.setOnClickListener(v -> dialog.dismiss());

        // 7. إعداد معالج حدث زر التعديل
        editButton.setOnClickListener(v -> {
            dialog.dismiss();
            showEditNoteDialog(word, note);
        });

        ImageButton deleteButton = dialogView.findViewById(R.id.deleteButton);

        deleteButton.setOnClickListener(v -> {
            // تضخيم واجهة الحوار المخصصة
            View deleteDialogView = LayoutInflater.from(this).inflate(R.layout.custom_delete_dialog, null);

            // إنشاء AlertDialog مخصص
            AlertDialog.Builder deleteBuilder = new AlertDialog.Builder(this);
            deleteBuilder.setView(deleteDialogView);
            AlertDialog deleteDialog = deleteBuilder.create();

            // الحصول على عناصر الواجهة
            TextView cancelDelete = deleteDialogView.findViewById(R.id.cancelDelete);
            TextView confirmDelete = deleteDialogView.findViewById(R.id.confirmDelete);

            // إعداد معالج حدث زر الإلغاء
            cancelDelete.setOnClickListener(cancelView -> {
                deleteDialog.dismiss();
            });

            // إعداد معالج حدث زر الحذف
            confirmDelete.setOnClickListener(deleteView -> {
                dbHelper.deleteNoteForWord(word); // تأكد أن هذه الدالة موجودة في DBHelper
                applySavedNotes(); // لتحديث العرض
                dialog.dismiss(); // إغلاق حوار الملاحظة
                deleteDialog.dismiss(); // إغلاق حوار الحذف
                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
            });

            deleteDialog.show();
        });

        // 8. عرض الحوار
        dialog.show();
    }

    private void showNoteDialog(String word, int start, int end) {
        // 1. إنشاء AlertDialog مخصص
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. تضخيم واجهة الحوار المخصصة
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_note_dialog, null);

        // 3. الحصول على عناصر الواجهة
        EditText noteInput = dialogView.findViewById(R.id.noteText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // 4. تعطيل زر الحفظ بشكل افتراضي
        saveButton.setEnabled(true);

        // 5. إعداد AlertDialog
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // 6. إعداد TextWatcher لمراقبة تغيرات النص
        noteInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // تمكين/تعطيل زر الحفظ بناءً على وجود نص
                boolean hasText = !s.toString().trim().isEmpty();
                saveButton.setEnabled(hasText);

                // تغيير لون النص حسب حالة الزر (اختياري)
                int colorRes = hasText ? R.color.blue : R.color.gray;
                saveButton.setTextColor(ContextCompat.getColor(Test.this, colorRes));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 7. إعداد معالج حدث زر الإلغاء
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // 8. إعداد معالج حدث زر الحفظ
        saveButton.setOnClickListener(v -> {
            String note = noteInput.getText().toString().trim();

            if (note.isEmpty()) {
                // عرض رسالة خطأ إذا كان الحقل فارغًا
                Toast.makeText(this, "Please enter a note before saving", Toast.LENGTH_SHORT).show();

                // اهتزاز حقل الإدخال لتنبيه المستخدم (اختياري)
                Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
                noteInput.startAnimation(shake);

                return;
            }

            // حفظ الملاحظة إذا كانت تحتوي على نص
            dbHelper.saveNoteWithPosition(word, note, dbHelper.getColorForPosition(start, end), start, end);
            applySavedNotes();
            Toast.makeText(this, "Note Saved!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // 9. عرض الحوار
        dialog.show();
    }
    private void showEditNoteDialog(String word, String existingNote) {
        // 1. إنشاء AlertDialog مخصص
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. تضخيم واجهة الحوار المخصصة
        View dialogView = LayoutInflater.from(this).inflate(R.layout.edit_note_dialog, null);

        // 3. الحصول على عناصر الواجهة
        EditText noteInput = dialogView.findViewById(R.id.noteText);
        Button saveButton = dialogView.findViewById(R.id.saveButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        // 4. تعيين القيم الأولية
        noteInput.setText(existingNote);
        noteInput.setSelection(noteInput.getText().length()); // وضع المؤشر في نهاية النص

        // 5. إعداد AlertDialog
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // 6. إعداد TextWatcher لمراقبة تغيرات النص
        noteInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasChanges = !s.toString().equals(existingNote);
                saveButton.setEnabled(hasChanges);

                // تغيير لون النص حسب حالة الزر
                int colorRes = hasChanges ? R.color.blue : R.color.gray;
                saveButton.setTextColor(ContextCompat.getColor(Test.this, colorRes));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 7. إعداد معالج حدث زر الإلغاء
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // 8. إعداد معالج حدث زر الحفظ
        saveButton.setOnClickListener(v -> {
            String newNote = noteInput.getText().toString().trim();

            if (newNote.isEmpty()) {
                Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show();
                Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
                noteInput.startAnimation(shake);
                return;
            }

            int[] positions = dbHelper.getPositionsForWord(word);
            if (positions != null) {
                dbHelper.saveNoteWithPosition(word, newNote,
                        dbHelper.getColorForPosition(positions[0], positions[1]),
                        positions[0], positions[1]);
                applySavedNotes();
                Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // 9. عرض الحوار
        dialog.show();
    }
    private void showColorPopup(String word, int start, int end) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.color_popup_layout, null);

        View yellow = dialogView.findViewById(R.id.yellow);
        View blue = dialogView.findViewById(R.id.blue);
        View red = dialogView.findViewById(R.id.red);
        View orange = dialogView.findViewById(R.id.orange);

        AlertDialog dialog = builder.setView(dialogView).create(); // إنشاء AlertDialog

        View.OnClickListener listener = v -> {
            String colorName = "";
            if (v.getId() == R.id.yellow) colorName = "Yellow";
            else if (v.getId() == R.id.blue) colorName = "Blue";
            else if (v.getId() == R.id.red) colorName = "Red";
            else if (v.getId() == R.id.orange) colorName = "Orange";

            // حفظ اللون والملاحظة (إن وُجدت) في قاعدة البيانات
            dbHelper.saveNoteWithPosition(
                    word,
                    dbHelper.getNoteForPosition(start, end),
                    colorName,
                    start,
                    end
            );

            applySavedNotes(); // إعادة تطبيق الألوان والملاحظات
            dialog.dismiss(); // إغلاق نافذة الألوان
        };

        // تعيين المستمع للأزرار
        yellow.setOnClickListener(listener);
        blue.setOnClickListener(listener);
        red.setOnClickListener(listener);
        orange.setOnClickListener(listener);

        dialog.show(); // عرض النافذة
    }
}
