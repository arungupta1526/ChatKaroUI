package com.prem.chatkaroui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, zero-dependency Markdown → SpannableStringBuilder parser.
 *
 * Supported syntax (WhatsApp / Telegram subset):
 * **bold** → bold
 * *italic* → italic
 * _italic_ → italic
 * __underline__ → (rendered as italic+bold for compat — no UnderlineSpan clash)
 * ~~strikethrough~~ → strikethrough
 * `inline code` → monospace + light grey background
 * ```code block``` → monospace + light grey background (multi-line)
 * [text](url) → clickable URLSpan
 * # Heading 1 → large bold
 * ## Heading 2 → medium bold
 * ### Heading 3 → slightly larger bold
 * > blockquote → indented italic with accent color
 * - item / * item → bullet point (•)
 * \n → newline (preserved)
 *
 * Processing order matters: code blocks first (to prevent inner markers being
 * parsed), then inline code, then all other patterns.
 */
public class MarkdownParser {

        // ── Compiled patterns (compiled once, reused) ────────────────────────────

        private static final Pattern P_CODE_BLOCK = Pattern.compile("```([\\s\\S]*?)```");
        private static final Pattern P_CODE_INLINE = Pattern.compile("`([^`]+)`");
        private static final Pattern P_BOLD_STAR = Pattern.compile("\\*\\*(.+?)\\*\\*");
        private static final Pattern P_BOLD_UNDER = Pattern.compile("__(.+?)__");
        private static final Pattern P_ITALIC_STAR = Pattern.compile("\\*(?!\\*)(.+?)(?<!\\*)\\*");
        private static final Pattern P_ITALIC_UNDER = Pattern.compile("_(?!_)(.+?)(?<!_)_");
        private static final Pattern P_STRIKE = Pattern.compile("~~(.+?)~~");
        private static final Pattern P_LINK = Pattern.compile("\\[([^\\[\\]]+)\\]\\(([^()]+)\\)");
        private static final Pattern P_H1 = Pattern.compile("(?m)^# (.+)$");
        private static final Pattern P_H2 = Pattern.compile("(?m)^## (.+)$");
        private static final Pattern P_H3 = Pattern.compile("(?m)^### (.+)$");
        private static final Pattern P_BLOCKQUOTE = Pattern.compile("(?m)^> (.+)$");
        private static final Pattern P_BULLET = Pattern.compile("(?m)^[\\-\\*] (.+)$");

        // Placeholder used during code-block protection
        private static final String CODE_PLACEHOLDER_PREFIX = "\u0000CODE\u0000";

        // Add pre-compiled patterns for heading/blockquote replacement
        private static final Pattern P_H1_MARKER = Pattern
                        .compile(Pattern.quote("\u0002H1\u0002") + "(.*?)" + Pattern.quote("\u0002"));
        private static final Pattern P_H2_MARKER = Pattern
                        .compile(Pattern.quote("\u0002H2\u0002") + "(.*?)" + Pattern.quote("\u0002"));
        private static final Pattern P_H3_MARKER = Pattern
                        .compile(Pattern.quote("\u0002H3\u0002") + "(.*?)" + Pattern.quote("\u0002"));
        private static final Pattern P_BQ_MARKER = Pattern
                        .compile(Pattern.quote("\u0002BQ\u0002") + "(.*?)" + Pattern.quote("\u0002"));

        // ────────────────────────────────────────────────────────────────────────

        /**
         * Parse {@code markdown} and return a {@link SpannableStringBuilder} ready
         * to be set on a {@code TextView}.
         */
        public static SpannableStringBuilder parse(String markdown) {
                if (markdown == null || markdown.isEmpty()) {
                        return new SpannableStringBuilder("");
                }

                // Step 1: protect code blocks from inner parsing
                java.util.List<String> codeBlocks = new java.util.ArrayList<>();
                StringBuffer sb1 = new StringBuffer();
                Matcher m = P_CODE_BLOCK.matcher(markdown);
                while (m.find()) {
                        codeBlocks.add(m.group(1));
                        m.appendReplacement(sb1, CODE_PLACEHOLDER_PREFIX + (codeBlocks.size() - 1) + "\u0000");
                }
                m.appendTail(sb1);
                String protected1 = sb1.toString();

                // Step 2: protect inline code
                java.util.List<String> inlineCodes = new java.util.ArrayList<>();
                StringBuffer sb2 = new StringBuffer();
                Matcher m2 = P_CODE_INLINE.matcher(protected1);
                while (m2.find()) {
                        inlineCodes.add(m2.group(1));
                        m2.appendReplacement(sb2, "\u0001CODE\u0001" + (inlineCodes.size() - 1) + "\u0001");
                }
                m2.appendTail(sb2);
                String protected2 = sb2.toString();

                // Step 3: apply line-level transforms (headings, bullets, blockquotes)
                String lineParsed = applyLinePatterns(protected2);

                // Step 4: build SpannableStringBuilder and apply inline spans
                SpannableStringBuilder ssb = new SpannableStringBuilder(lineParsed);
                applyInlineSpans(ssb, codeBlocks, inlineCodes);

                return ssb;
        }

        // ── Line-level patterns ──────────────────────────────────────────────────

        private static String applyLinePatterns(String text) {
                // Order: H3 before H2 before H1 (longer prefix first)
                text = P_H3.matcher(text).replaceAll("\u0002H3\u0002$1\u0002");
                text = P_H2.matcher(text).replaceAll("\u0002H2\u0002$1\u0002");
                text = P_H1.matcher(text).replaceAll("\u0002H1\u0002$1\u0002");
                text = P_BLOCKQUOTE.matcher(text).replaceAll("\u0002BQ\u0002$1\u0002");
                text = P_BULLET.matcher(text).replaceAll("• $1");
                return text;
        }

        // ── Inline span application ──────────────────────────────────────────────

        private static void applyInlineSpans(SpannableStringBuilder ssb,
                        java.util.List<String> codeBlocks,
                        java.util.List<String> inlineCodes) {
                applyPattern(ssb, P_LINK, (s, start, end, groups) -> {
                        // groups[0]=display text, groups[1]=url
                        ssb.replace(start, end, groups[0]);
                        ssb.setSpan(new URLSpan(groups[1]), start, start + groups[0].length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                });
                applyPattern(ssb, P_BOLD_STAR,
                                (s, start, end, groups) -> wrapSpan(ssb, start, end, groups[0],
                                                new StyleSpan(Typeface.BOLD)));
                applyPattern(ssb, P_BOLD_UNDER,
                                (s, start, end, groups) -> wrapSpan(ssb, start, end, groups[0],
                                                new StyleSpan(Typeface.BOLD)));
                applyPattern(ssb, P_ITALIC_STAR,
                                (s, start, end, groups) -> wrapSpan(ssb, start, end, groups[0],
                                                new StyleSpan(Typeface.ITALIC)));
                applyPattern(ssb, P_ITALIC_UNDER,
                                (s, start, end, groups) -> wrapSpan(ssb, start, end, groups[0],
                                                new StyleSpan(Typeface.ITALIC)));
                applyPattern(ssb, P_STRIKE,
                                (s, start, end, groups) -> wrapSpan(ssb, start, end, groups[0],
                                                new StrikethroughSpan()));

                // Heading markers
                replaceHeadingMarker(ssb, "\u0002H1\u0002", "\u0002", 1.5f);
                replaceHeadingMarker(ssb, "\u0002H2\u0002", "\u0002", 1.3f);
                replaceHeadingMarker(ssb, "\u0002H3\u0002", "\u0002", 1.1f);

                // Blockquote markers
                replaceBlockquoteMarker(ssb, "\u0002BQ\u0002", "\u0002");

                String str = ssb.toString();
                java.util.regex.Matcher mIC = java.util.regex.Pattern.compile("\u0001CODE\u0001(\\d+)\u0001")
                                .matcher(str);
                java.util.List<int[]> icMatches = new java.util.ArrayList<>();
                java.util.List<Integer> icIndices = new java.util.ArrayList<>();
                while (mIC.find()) {
                        icMatches.add(new int[] { mIC.start(), mIC.end() });
                        icIndices.add(Integer.parseInt(mIC.group(1)));
                }
                for (int j = icMatches.size() - 1; j >= 0; j--) {
                        int[] range = icMatches.get(j);
                        String code = inlineCodes.get(icIndices.get(j));
                        ssb.replace(range[0], range[1], code);
                        ssb.setSpan(new TypefaceSpan("monospace"), range[0], range[0] + code.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ssb.setSpan(new BackgroundColorSpan(0xFFEEEEEE), range[0], range[0] + code.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                str = ssb.toString();
                java.util.regex.Matcher mCB = java.util.regex.Pattern
                                .compile(java.util.regex.Pattern.quote(CODE_PLACEHOLDER_PREFIX) + "(\\d+)\u0000")
                                .matcher(str);
                java.util.List<int[]> cbMatches = new java.util.ArrayList<>();
                java.util.List<Integer> cbIndices = new java.util.ArrayList<>();
                while (mCB.find()) {
                        cbMatches.add(new int[] { mCB.start(), mCB.end() });
                        cbIndices.add(Integer.parseInt(mCB.group(1)));
                }
                for (int j = cbMatches.size() - 1; j >= 0; j--) {
                        int[] range = cbMatches.get(j);
                        String code = "\n" + codeBlocks.get(cbIndices.get(j)).trim() + "\n";
                        ssb.replace(range[0], range[1], code);
                        ssb.setSpan(new TypefaceSpan("monospace"), range[0], range[0] + code.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ssb.setSpan(new BackgroundColorSpan(0xFFEEEEEE), range[0], range[0] + code.length(),
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
        }

        // ── Span helpers ─────────────────────────────────────────────────────────

        private interface SpanApplier {
                void apply(SpannableStringBuilder ssb, int start, int end, String[] groups);
        }

        /**
         * Finds all occurrences of {@code pattern} in {@code ssb}, calls
         * {@code applier} for each (working backwards to keep indices stable).
         * Applier receives the raw match bounds and captured groups.
         */
        private static void applyPattern(SpannableStringBuilder ssb,
                        Pattern pattern, SpanApplier applier) {
                // Work on a plain-string snapshot; apply backwards
                java.util.List<int[]> matches = new java.util.ArrayList<>();
                java.util.List<String[]> groupsList = new java.util.ArrayList<>();
                Matcher m = pattern.matcher(ssb.toString());
                while (m.find()) {
                        matches.add(new int[] { m.start(), m.end() });
                        String[] groups = new String[m.groupCount()];
                        for (int g = 0; g < m.groupCount(); g++)
                                groups[g] = m.group(g + 1);
                        groupsList.add(groups);
                }
                // Apply in reverse order so replacements don't shift earlier indices
                for (int i = matches.size() - 1; i >= 0; i--) {
                        int[] range = matches.get(i);
                        applier.apply(ssb, range[0], range[1], groupsList.get(i));
                }
        }

        /** Replaces [start,end] with inner text and applies span to the inner text. */
        private static void wrapSpan(SpannableStringBuilder ssb,
                        int start, int end,
                        String innerText, Object span) {
                ssb.replace(start, end, innerText);
                ssb.setSpan(span, start, start + innerText.length(),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private static void replaceHeadingMarker(SpannableStringBuilder ssb,
                        String startMarker,
                        String endMarker,
                        float relativeSize) {
                String str = ssb.toString();
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                                java.util.regex.Pattern.quote(startMarker) + "(.*?)"
                                                + java.util.regex.Pattern.quote(endMarker));
                java.util.regex.Matcher m = p.matcher(str);
                java.util.List<int[]> matches = new java.util.ArrayList<>();
                java.util.List<String> texts = new java.util.ArrayList<>();
                while (m.find()) {
                        matches.add(new int[] { m.start(), m.end() });
                        texts.add(m.group(1));
                }
                for (int i = matches.size() - 1; i >= 0; i--) {
                        int[] range = matches.get(i);
                        String text = texts.get(i);
                        ssb.replace(range[0], range[1], text + "\n");
                        ssb.setSpan(new StyleSpan(Typeface.BOLD),
                                        range[0], range[0] + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ssb.setSpan(new RelativeSizeSpan(relativeSize),
                                        range[0], range[0] + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
        }

        private static void replaceBlockquoteMarker(SpannableStringBuilder ssb,
                        String startMarker,
                        String endMarker) {
                String str = ssb.toString();
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                                java.util.regex.Pattern.quote(startMarker) + "(.*?)"
                                                + java.util.regex.Pattern.quote(endMarker));
                java.util.regex.Matcher m = p.matcher(str);
                java.util.List<int[]> matches = new java.util.ArrayList<>();
                java.util.List<String> texts = new java.util.ArrayList<>();
                while (m.find()) {
                        matches.add(new int[] { m.start(), m.end() });
                        texts.add(m.group(1));
                }
                for (int i = matches.size() - 1; i >= 0; i--) {
                        int[] range = matches.get(i);
                        String text = "  " + texts.get(i);
                        ssb.replace(range[0], range[1], text + "\n");
                        ssb.setSpan(new StyleSpan(Typeface.ITALIC),
                                        range[0], range[0] + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        ssb.setSpan(new ForegroundColorSpan(0xFF888888),
                                        range[0], range[0] + text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
        }
}
