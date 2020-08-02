package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.IndexEngine.findIndexedInterval;
import static com.teamadmino.admino_backend.server.database.IndexEngine.findIndexedPosition;
import static com.teamadmino.admino_backend.server.database.ReportingSnapshot.getsnapshot;
import static com.teamadmino.admino_backend.server.http.RequestContext.getTempTableName;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.addJobLog;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.filterBetween;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.filterContainsCaseInsensitive;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.filterDivEQ;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.filterEQ;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.filterEndsWithCaseInsensitive;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.filterModuloEQ;
import static com.teamadmino.application.fbzprod.plugins.GenericFilterUtils.filterStartsWithCaseInsensitive;

import com.teamadmino.admino_backend.server.database.DatabaseSchemaDefinition;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.Record;
import com.teamadmino.admino_backend.server.database.ReportingSnapshot;
import com.teamadmino.admino_backend.server.processing.JobConfig;
import com.teamadmino.admino_backend.server.utils.DateUtils;
import com.teamadmino.admino_backend.server.utils.Interval;
import com.teamadmino.admino_backend.server.utils.StringVector;

import java.util.Arrays;

public class JobTetelesFeldolgozas {

    public static void tetelesFeldolgozasV0(int jobId, JobConfig jobConfig) throws Exception {

        //tulajd: kod,name,mn,1; feld: kod,date,tula,fbev,fkia; gongy: kg1; cikk: ckod,name,1; bbej: bcik,bzsz,bgon,bbru,src,4; bkia: bcik,bzsz,bgon,bbru,src,4; feldgep:name; feldn:name
        ReportingSnapshot snapshot = getsnapshot(Arrays.asList(
            "BBEJ#4",
            "BBEJ.bbru",
            "BBEJ.bcik",
            "BBEJ.bgon",
            "BBEJ.bzsz",
            "BBEJ.src",
            "BBEJ?",
            "BKIA#4",
            "BKIA.bbru",
            "BKIA.bcik",
            "BKIA.bgon",
            "BKIA.bzsz",
            "BKIA.src",
            "BKIA?",
            "CIKK#1",
            "CIKK.ckod",
            "CIKK.name",
            "CIKK?",
            "FELD.date",
            "FELD.fbev",
            "FELD.fkia",
            "FELD.kod",
            "FELD.tula",
            "FELD?",
            "FELDGEP.name",
            "FELDGEP?",
            "FELDN.name",
            "FELDN?",
            "GONGY.kg1",
            "GONGY?",
            "TULAJD#1",
            "TULAJD.kod",
            "TULAJD.mn",
            "TULAJD.name",
            "TULAJD?"
        ));

        int[] iBBEJ4 = snapshot.indexes.get("BBEJ#4");
        char[] BBEJ_bbru = (char[]) snapshot.data.get("BBEJ.bbru");
        char[] BBEJ_bcik = (char[]) snapshot.data.get("BBEJ.bcik");
        byte[] BBEJ_bgon = (byte[]) snapshot.data.get("BBEJ.bgon");
        int[] BBEJ_bzsz = (int[]) snapshot.data.get("BBEJ.bzsz");
        byte[] BBEJ_src = (byte[]) snapshot.data.get("BBEJ.src");
        int aBBEJ = snapshot.sizes.get("BBEJ?");
        int[] iBKIA4 = snapshot.indexes.get("BKIA#4");
        char[] BKIA_bbru = (char[]) snapshot.data.get("BKIA.bbru");
        char[] BKIA_bcik = (char[]) snapshot.data.get("BKIA.bcik");
        byte[] BKIA_bgon = (byte[]) snapshot.data.get("BKIA.bgon");
        int[] BKIA_bzsz = (int[]) snapshot.data.get("BKIA.bzsz");
        byte[] BKIA_src = (byte[]) snapshot.data.get("BKIA.src");
        int aBKIA = snapshot.sizes.get("BKIA?");
        int[] iCIKK1 = snapshot.indexes.get("CIKK#1");
        char[] CIKK_ckod = (char[]) snapshot.data.get("CIKK.ckod");
        StringVector CIKK_name = (StringVector) snapshot.data.get("CIKK.name");
        int aCIKK = snapshot.sizes.get("CIKK?");
        int[] FELD_date = (int[]) snapshot.data.get("FELD.date");
        int[] FELD_fbev = (int[]) snapshot.data.get("FELD.fbev");
        int[] FELD_fkia = (int[]) snapshot.data.get("FELD.fkia");
        int[] FELD_kod = (int[]) snapshot.data.get("FELD.kod");
        byte[] FELD_tula = (byte[]) snapshot.data.get("FELD.tula");
        int aFELD = snapshot.sizes.get("FELD?");
        StringVector FELDGEP_name = (StringVector) snapshot.data.get("FELDGEP.name");
        int aFELDGEP = snapshot.sizes.get("FELDGEP?");
        StringVector FELDN_name = (StringVector) snapshot.data.get("FELDN.name");
        int aFELDN = snapshot.sizes.get("FELDN?");
        int[] GONGY_kg1 = (int[]) snapshot.data.get("GONGY.kg1");
        int aGONGY = snapshot.sizes.get("GONGY?");
        int[] iTULAJD1 = snapshot.indexes.get("TULAJD#1");
        byte[] TULAJD_kod = (byte[]) snapshot.data.get("TULAJD.kod");
        StringVector TULAJD_mn = (StringVector) snapshot.data.get("TULAJD.mn");
        StringVector TULAJD_name = (StringVector) snapshot.data.get("TULAJD.name");
        int aTULAJD = snapshot.sizes.get("TULAJD?");

        addJobLog(jobId, "Data loaded", (byte) 0);

        String tempTableName = getTempTableName("resultTableName");
        DatabaseSchemaDefinition databaseSchema;
        DatabaseTable tempTable;
        Record record;
        String title;

        title = "Feldolgozások tételesen";
        databaseSchema = new DatabaseSchemaDefinition(tempTableName, title, "schema");
        databaseSchema
            .addField("fe", "Feldolgozás", "int32", "composite2z", 8)
            .addField("tu", "Tulajdonos", "string", "string", 2)
            .addField("dt", "Dátum", "int32", "date", 10)
            .addField("kb", "(Ki) Bála", "int32", "number", 6)
            .addField("kn", "(Ki) Nettó kg", "int32", "fixed1", 8)
            .addField("bb", "(Be) Bála", "int32", "number", 6)
            .addField("bn", "(Be) Nettó kg", "int32", "fixed1", 8);

        DatabaseTable filters = (DatabaseTable) jobConfig.config.get("filters");
        boolean[] feld = new boolean[aFELD + 1];
        Arrays.fill(feld, true);

        String details = "";
        for (int f = 1; f <= filters.recordCount; f++) {
            details += filters.getMemoryRecord(f).getAsString("field").trim()
                       + "\t"
                       + filters.getMemoryRecord(f).getAsString("value").trim()
                       + "\n";

            String filter = filters.getMemoryRecord(f).getAsString("field").trim();
            switch (filter) {
                case "Tulajdonos": {
                    byte tu = (byte) Integer.parseInt(filters.getMemoryRecord(f).getAsString("value").trim()
                                                          .replaceAll("^0+(?=.)", ""));
                    filterEQ(tu, FELD_tula, feld);
                    break;
                }
                case "Időszak": {
                    String dt = filters.getMemoryRecord(f).getAsString("value");
                    int d1 = DateUtils.date2int(dt);
                    int d2 = DateUtils.date2int(dt.substring(dt.indexOf('~') + 2));
                    filterBetween(d1, d2, FELD_date, feld);
                    break;
                }
                case "Feldolgozó gép": {
                    byte gep = (byte) Integer.parseInt(filters.getMemoryRecord(f).getAsString("value").trim());
                    for (int b = 1; b <= aFELD; b++) {
                        if (feld[b]) {
                            boolean match = false;
                            Interval bbi = findIndexedInterval(FELD_fbev[b], FELD_fbev[b], BBEJ_bzsz, iBBEJ4);
                            for (int ip = bbi.start; ip <= bbi.end; ip++) {
                                int i = iBBEJ4[ip];
                                if (BBEJ_src[i] == gep) {
                                    match = true;
                                    break;
                                }
                            }
                            if (!match) {
                                Interval kbi = findIndexedInterval(FELD_fkia[b], FELD_fkia[b], BKIA_bzsz, iBKIA4);
                                for (int ip = kbi.start; ip <= kbi.end; ip++) {
                                    int i = iBKIA4[ip];
                                    if (BKIA_src[i] == gep) {
                                        match = true;
                                        break;
                                    }
                                }
                            }
                            feld[b] = match;
                        }
                    }
                    break;
                }
                case "Feldolgozás típus": {
                    int type = Integer.parseInt(filters.getMemoryRecord(f).getAsString("value").trim());
                    filterDivEQ(type, 1000000, FELD_kod, feld);
                    break;
                }
                case "Feldolgozás száma": {
                    int bz = Integer.parseInt(filters.getMemoryRecord(f).getAsString("value").trim()
                                                  .replaceAll("^0+(?=.)", ""));
                    filterDivEQ(bz, 100, FELD_kod, feld);
                    break;
                }
                case "Feldolgozás éve": {
                    byte ev = (byte) Integer.parseInt(filters.getMemoryRecord(f).getAsString("value").trim());
                    filterModuloEQ(ev, 100, FELD_kod, feld);
                    break;
                }
                case "Kiadott cikkszám volt benne":
                    int kcik = Integer.parseInt(filters.getMemoryRecord(f).getAsString("value").trim());
                    for (int b = 1; b <= aFELD; b++) {
                        if (feld[b]) {
                            boolean match = false;
                            Interval kbi = findIndexedInterval(FELD_fkia[b], FELD_fkia[b], BKIA_bzsz, iBKIA4);
                            for (int ip = kbi.start; ip <= kbi.end; ip++) {
                                int i = iBKIA4[ip];
                                if (BKIA_bcik[i] == kcik) {
                                    match = true;
                                    break;
                                }
                            }
                            feld[b] = match;
                        }
                    }
                    break;
                case "Bevételezett cikkszám volt benne":
                    int bcik = Integer.parseInt(filters.getMemoryRecord(f).getAsString("value").trim());
                    for (int b = 1; b <= aFELD; b++) {
                        if (feld[b]) {
                            boolean match = false;
                            Interval bbi = findIndexedInterval(FELD_fbev[b], FELD_fbev[b], BBEJ_bzsz, iBBEJ4);
                            for (int ip = bbi.start; ip <= bbi.end; ip++) {
                                int i = iBBEJ4[ip];
                                if (BBEJ_bcik[i] == bcik) {
                                    match = true;
                                    break;
                                }
                            }
                            feld[b] = match;
                        }
                    }
                    break;
                case "Van-e kiadott bála rögzítve?": {
                    boolean igen = filters.getMemoryRecord(f).getAsString("value").trim().equals("Igen");
                    for (int b = 1; b <= aFELD; b++) {
                        if (feld[b]) {
                            Interval kbi = findIndexedInterval(FELD_fkia[b], FELD_fkia[b], BKIA_bzsz, iBKIA4);
                            if ((kbi.start <= kbi.end) != igen) {
                                feld[b] = false;
                            }
                        }
                    }
                    break;
                }
                case "Van-e bevételezett bála rögzítve?": {
                    boolean igen = filters.getMemoryRecord(f).getAsString("value").trim().equals("Igen");
                    for (int b = 1; b <= aFELD; b++) {
                        if (feld[b]) {
                            Interval bbi = findIndexedInterval(FELD_fbev[b], FELD_fbev[b], BBEJ_bzsz, iBBEJ4);
                            if ((bbi.start <= bbi.end) != igen) {
                                feld[b] = false;
                            }
                        }
                    }
                    break;
                }
                default:
                    throw new Exception("Unknown filter: " + filter);
            }
        }

        int recordCount = 0;
        for (int i = 1; i <= aFELD; i++) {
            if (feld[i]) {
                recordCount++;
            }
        }
        tempTable = new DatabaseTable("_test", databaseSchema, recordCount);
        record = tempTable.newRecordObject();
        tempTable.description = title;
        tempTable.details = details;
        addJobLog(jobId, "Filtering done", System.currentTimeMillis(), (byte) 0);

        for (int f = 1; f <= aFELD; f++) {
            if (feld[f]) {
                int kn = 0;
                int kb = 0;
                Interval kbi = findIndexedInterval(FELD_fkia[f], FELD_fkia[f], BKIA_bzsz, iBKIA4);
                for (int ip = kbi.start; ip <= kbi.end; ip++) {
                    int i = iBKIA4[ip];
                    kn += BKIA_bbru[i] - GONGY_kg1[BKIA_bgon[i]];
                    kb++;
                }
                int bn = 0;
                int bb = 0;
                Interval bbi = findIndexedInterval(FELD_fbev[f], FELD_fbev[f], BBEJ_bzsz, iBBEJ4);
                for (int ip = bbi.start; ip <= bbi.end; ip++) {
                    int i = iBBEJ4[ip];
                    bn += BBEJ_bbru[i] - GONGY_kg1[BBEJ_bgon[i]];
                    bb++;
                }
                record
                    .setField("fe", FELD_kod[f])
                    .setField("dt", FELD_date[f])
                    .setField("tu",
                              TULAJD_mn.select(iTULAJD1[findIndexedPosition((byte) (FELD_tula[f] & 0xFF), TULAJD_kod,
                                                                            iTULAJD1,
                                                                            true)]))
                    .setField("kb", kb)
                    .setField("kn", kn)
                    .setField("bb", bb)
                    .setField("bn", bn)
                    .insert();
            }
        }
        addJobLog(jobId, "Output generated", (byte) 99);

        //.addIndex("fe");
//        if (databaseSchema.indexes.length() > 0) {
//            String primary = databaseSchema.indexes.getJSONObject(0).getJSONArray("keys").getString(0);
//            for (int f = 1; f < databaseSchema.databaseFieldDefinitionList.size(); f++) {
//                databaseSchema.addIndex(databaseSchema.databaseFieldDefinitionList.get(f).name + "," + primary);
//            }
//        }
        String primary = databaseSchema.databaseFieldDefinitionList.get(0).name;
        tempTable.addIndex(primary);
        addJobLog(jobId, "Sorting col" + 1 + " done", (byte) 99);
        for (int f = 1; f < tempTable.fieldList.size(); f++) {
            tempTable.addIndex(tempTable.fieldList.get(f).name + "," + primary);
            addJobLog(jobId, "Sorting col" + (f + 1) + " done", (byte) 99);
        }

        addJobLog(jobId, "Sorting done", (byte) 99);

        jobConfig.config.put("title", title);
        jobConfig.config.put("resultTableName", tempTableName);
        jobConfig.config.put("resultTableSize", tempTable.recordCount);
    }

}
