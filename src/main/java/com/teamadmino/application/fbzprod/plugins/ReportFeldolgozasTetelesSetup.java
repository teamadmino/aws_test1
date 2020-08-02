package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.jobConfigs;
import static com.teamadmino.admino_backend.server.database.ServerMain.sessionVars;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.createJob;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.getJodStatus;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.genericAddFilterAction;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.genericAddFilterActionOnReturn;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.genericDelFilterAction;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.getAddFilterItem;

import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.FieldDefinition;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReportFeldolgozasTetelesSetup {

    public static void init_onload(RequestContext rc) throws Exception {

        String tempTableName = RequestContext.getTempTableName("filterTableName");
        rc.getScreenVars().put("filterTableName", tempTableName);

        DatabaseSchemaDefinition databaseSchema = new DatabaseSchemaDefinition(
            tempTableName, "filters", "schema"
        );
        databaseSchema
            .addField("field", "Adatmező", "string", "string", 40)
            .addField("operator", "Feltétel", "string", "string", 5)
            .addField("value", "Érték", "string", "string", 30);
        DatabaseTable tempTable = new DatabaseTable("_test", databaseSchema);

        JSONObject settings = sessionVars.getOrDefault(rc.getSessionInfo().sid, new JSONObject());
        String group = settings.optString("feldolgozasGeneric_group");

        JSONArray filters = settings.optJSONArray("feldolgozasGeneric_filters");
        if (filters != null) {
            for (int i = 1; i <= filters.length(); i++) {
                JSONObject filter = filters.getJSONObject(i - 1);
                new Record(tempTable)
                    .setField("field", filter.getString("field"))
                    .setField("operator", filter.getString("operator"))
                    .setField("value", filter.getString("value"))
                    .insert();
            }
        }

        TableViewDefinition tableViewDefinition = new TableViewDefinition(tempTable.name);
        tableViewDefinition
            .setHeaderContainerStyle(rc.getUiElement("filters").definition.optJSONObject("$headerContainerStyle"))
            .setHeaderStyle(rc.getUiElement("filters").definition.optJSONObject("$headerStyle"));

        for (FieldDefinition field : tempTable.fieldList) {
            tableViewDefinition.addTableField(field.name);
        }

        rc.setupTableView("filters", tableViewDefinition);
    }

    public static void addfilter_action(RequestContext rc) throws Exception {
        genericAddFilterAction(rc, "Tulajdonos",
                               "Időszak",
                               "Feldolgozó gép",
                               "Feldolgozás típus",
                               "Feldolgozás száma",
                               "Feldolgozás éve",
                               "Kiadott cikkszám volt benne",
                               "Bevételezett cikkszám volt benne",
                               "Van-e kiadott bála rögzítve?",
                               "Van-e bevételezett bála rögzítve?"
        );
    }

    public static void addfilter_action_onreturn(RequestContext rc) throws Exception {
        genericAddFilterActionOnReturn(rc);
    }

    public static void getGenericFilterItem_popup_onreturn(RequestContext rc) throws Exception {
        getAddFilterItem(rc);
    }

    public static void delfilter_action(RequestContext rc) throws Exception {
        genericDelFilterAction(rc);
    }

    public static void run_action(RequestContext rc) throws Exception {
        String sid = rc.getSessionInfo().sid;
        JSONObject settings = sessionVars.getOrDefault(sid, new JSONObject());
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        JSONArray filters = new JSONArray();
        for (int i = 1; i <= filterTable.recordCount; i++) {
            JSONObject filter = new JSONObject();
            filter.put("field", filterTable.getMemoryRecord(i).getAsString("field"));
            filter.put("operator", filterTable.getMemoryRecord(i).getAsString("operator"));
            filter.put("value", filterTable.getMemoryRecord(i).getAsString("value"));
            filters.put(filter);
        }
        settings.put("feldolgozasGeneric_filters", filters);

        sessionVars.put(sid, settings);

        int jobId = createJob(new JobConfig("Tételes feldolgozás lista")
                                  .putParam("handler", "tetelesFeldolgozasV0")
                                  .putParam("filters", tables.get(rc.getScreenVars().getString("filterTableName")))
        );
        rc.getScreenVars().put("jobId", jobId);
        rc.startBlockingPoll();
        rc.set("mainGroupId", "isLoading", true);
    }

    public static void run_poll(RequestContext rc) throws Exception {
        int jobId = rc.getScreenVars().getInt("jobId");
        String status = getJodStatus(jobId);
        if (status.startsWith("*")) {
            rc.setValue(rc.event + "@poll", 0);
            rc.set("mainGroupId", "isLoading", false);
            if (status.equals("*Success")) {
                if ((long) jobConfigs.get(jobId).config.get("resultTableSize") == 0) {
                    rc.snackWarningMessage("Nincs adat");
                    return;
                }
                JSONObject args = new JSONObject();
                args.put("screenTitle", jobConfigs.get(jobId).config.get("title"));
                args.put("resultTableName", jobConfigs.get(jobId).config.get("resultTableName"));
                JSONArray buttons = new JSONArray()
                    .put(new JSONObject()
                             .put("label", "Kijelölt sor: elszámolás")
                             .put("class", "com.teamadmino.application.fbzprod.plugins.ActionElszamolasPDF")
                             .put("method", "elszamolasSingle")
                    )
                    //.put(new JSONObject().put("label", "Kiadási bizonylat"))
                    //.put(new JSONObject().put("label", "Bevételi bizonylat"))
                    .put(new JSONObject()
                             .put("label", "Minden sor külön lapokra: elszámolás")
                             .put("class", "com.teamadmino.application.fbzprod.plugins.ActionElszamolasPDF")
                             .put("method", "elszamolasAll")
                    )
                    //.put(new JSONObject().put("label", "Kiadási bizonylat"))
                    //.put(new JSONObject().put("label", "Bevételi bizonylat")
                    ;
                args.put("customButtons", buttons);

                rc.jumpTo("fbz_prod:reports/common/tableview", args);
            } else {
                rc.snackWarningMessage("Error creating results");
            }
        }
    }

}
