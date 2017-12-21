package com.gy.woodpecker;

import com.gy.woodpecker.command.Commands;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import org.apache.commons.lang3.StringUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.gy.woodpecker.tools.GaStringUtils.DEFAULT_PROMPT;
import static java.io.File.separatorChar;
import static java.lang.System.getProperty;
import static jline.console.KeyMap.CTRL_D;
import static jline.internal.Preconditions.checkNotNull;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class WPConsole {

    private static final int EOT = 48;
    private static final byte EOF = -1;

    // 5分钟
    private static final int _1MIN = 60 * 1000;

    // 工作目录
    private static final String WORKING_DIR = getProperty("user.home");

    // 历史命令存储文件
    private static final String HISTORY_FILENAME = ".wp_history";

    private final ConsoleReader console;
    private final History history;
    private final Writer out;

    private final Socket socket;
    private BufferedWriter socketWriter;
    private BufferedReader socketReader;

    private volatile boolean isRunning;

    private volatile boolean flag = true;


    public WPConsole(InetSocketAddress address) throws IOException {

        this.console = initConsoleReader();
        this.history = initHistory();

        this.out = console.getOutput();
        this.history.moveToEnd();
        this.console.setHistoryEnabled(true);
        this.console.setHistory(history);
        this.console.setExpandEvents(false);
        this.socket = connect(address);
        // 初始化自动补全
        initCompleter();
        this.isRunning = true;
        activeConsoleReader();
        loopForWriter();
    }

    // jLine的自动补全
    private void initCompleter() {
        final SortedSet<String> commands = new TreeSet<String>();
        commands.addAll(Commands.getInstance().listCommands().keySet());

        console.addCompleter(new Completer() {
            @Override
            public int complete(String buffer, int cursor, List<CharSequence> candidates) {
                // buffer could be null
                checkNotNull(candidates);

                if (buffer == null) {
                    candidates.addAll(commands);
                } else {
                    String prefix = buffer;
                    if (buffer.length() > cursor) {
                        prefix = buffer.substring(0, cursor);
                    }
                    for (String match : commands.tailSet(prefix)) {
                        if (!match.startsWith(prefix)) {
                            break;
                        }

                        candidates.add(match);
                    }
                }

                if (candidates.size() == 1) {
                    candidates.set(0, candidates.get(0) + " ");
                }

                return candidates.isEmpty() ? -1 : 0;
            }

        });
    }


    private History initHistory() throws IOException {
        final File WORK_DIR = new File(WORKING_DIR);
        final File historyFile = new File(WORKING_DIR + separatorChar + HISTORY_FILENAME);
        if (WORK_DIR.canWrite()
                && WORK_DIR.canRead()
                && ((!historyFile.exists() && historyFile.createNewFile()) || historyFile.exists())) {
            return new FileHistory(historyFile);
        }
        return new MemoryHistory();
    }

    private ConsoleReader initConsoleReader() throws IOException {
        final ConsoleReader console = new ConsoleReader(System.in, System.out);

        console.getKeys().bind("" + CTRL_D, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    socketWriter.write("kill\n");
                    socketWriter.flush();
                } catch (Exception e1) {
                    WPConsole.this.err("write fail : %s", e1.getMessage());
                    shutdown();
                }
            }

        });

        return console;
    }


    /**
     * 激活网络
     */
    private Socket connect(InetSocketAddress address) throws IOException {
        final Socket socket = new Socket();
        socket.setSoTimeout(0);
        socket.connect(address, _1MIN);
        socket.setKeepAlive(true);
        socketWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return socket;
    }

    /**
     * 激活读线程
     */
    private void activeConsoleReader() {
        final Thread socketThread = new Thread("wp-console-reader-daemon") {

            private StringBuilder lineBuffer = new StringBuilder();

            @Override
            public void run() {
                try {

                    while (isRunning) {

                        final String line = console.readLine();
                        if (!flag) {
                            continue;
                        }

                        // 如果是\结尾，则说明还有下文，需要对换行做特殊处理
                        if (StringUtils.endsWith(line, "\\")) {
                            // 去掉结尾的\
                            lineBuffer.append(line.substring(0, line.length() - 1));
                            continue;
                        } else {
                            lineBuffer.append(line);
                        }

                        final String lineForWrite = lineBuffer.toString();
                        lineBuffer = new StringBuilder();

                        // flush if need
                        if (history instanceof Flushable) {
                            ((Flushable) history).flush();
                        }

                        console.setPrompt(EMPTY);
                        if (isNotBlank(lineForWrite)) {
                            socketWriter.write(lineForWrite + "\n");
                        } else {
                            socketWriter.write("\n");
                        }
                        socketWriter.flush();

                        flag = false;
                    }
                } catch (IOException e) {
                    err("read fail : %s", e.getMessage());
                    shutdown();
                }

            }

        };
        socketThread.setDaemon(true);
        socketThread.start();
    }


    private volatile boolean hackingForReDrawPrompt = true;

    private void loopForWriter() {

        try {
            while (isRunning) {
                final int c = socketReader.read();
                if(c == 1){
                    continue;
                }
                if (c == EOF) {
                    break;
                }
                if (c == 0) {
                    console.setPrompt(DEFAULT_PROMPT);
                    console.redrawLine();
                    //可以输入
                    flag = true;
                } else {
                    out.write(c);
                }
                out.flush();
            }
        } catch (IOException e) {
            err("write fail : %s", e.getMessage());
        } finally {
            shutdown();
        }

    }

    private void err(String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    /**
     * 关闭Console
     */
    private void shutdown() {
        isRunning = false;
        closeQuietly(socketWriter);
        closeQuietly(socketReader);
        closeQuietly(socket);
        console.shutdown();
    }

    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            args = new String[2];
            args[0] = "127.0.0.1";
            args[1] = "8889";
        }
        new WPConsole(new InetSocketAddress(args[0], Integer.parseInt(args[1])));
    }

}
