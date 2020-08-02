package com.teamadmino.application.fbzprod.plugins;

import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.utils.DateUtils;
import org.json.JSONObject;

public class GetNumber {

    public static void init_onload(RequestContext rc) throws Exception {
        rc.getCreateJSONObject(
            rc.getResponseScreen().getJSONArray("elements").getJSONObject(0), "style"
        ).put("width", String.format("%dpx", 500));

        rc.getScreenVars().put("args", rc.getArgs());

        long min = rc.getArgs().optLong("min", 1);
        long max = rc.getArgs().optLong("max", Long.MAX_VALUE);

        long val = rc.getArgs().optLong("value", min - 1);
        if (val >= min && val != 0) {
            rc.setValue("number", String.valueOf(val));
        }
        int maxLength = Math.max(String.valueOf(min).length(), String.valueOf(max).length());
        rc.set("number", "maxLength", maxLength);
        rc.setValue("label1", rc.getArgs().optString("title", "Számérték"));
        rc.setFocus("number");
    }

    public static void numberOk_action(RequestContext rc) throws Exception {
        try {
            JSONObject returnValue = new JSONObject();
            String numberString = rc.getValueString("number").trim().replaceAll("^0+(?=.)", "");
            long i = Long.parseLong(numberString);
            long min = rc.getArgs().optLong("min", 1);
            long max = rc.getArgs().optLong("max", Long.MAX_VALUE);
            if (min <= i && i <= max) {
                returnValue.put("number", String.valueOf(i));
                rc.returnResult(returnValue);
                return;
            }
        } catch (Exception e) {
        }
        rc.snackErrorMessage("Nem megfelelő számérték");
    }

}
