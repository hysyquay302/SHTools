// IASBLInterface.aidl
package com.quayquay.shtools;

import com.quayquay.shtools.screendefinitions.ScreenInfo;
import com.quayquay.shtools.screendefinitions.ScreenNode;

interface IASBLInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    boolean clickByPos(int x, int y, boolean longPress);

    boolean clickByComp(String screenID, String compId);

    boolean doubleClickByPos(int x, int y);

    boolean swipe(int x1, int y1, int x2, int y2, int duration);

    boolean openPackage(String pckg);

    boolean inputText(String txt, in ScreenNode target, boolean delay);

    boolean scrollForward();

    boolean scrollBackward();

    boolean globalBack();

    String getCurrentForgroundPkg();

    String getLastToastMessage(String packageName);

    void clearToastCache(String packageName);

    void updateKeywordDefinitions(String definitions);

    ScreenInfo detectScreen(String appName, boolean includeNullNode);

    boolean isKeyboardShown();

    boolean performGlobalAction(int action);
}