package com.quayquay.shtools.extention;

import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import com.quayquay.shtools.screendefinitions.ScreenNode;

public class ASUtils {
    private static final String TAG = "ASUtils";
    public static boolean find(String componentID, List<ScreenNode> nodes_in_screen, boolean matchRequired) throws RemoteException {
        boolean retsult = false;
        ScreenNode targetItem = null;
        for (ScreenNode node : nodes_in_screen) {
            if (node.keyword != null && componentID.equals(node.keyword) && (node.match || !matchRequired)) {
                if (targetItem == null || node.match) {
                    targetItem = node;
                }
            }
        }

        if (targetItem != null) {
            retsult = true;
        }

        LOG.D("find ", componentID + ":" + (retsult ? "SUCCESS" : "FAIL"));
        return retsult;
    }

    public static boolean findAndClick(String componentID, List<ScreenNode> nodes_in_screen) throws RemoteException {
        boolean retsult = false;
        ScreenNode targetItem = null;
        for (ScreenNode node : nodes_in_screen) {
            if (node.keyword != null && componentID.equals(node.keyword)) {
                if (targetItem == null || node.match) {
                    targetItem = node;
                }
            }
        }

        if (targetItem != null) {
            int x = targetItem.x;
            int y = targetItem.y;
            int width = targetItem.width;
            int height = targetItem.height;
            ASInterface.instance().clickByPos(x + width / 2, y + height / 2, false);
            retsult = true;
        }

        LOG.D("findAndClick ", componentID + ":" + (retsult ? "SUCCESS" : "FAIL"));
        return retsult;
    }

    public static boolean findAndClickAll(String componentID, List<ScreenNode> nodes) {
        boolean retsult = false;
        try {
            for (ScreenNode node : nodes) {
                if (componentID.equals(node.keyword)) {
                    int x = node.x;
                    int y = node.y;
                    int width = node.y;
                    int height = node.y;
                    if (ASInterface.instance().clickByPos(x + width / 2, y + height / 2 , false)) {
                        delay(200);
                        retsult = true;
                    }
                }
            }
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
        }
        LOG.D(TAG, "findAndClick " + componentID + ": " + (retsult ? "SUCCESS" : "FAIL"));
        return retsult;
    }

    public static boolean findAndClickWithOffset(String componentID, List<ScreenNode> nodes, int xOffset, int yOffset) {
        boolean retsult = false;
        try {
            for (ScreenNode node : nodes) {
                if (componentID.equals(node.keyword)) {
                    int x = node.x;
                    int y = node.y;
                    if (ASInterface.instance().clickByPos(x + xOffset, y + yOffset, false)) {
                        delay(200);
                        retsult = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
        }
        LOG.D(TAG, "findAndClickWithOffset " + componentID + ": " + (retsult ? "SUCCESS" : "FAIL"));
        return retsult;
    }

    public static boolean findAndClickByTextOrDes(String textOrDes, List<ScreenNode> nodes, boolean matchRequired) {
        boolean retsult = false;
        try {
            for (ScreenNode node : nodes) {
                if ((matchRequired && (textOrDes.equals(node.text) || textOrDes.equals(node.contentDescription))) ||
                        (node.text.contains(textOrDes) || node.contentDescription.contains(textOrDes))) {
                    if (ASInterface.instance().clickByPos(node.x + node.width / 2, node.y + node.height / 2, false)) {
                        delay(200);
                        retsult = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
        }
        LOG.D(TAG, "findAndClickByTextOrDes " + textOrDes + ": "  + (retsult ? "SUCCESS" : "FAIL"));
        return retsult;
    }

    public static boolean findByTextOrDes(String textOrDes, List<ScreenNode> nodes) {
        boolean retsult = false;
        try {
            if(textOrDes != null) {
                for (ScreenNode node : nodes) {
                    if (textOrDes.equals(node.text) || textOrDes.equals(node.contentDescription)) {
                        retsult = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
        }
        LOG.D(TAG, "findByTextOrDes " + textOrDes + ": " + (retsult ? "SUCCESS" : "FAIL"));
        return retsult;
    }

    public static List<ScreenNode> getListComponentInfo(String idComponent, List<ScreenNode> nodes) {
        List<ScreenNode> retsult = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            ScreenNode item = nodes.get(i);
            if (idComponent.equals(item.keyword)) {
                retsult.add(item);
            }
        }
        LOG.D("getComponentInfoByText ", retsult == null ? "NULL" : retsult.toString());
        return retsult;
    }

    public static ScreenNode getComponentInfo(String idComponent, List<ScreenNode> nodes) {
        ScreenNode retsult = null;
        for (int i = 0; i < nodes.size(); i++) {
            ScreenNode item = nodes.get(i);
            if (idComponent.equals(item.keyword)) {
                retsult = item;
            }
        }
        LOG.D("getComponentInfoByText ", retsult == null ? "NULL" : retsult.toString());
        return retsult;
    }

    static public void delay(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception ex) {
        }
    }
}
