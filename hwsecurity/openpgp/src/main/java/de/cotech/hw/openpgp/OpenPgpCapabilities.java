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

package de.cotech.hw.openpgp;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import de.cotech.hw.internal.iso7816.Iso7816TLV;
import de.cotech.hw.openpgp.internal.openpgp.KeyFormat;
import de.cotech.hw.openpgp.internal.openpgp.KeyType;
import de.cotech.hw.openpgp.internal.openpgp.OpenPgpAid;


@SuppressWarnings("unused") // just expose all included data
@AutoValue
public abstract class OpenPgpCapabilities {
    private static final byte[] EMPTY_FINGERPRINT = new byte[20];

    private final static int MASK_SM = 1 << 7;
    private final static int MASK_KEY_IMPORT = 1 << 5;
    private final static int MASK_ATTRIBUTES_CHANGABLE = 1 << 2;

    private static final int MAX_PW1_LENGTH_INDEX = 1;
    private static final int MAX_PW3_LENGTH_INDEX = 3;

    @SuppressWarnings("mutable")
    public abstract byte[] getAid();

    public abstract OpenPgpAid getOpenPgpAid();

    @SuppressWarnings("mutable")
    public abstract byte[] getHistoricalBytes();

    @Nullable
    @SuppressWarnings("mutable")
    public abstract byte[] getFingerprintSign();

    @Nullable
    @SuppressWarnings("mutable")
    public abstract byte[] getFingerprintEncrypt();

    @Nullable
    @SuppressWarnings("mutable")
    public abstract byte[] getFingerprintAuth();

    @SuppressWarnings("mutable")
    public abstract byte[] getPwStatusBytes();

    public abstract KeyFormat getSignKeyFormat();

    public abstract KeyFormat getEncryptKeyFormat();

    public abstract KeyFormat getAuthKeyFormat();

    abstract boolean isHasKeyImport();

    public abstract boolean isAttributesChangable();

    abstract boolean isHasSM();

    abstract boolean isHasAesSm();

    public abstract boolean isHasScp11bSm();

    abstract int getMaxGetChallengeLength();

    public abstract int getMaxCardholderCertLength();

    abstract int getMaxSpecialDoLength();

    public static OpenPgpCapabilities fromBytes(byte[] rawOpenPgpCapabilities) throws IOException {
        Iso7816TLV[] parsedTlvData = Iso7816TLV.readList(rawOpenPgpCapabilities, true);
        return new AutoValue_OpenPgpCapabilities.Builder().updateWithTLV(parsedTlvData).build();
    }

    public KeyFormat getFormatForKeyType(@NonNull KeyType keyType) {
        switch (keyType) {
            case SIGN:
                return getSignKeyFormat();
            case ENCRYPT:
                return getEncryptKeyFormat();
            case AUTH:
                return getAuthKeyFormat();
        }
        return null;
    }

    @Nullable
    public byte[] getKeyFingerprint(@NonNull KeyType keyType) {
        switch (keyType) {
            case SIGN:
                return getFingerprintSign();
            case ENCRYPT:
                return getFingerprintEncrypt();
            case AUTH:
                return getFingerprintAuth();
        }
        return null;
    }

    public boolean hasSignKey() {
        return !Arrays.equals(EMPTY_FINGERPRINT, getFingerprintSign());
    }

    public boolean hasEncryptKey() {
        return !Arrays.equals(EMPTY_FINGERPRINT, getFingerprintEncrypt());
    }

    public boolean hasAuthKey() {
        return !Arrays.equals(EMPTY_FINGERPRINT, getFingerprintAuth());
    }

    public boolean isPw1ValidForMultipleSignatures() {
        return getPwStatusBytes()[0] == 1;
    }

    public int getPw1MaxLength() {
        return getPwStatusBytes()[MAX_PW1_LENGTH_INDEX];
    }

    public int getPw3MaxLength() {
        return getPwStatusBytes()[MAX_PW3_LENGTH_INDEX];
    }

    public int getPw1TriesLeft() {
        return getPwStatusBytes()[4];
    }

    public int getPw3TriesLeft() {
        return getPwStatusBytes()[6];
    }

    @AutoValue.Builder
    @SuppressWarnings("UnusedReturnValue")
    abstract static class Builder {
        abstract Builder aid(byte[] mV);

        abstract Builder openPgpAid(OpenPgpAid openPgpAid);

        abstract Builder historicalBytes(byte[] historicalBytes);

        abstract Builder fingerprintSign(byte[] fingerprint);

        abstract Builder fingerprintEncrypt(byte[] fingerprint);

        abstract Builder fingerprintAuth(byte[] fingerprint);

        abstract Builder pwStatusBytes(byte[] mV);

        abstract Builder authKeyFormat(KeyFormat keyFormat);

        abstract Builder encryptKeyFormat(KeyFormat keyFormat);

        abstract Builder signKeyFormat(KeyFormat keyFormat);


        abstract Builder hasKeyImport(boolean hasKeyImport);

        abstract Builder attributesChangable(boolean attributesChangable);

        abstract Builder hasSM(boolean hasSm);

        abstract Builder hasAesSm(boolean hasAesSm);

        abstract Builder hasScp11bSm(boolean hasScp11bSm);

        abstract Builder maxGetChallengeLength(int maxResponseLen);

        abstract Builder maxCardholderCertLength(int maxCommandLen);

        abstract Builder maxSpecialDoLength(int maxSpecialDoLength);

        abstract OpenPgpCapabilities build();

        public Builder() {
            hasKeyImport(false);
            attributesChangable(false);
            hasSM(false);
            hasAesSm(false);
            hasScp11bSm(false);
            maxGetChallengeLength(0);
            maxCardholderCertLength(0);
            maxSpecialDoLength(0);
        }

        Builder updateWithTLV(Iso7816TLV[] tlvs) {
            if (tlvs.length == 1 && tlvs[0].mT == 0x6E) {
                tlvs = ((Iso7816TLV.Iso7816CompositeTLV) tlvs[0]).mSubs;
            }

            for (Iso7816TLV tlv : tlvs) {
                switch (tlv.mT) {
                    case 0x4F:
                        aid(tlv.mV);
                        openPgpAid(OpenPgpAid.create(tlv.mV));
                        break;
                    case 0x5F52:
                        historicalBytes(tlv.mV);
                        break;
                    case 0x73:
                        parseDdo((Iso7816TLV.Iso7816CompositeTLV) tlv);
                        break;
                    case 0xC0:
                        parseExtendedCaps(tlv.mV);
                        break;
                    case 0xC1:
                        signKeyFormat(KeyFormat.fromBytes(tlv.mV));
                        break;
                    case 0xC2:
                        encryptKeyFormat(KeyFormat.fromBytes(tlv.mV));
                        break;
                    case 0xC3:
                        authKeyFormat(KeyFormat.fromBytes(tlv.mV));
                        break;
                    case 0xC4:
                        pwStatusBytes(tlv.mV);
                        break;
                    case 0xC5:
                        parseFingerprints(tlv.mV);
                        break;
                }
            }

            return this;
        }

        private void parseDdo(Iso7816TLV.Iso7816CompositeTLV tlvs) {
            for (Iso7816TLV tlv : tlvs.mSubs) {
                switch (tlv.mT) {
                    case 0xC0:
                        parseExtendedCaps(tlv.mV);
                        break;
                    case 0xC1:
                        signKeyFormat(KeyFormat.fromBytes(tlv.mV));
                        break;
                    case 0xC2:
                        encryptKeyFormat(KeyFormat.fromBytes(tlv.mV));
                        break;
                    case 0xC3:
                        authKeyFormat(KeyFormat.fromBytes(tlv.mV));
                        break;
                    case 0xC4:
                        pwStatusBytes(tlv.mV);
                        break;
                    case 0xC5:
                        parseFingerprints(tlv.mV);
                        break;
                }
            }
        }

        private void parseFingerprints(byte[] mV) {
            ByteBuffer fpBuf = ByteBuffer.wrap(mV);

            byte[] buf;

            buf = new byte[20];
            fpBuf.get(buf);
            fingerprintSign(buf);

            buf = new byte[20];
            fpBuf.get(buf);
            fingerprintEncrypt(buf);

            buf = new byte[20];
            fpBuf.get(buf);
            fingerprintAuth(buf);
        }

        private void parseExtendedCaps(byte[] v) {
            hasKeyImport((v[0] & MASK_KEY_IMPORT) != 0);
            attributesChangable((v[0] & MASK_ATTRIBUTES_CHANGABLE) != 0);

            if ((v[0] & MASK_SM) != 0) {
                hasSM(true);
                int smType = v[1];
                hasAesSm(smType == 1 || smType == 2);
                hasScp11bSm(smType == 3);
            }

            maxGetChallengeLength(((v[2] & 0xff) << 8) + (v[3] & 0xff));
            maxCardholderCertLength(((v[4] & 0xff) << 8) + (v[5] & 0xff));
            maxSpecialDoLength(((v[6] & 0xff) << 8) + (v[7] & 0xff));
        }

    }
}
