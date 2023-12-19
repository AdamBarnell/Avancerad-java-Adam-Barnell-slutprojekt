package com.example.avanceradjavaadambarnellslutprojekt;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class HelloController {

    public Button randomRecipeButton;
    public Button saveButton;
    public Button getRecipe;
    @FXML private ChoiceBox<String> recipeChoiceBox;
    @FXML private Button deleteRecipeButton;
    @FXML private AnchorPane ap;
    @FXML private TextArea recipesTextArea;
    @FXML private TextArea recipeTextArea;
    private final Map<String, String> recipeMap = new HashMap<>();
    //Sätter olika strings för de apierna, detta gör det enklare att hämta senare
    private static final String FIREBASE_URL = "https://slutprojekt-8495f-default-rtdb.europe-west1.firebasedatabase.app/.json";
    private static final String API_KEY = "2da3e217a3f349d088a263038d50cef7";
    private static final String RANDOM_RECIPE_URL = "https://api.spoonacular.com/recipes/random?apiKey=" + API_KEY;

    @FXML
    public void initialize() {
        loadRecipesFirebase(); // Laddar alla recept från firebase i start av app
    }
    //Lägger till alla sidor som dirigeras genom mousevents
    @FXML
    void page3(MouseEvent event) {
        loadPage("Recipe.fxml");
    }

    @FXML
    void page4(MouseEvent event) {
        loadPage("Page4.fxml");
    }

    @FXML
    void randomizer(MouseEvent event) {
        loadPage("Randomizer.fxml");
    }

    @FXML
    void recipes(MouseEvent event) {
        loadPage("Page1.fxml");
    }

    private void loadPage(String page) {
        try {
            Node pane = FXMLLoader.load(getClass().getResource(page));
            ap.getChildren().setAll(pane);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveRecipe(String recipe) { //Skapar receptet till firebase
        HttpClient client = HttpClient.newHttpClient();
        Map<String, String> data = new HashMap<>();
        data.put("recipe", recipe);

        try {
            JsonObject json = new JsonObject();
            data.forEach(json::add);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(FIREBASE_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(responseBody -> System.out.println("Firebase response: " + responseBody));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML   //Hanterar sparfunktionen för knappen
    private void saveButton(ActionEvent event) {
        String recipeText = recipeTextArea.getText();
        if (!recipeText.isEmpty()) {
            saveRecipe(recipeText);
        }
    }

    @FXML   // Hämtar ett slumpmässigt recept från Spoonacular API
    private void getRandomRecipe(ActionEvent event) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RANDOM_RECIPE_URL))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::processJson)
                .join();
    }

    private void processJson(String responseBody) { //Bearbetar Json filerna som kommer in från API:n
        try {
            JsonArray recipesArray = Json.parse(responseBody).asObject().get("recipes").asArray();
            if (!recipesArray.isEmpty()) {
                JsonObject recipe = recipesArray.get(0).asObject();
                String title = recipe.get("title").asString();
                String instructions = recipe.get("instructions").asString();

                Platform.runLater(() -> recipeTextArea.setText("Title: " + title + "\n\nInstructions:\n" + instructions));
            }
        } catch (Exception e) {
            Platform.runLater(() -> recipeTextArea.setText("Failed to load recipe."));
        }
    }

    @FXML   //Hämtar recept från firebase
    private void getRecipe(ActionEvent event) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FIREBASE_URL))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::showRecipes)
                .join();
    }
    // Visar de recepten som finns i firebase
    private void showRecipes(String responseBody) {
        try {
            JsonObject rootNode = Json.parse(responseBody).asObject();
            StringBuilder recipes = new StringBuilder();
            for (JsonObject.Member entry : rootNode) {
                String key = entry.getName();
                String recipe = entry.getValue().asObject().getString("recipe", "");
                recipes.append("\nRecipe: ").append(recipe).append("\n\n");
            }

            String finalRecipes = recipes.toString();
            Platform.runLater(() -> recipesTextArea.setText(finalRecipes.isEmpty() ? "No recipes found." : finalRecipes));
        } catch (Exception e) {
            Platform.runLater(() -> recipesTextArea.setText("Failed to fetch recipes."));
        }
    }

    @FXML //Hanterar att deletefunktionen på deleteknappen
    private void deleteRecipe(ActionEvent event) {
        String selectedTitle = recipeChoiceBox.getValue();
        if (selectedTitle != null && recipeMap.containsKey(selectedTitle)) {
            String selectedKey = recipeMap.get(selectedTitle);
            deleteRecipeFirebase(selectedKey);
        } else {
            System.out.println("Failed");
        }
    }
    // Tar bort recept från Firebase
    private void deleteRecipeFirebase(String recipeKey) {
        String deleteUrl = FIREBASE_URL.replace(".json", "/" + recipeKey + ".json");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(deleteUrl)).DELETE().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(responseBody -> {
                    //Hanterar svaret från deletefunktionen
                    loadRecipesFirebase(); //Laddar om recepten efter deleten
                });
    }
    // Laddar in recept från firebase och uppdaterar Choiceboxen
    private void loadRecipes(String responseBody) {
        try {
            JsonObject recipes = Json.parse(responseBody).asObject();
            recipeMap.clear();
            Platform.runLater(() -> {
                recipeChoiceBox.getItems().clear();
                for (JsonObject.Member member : recipes) {
                    String recipeKey = member.getName();
                    JsonObject recipeObj = member.getValue().asObject();
                    String fullRecipeText = recipeObj.getString("recipe", "");

                    // Getting the recipetitle from the whole text
                    String recipeTitle = titleExtraction(fullRecipeText);

                    recipeMap.put(recipeTitle, recipeKey);
                    recipeChoiceBox.getItems().add(recipeTitle);
                }
            });
        } catch (Exception e) {
            System.out.println("Error parsing recipe data: " + e.getMessage());
        }
    }
    //Hämtar titeln från recepten
    private String titleExtraction(String fullRecipeText) {
        int endIndex = fullRecipeText.indexOf("\n\nInstructions:\n");
        if (endIndex != -1) {
            return fullRecipeText.substring(0, endIndex);
        } else {
            return "Unnamed Recipe";
        }
    }
    //Laddar recept från Firebase
    private void loadRecipesFirebase() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(FIREBASE_URL)).GET().build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(this::loadRecipes)
                .join();
    }
}
