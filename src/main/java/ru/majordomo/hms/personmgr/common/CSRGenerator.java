package ru.majordomo.hms.personmgr.common;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class CSRGenerator {
    private KeyPair keyPair;

    public CSRGenerator() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());

        keyPair = keyGen.generateKeyPair();
    }

    public String getCSR(String commonName, String organizationUnit, String organizationName, String location, String state, String country) throws Exception {
        X500Name x500Name = buildX500Name(
                commonName, organizationUnit, organizationName, location, state, country
        );

        byte[] csr = generatePKCS10(x500Name);

        PemObject pemObject = new PemObject("CERTIFICATE REQUEST", csr);
        StringWriter str = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(str);
        pemWriter.writeObject(pemObject);
        pemWriter.close();
        str.close();
        return str.toString();
    }

    private byte[] generatePKCS10(X500Name x500Name) throws Exception {
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                new X500Principal(x500Name.toString()),
                keyPair.getPublic()
        );

        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withRSA");

        ContentSigner signer = csBuilder.build(keyPair.getPrivate());

        PKCS10CertificationRequest csr = p10Builder.build(signer);

        return csr.getEncoded();
    }

    private X500Name buildX500Name(
            String commonName, String organizationUnit, String organization, String locality, String state, String country
    ) {
        X500NameBuilder nameBuilder = new X500NameBuilder();

        if (!commonName.isEmpty()) {
            nameBuilder.addRDN(BCStyle.CN, commonName);
        }
        if (!organizationUnit.isEmpty()) {
            nameBuilder.addRDN(BCStyle.OU, organizationUnit);
        }
        if (!organization.isEmpty()) {
            nameBuilder.addRDN(BCStyle.O, organization);
        }
        if (!locality.isEmpty()) {
            nameBuilder.addRDN(BCStyle.L, locality);
        }
        if (!state.isEmpty()) {
            nameBuilder.addRDN(BCStyle.ST, state);
        }
        if (!country.isEmpty()) {
            nameBuilder.addRDN(BCStyle.C, country);
        }

        return nameBuilder.build();
    }

    public String getPrivateKeyAsString() throws IOException {
        StringWriter sw = new StringWriter();
        JcaPEMWriter writer = new JcaPEMWriter(sw);
        writer.writeObject(keyPair.getPrivate());
        writer.close();
        return sw.getBuffer().toString();
    }

    /*public static void main(String[] args) throws Exception {
        CSRGenerator gcsr = new CSRGenerator();

        System.out.println("Public Key:\n"+gcsr.getPublicKey().toString());

        System.out.println("Private Key:\n"+gcsr.getPrivateKey().toString());

        String csr = gcsr.getCSR(
                "journaldev.com <https://www.journaldev.com>",
                "Java",
                "JournalDev",
                "Cupertino",
                "California",
                "USA"
        );
        System.out.println("CSR Request Generated!!");
        System.out.println(csr);
    }*/
}
