package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.log;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.createJob;

import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import org.json.JSONObject;

public class ActionElszamolasPDF {

    public static void elszamolasSingle(RequestContext rc) throws Exception {
        int fe = 0;
        try {
            fe = rc.getValueJson("list").getJSONObject("keys").getInt("fe");
        } catch (Exception e) {
            rc.snackErrorMessage("A feldolgozás kijelölés nem azonosítható");
            return;
        }

        JobConfig jobConfig = new JobConfig("Elszámolás: " + String.format("%05d/%02d", fe / 100, fe % 100))
            .putParam("handler", "feldolgozasElszamolasSingleV0")
            .putParam("id", fe)
            .putParam("list", rc.getScreenVars().get("resultTableName"))
            .putParam("sessionInfo", rc.getSessionInfo());
        int jobId = createJob(jobConfig);
        rc.getScreenVars().put("jobId", jobId);

        JSONObject pollTimer = new JSONObject();
        pollTimer
            .put("id", rc.event + "@poll")
            .put("type", "timer")
            .put("frequency", 500)
            .put("value", -1)
            .put("action", new JSONObject()
                .put("type", "backend")
                .put("backendAction", rc.screenSessionId + "$" + rc.event + "@poll")
            );
        rc.addElement(pollTimer);
        rc.set("mainGroupId", "isLoading", true);
    }

    public static void elszamolasAll(RequestContext rc) throws Exception {

        if (tables.get(rc.getScreenVars().get("resultTableName")).recordCount > 10000) {
            rc.snackErrorMessage("Maximum 10000 feldolgozás elszámolás kötegelhetõ");
            return;
        }

        JobConfig jobConfig = new JobConfig("Elszámolás kötegelt")
            .putParam("handler", "feldolgozasElszamolasSingleV0")
            .putParam("list", rc.getScreenVars().get("resultTableName"))
            .putParam("sessionInfo", rc.getSessionInfo());
        int jobId = createJob(jobConfig);
        rc.getScreenVars().put("jobId", jobId);

        JSONObject pollTimer = new JSONObject();
        pollTimer
            .put("id", rc.event + "@poll")
            .put("type", "timer")
            .put("frequency", 500)
            .put("value", -1)
            .put("action", new JSONObject()
                .put("type", "backend")
                .put("backendAction", rc.screenSessionId + "$" + rc.event + "@poll")
            );
        rc.addElement(pollTimer);
        rc.set("mainGroupId", "isLoading", true);
    }

}
