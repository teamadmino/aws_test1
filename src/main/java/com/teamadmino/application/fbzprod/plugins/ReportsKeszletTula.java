package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ReportingSnapshot.getsnapshot;
import static com.teamadmino.admino_backend.server.database.ServerMain.log;

import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.FieldDefinition;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.ReportingSnapshot;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import org.json.JSONObject;

import java.util.Arrays;

public class ReportsKeszletTula {

    public static void init_onload(RequestContext rc) throws Exception {
        String tempTableName = RequestContext.getTempTableName("resultTableName");
        DatabaseSchemaDefinition databaseSchema = new DatabaseSchemaDefinition(
            tempTableName,
            rc.getValueString("screenTitle"), "schema"
        );
        databaseSchema
            .addField("tu", "Kódszám", "int8", "zeropadded", 3)
            .addField("tn", "Megnevezés", "string", "string", 30)
            .addField("bl", "Bála darab", "int32", "number", 10)
            .addField("nt", "Nettó Kg", "int32", "fixed1", 10);
        DatabaseTable tempTable = new DatabaseTable("_test", databaseSchema);
        Record record = tempTable.newRecordObject();

        ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
            "KESZ?",
            "KESZ.bbru",
            "KESZ.bgon",
            "KESZ.btul",
            "TULAJD?",
            "TULAJD#1",
            "TULAJD.kod",
            "TULAJD!name",
            "GONGY.kg1"
        ));

        Integer aKe = snapshot.sizes.get("KESZ?");
        byte[] bg = (byte[]) snapshot.data.get("KESZ.bgon");
        char[] bb = (char[]) snapshot.data.get("KESZ.bbru");
        byte[] bt = (byte[]) snapshot.data.get("KESZ.btul");

        Integer aTu = snapshot.sizes.get("TULAJD?");
        int[] tula1 = snapshot.indexes.get("TULAJD#1");
        byte[] tkod = (byte[]) snapshot.data.get("TULAJD.kod");
        String[] name = (String[]) snapshot.data.get("TULAJD!name");
        int[] ti = new int[256];
        for (int i = 1; i <= aTu; i++) {
            ti[tkod[i] & 0xFF] = i;
        }

        int[] kg1 = (int[]) snapshot.data.get("GONGY.kg1");

        int[] bl = new int[10000];
        int[] nt = new int[10000];
        for (int i = 1; i <= aKe; i++) {
            int p = ti[bt[i] & 0xFF];
            bl[p]++;
            nt[p] += bb[i] - kg1[bg[i]];
        }

        for (int i = 0; i < 256; i++) {
            int p = ti[i];
            if (bl[p] == 0) {
                continue;
            }
            record
                .setField("tu", (byte) i)
                .setField("tn", name[p])
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

    public static void cikkek_action(RequestContext rc) throws Exception {
        JSONObject args = new JSONObject();
        args.put("tkod",rc.getValueJson("list").getJSONObject("keys").getString("$1"));
        args.put("tnev",rc.getValueJson("list").getJSONObject("keys").getString("$2"));
        rc.jumpTo("fbz_prod:reports/keszlet/cikk", args);
    }

    public static void export_action(RequestContext rc) throws Exception {
        rc.stdExeclExportAction();
    }

    public static void export_poll(RequestContext rc) throws Exception {
        rc.stdExeclExportPoll();
    }

}
