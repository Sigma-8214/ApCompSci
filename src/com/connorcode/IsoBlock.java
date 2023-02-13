package com.connorcode;

import paintingcanvas.Canvas;
import paintingcanvas.drawable.Polygon;
import paintingcanvas.drawable.Rectangle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class IsoBlock {
    static final int PIXEL_SIZE = 4;
    static final int PIXEL_WIDTH = 8;
    static final float PIXEL_HEIGHT = 4.7f;
    static final int SIDE_LEN = (int) (Math.sqrt(2) * PIXEL_SIZE) - 2;
    static final ClassLoader loader = IsoBlock.class.getClassLoader();
    static final List<String> BLOCKS = new BufferedReader(new InputStreamReader(
            Objects.requireNonNull(loader.getResourceAsStream("resources/IsoBlock/textures.txt")))).lines()
            .filter(x -> !x.startsWith("#")).collect(Collectors.toUnmodifiableList());
    static Canvas _canvas = new Canvas(700, 500, "IsoBlock - Connor Slade");

    public static void main(String[] args) {
        // enable dark mode
        new Rectangle(0, 0, 10000, 10000, Color.BLACK);

        var textures = BLOCKS.stream()
                .map(i -> new Texture(loader.getResource("resources/IsoBlock/" + i)))
                .collect(Collectors.toList());

        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++) {
                var index = y * 4 + x;
                if (index >= textures.size()) break;
                new Block(textures.get(index)).draw().translate(x * 390 + (y % 2 == 0 ? 0 : 390 / 2), y * 343);
            }
    }

    static Color darken(Color color, float light) {
        return new Color((int) (color.getRed() * light), (int) (color.getGreen() * light),
                (int) (color.getBlue() * light));
    }

    static int[][] top(int x, int y) {
        var wh = PIXEL_WIDTH / 2;
        var hh = PIXEL_HEIGHT / 2;
        var size = PIXEL_SIZE;
        return new int[][]{
                {
                        (x - y) * wh,
                        (int) ((x + y) * hh)
                },
                {
                        (size + x - y) * wh,
                        (int) ((size + x + y) * hh)
                },
                {
                        (x - y) * wh,
                        (int) ((2 * size + x + y) * hh)
                },
                {
                        (x - y - size) * wh,
                        (int) ((x + y + size) * hh)
                }
        };
    }

    static int[][] left(int x, int y) {
        var wh = PIXEL_WIDTH / 2;
        var hh = PIXEL_HEIGHT / 2;
        var size = PIXEL_SIZE;
        return new int[][]{
                {
                        (x - y) * wh,
                        (int) ((2 * size + x + y) * hh)
                },
                {
                        (x - y) * wh,
                        (int) ((3 * size + x + y + SIDE_LEN) * hh)
                },
                {
                        (x - y - size) * wh,
                        (int) ((2 * size + x + y + SIDE_LEN) * hh)
                },
                {
                        (x - y - size) * wh,
                        (int) ((x + y + size) * hh)
                },
                };
    }

    static int[][] right(int x, int y) {
        var wh = PIXEL_WIDTH / 2;
        var hh = PIXEL_HEIGHT / 2;
        var size = PIXEL_SIZE;
        return new int[][]{
                {
                        (size + x - y) * wh,
                        (int) ((size + x + y) * hh)
                },
                {
                        (x - y) * wh,
                        (int) ((2 * size + x + y) * hh)
                },
                {
                        (x - y) * wh,
                        (int) ((3 * size + x + y + SIDE_LEN) * hh)
                },
                {
                        (size + x - y) * wh,
                        (int) ((2 * size + x + y + SIDE_LEN) * hh)
                },
                };
    }

    static class Block {
        Polygon[] elements;
        Texture texture;

        Block(Texture texture) {
            this.texture = texture;
        }

        Block draw() {
            var ei = 0;
            elements = new Polygon[16 * 16 * 3];

            // left, right, top
            for (var xi = 0; xi < 16; xi++) {
                for (var yi = 0; yi < 16; yi++) {
                    elements[ei++] = new Polygon(left((xi + yi) * SIDE_LEN, yi * SIDE_LEN))
                            .setColor(darken(new Color(texture.get(xi, yi)), 0.7f))
                            .moveHorizontal((int) (-PIXEL_WIDTH * 16 * Math.sqrt(2)) + 2);

                    elements[ei++] = new Polygon(right(yi * SIDE_LEN, (xi + yi) * SIDE_LEN))
                            .setColor(darken(new Color(texture.get(15 - xi, yi)), 0.55f))
                            .moveHorizontal((int) (PIXEL_WIDTH * 16 * Math.sqrt(2)) - 2);

                    elements[ei++] = new Polygon(top(xi * SIDE_LEN, yi * SIDE_LEN))
                            .setColor(texture.getTop(xi, yi))
                            .moveVertical((int) (16 * -PIXEL_HEIGHT * Math.sqrt(2)));
                }
            }

            return this;
        }

        void translate(int x, int y) {
            for (var i : elements)
                i.move(x, y);
        }
    }

    static class Texture {
        int[] texture;
        int[] topTexture;

        // Load texture from file
        Texture(URL file) {
            try {
                this.texture = load(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try {
                this.topTexture = load(asTop(file));
                System.out.println(" \\  FOUND TOP");
            } catch (IOException ignored) {
                this.topTexture = new int[0];
            }
        }

        private int[] load(URL file) throws IOException {
            BufferedImage image;
            image = ImageIO.read(file);
            System.out.printf("[*] Loaded %s - (%d x %d)\n", file, image.getWidth(), image.getHeight());
            assert image.getWidth() == 16 && image.getHeight() == 16;
            return image.getRGB(0, 0, 16, 16, null, 0, 16);
        }

        private URL asTop(URL file) {
            var filePath = file.getPath();
            var extensionIndex = filePath.lastIndexOf(".");
            var newFilePath = filePath.substring(0, extensionIndex) + "_top" + filePath.substring(extensionIndex);
            try {
                return new URL(file.getProtocol(), file.getHost(), file.getPort(), newFilePath);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }


        int get(int x, int y) {
            return texture[y * 16 + x];
        }

        int getTop(int x, int y) {
            if (topTexture.length == 0) return get(x, y);
            return topTexture[y * 16 + x];
        }
    }
}
