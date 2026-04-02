package com.williamcallahan.chatclient;

import com.williamcallahan.tui4j.term.Clipboard;

public final class ClipboardSmokeProbe {

    private ClipboardSmokeProbe() {}

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: ClipboardSmokeProbe <probe-text>");
            System.exit(2);
        }
        System.out.println(Clipboard.tryCopy(args[0]));
    }
}
