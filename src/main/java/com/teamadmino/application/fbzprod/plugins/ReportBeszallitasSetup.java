package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.*;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.createJob;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.getJodStatus;

import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.FieldDefinition;
import com.teamadmino.admino_backend.server.database.MemoryRecord;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.ServerMain;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import com.teamadmino.admino_backend.server.ui.ElementUtils;
import org.json.JSONArray;
import org.json.JSONObject;


public class ReportBeszallitasSetup {

    public static void init_onload(RequestContext rc) throws Exception {

        String tempTableName = RequestContext.getTempTableName("filterTableName");
        rc.getScreenVars().put("filterTableName", tempTableName);

        DatabaseSchemaDefinition databaseSchema = new DatabaseSchemaDefinition(
            tempTableName, "filters", "schema"
        );
        databaseSchema
            .addField("field", "Adatmező", "string", "string", 20)
            .addField("operator", "Feltétel", "string", "string", 5)
            .addField("value", "Érték", "string", "string", 30);
        DatabaseTable tempTable = new DatabaseTable("_test", databaseSchema);

        JSONObject settings = sessionVars.getOrDefault(rc.getSessionInfo().sid, new JSONObject());
        String group = settings.optString("bevetelGeneric_group");
        if (group.length() != 0) {
            rc.setValue("reportType", group);
            JSONArray filters = settings.getJSONArray("bevetelGeneric_filters");
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

        rc.setFocus("reportType");
    }

    public static void addfilter_action(RequestContext rc) throws Exception {
        JSONArray desc = new JSONArray().put("Description");
        for (String item : "Tulajdonos,Szállító,Időszak,Bizonylat száma,Bizonylat éve,Bizonylat jelleg".split(",")) {
            desc.put(item);
        }
        JSONArray data = new JSONArray().put(desc);
        JSONObject args = new JSONObject().put("data", data);

        rc.loadToPopupTest("_admino:generic/selectSimple", args);
    }

    public static void delfilter_action(RequestContext rc) throws Exception {
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        if (filterTable.recordCount == 0) {
            rc.snackErrorMessage("A filter tábla üres");
            return;
        }
        new Record(filterTable).delete((int) rc.getTableRowPos("filters"));
        rc.tableForceRefresh("filters");
    }

    public static void addfilter_action_onreturn(RequestContext rc) throws Exception {
        switch (rc.getReturnValue().getString("col0")) {
            case "Tulajdonos": {
                JSONArray kod = new JSONArray().put("id");
                JSONArray name = new JSONArray().put("name");
                DatabaseTable tulajdonos = tables.get("TULAJD");
                for (int i = 1; i <= tulajdonos.recordCount; i++) {
                    MemoryRecord memoryRecord = tulajdonos.getMemoryRecord(i, 1);
                    kod.put(memoryRecord.getASFormattedString("kod"));
                    name.put(memoryRecord.getASFormattedString("name"));
                }
                JSONArray data = new JSONArray();
                data.put(kod);
                data.put(name);
                JSONObject args = new JSONObject().put("data", data);
                rc.loadToPopupTest2("_admino:generic/selectSimple", args, "getTulajdonos");
                break;
            }
            case "Szállító": {
                JSONArray kod = new JSONArray().put("id");
                JSONArray name = new JSONArray().put("name");
                DatabaseTable szallito = tables.get("SZALLITO");
                for (int i = 1; i <= szallito.recordCount; i++) {
                    MemoryRecord memoryRecord = szallito.getMemoryRecord(i, 1);
                    kod.put(memoryRecord.getASFormattedString("kod"));
                    name.put(memoryRecord.getASFormattedString("name"));
                }
                JSONArray data = new JSONArray();
                data.put(kod);
                data.put(name);
                JSONObject args = new JSONObject().put("data", data);
                rc.loadToPopupTest2("_admino:generic/selectSimple", args, "getSzallito");
                break;
            }
            case "Időszak":
                rc.loadToPopupTest2("fbz_prod:common/getDateInterval", null, "getDateInterval");
                break;
            case "Bizonylat száma":
                rc.loadToPopupTest2("fbz_prod:common/getNumber", null, "getBizSzam");
                break;
            case "Bizonylat éve":
                rc.loadToPopupTest2("fbz_prod:common/getNumber", null, "getBizEv");
                break;
            case "Bizonylat jelleg":
                rc.loadToPopupTest2("fbz_prod:common/getBevetelJelleg", null, "getBizJelleg");
                break;


        }
        rc.closeSender();
    }

    public static void getBizSzam_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Bizonylat száma")
            .setField("operator", "==")
            .setField("value", rc.getReturnValue().getString("number"))
            .insert();

        rc
            .set("filters", "value", new JSONObject()
                .put("keys", ElementUtils.tablePositionLast)
                .put("cursorpos", 0))
            .tableForceRefresh("filters");
        rc.setFocus("filters");

    }

    public static void getBizJelleg_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Bizonylat jelleg")
            .setField("operator", "==")
            .setField("value", rc.getReturnValue().getString("text").toUpperCase())
            .insert();

        rc
            .set("filters", "value", new JSONObject()
                .put("keys", ElementUtils.tablePositionLast)
                .put("cursorpos", 0))
            .tableForceRefresh("filters");
        rc.setFocus("filters");

    }

    public static void getBizEv_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Bizonylat éve")
            .setField("operator", "==")
            .setField("value", rc.getReturnValue().getString("number"))
            .insert();

        rc
            .set("filters", "value", new JSONObject()
                .put("keys", ElementUtils.tablePositionLast)
                .put("cursorpos", 0))
            .tableForceRefresh("filters");
        rc.setFocus("filters");
    }

    public static void getDateInterval_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Időszak")
            .setField("operator", "==")
            .setField("value", rc.getReturnValue().getString("date1")
                               + " ~ " + rc.getReturnValue().getString("date2"))
            .insert();

        rc
            .set("filters", "value", new JSONObject()
                .put("keys", ElementUtils.tablePositionLast)
                .put("cursorpos", 0))
            .tableForceRefresh("filters");
        rc.setFocus("filters");
    }

    public static void getTulajdonos_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Tulajdonos")
            .setField("operator", "==")
            .setField("value", rc.getReturnValue().getString("col0"))
            .insert();

        rc
            .set("filters", "value", new JSONObject()
                .put("keys", ElementUtils.tablePositionLast)
                .put("cursorpos", 0))
            .tableForceRefresh("filters");
        rc.setFocus("filters");
    }

    public static void getSzallito_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        Record record = new Record(filterTable)
            .setField("field", "Szállító")
            .setField("operator", "==")
            .setField("value", rc.getReturnValue().getString("col0"));
        int i = record.insert();

        rc
            .set("filters", "value", new JSONObject()
                .put("keys", ElementUtils.tablePositionLast)
                .put("cursorpos", 0))
            .tableForceRefresh("filters");
        rc.setFocus("filters");
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
        settings.put("bevetelGeneric_group", rc.getValueString("reportType"));
        settings.put("bevetelGeneric_filters", filters);
        sessionVars.put(sid, settings);

        int jobId = createJob(new JobConfig("Bevételezés")
                                  .putParam("handler", "bevetelGenericV0")
                                  .putParam("group", rc.getValueString("reportType"))
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
                rc.jumpTo("fbz_prod:reports/common/tableview", args);
            } else {
                rc.snackWarningMessage("Error creating results");
            }
        }
    }

}
