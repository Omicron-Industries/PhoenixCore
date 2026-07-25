package net.phoenix.core.integration.phoenix_chronicles;

public class QuestGroup {

    private String id;
    private String label;
    private int color;
    private int borderColor;
    private int x, y, width, height;
    private String category;

    private static final int DEFAULT_COLOR = 0x22FFFFFF;
    private static final int DEFAULT_BORDER = 0x44FFFFFF;

    public QuestGroup(String id, String label, String category) {
        this.id = id;
        this.label = label;
        this.category = category;
        this.color = DEFAULT_COLOR;
        this.borderColor = DEFAULT_BORDER;
        this.x = 0;
        this.y = 0;
        this.width = 120;
        this.height = 80;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getCategory() {
        return category;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
