package com.quayquay.shtools.screendefinitions;

import com.quayquay.shtools.extention.LOG;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefintionElement {
    private static final String TAG = "DefintionElement";
    public String screen_id = null;
    public String app_name = null;
    public Map<String, List<List<DefinitionNode>>> definitions = new HashMap<>();
    public Map<String, List<DefinitionNode>> keywords = new HashMap<>();


    private DefintionElement() {}

    public DefintionElement(JSONObject element) throws Exception {
        try {
            screen_id = element.getString("screen");
            app_name = element.getString("appname").toLowerCase();


            JSONObject _definitions = element.getJSONObject("definitons");
            Iterator<String> keys = _definitions.keys();
            while(keys.hasNext()) {
                String langeCode = keys.next();
                JSONArray groupsByLang = _definitions.getJSONArray(langeCode);
                List<List<DefinitionNode>> lGroupsByLang = new ArrayList<>();

                for (int i = 0; i < groupsByLang.length(); i++) {
                    JSONArray evidenceGrp = groupsByLang.getJSONArray(i);
                    List<DefinitionNode> lEvidenceGrp = new ArrayList<>();
                    for (int j = 0; j < evidenceGrp.length(); j++) {
                        JSONObject evidence = evidenceGrp.getJSONObject(j);
                        try {
                            lEvidenceGrp.add(new DefinitionNode(evidence));
                        } catch (Exception e) {
                            LOG.printStackTrace(TAG, e);
                        }
                    }
                    lGroupsByLang.add(lEvidenceGrp);
                }
                definitions.put(langeCode, lGroupsByLang);
            }

            if(element.has("keywords")) {
                JSONObject _keywords = element.getJSONObject("keywords");
                keys = _keywords.keys();
                while (keys.hasNext()) {
                    String langeCode = keys.next();
                    JSONArray keywordsByLang = _keywords.getJSONArray(langeCode);
                    List<DefinitionNode> lKeywordsByLang = new ArrayList<>();

                    for (int i = 0; i < keywordsByLang.length(); i++) {
                        JSONObject keyword = keywordsByLang.getJSONObject(i);
                        try {
                            lKeywordsByLang.add(new DefinitionNode(keyword));
                        } catch (Exception e) {
                            LOG.printStackTrace(TAG, e);
                        }
                    }
                    keywords.put(langeCode, lKeywordsByLang);
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String toString() {
        return "DefintionElement{" +
                "screen_id='" + screen_id + '\'' +
                ", app_name='" + app_name + '\'' +
                ", definitions=" + definitions +
                ", keywords=" + keywords +
                '}';
    }
}
