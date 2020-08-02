package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ReportingSnapshot.getsnapshot;

import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.FieldDefinition;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.ReportingSnapshot;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import org.json.JSONObject;

import java.util.Arrays;

public class ReportsKeszletCikk {

    public static void init_onload(RequestContext rc) throws Exception {

        JSONObject agrs = rc.getArgs();
        byte tf;
        if (agrs.keySet().contains("tkod")) {
            tf = (byte) Integer.parseInt(agrs.getString("tkod"));
            rc
                .setValue("screenTitle",
                          "Készlet: "
                          + agrs.getString("tkod") + " / "
                          + agrs.getString("tnev").trim()
                )
                .set("full", "hidden", false)
                .set("tula", "hidden", false);
        } else {
            tf = 0;
            rc.setValue("screenTitle", "Aktuális teljes készlet cikkszámonként");
        }

        String tempTableName = RequestContext.getTempTableName("resultTableName");
        DatabaseSchemaDefinition databaseSchema = new DatabaseSchemaDefinition(
            tempTableName,
            rc.getValueString("screenTitle"), "schema"
        );
        databaseSchema
            .addField("ck", "Cikk", "int16", "zeropadded", 4)
            .addField("nm", "Megnevezés", "string", "string", 30)
            .addField("bl", "Bála darab", "int32", "number", 10)
            .addField("nt", "Nettó Kg", "int32", "fixed1", 10);
        DatabaseTable tempTable = new DatabaseTable("_test", databaseSchema);
        Record record = tempTable.newRecordObject();

        ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
            "KESZ?",
            "KESZ.bbru",
            "KESZ.btul", //todo (only if needed)
            "KESZ.bgon",
            "KESZ.bcik",
            "CIKK?",
            "CIKK#1",
            "CIKK.ckod",
            "CIKK!name",
            "GONGY.kg1"
        ));

        Integer aKe = snapshot.sizes.get("KESZ?");
        byte[] bg = (byte[]) snapshot.data.get("KESZ.bgon");
        byte[] bt = (byte[]) snapshot.data.get("KESZ.btul");
        char[] bb = (char[]) snapshot.data.get("KESZ.bbru");
        char[] bc = (char[]) snapshot.data.get("KESZ.bcik");

        Integer aCk = snapshot.sizes.get("CIKK?");
        int[] cikk1 = snapshot.indexes.get("CIKK#1");
        char[] ckod = (char[]) snapshot.data.get("CIKK.ckod");
        String[] name = (String[]) snapshot.data.get("CIKK!name");
        int[] ci = new int[10000];
        for (int i = 1; i <= aCk; i++) {
            ci[ckod[i]] = i;
        }

        int[] kg1 = (int[]) snapshot.data.get("GONGY.kg1");

        int[] bl = new int[10000];
        int[] nt = new int[10000];
        for (int i = 1; i <= aKe; i++) {
            if (tf == 0 || tf == bt[i]) {
                int p = ci[bc[i]];
                bl[p]++;
                nt[p] += bb[i] - kg1[bg[i]];
            }
        }

        for (int i = 0; i <= 9999; i++) {
            int p = ci[i];
            if (bl[p] == 0) {
                continue;
            }
            record
                .setField("ck", (char) i)
                .setField("nm", name[p])
                .setField("bl", bl[p])
                .setField("nt", nt[p])
                .insert();
        }

        TableViewDefinition tableViewDefinition = new TableViewDefinition(tempTableName);
        for (FieldDefinition field : tempTable.fieldList) {
            tableViewDefinition.addTableField(field.name);
        }
        rc.setupTableView("list", tableViewDefinition).setFocus("list");
        rc.getScreenVars().put("resultTableName", tempTableName);
    }

    public static void export_action(RequestContext rc) throws Exception {
        rc.stdExeclExportAction();
    }

    public static void export_poll(RequestContext rc) throws Exception {
        rc.stdExeclExportPoll();
    }

    public static void full_action(RequestContext rc) throws Exception {
        rc.jumpTo("fbz_prod:reports/keszlet/cikk", null);
    }
    public static void tula_action(RequestContext rc) throws Exception {
        rc.jumpTo("fbz_prod:reports/keszlet/tula", null);
    }

}
