package net.codebot.pdfviewer;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provied them with this code.

public class MainActivity extends AppCompatActivity {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;

    // PDF variables
    LinearLayout layout;
    Tool tool = Tool.NONE;
    int current_page = 0;
    int total_pages;
    float page_scaling = 1;         // used for zooming in/out, min=0.5

    // Titlebar references
    ImageView titlebar_pencil_image_view;
    ImageView titlebar_highlighter_image_view;
    ImageView titlebar_eraser_image_view;
    ImageView titlebar_undo_image_view;
    ImageView titlebar_redo_image_view;


    // Statusbar references
    ImageView statusbar_prev_image_view;
    ImageView statusbar_next_image_view;
    ImageView statusbar_zoom_in_image_view;
    ImageView statusbar_zoom_out_image_view;
    TextView statusbar_page_text_view;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPDFPage;

    // custom ImageView class that captures strokes and draws them over the image
    List<PDFimage> pdf_images = new ArrayList<>();      // each pdf page will have its own pdf image
    PDFimage current_pdfImage;

    enum Tool {
        PENCIL, HIGHLIGHTER, ERASER, NONE;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);      // runs only in portrait mode
        setContentView(R.layout.activity_main);
        this.getSupportActionBar().hide();
        layout = findViewById(R.id.pdfLayout);
        layout.setEnabled(true);

        // Set up titlebar
        titlebar_pencil_image_view = (ImageView) findViewById(R.id.titlebar_pencil_image);
        titlebar_pencil_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToolImages();
                if (tool != Tool.PENCIL) {
                    selectPencilTool();
                }
                else {
                    tool = Tool.NONE;
                    current_pdfImage.selectNone();
                }
            }
        });
        titlebar_highlighter_image_view = (ImageView) findViewById(R.id.titlebar_highlighter_image);
        titlebar_highlighter_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToolImages();
                if (tool != Tool.HIGHLIGHTER) {
                    selectHighlighterTool();
                }
                else {
                    tool = Tool.NONE;
                    current_pdfImage.selectNone();
                }
            }
        });
        titlebar_eraser_image_view = (ImageView) findViewById(R.id.titlebar_eraser_image);
        titlebar_eraser_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToolImages();
                if (tool != Tool.ERASER) {
                    selectEraserTool();
                }
                else {
                    tool = Tool.NONE;
                    current_pdfImage.selectNone();
                }
            }
        });
        titlebar_undo_image_view = (ImageView) findViewById(R.id.titlebar_undo_image);
        titlebar_undo_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_pdfImage.undo();
            }
        });
        titlebar_redo_image_view = (ImageView) findViewById(R.id.titlebar_redo_image);
        titlebar_redo_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_pdfImage.redo();
            }
        });

        // Set up statusbar
        statusbar_prev_image_view = (ImageView) findViewById(R.id.statusbar_prev_image);
        statusbar_prev_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (current_page - 1 >= 0) {
                    current_page -= 1;
                    showPage(current_page);
                }
            }
        });
        statusbar_next_image_view = (ImageView) findViewById(R.id.statusbar_next_image);
        statusbar_next_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (current_page + 1 < total_pages) {
                    current_page += 1;
                    showPage(current_page);
                }
            }
        });
        statusbar_zoom_in_image_view = (ImageView) findViewById(R.id.statusbar_zoom_in_image);
        statusbar_zoom_in_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page_scaling < 3.0) {
                    page_scaling += 0.1;
                    showPage(current_page);
                }
            }
        });
        statusbar_zoom_out_image_view = (ImageView) findViewById(R.id.statusbar_zoom_out_image);
        statusbar_zoom_out_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page_scaling > 0.5) {
                    page_scaling -= 0.1;
                    showPage(current_page);
                }
            }
        });
        statusbar_page_text_view = (TextView) findViewById(R.id.statusbar_page_text);

        // open page of the PDF, it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            showPage(current_page);
//            closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }

        // Read any old state data from file
        readFromFileInput();
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();

        writeToFileOutput();
//        try {
//            closeRenderer();
//        } catch (IOException ex) {
//            Log.d(LOGNAME, "Unable to close PDF renderer");
//        }
    }

    void writeToFileOutput() {
        try {
            FileOutputStream fileOutputStream = openFileOutput("save_state.txt", MODE_PRIVATE);

            fileOutputStream.write(Integer.toString(current_page).getBytes());          // Write current tool
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write(Integer.toString(toolToPos()).getBytes());           // Write current page
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.write(Integer.toString(total_pages).getBytes());           // Write number of pages in PDF
            fileOutputStream.write("\n".getBytes());

            // Go through each PDF page
            for (int i = 0; i < total_pages; i ++) {
                ArrayList<String> pathStrings = pdf_images.get(i).pathsToPointsStrings();

                // Write number of paths in PDF page
                fileOutputStream.write(Integer.toString(pathStrings.size()).getBytes());
                fileOutputStream.write("\n".getBytes());

                // Go through each path in PDF page
                for (int j = 0; j < pathStrings.size(); j ++) {
                    // Write each path as string of points
                    fileOutputStream.write(pathStrings.get(j).getBytes());
                    fileOutputStream.write("\n".getBytes());
                }
            }
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void readFromFileInput() {
        try {
            FileInputStream fileInputStream = openFileInput("save_state.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            ArrayList<String> file_lines = new ArrayList<>();
            String line = bufferedReader.readLine();
            while (line != null) {
                file_lines.add(line);
                line = bufferedReader.readLine();
            }
            if (file_lines.size() >= 3) {
                current_page = Integer.parseInt(file_lines.get(0));
                int total_pages_read = Integer.parseInt(file_lines.get(2));
                int line_num = 3;

                // Read paths for each PDF page
                for (int i = 0; i < total_pages_read; i ++) {
                    if (line_num >= file_lines.size()) {            // temp fix to bug where trailing 0's disappear
                        break;
                    }
                    // Read number of paths for each PDF page
                    int num_paths = Integer.parseInt(file_lines.get(line_num));
                    line_num += 1;
                    ArrayList<String> pathStrings = new ArrayList<>();      // Will hold array of strings representing paths

                    // Read paths in PDF page
                    for (int j = 0; j < num_paths; j ++) {
                        // Read each path as string of points
                        String path = file_lines.get(line_num);
                        pathStrings.add(path);
                        line_num += 1;
                    }
                    // Add page i's paths to pdf page i
                    pdf_images.get(i).addStringPaths(pathStrings);
                }
                setTool(Integer.parseInt(file_lines.get(1)));
                showPage(current_page);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Pressing the back button does not kill the app (redo stack should only be destroyed when app is deliberately killed)
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            total_pages = pdfRenderer.getPageCount();
            createPDFImages();
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPDFPage) {
            currentPDFPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        layout.removeView(current_pdfImage);
        updatePDFImage();
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Close the current page before opening another one.
        if (null != currentPDFPage) {
            currentPDFPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPDFPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPDFPage.getWidth(), currentPDFPage.getHeight(), Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix();
        matrix.setScale(page_scaling, page_scaling);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPDFPage.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        current_pdfImage.setImage(bitmap);
        layout.addView(current_pdfImage);
        updatePageNumber();
    }

    // Called when tool needs to be set to PENCIL
    void selectPencilTool() {
        tool = Tool.PENCIL;
        current_pdfImage.selectPencil();
        titlebar_pencil_image_view.setBackgroundResource(R.mipmap.pencil_clicked);
    }

    // Called when tool needs to be set to HIGHLIGHTER
    void selectHighlighterTool() {
        tool = Tool.HIGHLIGHTER;
        current_pdfImage.selectHighlighter();
        titlebar_highlighter_image_view.setBackgroundResource(R.mipmap.highlighter_clicked);
    }

    // Called when tool needs to be set to ERASER
    void selectEraserTool() {
        tool = Tool.ERASER;
        current_pdfImage.selectEraser();
        titlebar_eraser_image_view.setBackgroundResource(R.mipmap.eraser_clicked);
    }

    // Called when page displayed is changed, updates page number text in status bar
    void updatePageNumber() {
        int actual_page = current_page+1;
        String page_string = "Page " + actual_page + " of " + total_pages;
        statusbar_page_text_view.setText(page_string);
    }

    // Creates a PDFImage for each page in the pdf
    void createPDFImages() {
        for (int i = 0; i < total_pages; i ++) {
            PDFimage pdfImage = new PDFimage(this);
            pdfImage.setMinimumWidth(1000);
            pdfImage.setMinimumHeight(2000);
            pdf_images.add(pdfImage);
        }
    }

    // Updates the PDF image tool to be the current tool selected
    void updatePDFImage() {
        current_pdfImage = pdf_images.get(current_page);
        switch (tool) {
            case PENCIL:
                current_pdfImage.selectPencil();
                break;
            case HIGHLIGHTER:
                current_pdfImage.selectHighlighter();
                break;
            case ERASER:
                current_pdfImage.selectEraser();
                break;
            case NONE:
                current_pdfImage.selectNone();
                break;
        }
    }

    // Resets tool images to be non-selected, called when a tool is selected or deselected
    void resetToolImages() {
        titlebar_pencil_image_view.setBackgroundResource(R.mipmap.pencil);
        titlebar_highlighter_image_view.setBackgroundResource(R.mipmap.highlighter);
        titlebar_eraser_image_view.setBackgroundResource(R.mipmap.eraser);
    }

    // Returns current tool index in enum
    int toolToPos() {
        int pos = 3;
        switch(tool) {
            case PENCIL:
                pos = 0;
                break;
            case HIGHLIGHTER:
                pos = 1;
                break;
            case ERASER:
                pos = 2;
                break;
        }
        return pos;
    }

    void setTool(int pos) {
        switch(pos) {
            case 0:
                selectPencilTool();
                break;
            case 1:
                selectHighlighterTool();
                break;
            case 2:
                selectEraserTool();
                break;
        }
    }
}
