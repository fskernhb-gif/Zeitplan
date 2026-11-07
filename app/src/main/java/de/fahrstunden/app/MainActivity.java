package de.fahrstunden.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int VOICE_REQ = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void startVoice() {
            runOnUiThread(() -> {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE");
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Person und Zeiten sprechen");
                try {
                    startActivityForResult(i, VOICE_REQ);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Spracherkennung ist auf diesem Gerät nicht verfügbar.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface
        public void exportPdf() {
            runOnUiThread(() -> createPdf());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQ && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String text = results.get(0).replace("\\", "\\\\")
                        .replace("'", "\\'");
                webView.evaluateJavascript(
                        "window.receiveVoiceText('" + text + "')", null);
            }
        }
    }

    private void createPdf() {
        try {
            PdfDocument pdf = new PdfDocument();
            int w = 1080;
            int h = 1920;
            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo.Builder(w, h, 1).create();
            PdfDocument.Page page = pdf.startPage(info);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(Color.WHITE);

            webView.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.AT_MOST));
            webView.layout(0, 0, w, Math.min(webView.getMeasuredHeight(), h));
            webView.draw(canvas);

            pdf.finishPage(page);

            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = getFilesDir();
            File out = new File(dir, "Fahrstunden-Uebersicht.pdf");
            FileOutputStream fos = new FileOutputStream(out);
            pdf.writeTo(fos);
            fos.close();
            pdf.close();

            Toast.makeText(this,
                    "PDF gespeichert: " + out.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "PDF konnte nicht erstellt werden.", Toast.LENGTH_LONG).show();
        }
    }
}
