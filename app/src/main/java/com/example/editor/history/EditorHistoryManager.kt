package com.example.editor.history

import com.example.editor.model.EditorProjectState

/**
 * Bounded history stack manager for non-destructive editor undo and redo operations.
 * Bounded to max [maxHistorySize] states to prevent unbounded RAM growth.
 */
class EditorHistoryManager(
    private val maxHistorySize: Int = 30
) {
    private val undoStack = ArrayDeque<EditorProjectState>()
    private val redoStack = ArrayDeque<EditorProjectState>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * Push current state before a mutation occurs. Clears the redo stack.
     */
    fun pushState(state: EditorProjectState) {
        if (undoStack.size >= maxHistorySize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(state)
        redoStack.clear()
    }

    /**
     * Undo the last change. Moves the currentState into the redo stack and returns previous state.
     */
    fun undo(currentState: EditorProjectState): EditorProjectState? {
        if (undoStack.isEmpty()) return null
        val previousState = undoStack.removeLast()
        redoStack.addLast(currentState)
        return previousState
    }

    /**
     * Redo the previously undone change. Moves currentState into undo stack and returns redo state.
     */
    fun redo(currentState: EditorProjectState): EditorProjectState? {
        if (redoStack.isEmpty()) return null
        val nextState = redoStack.removeLast()
        undoStack.addLast(currentState)
        return nextState
    }

    /**
     * Clear all history when loading a new project.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
