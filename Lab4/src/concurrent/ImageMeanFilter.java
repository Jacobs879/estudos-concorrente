import java.awt.List;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import java.io.IOException;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image.
 * The mean filter is used to smooth images by averaging the pixel values
 * in a neighborhood defined by a kernel size.
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * ImageMeanFilter.applyMeanFilter("input.jpg", "output.jpg", 3);
 * }
 * </pre>
 * 
 * <p>Supported image formats: JPG, PNG</p>
 * 
 * <p>Author: temmanuel@comptuacao.ufcg.edu.br</p>
 */
public class ImageMeanFilter {

   

    //1-aplico uma especie de buffer de threads e substituo os pixels apenas no final.
    //2-Aplico un filtro para cada linha ao mesmo tempo que substituo

    //Vou usar 2
    /*
     * Crio um multiplex que limita a quantidade maxima de threads
     * Crio um mutex para acessar o buffer de resultado - ou não
     * Vou criar um thread por linha  e executar o algortimo de filtro
     * Vou quarda o resultado em um array e faço uma substituição total por linha
     * 
     * Ordem:
     * 1-For na altura
     * 2-pego uma thread para cada pixel da linha
     * 3-executo todo o for e quardo em um array
     * 4-depois do array eu faço outro for na linha para substituir
     * 5- faço tudo denovo até acabar a altura e escrevo a imagem
     * 
     * 
     * 
     */
    
    /**
     * Applies mean filter to an image
     * 
     * @param inputPath  Path to input image
     * @param outputPath Path to output image 
     * @param kernelSize Size of mean kernel
     * @throws IOException If there is an error reading/writing
     */
    public static void applyMeanFilter(String inputPath, String outputPath, int kernelSize, int numThreads) throws IOException {

        Semaphore mutex = new Semaphore(1);
        Semaphore multiplex = new Semaphore(numThreads);

        // Load image
        BufferedImage originalImage = ImageIO.read(new File(inputPath));
        
        // Create result image
        BufferedImage filteredImage = new BufferedImage(
            originalImage.getWidth(), 
            originalImage.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        
        // Image processing
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        int[][] lineResp = new int[height][width];

        ArrayList listaThread = new ArrayList();

        // Process each pixel
        for (int y = 0; y < height; y++) {
            int count = 0;
            for (int x = 0; x < width; x++) {

                multiplex.wait();
                CalculateNeighborhoodAverageThread foo = new CalculateNeighborhoodAverageThread(originalImage, x, y, kernelSize, count);
                Thread t = new Thread(foo);
                listaThread.add(t);
                t.start();
                
                
                
            }
            
            //dorme
            for(Thread x : listaThread){

                x.join();
            }

            //susbtitui os pixelsnumThreads
            for (int x = 0; x < width; x++) {
                // Calculate neighborhood average

                int[] avgColor = lineResp[x];
                
                // Set filtered pixel
                filteredImage.setRGB(x, y, 
                    (avgColor[0] << 16) | 
                    (avgColor[1] << 8)  | 
                    avgColor[2]
                );
            }


        }
        
        // Save filtered image
        ImageIO.write(filteredImage, "jpg", new File(outputPath));
    }
    
    /**
     * Calculates average colors in a pixel's neighborhood
     * 
     * @param image      Source image
     * @param centerX    X coordinate of center pixel
     * @param centerY    Y coordinate of center pixel
     * @param kernelSize Kernel size
     * @return Array with R, G, B averages
     */
    private static int[] calculateNeighborhoodAverage(BufferedImage image, int centerX, int centerY, int kernelSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pad = kernelSize / 2;

        
        // Arrays for color sums
        long redSum = 0, greenSum = 0, blueSum = 0;
        int pixelCount = 0;
        
        // Process neighborhood
        for (int dy = -pad; dy <= pad; dy++) {
            for (int dx = -pad; dx <= pad; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;
                
                // Check image bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    // Get pixel color
                    int rgb = image.getRGB(x, y);
                    
                    // Extract color componentsnumThreads
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    
                    // Sum colors
                    redSum += red;
                    greenSum += green;
                    blueSum += blue;
                    pixelCount++;
                }
            }
        }
        
        // Calculate average
        return new int[] {
            (int)(redSum / pixelCount),
            (int)(greenSum / pixelCount),
            (int)(blueSum / pixelCount)
        };
    }
    
    /**
     * Main method for demonstration
     * 
     * Usage: java ImageMeanFilter <input_file>
     * 
     * Arguments:
     *   input_file - Path to the input image file to be processedint
     *                Supported formats: JPG, PNG
     * 
     * Example:
     *   java ImageMeanFilter input.jpg
     * 
     * The program will generate a filtered output image named "filtered_output.jpg"
     * using a 7x7 mean filter kernel
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java ImageMeanFilter <input_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        try {
            applyMeanFilter(inputFile, "filtered_output.jpg", 7, args[1]);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }



    public class CalculateNeighborhoodAverageThread implements Runnable{

        private BufferedImage image;
        private int centerX;
        private int centerY;
        private int kernelSize;

        public CalculateNeighborhoodAverageThread(BufferedImage image, int centerX, int centerY, int kernelSize, int count){

            this.image = image;
            this.centerX = centerX;
            this.centerY = centerY;
            this.kernelSize = kernelSize;
            this.count = count;
        }

        @Override
        public void run(){

            int[] avgColor = calculateNeighborhoodAverage(image, centerX, centerY, kernelSize);
            lineResp[count] = avgColor;


        }

    }
}
