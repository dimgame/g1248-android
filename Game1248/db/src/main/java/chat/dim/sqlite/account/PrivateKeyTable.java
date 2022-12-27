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
package chat.dim.sqlite.account;

import android.content.ContentValues;

import java.util.List;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.dbi.PrivateKeyDBI;
import chat.dim.format.JSON;
import chat.dim.protocol.ID;
import chat.dim.sqlite.DataRowExtractor;
import chat.dim.sqlite.DataTableHandler;
import chat.dim.sqlite.DatabaseConnector;

public final class PrivateKeyTable extends DataTableHandler<PrivateKey> implements PrivateKeyDBI {

    private DataRowExtractor<PrivateKey> extractor;

    public PrivateKeyTable(DatabaseConnector connector) {
        super(connector);
        // lazy load
        extractor = null;
    }

    @Override
    protected DataRowExtractor<PrivateKey> getDataRowExtractor() {
        return extractor;
    }

    private boolean prepare() {
        if (extractor == null) {
            // prepare data row extractor
            extractor = (cursor, index) -> {
                String json = cursor.getString(0);
                Object key = JSON.decode(json);
                return PrivateKey.parse(key);
            };
        }
        return true;
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        if (!prepare()) {
            // db error
            return null;
        }

        String[] columns = {"pri_key"};
        String[] selectionArgs = {user.toString()};
        List<PrivateKey> results = select(AccountDatabase.T_PRIVATE_KEY, columns,
                "user=? AND decrypt=1", selectionArgs,
                null, null, "type DESC", null);
        return PrivateKeyDBI.convertDecryptKeys(results);
    }

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        // TODO: support multi private keys
        return getPrivateKeyForVisaSignature(user);
    }

    @Override
    public PrivateKey getPrivateKeyForVisaSignature(ID user) {
        if (!prepare()) {
            // db error
            return null;
        }

        String[] columns = {"pri_key"};
        String[] selectionArgs = {user.toString()};
        List<PrivateKey> results = select(AccountDatabase.T_PRIVATE_KEY, columns,
                "user=? AND type='M' AND sign=1", selectionArgs,
                null, null, "id DESC", null);
        // return first record only
        return results == null || results.size() == 0 ? null : results.get(0);
    }

    private boolean savePrivateKey(ID user, PrivateKey key, String type, int sign, int decrypt) {
        if (!prepare()) {
            // db error
            return false;
        }
        String json = JSON.encode(key);

        ContentValues values = new ContentValues();
        values.put("user", user.toString());
        values.put("pri_key", json);
        values.put("type", type);
        values.put("sign", sign);
        values.put("decrypt", decrypt);
        return insert(AccountDatabase.T_PRIVATE_KEY, null, values) > 0;
    }

    @Override
    public boolean savePrivateKey(PrivateKey key, String type, ID user) {
        if (key instanceof DecryptKey) {
            return savePrivateKey(user, key, type, 1, 1);
        } else {
            return savePrivateKey(user, key, type, 1, 0);
        }
    }
}
