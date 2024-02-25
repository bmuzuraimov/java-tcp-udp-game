import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import javax.swing.border.LineBorder;

enum PaintMode {Pixel, Area}
public class UI extends JFrame implements UIUpdateListener{
	private String username;
	private TCPClient tcp_client;
	private UDPClient udp_client;
	private Server server;
	private JComboBox<String> studioDropdown = new JComboBox<>(new String[]{"Search studio...", "My studio"});
	private JTextField msgField;
	private JTextArea chatArea;
	private JPanel pnlColorPicker;
	private JPanel paintPanel;
	private JToggleButton tglPen;
	private JToggleButton tglBucket;
	
	private static UI instance;
	private int selectedColor = -543230; 	//golden
	private int tempSelectedColor;
	
	int[][] data = new int[80][80];
	int blockSize;
	PaintMode paintMode = PaintMode.Pixel;

	/**
	 * get the instance of UI. Singleton design pattern.
	 * @return
	 */
	public static UI getInstance() throws IOException{
		try {
		if (instance == null)
			instance = new UI();
		}catch(IOException e) {
			System.out.println("UI class doesn't exists");
		}
		return instance;
	}

	private void saveToFile(String filename) {
		try {
			// Create a file chooser
			JFileChooser fileChooser = new JFileChooser();

			// Set the default file name
			fileChooser.setSelectedFile(new File(filename));

			// Show the file chooser dialog
			int result = fileChooser.showSaveDialog(this);

			// If the user selected a file, save the data to that file
			if (result == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();

				// Create a file writer
				FileWriter writer = new FileWriter(file);

				// Write the data to the file
				for (int x = 0; x < data.length; x++) {
					for (int y = 0; y < data[0].length; y++) {
						writer.write(data[x][y] + "\n");
					}
				}

				// Close the file writer
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadFromFile(String filename) {
		try {
			// Create a file chooser
			JFileChooser fileChooser = new JFileChooser();

			// Set the default file name
			fileChooser.setSelectedFile(new File(filename));

			// Show the file chooser dialog
			int result = fileChooser.showOpenDialog(this);

			// If the user selected a file, load the data from that file
			if (result == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();

				// Create a file scanner
				Scanner scanner = new Scanner(file);

				// Read the data from the file
				for (int x = 0; x < data.length; x++) {
					for (int y = 0; y < data[0].length; y++) {
						data[x][y] = scanner.nextInt();
						try {
							tcp_client.getOut().writeInt(1);
							tcp_client.getOut().writeInt(data[x][y]);
							tcp_client.getOut().writeInt(x);
							tcp_client.getOut().writeInt(y);
							tcp_client.getOut().flush();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}

				// Close the file scanner
				scanner.close();

				// Repaint the paint panel to display the loaded data
				SwingUtilities.invokeLater(() -> {
					paintPanel.repaint();
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * private constructor. To create an instance of UI, call UI.getInstance() instead.
	 */
	private void get_username(){
		this.username = JOptionPane.showInputDialog(null, "Enter your username:", "Username", JOptionPane.PLAIN_MESSAGE);
		if (this.username == null || this.username.isEmpty()) {
			System.exit(0);
		}
	}
	private void initializeUI() throws IOException{
		get_username();

		udp_client = new UDPClient();
		udp_client.start_socket();
		udp_client.broadcast();
		udp_client.listen_studio(studioDropdown);

		tcp_client = new TCPClient(this);
		server = new Server(tcp_client.getMy_address(), 12345);

		setTitle("KidPaint");
		JPanel basePanel = new JPanel();
		getContentPane().add(basePanel, BorderLayout.CENTER);
		basePanel.setLayout(new BorderLayout(0, 0));

		paintPanel = new JPanel() {

			// refresh the paint panel
			@Override
			public void paint(Graphics g) {
				super.paint(g);

				Graphics2D g2 = (Graphics2D) g; // Graphics2D provides the setRenderingHints method

				// enable anti-aliasing
				RenderingHints rh = new RenderingHints(
						RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHints(rh);

				// clear the paint panel using black
				g2.setColor(Color.black);
				g2.fillRect(0, 0, this.getWidth(), this.getHeight());

				// draw and fill circles with the specific colors stored in the data array
				for(int x=0; x<data.length; x++) {
					for (int y=0; y<data[0].length; y++) {
						g2.setColor(new Color(data[x][y]));
						g2.fillArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
						g2.setColor(Color.darkGray);
						g2.drawArc(blockSize * x, blockSize * y, blockSize, blockSize, 0, 360);
					}
				}
			}
		};

		paintPanel.addMouseListener(new MouseListener() {
			@Override public void mouseClicked(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}

			// handle the mouse-up event of the paint panel
			@Override
			public void mouseReleased(MouseEvent e) {
				if (paintMode == PaintMode.Area && e.getX() >= 0 && e.getY() >= 0)
					paintArea(e.getX() / blockSize, e.getY() / blockSize);
			}
		});
		paintPanel.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (paintMode == PaintMode.Pixel && e.getX() >= 0 && e.getY() >= 0)
					try{
						tcp_client.getOut().writeInt(1);
						tcp_client.getOut().writeInt(selectedColor);
						tcp_client.getOut().writeInt(e.getX()/blockSize);
						tcp_client.getOut().writeInt(e.getY()/blockSize);
						tcp_client.getOut().flush();
					}catch(IOException ex){
						ex.printStackTrace();
					}
			}
			@Override public void mouseMoved(MouseEvent e) {}

		});

		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));

		JScrollPane scrollPaneLeft = new JScrollPane(paintPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		basePanel.add(scrollPaneLeft, BorderLayout.CENTER);

		implementToolBox(basePanel);

		implementMessageBox();

		this.setSize(new Dimension(1000, 800));
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setResizable(false);

		this.setLocationRelativeTo(null);
	}

	public void implementToolBox(JPanel basePanel){
		JPanel toolPanel = new JPanel();
		basePanel.add(toolPanel, BorderLayout.NORTH);
		toolPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		JButton saveButton = new JButton("Save");
		toolPanel.add(saveButton);
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveToFile("canvas.txt");
			}
		});

		JButton uploadButton = new JButton("Upload");
		toolPanel.add(uploadButton);
		uploadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadFromFile("canvas.txt");
			}
		});

		JButton eraser = new JButton("Eraser");
		toolPanel.add(eraser);
		eraser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tglPen.setSelected(true);
				tglBucket.setSelected(false);
				tempSelectedColor=selectedColor;
				selectedColor=000000;
				paintMode = PaintMode.Pixel;
			}
		});

		pnlColorPicker = new JPanel();
		pnlColorPicker.setPreferredSize(new Dimension(24, 24));
		pnlColorPicker.setBackground(new Color(selectedColor));
		pnlColorPicker.setBorder(new LineBorder(new Color(0, 0, 0)));

		// show the color picker
		pnlColorPicker.addMouseListener(new MouseListener() {
			@Override public void mouseClicked(MouseEvent e) {}
			@Override public void mouseEntered(MouseEvent e) {}
			@Override public void mouseExited(MouseEvent e) {}
			@Override public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) {
				ColorPicker picker = ColorPicker.getInstance(UI.instance);
				Point location = pnlColorPicker.getLocationOnScreen();
				location.y += pnlColorPicker.getHeight();
				picker.setLocation(location);
				picker.setVisible(true);
			}

		});
		toolPanel.add(studioDropdown);
		JButton refreshBtn = new JButton("Refresh");
		toolPanel.add(refreshBtn);
		refreshBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				udp_client.broadcast();
			}
		});
		studioDropdown.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// Get the selected item
				String selectedStudio = (String) studioDropdown.getSelectedItem();
				if(selectedStudio.equals("My studio")) {
					try {
						udp_client.stop_socket();
						tcp_client.closeConnections();
						server.start_server();
						server.listen_clients();
						tcp_client.setStudio_address("127.0.0.1");
						tcp_client.setStudio_port(12345);
						while (!server.is_server) {
							Thread.sleep(100);
						}
						tcp_client.connect_studio(data);
						tcp_client.getOut().writeInt(2);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}else if(selectedStudio.equals("Search studio...")){
					udp_client.stop_socket();
					udp_client.start_socket();
					udp_client.broadcast();
					udp_client.listen_studio(studioDropdown);
				}else{
					server.stop_server();
					udp_client.start_socket();
					udp_client.broadcast();
					udp_client.listen_studio(studioDropdown);
					tcp_client.setStudio_address(selectedStudio.split(":")[0]);
					tcp_client.setStudio_port(Integer.parseInt(selectedStudio.split(":")[1]));
					tcp_client.connect_studio(data);
					try {
						tcp_client.getOut().writeInt(2);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		toolPanel.add(pnlColorPicker);

		tglPen = new JToggleButton("Pen");
		tglPen.setSelected(true);
		toolPanel.add(tglPen);

		tglBucket = new JToggleButton("Bucket");
		toolPanel.add(tglBucket);

		// change the paint mode to PIXEL mode
		tglPen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(true);
				tglBucket.setSelected(false);
				if(tempSelectedColor!=000000){
					selectedColor=tempSelectedColor;
				}
				paintMode = PaintMode.Pixel;
			}
		});

		// change the paint mode to AREA mode
		tglBucket.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tglPen.setSelected(false);
				tglBucket.setSelected(true);
				paintMode = PaintMode.Area;
			}
		});
	}
	public void implementMessageBox(){
		JPanel msgPanel = new JPanel();

		getContentPane().add(msgPanel, BorderLayout.EAST);

		msgPanel.setLayout(new BorderLayout(0, 0));

		msgField = new JTextField();	// text field for inputting message

		msgPanel.add(msgField, BorderLayout.SOUTH);

		// handle key-input event of the message field
		msgField.addKeyListener(new KeyListener() {
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == 10) {		// if the user press ENTER
					onTextInputted(msgField.getText());
					msgField.setText("");
				}
			}
		});

		chatArea = new JTextArea();		// the read only text area for showing messages
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);

		JScrollPane scrollPaneRight = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPaneRight.setPreferredSize(new Dimension(300, this.getHeight()));
		msgPanel.add(scrollPaneRight, BorderLayout.CENTER);
	}
	public UI() throws IOException{
		initializeUI();
	}
	
	/**
	 * it will be invoked if the user selected the specific color through the color picker
	 * @param colorValue - the selected color
	 */
	public void selectColor(int colorValue) {
		SwingUtilities.invokeLater(()->{
			selectedColor = colorValue;
			pnlColorPicker.setBackground(new Color(colorValue));
		});
	}
		 
	/**
	 * it will be invoked if the user inputted text in the message field
	 * @param text - user inputted text
	 */
	private void onTextInputted(String text) {
//		chatArea.setText(chatArea.getText() + text + "\n");
//		send out message to server
		
		try{
			// Send a message indicator
			tcp_client.getOut().writeInt(0);

			// Send the username
			byte[] usernameBytes = this.username.getBytes();
			tcp_client.getOut().writeInt(usernameBytes.length);
			tcp_client.getOut().write(usernameBytes);

			// Send the address
			byte[] addressBytes = tcp_client.getMy_address().getBytes();
			tcp_client.getOut().writeInt(addressBytes.length);
			tcp_client.getOut().write(addressBytes);

			// send the actual chat message
			tcp_client.getOut().writeInt(text.getBytes().length);
			tcp_client.getOut().write(text.getBytes());
			tcp_client.getOut().flush();
		}catch(IOException e) {
			System.out.println("Error sending a chat message: ");
			e.printStackTrace();
		}
	}
	public void paintPixel(int col, int row) {
		paintPixel(selectedColor, col, row);
	}
	@Override
	public void paintPixel(int color, int col, int row) {
		if (col >= data.length || row >= data[0].length) return;

		data[col][row] = color;
		paintPanel.repaint(col * blockSize, row * blockSize, blockSize, blockSize);
	}
	public List paintArea(int col, int row) {
		LinkedList<Point> filledPixels = new LinkedList<Point>();

		if (col >= data.length || row >= data[0].length) return filledPixels;

		int oriColor = data[col][row];
		LinkedList<Point> buffer = new LinkedList<Point>();
		
		if (oriColor != selectedColor) {
			buffer.add(new Point(col, row));
			
			while(!buffer.isEmpty()) {
				Point p = buffer.removeFirst();
				int x = p.x;
				int y = p.y;
				try{
					tcp_client.getOut().writeInt(1);
					tcp_client.getOut().writeInt(selectedColor);
					tcp_client.getOut().writeInt(x);
					tcp_client.getOut().writeInt(y);
					tcp_client.getOut().flush();
				}catch (Exception e){
					e.printStackTrace();
				}
				if (data[x][y] != oriColor) continue;
				
				data[x][y] = selectedColor;
				filledPixels.add(p);
	
				if (x > 0 && data[x-1][y] == oriColor) buffer.add(new Point(x-1, y));
				if (x < data.length - 1 && data[x+1][y] == oriColor) buffer.add(new Point(x+1, y));
				if (y > 0 && data[x][y-1] == oriColor) buffer.add(new Point(x, y-1));
				if (y < data[0].length - 1 && data[x][y+1] == oriColor) buffer.add(new Point(x, y+1));
			}
			repaintPaintPanel();
		}
		return filledPixels;
	}
	@Override
	public void repaintPaintPanel(){
		paintPanel.repaint();
	}
	@Override
	public void appendChatMessage(String message, boolean is_self){
		SwingUtilities.invokeLater(()->{
			chatArea.append(message+"\n");
		});
	}
	public void setData(int[][] data, int blockSize) {
		this.data = data;
		this.blockSize = blockSize;
		if(this.server != null){
			this.server.setData(data);
		}
		paintPanel.setPreferredSize(new Dimension(data.length * blockSize, data[0].length * blockSize));
		repaintPaintPanel();
	}
}
