package org.tmar.tmap.activity;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.tmar.tmap.MapApplication;
import org.tmar.tmap.MapDescriptor;
import org.tmar.tmap.R;
import org.tmar.tmap.helpers.MapListViewAdapter;

import java.util.List;

public class MapsActivity extends AppCompatActivity implements MapListViewAdapter.OnClickListener {

    private MapApplication mApp;
    private MapDescriptor mCurrentMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        mApp = (MapApplication) getApplication();
    }

    @Override
    public void onResume() {
        super.onResume();

        List<MapDescriptor> maps = mApp.getMapOverlays();
        ListView listView = findViewById(R.id.mapsListView);
        listView.setAdapter(new MapListViewAdapter(this, maps, R.layout.map_delegate, this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.maplayer_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.mapDownload:
                return showDownloadMapActivity();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.maplayer_context_menu, menu);

        // Set menu items' visibilities
        List<MapDescriptor> maps = mApp.getMapOverlays();
        int position = (int) v.getTag();
        mCurrentMap = maps.get(position);

        menu.findItem(R.id.mapShow).setVisible(!mCurrentMap.isVisible());
        menu.findItem(R.id.mapHide).setVisible(mCurrentMap.isVisible());
        menu.findItem(R.id.mapShareStart).setVisible(!mCurrentMap.isShared());
        menu.findItem(R.id.mapShareEnd).setVisible(mCurrentMap.isShared());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mapShow:
                mCurrentMap.setVisible(true);
                break;
            case R.id.mapHide:
                mCurrentMap.setVisible(false);
                break;
            case R.id.mapShareStart:
                mCurrentMap.setShared(true);
                Toast.makeText(this, R.string.errorNotImplemented, Toast.LENGTH_LONG).show();
                break;
            case R.id.mapShareEnd:
                mCurrentMap.setShared(false);
                Toast.makeText(this, R.string.errorNotImplemented, Toast.LENGTH_LONG).show();
                break;
            default:
                return super.onContextItemSelected(item);
        }

        notifyDataSetChanged();
        return true;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }
    private boolean showDownloadMapActivity() {
        Toast.makeText(this, R.string.errorNotImplemented, Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public void onClick(int position) {
        // Toggle map visibility
        List<MapDescriptor> maps = mApp.getMapOverlays();
        MapDescriptor map = maps.get(position);
        map.setVisible(!map.isVisible());

        notifyDataSetChanged();

        mApp.savePrefs();
    }

    /*
        Update listview content
     */
    private void notifyDataSetChanged() {
        ListView listView = findViewById(R.id.mapsListView);
        BaseAdapter adapter = (BaseAdapter) listView.getAdapter();
        adapter.notifyDataSetChanged();
    }
}
