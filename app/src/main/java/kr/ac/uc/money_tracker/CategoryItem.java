package kr.ac.uc.money_tracker;

import androidx.annotation.NonNull;

public class CategoryItem {
    private int id;
    private String name;

    public CategoryItem(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }

    @NonNull
    @Override
    public String toString() {
        return name; // Spinner에 보일 텍스트
    }
}
