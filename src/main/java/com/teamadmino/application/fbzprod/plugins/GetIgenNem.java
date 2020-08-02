package com.teamadmino.application.fbzprod.plugins;

import com.teamadmino.admino_backend.server.http.RequestContext;
import org.json.JSONObject;

public class GetIgenNem {

    public static void init_onload(RequestContext rc) throws Exception {
        rc.getCreateJSONObject(
            rc.getResponseScreen().getJSONArray("elements").getJSONObject(0), "style"
        ).put("width", String.format("%dpx", 500));

        rc.getScreenVars().put("args", rc.getArgs());
        rc.setValue("label1", rc.getArgs().optString("title", "?"));
        rc.setFocus("igen");
    }

    public static void igen_action(RequestContext rc) throws Exception {
        JSONObject returnValue = new JSONObject();
        returnValue.put("text", "Igen");
        rc.returnResult(returnValue);
    }

    public static void nem_action(RequestContext rc) throws Exception {
        JSONObject returnValue = new JSONObject();
        returnValue.put("text", "Nem");
        rc.returnResult(returnValue);
    }

}
