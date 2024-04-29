/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https:
 */
package jdk.internal.org.jline.utils;

import java.util.Locale;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static jdk.internal.org.jline.utils.AttributedStyle.*;


/**
 * Resolves named (or source-referenced) {@link AttributedStyle}.
 *
 * @since 3.6
 */
public class StyleResolver {

    private final Function<String, String> source;

    public StyleResolver(final Function<String, String> source) {
        this.source = requireNonNull(source);
    }

    /**
     * Returns the RGB color for the given name.
     * <p>
     * Bright color can be specified with: {@code !<color>} or {@code bright-<color>}.
     * <p>
     * Full xterm256 color can be specified with: {@code ~<color>}.
     * RGB colors can be specified with: {@code x<rgb>} or {@code #<rgb>} where {@code rgb} is
     * a 24 bits hexadecimal color.
     *
     * @param name the name of the color
     * @return color code, or {@code null} if unable to determine.
     */
    private static Integer colorRgb(String name) {
        name = name.toLowerCase(Locale.US);
        if (name.charAt(0) == 'x' || name.charAt(0) == '#') {
            try {
                return Integer.parseInt(name.substring(1), 16);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
           Integer color = color(name);
           if (color != null && color != -1) {
               color = Colors.DEFAULT_COLORS_256[color];
           }
           return color;
        }
    }

    /**
     * Returns the color identifier for the given name.
     * <p>
     * Bright color can be specified with: {@code !<color>} or {@code bright-<color>}.
     * <p>
     * Full xterm256 color can be specified with: {@code ~<color>}.
     *
     * @param name the name of the color
     * @return color code, or {@code null} if unable to determine.
     */
    private static Integer color(String name) {
        int flags = 0;

        if (name.equals("default")) {
            return -1;
        }
        else if (name.charAt(0) == '!') {
            name = name.substring(1);
            flags = BRIGHT;
        } else if (name.startsWith("bright-")) {
            name = name.substring(7);
            flags = BRIGHT;
        } else if (name.charAt(0) == '~') {
            name = name.substring(1);
            try {
                return Colors.rgbColor(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        switch (name) {
            case "black":
            case "k":
                return flags + BLACK;

            case "red":
            case "r":
                return flags + RED;

            case "green":
            case "g":
                return flags + GREEN;

            case "yellow":
            case "y":
                return flags + YELLOW;

            case "blue":
            case "b":
                return flags + BLUE;

            case "magenta":
            case "m":
                return flags + MAGENTA;

            case "cyan":
            case "c":
                return flags + CYAN;

            case "white":
            case "w":
                return flags + WHITE;
        }

        return null;
    }


    /**
     * Resolve the given style specification.
     * <p>
     * If for some reason the specification is invalid, then {@link AttributedStyle#DEFAULT} will be used.
     *
     * @param spec the specification
     * @return the style
     */
    public AttributedStyle resolve(final String spec) {
        requireNonNull(spec);


        int i = spec.indexOf(":-");
        if (i != -1) {
            String[] parts = spec.split(":-");
            return resolve(parts[0].trim(), parts[1].trim());
        }

        return apply(DEFAULT, spec);
    }

    /**
     * Resolve the given style specification.
     * <p>
     * If this resolves to {@link AttributedStyle#DEFAULT} then given default specification is used if non-null.
     *
     * @param spec the specification
     * @param defaultSpec the default specifiaction
     * @return the style
     */
    public AttributedStyle resolve(final String spec, final String defaultSpec) {
        requireNonNull(spec);


        AttributedStyle style = apply(DEFAULT, spec);
        if (style == DEFAULT && defaultSpec != null) {
            style = apply(style, defaultSpec);
        }
        return style;
    }

    /**
     * Apply style specification.
     *
     * @param style the style to apply to
     * @param spec the specification
     * @return the new style
     */
    private AttributedStyle apply(AttributedStyle style, final String spec) {

        for (String item : spec.split(",")) {
            item = item.trim();
            if (item.isEmpty()) {
                continue;
            }

            if (item.startsWith(".")) {
                style = applyReference(style, item);
            } else if (item.contains(":")) {
                style = applyColor(style, item);
            } else if (item.matches("[0-9]+(;[0-9]+)*")) {
                style = applyAnsi(style, item);
            } else {
                style = applyNamed(style, item);
            }
        }

        return style;
    }

    private AttributedStyle applyAnsi(final AttributedStyle style, final String spec) {

        return new AttributedStringBuilder()
                .style(style)
                .ansiAppend("\033[" + spec + "m")
                .style();
    }

    /**
     * Apply source-referenced named style.
     *
     * @param style the style to apply to
     * @param spec the specification
     * @return the new style
     */
    private AttributedStyle applyReference(final AttributedStyle style, final String spec) {

        if (spec.length() == 1) {
        } else {
            String name = spec.substring(1);
            String resolvedSpec = source.apply(name);
            if (resolvedSpec != null) {
                return apply(style, resolvedSpec);
            }
        }

        return style;
    }

    /**
     * Apply default named styles.
     *
     * @param style the style to apply to
     * @param name the named style
     * @return the new style
     */
    private AttributedStyle applyNamed(final AttributedStyle style, final String name) {


        switch (name.toLowerCase(Locale.US)) {
            case "default":
                return DEFAULT;

            case "bold":
                return style.bold();

            case "faint":
                return style.faint();

            case "italic":
                return style.italic();

            case "underline":
                return style.underline();

            case "blink":
                return style.blink();

            case "inverse":
                return style.inverse();

            case "inverse-neg":
            case "inverseneg":
                return style.inverseNeg();

            case "conceal":
                return style.conceal();

            case "crossed-out":
            case "crossedout":
                return style.crossedOut();

            case "hidden":
                return style.hidden();

            default:
                return style;
        }
    }


    /**
     * Apply color styles specification.
     *
     * @param style The style to apply to
     * @param spec  Color specification: {@code <color-mode>:<color-name>}
     * @return      The new style
     */
    private AttributedStyle applyColor(final AttributedStyle style, final String spec) {

        String[] parts = spec.split(":", 2);
        String colorMode = parts[0].trim();
        String colorName = parts[1].trim();

        Integer color;
        switch (colorMode.toLowerCase(Locale.US)) {
            case "foreground":
            case "fg":
            case "f":
                color = color(colorName);
                if (color == null) {
                    break;
                }
                return color >= 0 ? style.foreground(color) : style.foregroundDefault();

            case "background":
            case "bg":
            case "b":
                color = color(colorName);
                if (color == null) {
                    break;
                }
                return color >= 0 ? style.background(color) : style.backgroundDefault();

            case "foreground-rgb":
            case "fg-rgb":
            case "f-rgb":
                color = colorRgb(colorName);
                if (color == null) {
                    break;
                }
                return color >= 0 ? style.foregroundRgb(color) : style.foregroundDefault();

            case "background-rgb":
            case "bg-rgb":
            case "b-rgb":
                color = colorRgb(colorName);
                if (color == null) {
                    break;
                }
                return color >= 0 ? style.backgroundRgb(color) : style.backgroundDefault();

            default:
        }
        return style;
    }
}
