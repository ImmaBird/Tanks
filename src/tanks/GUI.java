package tanks;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class GUI extends Application {
	private static int port = 7777;
	private static String ip = "localhost";
	private static String name = "Player";
	private static String yourColor = "Blue";
	private Stage stage;
	private Server server = null;
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public void startClient() {
		new Client(ip, port, name, yourColor);
	}
	
	public void startServer() {
		server = new Server(port, stage.getX(), stage.getWidth(), stage.getY());
	}
	
	private Scene mainMenuScene;
	private Scene settingsScene;
	private Scene howToPlayScene;
	
	private Button newButton(String name) {
		Button button = new Button(name);
		button.setFont(GUI_SETTINGS.FONT);
		button.setPrefSize(GUI_SETTINGS.BUTTON_WIDTH, GUI_SETTINGS.BUTTON_HEIGHT);
		button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				Node node = (Node) event.getSource();
				Stage stage = (Stage) node.getScene().getWindow();
				Button button = (Button) node;
				
				switch(button.getText()) {
					case"Start Game":
						startClient();
						break;
					case"Start Server":
						if(server == null || !server.isOn()) //don't allow multiple servers
							startServer();
						break;
					case"Settings":
						stage.setScene(settingsScene);
						break;
					case "How To Play":
						stage.setScene(howToPlayScene);
						break;
					case"Done":
						//Go up chain to get VBox of the fields
						VBox vBoxOfAllElements = (VBox) button.getParent();
						HBox hBoxOfInputs = (HBox) vBoxOfAllElements.getChildren().get(1);
						VBox vBoxOfFields = (VBox) hBoxOfInputs.getChildren().get(1);
					
						GUI.name = ((TextField)vBoxOfFields.getChildren().get(0)).getText();
						GUI.ip = ((TextField)vBoxOfFields.getChildren().get(1)).getText();
						try {
							GUI.port = Integer.parseInt(((TextField)vBoxOfFields.getChildren().get(2)).getText());
						} catch(Exception ex) {}
					
						stage.setScene(mainMenuScene);
						break;
				} //end switch
			}//end handle
		}); //end setOnAction
		return button;
	}
	
	private void createMainMenuScene() {
		// Makes the title
		ImageView title = new ImageView(new Image(getClass().getResource("/imgs/Title.png").toExternalForm()));
		
		// Makes all the buttons and adds them to a VBox
		VBox vBox = new VBox(GUI_SETTINGS.VERT_SPACING_BTN_ELEMENTS);
		vBox.setAlignment(Pos.CENTER);
		vBox.getChildren().addAll(title,newButton("Start Game"),newButton("Start Server"),newButton("Settings"),newButton("How To Play"));
		
		// Puts the VBox in a Pane and centers it
		StackPane pane = new StackPane(vBox);
		StackPane.setAlignment(vBox, Pos.CENTER);
		pane.setPrefSize(GUI_SETTINGS.WINDOW_WIDTH,GUI_SETTINGS.WINDOW_HEIGHT);
		
		// Creates the scene
		mainMenuScene = new Scene(new AnchorPane(pane));
	}
	
	private void createSettingsScene() {
		// Makes the title
		ImageView title = new ImageView(new Image(getClass().getResource("/imgs/SettingsTitle.png").toExternalForm()));
		
		TextField nameField = new TextField(name);
		nameField.setFont(GUI_SETTINGS.FONT);
		
		TextField ipField = new TextField(ip);
		ipField.setFont(GUI_SETTINGS.FONT);
		
		TextField portField = new TextField(""+port);
		portField.setFont(GUI_SETTINGS.FONT);
		
		Label nameLabel = new Label("Name:");
		nameLabel.setFont(GUI_SETTINGS.FONT);
		
		Label ipLabel = new Label("IP:");
		ipLabel.setFont(GUI_SETTINGS.FONT);
		
		Label portLabel = new Label("Port:");
		portLabel.setFont(GUI_SETTINGS.FONT);
		
		VBox labelVBox = new VBox(15 + GUI_SETTINGS.VERT_SPACING_BTN_ELEMENTS);
		labelVBox.setAlignment(Pos.CENTER);
		labelVBox.getChildren().addAll(nameLabel,ipLabel,portLabel);
		
		VBox fieldVBox = new VBox(GUI_SETTINGS.VERT_SPACING_BTN_ELEMENTS);
		fieldVBox.setAlignment(Pos.CENTER);
		fieldVBox.getChildren().addAll(nameField,ipField,portField);
		
		HBox textHBox = new HBox(GUI_SETTINGS.HOR_SPACING_BTN_ELEMENTS);
		textHBox.setAlignment(Pos.CENTER);
		textHBox.getChildren().addAll(labelVBox,fieldVBox);
		
		HBox yourTankSelection = new HBox(GUI_SETTINGS.HOR_SPACING_BTN_COLORS);
		yourTankSelection.setAlignment(Pos.CENTER);
		Label yourSelectTank = new Label("Select Your Tank:");
		yourSelectTank.setFont(GUI_SETTINGS.FONT);
		ArrayList<ImageView> yourImages = new ArrayList<>();
		ArrayList<String> colors = new ArrayList<>(Arrays.asList("Red", "Blue", "Yellow", "Green", "Orange", "Purple", "Black", "Pink"));
		DropShadow shadow = new DropShadow(20, Color.BLACK);
		for(String c : colors){
			ImageView temp = new ImageView(new Image(getClass().getResource("/imgs/" + c + "Tank.png").toExternalForm()));
			temp.setOnMousePressed(e -> {
				for(ImageView i : yourImages)
					i.setEffect(null);
				temp.setEffect(shadow);
				yourColor = c;
			});
			yourImages.add(temp);
		}
		yourImages.get(colors.indexOf(yourColor)).setEffect(shadow);
		yourTankSelection.getChildren().add(yourSelectTank);
		yourTankSelection.getChildren().addAll(yourImages);
		
		VBox vBox3 = new VBox(GUI_SETTINGS.VERT_SPACING_BTN_ELEMENTS);
		vBox3.setAlignment(Pos.CENTER);
		vBox3.getChildren().addAll(title, textHBox, yourTankSelection, newButton("Done"));
		
		// Puts the VBox in a StackPane and centers it
		StackPane pane = new StackPane(vBox3);
		StackPane.setAlignment(vBox3, Pos.CENTER);
		pane.setPrefSize(GUI_SETTINGS.WINDOW_WIDTH,GUI_SETTINGS.WINDOW_HEIGHT);
		
		// Creates the scene
		settingsScene = new Scene(new AnchorPane(pane));
	}
	
	private void createHowToPlayScene(){
		ImageView title = new ImageView(new Image(getClass().getResource("/imgs/HowToPlayTitle.png").toExternalForm()));
		
		//Reading in the rules file, works for jar file
		TextArea text = new TextArea();
		text.setEditable(false);
		try(InputStream in = getClass().getResourceAsStream("/Rules.txt");
			Scanner scan = new Scanner(in)){
				while(scan.hasNextLine())
					text.setText(text.getText() + scan.nextLine() + "\n");
		}catch(Exception ex){ex.printStackTrace();}
		
		Button done = new Button("Done");
		done.setOnAction(e -> {
			stage.setScene(mainMenuScene);
		});
		done.setPrefSize(GUI_SETTINGS.BUTTON_WIDTH, GUI_SETTINGS.BUTTON_HEIGHT);
		
		BorderPane pane = new BorderPane();
		pane.setTop(title);
		pane.setCenter(text);
		pane.setBottom(done);
		
		pane.setPadding(new Insets(GUI_SETTINGS.VERT_SPACING_BTN_ELEMENTS));
		BorderPane.setMargin(title, new Insets(0,0,GUI_SETTINGS.VERT_SPACING_BTN_ELEMENTS,0));
		BorderPane.setAlignment(title, Pos.CENTER);
		BorderPane.setMargin(done, new Insets(GUI_SETTINGS.VERT_SPACING_BTN_ELEMENTS,0,0,0));
		BorderPane.setAlignment(done, Pos.CENTER);
		
		pane.setPrefSize(GUI_SETTINGS.WINDOW_WIDTH,GUI_SETTINGS.WINDOW_HEIGHT);
		howToPlayScene = new Scene(pane);
	}
	
	public void start(Stage stage) {
		try(Scanner sc = new Scanner(new File("settings.txt"))) {
			name = sc.nextLine();
			ip = sc.nextLine();
			port = Integer.parseInt(sc.nextLine());
			yourColor = sc.nextLine();
		} catch(Exception ex) {}
		
		try {
			createMainMenuScene();
			createSettingsScene();
			createHowToPlayScene();
			stage.setScene(mainMenuScene);
			stage.setTitle(GUI_SETTINGS.MENU_TITLE);
			stage.show();
			this.stage = stage;
			
			stage.setOnCloseRequest(EventHandler -> {
				try(PrintWriter pr = new PrintWriter(new File("settings.txt"))) {
					pr.println(name);
					pr.println(ip);
					pr.println(port);
					pr.println(yourColor);
				} catch(Exception ex) {}
				System.exit(0);
			});
		} catch(Exception ex) {
			System.out.println("Something has gone wrong with the GUI.");
			ex.printStackTrace();
			System.exit(0);
		}
	}
}