package org.tmar.tmap.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.alternativevision.gpx.beans.GPX;
import org.tmar.tmap.MapApplication;
import org.tmar.tmap.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DocumentActivity extends AppCompatActivity {
    private MapApplication mApp;

    private AdapterView.OnItemClickListener mClickListener = (parent, view, position, id) -> {
        showDocument(position);
    };
    private AdapterView.OnItemLongClickListener mLongClickListener = (parent, view, position, id) -> {
        showConfirmationDialog(position);
        return true;
    };
    private String[] mNameArray;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApp = (MapApplication) getApplication();

        setContentView(R.layout.activity_documents);

        ListView listView = findViewById(R.id.documentListView);
        listView.setOnItemClickListener(mClickListener);
        listView.setOnItemLongClickListener(mLongClickListener);

        try {
            updateList();
        } catch (IOException e) {
            mApp.handleException(e);
        }
    }

    private void updateList() throws IOException {
        // Populate listview content
        MapApplication app = (MapApplication) getApplication();
        List<GPX> documents = app.getOpenFiles();
        List<String> documentNames = new ArrayList<>();
        for (GPX gpx : documents){
            Uri  fileUri = Uri.parse(gpx.getCreator());
            String filename = app.getFilenameFromUri(fileUri);
            documentNames.add(filename);
        }
        mNameArray = documentNames.toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mNameArray);

        ListView listView = findViewById(R.id.documentListView);
        listView.setAdapter(adapter);
    }

    private void showDocument(int documentIndex) {
        MapApplication app = (MapApplication) getApplication();
        GPX gpx = app.getOpenFiles().get(documentIndex);
        Uri uri = Uri.parse(gpx.getCreator());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void showConfirmationDialog(int documentIndex) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.closeDocument)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    mApp.closeFile(documentIndex);

                    try {
                        updateList();
                    } catch (IOException e) {
                        mApp.handleException(e);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
