package com.virtualightning.transport.http;

import com.virtualightning.transport.environment.TransportConstants;
import com.virtualightning.transport.serversocket.ServerSocketThread;
import com.virtualightning.transport.serversocket.SocketResources;
import com.virtualightning.transport.serversocket.SocketThread;
import com.virtualightning.transport.utils.LogUtils;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;

/**
 * Anchor : Create by CimZzz
 * Time : 2020/05/17 23:28:54
 * Project : player
 * Since Version : Alpha
 */
public class HttpServer {
    private ServerSocketThread serverSocketThread;

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 上午10:42
    // 启动 HTTP Server
    ///////////////////////////////////////////////////////////////////////////
    public void start() {
        if(serverSocketThread == null) {
            serverSocketThread = new ServerSocketThread(TransportConstants.Resource_Port, false, new SocketFactory(), null);
            new Thread(serverSocketThread).start();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 上午10:42
    // 关闭 HTTP Server
    ///////////////////////////////////////////////////////////////////////////
    public void close() {
        if(serverSocketThread != null) {
            serverSocketThread.close();
            serverSocketThread = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 上午10:43
    // 添加资源路径
    ///////////////////////////////////////////////////////////////////////////
    public void addPath(String path, FileReadable fileReadable) {
        serverSocketThread.modifySocketResources(new ModifyPathCallback(true, path, fileReadable));
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 上午10:43
    // 移除资源路径
    ///////////////////////////////////////////////////////////////////////////
    public void removePath(String path) {
        serverSocketThread.modifySocketResources(new ModifyPathCallback(false, path, null));
    }

    public interface FileReadable {
        FileInputStream getFileInputStream() throws Exception;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 上午10:43
    // 修改资源路径回调方法
    ///////////////////////////////////////////////////////////////////////////
    private static class ModifyPathCallback implements ServerSocketThread.ModifyResourceCallback {
        private final boolean isAdd;
        private final String path;
        private final FileReadable fileReadable;

        private ModifyPathCallback(boolean isAdd, String path, FileReadable fileReadable) {
            this.isAdd = isAdd;
            this.path = path;
            this.fileReadable = fileReadable;
        }

        @Override
        public void onModify(SocketResources socketResources) {
            if(isAdd) {
                socketResources.put(path, fileReadable);
            }
            else {
                socketResources.remove(path);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 上午10:43
    // SocketThread 工厂
    ///////////////////////////////////////////////////////////////////////////
    private static class SocketFactory implements ServerSocketThread.SocketThreadFactory {
        @Override
        public SocketThread generateSocketThread(int idCode, Socket socket) {
            return new HttpSocketThread(idCode, socket);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Add by CimZzz on 2020/5/19 上午10:43
    // SocketThread 实例
    ///////////////////////////////////////////////////////////////////////////
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
            if(socketOutput != null) {
                try { socketOutput.close(); } catch (Exception ignore) {}
                socketOutput = null;
            }
            if(socketInput != null) {
                try { socketInput.close(); } catch (Exception ignore) {}
                socketInput = null;
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
            String[] firstLineArr = firstLine.split(" ");
            if(firstLineArr.length != 3) {
                LogUtils.log("first line error: " + firstLine);
                return;
            }

            if(!firstLineArr[0].toUpperCase().equals("GET")) {
                LogUtils.log("unsupported method: " + firstLineArr[0]);
                return;
            }

            String path = firstLineArr[1];

            // resolve http headers
            HashMap<String, String> headers = new HashMap<>();
            while (true) {
                String headerLine = readHeaderLine();
                if(headerLine.length() == 0) {
                    break;
                }
                int splitIndex = headerLine.indexOf(':');
                if(splitIndex == -1) {
                    LogUtils.log("unknown header: " + headerLine);
                    return;
                }
                headers.put(headerLine.substring(0, splitIndex).toUpperCase(), headerLine.substring(splitIndex + 1));
            }

            // Get FileDescriptor
            Object remoteObj = getResource(path, null);
            if(!(remoteObj instanceof FileReadable)) {
                print404();
            }
            FileReadable fileReadable = (FileReadable) remoteObj;
            fileInputStream = fileReadable.getFileInputStream();
            printFileDescriptor(headers);
        }

        ///////////////////////////////////////////////////////////////////////////
        // Add by CimZzz on 2020/5/19 上午11:03
        // 输出 404
        ///////////////////////////////////////////////////////////////////////////
        private void print404() throws Exception {
            socketOutput.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
            socketOutput.flush();
        }

        ///////////////////////////////////////////////////////////////////////////
        // Add by CimZzz on 2020/5/19 上午11:03
        // 输出文件
        ///////////////////////////////////////////////////////////////////////////
        private void printFileDescriptor(HashMap<String, String> headerMap) throws Exception {
            long beginOffset = -1;
            long endOffset = -1;
            String range = headerMap.get("RANGE");
            if(range != null) {
                int eqIdx = range.indexOf('=');
                if(eqIdx != -1) {
                    String realRange = range.substring(eqIdx + 1);
                    String[] rangeArray = realRange.split("-");
                    if(rangeArray.length == 1) {
                        try { beginOffset = Long.parseLong(rangeArray[0].trim()); } catch (Exception ignored) { }
                    }
                    else if(rangeArray.length == 2){
                        try { beginOffset = Long.parseLong(rangeArray[0].trim()); } catch (Exception ignored) { }
                        try { endOffset = Long.parseLong(rangeArray[1].trim()); } catch (Exception ignored) { }
                    }
                }
            }

            socketOutput.write("HTTP/1.1 206 Partial Content\r\n".getBytes());
            socketOutput.write("Content-Type: video/mp4\r\n".getBytes());
            socketOutput.write("Accept-Ranges: bytes\r\n".getBytes());
            long fileLength = fileInputStream.available();
            long contentLength = fileLength;

            if(beginOffset != -1) {
                fileInputStream.skip(beginOffset);
                contentLength = endOffset != -1 ? endOffset - beginOffset : contentLength - beginOffset;
            }
            long start = beginOffset == -1 ? 0 : beginOffset;
            long end = endOffset != -1 ? endOffset - beginOffset : fileLength - 1;
            socketOutput.write(("Content-Range: bytes " + start + "-" + end + "/" + fileLength + "\r\n").getBytes());
            socketOutput.write(("Content-Length: " + contentLength + "\r\n").getBytes());
            socketOutput.write("\r\n".getBytes());
            socketOutput.flush();

            byte[] buffer = new byte[1024];
            while (contentLength > 0) {
                int needLength = contentLength >= 1024 ? 1024 : (int) contentLength;
                int readLength = fileInputStream.read(buffer, 0, needLength);
                contentLength -= readLength;
                socketOutput.write(buffer, 0, readLength);
                socketOutput.flush();
            }
            socketOutput.flush();
        }

        ///////////////////////////////////////////////////////////////////////////
        // Add by CimZzz on 2020/5/19 上午11:04
        // 读取 Headers
        ///////////////////////////////////////////////////////////////////////////
        private String readHeaderLine() throws Exception {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int read;
            boolean doOutput;
            boolean isFound = false;
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
                            isFound = true;
                            doOutput = false;
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
                if(isFound) {
                    break;
                }
            }

            outputStream.close();
            return new String(outputStream.toByteArray());
        }
    }
}
