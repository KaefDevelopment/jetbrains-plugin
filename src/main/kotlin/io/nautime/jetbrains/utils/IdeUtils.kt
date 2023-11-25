package io.nautime.jetbrains.utils

import java.awt.KeyboardFocusManager

class IdeUtils {

    companion object {

        fun isIdeInFocus(): Boolean {
            return KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow != null
        }



    }
}
