package tanks;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

class Client extends Application{
	//Where the server is
	private String ip;
	private int port;
	//The client's name
	private String name;
	//Hashmap of all other client tanks on the server
	private volatile HashMap<String, Tank> tanks = new HashMap<>();
	//This Client's Tank
	private volatile Tank myTank;
	//The heart pictures for life
	private Hearts hearts = new Hearts();
	//Label to show when restarting a game
	private ImageView restartLabel;
	private ImageView startLabel;
	private ImageView lobbyLabel;
	private ImageView winLabel;
	private ImageView loseLabel;
	//Color of my tank
	private String myColor;
	// Keeps track of the connection with the server
	private volatile boolean connectedToServer = false;
	//Turns true to let everything know that the attempt to connect to the server failed
	private volatile boolean connectionTimedOut = false;
	//Stuff for sending/receiving from the server
	private volatile LinkedList<Package> packages = new LinkedList<>();
	private ObjectOutputStream write;
	//GUI's stage
	private Stage stage;
	//GUI's pane
	private Pane pane;
	
	/**Makes a new Client given the String, IP, and the client's name
	 * @param anIp  The IP address of the Server
	 * @param aPort  The Port that the server is using
	 * @param name  The name of the Client*/
	public Client(String anIp, int aPort, String name, String myColor) {
		//Connecting to the server
		ip = anIp;
		port = aPort;
		this.name = name;
		if(name.length() > 20) //limit name to 20 characters
			this.name = name.substring(0, 20);
		String n = this.name.toLowerCase();
		
		//Color easter eggs
		if(n.contains("rainbow") || n.contains("unicorn"))
			this.myColor = "Rainbow";
		else if(n.contains("merica") || (n.contains("chuck") && n.contains("norris")) )
			this.myColor = "America";
		else if(n.contains("shrek"))
			this.myColor = "Shrek";
		else if(n.contains("donkey"))
			this.myColor = "Donkey";
		else if(n.contains("farquaad"))
			this.myColor = "Farquaad";
		else
			this.myColor = myColor;
		
		connect();
		
		//Starting up GUI
		try {
			start(new Stage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**@return true if the client is connected to the server*/
	public boolean isConnected() {
		return connectedToServer;
	}
	
	/**Disconnects from the server and closes the GUI*/
	public void stop() {
		connectedToServer = false;
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				stage.close();
			}
		});
	}
	
	/**Sends a Package to the Server*/
	public synchronized void write(Package data) {
		int i = 0;
		while(!connectedToServer && i < 500) {
			try {
				Thread.sleep(10); // The thread tries to wait for a connection to the server
			} catch(Exception ex) {
				System.out.println("Thread sleep failed. Not signifigant error.");
			}
			i++;
		}
		try {
			write.writeObject(data); // Sends a package to the server
		} catch(Exception ex) {
			System.out.println("An attempt to write the server has failed.");
			ex.printStackTrace();
		}
	}
	
	/**Sends the Client's Tank to the Server*/
	public void writeTank(){
		if(connectedToServer)
			write(myTank.getPackage());
	}
	
	/**Sends a message to the server letting it know that it is leaving*/
	private void sendLeaveMessage(){
		write(new Package(name, true));
	}
	
	/**Connects to the server and keeps listening for packages*/
	private void connect() {
		new Thread(new Runnable() {
			public void run() {
				try(
						 Socket clientSocket = new Socket(ip,port) // Connects the the server
						;ObjectOutputStream aWriter = new ObjectOutputStream(clientSocket.getOutputStream()) // Opens output stream
						;ObjectInputStream read = new ObjectInputStream(clientSocket.getInputStream()) // Opens input stream
				) {
					write = aWriter;
					connectedToServer = true; // Client is officially connected
					Package data;
					while(connectedToServer) { // Reads in the data from the server
						data = (Package)read.readObject();
						addPackage(data);
					}
				} catch(EOFException ex1) {
					System.out.println("Client closed");
				} catch(Exception ex2) {
					connectedToServer = false;
					connectionTimedOut = true;
					System.out.println(name + "'s connection the the server has failed.");
					ex2.printStackTrace();
					Platform.runLater(new Runnable(){ //close the window
						@Override
						public void run() {
							stage.close();
						}
					});
				}
			}
		}).start();	
	}
	
	/**Make a thread to update all of the other player's info*/
	private void updateOtherPlayers(){
		Client This = this;
		new Thread(new Runnable(){
			@Override
			public void run() {	
				//Wait for the server to connect, if timed out just quit
				while(!connectedToServer && !connectionTimedOut) {}
				boolean needMoreTanks = false; //used for starting up a game thread racing
				
				//Connected to the server, start reading through packets and updating
				while(connectedToServer){
					if(!packagesIsEmpty()){//there's a package to get
						Package currentP = getPackage();
						if(currentP.getNewName()){ //someone on server has name already
							new NewNamePopUp();
							stop();
						}
						if(currentP.isLeaving() || currentP.isDead()){ //someone is leaving or they died
							Tank removed = tanks.remove(currentP.getName());
							//Take the take off of the pane
							Platform.runLater(new Runnable(){
								@Override
								public void run() {
									if(removed != null) {
										if(removed.getComponents()[0] != null)
											pane.getChildren().remove(removed.getComponents()[0]);
										if(removed.getComponents()[1] != null)
											pane.getChildren().remove(removed.getComponents()[1]);
										if(removed.getComponents()[2] != null)
											pane.getChildren().remove(removed.getComponents()[2]);
									}
								}
							});
							if(tanks.isEmpty()){ //you win
								winLabel.setVisible(true);
								new Thread(new Runnable(){
									@Override
									public void run() {
										try{Thread.sleep(3000);}catch(Exception e){}
										winLabel.setVisible(false);
									}
								}).start();
							}
						}
						else if(!currentP.getRestart() && !currentP.getStart() && !tanks.containsKey(currentP.getName())){ //Tank isn't in the hashmap yet
							//Add the new tank to the pane
							Platform.runLater(new Runnable(){
								public void run(){
									if(!tanks.containsKey(currentP.getName())){
										tanks.put(currentP.getName(), new Tank(currentP.getName(), currentP.getRotate(),currentP.getX(),
																			currentP.getY(),currentP.getCannonRotate(),currentP.getColor(), This));
										pane.getChildren().add(tanks.get(currentP.getName()));
									}
								}
							});
							writeTank(); //Send out position once a new player joins
						}
						else{//just update the tank in the map
							if(!currentP.getStart() && !currentP.getRestart()){
								Platform.runLater(new Runnable(){
									@Override
									public void run() {
										if(tanks.containsKey(currentP.getName()))
											tanks.get(currentP.getName()).updateFromPackage(currentP);
									}
								});
							}
						}
						
						//Special messages from server
						if(currentP.getRestart()){ //game is restarting
							Platform.runLater(new Runnable(){
								@Override
								public void run() {
									myTank.reset(pane);
									if(!pane.getChildren().contains(myTank))
										pane.getChildren().addAll(myTank.getComponents());
									writeTank();
									hearts.reset();
									startLabel.setVisible(false); //Make sure startLabel isn't showing
									winLabel.setVisible(false); //Make sure winLabel isn't showing
									loseLabel.setVisible(false); //Make sure loseLabel isn't showing
									restartLabel.setVisible(true);
									lobbyLabel.setVisible(true);
									restartLabel.toFront();
									new Thread(new Runnable(){
										@Override
										public void run() {
											try{Thread.sleep(1250);}catch(Exception e){}
											restartLabel.setVisible(false);
										}
									}).start();
									myTank.requestFocus();
									writeTank();
								}
							});
							continue;
						}
						
						if(currentP.getStart() || needMoreTanks) { //Game is starting
							ArrayList<Tank> tanks = new ArrayList<Tank>();
							if(!needMoreTanks){
								Platform.runLater(new Runnable(){
									@Override
									public void run() {
										myTank.setIsDead(true);
										if(!pane.getChildren().contains(myTank))
											pane.getChildren().addAll(myTank.getComponents());
										myTank.reset(pane);
										hearts.reset();
										writeTank(); //let everyone know that you're now alive
									}
								});
							}
							Platform.runLater(new Runnable(){
								@Override
								public void run() {
									for(Node node : pane.getChildren()) {
										if(node instanceof Tank) {
											if(!tanks.contains(node))
												tanks.add((Tank) node);
										}
									}
								}
							});
							
							try{Thread.sleep(5);}catch(Exception e){}
							if(tanks.size() != currentP.getNumOnServer()){ //wait until all players are added back
								needMoreTanks = true;
								continue;
							}else{needMoreTanks = false;}
							
							Collections.sort(tanks);
							
							Platform.runLater(new Runnable(){
								@Override
								public void run() {
									int count = 0;
									int i = 5;
									int j = 5;
									for (Tank tank : tanks) {
										if (count % 4 == 0) {
											tank.setRotate(0);
											tank.setX(i);
											tank.setY(j);
										} else if (count % 4 == 1) {
											tank.setRotate(180);
											tank.setX(pane.getWidth() - GUI_SETTINGS.TANK_WIDTH - i);
											tank.setY(pane.getHeight() - GUI_SETTINGS.TANK_HEIGHT - j);
										} else if (count % 4 == 2) {
											tank.setRotate(0);
											tank.setX(i);
											tank.setY(pane.getHeight() - GUI_SETTINGS.TANK_HEIGHT - j);
										} else if (count % 4 == 3) {
											tank.setRotate(180);
											tank.setX(pane.getWidth() - GUI_SETTINGS.TANK_WIDTH - i);
											tank.setY(j);
											j += GUI_SETTINGS.TANK_HEIGHT + 5;
											i += GUI_SETTINGS.TANK_WIDTH + 5;
										}
										count++;
										tank.snapComponents();
									}
									writeTank(); // send out new position once corner has been determined

									restartLabel.setVisible(false); // Make sure restartLabel isn't showing
									lobbyLabel.setVisible(false); // Turn lobbyLable off
									winLabel.setVisible(false); // Make sure winLabel isn't showing
									loseLabel.setVisible(false); // Make sure loseLabel isn't showing
									startLabel.setVisible(true);
									startLabel.toFront();
									new Thread(new Runnable() {
										@Override
										public void run() {
											try {
												Thread.sleep(1250);
											} catch (Exception e) {
											}
											startLabel.setVisible(false);
										}
									}).start();
								}
							});
						}
						putStuffOnFront();
					} else {
						putStuffOnFront();
						//Sleep the thread for a millisecond every cycle so it doesn't use 40% CPU Usage
						try{Thread.sleep(1);}catch(Exception ex){}
					}
				}
			}
		}).start();
	}
	
	/**Gets the next package from the queue (synchronized)*/
	private synchronized Package getPackage(){
		return packages.poll();
	}
	
	/*Adds a new package to the queue (synchronized)*/ 
	private synchronized void addPackage(Package p){
		packages.add(p);
	}
	
	/**@return true if the packages queue is empty (synchronized)*/
	private synchronized boolean packagesIsEmpty(){
		return packages.isEmpty();
	}
	
	/**Puts everything that needs to be front on the pane on the front*/
	private void putStuffOnFront(){
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				for(Node n : myTank.getComponents()){
					n.toFront();
				}
				restartLabel.toFront();
				startLabel.toFront();
				lobbyLabel.toFront();
				winLabel.toFront();
				loseLabel.toFront();
				hearts.toFront();
			}
		});
	}
	
	/**Makes a new Tank for this client*/
	private void makeNewTank(){
		Client This = this;
		//Makes sure that your tank is the focus
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				myTank = new Tank(name, This, myColor);
				pane.getChildren().add(myTank);
				myTank.requestFocus();
				writeTank();
			}
		});
	}
	
	public Tank getTank() {
		return myTank;
	}
	
	public void decrementHeart(){
		hearts.removeAHeart();
		if(myTank.getHealth() == 0){
			loseLabel.setVisible(true);
			new Thread(new Runnable(){
				@Override
				public void run() {
					try{Thread.sleep(3000);}catch(Exception e){}
					loseLabel.setVisible(false);
				}
			}).start();
		}
	}
	
	@Override
	/**Starts up the GUI window*/
	public void start(Stage primaryStage) throws Exception {
		//Setup the pane
		pane = new Pane();
		pane.setPrefSize(GUI_SETTINGS.GAME_WINDOW_WIDTH, GUI_SETTINGS.GAME_WINDOW_HEIGHT);
		pane.setPrefSize(GUI_SETTINGS.GAME_WINDOW_WIDTH, GUI_SETTINGS.GAME_WINDOW_HEIGHT);
		
		pane.setBackground(new Background(new BackgroundImage(new Image("imgs/metal scratch cartoon.jpg"), null, null, null, null)));
		
		//Start and Restart Labels
		DropShadow textDS = new DropShadow(20, Color.BLACK);
		startLabel = new ImageView("imgs/GameStart.png");
		pane.getChildren().add(startLabel);
		startLabel.setX(GUI_SETTINGS.GAME_WINDOW_WIDTH / 2 - startLabel.getImage().getWidth()/2);
		startLabel.setY(GUI_SETTINGS.GAME_WINDOW_HEIGHT / 2 - startLabel.getImage().getHeight()/2);
		startLabel.setEffect(textDS);
		startLabel.setVisible(false);
		restartLabel = new ImageView("imgs/GameRestart.png");
		pane.getChildren().add(restartLabel);
		restartLabel.setX(GUI_SETTINGS.GAME_WINDOW_WIDTH / 2 - restartLabel.getImage().getWidth()/2);
		restartLabel.setY(GUI_SETTINGS.GAME_WINDOW_HEIGHT / 2 - 100);
		restartLabel.setEffect(textDS);
		restartLabel.setVisible(false);
		lobbyLabel = new ImageView("imgs/Lobby.png");
		pane.getChildren().add(lobbyLabel);
		lobbyLabel.setX(GUI_SETTINGS.GAME_WINDOW_WIDTH / 2 - lobbyLabel.getImage().getWidth()/2);
		lobbyLabel.setEffect(textDS);
		//Have lobbyLabel visible when joining a game
		winLabel = new ImageView("imgs/Win.png");
		pane.getChildren().add(winLabel);
		winLabel.setX(GUI_SETTINGS.GAME_WINDOW_WIDTH / 2 - winLabel.getImage().getWidth()/2);
		winLabel.setY(GUI_SETTINGS.GAME_WINDOW_HEIGHT / 2 - winLabel.getImage().getHeight()/2);
		winLabel.setEffect(textDS);
		winLabel.setVisible(false);
		loseLabel = new ImageView("imgs/Lose.png");
		pane.getChildren().add(loseLabel);
		loseLabel.setX(GUI_SETTINGS.GAME_WINDOW_WIDTH / 2 - loseLabel.getImage().getWidth()/2);
		loseLabel.setY(GUI_SETTINGS.GAME_WINDOW_HEIGHT / 2 - loseLabel.getImage().getHeight()/2);
		loseLabel.setEffect(textDS);
		loseLabel.setVisible(false);
		
		//Add the player's tank to the pane
		makeNewTank();
		updateOtherPlayers();
		
		pane.getChildren().add(hearts);
		hearts.toFront();
		
		//no resize
		primaryStage.setResizable(false);
		
		//Show the pane
		Scene scene = new Scene(pane);
		primaryStage.setTitle(GUI_SETTINGS.MENU_TITLE + " | Player: " + name);
		primaryStage.setScene(scene);
		primaryStage.show();
		primaryStage.setOnCloseRequest(e -> {
			sendLeaveMessage();
			stop();
			primaryStage.close();
		});
		stage = primaryStage;
	}
	
	/**Holds the heart images*/
	private class Hearts extends HBox{
		ImageView[] images = new ImageView[GUI_SETTINGS.PLAYER_MAX_LIFE];
		
		private Hearts(){
			super(10);
			for(int x = 0; x < GUI_SETTINGS.PLAYER_MAX_LIFE; x++){
				images[x] = new ImageView("imgs/Heart.png");
				getChildren().add(images[x]);
			}
		}
		
		private void removeAHeart(){
			getChildren().get(myTank.getHealth()).setVisible(false);
		}
	
		private void reset(){
			for(Node n : getChildren()){
				n.setVisible(true);
			}
		}
	}
	
	/**Popup window for having the same name as someone else on the server*/
	private class NewNamePopUp extends Application{
		private NewNamePopUp(){
			Platform.runLater(new Runnable(){
				@Override
				public void run() {
					try {
						start(new Stage());
					} catch (Exception e) {
						System.out.println("New Name Pop Up window failed to start.");
						e.printStackTrace();
					}
				}
			});
		}
		
		@Override
		public void start(Stage stage) throws Exception {
			Label topError = new Label("Someone on this server already has your name.");
			topError.setFont(GUI_SETTINGS.FONT);
			topError.setTextAlignment(TextAlignment.CENTER);
			Label bottomError = new Label("Please go into settings and choose a new name.");
			bottomError.setFont(GUI_SETTINGS.FONT);
			bottomError.setTextAlignment(TextAlignment.CENTER);
			VBox errors = new VBox();
			errors.setAlignment(Pos.CENTER);
			errors.getChildren().addAll(topError, bottomError);
			
			Button okBtn = new Button("OK");
			okBtn.setPrefSize(GUI_SETTINGS.POPUP_BTN_WIDTH, GUI_SETTINGS.POPUP_BTN_HEIGHT);
			okBtn.setOnAction(e -> {
				stage.close();
			});
			
			BorderPane pane = new BorderPane();
			pane.setPrefSize(GUI_SETTINGS.POPUP_WIDTH, GUI_SETTINGS.POPUP_HEIGHT);
			BorderPane.setMargin(okBtn, new Insets(GUI_SETTINGS.POPUP_MARGIN));
			pane.setPadding(new Insets(GUI_SETTINGS.POPUP_PADDING));
			pane.setCenter(errors);
			pane.setBottom(okBtn);
			BorderPane.setAlignment(errors, Pos.CENTER);
			BorderPane.setAlignment(okBtn, Pos.CENTER);
			
			
			
			stage.setScene(new Scene(pane));
			stage.setTitle("ERROR");
			stage.show();
		}
	}
}

