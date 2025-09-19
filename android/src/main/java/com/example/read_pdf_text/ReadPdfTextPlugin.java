package com.example.read_pdf_text;

import androidx.annotation.NonNull;
import android.os.AsyncTask;
import java.util.ArrayList;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import android.util.Log;

/** ReadPdfTextPlugin */
public class ReadPdfTextPlugin implements FlutterPlugin, MethodCallHandler {
  private MethodChannel channel;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "read_pdf_text");
    channel.setMethodCallHandler(this);
    PDFBoxResourceLoader.init(flutterPluginBinding.getApplicationContext());
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (channel != null) {
        channel.setMethodCallHandler(null);
        channel = null;
    }
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPDFtext")) {
        final String path = call.argument("path");
        parsePDFtext(path, result);
    } 
    else if(call.method.equals("getPDFtextPaginated")) {
        final String path = call.argument("path");
        paginatePDFtext(path, result);
    }
    else if(call.method.equals("getPDFlength")) {
        final String path = call.argument("path");
        getPDFlength(path, result);
    }
    else {
      result.notImplemented();
    }   
  }

  // Creates a [AsyncTask] to parse the text from the pdf file.
  private void parsePDFtext(String path, Result result) {
    PdfAsyncTask task = new PdfAsyncTask(result);
    task.execute(path);
  }
  
  private void paginatePDFtext(String path, Result result) {
    PaginatePDFAsyncTask task = new PaginatePDFAsyncTask(result);
    task.execute(path);
  }
  
  // Gets the number of pages in PDF document
  private void getPDFlength(String path, Result result) {
    PDDocument document = null;
    try {
        File renderFile = new File(path);
        document = PDDocument.load(renderFile);
        if (document != null) {
            result.success(document.getNumberOfPages());
        } else {
            result.error("GENERIC_ERROR", "Failed to load document", null);
        }
    } catch(IOException e) {
        Log.e("Flutter-Read-Pdf-Plugin", "Exception thrown while loading document", e);
        result.error("IO_ERROR", "Failed to load document: " + e.getMessage(), null);
    } finally {
        try {
            if (document != null) document.close();
        } catch (IOException e) {
            Log.e("Flutter-Read-Pdf-Plugin", "Exception thrown while closing document", e);
        }
    }
  }

  // This [AsyncTask] runs on another Thread
  private class PdfAsyncTask extends AsyncTask<String, Void, String> {
    private final Result result;

    public PdfAsyncTask(Result result) {
      this.result = result;
    }

    @Override
    protected String doInBackground(String... strings) {
        String parsedText = null;
        PDDocument document = null;
        try {
            final String path = strings[0];
            File renderFile = new File(path);
            document = PDDocument.load(renderFile);
            
            if (document != null) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                parsedText = pdfStripper.getText(document);
            }
        } catch(IOException e) {
            Log.e("Flutter-Read-Pdf-Plugin", "Exception thrown while loading document", e);
        } finally {
            try {
                if (document != null) document.close();
            } catch (IOException e) {
                Log.e("Flutter-Read-Pdf-Plugin", "Exception thrown while closing document", e);
            }
        }
        return parsedText;
    }

    @Override
    protected void onPostExecute(String parsedText) {
        if (parsedText != null) {
            result.success(parsedText);
        } else {
            result.error("GENERIC_ERROR", "Something went wrong while parsing!", null);
        }
    }
  }

  // This [AsyncTask] runs on another Thread
  private class PaginatePDFAsyncTask extends AsyncTask<String, Void, ArrayList<String>> {
    private final Result result;

    public PaginatePDFAsyncTask(Result result) {
      this.result = result;
    }

    @Override
    protected ArrayList<String> doInBackground(String... strings) {
        ArrayList<String> paginatedText = new ArrayList<String>();
        PDDocument document = null;
        try {
            final String path = strings[0];
            File renderFile = new File(path);
            document = PDDocument.load(renderFile);
            
            if (document != null) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                int documentLength = document.getNumberOfPages();

                for(int i = 1; i <= documentLength; i++) {
                    pdfStripper.setStartPage(i);
                    pdfStripper.setEndPage(i);
                    paginatedText.add(pdfStripper.getText(document));
                }
            }
        } catch(IOException e) {
            Log.e("Flutter-Read-Pdf-Plugin", "Exception thrown while loading document", e);
        } finally {
            try {
                if (document != null) document.close();
            } catch (IOException e) {
                Log.e("Flutter-Read-Pdf-Plugin", "Exception thrown while closing document", e);
            }
        }
        return paginatedText;
    }

    @Override
    protected void onPostExecute(ArrayList<String> paginatedText) {
        if(paginatedText != null && !paginatedText.isEmpty()) {
            result.success(paginatedText);
        } else {
            result.error("GENERIC_ERROR", "Something went wrong while parsing!", null);
        }
    }
  }
}
