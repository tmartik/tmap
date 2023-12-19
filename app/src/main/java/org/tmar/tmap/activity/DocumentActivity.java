package org.tmar.tmap.activity;

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

import java.util.List;
import java.util.stream.Collectors;

public class DocumentActivity extends AppCompatActivity {
    private AdapterView.OnItemLongClickListener mLongClickListener = (parent, view, position, id) -> {
        showConfirmationDialog(position);
        return true;
    };
    private String[] mNameArray;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_documents);

        ListView listView = findViewById(R.id.documentListView);
        listView.setOnItemLongClickListener(mLongClickListener);

        updateList();
    }

    private void updateList() {
        // Populate listview content
        MapApplication app = (MapApplication) getApplication();
        List<GPX> documents = app.getOpenFiles();
        List<String> documentNames = documents.stream().map(gpx -> gpx.getCreator()).collect(Collectors.toList());
        mNameArray = documentNames.toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mNameArray);

        ListView listView = findViewById(R.id.documentListView);
        listView.setAdapter(adapter);
    }

    private void showConfirmationDialog(int documentIndex) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.closeDocument)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    MapApplication app = (MapApplication) getApplication();
                    app.closeFile(documentIndex);

                    updateList();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
}
