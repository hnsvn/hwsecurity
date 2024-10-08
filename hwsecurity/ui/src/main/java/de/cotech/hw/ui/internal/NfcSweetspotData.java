/*
 * Copyright (C) 2018-2020 Confidential Technologies GmbH
 *
 * You can purchase a commercial license at https://hwsecurity.dev.
 * Buying such a license is mandatory as soon as you develop commercial
 * activities involving this program without disclosing the source code
 * of your own applications.
 *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.cotech.hw.ui.internal;

import android.content.Context;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.RestrictTo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class NfcSweetspotData {
    private JSONObject jsonData;

    private static NfcSweetspotData instance;

    public static NfcSweetspotData getInstance(Context context) {
        if (instance == null) {
            instance = new NfcSweetspotData();
            instance.loadData(context);
        }
        return instance;
    }

    private void loadData(Context context) {
        try {
            jsonData = new JSONObject(loadJSONFromAsset(context));
        } catch (JSONException e) {
            throw new RuntimeException("cannot load NFC sweetspot data from assets");
        }
    }

    private String loadJSONFromAsset(Context context) {
        String json;
        try {
            InputStream is = context.getAssets().open("hwsecurity-nfc-sweetspots.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public Pair<Double, Double> getSweetspotForBuildModel() {
        return getSweetspotForBuildModel(Build.MODEL);
    }

    public Pair<Double, Double> getSweetspotForBuildModel(String buildModel) {
        try {
            JSONObject model = jsonData.getJSONObject(buildModel);
            Double x = model.getDouble("x");
            Double y = model.getDouble("y");

            return new Pair<>(x, y);
        } catch (JSONException e) {
            // no data for this model available
            return null;
        }
    }

}
