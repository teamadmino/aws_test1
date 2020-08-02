package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.jobConfigs;
import static com.teamadmino.admino_backend.server.database.ServerMain.sessionVars;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.createJob;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.getJodStatus;

import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.FieldDefinition;
import com.teamadmino.admino_backend.server.database.MemoryRecord;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import com.teamadmino.admino_backend.server.ui.ElementUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ReportFeldolgozasElszamolasSetup {

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
        if (group.length() != 0) {
            //rc.setValue("reportType", group);
            JSONArray filters = settings.getJSONArray("feldolgozasGeneric_filters");
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

        //rc.setFocus("reportType");
    }

    public static void addfilter_action(RequestContext rc) throws Exception {
        JSONArray desc = new JSONArray().put("Description");
        for (String item : "Tulajdonos,Időszak,Feldolgozó gép,Feldolgozás típus,Feldolgozás száma,Feldolgozás éve,Kiadott cikkszám volt benne,Bevételezett cikkszám volt benne"
            .split(",")) {
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
            case "Feldolgozó gép": {
                JSONArray kod = new JSONArray().put("id");
                JSONArray name = new JSONArray().put("name");
                DatabaseTable feldgep = tables.get("FELDGEP");
                for (int i = 1; i <= feldgep.recordCount; i++) {
                    MemoryRecord memoryRecord = feldgep.getMemoryRecord(i);
                    kod.put(String.format("%02d", i));
                    name.put(memoryRecord.getASFormattedString("name"));
                }
                JSONArray data = new JSONArray();
                data.put(kod);
                data.put(name);
                JSONObject args = new JSONObject().put("data", data);
                rc.loadToPopupTest2("_admino:generic/selectSimple", args, "getFeldgep");
                break;
            }
            case "Feldolgozás típus": {
                JSONArray kod = new JSONArray().put("id");
                JSONArray name = new JSONArray().put("name");
                DatabaseTable feldn = tables.get("FELDN");
                for (int i = 1; i <= feldn.recordCount; i++) {
                    MemoryRecord memoryRecord = feldn.getMemoryRecord(i);
                    kod.put(String.valueOf(i - 1));
                    name.put(memoryRecord.getASFormattedString("name"));
                }
                JSONArray data = new JSONArray();
                data.put(kod);
                data.put(name);
                JSONObject args = new JSONObject().put("data", data);
                rc.loadToPopupTest2("_admino:generic/selectSimple", args, "getFeldn");
                break;
            }
            case "Időszak":
                rc.loadToPopupTest2("fbz_prod:common/getDateInterval", null, "getDateInterval");
                break;
            case "Feldolgozás száma":
                rc.loadToPopupTest2("fbz_prod:common/getNumber", null, "getFeldSzam");
                break;
            case "Feldolgozás éve":
                rc.loadToPopupTest2("fbz_prod:common/getNumber", null, "getFeldEv");
                break;
            case "Kiadott cikkszám volt benne":
                rc.loadToPopupTest2("fbz_prod:common/getNumber", null, "getKcik");
                break;
            case "Bevételezett cikkszám volt benne":
                rc.loadToPopupTest2("fbz_prod:common/getNumber", null, "getBcik");
                break;
            default:
                rc.snackWarningMessage("Ez a filter opció még fejlesztés alatt van");

        }
        rc.closeSender();
    }

    public static void getFeldSzam_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Feldolgozás száma")
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

    public static void getBcik_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Bevételezett cikkszám volt benne")
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

    public static void getKcik_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Kiadott cikkszám volt benne")
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

    public static void getFeldEv_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Feldolgozás éve")
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

    public static void getFeldn_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Feldolgozás típus")
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

    public static void getFeldgep_popup_onreturn(RequestContext rc) throws Exception {
        rc.closeSender();
        DatabaseTable filterTable = tables.get(rc.getScreenVars().getString("filterTableName"));
        new Record(filterTable)
            .setField("field", "Feldolgozó gép")
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

        if (settings.optString("feldolgozasGeneric_group").length() == 0) {
            settings.put("feldolgozasGeneric_group", "cikk");
        }

        settings.put("feldolgozasGeneric_filters", filters);
        sessionVars.put(sid, settings);

        int jobIdK = createJob(new JobConfig("Feldolgozás Elszámolás Kiadás")
                                   .putParam("handler", "feldolgozasKiadasGenericV0")
                                   .putParam("group", "cikk")
                                   .putParam("filters", tables.get(rc.getScreenVars().getString("filterTableName")))
        );
        int jobIdB = createJob(new JobConfig("Feldolgozás Elszámolás Bevétel")
                                   .putParam("handler", "feldolgozasBevetelGenericV0")
                                   .putParam("group", "cikk")
                                   .putParam("filters", tables.get(rc.getScreenVars().getString("filterTableName")))
        );

        rc.getScreenVars().put("jobIdK", jobIdK);
        rc.getScreenVars().put("jobIdB", jobIdB);

        rc.startBlockingPoll();
        rc.set("mainGroupId", "isLoading", true);
    }

    public static void run_poll(RequestContext rc) throws Exception {
        int jobIdK = rc.getScreenVars().getInt("jobIdK");
        String statusK = getJodStatus(jobIdK);
        int jobIdB = rc.getScreenVars().getInt("jobIdB");
        String statusB = getJodStatus(jobIdB);

        if (statusK.startsWith("*") && statusB.startsWith("*")) {
            rc.setValue(rc.event + "@poll", 0);
            rc.set("mainGroupId", "isLoading", false);
            if (statusK.equals("*Success")) {
                if ((long) jobConfigs.get(jobIdK).config.get("resultTableSize") == 0 &&
                    (long) jobConfigs.get(jobIdB).config.get("resultTableSize") == 0) {
                    rc.snackWarningMessage("Nincs adat");
                    return;
                }
                JSONObject args = new JSONObject();
                args.put("resultTableNameK", jobConfigs.get(jobIdK).config.get("resultTableName"));
                args.put("resultTableNameB", jobConfigs.get(jobIdB).config.get("resultTableName"));
                rc.jumpTo("fbz_prod:reports/common/tableViewElszamolas", args);
            } else {
                rc.snackWarningMessage("Error creating results");
            }
        }
    }

}
