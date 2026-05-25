package com.quayquay.shtools.screendefinitions;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.Nullable;

import org.json.JSONObject;

public class ScreenNode implements Parcelable {
    public String className = null;
    public boolean isSelected = false;
    public boolean isChecked = false;
    public boolean isClickable = false;
    public boolean isEnable = false;
    public String text = null;
    public String textLower = null;
    public String contentDescription = null;
    public String contentDescriptionLower = null;
    public String keyword = null;
    public String hintText = null;
    public String hintTextLower = null;
    public boolean match = false;
    public int x = -1;
    public int y = -1;
    public int width = -1;
    public int height = -1;

    public ScreenNode(AccessibilityNodeInfo node) throws Exception {
        if(node == null)
            throw new Exception() {
                @Nullable
                @Override
                public String getMessage() {
                    return "AccessibilityNodeInfo is NULL";
                }
            };
        else {
            text = node.getText() == null? "null" : String.valueOf(node.getText());
            textLower = text.toLowerCase();
            contentDescription = node.getContentDescription() == null? "null" : String.valueOf(node.getContentDescription());
            contentDescriptionLower = contentDescription.toLowerCase();
            hintText = node.getHintText() == null? "null" : String.valueOf(node.getHintText());
            hintTextLower = hintText.toLowerCase();
            className = node.getClassName() == null? "null" : String.valueOf(node.getClassName());
            isSelected = node.isSelected();
            isChecked = node.isChecked();
            isClickable = node.isClickable();
            isEnable = node.isEnabled();

            Rect tmp = new Rect();
            node.getBoundsInScreen(tmp);
            x = tmp.left;
            y = tmp.top;
            width = tmp.width();
            height = tmp.height();
        }
    }

    protected ScreenNode(Parcel in) {
        className = in.readString();
        isSelected = in.readByte() != 0;
        isChecked = in.readByte() != 0;
        isClickable = in.readByte() != 0;
        isEnable = in.readByte() != 0;
        text = in.readString();
        textLower = in.readString();
        contentDescription = in.readString();
        contentDescriptionLower = in.readString();
        keyword = in.readString();
        hintText = in.readString();
        hintTextLower = in.readString();
        match = in.readByte() != 0;
        x = in.readInt();
        y = in.readInt();
        width = in.readInt();
        height = in.readInt();
    }

    public static final Creator<ScreenNode> CREATOR = new Creator<ScreenNode>() {
        @Override
        public ScreenNode createFromParcel(Parcel in) {
            return new ScreenNode(in);
        }

        @Override
        public ScreenNode[] newArray(int size) {
            return new ScreenNode[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(className);
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeByte((byte) (isChecked ? 1 : 0));
        dest.writeByte((byte) (isClickable ? 1 : 0));
        dest.writeByte((byte) (isEnable ? 1 : 0));
        dest.writeString(text);
        dest.writeString(textLower);
        dest.writeString(contentDescription);
        dest.writeString(contentDescriptionLower);
        dest.writeString(keyword);
        dest.writeString(hintText);
        dest.writeString(hintTextLower);
        dest.writeByte((byte) (match ? 1 : 0));
        dest.writeInt(x);
        dest.writeInt(y);
        dest.writeInt(width);
        dest.writeInt(height);
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject().put("className", className)
                    .put("text", text)
                    .put("contentDescription", contentDescription)
                    .put("hintText", hintText)
                    .put("selected", isSelected)
                    .put("checked", isChecked)
                    .put("clickable", isClickable)
                    .put("enable", isEnable)
                    .put("match", match)
                    .put("x", x)
                    .put("y", y)
                    .put("width", width)
                    .put("height", height);
            if(keyword != null) json.put("keyword", keyword);
            return json;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public String toString() {
        return "ScreenNode{" +
                "className='" + className + '\'' +
                ", isSelected=" + isSelected +
                ", isChecked=" + isChecked +
                ", isClickable=" + isClickable +
                ", isEnable=" + isEnable +
                ", text='" + text + '\'' +
                ", contentDescription='" + contentDescription + '\'' +
                ", keyword='" + keyword + '\'' +
                ", hintText='" + hintText + '\'' +
                ", match=" + match +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
