package com.virtualightning.transport.http;

import com.virtualightning.transport.environment.TransportConstants;
import com.virtualightning.transport.serversocket.ServerSocketThread;
import com.virtualightning.transport.serversocket.SocketResources;
import com.virtualightning.transport.serversocket.SocketThread;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 23:28:54
 * Project : player
 * Since Version : Alpha
 */
public class HttpServer {
    private ServerSocketThread serverSocketThread;

    public void start() {
        if(serverSocketThread == null) {
            serverSocketThread = new ServerSocketThread(TransportConstants.Resource_Port,
                    new SocketFactory(), null);
        }
    }

    public void close() {
        if(serverSocketThread != null) {
            serverSocketThread.close();
            serverSocketThread = null;
        }
    }

    public void addPath(String path, FileDescriptor fileDescriptor) {
        serverSocketThread.modifySocketResources(new ModifyPathCallback(true, path, fileDescriptor));
    }

    public void removePath(String path) {
        serverSocketThread.modifySocketResources(new ModifyPathCallback(false, path, null));
    }

    private static class ModifyPathCallback implements ServerSocketThread.ModifyResourceCallback {
        private final boolean isAdd;
        private final String path;
        private final FileDescriptor fileDescriptor;

        private ModifyPathCallback(boolean isAdd, String path, FileDescriptor fileDescriptor) {
            this.isAdd = isAdd;
            this.path = path;
            this.fileDescriptor = fileDescriptor;
        }

        @Override
        public void onModify(SocketResources socketResources) {
            if(isAdd) {
                socketResources.put(path, fileDescriptor);
            }
            else {
                socketResources.remove(path);
            }
        }
    }

    private static class SocketFactory implements ServerSocketThread.SocketThreadFactory {
        @Override
        public SocketThread generateSocketThread(int idCode, Socket socket) {
            return new HttpSocketThread(idCode, socket);
        }
    }

    private static class HttpSocketThread extends SocketThread {
        InputStream socketInput;
        OutputStream socketOutput;
        FileInputStream fileInputStream;

        HttpSocketThread(int idCode, Socket socket) {
            super(idCode, socket);
        }

        @Override
        protected void onClose() {
            if(fileInputStream != null) {
                try { fileInputStream.close(); } catch (Exception ignore) {}
                fileInputStream = null;
            }
        }

        @Override
        protected void onInit() throws Exception {
            socketInput = socket.getInputStream();
            socketOutput = socket.getOutputStream();
        }

        @Override
        protected void onRun() throws Exception {
            markLooperEnd();
            String firstLine = readHeaderLine();

        }

        protected String readHeaderLine() throws Exception {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int read;
            boolean doOutput;
            boolean hasReset = false;
            while((read = socketInput.read()) != -1) {
                doOutput = true;
                switch (read) {
                    case '\r':
                        if(!hasReset) {
                            doOutput = false;
                            hasReset = true;
                        }
                        break;
                    case '\n':
                        if(hasReset) {
                            break;
                        }
                        break;
                    default:
                        if(hasReset) {
                            outputStream.write('\r');
                            hasReset = false;
                        }
                        break;
                }
                if(doOutput) {
                    outputStream.write(read);
                }
            }

            outputStream.close();
            return new String(outputStream.toByteArray());
        }
    }

}
