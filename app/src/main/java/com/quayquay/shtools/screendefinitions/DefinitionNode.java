package com.quayquay.shtools.screendefinitions;

import com.quayquay.shtools.extention.LOG;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;

public class DefinitionNode {
    public String className = null;
    public Boolean isSelected = null;
    public Boolean isChecked = null;
    public Boolean isClickable = null;
    public Boolean isEnable = null;
    public String text = null;
    public String contentDescription = null;
    public String keyword = null;
    public String hintText = null;

    enum COMPARE_RESULT {
        DIFF,
        MATCH,
        CONTAIN
    }

    private DefinitionNode(){}
    public DefinitionNode(JSONObject node) throws Exception {
        try {
            if (node.has("keyword")) keyword = node.getString("keyword");
            if (node.has("selected")) isSelected = node.getBoolean("selected");
            if (node.has("checked")) isChecked = node.getBoolean("checked");
            if (node.has("clickable")) isClickable = node.getBoolean("clickable");
            if (node.has("enable")) isEnable = node.getBoolean("enable");
            if (node.has("className")) className = node.getString("className");
            if (node.has("hintText")) hintText = node.getString("hintText");
            if (node.has("text")) text = node.getString("text").toLowerCase();
            if (node.has("contentDescription")) contentDescription = node.getString("contentDescription").toLowerCase();
        } catch (Exception e) {
            throw new Exception() {
                @Override
                public String getMessage(){
                    return "Definition node is NULL";
                }
            };
        }
    }

    COMPARE_RESULT compare(ScreenNode node) {
        return compare(node, false);
    }

    COMPARE_RESULT compare(ScreenNode node, boolean debug) {
        if(debug) {
            LOG.D("compare", "node: " + node.toString());
            LOG.D("compare", "this: " + this.toString());
        }
        try {
            if ((isSelected == null || node.isSelected == isSelected) &&
                    (isChecked == null || node.isChecked == isChecked) &&
                    (isClickable == null || node.isClickable == isClickable) &&
                    (isEnable == null || node.isEnable == isEnable) &&
                    (className == null || node.className.equals(className)) &&
                    (hintText == null || node.hintText.equals(hintText))) {
                if (((text == null || node.textLower.equals(text) || (text.contains("@") && diffSepcialString(node.textLower, text))) &&
                        (contentDescription == null || node.contentDescriptionLower.equals(contentDescription) || (contentDescription.contains("@") && diffSepcialString(node.contentDescriptionLower, contentDescription))))) {
                    return COMPARE_RESULT.MATCH;
                } else if ((text == null || node.textLower.contains(text)) &&
                        (contentDescription == null || node.contentDescriptionLower.contains(contentDescription))) {
                    return COMPARE_RESULT.CONTAIN;
                }
            }
        } catch (Exception e) {}
        return COMPARE_RESULT.DIFF;
    }

    public static boolean diffSepcialString(String str1, String str2) {
        if(str1 == null || str2 == null) return false;
        List<DiffMatchPatch.Diff> diffs = new DiffMatchPatch().diffMain(str1, str2);
        for (DiffMatchPatch.Diff diff : diffs) {
            if (diff.operation == DiffMatchPatch.Operation.DELETE) {
                str2 = str2.replaceFirst("@", Matcher.quoteReplacement(diff.text));
            }
        }
        return str2.equals(str1);
    }


        @Override
    public String toString() {
        return "DefinitionNode{" +
                "className='" + className + '\'' +
                ", isSelected=" + isSelected +
                ", isChecked=" + isChecked +
                ", isClickable=" + isClickable +
                ", isEnable=" + isEnable +
                ", text='" + text + '\'' +
                ", contentDescription='" + contentDescription + '\'' +
                ", keyword='" + keyword + '\'' +
                ", hintText='" + hintText + '\'' +
                '}';
    }
}
