/*
* Assuming that most heights vary between -100 and 100 nm among nanostructured surfaces.
* Splitting space [-100nm,100nm] to even subspaces and  labelling them with Latin letters.
*/

package encode;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramGraph;
import input_load.CSVRead;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

public class Main {
    static int SpacesNo = 1;
    static int Scale = 0;

    static Vector<DocumentNGramGraph> NGGs;

    public static void main(String[] argv) throws IOException {
        
        String csvFile = "";
        String out_filename = "";
        int out_flag = 0;

        for (int i=0 ; i<argv.length ; i++) {
            if (argv[i].equals("-in")) {
                csvFile = argv[++i];
            }
            if (argv[i].equals("-z")) { // number of spaces to divide [-100nm,100nm] into
                SpacesNo = Integer.parseInt(argv[++i]);
                if( SpacesNo>26 && (SpacesNo%2 != 0) ){
                    System.out.println("Please provide different number of spaces");
                }

            }
            // in case input is not measured in nm
            // e.g. to multiply all by 10^1: -scale 1
            if (argv[i].equals("-scale")) {
                Scale = Integer.parseInt(argv[++i]);
            }
            // print text to output file
            if( argv[i].equals("-out") ){
                out_filename = argv[++i];
                out_flag = 1;

                // check if file exists
                File f = new File(out_filename);
                // erase content if exists
                if(f.exists() && !f.isDirectory()) {
                    FileWriter writer = new FileWriter(out_filename);
                    writer.write("");
                    writer.close();
                }
            }
        }

        NGGs = new Vector<>();

        CSVRead reader = new CSVRead(csvFile,Scale);
        Encoder encoder = new Encoder(SpacesNo, reader.SurfTable.get(0));
        for (int i=1; i<reader.SurfTable.size(); i++) { // for all surfaces in file

            // encode in text and print surface
            encoder.InText();
            if( out_flag==0 ){ // standard output
                encoder.printText();
            } else { // file
                try{
                    FileWriter writer = new FileWriter(out_filename,true);
                    encoder.printText(writer);
                } catch (IOException ex){
                    System.out.println("There was a problem creating/writing to the file");
                    ex.printStackTrace();
                }
            }

            encoder.changeSurface(reader.SurfTable.get(i)); // next surface to encode
        }
        // print last surface
        encoder.InText();
        if( out_flag==0 ){ // standard output
            encoder.printText();
        } else { // file
            try{
                FileWriter writer = new FileWriter(out_filename,true);
                encoder.printText(writer);
            } catch (IOException ex){
                System.out.println("There was a problem creating/writing to the file");
                ex.printStackTrace();
            }
        }

        System.out.println();

    }
}