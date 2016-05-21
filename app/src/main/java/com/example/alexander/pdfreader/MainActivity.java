package com.example.alexander.pdfreader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created by alexander on 28.01.16.
 */

public class MainActivity extends Activity {
    private final static String URL_PATH = "http://www.google.com";

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setContentView(R.layout.main_activity);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PagesActivity.class);
                startActivity(intent);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                CookieManager cookieManager = new CookieManager();
                CookieHandler.setDefault(cookieManager);
                try {
                    URL url = new URL(URL_PATH);
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.getContent();
                    CookieStore cookieStore = cookieManager.getCookieStore();
                    List<HttpCookie> cookieList = cookieStore.getCookies();
                    for (HttpCookie cookie : cookieList){
                        Log.i("TAG", cookie.getDomain());
                    }



                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();



    }
}
