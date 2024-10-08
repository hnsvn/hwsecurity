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

package de.cotech.hw.openpgp.internal.openpgp;


import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;


@RestrictTo(Scope.LIBRARY_GROUP)
public enum KeyType {
    SIGN(0, 0xB6, 0xCE, 0xC7, 0xC1),
    ENCRYPT(1, 0xB8, 0xCF, 0xC8, 0xC2),
    AUTH(2, 0xA4, 0xD0, 0xC9, 0xC3);

    private final int mIdx;
    private final int mSlot;
    private final int mTimestampObjectId;
    private final int mFingerprintObjectId;
    private final int mAlgoAttributeSlot;

    KeyType(int idx, int slot, int timestampObjectId, int fingerprintObjectId, int algoAttributeSlot) {
        this.mIdx = idx;
        this.mSlot = slot;
        this.mTimestampObjectId = timestampObjectId;
        this.mFingerprintObjectId = fingerprintObjectId;
        this.mAlgoAttributeSlot = algoAttributeSlot;
    }

    public int getIdx() {
        return mIdx;
    }

    public int getSlot() {
        return mSlot;
    }

    public int getTimestampObjectId() {
        return mTimestampObjectId;
    }

    public int getFingerprintObjectId() {
        return mFingerprintObjectId;
    }

    public int getAlgoAttributeSlot() {
        return mAlgoAttributeSlot;
    }
}
