package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.backendService;
import static com.teamadmino.admino_backend.server.database.ServerMain.getNextTempId;
import static com.teamadmino.admino_backend.server.database.ServerMain.jobConfigs;
import static com.teamadmino.admino_backend.server.database.ServerMain.log;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;
import static com.teamadmino.admino_backend.server.database.ServerMain.workdir;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.getJobProgress;
import static com.teamadmino.admino_backend.server.processing.JobProcessor.getJodStatus;

import com.teamadmino.admino_backend.server.database.CellContext;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.FieldDefinition;
import com.teamadmino.admino_backend.server.database.MemoryRecord;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.ui.ElementUtils;
import com.teamadmino.admino_backend.server.ui.UiElement;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;

public class ReportsCommonTableview {

    public static void init_onload(RequestContext rc) throws Exception {
        JSONObject args = rc.getArgs();
        String tempTableName = args.getString("resultTableName");
        TableViewDefinition tableViewDefinition = new TableViewDefinition(tempTableName);
        DatabaseTable tempTable = tables.get(tempTableName);
        rc.getScreenVars().put("resultTableName", tempTableName);

        for (FieldDefinition field : tempTable.fieldList) {
            tableViewDefinition.addTableField(field.name);
        }

        if (args.optBoolean("noPdf", false)) {
            rc.set("exportPdf", "hidden", true);
        }

        JSONArray buttons = args.optJSONArray("customButtons");
        if (buttons != null) {
            JSONArray group = rc.getUiElement("customButtons").definition.getJSONArray("elements");
            for (int i = 0; i < buttons.length(); i++) {
                JSONObject button = buttons.getJSONObject(i);
                JSONObject element = new JSONObject();
                element.put("label", button.getString("label"));
                element.put("id", "custom_" + i);
                element.put("type", "button");
                element.put("color", "accent");
                element.put("action", new JSONObject()
                    .put("type", "backend")
                    .put("backendAction", rc.screenId + "$customButton@action")
                    .put("isBlocking", true)
                    .put("clear", true));

                element.put("style", new JSONObject().put("margin-right", "15px"));
                group.put(element);
            }
            rc.set("customButtons", "hidden", false);
            rc.getScreenVars().put("customButtons", buttons);
        }

        rc
            .setValue("screenTitle", args.getString("screenTitle"))
            .setupTableView("list", tableViewDefinition).setFocus("list");

        JSONArray columns = rc.getUiElement("list").definition.getJSONArray("columns");
        JSONArray headers = new JSONArray();
        JSONArray orders = new JSONArray();
        JSONArray types = new JSONArray();
        for (int i = 0; i < columns.length(); i++) {
            headers.put(columns.getJSONObject(i).getString("description"));
        }

        if (tempTable.indexCount > 0) {
            for (int i = 0; i < tempTable.fieldList.size(); i++) {
                String order;
                String type;
                switch (tempTable.fieldList.get(i).textFormat) {
                    case TEXT_FORMAT_NUMBER:
                    case TEXT_FORMAT_FIXED1:
                    case TEXT_FORMAT_FIXED2:
                    case TEXT_FORMAT_FIXED3:
                    case TEXT_FORMAT_FIXED4:
                    case TEXT_FORMAT_SPACEPADDED:
                    case TEXT_FORMAT_INTEGER:
                    case TEXT_FORMAT_DEC1:
                    case TEXT_FORMAT_DEC2:
                    case TEXT_FORMAT_DEC3:
                    case TEXT_FORMAT_DEC4:
                    case TEXT_FORMAT_DEC5:
                    case TEXT_FORMAT_DEC6:
                    case TEXT_FORMAT_DEC7:
                    case TEXT_FORMAT_DEC8:
                    case TEXT_FORMAT_DEC9:
                    case TEXT_FORMAT_NUMBER_0P:
                    case TEXT_FORMAT_NUMBER_X:
                    case TEXT_FORMAT_FIXED1_X:
                    case TEXT_FORMAT_FIXED1_0P:
                    case TEXT_FORMAT_FIXED2_X:
                        type = "num";
                        order = "desc";
                        break;
                    case TEXT_FORMAT_TIME:
                    case TEXT_FORMAT_DATE:
                    case TEXT_FORMAT_COMPOSITE1:
                    case TEXT_FORMAT_COMPOSITE2:
                    case TEXT_FORMAT_COMPOSITE2Z:
                    case TEXT_FORMAT_ZEROPADDED:
                    case TEXT_FORMAT_COMPOSITE2Z_0P:
                    case TEXT_FORMAT_COMPOSITE2Z_X:
                    case TEXT_FORMAT_ZEROPADDED_X:
                    case TEXT_FORMAT_ZEROPADDED_0P:
                    case TEXT_FORMAT_HEX:
                        type = "num";
                        order = "asc";
                        break;
                    //case TEXT_FORMAT_STRING:
                    //case TEXT_FORMAT_CHAR:
                    //case TEXT_FORMAT_PARENTS:
                    //case TEXT_FORMAT_NOP:
                    //case TEXT_FORMAT_CODE1:
                    //case TEXT_FORMAT_UNKNOWN:
                    default:
                        type = "text";
                        order = "asc";
                        break;
                }
                orders.put(order);
                types.put(type);
            }
            rc.getScreenVars().put("columns", columns);
            rc.getScreenVars().put("headers", headers);
            rc.getScreenVars().put("orders", orders);
            rc.getScreenVars().put("types", types);
            rc.getScreenVars().put("sortBy", tempTable.fieldList.size() - 1);
            rc.getScreenVars().put("sortOrder", orders.getString(tempTable.fieldList.size() - 1));
            rc.getScreenVars().put("indexed", true);
            setupHeaders(rc);

            int newIndex =
                rc.getScreenVars().getString("sortOrder").equals("asc") ?
                tempTable.fieldList.size() : -tempTable.fieldList.size();
            JSONObject val = new JSONObject();
            val.put("index", String.valueOf(newIndex));
            rc.putScreenValue(rc.getResponseScreen(), rc.getUiElement("list").path, "value", val);

            int lastGroup;
            for (lastGroup = tempTable.fieldList.size() - 1; lastGroup > 0; lastGroup--) {
                if (orders.get(lastGroup).equals("asc")) {
                    break;
                }
            }

            String lastMetric = "";
            JSONArray metrics = new JSONArray();
            for (int i = lastGroup + 1; i < tempTable.fieldList.size(); i++) {
                JSONObject metric = new JSONObject();
                metric.put("label", tempTable.fieldList.get(i).description);
                lastMetric = tempTable.fieldList.get(i).name;
                metric.put("value", lastMetric);
                metrics.put(metric);
            }
            rc.setValue("chartMetric", lastMetric);
            rc.set("chartMetric", "options", metrics);
            rc.set("chartArea", "hidden", true);
        } else {
            rc.set("chartOptions", "hidden", true);
            rc.set("chartArea", "hidden", true);
        }

    }

    public static void customButton_action(RequestContext rc) throws Exception {
        try {
            int id = Integer.parseInt(rc.getInitiatedBy().split("_")[1]);
            JSONObject button = rc.getScreenVars().getJSONArray("customButtons").getJSONObject(id);
            String c = button.getString("class");
            String m = button.getString("method");
            long now = System.currentTimeMillis();
            rc.getScreenVars().put("jobStart", now);
            rc.getScreenVars().put("jobPoll", now);
            rc.getScreenVars().put("jobProgress", getNextTempId());
            Method method = Class.forName(c).getMethod(m, RequestContext.class);
            method.invoke(method, rc);
        } catch (Exception e) {
            log.error("Error handling custom button: " + rc.getInitiatedBy(), e);
            rc.snackErrorMessage("Error processing request");
        }
    }

    public static void customButton_poll(RequestContext rc) throws Exception {
        int jobId = rc.getScreenVars().getInt("jobId");
        String status = getJodStatus(jobId);
        if (status.startsWith("*")) {
            rc.setValue(rc.event + "@poll", 0);
            rc.set("mainGroupId", "isLoading", false);
            if (status.equals("*Success")) {
                rc.addOpenStartAction((String) jobConfigs.get(jobId).config.get("localFileName"));
            } else {
                rc.snackErrorMessage("Job FAILED");
            }
        } else {
            long now = System.currentTimeMillis();
            if (now - rc.getScreenVars().getLong("jobPoll") > 900) {
                long start = rc.getScreenVars().getLong("jobStart");
                byte progress = getJobProgress(jobId);
                //byte progress = getJodProgress(jobId);
                rc.snackInfoMessage(String.format("Running: %.1f sec, %d%%", (double) (now - start) / 1000, progress),
                                    rc.getScreenVars().getLong("jobProgress"), 2000);
                rc.getScreenVars().put("jobPoll", now);
            }
        }
    }

    private static void setupHeaders(RequestContext rc) {
        JSONArray columns = rc.getScreenVars().getJSONArray("columns");
        JSONArray types = rc.getScreenVars().getJSONArray("types");
        JSONArray headers = rc.getScreenVars().getJSONArray("headers");
        int index = rc.getScreenVars().getInt("sortBy");
        String sortOrder = rc.getScreenVars().getString("sortOrder");
        for (int i = 0; i < columns.length(); i++) {
            if (i != index) {
                String order = "<span class='material-icons' style='color:#666666; font-size:20px'>swap_vert</span>";
                if (columns.getJSONObject(i).optString("headerStyle").contains("right")) {
                    columns.getJSONObject(i).put("description",
                                                 order + " "
                                                 + "<span style='color:#00DDDD;'>"
                                                 + headers.getString(i)
                                                 + "</span>");
                } else {
                    columns.getJSONObject(i).put("description",
                                                 "<span style='color:#00DDDD;'>"
                                                 + headers.getString(i)
                                                 + "</span> " + order);
                }
                columns.getJSONObject(i)
                    .put("headerContainerStyle", new JSONObject("{\"background\": \"#003333\"}"));
            } else {
                String order;
                if (sortOrder.equals("asc")) {
                    if (types.get(i).equals("num")) {
                        order =
                            "<span class='admino-icon-sort-numeric-asc' style='color:#00FF00; font-size:20px'></span>";
                    } else {
                        order =
                            "<span class='admino-icon-sort-alpha-asc' style='color:#00FF00; font-size:20px'></span>";
                    }
                } else {
                    if (types.get(i).equals("num")) {
                        order =
                            "<span class='admino-icon-sort-numeric-desc' style='color:#FF0000; font-size:20px'></span>";
                    } else {
                        order =
                            "<span class='admino-icon-sort-alpha-desc' style='color:#FF0000; font-size:20px'></span>";
                    }
                }
//                if (sortOrder.equals("asc")) {
//                    if (types.get(i).equals("num")) {
//                        order = "<span class='admino-icon-sort-amount-asc' style='color:#00FF00;'></span>";
//                    } else {
//                        order = "<span class='admino-icon-sort-amount-asc' style='color:#00FF00;'></span>";
//                    }
//                } else {
//                    if (types.get(i).equals("num")) {
//                        order = "<span class='admino-icon-sort-amount-desc' style='color:#FF0000;'></span>";
//                    } else {
//                        order = "<span class='admino-icon-sort-amount-desc' style='color:#FF0000;'></span>";
//                    }
//                }

                if (columns.getJSONObject(i).optString("headerStyle").contains("right")) {
                    columns.getJSONObject(i).put("description",
                                                 order + "<span style='color:#FFFFAA;'>&nbsp"
                                                 + headers.getString(i)
                                                 + "</span>"
                    );
                } else {
                    columns.getJSONObject(i).put("description",
                                                 "<span style='color:#FFFFAA;'>"
                                                 + headers.getString(i) + "&nbsp</span>" + order);
                }
                columns.getJSONObject(i)
                    .put("headerContainerStyle", new JSONObject("{\"background\": \"#004444\"}"));
            }
        }
    }

    public static void list_headerCellClick(RequestContext rc) throws Exception {
        if (!rc.getScreenVars().optBoolean("indexed", false)) {
            return;
        }
        String modifiers = rc.posted.getJSONArray("activeModifierKeys").toString();

        JSONArray columns = rc.getScreenVars().getJSONArray("columns");
        JSONArray headers = rc.getScreenVars().getJSONArray("headers");
        JSONArray orders = rc.getScreenVars().getJSONArray("orders");
        int index = rc.getScreenVars().getInt("sortBy");
        String sortOrder = rc.getScreenVars().getString("sortOrder");
        int newIndex = rc.getValueJson("list").getInt("selectedHeaderColumnIndex");
        if (newIndex == index) {
            sortOrder = sortOrder.equals("asc") ? "desc" : "asc";
            orders.put(newIndex, sortOrder);
        } else {
            sortOrder = orders.getString(newIndex);
        }
        rc.getScreenVars().put("sortBy", newIndex);
        rc.getScreenVars().put("sortOrder", sortOrder);
        setupHeaders(rc);
        rc.set("list", "columns", columns);

        newIndex = sortOrder.equals("asc") ? newIndex + 1 : -1 - newIndex;
        JSONObject val = new JSONObject();
        val.put("index", String.valueOf(newIndex));

        if (modifiers.contains("Alt")) {
            val.put("cursorpos", rc.getValueJson("list").getInt("count") / 2);
        } else {
            val.put("keys", ElementUtils.tablePositionFirst).put("cursorpos", 0);
        }

        rc.putScreenValue(rc.getResponseScreen(), rc.getUiElement("list").path, "value", val);
        rc.tableForceRefresh("list");
    }

    public static void exportXls_action(RequestContext rc) throws Exception {
        long now = System.currentTimeMillis();
        rc.getScreenVars().put("jobStart", now);
        rc.getScreenVars().put("jobPoll", now);
        rc.getScreenVars().put("jobProgress", getNextTempId());
        rc.stdExeclExportAction();
    }

    public static void exportXls_poll(RequestContext rc) throws Exception {
        int jobId = rc.getScreenVars().getInt("jobId");
        String status = getJodStatus(jobId);
        if (status.startsWith("*")) {
            rc.setValue(rc.event + "@poll", 0);
            rc.set("mainGroupId", "isLoading", false);
            if (status.equals("*Success")) {
                rc.addDownloadStartAction((String) jobConfigs.get(jobId).config.get("localFileName"));
            } else {
                rc.snackErrorMessage("Job FAILED");
            }
        } else {
            long now = System.currentTimeMillis();
            if (now - rc.getScreenVars().getLong("jobPoll") > 900) {
                long start = rc.getScreenVars().getLong("jobStart");
                byte progress = getJobProgress(jobId);
                //byte progress = getJodProgress(jobId);
                rc.snackInfoMessage(String.format("Running: %.1f sec, %d%%", (double) (now - start) / 1000, progress),
                                    rc.getScreenVars().getLong("jobProgress"), 2000);
                rc.getScreenVars().put("jobPoll", now);
            }
        }
    }

    public static void exportPdf_action(RequestContext rc) throws Exception {
        rc.stdPdfExportAction();
    }

    public static void exportPdf_poll(RequestContext rc) throws Exception {
        rc.stdPdfExportPoll();
    }

    public static void chart_action(RequestContext rc) throws Exception {
        String url = backendService + "/request/" + rc.screenId + "$" + "chartiframe" + "@download"
                     + "?sid=" + rc.posted.get("sid") + "&id=" + getNextTempId();
        rc.setValue("chartiframe", url);
        rc.iFrameUpdate("chartiframe");
        rc.setFocus("chartFocus");
        rc.getScreenVars().put("chartType", rc.getValueString("chartType"));
        rc.getScreenVars().put("chartLimit", rc.getValueString("chartLimit"));
        rc.getScreenVars().put("chartMetric", rc.getValueString("chartMetric"));
        rc.set("chartArea", "hidden", false);

    }

    public static void chartType_change(RequestContext rc) throws Exception {
        chart_action(rc);
    }

    public static void chartLimit_change(RequestContext rc) throws Exception {
        chart_action(rc);
    }

    public static void chartMetric_change(RequestContext rc) throws Exception {
        chart_action(rc);
    }

    public static void chartiframe_download(RequestContext rc) throws Exception {

        String staticSource;
        switch (rc.getScreenVars().getString("chartType")) {
            case "bar":
                staticSource = workdir + "/_admino/resources/am4/demos/column-with-rotated-series.html";
                break;
            case "bar3d":
                staticSource = workdir + "/_admino/resources/am4/demos/3d-column-chart.html";
                break;
            case "cylinder":
                staticSource = workdir + "/_admino/resources/am4/demos/3d-cylinder-chart.html";
                break;
            case "pie":
                staticSource = workdir + "/_admino/resources/am4/demos/pie-chart.html";
                break;
            case "pie3d":
                staticSource = workdir + "/_admino/resources/am4/demos/3d-pie-chart.html";
                break;
            case "donut3d":
                staticSource = workdir + "/_admino/resources/am4/demos/donut-with-radial-gradient.html";
                break;
            case "draggingpie":
                staticSource = workdir + "/_admino/resources/am4/demos/dragging-slices-pie.html";
                break;
            default:
                staticSource = workdir + "/_admino/resources/am4/demos/column-with-rotated-series.html";
                break;
        }

        DatabaseTable tempTable = tables.get(rc.getScreenVars().getString("resultTableName"));
        MemoryRecord memoryRecord;
        String label;
        String primary = null;
        if (tempTable.fieldList.get(1).description.equals("Megnevezés")) {
            label = tempTable.fieldList.get(1).name;
            primary = tempTable.fieldList.get(0).name;
        } else {
            label = tempTable.fieldList.get(0).name;
        }
        String value = rc.getScreenVars().getString("chartMetric");

        JSONArray data = new JSONArray();
        int limit = Integer.parseInt(rc.getScreenVars().getString("chartLimit"));
        limit = tempTable.recordCount > limit ? limit : (int) tempTable.recordCount;

        int[] index;
        boolean indexAsc;
        if (tempTable.indexCount > 0) {
            index = tempTable.indexes.get(rc.getScreenVars().getInt("sortBy") + 1);
            indexAsc = rc.getScreenVars().get("sortOrder").equals("asc");
        } else {
            index = null;
            indexAsc = true;
        }

        if (rc.getScreenVars().getString("chartType").equals("draggingpie")) {
            JSONObject item = new JSONObject();
            item.put("label", "Dummy");
            item.put("disabled", true);
            item.put("value", 1000);
            item.put("color", "#555555");
            item.put("opacity", 0.1);
            item.put("strokeDasharray", "4,4");
            data.put(item);
        }

        for (int i = 1; i <= limit; i++) {
            int absPos = index == null ? i : indexAsc ? index[i] : index[(int) (tempTable.recordCount - i + 1)];
            memoryRecord = tempTable.getMemoryRecord(absPos);
            JSONObject item = new JSONObject();
            item.put("label",
                     primary == null ? memoryRecord.getASFormattedString(label).trim() :
                     memoryRecord.getASFormattedString(primary).trim() + " " + memoryRecord.getAsString(label)
                         .trim()
            );
            item.put("value", Double.parseDouble(memoryRecord.getASFormattedString(value).trim()));
            data.put(item);
        }
        StringBuilder out = new StringBuilder();
        out.append("" +
                   "<!-- Generated by the backend -->\n"
                   + "<script>\n"
                   + "function getBackendVariables() {\n"
                   + "  var vars = " + data.toString() + ";\n"
                   + "  return vars;\n"
                   + "}\n"
                   + "</script>\n"
                   + "<!-- End of generated code -->\n");

        byte[] generatedContent = out.toString().getBytes();
        int staticContentLength = (int) new File(staticSource).length();

        byte[] response = new byte[generatedContent.length + staticContentLength];
        System.arraycopy(generatedContent, 0, response, 0, generatedContent.length);

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(staticSource, "r")) {
            randomAccessFile.read(response, generatedContent.length, staticContentLength);
            rc.setCustomResponse(response, "text/html; charset=utf-8", 200);
        }

    }


}
