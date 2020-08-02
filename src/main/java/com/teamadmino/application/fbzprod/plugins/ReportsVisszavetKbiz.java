package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ReportingSnapshot.getsnapshot;
import static com.teamadmino.admino_backend.server.database.ServerMain.jobConfigs;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;
import static com.teamadmino.admino_backend.server.http.RequestContext.*;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.createJob;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.getJodStatus;

import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.ReportingSnapshot;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ReportsVisszavetKbiz {

    public static void init_onload(RequestContext rc) throws Exception {
        String tempTableName = getTempTableName("resultTableName");
        DatabaseSchemaDefinition databaseSchema = new DatabaseSchemaDefinition(
            tempTableName,
            "Visszavételezéssel javított kiadási bizonylatok", "schema"
        );
        databaseSchema
            .addField("bz", "Bizonylat", "int32", "composite2z", 9)
            .addField("dt", "Időpont", "int32", "date", 10)
            .addField("bl", "Aktuális bála", "int32", "number", 3)
            .addField("bv", "Visszavételezve", "int32", "number", 3)
            .addField("sb", "Eredeti bála", "int32", "number", 3);
        DatabaseTable tempTable = new DatabaseTable("_test", databaseSchema);
        Record record = tempTable.newRecordObject();

        ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
            "BHISTORY?",
            "BHISTORY.bhtp",
            "BHISTORY.bhbz",
            "BHISTORY.bhbl"
        ));

        Integer aBh = snapshot.sizes.get("BHISTORY?");
        byte[] tp = (byte[]) snapshot.data.get("BHISTORY.bhtp");
        int[] bl = (int[]) snapshot.data.get("BHISTORY.bhbl");
        int[] bz = (int[]) snapshot.data.get("BHISTORY.bhbz");

        Map<Integer, Set<Integer>> kmb = new TreeMap<>();
        Map<Integer, Integer> kmd = new TreeMap<>();

        for (int i = 1; i <= aBh; i++) {
            if (tp[i] == 5) {
                Set<Integer> bs = kmb.computeIfAbsent(bz[i], s -> new TreeSet<Integer>());
                bs.add(bl[i]);
            }
        }

        DatabaseTable kbiz = tables.get("KBIZ");
        Record recordKbiz = new Record(kbiz);

        for (int k : kmb.keySet()) {
            int d, b;
            int bv = kmb.get(k).size();
            if (recordKbiz.setField("kod", k).readByPrimaryKeys()) {
                d = Integer.parseInt(recordKbiz.getAsString("date"));
                b = Integer.parseInt(recordKbiz.getAsString("blnm"));
            } else {
                d = 0;
                b = 0;
            }
            int sb = b + bv;
            record
                .setField("bz", k)
                .setField("dt", d)
                .setField("bl", b)
                .setField("bv", bv)
                .setField("sb", sb)
                .insert();
        }

        TableViewDefinition tableViewDefinition = new TableViewDefinition(tempTableName);
        tableViewDefinition.addTableField("bz");
        tableViewDefinition.addTableField("dt");
        tableViewDefinition.addTableField("bl");
        tableViewDefinition.addTableField("bv");
        tableViewDefinition.addTableField("sb");
        rc.setupTableView("list", tableViewDefinition).setFocus("list");
        rc.getScreenVars().put("resultTableName", tempTableName);
    }

    public static void actual_action(RequestContext rc) throws Exception {
        bizonylat(rc);
    }

    public static void original_action(RequestContext rc) throws Exception {
        bizonylat(rc);
    }

    public static void vissza_action(RequestContext rc) throws Exception {
        bizonylat(rc);
    }

    public static void actual_poll(RequestContext rc) throws Exception {
        bizonylat_poll(rc);
    }

    public static void original_poll(RequestContext rc) throws Exception {
        bizonylat_poll(rc);
    }

    public static void vissza_poll(RequestContext rc) throws Exception {
        bizonylat_poll(rc);
    }

    private static void bizonylat(RequestContext rc) throws Exception {
        int jobId = createJob(new JobConfig("Bizonylat")
                                  .putParam("handler", "kiadasiBizonylatVisszavet")
                                  .putParam("requestContext", rc)
        );
        rc.getScreenVars().put("jobId", jobId);
        rc.startBlockingPoll();
        rc.set("mainGroupId", "isLoading", true);
    }

    private static void bizonylat_poll(RequestContext rc) throws Exception {
        int jobId = rc.getScreenVars().getInt("jobId");
        String status = getJodStatus(jobId);
        if (status.startsWith("*")) {
            rc.setValue(rc.event + "@poll", 0);
            rc.set("mainGroupId", "isLoading", false);
            if (status.equals("*Success")) {
                JSONObject args = new JSONObject();
                args.put("screenTitle", jobConfigs.get(jobId).config.get("title"));
                args.put("resultTableName", jobConfigs.get(jobId).config.get("resultTableName"));
                rc.jumpTo("fbz_prod:reports/common/tableview", args);
            } else {
                rc.snackWarningMessage("Error exporting data");
            }
        }
    }

    public static void export_action(RequestContext rc) throws Exception {
        rc.stdExeclExportAction();
    }

    public static void export_poll(RequestContext rc) throws Exception {
        rc.stdExeclExportPoll();
    }

}
