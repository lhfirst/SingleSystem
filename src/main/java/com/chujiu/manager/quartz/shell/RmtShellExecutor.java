package com.chujiu.manager.quartz.shell;


import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class RmtShellExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(RmtShellExecutor.class);

    private Connection conn;
    private String ip;
    private String usr;
    private String psword;
    private String charset = Charset.defaultCharset().toString();

    private static final int TIME_OUT = 1000 * 5 * 60;

    public RmtShellExecutor(String ip, String usr, String ps) {
        this.ip = ip;
        this.usr = usr;
        this.psword = ps;
    }

    private boolean login() throws IOException {
        conn = new Connection(ip);
        conn.connect();
        return conn.authenticateWithPassword(usr, psword);
    }

    public String exec(String cmds) throws IOException {
        InputStream stdOut = null;
        InputStream stdErr = null;
        String outStr = "";
        String outErr = "";
        int ret = -1;

        try {
            if (login()) {
                Session session = conn.openSession();
                session.execCommand(cmds);
                stdOut = new StreamGobbler(session.getStdout());
                outStr = processStream(stdOut, charset);
                LOG.info("caijl:[INFO] outStr=" + outStr);
                stdErr = new StreamGobbler(session.getStderr());
                outErr = processStream(stdErr, charset);
                LOG.info("caijl:[INFO] outErr=" + outErr);
                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);
                ret = session.getExitStatus();

            } else {
                LOG.error("caijl:[INFO] ssh2 login failure:" + ip);
                throw new IOException("SSH2_ERR");
            }

        } finally {
            if (conn != null) {
                conn.close();
            }
            if (stdOut != null)
                stdOut.close();
            if (stdErr != null)
                stdErr.close();
        }

        return outStr;
    }

    private String processStream(InputStream in, String charset) throws IOException {
        byte[] buf = new byte[1024];
        StringBuilder sb = new StringBuilder();
        while (in.read(buf) != -1) {
            sb.append(new String(buf, charset));
        }
        return sb.toString();
    }

    public static void main(String[] args) {

        String usr = "root";
        String password = "cptbtptp";
        String serverIP = "192.168.1.202";
        String shPath = "/home/jesse/文档/hello.sh";

        RmtShellExecutor exe = new RmtShellExecutor(serverIP, usr, password);

        String outInf;

        try {
            outInf = exe.exec("sh " + shPath + " xn");
            System.out.println("outInf= " + outInf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}