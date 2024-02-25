import java.io.IOException;

public class KidPaint {
	public static void main(String[] args) throws IOException {
		UI ui = UI.getInstance();
		ui.setData(new int[80][80], 10);
		ui.setVisible(true);
	}
}