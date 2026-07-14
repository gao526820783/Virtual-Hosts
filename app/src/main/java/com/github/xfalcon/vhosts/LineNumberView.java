package com.github.xfalcon.vhosts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

public class LineNumberView extends View {

    private EditText editor;
    private Paint paint;

    public LineNumberView(Context context) {
        super(context);
        init();
    }

    public LineNumberView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(0xFF888888);
        paint.setTextSize(36f);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setAntiAlias(true);
    }

    public void setEditor(EditText editor) {
        this.editor = editor;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;
        if (editor != null && editor.getLayout() != null) {
            height = editor.getLayout().getHeight() + editor.getTotalPaddingTop()
                     + editor.getTotalPaddingBottom();
        }
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            Math.max(height, MeasureSpec.getSize(heightMeasureSpec)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (editor == null || editor.getLayout() == null) return;

        int lineCount = editor.getLineCount();
        if (lineCount == 0) return;

        int lineHeight = editor.getLineHeight();
        int paddingTop = editor.getTotalPaddingTop();
        int scrollY = editor.getScrollY();

        int firstLine = scrollY / lineHeight;
        if (firstLine < 0) firstLine = 0;

        int visibleLines = getHeight() / lineHeight + 1;
        int lastLine = firstLine + visibleLines;
        if (lastLine > lineCount) lastLine = lineCount;

        int y = paddingTop + (firstLine + 1) * lineHeight - scrollY;
        for (int i = firstLine; i < lastLine; i++) {
            canvas.drawText(String.valueOf(i + 1), getWidth() - 8f, y, paint);
            y += lineHeight;
        }
    }
}
