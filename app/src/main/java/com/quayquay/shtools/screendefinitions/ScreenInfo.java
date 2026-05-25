package com.quayquay.shtools.screendefinitions;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityNodeInfo;

import com.quayquay.shtools.extention.LOG;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScreenInfo implements Parcelable {
    static final String TAG = "ScreenInfo";
    public static List<DefintionElement> sDefinitions = null;
    public String tartget_app;
    public String screen_id = "SCREEN_UNKNOWN";
    public List<ScreenNode> screen_nodes = new ArrayList<>();

    public ScreenInfo(List<AccessibilityNodeInfo> nodes, String tartget_app) {
        this.tartget_app = tartget_app;
        for (AccessibilityNodeInfo node : nodes) {
            try {
                this.screen_nodes.add(new ScreenNode(node));
            } catch (Exception e) {
                LOG.printStackTrace(TAG, e);
            }
        }
        detectScreen();
    }

    protected ScreenInfo(Parcel in) {
        tartget_app = in.readString();
        screen_id = in.readString();
        in.readList(screen_nodes, screen_nodes.getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(tartget_app);
        dest.writeString(screen_id);
        dest.writeList(screen_nodes);
    }

    public static final Creator<ScreenInfo> CREATOR = new Creator<ScreenInfo>() {
        @Override
        public ScreenInfo createFromParcel(Parcel in) {
            return new ScreenInfo(in);
        }

        @Override
        public ScreenInfo[] newArray(int size) {
            return new ScreenInfo[size];
        }
    };

    void detectScreen() {
        try {
//            LOG.D(TAG, "sDefinitions: " + sDefinitions.size());
            if (sDefinitions == null) {
                LOG.E(TAG, "definitions is empty");
                return;
            }

            for (DefintionElement defintionElement : sDefinitions) {
//                LOG.D(TAG, "defintionElement.app_name: " + defintionElement.app_name);
                if (defintionElement.app_name.equals(tartget_app) ||
                        defintionElement.app_name.equalsIgnoreCase("common")) {
                    boolean debug = false;//defintionElement.screen_id.equals("SCREEN_AUTOFARMER_MODIFY_SYSTEM_SETTINGS");

//                    if(debug) LOG.D(TAG, "checking screen_Id: " + defintionElement.screen_id);

                    for (String langCode : defintionElement.definitions.keySet()) {
                        List<List<DefinitionNode>> groupsByLang = defintionElement.definitions.get(langCode);
//                        if(debug) LOG.D(TAG, "checking in: " + langCode);
//                        if(debug) LOG.D(TAG, "nodes_in_screen: " + nodes_in_screen.size());
                        for (List<DefinitionNode> evidenceGrp : groupsByLang) {
                            boolean detected = true;
                            for (DefinitionNode evidence : evidenceGrp) {
                                boolean existedPartern = false;
                                for (ScreenNode node : screen_nodes) {
                                    DefinitionNode.COMPARE_RESULT compare = evidence.compare(node);
//                                    if(debug) LOG.D(TAG, "compare " + compare + "-- node: " + node.text + "|" + node.contentDescription + " -- evidence: " + evidence.text + "|" + evidence.contentDescription);
                                    if (compare == DefinitionNode.COMPARE_RESULT.MATCH ||
                                            compare == DefinitionNode.COMPARE_RESULT.CONTAIN) {
                                        existedPartern = true;
                                        break;
                                    }
                                }
                                if (!existedPartern) {
                                    detected = false;
                                    break;
                                }
                            }

                            if (detected) {
                                screen_id = defintionElement.screen_id;
                                detectKeywords(defintionElement);
                                LOG.D(TAG, "detected: " + evidenceGrp);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
        }
    }

    void detectKeywords(DefintionElement defintionElement) {
        try {
            for (ScreenNode logNode : screen_nodes) {
                for (String langCode : defintionElement.keywords.keySet()) {
                    List<DefinitionNode> keywordByLange = defintionElement.keywords.get(langCode);
                    for (DefinitionNode keywordNode : keywordByLange) {
                        DefinitionNode.COMPARE_RESULT compare_result = keywordNode.compare(logNode);
                        if (compare_result == DefinitionNode.COMPARE_RESULT.MATCH ||
                                (logNode.keyword == null) &&
                                        (compare_result == DefinitionNode.COMPARE_RESULT.CONTAIN)) {
                            logNode.keyword = keywordNode.keyword;
                            logNode.match = compare_result == DefinitionNode.COMPARE_RESULT.MATCH;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.printStackTrace(TAG, e);
        }
    }

    public static boolean isDefinitionEmpty() {
        return sDefinitions == null ||
                sDefinitions.size() == 0;
    }

    public static void updateDefinitions(JSONArray defArr) {
        if(defArr == null || defArr.length() == 0) return;

        if(sDefinitions == null)
            sDefinitions = new ArrayList<>();
        else
            sDefinitions.clear();

        for (int i = 0; i < defArr.length(); i++) {
            try {
                JSONObject object = new JSONObject(defArr.getString(i));
                sDefinitions.add(new DefintionElement(object));
            } catch (Exception e) {
                LOG.printStackTrace(TAG, e);
            }
        }
    }

    @Override
    public String toString() {
        return "ScreenInfo{" +
                "tartget_app='" + tartget_app + '\'' +
                ", detected_screen_id='" + screen_id + '\'' +
                ", nodes_in_screen=" + Arrays.toString(screen_nodes.toArray()) +
                '}';
    }
}
