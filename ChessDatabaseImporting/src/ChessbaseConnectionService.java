import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicOptionPaneUI;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
/**
 * This method handles connection to the database, along with the GUI
 * @author zonickba, juricar
 *
 */
public class ChessbaseConnectionService {
	
	private final String connectionURL = "jdbc:sqlserver://${dbServer};databaseName=${dbName};user=${user};password={${pass}}";

	private Connection connection = null;
	private String serverName, databaseName;
	private JFrame loginFrame;
	private JFrame useFrame;
	private JFrame inputFrame;
	private JTextField userBox;
	private JTextField passBox;
	private JTextField searchBox;
	private JTable table;
	private JComboBox selectionMenu;
	private JComboBox constraintMenu;
	private int numFields;
	private ArrayList<String> fieldNames;
	private JPanel inputPanel;
	private ArrayList<JTextField> inputs;
	private String[] tableNames = {"Judge", "Person", "Player", "Tournament", "MatchHost", "ChessMatch", "ChessMove", "CompetesIn"};
	private String[] searchOptions = {"None", "ELO Search", "Full Name Search", "Tournament Name Search","Player Matches", "Player Wins", "Player Losses", "Moves By Match ID"};
	private String[] searchTables = {"Player", "Person", "Tournament", "PlayerMatchHistory", "PlayerWinHistory", "PlayerLossHistory", "MatchMoves"};
	private String[] searchWheres = {"ELO", "FullName", "TournamentName", "Username", "Username", "Username", "MatchID"};
	private JButton searchButton;
	private boolean isGuest;
	
	/**
	 * A basic constructor for our connection service.
	 * @param serverName the name of the server we are connecting to
	 * @param databaseName our database name
	 */
	public ChessbaseConnectionService(String serverName, String databaseName) {
		this.serverName = serverName;
		this.databaseName = databaseName;
		this.loginFrame = makeLoginDialog();
		this.isGuest = true;
	}
	
	/**
	 * This is the method that connects to the database.
	 * @param user username
	 * @param pass password
	 * @return
	 */
	public boolean connect(String user, String pass) {
		
		String connectionString = connectionURL.replace("${dbServer}", serverName).replace("${dbName}", databaseName).replace("${user}", user).replace("{${pass}}", pass);
		//System.out.println(connectionString);
		try {
			Connection c = DriverManager.getConnection(connectionString);
			this.connection = c;
			return true;
			
		}
		catch(SQLException e) {
			System.out.println("Invalid Login Credentials");
			JOptionPane.showMessageDialog(null, "Invalid Login Credentials");
		}
		return false;	
	}
	
	/**
	 * @return a connection object (our connection)
	 */
	public Connection getConnection() {
		return this.connection;
	}
	
	/**
	 * closes the connection
	 */
	public void closeConnection() {
		try {
			this.connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}	
	
	/**
	 * Opens the 'use' frame, where we can select from and edit our data.
	 */
	public void openUseFrame() {
		if(this.useFrame != null) {
			return;
		}
		JFrame frame = new JFrame("Chessbase Query Window");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		createMenuBar(frame);
		JPanel panel = new JPanel();
		frame.add(panel, BorderLayout.CENTER);
		JPanel topPanel = new JPanel();
		panel.add(topPanel, BorderLayout.PAGE_START);
		this.loginFrame.dispose();
		if (this.inputFrame != null) {
			this.inputFrame.dispose();
			this.inputFrame = null;
		}
		this.useFrame = frame;
		
		try {
			String query = "SELECT Username, Pswd, FullName, JoinDate FROM Person"; //The starting info, in there by default.
			Statement s = connection.createStatement();
			ResultSet rs = s.executeQuery(query);
			
			//System.out.println("Made Query");
			
			JTable table = new JTable(new DefaultTableModel());
			
			int i = 0;
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			
			model.addColumn("Username"); 
			model.addColumn("Password");
			model.addColumn("Full Name");
			model.addColumn("Join Date");
			while(rs.next()) {
				i++;
				
				model.addRow(new Object[]{rs.getString("Username"), rs.getString("Pswd"), rs.getString("FullName"), rs.getString("JoinDate")});
				
				//System.out.println(rs.getString("Username") + " : " + rs.getString("Pswd") + " : " +rs.getString("FullName") + " : " +rs.getString("JoinDate"));
			}
			JScrollPane scrollPane = new JScrollPane(table);
			
			this.table = table;
			table.setDefaultEditor(Object.class, null);
			table.getTableHeader().setReorderingAllowed(false); 
			
			JComboBox tableList = new JComboBox(tableNames);
			tableList.setSelectedIndex(1);
			tableList.addActionListener(new TableSwitchListener());
			this.selectionMenu = tableList;
			
			JComboBox constraintList = new JComboBox(searchOptions);
			constraintList.setSelectedIndex(0);
			this.constraintMenu = constraintList;
			
			JTextField entryBox = new JTextField(50);
			this.searchBox = entryBox;
			
			JButton searchButton = new JButton("Search");
			searchButton.addActionListener(new SearchButtonListener());
			this.searchButton = searchButton;
			
			topPanel.add(tableList, BorderLayout.WEST);
			topPanel.add(constraintList,BorderLayout.CENTER);
			topPanel.add(entryBox, BorderLayout.EAST);
			topPanel.add(searchButton, BorderLayout.EAST);
			panel.add(scrollPane, BorderLayout.PAGE_END);
			frame.pack();
			frame.setVisible(true);
			//System.out.println("Returned");
			return;
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		closeConnection();
	}
	
	private void openInputFrame() {
		if(this.inputFrame != null) {
			return;
		}
		if(this.isGuest) {
			System.out.println("User is guest. Cannot input or update.");
			JOptionPane.showMessageDialog(null, "User is guest. Cannot input or update.");
			return;
		}
		JFrame frame = new JFrame("Chessbase Query Window");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(1250, 350));
		createMenuBar(frame);
		JPanel panel = new JPanel(new BorderLayout());
		frame.add(panel);
		if (this.useFrame != null) {
			this.useFrame.dispose();
			this.useFrame = null;
		}
		
		JPanel topPanel = new JPanel();
		JLabel tableText = new JLabel("Table to insert into:");
		JComboBox tableChoice = new JComboBox(tableNames);
		tableChoice.setSelectedIndex(-1);
		tableChoice.addActionListener(new TableSwitchListener());
		this.selectionMenu = tableChoice;
		topPanel.add(tableText);
		topPanel.add(tableChoice);
		panel.add(topPanel, BorderLayout.NORTH);
		
		JPanel centerPanel = new JPanel();
		this.inputPanel = centerPanel;
		panel.add(centerPanel, BorderLayout.CENTER);
		
		JPanel bottomPanel = new JPanel();
		JButton insertButton = new JButton("Insert into Table");
		insertButton.addActionListener(null);
		
		this.inputFrame = frame;
		frame.setVisible(true);
	}
	
	private void createMenuBar(JFrame frame) {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Applications");
		
		JMenuItem search = new JMenuItem("Search");
		search.setToolTipText("Search through the Database");
		search.addActionListener((event) -> openUseFrame());
		
		JMenuItem insert = new JMenuItem("Insert");
		insert.setToolTipText("Input data into the Database");
		insert.addActionListener((event) -> openInputFrame());
		
		menu.add(search);
		menu.add(insert);
		
		menuBar.add(menu);
		
		frame.setJMenuBar(menuBar);
		
		
	}
	
	/**
	 * This method makes the login dialog, where the user enters login credentials to acces our database.
	 * @return
	 */
	public JFrame makeLoginDialog() {
		JFrame frame = new JFrame("Chessbase Connection Window");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		frame.add(panel, BorderLayout.CENTER);
		
		JTextField usernameEntry = new JTextField(50);
		usernameEntry.setText("SodaBaseUserzonickba20"); //default credentials
		JTextField passwordEntry = new JTextField(50);
		passwordEntry.setText("Password123");
		JButton connectButton = new JButton("Connect");
		connectButton.addActionListener(new ConnectActionListener());
		JButton connectGuestButton = new JButton("Connect As Guest");
		connectGuestButton.addActionListener(new ConnectGuestActionListener());
		
		panel.add(usernameEntry);
		panel.add(passwordEntry);
		panel.add(connectButton);
		panel.add(connectGuestButton);
		
		this.userBox = usernameEntry;
		this.passBox = passwordEntry;
		
		frame.pack();
		frame.setVisible(true);
		return frame;
	}
	
	/**
	 * This action listener is associated with the connection button, and calls the 
	 * relevant method when the button is hit
	 * @author zonickba
	 *
	 */
	private class ConnectActionListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean connectionSuccessful = connectFromFrame();
			if(!connectionSuccessful) {
				System.out.println("Failed Connection");
				JOptionPane.showMessageDialog(null, "Failed Connection");
				return;
			}
			ChessbaseConnectionService.this.setIsGuest(false);
			openUseFrame();
		}

		private void openUseFrame() {
			ChessbaseConnectionService.this.openUseFrame();
		}

	}
	
	private class ConnectGuestActionListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			boolean connectionSuccessful = connectGuest();
			if(!connectionSuccessful) {
				System.out.println("Failed Connection");
				JOptionPane.showMessageDialog(null, "Failed Connection");
				return;
			}
			openUseFrame();
		}

		private void openUseFrame() {
			ChessbaseConnectionService.this.openUseFrame();
		}

	}
	
	/**
	 * This method gets called when the 'connect' button gets hit. Takes info from the user and password
	 * boxes and connects with it.
	 * @return
	 */
	private boolean connectFromFrame() {
		
		String user = this.userBox.getText();
		String pass = this.passBox.getText();
		
		return connect(user, pass);
	}
	
	public void setIsGuest(boolean b) {
		this.isGuest = b;		
	}

	private boolean connectGuest() {
		
		String user = "SodaBaseUserzonickba20";
		String pass = "Password123";
		
		return connect(user, pass);
	}
	
	/**
	 * This button is attached to the 'Search' button, and executes the search when the button is hit.
	 * @author zonickba
	 *
	 */
	private class SearchButtonListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			String query = "SELECT * FROM ";
			PreparedStatement ps;
			if(constraintMenu.getSelectedIndex() == 0) {
				System.out.println("Bad Selection!");
				JOptionPane.showMessageDialog(null, "Bad Selection!");
				return;
			}
			try { //This part goes and executes the query we want executed.
				query = query + searchTables[constraintMenu.getSelectedIndex() -1] + " WHERE " + searchWheres[constraintMenu.getSelectedIndex() - 1] + " = '" +searchBox.getText()+"'";
				//System.out.println(query);
				ps = connection.prepareStatement(query);
				ResultSet rs = ps.executeQuery();

				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.setColumnCount(0);
				model.setRowCount(0);
				

				ResultSetMetaData rsmd = rs.getMetaData();
				ArrayList<String> names = new ArrayList<String>();
				int columnCount = rsmd.getColumnCount();

				
				for(int i = 0; i < columnCount; i++) {
					names.add(rsmd.getColumnName(i+1));
					model.addColumn(rsmd.getColumnName(i+1));
				}
				
				while(rs.next()) {
					Object[] data = new Object[names.size()];
					for(int j = 0; j < names.size(); j++) {
						data[j] = rs.getString(j+1);
					}
					model.addRow(data);
				}
				
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			
		}
		
	}
	
	/**
	 * When someone switches what table they are viewing, this listener activates, and updates the table
	 * displayed.
	 * @author zonickba
	 *
	 */
	private class TableSwitchListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			Object selected = selectionMenu.getSelectedItem();
			
			String query = "SELECT * FROM "+ selected.toString();
			
			Statement s;
			
			try {
				s = connection.createStatement();
				ResultSet rs = s.executeQuery(query);
				ResultSetMetaData rsmd = rs.getMetaData();
				ArrayList<String> names = new ArrayList<String>();
				int columnCount = rsmd.getColumnCount();
				numFields = columnCount;
			
				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.setColumnCount(0);
				model.setRowCount(0);
				for(int i = 0; i < columnCount; i++) {
					names.add(rsmd.getColumnName(i+1));
					model.addColumn(rsmd.getColumnName(i+1));
				}
				fieldNames = names;
				
				int i = 0;
				while(rs.next()) {
					i++;
					Object[] data = new Object[names.size()];
					for(int j = 0; j < names.size(); j++) {
						data[j] = rs.getString(j+1);
					}
					model.addRow(data);
				}
				if (inputPanel != null) {
					inputPanel.removeAll();
					ArrayList<JTextField> texts = new ArrayList<JTextField>();
					for (int k = 0; k < numFields; k++) {
						JTextField text = new JTextField(10);
						text.setText(fieldNames.get(k));
						texts.add(text);
						inputPanel.add(text);
					}
					inputs = texts;
					inputPanel.revalidate();
					inputPanel.repaint();
					inputFrame.repaint();
				}
				
				
				//System.out.println(names);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			//System.out.println(query);
		}
		
	}
	
	private class InsertButtonListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			Object selected = selectionMenu.getSelectedItem();
			
			String query = "EXEC " + selected +"Insert ";
			PreparedStatement ps;
			try { //This part goes and executes the query we want executed.
				query = query + searchTables[constraintMenu.getSelectedIndex() -1] + " WHERE " + searchWheres[constraintMenu.getSelectedIndex() - 1] + " = '" +searchBox.getText()+"'";
				//System.out.println(query);
				ps = connection.prepareStatement(query);
				ResultSet rs = ps.executeQuery();

				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.setColumnCount(0);
				model.setRowCount(0);
				

				ResultSetMetaData rsmd = rs.getMetaData();
				ArrayList<String> names = new ArrayList<String>();
				int columnCount = rsmd.getColumnCount();

				
				for(int i = 0; i < columnCount; i++) {
					names.add(rsmd.getColumnName(i+1));
					model.addColumn(rsmd.getColumnName(i+1));
				}
				
				while(rs.next()) {
					Object[] data = new Object[names.size()];
					for(int j = 0; j < names.size(); j++) {
						data[j] = rs.getString(j+1);
					}
					model.addRow(data);
				}
				
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			
		}
		
	}

}
