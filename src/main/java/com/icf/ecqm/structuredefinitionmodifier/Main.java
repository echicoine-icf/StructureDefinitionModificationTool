package com.icf.ecqm.structuredefinitionmodifier;

import java.io.*;
import java.util.*;

import com.google.gson.*;

public class Main {

    public static final String QICORE_KEYELEMENT = "qicore-keyelement";
    public static final String USCDI_REQUIREMENT = "uscdi-requirement";
    public static final String URL = "url";
    public static final String SNAPSHOT = "snapshot";
    public static final String ELEMENT = "element";
    public static final String EXTENSION = "extension";
    public static final String STRUCTURE_DEFINITION = "StructureDefinition";
    public static final String DIFFERENTIAL = "differential";

    public static void main(String[] args) {

        String inputFolder = "input" + File.separator + "profiles";
        String outputFolder = "output";

        File outputDir = new File(outputFolder);
        File[] outputFiles = outputDir.listFiles((dir, name) -> name.toLowerCase().startsWith(STRUCTURE_DEFINITION.toLowerCase()) &&
                name.toLowerCase().endsWith(".json"));

        if (outputFiles != null) {
            Map<String, List<String>> shortDescriptionFields = new HashMap<>();

            for (File outputFile : outputFiles) {
                System.out.println("\r\nProcessing " + outputFile.getAbsolutePath());
                List<String> shortDescriptionsToChange = getShortDescriptionsToChangeList(outputFile);
                if (!shortDescriptionsToChange.isEmpty()) {
                    shortDescriptionFields.put(outputFile.getName(), shortDescriptionsToChange);
                }
            }
            System.out.println("\r\n");
            System.out.println("Files that met criteria: " + String.join(",", shortDescriptionFields.keySet()));
            System.out.println("\r\n");
            File inputDir = new File(inputFolder);
            //now loop through files found to have descriptions changed:
            File[] inputFiles = inputDir.listFiles((dir, name) -> shortDescriptionFields.containsKey(name));

            System.out.println("Found matching files in " + inputFolder + ": " + Arrays.toString(inputFiles));
            System.out.println("\r\n");
            if (inputFiles != null) {
                for (File inputFile : inputFiles) {
                    List<String> shortDescriptionsToChangeInInputFile = shortDescriptionFields.get(inputFile.getName());
                    if (shortDescriptionsToChangeInInputFile != null && !shortDescriptionsToChangeInInputFile.isEmpty()) {

                        System.out.println("Processing short descriptions in " + inputFile.getAbsolutePath());
                        changeShortDescriptions(inputFile, shortDescriptionsToChangeInInputFile);
                    }
                }
            } else {
                System.err.println("Error: Unable to list files in the input folder.");
            }

            System.out.println("\r\n");

        } else {
            System.err.println("Error: Unable to list files in the output folder.");
        }

        System.out.println("File modification is done. Generating the IG should show updated shortDescription.");
    }

    private static void changeShortDescriptions(File inputFile, List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            System.err.println("Error: List of short descriptions for " + inputFile.getName() + " are blank.");
            return;
        }else{
            System.out.println("Looking for identifiers: " + String.join(",", identifiers));
        }

        try {
            JsonObject inputJson = parseJsonFromFile(inputFile);
            if (inputJson.has(DIFFERENTIAL) && inputJson.getAsJsonObject(DIFFERENTIAL).has(ELEMENT)) {
                JsonArray elementsArray = inputJson.getAsJsonObject(DIFFERENTIAL).getAsJsonArray(ELEMENT);
                for (JsonElement element : elementsArray) {

                    if (element instanceof JsonObject) {

                        JsonObject elementObj = (JsonObject) element;

                        String elementIdentifier = elementObj.get("id").getAsString();

                        System.out.println(elementIdentifier);
                        if (identifiers.contains(elementIdentifier)) {
                            System.out.println("Match: " + elementIdentifier);

                            String shortDescription = "";

                            try{
                                shortDescription = elementObj.getAsJsonPrimitive("short").getAsString();
                            }catch (Exception e){
                                //doesn't have a short description yet, we'll add one.
                            }

                            // Modify shortDescription
                            String newShortDescription = "(USCDI)(QI-Core)" + shortDescription
                                    .replace("(QI-Core)", "")
                                    .replace("(USCDI)", "");

                            // Update shortDescription in the output JSON
                            elementObj.addProperty("short", newShortDescription);

                            System.out.println(elementIdentifier + " to be modified: " + newShortDescription);
                        }
                    }
                }
            }

            writeJsonToFile(inputFile, inputJson);

        } catch (Exception e) {
            System.err.println("Error processing file: " + inputFile.getName());
            e.printStackTrace();
        }

    }

    private static List<String> getShortDescriptionsToChangeList(File outputFile) {

        List<String> shortDescriptionsToChange = new ArrayList<>();
        try {
            // Parse JSON data from the file in output folder:
            JsonObject outputJson = parseJsonFromFile(outputFile);
            // Get the snapshot array
            if (outputJson.has(SNAPSHOT) && outputJson.getAsJsonObject(SNAPSHOT).has(ELEMENT)) {
                //get all the elements to loop through:
                JsonArray elementsArray = outputJson.getAsJsonObject(SNAPSHOT).getAsJsonArray(ELEMENT);

                for (JsonElement element : elementsArray) {

                    if (element instanceof JsonObject) {
                        JsonObject elementObj = (JsonObject) element;
                        String elementIdentifier = elementObj.get("id").getAsString();
                        // Check if the elementObj has the "extension" property
                        if (elementObj.has(EXTENSION)) {

                            //get urls from extension array
                            JsonArray extensionArray = elementObj.getAsJsonArray(EXTENSION);

                            // Check condition:
                            if (hasBothURLTypes(extensionArray)) {
                                // Find shortDescription
                                String shortDescription = elementObj.getAsJsonPrimitive("short").getAsString();

                                // Modify shortDescription
                                String newShortDescription = "(USCDI)(QI-Core)" + shortDescription
                                        .replace("(QI-Core)", "")
                                        .replace("(USCDI)", "");

                                // Update shortDescription in the output JSON
                                System.out.println("Will update: " + elementIdentifier + " - " + newShortDescription);
                                shortDescriptionsToChange.add(elementIdentifier);

                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing file: " + outputFile.getName());
            e.printStackTrace();
        }

        return shortDescriptionsToChange;
    }

    private static boolean hasBothURLTypes(JsonArray extensionArray) {
        boolean qiCoreFound = false;
        boolean uscdiFound = false;

        for (JsonElement extensionElement : extensionArray) {
            if (extensionElement instanceof JsonObject) {
                JsonObject extensionObj = (JsonObject) extensionElement;
                if (extensionObj.has(URL) && extensionObj.getAsJsonPrimitive(URL).getAsString().endsWith(QICORE_KEYELEMENT)) {
                    qiCoreFound = true;
                }

                if (extensionObj.has(URL) && extensionObj.getAsJsonPrimitive(URL).getAsString().endsWith(USCDI_REQUIREMENT)) {
                    uscdiFound = true;
                }
            }
        }

        return qiCoreFound && uscdiFound;
    }


    private static JsonObject parseJsonFromFile(File file) throws Exception {
        try (Reader reader = new FileReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void writeJsonToFile(File file, JsonObject json) throws Exception {
        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(json, writer);
            System.out.println("Changes written to " + file.getAbsolutePath() + "\r\n");
        }
    }


}