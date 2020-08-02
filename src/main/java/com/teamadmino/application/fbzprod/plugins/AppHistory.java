package com.teamadmino.application.fbzprod.plugins;

import static com.teamadmino.admino_backend.server.database.ServerMain.browsers;
import static com.teamadmino.admino_backend.server.database.ServerMain.tables;

import com.teamadmino.admino_backend.server.database.BrowserEngine;
import com.teamadmino.admino_backend.server.database.CellContext;
import com.teamadmino.admino_backend.server.database.DatabaseTable;
import com.teamadmino.admino_backend.server.database.TableViewDefinition;
import com.teamadmino.admino_backend.server.http.RequestContext;
import com.teamadmino.admino_backend.server.ui.ElementUtils;
import org.json.JSONObject;

public class AppHistory {

    public static void init_onload(RequestContext rc) throws Exception {
        String tableName = "apphistory";
        DatabaseTable table = tables.get(tableName);
        TableViewDefinition tableViewDefinition = new TableViewDefinition(tableName);
        tableViewDefinition
            .addTableField("$pos()")
            .addTableField("appHistory()");
        rc
            .set("browser", "value", new JSONObject()
                .put("keys", ElementUtils.tablePositionLast)
                .put("cursorpos", 0))
            .tableForceRefresh("browser")
            .setFocus("browser");

        BrowserEngine browserEngine = new BrowserEngine("apphistory", tableViewDefinition);
        browsers.put("apphistory", browserEngine);
    }

    public static JSONObject appHistory(CellContext cellContext) throws Exception {

        //todo: metadata
        if (cellContext == null) {
            JSONObject definition = new JSONObject();
            definition.put("length", 1);
            definition.put("description", "header");
            definition.put("format", "string");
            return definition;
        }

        int abs = (int) cellContext.memoryRecord.absolutePosition;
        int pos = cellContext.table.logLinesStart.get(abs - 1);

        String line = cellContext.table.logLinesAll.substring(
            pos,
            pos + cellContext.table.logLinesLength.get(abs - 1)
        );
/*
        while (true) {
            int p = line.indexOf('$');
            if (p < 0) {
                break;
            }
            String c;
            switch (line.charAt(p + 1)) {
                case 'b':
                    c = "#0022EE";
                    break;
                case 'c':
                    c = "#009999";
                    break;
                case 'g':
                    c = "#008800";
                    break;
                case 'Y':
                    c = "#EEEE00";
                    break;
                case 'y':
                    c = "#888800";
                    break;
                case 's':
                    c = "#22FF22";
                    break;
                case 'w':
                    c = "#FFFFFF";
                    break;
                default:
                    c = "#AAAAAA";
                    break;
            }
            int e = line.indexOf('$', p + 1);
            line = line.substring(0, p) + "<span style='color:" + c + "'>"
                   + line.substring(p + 2, e) + "</span>" + line.substring(e + 1);
        }
*/
        if (line.startsWith("2020") && line.length() == 10) {
            line = "<span style='color:#BBBB00'>" + line + "</span>";
        }
        cellContext.output.append(line);
        return null;
    }

}
