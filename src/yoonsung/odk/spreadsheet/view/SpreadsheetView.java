package yoonsung.odk.spreadsheet.view;

import yoonsung.odk.spreadsheet.R;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs;
import yoonsung.odk.spreadsheet.DataStructure.DisplayPrefs.ColumnColorRuler;
import yoonsung.odk.spreadsheet.data.TableViewSettings;
import yoonsung.odk.spreadsheet.data.UserTable;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;


public class SpreadsheetView extends LinearLayout {
    
    enum TableType { MAIN_DATA, MAIN_HEADER, MAIN_FOOTER,
        INDEX_DATA, INDEX_HEADER, INDEX_FOOTER }
    
    private static final int MIN_CLICK_DURATION = 0;
    private static final int MIN_LONG_CLICK_DURATION = 1000;
    
    private final Context context;
    private final Controller controller;
    private final TableViewSettings tvs;
    private final UserTable table;
    private final int indexedCol;
    private final DisplayPrefs dp;
    
    private ScrollView indexScroll;
    private ScrollView mainScroll;
    private TabularView indexData;
    private TabularView indexHeader;
    private TabularView indexFooter;
    private TabularView mainData;
    private TabularView mainHeader;
    private TabularView mainFooter;
    
    private View.OnTouchListener mainDataCellClickListener;
    private View.OnTouchListener mainHeaderCellClickListener;
    private View.OnTouchListener mainFooterCellClickListener;
    private View.OnTouchListener indexDataCellClickListener;
    private View.OnTouchListener indexHeaderCellClickListener;
    private View.OnTouchListener indexFooterCellClickListener;
    
    private int lastLongClickedCellId;
    
    public SpreadsheetView(Context context, Controller controller,
            TableViewSettings tvs, UserTable table, int indexedCol,
            DisplayPrefs dp) {
        super(context);
        this.context = context;
        this.controller = controller;
        this.tvs = tvs;
        this.table = table;
        this.indexedCol = indexedCol;
        this.dp = dp;
        initListeners();
        if (indexedCol < 0) {
            buildNonIndexedTable();
        } else {
            buildIndexedTable(indexedCol);
            indexData.setOnTouchListener(indexDataCellClickListener);
            indexHeader.setOnTouchListener(indexHeaderCellClickListener);
            indexFooter.setOnTouchListener(indexFooterCellClickListener);
        }
        mainData.setOnTouchListener(mainDataCellClickListener);
        mainHeader.setOnTouchListener(mainHeaderCellClickListener);
        mainFooter.setOnTouchListener(mainFooterCellClickListener);
    }
    
    /**
     * Initializes the click listeners.
     */
    private void initListeners() {
        mainDataCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = mainData.getCellNumber(x, y);
                if (indexedCol < 0) {
                    return cellNum;
                } else {
                    int colNum = cellNum % (table.getWidth() - 1);
                    int rowNum = cellNum / (table.getWidth() - 1);
                    return cellNum + rowNum + ((colNum < indexedCol) ? 0 : 1);
                }
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.regularCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(mainData);
            }
        };
        mainHeaderCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = mainHeader.getCellNumber(x, y);
                if (indexedCol < 0) {
                    return cellNum;
                } else {
                    int colNum = cellNum % (table.getWidth() - 1);
                    return cellNum + ((colNum < indexedCol) ? 0 : 1);
                }
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.headerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(mainHeader);
            }
        };
        mainFooterCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = mainFooter.getCellNumber(x, y);
                if (indexedCol < 0) {
                    return cellNum;
                } else {
                    int colNum = cellNum % (table.getWidth() - 1);
                    return cellNum + ((colNum < indexedCol) ? 0 : 1);
                }
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.footerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(mainFooter);
            }
        };
        indexDataCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                int cellNum = indexData.getCellNumber(x, y);
                return (cellNum * table.getWidth()) + indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.indexedColCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(indexData);
            }
        };
        indexHeaderCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                return indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.headerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(indexHeader);
            }
        };
        indexFooterCellClickListener = new CellClickListener() {
            @Override
            protected int figureCellId(int x, int y) {
                return indexedCol;
            }
            @Override
            protected void takeClickAction(int cellId) {
                controller.footerCellClicked(cellId);
            }
            @Override
            protected void takeLongClickAction(int cellId) {
                controller.openContextMenu(indexFooter);
            }
        };
    }
    
    private void buildNonIndexedTable() {
        View wrapper = buildTable(-1, false);
        HorizontalScrollView wrapScroll = new HorizontalScrollView(context);
        wrapScroll.addView(wrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        wrapLp.weight = 1;
        addView(wrapScroll, wrapLp);
    }
    
    private void buildIndexedTable(int indexedCol) {
        View mainWrapper = buildTable(indexedCol, false);
        View indexWrapper = buildTable(indexedCol, true);
        HorizontalScrollView wrapScroll = new HorizontalScrollView(context);
        wrapScroll.addView(mainWrapper, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.addView(indexWrapper);
        wrapper.addView(wrapScroll);
        addView(wrapper);
        indexScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                mainScroll.scrollTo(mainScroll.getScrollX(),
                        view.getScrollY());
                return false;
            }
        });
        mainScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                indexScroll.scrollTo(indexScroll.getScrollX(),
                        view.getScrollY());
                return false;
            }
        });
    }
    
    /**
     * Builds a (piece of a) table. The table may either be the indexed column
     * of an indexed table, the non-indexed columns of an indexed table, or the
     * entirety of an unindexed table.
     * @param indexedCol the column that is indexed (or -1)
     * @param isIndexed whether this table is for the indexed column
     * @return a view including the header, body, and footer of the table
     */
    private View buildTable(int indexedCol, boolean isIndexed) {
        String[][] header;
        String[][] data;
        String[][] footer;
        ColumnColorRuler[] colorRulers;
        int[] colWidths;
        int[] completeColWidths = tvs.getTableColWidths();
        if (isIndexed) {
            header = new String[1][1];
            header[0][0] = table.getHeader(indexedCol);
            data = new String[table.getHeight()][1];
            for (int i = 0; i < table.getHeight(); i++) {
                data[i][0] = table.getData(i, indexedCol);
            }
            footer = new String[1][1];
            footer[0][0] = table.getFooter(indexedCol);
            colorRulers = new ColumnColorRuler[1];
            colorRulers[0] = dp.getColColorRuler(table.getHeader(indexedCol));
            colWidths = new int[1];
            colWidths[0] = completeColWidths[indexedCol];
        } else {
            int width = (indexedCol < 0) ? table.getWidth() :
                table.getWidth() - 1;
            header = new String[1][width];
            data = new String[table.getHeight()][width];
            footer = new String[1][width];
            colorRulers = new ColumnColorRuler[width];
            colWidths = new int[width];
            int addIndex = 0;
            for (int i = 0; i < table.getWidth(); i++) {
                if (i == indexedCol) {
                    continue;
                }
                header[0][addIndex] = table.getHeader(i);
                for (int j = 0; j < table.getHeight(); j++) {
                    data[j][addIndex] = table.getData(j, i);
                }
                footer[0][addIndex] = table.getFooter(i);
                colorRulers[addIndex] =
                    dp.getColColorRuler(table.getHeader(i));
                colWidths[addIndex] = completeColWidths[i];
                addIndex++;
            }
        }
        int avanda = getResources().getColor(R.color.Avanda);
        int headerData = getResources().getColor(R.color.header_data);
        int headerIndex = getResources().getColor(R.color.header_index);
        int footerIndex = getResources().getColor(R.color.footer_index);
        ScrollView dataScroll = new ScrollView(context);
        TabularView dataTable = new TabularView(context, data, avanda,
                Color.BLACK, Color.GRAY, colWidths, colorRulers,
                (isIndexed ? TableType.INDEX_DATA : TableType.MAIN_DATA));
        dataScroll.addView(dataTable, new ViewGroup.LayoutParams(
                dataTable.getTableWidth(), dataTable.getTableHeight()));
        TabularView headerTable = new TabularView(context, header,
                (isIndexed ? headerIndex : headerData), Color.BLACK,
                Color.GRAY, colWidths, null,
                (isIndexed ? TableType.INDEX_HEADER : TableType.MAIN_HEADER));
        TabularView footerTable = new TabularView(context, footer, footerIndex,
                Color.BLACK, Color.GRAY, colWidths, null,
                (isIndexed ? TableType.INDEX_FOOTER : TableType.MAIN_FOOTER));
        if (isIndexed) {
            indexData = dataTable;
            indexHeader = headerTable;
            indexFooter = footerTable;
            indexScroll = dataScroll;
        } else {
            mainData = dataTable;
            mainHeader = headerTable;
            mainFooter = footerTable;
            mainScroll = dataScroll;
        }
        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(headerTable, headerTable.getTableWidth(),
                headerTable.getTableHeight());
        LinearLayout.LayoutParams dataLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        dataLp.weight = 1;
        wrapper.addView(dataScroll, dataLp);
        wrapper.addView(footerTable, footerTable.getTableWidth(),
                footerTable.getTableHeight());
        return wrapper;
    }
    
    private void onCreateMainDataContextMenu(ContextMenu menu, int cellId) {
        controller.prepRegularCellOccm(menu, cellId);
    }
    
    private void onCreateIndexDataContextMenu(ContextMenu menu, int cellId) {
        controller.prepIndexedColCellOccm(menu, cellId);
    }
    
    private void onCreateHeaderContextMenu(ContextMenu menu, int cellId) {
        controller.prepHeaderCellOccm(menu, cellId);
    }
    
    private void onCreateFooterContextMenu(ContextMenu menu, int cellId) {
        controller.prepFooterCellOccm(menu, cellId);
    }
    
    private class TabularView extends View {
        
        private static final int ROW_HEIGHT = 30;
        private static final int HORIZONTAL_CELL_PADDING = 5;
        private static final int VERTICAL_CELL_PADDING = 9;
        private static final int BORDER_WIDTH = 1;
        
        private String[][] data;
        private int backgroundColor;
        private int foregroundColor;
        private int borderColor;
        private int[] columnWidths;
        private ColumnColorRuler[] colorRulers;
        private TableType type;
        
        private int totalHeight;
        private int totalWidth;
        
        public TabularView(Context context, String[][] data,
                int backgroundColor, int foregroundColor, int borderColor,
                int[] columnWidths, ColumnColorRuler[] colorRulers,
                TableType type) {
            super(context);
            init(data, backgroundColor, foregroundColor, borderColor,
                    columnWidths, colorRulers, type);
        }
        
        public TabularView(Context context, String[] data, int backgroundColor,
                int foregroundColor, int borderColor, int[] columnWidths,
                ColumnColorRuler[] colorRulers, TableType type) {
            super(context);
            String[][] outerData = {data};
            init(outerData, backgroundColor, foregroundColor, borderColor,
                    columnWidths, colorRulers, type);
        }
        
        private void init(String[][] data, int backgroundColor,
                int foregroundColor, int borderColor, int[] columnWidths,
                ColumnColorRuler[] colorRulers, TableType type) {
            this.data = data;
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            this.borderColor = borderColor;
            this.columnWidths = columnWidths;
            this.colorRulers = colorRulers;
            totalHeight = (ROW_HEIGHT + BORDER_WIDTH) * data.length +
                    BORDER_WIDTH;
            totalWidth = BORDER_WIDTH;
            for (int i = 0; i < columnWidths.length; i++) {
                totalWidth += columnWidths[i] + BORDER_WIDTH;
            }
            setMinimumHeight(totalHeight);
            setMinimumWidth(totalWidth);
            this.type = type;
            setClickable(true);
        }
        
        public int getTableHeight() {
            return totalHeight;
        }
        
        public int getTableWidth() {
            return totalWidth;
        }
        
        public int getCellNumber(int x, int y) {
            int row = y / (ROW_HEIGHT + BORDER_WIDTH);
            int col = -1;
            while (x > 0) {
                col++;
                x -= columnWidths[col] + BORDER_WIDTH;
            }
            return (row * columnWidths.length) + col; 
        }
        
        @Override
        public void onDraw(Canvas canvas) {
            if (data.length == 0) {
                return;
            }
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            // drawing the background
            paint.setColor(backgroundColor);
            canvas.drawRect(0, 0, totalWidth, totalHeight, paint);
            // drawing the borders
            paint.setColor(borderColor);
            int yCoord = 0;
            for (int i = 0; i <= data.length; i++) {
                canvas.drawRect(0, yCoord, totalWidth, yCoord + BORDER_WIDTH,
                        paint);
                yCoord += ROW_HEIGHT + BORDER_WIDTH;
            }
            int xCoord = 0;
            for (int i = 0; i <= data[0].length; i++) {
                canvas.drawRect(xCoord, 0, xCoord + BORDER_WIDTH, totalHeight,
                        paint);
                xCoord += (i == data[0].length) ? 0 :
                        columnWidths[i] + BORDER_WIDTH;
            }
            // drawing the cells
            paint.setTextSize(16);
            int x = BORDER_WIDTH;
            int y = BORDER_WIDTH;
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[0].length; j++) {
                    String datum = data[i][j];
                    if (datum == null) {
                        datum = "";
                    }
                    int columnWidth = columnWidths[j];
                    int foregroundColor = (colorRulers == null) ?
                            this.foregroundColor :
                            colorRulers[j].getForegroundColor(datum,
                                    this.foregroundColor);
                    int backgroundColor = (colorRulers == null) ?
                            this.backgroundColor :
                            colorRulers[j].getBackgroundColor(datum,
                                    this.backgroundColor);
                    drawCell(canvas, paint, x, y, datum, backgroundColor,
                            foregroundColor, columnWidth);
                    x += columnWidth + BORDER_WIDTH;
                }
                x = BORDER_WIDTH;
                y += ROW_HEIGHT + BORDER_WIDTH;
            }
        }
        
        private void drawCell(Canvas canvas, Paint paint, int x, int y,
                String datum, int backgroundColor, int foregroundColor,
                int columnWidth) {
            if (backgroundColor != this.backgroundColor) {
                paint.setColor(backgroundColor);
                canvas.drawRect(x, y, x + columnWidth, y + ROW_HEIGHT, paint);
            }
            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.clipRect(x + HORIZONTAL_CELL_PADDING, y,
                    x + columnWidth - (2 * HORIZONTAL_CELL_PADDING),
                    y + ROW_HEIGHT);
            paint.setColor(foregroundColor);
            canvas.drawText(datum, x + HORIZONTAL_CELL_PADDING,
                    (y + ROW_HEIGHT - VERTICAL_CELL_PADDING), paint);
            canvas.restore();
        }
        
        @Override
        public void onCreateContextMenu(ContextMenu menu) {
            switch(type) {
            case MAIN_DATA:
                onCreateMainDataContextMenu(menu, lastLongClickedCellId);
                return;
            case INDEX_DATA:
                onCreateIndexDataContextMenu(menu, lastLongClickedCellId);
                return;
            case MAIN_HEADER:
            case INDEX_HEADER:
                onCreateHeaderContextMenu(menu, lastLongClickedCellId);
                return;
            case MAIN_FOOTER:
            case INDEX_FOOTER:
                onCreateFooterContextMenu(menu, lastLongClickedCellId);
                return;
            }
        }
    }
    
    private abstract class CellClickListener implements View.OnTouchListener {
        
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            long duration = event.getEventTime() - event.getDownTime();
            if (event.getAction() != MotionEvent.ACTION_UP ||
                    duration < MIN_CLICK_DURATION) {
                return false;
            }
            int x = (new Float(event.getX())).intValue();
            int y = (new Float(event.getY())).intValue();
            int cellId = figureCellId(x, y);
            if (duration < MIN_LONG_CLICK_DURATION) {
                takeClickAction(cellId);
                return true;
            } else {
                lastLongClickedCellId = cellId;
                takeLongClickAction(cellId);
                return true;
            }
        }
        
        protected abstract int figureCellId(int x, int y);
        
        protected abstract void takeClickAction(int cellId);
        
        protected abstract void takeLongClickAction(int cellId);
    }
    
    public interface Controller {
        
        public void regularCellClicked(int cellId);
        
        public void headerCellClicked(int cellId);
        
        public void footerCellClicked(int cellId);
        
        public void indexedColCellClicked(int cellId);
        
        public void openContextMenu(View view);
        
        public void prepRegularCellOccm(ContextMenu menu, int cellId);
        
        public void prepHeaderCellOccm(ContextMenu menu, int cellId);
        
        public void prepFooterCellOccm(ContextMenu menu, int cellId);
        
        public void prepIndexedColCellOccm(ContextMenu menu, int cellId);
    }
}