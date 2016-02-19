/*
 * Chat.onion - P2P Instant Messenger
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.chat;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Sock {

    static final int timeout = 60000;
    Socket sock;
    BufferedReader reader;
    BufferedWriter writer;

    public Sock(Context context, String host, int p) {

        log(host);

        sock = new Socket();

        try {

            Tor tor = Tor.getInstance(context);

            try {

                sock.connect(new InetSocketAddress("127.0.0.1", tor.getPort()), timeout);

            } catch (SocketTimeoutException ex3) {
                log("timeout");
                try {
                    sock.close();
                } catch (IOException ex2) {
                }
            } catch (IOException ex) {
                log("failed to open socket");
                try {
                    sock.close();
                } catch (IOException ex2) {
                }
            } catch (Exception ex) {
                log("sock connect err");
                try {
                    sock.close();
                } catch (IOException ex2) {
                }
            }

            sock.setSoTimeout(timeout);

            // connect to proxy
            {
                //    ByteArrayOutputStream os = new ByteArrayOutputStream();

                OutputStream os = sock.getOutputStream();

                os.write(4); // socks 4a
                os.write(1); // stream

                //Log.i(TAG, "proto " + u.getProtocol());
                //if (p < 0 && u.getProtocol().equals("http")) p = 80;
                //if (p < 0 && u.getProtocol().equals("https")) p = 443;
                //Log.i(TAG, "port " + p);
                os.write((p >> 8) & 0xff);
                os.write((p >> 0) & 0xff);

                os.write(0);
                os.write(0);
                os.write(0);
                os.write(1);

                os.write(0);

                os.write(host.getBytes());
                os.write(0);

                os.flush();

                //    sock.
            }


            // get proxy response

            {
                InputStream is = sock.getInputStream();

                byte[] h = new byte[8];
                is.read(h);

                if (h[0] != 0) {
                    log("unknown error");
                    try {
                        sock.close();
                    } catch (IOException ex2) {
                    }
                    return;
                }

                if (h[1] != 0x5a) {

                    if (h[1] == 0x5b) {
                        log("request rejected or failed");
                        try {
                            sock.close();
                        } catch (IOException ex2) {
                        }
                        return;
                    }

                    log("unknown error");
                    try {
                        sock.close();
                    } catch (IOException ex2) {
                    }
                    return;
                }

            }


            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));

        } catch (SocketTimeoutException ex3) {
            log("timeout");
            try {
                sock.close();
            } catch (IOException ex2) {
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log("failed to connect to tor");
            try {
                sock.close();
            } catch (IOException ex2) {
            }
        }

    }

    private void log(String s) {
        if (!BuildConfig.DEBUG) return;
        Log.i("Sock", s);
    }

    public void writeLine(String... ss) {
        String s = "";

        if (ss.length > 0) s = ss[0];
        for (int i = 1; i < ss.length; i++)
            s += " " + ss[i];

        log(" >> " + s);
        if (writer != null) {
            try {
                writer.write(s + "\r\n");
            } catch (SocketTimeoutException ex) {
                log("timeout");
                try {
                    sock.close();
                } catch (IOException ex2) {
                }
            } catch (Exception ex) {
            }
        }
    }

    public String readLine() {
        String s = null;
        if (reader != null) {
            try {
                s = reader.readLine();
            } catch (SocketTimeoutException ex) {
                log("timeout");
                try {
                    sock.close();
                } catch (IOException ex2) {
                }
            } catch (Exception ex) {
            }
        }
        if (s == null)
            s = "";
        else
            s = s.trim();
        log(" << " + s);
        return s;
    }

    public boolean readBool() {
        return readLine().equals("1");
    }

    public boolean queryBool(String... request) {
        writeLine(request);
        flush();
        return readBool();
    }

    public void queryOrClose(String... request) {
        if (!queryBool(request)) close();
    }

    public boolean queryAndClose(String... request) {
        boolean x = queryBool(request);
        close();
        return x;
    }

    public void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (SocketTimeoutException ex) {
                log("timeout");
                try {
                    sock.close();
                } catch (IOException ex2) {
                }
            } catch (Exception ex) {
            }
        }
    }

    public void close() {

        flush();

        if (sock != null) {
            try {
                sock.close();
            } catch (Exception ex) {
            }
        }

        reader = null;
        writer = null;
        sock = null;

    }

    public boolean isClosed() {
        //try {
        return sock.isClosed();
        /*} catch(IOException ex) {
            return true;
        }*/
    }


}
