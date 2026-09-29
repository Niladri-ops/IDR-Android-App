package com.voltedge.mitra

import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper

/**
 * A safe wrapper around [InputConnection] that guards against calls executed
 * on an unbound or closed input connection (common on vendor IMEs like Samsung HoneyBoard
 * or custom accessibility services).
 */
class SafeInputConnectionWrapper(
    target: InputConnection?,
    mutable: Boolean
) : InputConnectionWrapper(target, mutable) {

    @Volatile
    private var isBound = true

    override fun closeConnection() {
        isBound = false
        try {
            super.closeConnection()
        } catch (e: Throwable) {
            // Suppress unbind exception
        }
    }

    override fun finishComposingText(): Boolean {
        if (!isBound) return false
        return try {
            super.finishComposingText()
        } catch (e: Throwable) {
            false
        }
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        if (!isBound) return null
        return try {
            super.getExtractedText(request, flags)
        } catch (e: Throwable) {
            null
        }
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        if (!isBound) return null
        return try {
            super.getTextBeforeCursor(n, flags)
        } catch (e: Throwable) {
            null
        }
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        if (!isBound) return null
        return try {
            super.getTextAfterCursor(n, flags)
        } catch (e: Throwable) {
            null
        }
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        if (!isBound) return null
        return try {
            super.getSelectedText(flags)
        } catch (e: Throwable) {
            null
        }
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        if (!isBound) return 0
        return try {
            super.getCursorCapsMode(reqModes)
        } catch (e: Throwable) {
            0
        }
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (!isBound) return false
        return try {
            super.commitText(text, newCursorPosition)
        } catch (e: Throwable) {
            false
        }
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (!isBound) return false
        return try {
            super.deleteSurroundingText(beforeLength, afterLength)
        } catch (e: Throwable) {
            false
        }
    }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (!isBound) return false
        return try {
            super.setComposingText(text, newCursorPosition)
        } catch (e: Throwable) {
            false
        }
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        if (!isBound) return false
        return try {
            super.setComposingRegion(start, end)
        } catch (e: Throwable) {
            false
        }
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        if (!isBound) return false
        return try {
            super.setSelection(start, end)
        } catch (e: Throwable) {
            false
        }
    }

    override fun performEditorAction(editorAction: Int): Boolean {
        if (!isBound) return false
        return try {
            super.performEditorAction(editorAction)
        } catch (e: Throwable) {
            false
        }
    }

    override fun performContextMenuAction(id: Int): Boolean {
        if (!isBound) return false
        return try {
            super.performContextMenuAction(id)
        } catch (e: Throwable) {
            false
        }
    }

    override fun beginBatchEdit(): Boolean {
        if (!isBound) return false
        return try {
            super.beginBatchEdit()
        } catch (e: Throwable) {
            false
        }
    }

    override fun endBatchEdit(): Boolean {
        if (!isBound) return false
        return try {
            super.endBatchEdit()
        } catch (e: Throwable) {
            false
        }
    }

    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        if (!isBound) return false
        return try {
            super.sendKeyEvent(event)
        } catch (e: Throwable) {
            false
        }
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        if (!isBound) return false
        return try {
            super.clearMetaKeyStates(states)
        } catch (e: Throwable) {
            false
        }
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        if (!isBound) return false
        return try {
            super.reportFullscreenMode(enabled)
        } catch (e: Throwable) {
            false
        }
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        if (!isBound) return false
        return try {
            super.performPrivateCommand(action, data)
        } catch (e: Throwable) {
            false
        }
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        if (!isBound) return false
        return try {
            super.requestCursorUpdates(cursorUpdateMode)
        } catch (e: Throwable) {
            false
        }
    }

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        if (!isBound) return false
        return try {
            super.commitCompletion(text)
        } catch (e: Throwable) {
            false
        }
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        if (!isBound) return false
        return try {
            super.commitCorrection(correctionInfo)
        } catch (e: Throwable) {
            false
        }
    }

    override fun getHandler(): Handler? {
        if (!isBound) return null
        return try {
            super.getHandler()
        } catch (e: Throwable) {
            null
        }
    }
}
