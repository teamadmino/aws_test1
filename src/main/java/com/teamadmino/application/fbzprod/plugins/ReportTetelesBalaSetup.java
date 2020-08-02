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
import com.teamadmino.admino_backend.server.database.MemoryRecord;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import com.teamadmino.admino_backend.server.ui.ElementUtils;
import com.teamadmino.admino_backend.server.ui.UiElement;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReportTetelesBalaSetup {

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

        JSONArray filters = settings.optJSONArray("tetelesKeszletBala_filters");
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

        JSONArray columns = settings.optJSONArray("tetelesKeszletBala_columns");
        if (columns != null) {
            for (int i = 0; i < columns.length(); i++) {
                rc.set(columns.getString(i), "value", true);
            }
        } else {
            rc.set("tetl", "value", true);
            rc.set("bcik", "value", true);
            rc.set("_cikkname", "value", true);
            rc.set("_bnet", "value", true);
        }

        TableViewDefinition tableViewDefinition = new TableViewDefinition(tempTable.name);
        tableViewDefinition
            .setHeaderContainerStyle(rc.getUiElement("filters").definition.optJSONObject("$headerContainerStyle"))
            .setHeaderStyle(rc.getUiElement("filters").definition.optJSONObject("$headerStyle"));

        for (FieldDefinition field : tempTable.fieldList) {
            tableViewDefinition.addTableField(field.name);
        }

        JSONArray columnGroup = rc.uiElementMap.get("columngroup").definition.getJSONArray("elements");
        JSONArray columnIds = new JSONArray();
        for (int i = 0; i < columnGroup.length(); i++) {
            if (columnGroup.getJSONObject(i).optString("type", "").equals("checkbox")) {
                columnIds.put(columnGroup.getJSONObject(i).getString("id"));
            }
        }
        rc.getTabVars().put("columnIds", columnIds);
        rc.setupTableView("filters", tableViewDefinition);
    }

    public static void all_action(RequestContext rc) throws Exception {
        for (UiElement uiElement : rc.getUiElementMap().values()) {
            if (uiElement.type.equals("checkbox")) {
                rc.set(uiElement.id, "value", true);
            }
        }
    }

    public static void bala_action(RequestContext rc) throws Exception {
        for (UiElement uiElement : rc.getUiElementMap().values()) {
            if (uiElement.type.equals("checkbox")) {
                if (!uiElement.id.equals("bkod")) {
                    rc.set(uiElement.id, "value", false);
                }
            }
        }
    }


    public static void addfilter_action(RequestContext rc) throws Exception {
        genericAddFilterAction(rc, "Tulajdonos",
                               "Szállító",
                               "Cikkszám",
                               "Bálaszám év",
                               "Tételszám kezdete",
                               "Tételszám vége",
                               "Tételszám tartalmazza");
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
        settings.put("tetelesKeszletBala_filters", filters);

        JSONArray columnIds = rc.getTabVars().getJSONArray("columnIds");
        JSONArray columns = new JSONArray();
        for (int i = 0; i < columnIds.length(); i++) {
            String id = columnIds.getString(i);
            if (id.equals("bkod") || rc.getValueBoolean(id)) {
                columns.put(id);
            }
        }
        settings.put("tetelesKeszletBala_columns", columns);

        sessionVars.put(sid, settings);

        int jobId = createJob(new JobConfig("Tételes bálasoros készlet")
                                  .putParam("handler", "tetelesKeszletBalaV0")
                                  .putParam("columns", columns)
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
                args.put("noPdf", true);
                args.put("resultTableName", jobConfigs.get(jobId).config.get("resultTableName"));
                rc.jumpTo("fbz_prod:reports/common/tableview", args);
            } else {
                rc.snackWarningMessage("Error creating results");
            }
        }
    }

}
