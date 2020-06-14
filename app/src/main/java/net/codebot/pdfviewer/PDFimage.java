package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";
    Context context;

    // drawing path
    MainActivity.Tool tool = MainActivity.Tool.NONE;
    float orig_x, orig_y;
    float cur_x, cur_y;
    int desired_rect_area = 1000;            // Used when rects are too small/big to increase collision accuracy
    Path path = null;
    List<Path> paths = new ArrayList<>();
    List<Paint> path_paints = new ArrayList<>();
    List<ArrayList<Rect>> paths_rects = new ArrayList<ArrayList<Rect>>();

    // True if corresponding index in path is shown, false otherwise
    List<Boolean> hidden_paths = new ArrayList<>();

    // List of indices corresponding to paths to be modified in next undo/redo
    List<Integer> undo_indices = new ArrayList<>();
    List<Integer> redo_indices = new ArrayList<>();

    List<ArrayList<ArrayList<Integer>>> path_points = new ArrayList<>();       // Points used to create paths being made

    // image to display
    Bitmap bitmap;
    Paint paint = new Paint();

    // constructor
    public PDFimage(Context context) {
        super(context);
        this.context = context;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void selectNone() {
        tool = MainActivity.Tool.NONE;
    }

    public void selectPencil() {
        tool = MainActivity.Tool.PENCIL;
        paint.setColor(ContextCompat.getColor(context, R.color.darkBlue));
        paint.setStrokeWidth(10);
    }

    public void selectHighlighter() {
        tool = MainActivity.Tool.HIGHLIGHTER;
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(20);
        paint.setAlpha(100);
    }

    public void selectEraser() {
        tool = MainActivity.Tool.ERASER;
    }

    public void undo() {
        if (!undo_indices.isEmpty()) {
            int index = undo_indices.get(undo_indices.size()-1);
            hidden_paths.set(index, !hidden_paths.get(index));
            redo_indices.add(index);
            undo_indices.remove(undo_indices.size()-1);
        }
    }

    public void redo() {
        if (!redo_indices.isEmpty()) {
            int index = redo_indices.get(redo_indices.size()-1);
            hidden_paths.set(index, !hidden_paths.get(index));
            undo_indices.add(index);
            redo_indices.remove(redo_indices.size()-1);
        }
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                orig_x = event.getX();
                orig_y = event.getY();
                Log.d(LOGNAME, "Action down");
                if (tool == MainActivity.Tool.PENCIL || tool == MainActivity.Tool.HIGHLIGHTER) {
                    touch_down_draw(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(LOGNAME, "Action move");
                if (tool == MainActivity.Tool.PENCIL || tool == MainActivity.Tool.HIGHLIGHTER) {
                    touch_move_draw(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.d(LOGNAME, "Action up");
                if (tool == MainActivity.Tool.PENCIL || tool == MainActivity.Tool.HIGHLIGHTER) {
                    touch_up_draw(event.getX(), event.getY());
                }
                if (tool == MainActivity.Tool.ERASER) {
                    touch_up_erase(event.getX(), event.getY());
                }
                break;
        }
        return true;
    }

    void touch_down_draw(float event_x, float event_y) {
        path = new Path();
        paths.add(path);
        hidden_paths.add(true);

        // Update undo/redo index lists
        undo_indices.add(paths.size()-1);
        redo_indices.clear();

        // deep copy of paint
        Paint paint_copy = new Paint();
        paint_copy.setColor(paint.getColor());
        paint_copy.setStyle(paint.getStyle());
        paint_copy.setStrokeJoin(paint.getStrokeJoin());
        paint_copy.setStrokeCap(paint.getStrokeCap());
        paint_copy.setStrokeWidth(paint.getStrokeWidth());
        paint_copy.setAlpha(paint.getAlpha());
        path_paints.add(paint_copy);

        path.moveTo(event_x, event_y);
        cur_x = event_x;
        cur_y = event_y;

        // Add point to list
        ArrayList<ArrayList<Integer>> new_points = new ArrayList<>();
        path_points.add(new_points);
        addPoint();
    }

    void touch_move_draw(float event_x, float event_y) {
        // Use average of coordinates to draw smoother lines
        path.lineTo(event_x,event_y);
        cur_x = event_x;
        cur_y = event_y;

        // Add point to list
        addPoint();
    }

    void touch_up_draw(float event_x, float event_y) {
        path.lineTo(event_x,event_y);
        cur_x = event_x;
        cur_y = event_y;

        // Add point to list
        addPoint();
        addRects();
    }

    void addStringPaths(ArrayList<String> string_paths) {
        selectPencil();
        paths.clear();
        hidden_paths.clear();
        paths_rects.clear();
        path_paints.clear();
        path_points.clear();
        undo_indices.clear();
        redo_indices.clear();

        // Go through each path
        for (String string_path : string_paths) {
            String[] words = string_path.split(" ");

            path = new Path();
            paths.add(path);
            hidden_paths.add(true);

            // Update undo index lists
            undo_indices.add(paths.size()-1);

            ArrayList<ArrayList<Integer>> new_points = new ArrayList<>();
            path_points.add(new_points);

            // Determine tool used to make this path (pencil or highlighter) and set it
            int paint_int = Integer.parseInt(words[0]);
            if (paint_int == 0) {                // pencil case
                selectPencil();
            }
            else {                                      // highlighter case
                selectHighlighter();
            }

            // deep copy of paint
            Paint paint_copy = new Paint();
            paint_copy.setColor(paint.getColor());
            paint_copy.setStyle(paint.getStyle());
            paint_copy.setStrokeJoin(paint.getStrokeJoin());
            paint_copy.setStrokeCap(paint.getStrokeCap());
            paint_copy.setStrokeWidth(paint.getStrokeWidth());
            paint_copy.setAlpha(paint.getAlpha());
            path_paints.add(paint_copy);

            cur_x = Integer.parseInt(words[1]);
            cur_y = Integer.parseInt(words[2]);
            path.moveTo(Integer.parseInt(words[1]), Integer.parseInt(words[2]));
            addPoint();
            path.lineTo(Integer.parseInt(words[1]), Integer.parseInt(words[2]));        // in case path is a single point
            addPoint();
            // Go through each point in the path
            for (int j = 3; j < words.length; j++) {
                int x = Integer.parseInt(words[j]);
                int y = Integer.parseInt(words[j+1]);
                path.lineTo(x, y);
                cur_x = x;
                cur_y = y;
                addPoint();
                j ++;
            }
            addRects();
        }
    }

    void touch_up_erase(float event_x, float event_y) {
        if (event_x == orig_x && event_y == orig_y) {
            for (int i = paths_rects.size() - 1; i >= 0; i--) {
                if (hidden_paths.get(i)) {          // Can only delete a path that is visible
                    for (int j = 0; j < paths_rects.get(i).size(); j++) {
                        Rect rect = paths_rects.get(i).get(j);
                        if (rect.contains((int) event_x, (int) event_y)) {
                            hidden_paths.set(i, !hidden_paths.get(i));      // Guaranteed set to false due to previous if statement
                            undo_indices.add(i);
                            return;
                        }
                    }
                }
            }
        }
    }

    void addPoint() {
        if (path_points.get(paths.size()-1).size() > 0) {
            // add additional points in between if points are too spread apart
            // makes it so that additional rectangles are drawn, makes collision more accurate
            int x = path_points.get(paths.size()-1).get(path_points.get(paths.size()-1).size()-1).get(0);
            int y = path_points.get(paths.size()-1).get(path_points.get(paths.size()-1).size()-1).get(1);
            int width = (int) Math.abs(x-cur_x);
            int height = (int) Math.abs(y-cur_y);
            while (width*height > desired_rect_area) {
                ArrayList<Integer> point = new ArrayList();
                x = (int) (x+(cur_x-x)/10);
                y = (int) (y+(cur_y-y)/10);
                point.add(x);
                point.add(y);
                path_points.get(paths.size()-1).add(point);
                width = (int) Math.abs(x-cur_x);
                height = (int) Math.abs(y-cur_y);
            }
        }
        ArrayList<Integer> point = new ArrayList();
        point.add((int) cur_x);
        point.add((int) cur_y);
        path_points.get(paths.size()-1).add(point);
    }

    // Creates an array of rects made from the list of points that made up the path for collision detection
    void addRects() {
        ArrayList<Rect> current_path_rects = new ArrayList<>();
        for (int i = 0; i < path_points.get(paths.size()-1).size()-1; i ++) {
            int x1 = path_points.get(paths.size()-1).get(i).get(0);
            int y1 = path_points.get(paths.size()-1).get(i).get(1);
            int x2 = path_points.get(paths.size()-1).get(i+1).get(0);
            int y2 = path_points.get(paths.size()-1).get(i+1).get(1);
            Rect rect = new Rect();
            // Find correct x/y coords to set up for rect
            if (x1 <= x2) {
                if (y1 > y2) {
                    int temp_y1 = y1;
                    y1 = y2;
                    y2 = temp_y1;
                }
            }
            else {
                int temp_x1 = x1;
                x1 = x2;
                x2 = temp_x1;
                if (y1 > y2) {
                    int temp_y1 = y1;
                    y1 = y2;
                    y2 = temp_y1;
                }
            }
            // If rectangle is too small, collision will be hard
            // Make small rectangle into bigger rectangle
            ArrayList<Integer> adjusted_coords = adjustCoords(x1, y1, x2, y2);
            rect.set(adjusted_coords.get(0), adjusted_coords.get(1), adjusted_coords.get(2), adjusted_coords.get(3));
            current_path_rects.add(rect);
        }
        paths_rects.add(current_path_rects);
    }

    // Adjusts small rect to bigger rect to make collision easier, returns new coordiantes for rect
    ArrayList<Integer> adjustCoords(int x1, int y1, int x2, int y2) {
        ArrayList<Integer> adjusted_coords = new ArrayList<>();
        while ((x2-x1)*(y2-y1) < desired_rect_area) {
            x1 -= paint.getStrokeWidth()/2 + 2;
            y1 -= paint.getStrokeWidth()/2 + 2;
            x2 += paint.getStrokeWidth()/2 + 2;
            y2 += paint.getStrokeWidth()/2 + 2;
        }
        adjusted_coords.add(x1);
        adjusted_coords.add(y1);
        adjusted_coords.add(x2);
        adjusted_coords.add(y2);
        return adjusted_coords;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    // returns array of all paths on pdf represented by all points made to create them
    public ArrayList<String> pathsToPointsStrings() {
        // each item in array list is a path on the pdf
        ArrayList<String> pathStrings = new ArrayList<>();
        // For each path on the pdf
        for (int i = 0; i < path_points.size(); i ++) {
            if (hidden_paths.get(i)) {              // only saving visible paths
                String path_string = "";
                // Tool used to create path
                if (path_paints.get(i).getColor() == -16777077) {                // pencil case
                    path_string += "0 ";
                }
                else {                                                          // highlighter case
                    path_string += "1 ";
                }
                // For each point on the path
                for (int j = 0; j < path_points.get(i).size(); j++) {
                    path_string += path_points.get(i).get(j).get(0);
                    path_string += " ";
                    path_string += path_points.get(i).get(j).get(1);
                    if (j != path_points.get(i).size()-1) {
                        path_string += " ";
                    }
                }
                pathStrings.add(path_string);
            }
        }
        return pathStrings;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }
        // draw lines over it
        for (int i = 0; i < paths.size(); i ++) {
            if (hidden_paths.get(i)) {
                canvas.drawPath(paths.get(i), path_paints.get(i));
            }
        }

        // Draw rectangles for detecting collision for erasing
//        for (int i = paths_rects.size() - 1; i >= 0; i--) {
//            if (hidden_paths.get(i)) {
//                for (int j = 0; j < paths_rects.get(i).size(); j++) {
//                    Rect rect = paths_rects.get(i).get(j);
//                    canvas.drawRect(rect, path_paints.get(i));
//                }
//            }
//        }
        super.onDraw(canvas);
    }
}
