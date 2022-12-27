/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;

import java.util.ArrayList;
import java.util.List;

public abstract class DataTableHandler<T> extends DatabaseHandler {

    public DataTableHandler(DatabaseConnector sqliteConnector) {
        super(sqliteConnector);
    }

    protected abstract DataRowExtractor<T> getDataRowExtractor();

    public List<T> select(String table, String[] columns,
                          String selection, String[] selectionArgs) {
        return select(table, columns, selection, selectionArgs,
                null, null, null, null);
    }

    public List<T> select(String table, String[] columns,
                          String selection, String[] selectionArgs,
                          String groupBy, String having, String orderBy, String limit) {
        List<T> rows = new ArrayList<>();
        DataRowExtractor<T> extractor = getDataRowExtractor();
        try (Cursor cursor = query(false, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit)) {
            while (cursor.moveToNext()) {
                rows.add(extractor.extractRow(cursor, cursor.getPosition()));
            }
        } catch (SQLiteCantOpenDatabaseException e) {
            e.printStackTrace();
        }
        return rows;
    }
}
