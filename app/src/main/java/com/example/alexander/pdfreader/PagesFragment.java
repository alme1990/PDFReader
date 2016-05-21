package com.example.alexander.pdfreader;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by alexander on 17.01.16.
 */

public class PagesFragment extends Fragment {

    private ParcelFileDescriptor mFileDescriptor;
    private PdfRenderer mPdfRenderer;
    private PdfRenderer.Page mCurrentPage;
    private ListView listView;
    private ListPDF adapter;
    private ProgressBar progressBar;
    private TextView textView;
    private Parcelable stateListView;
    private boolean showButton;
    private int widthDisplay;
    private int heightDisplay;
    private int scale;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        new DownloadFile().execute("https://www.apple.com/legal/sla/docs/iphoneos31.pdf", "iphoneos31.pdf");
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                if (NavUtils.getParentActivityName(getActivity()) != null)
                    NavUtils.navigateUpFromSameTask(getActivity());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle bundle){
        View v = inflater.inflate(R.layout.list, parent, false);
        listView = (ListView) v.findViewById(R.id.list_view);
        progressBar = (ProgressBar) v.findViewById(R.id.progress_bar);
        textView = (TextView) v.findViewById(R.id.text_view);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        widthDisplay = sizeDisplay().x;
        heightDisplay = sizeDisplay().y;
        if (stateListView != null){
            updateListView(widthDisplay, heightDisplay);
            progressBar.setVisibility(View.INVISIBLE);
        }

        showButton = false;
        scale = 10;
        int sizeButton = 100*Math.min(widthDisplay, heightDisplay) / 1920;
        final Button buttonPlus = (Button) v.findViewById(R.id.button_plus);
        changeSize(buttonPlus, sizeButton, sizeButton);
        buttonPlus.setAlpha(0);
        buttonPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stateListView = listView.onSaveInstanceState();
                scale += 1;
                if (scale > 20)
                    scale = 20;
                updateListView(widthDisplay * scale / 10, heightDisplay * scale / 10);
            }
        });
        final Button buttonMinus = (Button) v.findViewById(R.id.button_minus);
        changeSize(buttonMinus, sizeButton, sizeButton);
        buttonMinus.setAlpha(0);
        buttonMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stateListView = listView.onSaveInstanceState();
                scale -= 1;
                if (scale < 10)
                    scale = 10;
                updateListView(widthDisplay * scale / 10, heightDisplay * scale / 10);
            }
        });
        // Слушатель для создание анимации кнопок отдаления/приближения
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && showButton == false) {
                    ValueAnimator alphaShow = ValueAnimator.ofFloat(0, 1);
                    alphaShow.setDuration(1000);
                    alphaShow.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            buttonMinus.setAlpha((Float) animation.getAnimatedValue());
                            buttonPlus.setAlpha((Float) animation.getAnimatedValue());
                        }
                    });
                    ValueAnimator transShow = ValueAnimator.ofFloat(widthDisplay, 0);
                    transShow.setDuration(1000);
                    transShow.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            buttonMinus.setTranslationX((Float) animation.getAnimatedValue());
                            buttonPlus.setTranslationX((Float) animation.getAnimatedValue());
                        }
                    });
                    ValueAnimator transHide = ValueAnimator.ofFloat(0, widthDisplay);
                    transHide.setDuration(200);
                    transHide.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            animation.setStartDelay(7000);
                            buttonMinus.setTranslationX((Float) animation.getAnimatedValue());
                            buttonPlus.setTranslationX((Float) animation.getAnimatedValue());
                        }
                    });
                    AnimatorSet set = new AnimatorSet();
                    set.play(alphaShow).with(transShow).before(transHide);
                    set.start();
                    set.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            showButton = false;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                    showButton = true;

                }
                return false;
            }
        });
        return v;
    }
    // Восстановление предыдущего состояния pdf-документа при изменении ориентации устройства
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void updateListView(int width, int height){
        adapter = new ListPDF(mPdfRenderer.getPageCount(), width, height);
        listView.setAdapter(adapter);
        listView.onRestoreInstanceState(stateListView);
    }
    // Определение размеров экрана устройства
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private Point sizeDisplay(){
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    @Override
    public void onPause(){
        super.onPause();
        stateListView = listView.onSaveInstanceState(); //Запись текущего состояния pdf-документа
    }
    // Изменение размеров любой View-шки
    private void changeSize(View view, int width, int height){
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }
    // Реализация кастомного адаптера
    private class ListPDF extends BaseAdapter{
        private LayoutInflater inflater = null;
        private int count;
        private int width;
        private int height;

        public ListPDF(int count, int width, int height){
            inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.count = count;
            this.width = width;
            this.height = height;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (convertView == null)
                view = inflater.inflate(R.layout.list_fragment, null);
            ImageView imageView = (ImageView) view.findViewById(R.id.image_view);
            showPage(position, imageView);
            int orientation = getActivity().getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                changeSize(imageView, width, height);
            if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                changeSize(imageView, height*16/10, width*16/10);
            return view;
        }
    }
    // Фоновый процесс для загрузки файла с интернета
    private class DownloadFile extends AsyncTask<String, Integer, Void> {
        @Override
        protected Void doInBackground(String... strings) {
            String fileUrl = strings[0];
            String fileName = strings[1];
            File folder = new File(Environment.getExternalStorageDirectory(), "pdf_test");
            folder.mkdirs();
            File pdfFile = new File(folder, fileName);
            if (!pdfFile.exists()){
                try {
                    pdfFile.createNewFile();
                    try {
                        URL url = new URL(fileUrl);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.setDoOutput(true);
                        try {
                            urlConnection.connect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        InputStream in = urlConnection.getInputStream();
                        FileOutputStream out = new FileOutputStream(pdfFile);

                        byte[] buffer = new byte[1024*1024];
                        int bufferLength = 0;
                        int currentLoad = 0;
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        while((bufferLength = in.read(buffer))>0 ){
                            out.write(buffer, 0, bufferLength);
                            currentLoad += bufferLength;
                            publishProgress(currentLoad*100/urlConnection.getContentLength());
                        }
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                        out.close();
                        in.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... params){
            progressBar.setProgress(params[0]);
            textView.setText(getResources().getString(R.string.state_download)+": "+params[0]+"%");
        }

        @Override
        protected void onPostExecute(Void param){
            progressBar.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.INVISIBLE);
            File pdfFile = new File(Environment.getExternalStorageDirectory()+"/pdf_test/iphoneos31.pdf");
            openRenderer(pdfFile);
            Toast.makeText(getActivity(), getResources().getString(R.string.download_finished), Toast.LENGTH_SHORT).show();
        }
    }

    private void openRenderer(File file) {
        try {
            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mPdfRenderer = new PdfRenderer(mFileDescriptor);
                adapter = new ListPDF(mPdfRenderer.getPageCount(), widthDisplay, heightDisplay);
                listView.setAdapter(adapter);
            }
        } catch (FileNotFoundException e) {
            Toast.makeText(getActivity(), getResources().getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Преобразование  pdf-страниц в bitmap
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index, ImageView pdfView) {
        if (mPdfRenderer == null || mPdfRenderer.getPageCount() <= index || index < 0)
            return;
        if (mCurrentPage != null)
            mCurrentPage.close();
        mCurrentPage = mPdfRenderer.openPage(index);
        int quality = 2; //уровень качества pdf-страниц
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth()*quality, mCurrentPage.getHeight()*quality, Bitmap.Config.ARGB_8888);
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        pdfView.setImageBitmap(bitmap);
    }
}
