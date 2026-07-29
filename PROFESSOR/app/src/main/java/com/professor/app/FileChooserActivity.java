package com.professor.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class FileChooserActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] acceptTypes = getIntent().getStringArrayExtra("ACCEPT_TYPES");
        if (acceptTypes != null && acceptTypes.length > 0) {
            intent.setType(acceptTypes[0]);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, acceptTypes);
        } else {
            intent.setType("*/*");
        }
        startActivityForResult(Intent.createChooser(intent, "اختر الملف أو الصورة"), FILE_CHOOSER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    result = new Uri[]{Uri.parse(dataString)};
                }
            }
            if (ProfessorBubbleService.filePathCallback != null) {
                ProfessorBubbleService.filePathCallback.onReceiveValue(result);
                ProfessorBubbleService.filePathCallback = null;
            }
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ProfessorBubbleService.filePathCallback != null) {
            ProfessorBubbleService.filePathCallback.onReceiveValue(null);
            ProfessorBubbleService.filePathCallback = null;
        }
    }
}