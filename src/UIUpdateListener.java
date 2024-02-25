public interface UIUpdateListener {
    void paintPixel(int color, int col, int row);
    void repaintPaintPanel();
    void appendChatMessage(String message, boolean is_self);
}
