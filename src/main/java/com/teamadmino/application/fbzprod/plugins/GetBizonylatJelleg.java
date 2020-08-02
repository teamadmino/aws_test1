package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.AnsiHun.string2byteArrayU;
import static com.teamadmino.admino_backend.server.database.ReportingSnapshot.getsnapshot;

import com.teamadmino.admino_backend.server.database.ReportingSnapshot;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.ui.UiElement;
import com.teamadmino.admino_backend.server.utils.StringVector;
import org.json.JSONObject;

import java.util.Arrays;

public class GetBizonylatJelleg {

    public static void init_onload(RequestContext rc) throws Exception {
        ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
            "FELDN.name"
        ));
        StringVector FELDN_name = (StringVector) snapshot.data.get("FELDN.name");
        for (int i = 0; i <= 9; i++) {
            rc.set(String.valueOf(i), "label", i + " - " + FELDN_name.getAsString(i + 1));
        }
    }

    public static void jellegOk_action(RequestContext rc) throws Exception {
        String selected = "";
        for (UiElement uiElement : rc.getUiElementMap().values()) {
            if (rc.getValueBoolean(uiElement.id)) {
                selected += uiElement.id;
            }
        }
        if (selected.length() == 0) {
            rc.snackErrorMessage("Legalább egy katagória kijelölése szükséges");
            return;
        }
        JSONObject returnValue = new JSONObject();
        rc.returnResult(new JSONObject().put("text", selected));
    }

}
