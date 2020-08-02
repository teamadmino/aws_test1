package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.utils.DateUtils.*;
import static com.teamadmino.admino_backend.server.utils.ExcelUtils.getTimeString;

import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.utils.DateUtils;
import org.json.JSONObject;

public class GetDateInterval {

    public static void init_onload(RequestContext rc) throws Exception {
        rc.getCreateJSONObject(
            rc.getResponseScreen().getJSONArray("elements").getJSONObject(0), "style"
        ).put("width", String.format("%dpx", 500));
        rc.setFocus("date1");
    }

    public static void dateIntervalOk_action(RequestContext rc) throws Exception {
        JSONObject returnValue = new JSONObject();
        String date1 = rc.getValueString("date1");
        String date2 = rc.getValueString("date2");
        int d1 = date2int(date1);
        int d2 = date2int(date2);
        if (d1 == 0 || d2 == 0 || d2 < d1) {
            rc.snackErrorMessage("Nem megfelelő dátum megjelölés");
            return;
        }
        returnValue.put("date1", date1.substring(0, 10));
        returnValue.put("date2", date2.substring(0, 10));
        rc.returnResult(returnValue);
    }

    public static void today_action(RequestContext rc) throws Exception {
        String dt = getTimeString().substring(0, 10);
        rc.setValue("date1", dt + "T00:00:00.000Z");
        rc.setValue("date2", dt + "T00:00:00.000Z");
        dateIntervalOk_action(rc);
    }

    public static void thisYear_action(RequestContext rc) throws Exception {
        String dt = getTimeString().substring(0, 4);
        rc.setValue("date1", dt + "-01-01T00:00:00.000Z");
        rc.setValue("date2", dt + "-12-31T00:00:00.000Z");
        dateIntervalOk_action(rc);
    }

    public static void thisMonth_action(RequestContext rc) throws Exception {
        String dt = getTimeString().substring(0, 7);
        rc.setValue("date1", dt + "-01T00:00:00.000Z");
        int d;
        for (d = 31; d > 29; d--) {
            if (date2int(dt + "-" + d) != 0) {
                break;
            }
        }
        rc.setValue("date2", dt + "-" + d + "T00:00:00.000Z");
        dateIntervalOk_action(rc);
    }

    public static void lastYear_action(RequestContext rc) throws Exception {
        String dt = String.valueOf(Integer.parseInt(getTimeString().substring(0, 4)) - 1);
        rc.setValue("date1", dt + "-01-01T00:00:00.000Z");
        rc.setValue("date2", dt + "-12-31T00:00:00.000Z");
        dateIntervalOk_action(rc);
    }

    public static void lastMonth_action(RequestContext rc) throws Exception {
        String dt = getTimeString();

        int y = Integer.parseInt(dt.substring(0, 4));
        int m = Integer.parseInt(dt.substring(5, 7));
        if (m == 1) {
            m = 12;
            y--;
        } else {
            m--;
        }
        dt = y + "-" + String.format("%02d", m);

        rc.setValue("date1", dt + "-01T00:00:00.000Z");
        int d;
        for (d = 31; d > 29; d--) {
            if (date2int(dt + "-" + d) != 0) {
                break;
            }
        }
        rc.setValue("date2", dt + "-" + d + "T00:00:00.000Z");
        dateIntervalOk_action(rc);
    }

    public static void yesterday_action(RequestContext rc) throws Exception {
        String dt = getTimeString();

        int y = Integer.parseInt(dt.substring(0, 4));
        int m = Integer.parseInt(dt.substring(5, 7));
        int d = Integer.parseInt(dt.substring(8, 10));

        if (d > 1) {
            d--;
        } else if (m == 1) {
            m = 12;
            d = 31;
            y--;
        } else {
            m--;
            for (d = 31; d > 29; d--) {
                if (date2int(dt + "-" + d) != 0) {
                    break;
                }
            }
        }
        dt = y + "-" + String.format("%02d", m) + "-" + String.format("%02d", d);
        rc.setValue("date1", dt + "T00:00:00.000Z");
        rc.setValue("date2", dt + "T00:00:00.000Z");
        dateIntervalOk_action(rc);
    }

}