package org.tmar.tmap.activity;

import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.alternativevision.gpx.beans.GPX;
import org.tmar.tmap.MapApplication;
import org.tmar.tmap.R;

import java.util.List;
import java.util.stream.Collectors;

public class DocumentActivity extends AppCompatActivity {
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_documents);

        // Populate listview content
        MapApplication app = (MapApplication) getApplication();
        ListView listView = findViewById(R.id.documentListView);

        List<GPX> documents = app.getOpenFiles();

        List<String> documentNames = documents.stream().map(gpx -> gpx.getCreator()).collect(Collectors.toList());

        String[] nameArray = documentNames.toArray(new String[0]);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, nameArray);
        listView.setAdapter(adapter);
    }
}
