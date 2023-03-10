package chat.dim.game1248;

import android.content.res.AssetManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.view.View;

import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import chat.dim.g1248.Client;
import chat.dim.game1248.hall.RoomsFragment;
import chat.dim.io.Permissions;
import chat.dim.mkm.User;
import chat.dim.threading.BackgroundThreads;
import chat.dim.threading.MainThread;
import chat.dim.ui.TitledActivity;
import chat.dim.utils.Log;

public class MainActivity extends TitledActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private RoomsFragment roomsFragment = null;

    public MainActivity() {
        super();
        MainThread.prepare();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            roomsFragment = RoomsFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, roomsFragment)
                    .commitNow();
        }


        try {
            // load config file
            AssetManager am = getResources().getAssets();
            InputStreamReader isr = new InputStreamReader(am.open("config.ini"));
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            Client.prepare(sb.toString(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // launch client
        if (Permissions.canWriteExternalStorage(this)) {
            BackgroundThreads.rush(() -> {
                GameApp app = GameApp.getInstance();
                User user = app.prepareLocalUser();
                // show user ID
                String uid = user.getIdentifier().toString();
                MainThread.call(() -> {
                    TextView textView = findViewById(R.id.nav_user_id);
                    if (textView == null) {
                        // FIXME: nav_user_id not found?
                        Log.error("nav_user_id not found");
                        return;
                    }
                    textView.setText(uid);
                });
            });
        } else {
            Permissions.requestExternalStoragePermissions(this);
            Log.warning("Requesting external storage permissions");
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_tools) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
