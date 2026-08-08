package com.riftcompanions.story;

import java.util.*;

/**
 * Task Interface — allows selecting exactly 5 tasks (e.g., numbered 5 to 10)
 * before the AI generates the complete 5-chapter story.
 */
public final class StregnerTaskSelectionInterface {
    private StregnerTaskSelectionInterface() {}

    public static final int MIN_TASK_NUMBER = 5;
    public static final int MAX_TASK_NUMBER = 10;
    public static final int TASK_COUNT = 5;

    private final List<Integer> selectedTaskNumbers = new ArrayList<>();

    public void selectTasks(List<Integer> numbers) {
        selectedTaskNumbers.clear();
        for (int n : numbers) {
            if (n >= MIN_TASK_NUMBER && n <= MAX_TASK_NUMBER) {
                selectedTaskNumbers.add(n);
            }
        }
    }

    public List<Integer> getSelectedTasks() {
        return new ArrayList<>(selectedTaskNumbers);
    }

    public boolean isValidSelection() {
        return selectedTaskNumbers.size() == TASK_COUNT;
    }

    public String getSelectionSummary() {
        return "Selected 5 tasks from range " + MIN_TASK_NUMBER + "-" + MAX_TASK_NUMBER + ": " + selectedTaskNumbers;
    }
}
