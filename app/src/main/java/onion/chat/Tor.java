/*
 * Chat.onion - P2P Instant Messenger
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.chat;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.digest.DigestUtils;
import org.spongycastle.asn1.ASN1OutputStream;
import org.spongycastle.asn1.x509.RSAPublicKeyStructure;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

public class Tor {

    private static String torname = "ctor";
    private static String tordirname = "tordata";
    private static String torservdir = "torserv";
    private static Tor instance = null;
    private Context context;
    private int port = -1;
    private String domain = "";
    private Listener listener = null;
    private LogListener logListener;
    private String status = "";
    private boolean ready = false;

    public Tor(Context c) {

        this.context = c;

        final Server server = Server.getInstance(context);

        domain = Utils.filestr(new File(getServiceDir(), "hostname")).trim();
        log(domain);

        new Thread() {
            @Override
            public void run() {
                try {
                    //test();

                    log("kill");
                    Native.killTor();

                    log("install");
                    extractFile(context, R.raw.tor, torname);

                    //log("delete on exit");
                    //context.getFileStreamPath(torname).deleteOnExit();

                    log("set executable");
                    context.getFileStreamPath(torname).setExecutable(true);

                    log("make dir");
                    File tordir = new File(context.getFilesDir(), tordirname);
                    tordir.mkdirs();

                    log("make service");
                    File torsrv = new File(context.getFilesDir(), torservdir);
                    torsrv.mkdirs();

                    log("configure");
                    PrintWriter torcfg = new PrintWriter(context.openFileOutput("torcfg", context.MODE_PRIVATE));
                    //torcfg.println("Log debug stdout");
                    torcfg.println("Log notice stdout");
                    torcfg.println("DataDirectory " + tordir.getAbsolutePath());
                    torcfg.println("SOCKSPort auto");
                    torcfg.println("HiddenServiceDir " + torsrv.getAbsolutePath());
                    //torcfg.println("HiddenServicePort 80 unix:" + server.getSocketName());
                    torcfg.println("HiddenServicePort " + getHiddenServicePort() + " " + server.getSocketName());
                    //torcfg.println("HiddenServicePort 80 unix:");
                    torcfg.println();
                    torcfg.close();
                    log(Utils.filestr(new File(context.getFilesDir(), "torcfg")));

                    log("start");
                    Process tor;
                    tor = Runtime.getRuntime().exec(
                            new String[]{
                                    context.getFileStreamPath(torname).getAbsolutePath(),
                                    "-f", context.getFileStreamPath("torcfg").getAbsolutePath()
                            });

                    BufferedReader torreader = new BufferedReader(new InputStreamReader(tor.getInputStream()));
                    while (true) {
                        final String line = torreader.readLine();
                        if (line == null) break;
                        log(line);

                        status = line;


                        if (line.contains("Socks listener listening on port")) {
                            String ns = line;
                            ns = ns.substring(ns.indexOf("Socks listener listening on port"));
                            log(ns);
                            ns = ns.replaceAll("[^0-9]", "");
                            log(ns);
                            port = Integer.parseInt(ns);
                            if (listener != null) listener.onChange();
                        }

                        boolean ready2 = ready;

                        if (domain == null || domain.length() == 0) {
                            domain = Utils.filestr(new File(torsrv, "hostname")).trim();
                        }

                        if (line.contains("100%")) {
                            ls(context.getFilesDir());
                            domain = Utils.filestr(new File(torsrv, "hostname")).trim();
                            log(domain);
                            if (listener != null) listener.onChange();
                            //ready = true;
                            //test();
                            ready2 = true;
                        }

                        if (!ready) {
                            ready = ready2;
                            LogListener l = logListener;
                            if (l != null) {
                                l.onLog();
                            }
                        }

                        ready = ready2;

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    //throw new Error(ex);
                }
            }
        }.start();
    }

    public static Tor getInstance(Context context) {
        if (instance == null) {
            instance = new Tor(context.getApplicationContext());
        }
        return instance;
    }

    static String computeID(RSAPublicKeySpec pubkey) {
        RSAPublicKeyStructure myKey = new RSAPublicKeyStructure(pubkey.getModulus(), pubkey.getPublicExponent());
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ASN1OutputStream as = new ASN1OutputStream(bs);
        try {
            as.writeObject(myKey.toASN1Object());
        } catch (IOException ex) {
            // TODO: error handling? ignore error?
            throw new Error(ex);
        }
        byte[] b = bs.toByteArray();
        b = DigestUtils.getSha1Digest().digest(b);
        return new Base32().encodeAsString(b).toLowerCase().substring(0, 16);
    }

    public static int getHiddenServicePort() {
        return 31512;
    }

    private void log(String s) {
        Log.i("Tor", s);
    }

    void ls(File f) {
        log(f.toString());
        if (f.isDirectory()) {
            for (File s : f.listFiles()) {
                ls(s);
            }
        }
    }

    public int getPort() {
        return port;
    }

    public String getOnion() {
        return domain.trim();
    }

    public String getID() {
        return domain.replace(".onion", "").trim();
    }

    public void setListener(Listener l) {
        listener = l;
        if (listener != null)
            listener.onChange();
    }

    private void extractFile(Context context, int id, String name) {
        try {
            InputStream i = context.getResources().openRawResource(id);
            OutputStream o = context.openFileOutput(name, context.MODE_PRIVATE);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = i.read(buffer)) > 0) {
                o.write(buffer, 0, read);
            }
            i.close();
            o.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            //throw new Error(ex);
        }
    }

    File getServiceDir() {
        return new File(context.getFilesDir(), torservdir);
    }

    KeyFactory getKeyFactory() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            return KeyFactory.getInstance("RSA", "BC");
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public String readPrivateKeyFile() {
        return Utils.filestr(new File(getServiceDir(), "private_key"));
    }

    RSAPrivateKey getPrivateKey() {
        String priv = readPrivateKeyFile();
        //log(priv);
        priv = priv.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
        priv = priv.replace("-----END RSA PRIVATE KEY-----", "");
        priv = priv.replaceAll("\\s", "");
        //log(priv);
        byte[] data = Base64.decode(priv, Base64.DEFAULT);
        //log("" + data.length);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(data);
        //log(keySpec.toString());
        try {
            return (RSAPrivateKey) getKeyFactory().generatePrivate(keySpec);
        } catch (InvalidKeySpecException ex) {
            throw new Error(ex);
        }
    }

    RSAPrivateKeySpec getPrivateKeySpec() {
        try {
            return getKeyFactory().getKeySpec(getPrivateKey(), RSAPrivateKeySpec.class);
        } catch (InvalidKeySpecException ex) {
            throw new Error(ex);
        }
    }

    RSAPublicKeySpec getPublicKeySpec() {
        return new RSAPublicKeySpec(getPrivateKeySpec().getModulus(), BigInteger.valueOf(65537));
    }

    RSAPublicKey getPublicKey() {
        try {
            return (RSAPublicKey) getKeyFactory().generatePublic(getPublicKeySpec());
        } catch (InvalidKeySpecException ex) {
            throw new Error(ex);
        }
    }

    String computeOnion() {
        return computeID(getPublicKeySpec()) + ".onion";
    }

    byte[] pubkey() {
        return getPrivateKeySpec().getModulus().toByteArray();
    }

    byte[] sign(byte[] msg) {
        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(getPrivateKey());
            signature.update(msg);
            return signature.sign();
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    boolean checksig(String id, byte[] pubkey, byte[] sig, byte[] msg) {
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(new BigInteger(pubkey), BigInteger.valueOf(65537));

        if (!id.equals(computeID(publicKeySpec))) {
            log("invalid id");
            return false;
        }

        PublicKey publicKey;
        try {
            publicKey = getKeyFactory().generatePublic(publicKeySpec);
        } catch (InvalidKeySpecException ex) {
            ex.printStackTrace();
            return false;
        }

        try {
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(publicKey);
            signature.update(msg);
            return signature.verify(sig);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    void test() {
        try {
            String domain = Utils.filestr(new File(getServiceDir(), "hostname")).trim();

            log(Utils.filestr(new File(getServiceDir(), "hostname")).trim());
            log(computeID(getPublicKeySpec()));
            log(computeOnion());
            log(Utils.filestr(new File(getServiceDir(), "hostname")).trim());

            log(Base64.encodeToString(pubkey(), Base64.DEFAULT));
            log("pub " + Base64.encodeToString(pubkey(), Base64.DEFAULT));

            byte[] msg = "alkjdalwkdjaw".getBytes();
            log("msg " + Base64.encodeToString(msg, Base64.DEFAULT));

            byte[] sig = sign(msg);
            log("sig " + Base64.encodeToString(sig, Base64.DEFAULT));

            log("chk " + checksig(getID(), pubkey(), sig, msg));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setLogListener(LogListener l) {
        logListener = l;
    }

    public String getStatus() {
        return status;
    }

    public boolean isReady() {
        return ready;
    }


    public interface Listener {
        void onChange();
    }


    public interface LogListener {
        void onLog();
    }
}
