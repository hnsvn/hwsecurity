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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import de.cotech.hw.openpgp.internal.openpgp.ECKeyFormat;
import de.cotech.hw.openpgp.internal.openpgp.KeyType;
import de.cotech.hw.openpgp.internal.openpgp.RSAKeyFormat;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;


@RestrictTo(Scope.LIBRARY_GROUP)
public class OpenPgpCardUtils {
    public static byte[] attributesForEccKey(KeyType slot, ASN1ObjectIdentifier curveOid) throws IOException {
        byte[] oid = curveOid.getEncoded();
        byte[] attrs = new byte[1 + (oid.length - 2) + 1];

        if (slot.equals(KeyType.SIGN))
            attrs[0] = ECKeyFormat.ECAlgorithmFormat.ECDSA_WITH_PUBKEY.getValue();
        else {
            attrs[0] = ECKeyFormat.ECAlgorithmFormat.ECDH_WITH_PUBKEY.getValue();
        }

        System.arraycopy(oid, 2, attrs, 1, (oid.length - 2));
        attrs[attrs.length - 1] = (byte) 0xff;
        return attrs;
    }

    public static byte[] createRSAPrivKeyTemplate(RSAPrivateCrtKey secretKey, KeyType slot,
            RSAKeyFormat format) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(),
                template = new ByteArrayOutputStream(),
                data = new ByteArrayOutputStream(),
                res = new ByteArrayOutputStream();

        int expLengthBytes = (format.getExponentLength() + 7) / 8;
        // Public exponent
        template.write(new byte[]{(byte) 0x91, (byte) expLengthBytes});
        writeBits(data, secretKey.getPublicExponent(), expLengthBytes);

        final int modLengthBytes = format.getModulusLength() / 8;

        // Prime P, length modLengthBytes / 2
        template.write(Hex.decode("928180"));
        writeBits(data, secretKey.getPrimeP(), modLengthBytes / 2);

        // Prime Q, length modLengthBytes / 2
        template.write(Hex.decode("938180"));
        writeBits(data, secretKey.getPrimeQ(), modLengthBytes / 2);


        if (format.getAlgorithmFormat().isIncludeCrt()) {
            // Coefficient (1/q mod p), length modLengthBytes / 2
            template.write(Hex.decode("948180"));
            writeBits(data, secretKey.getCrtCoefficient(), modLengthBytes / 2);

            // Prime exponent P (d mod (p - 1)), length modLengthBytes / 2
            template.write(Hex.decode("958180"));
            writeBits(data, secretKey.getPrimeExponentP(), modLengthBytes / 2);

            // Prime exponent Q (d mod (1 - 1)), length modLengthBytes / 2
            template.write(Hex.decode("968180"));
            writeBits(data, secretKey.getPrimeExponentQ(), modLengthBytes / 2);
        }

        if (format.getAlgorithmFormat().isIncludeModulus()) {
            // Modulus, length modLengthBytes, last item in private key template
            template.write(Hex.decode("97820100"));
            writeBits(data, secretKey.getModulus(), modLengthBytes);
        }

        // Bundle up

        // Ext header list data
        // Control Reference Template to indicate the private key
        stream.write(slot.getSlot());
        stream.write(0);

        // Cardholder private key template
        stream.write(Hex.decode("7F48"));
        stream.write(encodeLength(template.size()));
        stream.write(template.toByteArray());

        // Concatenation of key data as defined in DO 7F48
        stream.write(Hex.decode("5F48"));
        stream.write(encodeLength(data.size()));
        stream.write(data.toByteArray());

        // Result tlv
        res.write(Hex.decode("4D"));
        res.write(encodeLength(stream.size()));
        res.write(stream.toByteArray());

        return res.toByteArray();
    }

    public static byte[] createECPrivKeyTemplate(ECPrivateKey secretKey, ECPublicKey publicKey, KeyType slot,
            ECKeyFormat format) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(),
                template = new ByteArrayOutputStream(),
                data = new ByteArrayOutputStream(),
                res = new ByteArrayOutputStream();

        final int csize = (int) Math.ceil(publicKey.getParams().getCurve().getField().getFieldSize() / 8.0);

        writeBits(data, secretKey.getS(), csize);
        template.write(Hex.decode("92"));
        template.write(encodeLength(data.size()));

        if (format.getAlgorithmFormat().isWithPubkey()) {
            data.write(Hex.decode("04"));
            writeBits(data, publicKey.getW().getAffineX(), csize);
            writeBits(data, publicKey.getW().getAffineY(), csize);
            template.write(Hex.decode("99"));
            template.write(encodeLength(1 + 2 * csize));
        }

        // Bundle up

        // Ext header list data
        // Control Reference Template to indicate the private key
        stream.write(slot.getSlot());
        stream.write(0);

        // Cardholder private key template
        stream.write(Hex.decode("7F48"));
        stream.write(encodeLength(template.size()));
        stream.write(template.toByteArray());

        // Concatenation of key data as defined in DO 7F48
        stream.write(Hex.decode("5F48"));
        stream.write(encodeLength(data.size()));
        stream.write(data.toByteArray());

        // Result tlv
        res.write(Hex.decode("4D"));
        res.write(encodeLength(stream.size()));
        res.write(stream.toByteArray());

        return res.toByteArray();
    }

    public static byte[] encodeLength(int len) {
        if (len < 0) {
            throw new IllegalArgumentException("length is negative");
        } else if (len >= 16777216) {
            throw new IllegalArgumentException("length is too big: " + len);
        }
        byte[] res;
        if (len < 128) {
            res = new byte[1];
            res[0] = (byte) len;
        } else if (len < 256) {
            res = new byte[2];
            res[0] = -127;
            res[1] = (byte) len;
        } else if (len < 65536) {
            res = new byte[3];
            res[0] = -126;
            res[1] = (byte) (len / 256);
            res[2] = (byte) (len % 256);
        } else {
            res = new byte[4];

            res[0] = -125;
            res[1] = (byte) (len / 65536);
            res[2] = (byte) (len / 256);
            res[3] = (byte) (len % 256);
        }
        return res;
    }

    public static void writeBits(ByteArrayOutputStream stream, BigInteger value, int width) {
        if (value.signum() == -1) {
            throw new IllegalArgumentException("value is negative");
        } else if (width <= 0) {
            throw new IllegalArgumentException("width <= 0");
        }

        final byte[] prime = value.toByteArray();
        int skip = 0;

        while ((skip < prime.length) && (prime[skip] == 0)) ++skip;

        if ((prime.length - skip) > width) {
            throw new IllegalArgumentException("not enough width to fit value: "
                    + (prime.length - skip) + "/" + width);
        }

        byte[] res = new byte[width];

        System.arraycopy(prime, skip,
                res, width - (prime.length - skip),
                prime.length - skip);

        stream.write(res, 0, width);
        Arrays.fill(res, (byte) 0);
        Arrays.fill(prime, (byte) 0);
    }
}
