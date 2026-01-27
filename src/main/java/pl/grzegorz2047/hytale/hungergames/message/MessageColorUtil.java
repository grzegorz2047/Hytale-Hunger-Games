package pl.grzegorz2047.hytale.hungergames.message;

import com.hypixel.hytale.server.core.Message;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageColorUtil {
    static class Style {
        String color;
        boolean bold;
        boolean italic;

        Style copy() {
            Style s = new Style();
            s.color = this.color;
            s.bold = this.bold;
            s.italic = this.italic;
            return s;
        }
    }

    public static Message rawStyled(String input) {
        Message root = Message.empty();

        Pattern pattern = Pattern.compile(
                "<(color=([^>]+)|b|i)>(.*?)</(color|b|i)>",
                Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(input);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                root.insert(Message.raw(input.substring(lastEnd, matcher.start())));
            }

            String openTag = matcher.group(1);
            String color = matcher.group(2);
            String text = matcher.group(3);

            Message part = Message.raw(text);

            if ("b".equals(openTag)) {
                part.bold(true);
            } else if ("i".equals(openTag)) {
                part.italic(true);
            } else if (openTag.startsWith("color=")) {
                part.color(color);
            }

            root.insert(part);
            lastEnd = matcher.end();
        }

        if (lastEnd < input.length()) {
            root.insert(Message.raw(input.substring(lastEnd)));
        }

        return root;
    }

    public static Message rawStyledStack(String input) {
        Message root = Message.empty();


        Deque<Style> stack = new ArrayDeque<>();
        Style current = new Style();
        stack.push(current);

        StringBuilder buffer = new StringBuilder();

        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            if (c == '<') {
                // flush tekst
                if (!buffer.isEmpty()) {
                    Message part = Message.raw(buffer.toString());
                    applyStyle(part, current);
                    root.insert(part);
                    buffer.setLength(0);
                }

                int end = input.indexOf('>', i);
                if (end == -1) {
                    // niepoprawny tag → traktuj jak tekst
                    buffer.append(c);
                    i++;
                    continue;
                }

                String tag = input.substring(i + 1, end).trim();

                if (tag.startsWith("/")) {
                    // zamknięcie taga
                    if (stack.size() > 1) {
                        stack.pop();
                        current = stack.peek();
                    }
                } else {
                    // otwarcie taga
                    Style next = current.copy();

                    if (tag.equals("b")) {
                        next.bold = true;
                    } else if (tag.equals("i")) {
                        next.italic = true;
                    } else if (tag.startsWith("color=")) {
                        next.color = tag.substring("color=".length()).trim();
                    }

                    stack.push(next);
                    current = next;
                }

                i = end + 1;
            } else {
                buffer.append(c);
                i++;
            }
        }

        // flush końcówki
        if (!buffer.isEmpty()) {
            Message part = Message.raw(buffer.toString());
            applyStyle(part, current);
            root.insert(part);
        }

        return root;
    }

    private static void applyStyle(Message msg, Style  style) {

        if (style.color != null) {
            msg.color(style.color);
        }
        if (style.bold) {
            msg.bold(true);
        }
        if (style.italic) {
            msg.italic(true);
        }
    }


}
