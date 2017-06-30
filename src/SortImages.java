import javax.swing.*;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * reads the directory, saves all the fotos in an arrayList, saves all the paths in a seperate arrayList.
 * every foto which was taken within a given time will be pulled in a seperate folder.
 * <p>
 * Created by Chrissi on 11.05.2017.
 */
class SortImages {
    //arraylist where the fotos are saved
    private static ArrayList<Foto> fotos;
    //arrayList where the paths of subdirectories are saved
    private static ArrayList<Path> subDirs;
    //the path to the directory of the fotos to sort
    private static Path sourcePath;
    //distance between Takes of the images
    private static int distance;

    static void sort(int distanceBetweenTakes, Path dir) throws IOException {
        fotos = new ArrayList<>();
        subDirs = new ArrayList<>();
        sourcePath = dir;
        distance = distanceBetweenTakes;


        //Background task for loading images.
        SwingWorker worker = new SwingWorker<ArrayList<Foto>, Void>() {
            @Override
            public ArrayList<Foto> doInBackground() {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sourcePath)) {
                    for (Path file : directoryStream) {
                        if (!Files.isDirectory(file)) {
                            if (file.getFileName().toString().toLowerCase().endsWith("jpg") || file.getFileName().toString().toLowerCase().endsWith("jpeg") ||
                                    file.getFileName().toString().toLowerCase().endsWith("mp4") || file.getFileName().toString().toLowerCase().endsWith("mpeg4")) {
                                fotos.add(new Foto(sourcePath.toString(), file.getFileName().toString()));
                            }   //System.out.println(file.getFileName());
                        }
                    }
                } catch (IOException | DirectoryIteratorException e) {
                    e.printStackTrace();
                    System.err.println(e);
                }
                System.out.println(fotos.size() + "images / videos added to Foto ArrayList");

                Foto[] content = (Foto[]) fotos.toArray();
                System.out.println("Foto ArrayList before sorting: " + content.length + " items");
                //System.out.println(Arrays.toString(content));
                // fotos.sort(Comparator.comparing(Foto::getCreationDateTime));
                Collections.sort(fotos, (s1, s2) -> {
                    try {
                        return s2.getCreationDateTime().compareTo(s1.getCreationDateTime());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 1;
                    }
                });

                Foto[] cntAftrSrt = (Foto[]) fotos.toArray();
                System.out.println("Foto ArrayList after sorting: " + cntAftrSrt.length + " items");
                //System.out.println(Arrays.toString(cntAftrSrt));
                return fotos;
            }

            @Override
            public void done() {
                //Remove the "Loading images" label.
                for (int i = 0; i < fotos.size(); i++) {
                    if (i > 0) {
                        //System.out.println("compareFotos called " + (i + 1) + " times");
                        try {
                            compareFotos(fotos.get(i - 1), fotos.get(i));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        };

        worker.execute();

       /* Thread sortCollection = new SortCollThread();
        sortCollection.run();
        if (sortCollection.isAlive()) {
            System.out.println("sortCollection Thread still in progress...");
        } else {


        }*/
       /*
       SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
           @Override
           protected Void doInBackground() throws Exception {

               return null;
           }
           protected void done(){
               for (int i = 0; i < fotos.size(); i++) {
                   if (i > 0) {
                       compareFotos(fotos.get(i - 1), fotos.get(i));
                   }
               }
           }
       };
        sw.execute();*/
    }

    //c
    // creates new folder and moves foto into new folder

    /**
     * Compares two fotos, checks if they are taken within given time.
     * Checks if new directory exists. If not, calls createNewSubdir function
     * moves file(s) to new directory
     *
     * @param pic1 foto 1 to compare
     * @param pic2 foto 2 to compare
     */
    private static void compareFotos(Foto pic1, Foto pic2) throws IOException {
        //reads date and time when foto was created
        Date pic1Date = pic1.getCreationDateTime();
        Date pic2Date = pic2.getCreationDateTime();
        //output format of date and time
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH-mm");
        //System.out.println("Pic1: "+df.format(pic1Date)+ ", pic2: "+df.format(pic2Date));

        //creates new subdirectory if there's none yet. uses the dateFormatter
        // to name the subdirectory by the date the foto was made
        if (subDirs.size() < 1) {
            createNewSubDir(df.format(pic1Date));
        }

        //checks if fotos were created within given time. if it is so, files will be moved in new directory
        //if not, another subdirectory will be created

        if ((pic2Date.getTime() - pic1Date.getTime()) < (distance * 60 * 60 * 1000)) { // 6 Std. getTime liefert Zeit in Millisekunden zurück
            //System.out.println("Subdirs.path = "+subdirs.get(subdirs.size()-1));

            pic1.moveFile(subDirs.get((subDirs.size() - 1)).toString());
            //fotos.remove(0);
            pic2.moveFile(subDirs.get((subDirs.size()) - 1).toString());

        } else {
            //if i don't add this option, last foto will not be sorted!!!
            pic1.moveFile(subDirs.get((subDirs.size() - 1)).toString());
            //call function to create new subDir
            createNewSubDir(df.format(pic2Date));
            //System.out.println("createt new subDir" + df.format(pic2Date));
            //createNewSubDir(sourcePath, df.format(pic2Date), subdirs); */
        }
    }

    /**
     * Checks if new directory exists. if not, creates a new directory
     * adds foto to subdirs arrayList
     *
     * @param newSubDir Name of new subdirectory that needs to be created
     */
    private static void createNewSubDir(String newSubDir) {
        //this.sourcePath = sourcePath;
        // SortImages.newSubDir = newSubDir;
        Path newDir = Paths.get(sourcePath.toString(), newSubDir);
        if (!Files.exists(newDir)) {
            try {

                Files.createDirectory(newDir);
                //newDir = Files.setAttribute(newDir, "dos:readonly", false);
                //DosFileAttributes attr = Files.readAttributes(newDir, DosFileAttributes.class);
                //System.out.println("is readOnly: "+attr.isReadOnly());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        subDirs.add(newDir);
    }

    /**
     * walks through the ArrayList of subDirs and moves every file back to sourceDir.
     * After content was moved, directory will be deleted
     */
    static void undoChanges() {
        for (Path subdir : subDirs) {
            //gets the content of the subdirectory
            try (DirectoryStream<Path> directoryStreamSub = Files.newDirectoryStream(subdir)) {
                for (Path cont : directoryStreamSub) {
                    if (!Files.isDirectory(cont)) {
                        Foto file1 = new Foto(subdir.toString(), cont.getFileName().toString());
                        file1.moveFile(sourcePath.toString());//.renameTo(sourcePath + file.getName());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                //deletes the directory
                Files.delete(subdir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
/*
    private static class SortCollThread extends Thread {
        boolean stop = false;

        public void run() {

                return 0;
            });
            while (true) {

                if (stop) {
                    return;
                }
            }
        }*/

}