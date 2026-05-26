package com.quayquay.shtools.services;

import static com.quayquay.shtools.extention.ASUtils.delay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.lifecycle.LifecycleOwner;

import com.quayquay.hsq.tools.HSQConfig;
import com.quayquay.shtools.IASBLInterface;
import com.quayquay.shtools.extention.ASUtils;
import com.quayquay.shtools.extention.LOG;
import com.quayquay.shtools.screendefinitions.DefinitionNode;
import com.quayquay.shtools.screendefinitions.DefintionElement;
import com.quayquay.shtools.screendefinitions.ScreenInfo;
import com.quayquay.shtools.screendefinitions.ScreenNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

public class ASBLBridgeService extends Service {
    private static final String TAG = "ASBLBridgeService";

    @SuppressLint("StaticFieldLeak")
    public static AccessibilityService asblService = null;
    private static Map<String, String> sLastToastMsg = null;
    @SuppressLint("StaticFieldLeak")
    public static int widthOfScreen = 0;
    public static int heightOfScreen = 0;
    public static int dpi = 0;
    private static Rect tempRect = new Rect();
    private final Timer mCheckHangTimer = new Timer();
    private LifecycleOwner viewLifecycleOwner;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        setASBLInstance(ApiAccessibilityService.getInstance());
        WindowManager wmgr = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wmgr.getDefaultDisplay().getRealMetrics(metrics);
        widthOfScreen = metrics.widthPixels;
        heightOfScreen = metrics.heightPixels;
        dpi = metrics.densityDpi;
        mCheckHangTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    LOG.D(TAG, "checking hang ... ");
                    ScreenInfo screenInfo = detectScreen("common", false);
                    if ("SCREEN_APP_NOT_RESPONDING".equals(screenInfo.screen_id)) {
                        ASUtils.findAndClick("ID_CLOSE_APP_BTN", screenInfo.screen_nodes);
                    }
                } catch (Exception e) {
                    LOG.printStackTrace(TAG, e);
                }
            }
        }, 1000, 120000);

    }

    private IASBLInterface.Stub mBinder = new IASBLInterface.Stub() {
        @Override
        public boolean clickByPos(int x, int y, boolean longPress) throws RemoteException {
            return ASBLBridgeService.this.do_click(x, y, longPress);
        }

        @Override
        public boolean clickByComp(String screenID, String compId) throws RemoteException {
            return ASBLBridgeService.this.do_click(screenID, compId);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean doubleClickByPos(int x, int y) throws RemoteException {
            return ASBLBridgeService.this.do_double_click(x, y);
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public boolean swipe(int x1, int y1, int x2, int y2, int duration) throws RemoteException {
            return ASBLBridgeService.this.swipe(x1, y1, x2, y2, duration);
        }

        @Override
        public boolean openPackage(String pckg) throws RemoteException {
            return ASBLBridgeService.this.openPacakge(pckg);
        }

        @Override
        public boolean inputText(String txt, ScreenNode target, boolean delay) throws RemoteException {
            return ASBLBridgeService.this.inputText(txt, target, delay);
        }

        @Override
        public boolean scrollForward() throws RemoteException {
            return ASBLBridgeService.this.scrollForward();
        }

        @Override
        public boolean scrollBackward() throws RemoteException {
            return ASBLBridgeService.this.scrollBackward();
        }

        @Override
        public boolean globalBack() throws RemoteException {
            return ASBLBridgeService.this.globalBack();
        }

        @Override
        public String getCurrentForgroundPkg() throws RemoteException {
            return ASBLBridgeService.this.getCurrentForgroundPkg();
        }

        @Override
        public String getLastToastMessage(String packageName) throws RemoteException {
            return ASBLBridgeService.this.getLastToastMessage(packageName);
        }

        @Override
        public void clearToastCache(String packageName) throws RemoteException {
            ASBLBridgeService.this.clearToastCache(packageName);
        }

        @Override
        public void updateKeywordDefinitions(String definitions) throws RemoteException {
            ASBLBridgeService.this.updateKeywordDefinitions(definitions);
        }

        @Override
        public ScreenInfo detectScreen(String appName, boolean includeNullNode) throws RemoteException {
            return ASBLBridgeService.this.detectScreen(appName, includeNullNode);
        }

        @Override
        public boolean isKeyboardShown() throws RemoteException {
            return ASBLBridgeService.this.isKeyboardShown();
        }

        @Override
        public boolean performGlobalAction(int action) throws RemoteException {
            return ASBLBridgeService.this.performGlobalAction(action);
        }
    };

    public static boolean clickButtonById(String buttonId, int timeout, boolean longPress) {
        for (int i = 0; i < timeout; i++) {
            AccessibilityNodeInfo rootNode = asblService.getRootInActiveWindow();
            if (rootNode != null) {
                AccessibilityNodeInfo targetNode = findNodeById(rootNode, buttonId);
                if (targetNode != null && targetNode.isVisibleToUser() && targetNode.isEnabled()) {
                    Rect bounds = new Rect();
                    targetNode.getBoundsInScreen(bounds);
                    int centerX = (bounds.left + bounds.right) / 2;
                    int centerY = (bounds.top + bounds.bottom) / 2;
                    if (centerX >= 0 && centerY >= 0) {
                        boolean checkClick = do_click(centerX, centerY, longPress);
                        if (checkClick) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    return false;
                }
            }
            delay(1000);
        }
        return false;
    }

    public static AccessibilityNodeInfo findNodeById(AccessibilityNodeInfo rootNode, String id) {
        // Tìm nút bằng id trong cây nút
        AccessibilityNodeInfo targetNode = null;
        int childCount = rootNode.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo childNode = rootNode.getChild(i);
            if (childNode != null) {
                if (id.equals(childNode.getViewIdResourceName())) {
                    targetNode = childNode;
                    break;
                } else {
                    targetNode = findNodeById(childNode, id);
                    if (targetNode != null) {
                        break;
                    }
                }
            }
        }
        return targetNode;
    }

    public static List<AccessibilityNodeInfo> getAllNodesOnScreen() {
        AccessibilityNodeInfo rootNode = asblService.getRootInActiveWindow();
        if (rootNode == null) {
            return null;
        }

        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        collectNodes(rootNode, allNodes);
        try{
            rootNode.recycle();
        } catch (Exception e) {}
        return allNodes;
    }

    public static void collectNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> allNodes) {
        if (node == null) {
            return;
        }

        // Thêm node hiện tại vào danh sách
        allNodes.add(node);

        // Duyệt các node con của node hiện tại
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            collectNodes(childNode, allNodes);
        }
    }

    public static void clickByXPath(String xpath) {
        AccessibilityNodeInfo rootNode = asblService.getRootInActiveWindow();
        if (rootNode == null) {
            return;
        }

        List<AccessibilityNodeInfo> nodeList = findNodesByXPath(rootNode, xpath);
        if (!nodeList.isEmpty()) {
            nodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        try {
            rootNode.recycle();
        } catch (Exception e) {}
    }

    public static List<AccessibilityNodeInfo> findNodesByXPath(AccessibilityNodeInfo rootNode, String xpath) {
        String xml = buildXmlHierarchy(rootNode);
        Document document = parseXml(xml);

        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expression = xPath.compile(xpath);
            NodeList result = (NodeList) expression.evaluate(document, XPathConstants.NODESET);

            List<AccessibilityNodeInfo> nodeList = new ArrayList<>();
            for (int i = 0; i < result.getLength(); i++) {
                AccessibilityNodeInfo node = findNodeByXmlId(rootNode, result.item(i).getNodeValue());
                if (node != null) {
                    nodeList.add(node);
                }
            }

            return nodeList;
        } catch (Exception e) {
            // Xử lý ngoại lệ
            return new ArrayList<>();
        }
    }

    private static String buildXmlHierarchy(AccessibilityNodeInfo node) {
        StringBuilder builder = new StringBuilder();
        builder.append("<node id=\"").append(node.hashCode()).append("\" text=\"").append(node.getText())
                .append("\" class=\"").append(node.getClassName()).append("\">");
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                builder.append(buildXmlHierarchy(child));
                try {
                    child.recycle();
                } catch (Exception e) {}

            }
        }
        builder.append("</node>");
        return builder.toString();
    }

    private static Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml.getBytes()));
        } catch (Exception e) {
            // Xử lý ngoại lệ
            return null;
        }
    }

    private static AccessibilityNodeInfo findNodeByXmlId(AccessibilityNodeInfo rootNode, String xmlId) {
        if (String.valueOf(rootNode.hashCode()).equals(xmlId)) {
            return rootNode;
        }

        for (int i = 0; i < rootNode.getChildCount(); i++) {
            AccessibilityNodeInfo child = rootNode.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByXmlId(child, xmlId);
                if (result != null) {
                    return result;
                }
                try{
                    child.recycle();
                } catch (Exception e) {}

            }
        }

        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCheckHangTimer.cancel();
    }

    public static void setASBLInstance(AccessibilityService accessibilityService) {
        asblService = accessibilityService;
    }

    public static void setLastToastMsg(String packageName, String text) {
        if (sLastToastMsg == null) sLastToastMsg = new HashMap<>();
        synchronized (sLastToastMsg) {
            sLastToastMsg.put(packageName, text);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean do_click(int x, int y, boolean longPress) {
        try
        {
            LOG.I(TAG, String.format("[%d,%d]", x, y));
            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            Random rand = new Random();
            int pointCount = rand.nextInt(3) + 1;
            for (int i = 0; i < pointCount; i++)
            {
                clickPath.lineTo(x + rand.nextInt(6) - 3, y + rand.nextInt(6) - 3);
            }
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, longPress ? 1000 : 100));
            return asblService.dispatchGesture(
                    gestureBuilder.build(), new AccessibilityService.GestureResultCallback()
                    {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription)
                        {
                            super.onCompleted(gestureDescription);
                        }
                    }, null
            );
        }
        catch (Exception e)
        {
            return false;
        }

    }

    public static boolean do_click(String screenId, String compId) {
        for (int i = 0; i < ScreenInfo.sDefinitions.size(); i++) {
            try {
                DefintionElement obj = ScreenInfo.sDefinitions.get(i);
                if (obj != null && obj.screen_id.equals(screenId)) {
                    Iterator<String> keysItr = obj.keywords.keySet().iterator();
                    AccessibilityNodeInfo root = asblService.getRootInActiveWindow();

                    while (keysItr.hasNext()) {
                        String langCode = keysItr.next();
                        List<DefinitionNode> keywordsByLang = obj.keywords.get(langCode);
                        for (int j = 0; j < keywordsByLang.size(); j++) {
                            DefinitionNode keyword = keywordsByLang.get(j);
                            String keywordID = keyword.keyword;

                            if (keywordID.equals(compId)) {
                                String keywordDes = keyword.contentDescription;
                                String keywordText = keyword.text;

                                List<AccessibilityNodeInfo> nodes = null;
                                if (keywordText != null && !keywordText.equalsIgnoreCase("null"))
                                    nodes = findAccessibilityNodeInfosByTextDes(root, keywordText, true, false);
                                else if (keywordDes != null && !keywordDes.equalsIgnoreCase("null"))
                                    nodes = findAccessibilityNodeInfosByTextDes(root, keywordDes, true, false);

                                if (nodes != null) {
                                    for (AccessibilityNodeInfo node : nodes) {
                                        List<AccessibilityNodeInfo.AccessibilityAction> actions = node.getActionList();
                                        if (actions != null && actions.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)) {
                                            LOG.D(TAG, "performAction: ACTION_CLICK");
                                            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean do_double_click(final int x, final int y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        Random rand = new Random();
        clickPath.lineTo(x + rand.nextInt(6) - 3, y + rand.nextInt(6) - 3);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, 100));
        asblService.dispatchGesture(gestureBuilder.build(), null, null);

        sleep(200);
        return asblService.dispatchGesture(gestureBuilder.build(), null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static boolean swipe(int x1, int y1, int x2, int y2, int delay) {
        LOG.I(TAG, String.format("swipe from [%d,%d] to [%d,%d]",x1,y1,x2,y2));
        try {
            Path clickPath = new Path();
            clickPath.moveTo(x1, y1);
            for (int i = 1; i <= 20; i++) {
                int offset = new Random().nextInt(10);
                int x = x1 + (x2 - x1) * i * i / 400 + offset;
                int y = y1 + (y2 - y1) / 20 * i + offset / 2;
                clickPath.lineTo(x, y);
            }
            clickPath.lineTo(x2, y2);
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(clickPath, 0, new Random().nextInt(100) + delay));
            boolean res = asblService.dispatchGesture(gestureBuilder.build(), null, null);
            sleep(delay);
            return true;
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
            return false;
        }
    }

    public static boolean inputText(String text, ScreenNode targetObj, boolean delay) {
        LOG.D(TAG, "inputText text: " + text + " -- targetObj: " + targetObj + " -- delay: " + delay);
        AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
        if (root != null) {
            AccessibilityNodeInfo focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
            if (focus == null || !focus.isFocused() || !focus.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)) {
                focus = findInputFocus(root);
            }

            if (focus != null) {
                if (targetObj != null) {
                    try {
                        String contentDesFocused = focus.getContentDescription() != null ? focus.getContentDescription().toString() : "null";
                        String textFocused = focus.getText() != null ? focus.getText().toString() : "null";
                        String classNameFocused = focus.getClassName() != null ? focus.getClassName().toString() : "null";

                        if (targetObj.contentDescription.equalsIgnoreCase(contentDesFocused) &&
                                targetObj.text.equalsIgnoreCase(textFocused) &&
                                targetObj.className.equalsIgnoreCase(classNameFocused)) {
                            // Do nothing
                        } else {
                            try {
                                focus.recycle();
                                root.recycle();
                            } catch (Exception e) {}
                            return false;
                        }
                    } catch (Exception e) {
                        LOG.E(TAG, "inputText Error: " + e);
                        try {
                            focus.recycle();
                            root.recycle();
                        } catch (Exception f) {}
                        return false;
                    }
                }

                String test = "";
                Bundle arguments = new Bundle();
                if (delay) {
                    for (char c : text.toCharArray()) {
                        test += c;
                        arguments.putString(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, test);
                        focus.performAction(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, arguments);
                        sleep(new Random().nextInt(300) + 200);
                    }
                } else {
                    arguments.putString(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                    focus.performAction(AccessibilityNodeInfoCompat.ACTION_SET_TEXT, arguments);
                }
                try {
                    focus.recycle();
                    root.recycle();
                } catch (Exception e) {}
                return true;
            } else {
                LOG.E(TAG, "inputText: Focused node is not found");
            }
            try {
                root.recycle();
            } catch (Exception E) {}
        }
        return false;
    }

    public static AccessibilityNodeInfo findInputFocus(AccessibilityNodeInfo root) {
        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();

        if (root == null) {
            return null;
        }

        deque.add(root);
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            if (node != null) {
                if (node.isFocused() && node.getActionList().contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)) {
                    return node;
                }
                for (int i = 0; i < node.getChildCount(); i++) {
                    deque.addLast(node.getChild(i));
                }
            }
        }
        return null;
    }

    private static List<AccessibilityNodeInfo> findScrollables(AccessibilityNodeInfo root, int action) {
        List<AccessibilityNodeInfo> results = new ArrayList<>();
        Deque<AccessibilityNodeInfo> deque = new ArrayDeque<>();
        AccessibilityNodeInfo.AccessibilityAction accessibilityAction;
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            accessibilityAction = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD;
        else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            accessibilityAction = AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD;
        else
            return results;

        if (root == null) {
            return results;
        }
        deque.add(root);
        while (!deque.isEmpty()) {
            AccessibilityNodeInfo node = deque.removeFirst();
            boolean isCollectedNode = false;
            if (node != null) {
                if (node.getActionList().contains(accessibilityAction)) {
                    if (node.getClassName() == null || !node.getClassName().equals("androidx.viewpager.widget.ViewPager")) {
                        isCollectedNode = true;
                        results.add(node);
                    }
                }
                for (int i = 0; i < node.getChildCount(); i++) {
                    deque.addLast(node.getChild(i));
                }
                if (!isCollectedNode) {
                    try {
                        node.recycle();
                    } catch (Exception e) {}
                }
            }
        }
        return results;
    }

    private static boolean scrollMainScrollable(int action) {
        try {
            AccessibilityNodeInfo root = asblService.getRootInActiveWindow();
            if (root != null) {
                AccessibilityNodeInfo mainScrollable = findMainScrollable(root, action);
                if (mainScrollable != null) {
                    synchronized (mainScrollable) {
                        mainScrollable.performAction(action);
                        try {
                            mainScrollable.recycle();
                        } catch (Exception e){}
                    }
                    try {
                        root.recycle();
                    } catch (Exception e) {}
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.E(TAG, "scrollMainScrollable: " + e);
        }
        return false;
    }

    private static AccessibilityNodeInfo findMainScrollable(AccessibilityNodeInfo root, int action) {
        AccessibilityNodeInfo mainScrollable = null;
        try {
            if (root != null) {
                List<AccessibilityNodeInfo> scrollables = findScrollables(asblService.getRootInActiveWindow(), action);
                int mainScrollableHeight = 0;
                for (AccessibilityNodeInfo node : scrollables) {
                    if (node != null) {
                        Rect boundsInSCreen = new Rect();
                        node.getBoundsInScreen(boundsInSCreen);
                        if (mainScrollable == null || (boundsInSCreen.height() >= mainScrollableHeight &&
                                boundsInSCreen.left <= 0 && boundsInSCreen.right >= widthOfScreen)) {
                            mainScrollableHeight = boundsInSCreen.height();
                            if (mainScrollable != null) {
                                try {
                                    mainScrollable.recycle();
                                } catch (Exception e) {}
                            }
                            mainScrollable = node;
                        } else {
                            try {
                                node.recycle();
                            } catch (Exception e) {}

                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.E(TAG, "findMainScrollable: " + e);
        }
        return mainScrollable;
    }

    public static String getCurrentForgroundPkg() {
        String currentPkg = null;
        try {
            if (asblService.getRootInActiveWindow() != null) {
                currentPkg = asblService.getRootInActiveWindow().getPackageName().toString();
            }
            LOG.D(TAG, "Current package: " + (currentPkg == null ? "NULL" : currentPkg));
        } catch (Exception e) {
        }
        return currentPkg;
    }

    public static String getLastToastMessage(String packageName) {
        if (sLastToastMsg == null) return null;
        synchronized (sLastToastMsg) {
            return sLastToastMsg.get(packageName);
        }
    }

    public static void clearToastCache(String packageName) {
        if (sLastToastMsg == null) return;
        synchronized (sLastToastMsg) {
            if (sLastToastMsg.containsKey(packageName)) {
                sLastToastMsg.remove(packageName);
            }
        }
    }

    public static void updateKeywordDefinitions(String definitionsJson) {
        try {
            JSONArray defArr = new JSONArray(definitionsJson);
            if (defArr == null || defArr.length() <= 0) {
                LOG.E(TAG, "Invalid definitions");
            } else {
                ScreenInfo.updateDefinitions(defArr);
            }
        } catch (JSONException e) {
            LOG.printStackTrace(TAG, e);
        }
    }

    public ScreenInfo detectScreen(String currAppName, boolean includeNullNode) {
        LOG.D(TAG, "detectScreen: " + currAppName);
        try {
            /** Try to detect page on all window **/


            List<AccessibilityWindowInfo> windowList = asblService.getWindows();
            List<AccessibilityNodeInfo> nodesOnScreen = new ArrayList<AccessibilityNodeInfo>();

            AccessibilityNodeInfo rootInActWin = asblService.getRootInActiveWindow();
            if (rootInActWin != null) {
                rootInActWin.refresh();
                this.getNodeListOnTree(rootInActWin, includeNullNode);
            }

            for (AccessibilityWindowInfo window : windowList) {
                if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD ||
                        (window.isActive() && window.isFocused())) {
                    List<AccessibilityNodeInfo> nodesOnWindow = null;
                    nodesOnWindow = this.getNodeListOnTree(window.getRoot(), includeNullNode);
                    if (nodesOnWindow != null) {
                        nodesOnScreen.addAll(nodesOnWindow);
                    }
                }
            }

            ScreenInfo screenInfo = new ScreenInfo(nodesOnScreen, currAppName);
            LOG.D(TAG, screenInfo.toString());

            return screenInfo;
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private List<AccessibilityNodeInfo> getNodeListOnTree(AccessibilityNodeInfo mNodeInfo, boolean includeNullNode) {
        if (mNodeInfo == null) {
            return null;
        }
        List<AccessibilityNodeInfo> result = new ArrayList<AccessibilityNodeInfo>();


        for (int i = 0; i < mNodeInfo.getChildCount(); i++) {
            List<AccessibilityNodeInfo> subList = this.getNodeListOnTree(mNodeInfo.getChild(i), includeNullNode);
            if (subList != null) {
                result.addAll(subList);
            }
        }

        boolean isCollected = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isValidNodeOnScreen(mNodeInfo, includeNullNode)) {
                if (!result.contains(mNodeInfo)) {
                    result.add(mNodeInfo);
                }
                isCollected = true;
            }
        }

        if (!isCollected) {
            try {
                mNodeInfo.recycle();
            } catch (Exception e) {}
        }

        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static boolean isValidNodeOnScreen(AccessibilityNodeInfo node, boolean includeNullNode) {
        if (node != null) {
            node.getBoundsInScreen(tempRect);
            if (node.isVisibleToUser() &&
                    tempRect.left >= 0 &&
                    tempRect.top >= 0 &&
                    tempRect.top <= heightOfScreen &&
                    tempRect.left <= widthOfScreen) {
                if (includeNullNode ||
                        node.getText() != null ||
                        node.getContentDescription() != null ||
                        node.getHintText() != null)
                    return true;
                else
                    return false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isKeyboardShown() {
        try {
            List<AccessibilityWindowInfo> windows = asblService.getWindows();
            for (AccessibilityWindowInfo window : windows) {
                if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.E(TAG, "isKeyboardShown: " + e);
        }
        return false;
    }

    public static boolean performGlobalAction(int action) {
        return asblService.performGlobalAction(action);
    }

    public static List<AccessibilityNodeInfo> findNodesByAction(AccessibilityNodeInfo rootNode, int action) {
        List<AccessibilityNodeInfo> result = new ArrayList<>();
        findNodesByActionRecursive(rootNode, action, result);
        return result;
    }

    private static void findNodesByActionRecursive(AccessibilityNodeInfo node, int action, List<AccessibilityNodeInfo> result) {
        if (node == null) {
            return;
        }

        if ((node.getActions() & action) == action) {
            result.add(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            findNodesByActionRecursive(node.getChild(i), action, result);
        }
    }

    public static boolean openPacakge(String packageName) {
        try {
            Intent launchIntent = asblService.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.setFlags(launchIntent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_FROM_BACKGROUND);
                asblService.startActivity(launchIntent);
                return true;
            }
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
        }
        return false;
    }

    public static void StopApp(String packageName, boolean delete_data) {
        while (true) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", packageName, null); // Thay thế bằng package name chính xác của TunnelBear
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            HSQConfig.getContext().startActivity(intent);
            delay(3000);
            int checkstop = findMultiTextDes(15, true, true, false,false, "force stop");
            if (checkstop == 0) {
                clearrecents();
                continue;
            }
            boolean clickbtn = false;
            if (checkstop == 1) {
                clickbtn = clickButtonById("com.android.settings:id/button3", 2, false);
            } else if (checkstop == 2) {
                clickbtn = clickButtonById("com.android.settings:id/button2_negative", 2, false);
            }
            if (clickbtn) {
                delay(3000);
                if (!clickButtonById("android:id/button1", 15, false)) {
                    clearrecents();
                    continue;
                }
                delay(3000);
            }
            if (delete_data) {
                boolean clickStorage = findAndClickByTextDes("storage", false,true, true, false, 5);
                if (clickStorage) {
                    delay(3000);
                    boolean clickbtn2 = clickButtonById("com.android.settings:id/button1", 15, false);
                    if (!clickbtn2) {
                        clearrecents();
                        continue;
                    }
                    delay(3000);
                    boolean clickbtn3 = clickButtonById("android:id/button1", 10, false);
                    if (clickbtn3) {
                        delay(5000);
                        clearrecents();
                        return;
                    } else {
                        clearrecents();
                        continue;
                    }
                } else {
                    clearrecents();
                    continue;
                }
            } else {
                globalHome();
                delay(2000);
                return;
            }
        }
    }

    public static void clearrecents() {
        while(true) {
            globalHome();
            delay(3000);
            globalrecents();
            delay(3000);
            if (!findAndClickByTextDes("no recent items", true, true, true, false, 2)) {
                int x1 = (10 * widthOfScreen) / 100;
                int y = (50 * heightOfScreen) / 100;
                int x2 = (90 * widthOfScreen) / 100;
                swipe(x1, y, x2, y, 500);
                delay(1000);
                swipe(x1, y, x2, y, 500);
                delay(1000);
                swipe(x1, y, x2, y, 500);
                delay(1000);
                int j = 0;
                while (true) {
                    if (!findAndClickByTextDes("clear all", true, true, true, false, 2)) {
                        swipe(x1, y, x2, y, 500);
                        if (j > 10) {
                            break;
                        }
                        j++;
                        delay(1000);
                    } else {
                        break;
                    }
                }
                if(j > 10) {
                    continue;
                }
                delay(3000);
            }
            break;
        }
    }
    public static boolean globalBack () {
        return asblService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    public static boolean showPowerDialog () {
        return asblService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG);
    }

    public static boolean globalHome () {
        return asblService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    public static boolean globalrecents () {
        return asblService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
    }

    public static boolean scrollForward () {
        return scrollMainScrollable(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
    }

    public static boolean scrollBackward () {
        return scrollMainScrollable(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
    }

    public static List<CharSequence> collectContentDescriptions () {
        List<CharSequence> contentDescriptions = new ArrayList<>();
        List<AccessibilityNodeInfo> node = getAllNodesOnScreen();
        if (node == null) {
            return null;
        }
        for (int i = 0; i < node.size(); i++) {
            if (node.get(i).getContentDescription() != null) {
                contentDescriptions.add(node.get(i).getContentDescription());
            } else if (node.get(i).getText() != null) {
                contentDescriptions.add(node.get(i).getText());
            }
        }
        return contentDescriptions;
    }

    public static boolean checkDesInList (List < CharSequence > charSequences, String
    targetString){
        for (CharSequence charSequence : charSequences) {
            if (charSequence.toString().equals(targetString)) {
                return true;
            }
        }
        return false;
    }

    public static boolean findAndClickByTextDes (String textOrDes,boolean isEqual, boolean isToLower, boolean click, boolean longPress, int time){
        AccessibilityNodeInfo rootNood = null;
        for (int i = 0; i < time; i++) {
            rootNood = asblService.getRootInActiveWindow();
            if (rootNood != null) {
                List<AccessibilityNodeInfo> nodes = findAccessibilityNodeInfosByTextDes(rootNood, textOrDes, isEqual, isToLower);
                if (nodes != null && !nodes.isEmpty()) {
                    if (click) {
                        Rect bounds = new Rect();
                        int centerX = 0;
                        int centerY = 0;
                        for (int j = 0; j < nodes.size(); j++) {
                            try {
                                nodes.get(j).getBoundsInScreen(bounds);
                                centerX = (bounds.left + bounds.right) / 2;
                                centerY = (bounds.top + bounds.bottom) / 2;
                                if (centerX >= 0 && centerY >= 0) {
                                    do_click(centerX, centerY, longPress);
                                    break;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    try {
                        rootNood.recycle();
                    } catch (Exception e) {}
                    return true;
                }
            }
            delay(1000);
        }
        try {
            rootNood.recycle();
        } catch (Exception e) {}
        return false;
    }

    public static boolean clickHintText (String hintText,boolean match, boolean click, boolean longPress, int time){
        AccessibilityNodeInfo rootNood = null;
        for (int i = 0; i < time; i++) {
            rootNood = asblService.getRootInActiveWindow();
            if (rootNood != null) {
                List<AccessibilityNodeInfo> nodes = findHintText(rootNood, hintText, match);
                if (nodes != null && !nodes.isEmpty()) {
                    if (click) {
                        Rect bounds = new Rect();
                        int centerX = 0;
                        int centerY = 0;
                        for (int j = 0; j < nodes.size(); j++) {
                            try {
                                nodes.get(j).getBoundsInScreen(bounds);
                                centerX = (bounds.left + bounds.right) / 2;
                                centerY = (bounds.top + bounds.bottom) / 2;
                                if (centerX >= 0 && centerY >= 0) {
                                    do_click(centerX, centerY, longPress);
                                    break;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                    try {
                        rootNood.recycle();
                    } catch (Exception e) {}
                    return true;
                }
            }
            delay(1000);
        }
        try {
            rootNood.recycle();
        } catch (Exception e) {}
        return false;
    }
    public static void dumpTree(AccessibilityNodeInfo node, int depth) {
        if (node == null) return;
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) indent.append("  "); // Thụt lề cho dễ nhìn

        // In ra Logcat
        android.util.Log.d("ASBL_TREE", indent.toString() + "Class: " + node.getClassName() + " | Text: " + node.getText());

        // Đệ quy moi ruột từng đứa con
        for (int i = 0; i < node.getChildCount(); i++) {
            dumpTree(node.getChild(i), depth + 1);
        }
    }
    public static int findMultiTextDesWindow(int time, boolean isEqual, boolean isToLower, boolean click, boolean longPress, String... textOrDes) {
        for (int i = 0; i < time; i++) {
            // Lấy tất cả các lớp cửa sổ đang xếp chồng trên màn hình
            List<AccessibilityWindowInfo> windows = asblService.getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root != null) {
                        dumpTree(root, 0);
                        List<AccessibilityNodeInfo> nodes;
                        for (int j = 0; j < textOrDes.length; j++) {
                            // Đệ quy tìm text trên từng cửa sổ
                            nodes = findAccessibilityNodeInfosByTextDes(root, textOrDes[j], isEqual, isToLower);
                            if (nodes != null && !nodes.isEmpty()) {
                                if (click) {
                                    Rect bounds = new Rect();
                                    for (int k = 0; k < nodes.size(); k++) {
                                        try {
                                            nodes.get(k).getBoundsInScreen(bounds);
                                            int centerX = (bounds.left + bounds.right) / 2;
                                            int centerY = (bounds.top + bounds.bottom) / 2;
                                            if (centerX >= 0 && centerY >= 0) {
                                                do_click(centerX, centerY, longPress);
                                                break;
                                            }
                                        } catch (Exception e) {}
                                    }
                                }
                                try { root.recycle(); } catch (Exception e) {}
                                return j + 1; // Sếp trả về j+1 giống code cũ
                            }
                        }
                        try { root.recycle(); } catch (Exception e) {}
                    }
                }
            }
            delay(1000);
        }
        return 0;
    }
    public static List<AccessibilityNodeInfo> findHintText (AccessibilityNodeInfo root, String hintText,boolean match){
        if (root == null) {
            return null;
        }

        List<AccessibilityNodeInfo> subNodes = new ArrayList<>();

        boolean isCollected = false;
        if ((match && root.getHintText() != null && String.valueOf(root.getHintText()).equals(hintText)) ||
                (!match && root.getHintText() != null && root.getHintText().toString().contains(hintText))) {
            isCollected = true;
            subNodes.add(root);
        }

        if (root.getChildCount() < 1) {
            if (!isCollected) {
                try{
                    root.recycle();
                } catch (Exception e) {}
                return null;
            } else {
                return subNodes;
            }
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            List<AccessibilityNodeInfo> nodes = findHintText(child, hintText, match);
            if (nodes != null && !nodes.isEmpty()) {
                subNodes.addAll(nodes);
            }
        }

        return subNodes;
    }
    public static int findMultiTextDes ( int time, boolean isEqual, boolean isToLower, boolean click, boolean longPress, String...textOrDes){
        AccessibilityNodeInfo root = null;
        for (int i = 0; i < time; i++) {
            root = asblService.getRootInActiveWindow();
            if (root != null) {
                List<AccessibilityNodeInfo> nodes = new ArrayList<>();
                for (int j = 0; j < textOrDes.length; j++) {
                    nodes = findAccessibilityNodeInfosByTextDes(root, textOrDes[j], isEqual, isToLower);
                    if (nodes != null && !nodes.isEmpty()) {
                        if (click) {
                            Rect bounds = new Rect();
                            int centerX = 0;
                            int centerY = 0;
                            for (int k = 0; k < nodes.size(); k++) {
                                try {
                                    nodes.get(k).getBoundsInScreen(bounds);
                                    centerX = (bounds.left + bounds.right) / 2;
                                    centerY = (bounds.top + bounds.bottom) / 2;
                                    if (centerX >= 0 && centerY >= 0) {
                                        do_click(centerX, centerY, longPress);
                                        break;
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                        try{
                            root.recycle();
                        } catch (Exception e) {}
                        j = j + 1;
                        return j;
                    }
                }
            }
            delay(1000);
        }
        try{
            root.recycle();
        } catch (Exception e) {}
        return 0;
    }

    public static List<AccessibilityNodeInfo> findAccessibilityNodeInfosByTextDes (AccessibilityNodeInfo root, String textOrDes, boolean isEqual, boolean isToLower)
    {
        if (root == null) {
            return null;
        }

        List<AccessibilityNodeInfo> subNodes = new ArrayList<>();
        String rootText = "", rootContentDesctiption = "";
        if(root.getText() != null)
        {
            rootText = root.getText().toString();
            if(isToLower) {
                rootText = rootText.toLowerCase();
            }
        }
        if(root.getContentDescription() != null)
        {
            rootContentDesctiption = root.getContentDescription().toString();
            if(isToLower) {
                rootContentDesctiption = rootContentDesctiption.toLowerCase();
            }
        }

        boolean isCollected = false;
        if(root.isVisibleToUser()) {
            if (isEqual) {
                if (rootText.equals(textOrDes) || rootContentDesctiption.equals(textOrDes)) {
                    isCollected = true;
                    subNodes.add(root);
                }
            }
            else
            {
                if (rootText.length() > 1 && (rootText.contains(textOrDes) || rootContentDesctiption.contains(textOrDes)))
                {
                    isCollected = true;
                    subNodes.add(root);
                }
            }
        }

        if (root.getChildCount() < 1) {
            if (!isCollected) {
                try{
                    root.recycle();
                } catch (Exception e) {}
                return null;
            } else {
                return subNodes;
            }
        }

        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            List<AccessibilityNodeInfo> nodes = findAccessibilityNodeInfosByTextDes(child, textOrDes, isEqual, isToLower);
            if (nodes != null && !nodes.isEmpty()) {
                subNodes.addAll(nodes);
            }
        }

        return subNodes;
    }
    private static void sleep (long dur){
        try {
            Thread.sleep(dur);
        } catch (Exception ignore) { }
    }
}
