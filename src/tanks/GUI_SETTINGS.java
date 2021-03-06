package tanks;

import javafx.scene.image.Image;
import javafx.scene.text.Font;

public final class GUI_SETTINGS {
	final static public Font TITLE_FONT = new Font(46);
	final static public Font FONT = getFont("/fonts/RobotoCondensed-Regular.ttf", 18);
	final static public Font BOLD_FONT = getFont("/fonts/RobotoCondensed-Bold.ttf", 18);
	
	final static public int WINDOW_HEIGHT = 600;
	final static public int WINDOW_WIDTH = 800;
	
	final static public int BUTTON_HEIGHT = 20;
	final static public int BUTTON_WIDTH = 120;
	
	//How far away elements should be vertically from the one another
	final static public int VERT_SPACING_BTN_ELEMENTS = 20;
	//How far away elements should be horizontally from the one another
	final static public int HOR_SPACING_BTN_ELEMENTS = 7;
	//How far away tank color choice should be horizontally
	final static public int HOR_SPACING_BTN_COLORS = 15;
	
	final static public String MENU_TITLE = "Tanks";
	
	//Game window should be square, height and width size
	final static public double GAME_WINDOW_WIDTH = 1600;
	final static public double GAME_WINDOW_HEIGHT = 900;
	//The size of the tank
	final static public int TANK_WIDTH = 108;
	final static public int TANK_HEIGHT = 72;
	
	//Stuff for the swerber
	final static public int SERVER_HEIGHT = 100;
	final static public int SERVER_WIDTH = 200;
	
	final static public int POPUP_HEIGHT = 75;
	final static public int POPUP_WIDTH = 400;
	final static public int POPUP_PADDING = 10;
	final static public int POPUP_MARGIN = 5;
	final static public int POPUP_BTN_WIDTH = 80;
	final static public int POPUP_BTN_HEIGHT = 20;
	
	final static public int MAX_PLAYERS = 8;
	
	final static public int PLAYER_MAX_LIFE = 3;
	
	//Tanks name plate
	final static public Font NAME_FONT = new Font(24);
	public static final long SHOOT_DELAY = 500;
	
	final public static Image getBodyImage(String color){
		try {
			return new Image("imgs/Tank Body "+color+".png");
		} catch(Exception ex) {
			System.out.println("Failed to get body image, or tried to open client with same name.");
			return null;
		}
	}
	
	final public static Image getTopImage(String color){
		try {
			return new Image("imgs/Top Tank "+color+".png");
		} catch(Exception ex) {
			System.out.println("Failed to get top image, or tried to open client with same name.");
			return null;
		}
	}
	
	final private static Font getFont(String name, double size){
		try{
			return Font.loadFont(GUI_SETTINGS.class.getResourceAsStream(name), size);
		}
		catch(Exception ex){
			return null;
		}
	}
}