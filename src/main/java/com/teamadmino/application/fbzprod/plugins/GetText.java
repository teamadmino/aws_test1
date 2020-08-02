package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.AnsiHun.string2byteArrayU;

import com.teamadmino.admino_backend.server.http.RequestContext;
import org.json.JSONObject;

public class GetText {

    public static void init_onload(RequestContext rc) throws Exception {
        rc.getCreateJSONObject(
            rc.getResponseScreen().getJSONArray("elements").getJSONObject(0), "style"
        ).put("width", String.format("%dpx", 500));
        rc.setFocus("text");
    }

    public static void numberOk_action(RequestContext rc) throws Exception {
        JSONObject returnValue = new JSONObject();
        String text = rc.getValueString("text").trim();
        if (text.length() > 0) {
            try {
                string2byteArrayU(text);
            } catch (Exception e) {
                rc.snackErrorMessage("Nem megfelelő szöveg (speciális karakterek)");
                return;
            }
            returnValue.put("text", text);
            rc.returnResult(returnValue);
            return;
        }
        rc.snackErrorMessage("Nem megfelelő szöveg (üres)");
    }

}
