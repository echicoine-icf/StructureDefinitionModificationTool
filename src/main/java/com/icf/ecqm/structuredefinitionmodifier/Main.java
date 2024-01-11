package com.icf.ecqm.structuredefinitionmodifier;
import java.io.*;

import com.google.gson.*;

public class Main {


    public static final String QICORE_KEYELEMENT = "qicore-keyelement";
    public static final String USCDI_REQUIREMENT = "uscdi-requirement";
    public static final String URL = "url";
    public static final String SNAPSHOT = "snapshot";
    public static final String ELEMENT = "element";
    public static final String EXTENSION = "extension";
    public static final String STRUCTURE_DEFINITION = "StructureDefinition";

    public static void main(String[] args) {

            String inputFolder = "input" +  File.separator + "profiles";
            String outputFolder = "output";

            File outputDir = new File(outputFolder);
            File[] outputFiles = outputDir.listFiles((dir, name) -> name.toLowerCase().startsWith(STRUCTURE_DEFINITION.toLowerCase()) &&
                    name.toLowerCase().endsWith(".json"));

            if (outputFiles != null) {
                for (File outputFile : outputFiles) {
                    System.out.println("Processing " + outputFile.getAbsolutePath());
                    processFile(outputFile, inputFolder);
                    System.out.println("\r\n");
                }
            } else {
                System.err.println("Error: Unable to list files in the output folder.");
            }

            System.out.println("File modification is done. Generating the IG should show updated shortDescription.");
        }

    private static void processFile(File outputFile, String inputFolder) {
        String identifier = outputFile.getName();
        try {
            // Parse JSON data from the file in output folder:
            JsonObject outputJson = parseJsonFromFile(outputFile);
            // Get the snapshot array
            if (outputJson.has(SNAPSHOT) && outputJson.getAsJsonObject(SNAPSHOT).has(ELEMENT)) {
                //get all the elements to loop through:
                JsonArray elementsArray = outputJson.getAsJsonObject(SNAPSHOT).getAsJsonArray(ELEMENT);

                boolean changesWritten = false;
                // Loop through the elements array
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
                                elementObj.addProperty("short", newShortDescription);
                                changesWritten = true;
                                // Write contents of JSON data to the output file, overwriting it
                                System.out.println("Updated: " + elementIdentifier + " - " + newShortDescription);

                            }
                        }
                    }
                }
                if (changesWritten) {
                    writeJsonToFile(new File(inputFolder + File.separator + outputFile.getName()), outputJson);
                }else{
                    System.out.println("No changes made.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing file: " + outputFile.getName());
            e.printStackTrace();
        }
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
                System.out.println("Changes written to " + file.getAbsolutePath());
            }
        }



}