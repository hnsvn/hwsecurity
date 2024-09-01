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


import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import de.cotech.hw.internal.iso7816.Iso7816TLV;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.jce.ECPointUtil;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;


@RestrictTo(Scope.LIBRARY_GROUP)
public class ECKeyFormatParser implements KeyFormatParser {
    private final ASN1ObjectIdentifier curveOid;

    ECKeyFormatParser(ASN1ObjectIdentifier curveOid) {
        this.curveOid = curveOid;
    }

    @Override
    public ECPublicKey parseKey(byte[] publicKeyBytes) throws IOException {
        Iso7816TLV publicKeyTlv = Iso7816TLV.readSingle(publicKeyBytes, true);
        Iso7816TLV eccEncodedPoints = Iso7816TLV.findRecursive(publicKeyTlv, 0x86);
        if (eccEncodedPoints == null) {
            throw new IOException("Missing ECC public key data (tag 0x86)");
        }

        String curveName = ECNamedCurveTable.getName(curveOid);
        X9ECParameters spec = ECNamedCurveTable.getByOID(curveOid);
        if (spec == null) {
            throw new IOException("Unknown curve OID: " + curveOid.getId());
        }

        ECNamedCurveSpec params = new ECNamedCurveSpec(curveName, spec.getCurve(), spec.getG(), spec.getN());
        ECPoint point = ECPointUtil.decodePoint(params.getCurve(), eccEncodedPoints.mV);
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IOException(e);
        }
    }
}
